package com.like.location

import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ZoomControls
import com.baidu.location.BDLocation
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.trace.api.entity.EntityListResponse
import com.baidu.trace.api.entity.OnEntityListener
import com.like.livedatabus.LiveDataBus
import com.like.location.entity.CircleFenceInfo
import com.like.location.listener.MyLocationListener
import com.like.location.util.LocationConstants
import com.like.location.util.RxJavaUtils
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.Serializable

/**
 * 共享位置管理工具类
 * 注意：需要在Activity的相应生命周期调用相应的方法
 *
 * @param baiduMapView          百度的MapView
 * @param serviceId             轨迹服务id
 * @param myEntityName          自己的entityName，一般为userId
 * @param period                查询好友数据的周期，毫秒，默认5000
 */
class SharedLocationUtils(private val baiduMapView: MapView,
                          private val serviceId: Long,
                          private val myEntityName: String,
                          private val period: Long = LocationConstants.DEFAULT_QUERY_ENTITY_LIST_INTERVAL) {
    companion object {
        private val TAG = SharedLocationUtils::class.java.simpleName
    }

    private val context: Context by lazy { baiduMapView.context }
    private val markerInfos = mutableListOf<MarkerInfo>()// 需要显示在地图上的marker，不包括自己
    private val mMyTraceUtils: MyTraceUtils by lazy { MyTraceUtils(context, baiduMapView.map, serviceId, myEntityName) }
    private var disposable: Disposable? = null
    private var circleFenceInfoList: List<CircleFenceInfo>? = null
    // 传感器相关
    private val mSensorManager: SensorManager by lazy { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private var lastX: Double = 0.0
    // 定位自己
    private var mCurrentDirection = 0
    private var mCurrentLat = 0.0// 经度
    private var mCurrentLng = 0.0// 纬度
    private var mCurrentAccuracy = 0f// 精确度
    private var isFirstLoc = true // 是否首次定位
    private val mLocationUtils: LocationUtils by lazy {
        LocationUtils.getInstance(context).addListener(
                object : MyLocationListener {
                    override fun onReceiveLocation(location: BDLocation?) {
                        // map view 销毁后不在处理新接收的位置
                        if (location == null || baiduMapView.map == null) {
                            return
                        }
                        mCurrentLat = location.latitude
                        mCurrentLng = location.longitude
                        mCurrentAccuracy = location.radius

                        // 显示自己的位置，包括方向只是图标，精度圈
                        val locData = MyLocationData.Builder()
                                .accuracy(mCurrentAccuracy)
                                .direction(mCurrentDirection.toFloat())// 此处设置开发者获取到的方向信息，顺时针0-360
                                .latitude(mCurrentLat)
                                .longitude(mCurrentLng)
                                .build()
                        baiduMapView.map.setMyLocationData(locData)
                    }
                }
        )
    }

    init {
        // 初始化百度地图
        // 隐藏百度地图的logo
        val child = baiduMapView.getChildAt(1)
        if (child != null && (child is ImageView || child is ZoomControls)) {
            child.visibility = View.GONE
        }
        baiduMapView.showScaleControl(false)// 隐藏地图上的比例尺
        baiduMapView.showZoomControls(false)// 隐藏地图上的缩放控件
        // 开启定位图层
        baiduMapView.map.isMyLocationEnabled = true
        // Marker点击
        baiduMapView.map.setOnMarkerClickListener {
            LiveDataBus.post(LocationConstants.TAG_CLICK_MARKER, getMarkerInfoByMarker(it))
            true
        }

        mMyTraceUtils.startTrace()
        mLocationUtils.start()
    }

    fun setMarkerList(markerInfos: List<MarkerInfo>) {
        if (markerInfos.isEmpty()) return
        this.markerInfos.addAll(markerInfos)
        disposable = RxJavaUtils.interval(period, Schedulers.io()) { _ ->
            // 查询指定entityName的Entity，并添加到地图上
            val entityNames = markerInfos.map { it.entityName }
            if (entityNames.isNotEmpty()) {
                TraceUtils.getInstance(context).queryEntityList(serviceId, entityNames, 30000, listener = object : OnEntityListener() {
                    override fun onEntityListCallback(entityListResponse: EntityListResponse?) {
                        Log.d(TAG, "onEntityListCallback ${entityListResponse?.entities}")
                        if (entityListResponse == null || entityListResponse.entities == null || entityListResponse.entities.isEmpty()) {
                            Log.d(TAG, "没有查到entity，清除所有marker")
                            clearMarkerInfo()
                        } else {
                            val entityNamesResult = mutableListOf<String>()
                            entityListResponse.entities.mapTo(entityNamesResult) {
                                it.entityName
                            }
                            // 下线的
                            entityNames.subtract(entityNamesResult).forEach {
                                Log.d(TAG, "entity（$it）离线，删除")
                                removeMarkerInfo(it)
                            }
                            // 在线的
                            entityListResponse.entities.forEach {
                                val lat = it.latestLocation.location.latitude
                                val lng = it.latestLocation.location.longitude

                                val markerInfo = getMarkerInfoByEntityName(it.entityName)
                                        ?: return@forEach
                                markerInfo.lat = lat
                                markerInfo.lng = lng

                                val marker = markerInfo.marker
                                if (marker != null) {// 已经存在了
                                    Log.d(TAG, "entity（${it.entityName}）已经存在了，改变位置")
                                    marker.position = LatLng(lat, lng)
                                } else {
                                    Log.d(TAG, "entity（${it.entityName}）不存在，创建")
                                    val overlayOptions = MarkerOptions()
                                            .position(LatLng(markerInfo.lat, markerInfo.lng))
                                            .zIndex(9)
                                            .draggable(false)
                                    markerInfo.getBitmapDescriptor()?.let { bitmapDescriptor ->
                                        overlayOptions.icon(bitmapDescriptor)
                                    }
                                    markerInfo.marker = baiduMapView.map.addOverlay(overlayOptions) as Marker
                                }
                            }
                        }
                    }
                })
            }
        }
    }

    /**
     * 设置我的定位图标的视图
     */
    fun setMyLocationIconView(view: View) {
        baiduMapView.map.setMyLocationConfiguration(
                MyLocationConfiguration(
                        MyLocationConfiguration.LocationMode.NORMAL,
                        false,
                        BitmapDescriptorFactory.fromView(view),
                        0x00ffffff,
                        0x00ffffff
                )
        )
    }

    /**
     * 定位到自己的位置
     */
    fun locationMyPosition() {
        setMapCenter(LatLng(mCurrentLat, mCurrentLng))
    }

    /**
     * 创建围栏，只创建一次。并设置第一个围栏为地图中心。
     */
    fun createFences(circleFenceInfoList: List<CircleFenceInfo>) {
        if (circleFenceInfoList.isEmpty()) {
            return
        }
        if (isFirstLoc) {
            isFirstLoc = false
            this.circleFenceInfoList = circleFenceInfoList
            mMyTraceUtils.createLocalFences(circleFenceInfoList)
            // 设置第一个围栏为地图中心
            setMapCenter(circleFenceInfoList[0].latLng)
        }
    }

    // 设置指定index的围栏为地图中心
    fun setMapCenter(index: Int) {
        circleFenceInfoList?.get(index)?.let {
            setMapCenter(it.latLng)
        }
    }

    // 设置地图中心
    private fun setMapCenter(latLng: LatLng?) {
        latLng?.let {
            val builder = MapStatus.Builder().target(latLng).zoom(18.0f).overlook(0f)
            baiduMapView.map.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
        }
    }

    fun getMyLat() = mCurrentLat

    fun getMyLng() = mCurrentLng

    private fun getMarkerInfoByMarker(marker: Marker): MarkerInfo? {
        val filter = markerInfos.filter { it.marker == marker }
        return if (filter.isNotEmpty()) filter[0] else null
    }

    private fun getMarkerInfoByEntityName(entityName: String): MarkerInfo? {
        val filter = markerInfos.filter { it.entityName == entityName }
        return if (filter.isNotEmpty()) filter[0] else null
    }

    private fun removeMarkerInfo(entityName: String) {
        val listIterator = markerInfos.listIterator()
        run breaking@{
            listIterator.forEach continuing@{ markerInfo ->
                if (markerInfo.entityName == entityName) {
                    markerInfo.marker?.remove()
                    listIterator.remove()
                    return@breaking
                }
            }
        }
    }

    private fun clearMarkerInfo() {
        if (markerInfos.isNotEmpty()) {
            markerInfos.forEach {
                it.marker?.remove()
            }
            markerInfos.clear()
        }
    }

    fun onPause() {
        baiduMapView.onPause()
    }

    fun onResume() {
        baiduMapView.onResume()
        //为系统的方向传感器注册监听器
//        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI)
    }

    fun onStop() {
        //取消注册传感器监听
//        mSensorManager.unregisterListener(this)
    }

    // 传感器相关，需要类实现SensorEventListener接口
//    override fun onSensorChanged(sensorEvent: SensorEvent) {
//        val x = sensorEvent.values[SensorManager.DATA_X].toDouble()
//        if (Math.abs(x - lastX) > 1.0) {
//            mCurrentDirection = x.toInt()
//            val locData = MyLocationData.Builder()
//                    .accuracy(mCurrentAccuracy)
//                    // 此处设置开发者获取到的方向信息，顺时针0-360
//                    .direction(mCurrentDirection.toFloat()).latitude(mCurrentLat)
//                    .longitude(mCurrentLng).build()
//            baiduMapView.map.setMyLocationData(locData)
//        }
//        lastX = x
//    }
//
//    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun onDestroy() {
        disposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }

        mLocationUtils.stop()

        mMyTraceUtils.destroy()

        clearMarkerInfo()

        baiduMapView.map.isMyLocationEnabled = false
        baiduMapView.map.clear()// 清除地图上所有覆盖物

        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        baiduMapView.onDestroy() // 关闭定位图层
    }

    /**
     * @param entityName
     * @param iconView      marker的图标视图
     * @param extraInfo     额外的数据
     */
    class MarkerInfo(val entityName: String, val iconView: View, val extraInfo: Bundle) : Serializable {
        var marker: Marker? = null
        var lat = 0.0// 经度
        var lng = 0.0// 纬度

        fun getBitmapDescriptor(): BitmapDescriptor? = BitmapDescriptorFactory.fromView(iconView)
    }

}
