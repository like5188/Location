package com.like.location

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.View
import com.baidu.location.BDLocation
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.trace.api.entity.EntityListResponse
import com.baidu.trace.api.entity.OnEntityListener
import com.like.common.util.GlideUtils
import com.like.common.util.RxJavaUtils
import com.like.logger.Logger
import io.reactivex.disposables.Disposable

/**
 * 共享位置管理工具类
 * 注意：需要在Activity的相应生命周期调用相应的方法
 *
 * @param baiduMapView 百度的MapView
 * @param serviceId 轨迹服务id
 * @param markerInfos 需要显示在地图上的marker，不包括自己
 * @param defaultIconResId marker的默认icon资源id
 * @param period 查询好友数据的周期，毫秒，默认5000
 */
class SharedLocationUtils(val baiduMapView: MapView, val serviceId: Long, val markerInfos: List<MarkerInfo>, val defaultIconResId: Int = R.drawable.icon_marker_default, val period: Long = Constants.DEFAULT_QUERY_ENTITY_LIST_INTERVAL) : SensorEventListener {
    private val context: Context by lazy { baiduMapView.context }
    private val mMarkers = mutableListOf<Marker>()
    private val mTraceUtils: TraceUtils by lazy { TraceUtils(context, serviceId) }
    private val mGlideUtils: GlideUtils by lazy { GlideUtils(context) }
    private val disposable: Disposable  by lazy { RxJavaUtils.polling({ queryMarkers() }, 0, period) }
    // 定位自己
    private val mSensorManager: SensorManager by lazy { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private var lastX: Double = 0.0
    private var mCurrentDirection = 0
    private var mCurrentLat = 0.0
    private var mCurrentLon = 0.0
    private var mCurrentAccracy = 0f
    private var locData: MyLocationData? = null
    private var isFirstLoc = true // 是否首次定位
    private val mLocationUtils: LocationUtils by lazy {
        LocationUtils(context, object : MyLocationListener() {
            override fun onReceiveLocation(location: BDLocation?) {
                super.onReceiveLocation(location)
                // map view 销毁后不在处理新接收的位置
                if (location == null || baiduMapView.map == null) {
                    return
                }
                mCurrentLat = location.latitude
                mCurrentLon = location.longitude
                mCurrentAccracy = location.radius
                locData = MyLocationData.Builder()
                        .accuracy(location.radius)
                        .direction(mCurrentDirection.toFloat())// 此处设置开发者获取到的方向信息，顺时针0-360
                        .latitude(location.latitude)
                        .longitude(location.longitude).build()
                baiduMapView.map.setMyLocationData(locData)
                if (isFirstLoc) {
                    isFirstLoc = false
                    val ll = LatLng(location.latitude, location.longitude)
                    val builder = MapStatus.Builder()
                    builder.target(ll).zoom(18.0f)
                    baiduMapView.map.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
                }
            }
        })
    }

    fun start() {
        initBaiduMap(baiduMapView.map)
        mLocationUtils.start()
        mTraceUtils.startTrace()
        disposable
    }

    /**
     * 查询指定entityName的Entity，并添加到地图上
     */
    private fun queryMarkers() {
        val entityNames = getEntityNames()
        if (entityNames.isNotEmpty()) {
            mTraceUtils.queryEntityList(entityNames, object : OnEntityListener() {
                override fun onEntityListCallback(p0: EntityListResponse?) {
                    Logger.d("onEntityListCallback")
                    p0?.entities?.forEach {
                        val lat = it.latestLocation.location.latitude
                        val lng = it.latestLocation.location.longitude
                        val iconUrl = getIconUrl(it.entityName)
                        addMarker(baiduMapView.map, lat, lng, iconUrl)
                    }
                }
            })
        }
    }

    private fun getEntityNames(): List<String> {
        val entityNames = mutableListOf<String>()
        markerInfos.mapTo(entityNames) {
            it.entityName
        }
        return entityNames
    }

    private fun getIconUrl(entityName: String): String {
        val filter = markerInfos.filter { it.entityName == entityName }
        return if (filter.isNotEmpty()) {
            filter[0].iconUrl
        } else {
            ""
        }
    }

    private fun addMarker(baiduMap: BaiduMap, lat: Double, lng: Double, iconUrl: String) {
        if (iconUrl.isEmpty()) {
            addDefaultIconMarker(baiduMap, lat, lng)
        } else {
            mGlideUtils.downloadImage(iconUrl).subscribe(
                    { bitmap ->
                        addIconMarker(baiduMap, lat, lng, bitmap)
                    },
                    {
                        addDefaultIconMarker(baiduMap, lat, lng)
                    }
            )
        }
    }

    /**
     * 添加指定icon的marker
     */
    private fun addIconMarker(baiduMap: BaiduMap, lat: Double, lng: Double, bitmap: Bitmap) {
        val overlayOptions = MarkerOptions().position(LatLng(lat, lng)).icon(BitmapDescriptorFactory.fromBitmap(bitmap)).zIndex(9).draggable(false)
        mMarkers.add(baiduMap.addOverlay(overlayOptions) as Marker)
    }

    /**
     * 添加默认icon的marker
     */
    private fun addDefaultIconMarker(baiduMap: BaiduMap, lat: Double, lng: Double) {
        val overlayOptions = MarkerOptions().position(LatLng(lat, lng)).icon(BitmapDescriptorFactory.fromResource(defaultIconResId)).zIndex(9).draggable(false)
        mMarkers.add(baiduMap.addOverlay(overlayOptions) as Marker)
    }

    /**
     * 显示信息窗口
     */
    private fun showInfoWindow(baiduMap: BaiduMap, view: View, marker: Marker, listener: InfoWindow.OnInfoWindowClickListener) {
        val infoWindow = InfoWindow(BitmapDescriptorFactory.fromView(view), marker.position, -47, listener)
        baiduMap.showInfoWindow(infoWindow)
    }

    /**
     * 改变marker位置
     */
    private fun changeMarkerPosition(marker: Marker, lat: Double, lng: Double) {
        marker.position = LatLng(lat, lng)
    }

    /**
     * 改变marker图标
     */
    private fun changeMarkerIcon(marker: Marker, bitmapDescriptor: BitmapDescriptor) {
        marker.icon = bitmapDescriptor
    }

    /**
     * 移除marker
     */
    private fun removeMarker(marker: Marker) {
        marker.remove()
    }

    private fun initBaiduMap(baiduMap: BaiduMap, locationMode: MyLocationConfiguration.LocationMode = MyLocationConfiguration.LocationMode.FOLLOWING) {
        setLocationMode(baiduMap, locationMode)
        // 开启定位图层
        baiduMap.isMyLocationEnabled = true
    }

    /**
     * 设置定位模式
     */
    private fun setLocationMode(baiduMap: BaiduMap, mode: MyLocationConfiguration.LocationMode) {
        baiduMap.setMyLocationConfiguration(MyLocationConfiguration(mode, true, null))
        val builder = MapStatus.Builder()
        builder.overlook(0f)
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
    }

    fun onPause() {
        baiduMapView.onPause()
    }

    fun onResume() {
        baiduMapView.onResume()
        //为系统的方向传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI)
    }

    fun onStop() {
        //取消注册传感器监听
        mSensorManager.unregisterListener(this)
    }

    fun onDestroy() {
        if (!disposable.isDisposed) {
            disposable.dispose()
        }

        mLocationUtils.stop()

        mTraceUtils.stopTrace()

        baiduMapView.map.isMyLocationEnabled = false
        baiduMapView.map.clear()
        mMarkers.clear()
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        baiduMapView.onDestroy() // 关闭定位图层
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val x = sensorEvent.values[SensorManager.DATA_X].toDouble()
        if (Math.abs(x - lastX) > 1.0) {
            mCurrentDirection = x.toInt()
            locData = MyLocationData.Builder()
                    .accuracy(mCurrentAccracy)
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection.toFloat()).latitude(mCurrentLat)
                    .longitude(mCurrentLon).build()
            baiduMapView.map.setMyLocationData(locData)
        }
        lastX = x
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    data class MarkerInfo(val entityName: String, val iconUrl: String)

}
