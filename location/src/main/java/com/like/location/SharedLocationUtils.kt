package com.like.location

import android.content.Context
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.hardware.SensorManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.ZoomControls
import com.baidu.location.BDLocation
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.trace.api.entity.EntityListResponse
import com.baidu.trace.api.entity.OnEntityListener
import com.like.livedatabus.LiveDataBus
import com.like.location.databinding.ViewMapMarkerBinding
import com.like.location.entity.CircleFenceInfo
import com.like.location.listener.MyLocationListener
import com.like.location.util.GlideUtils
import com.like.location.util.LocationConstants
import com.like.location.util.RxJavaUtils
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.bundleOf
import java.io.Serializable

/**
 * 共享位置管理工具类
 * 注意：需要在Activity的相应生命周期调用相应的方法
 *
 * @param baiduMapView          百度的MapView
 * @param serviceId             轨迹服务id
 * @param myEntityName          自己的entityName，一般为userId
 * @param myIconResId           自己的图标
 * @param myIconUrl             自己的图标
 * @param defaultIconResId      marker的默认icon资源id
 * @param period                查询好友数据的周期，毫秒，默认5000
 */
class SharedLocationUtils(private val baiduMapView: MapView,
                          private val serviceId: Long,
                          private val myEntityName: String,
                          private val myIconResId: Int = -1,
                          private val myIconUrl: String = "",
                          private val defaultIconResId: Int = R.drawable.icon_marker_default,// 默认marker图标
                          private val period: Long = LocationConstants.DEFAULT_QUERY_ENTITY_LIST_INTERVAL) {
    companion object {
        private val TAG = SharedLocationUtils::class.java.simpleName
        const val KEY_MARKER_EXTRAINFO = "key_marker_extrainfo"
    }

    private val context: Context by lazy { baiduMapView.context }
    private val markerInfos = mutableListOf<MarkerInfo>()// 需要显示在地图上的marker，不包括自己
    private val mMarkers = mutableListOf<Marker>()
    private val mGlideUtils: GlideUtils by lazy { GlideUtils(context) }
    private val mTraceUtils: TraceUtils by lazy { TraceUtils(context, baiduMapView.map, serviceId, myEntityName) }
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
        mTraceUtils.startTrace()

        fun startLocationMy(myBitmapDescriptor: BitmapDescriptor) {
            initBaiduMap(baiduMapView, myBitmapDescriptor)
            mLocationUtils.start()
        }

        when {
            myIconResId != -1 -> startLocationMy(BitmapDescriptorFactory.fromResource(myIconResId))
            myIconUrl.isNotEmpty() -> mGlideUtils.downloadImage(myIconUrl, {
                startLocationMy(BitmapDescriptorFactory.fromView(wrapMarker(it)))
            }, {
                startLocationMy(BitmapDescriptorFactory.fromView(wrapMarker()))
            })
            else -> startLocationMy(BitmapDescriptorFactory.fromView(wrapMarker()))
        }
    }

    fun setMarkerList(markerInfos: List<MarkerInfo>) {
        if (markerInfos.isEmpty()) return
        this.markerInfos.addAll(markerInfos)
        disposable = RxJavaUtils.interval(period, Schedulers.io()) {
            queryMarkers()
        }
    }

    /**
     * 对marker图标进行了一层外圈包装
     */
    private fun wrapMarker(bitmap: Bitmap? = null): View {
        val binding = DataBindingUtil.inflate<ViewMapMarkerBinding>(LayoutInflater.from(context), R.layout.view_map_marker, null, false)
        if (bitmap != null) {
            binding.iv.setImageBitmap(bitmap)
        } else {
            binding.iv.setImageResource(defaultIconResId)
        }
        return binding.root
    }

    private fun initBaiduMap(baiduMapView: MapView, myBitmap: BitmapDescriptor) {
        // 隐藏百度地图的logo
        val child = baiduMapView.getChildAt(1)
        if (child != null && (child is ImageView || child is ZoomControls)) {
            child.visibility = View.GONE
        }
        baiduMapView.showScaleControl(false)// 隐藏地图上的比例尺
        baiduMapView.showZoomControls(false)// 隐藏地图上的缩放控件

        baiduMapView.map.setMyLocationConfiguration(MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, false, myBitmap, 0x00ffffff, 0x00ffffff))
        // 开启定位图层
        baiduMapView.map.isMyLocationEnabled = true
        // Marker点击
        baiduMapView.map.setOnMarkerClickListener {
            LiveDataBus.post(LocationConstants.TAG_CLICK_MARKER, getMarkerInfoFromMarker(it))
            true
        }
    }

    /**
     * 定位到自己的位置
     */
    fun locationMyPosition() {
        setMapCenter(LatLng(mCurrentLat, mCurrentLng))
    }

    /**
     * 创建围栏，只创建一次
     */
    fun createFences(circleFenceInfoList: List<CircleFenceInfo>) {
        if (circleFenceInfoList.isEmpty()) {
            return
        }
        if (isFirstLoc) {
            isFirstLoc = false
            this.circleFenceInfoList = circleFenceInfoList
            mTraceUtils.createLocalFences(circleFenceInfoList)
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

    fun getLat() = mCurrentLat

    fun getLng() = mCurrentLng

    /**
     * 查询指定entityName的Entity，并添加到地图上
     */
    private fun queryMarkers() {
        val entityNames = getEntityNames()
        if (entityNames.isNotEmpty()) {
            mTraceUtils.queryEntityList(entityNames, listener = object : OnEntityListener() {
                override fun onEntityListCallback(p0: EntityListResponse?) {
                    Log.d(TAG, "onEntityListCallback ${p0?.entities}")
                    if (p0 == null || p0.entities == null || p0.entities.isEmpty()) {
                        Log.d(TAG, "没有查到entity，清除所有marker")
                        clearMarker()
                    } else {
                        val entityNamesResult = mutableListOf<String>()
                        p0.entities.mapTo(entityNamesResult) {
                            it.entityName
                        }
                        // 下线的
                        entityNames.subtract(entityNamesResult).forEach {
                            Log.d(TAG, "entity（$it）离线，删除")
                            val marker = getMarkerByEntityName(it)
                            removeMarker(marker)
                        }
                        // 在线的
                        p0.entities.forEach {
                            val lat = it.latestLocation.location.latitude
                            val lng = it.latestLocation.location.longitude

                            val marker = getMarkerByEntityName(it.entityName)
                            if (marker != null) {// 已经存在了
                                Log.d(TAG, "entity（${it.entityName}）已经存在了，改变位置")
                                getMarkerInfoFromMarker(marker)?.let { markerInfo ->
                                    markerInfo.lat = lat
                                    markerInfo.lng = lng
                                }
                                changeMarkerPosition(marker, lat, lng)
                            } else {
                                Log.d(TAG, "entity（${it.entityName}）不存在，创建")
                                getMarkerInfoByEntityName(it.entityName)?.let { markerInfo ->
                                    markerInfo.lat = lat
                                    markerInfo.lng = lng
                                    addMarker(baiduMapView.map, markerInfo, lat, lng)
                                }
                            }
                        }
                    }
                }
            })
        }
    }

    private fun getMarkerByEntityName(entityName: String): Marker? {
        val filter = mMarkers.filter {
            (getMarkerInfoFromMarker(it)?.entityName ?: "") == entityName
        }
        return if (filter.isNotEmpty()) filter[0] else null
    }

    private fun getEntityNames(): List<String> {
        val entityNames = mutableListOf<String>()
        markerInfos.mapTo(entityNames) {
            it.entityName
        }
        return entityNames
    }

    private fun getMarkerInfoByEntityName(entityName: String): MarkerInfo? {
        val filter = markerInfos.filter { it.entityName == entityName }
        return if (filter.isNotEmpty()) {
            filter[0]
        } else {
            null
        }
    }

    private fun addMarker(baiduMap: BaiduMap, markerInfo: MarkerInfo, lat: Double, lng: Double) {
        if (markerInfo.iconUrl.isEmpty()) {
            addIconMarker(baiduMap, lat, lng, BitmapDescriptorFactory.fromView(wrapMarker()), markerInfo)
        } else {
            mGlideUtils.downloadImage(markerInfo.iconUrl,
                    { bitmap ->
                        addIconMarker(baiduMap, lat, lng, BitmapDescriptorFactory.fromView(wrapMarker(bitmap)), markerInfo)
                    },
                    {
                        addIconMarker(baiduMap, lat, lng, BitmapDescriptorFactory.fromView(wrapMarker()), markerInfo)
                    }
            )
        }
    }

    /**
     * 添加指定icon的marker
     */
    private fun addIconMarker(baiduMap: BaiduMap, lat: Double, lng: Double, bitmapDescriptor: BitmapDescriptor, markerInfo: MarkerInfo) {
        if (!mMarkers.any { getMarkerInfoFromMarker(it)?.entityName == markerInfo.entityName }) {
            val overlayOptions = MarkerOptions().position(LatLng(lat, lng)).icon(bitmapDescriptor).zIndex(9).draggable(false)
            mMarkers.add(addMarkerInfoToMarker(markerInfo, baiduMap.addOverlay(overlayOptions) as Marker))
        }
    }

    private fun addMarkerInfoToMarker(markerInfo: MarkerInfo, marker: Marker): Marker {
        marker.extraInfo = bundleOf(KEY_MARKER_EXTRAINFO to markerInfo)
        return marker
    }

    private fun getMarkerInfoFromMarker(marker: Marker): MarkerInfo? {
        val extraInfo = marker.extraInfo[KEY_MARKER_EXTRAINFO]
        return if (extraInfo != null) {
            extraInfo as MarkerInfo
        } else {
            null
        }
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

    private fun removeMarker(marker: Marker?) {
        marker?.let {
            it.remove()
            mMarkers.remove(it)
        }
    }

    private fun clearMarker() {
        if (mMarkers.isNotEmpty()) {
            mMarkers.forEach {
                it.remove()
            }
            mMarkers.clear()
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

        mTraceUtils.destroy()

        mMarkers.clear()

        baiduMapView.map.isMyLocationEnabled = false
        baiduMapView.map.clear()

        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        baiduMapView.onDestroy() // 关闭定位图层
    }

    /**
     * @param entityName
     * @param iconUrl marker的图标
     * @param userId userId
     * @param userNickName 用户昵称
     * @param phone 电话号码
     */
    class MarkerInfo(val entityName: String, val iconUrl: String, val userId: String, val name: String, val userNickName: String, val phone: String) : Serializable {
        var lat = 0.0// 经度
        var lng = 0.0// 纬度
    }

    /**
     * 查询本地围栏历史告警信息
     *
     * @param startTime         开始时间戳，默认为当前时间以前24小时
     * @param endTime           结束时间戳，默认为当前时间
     */
    fun queryFenceHistoryAlarmInfo(
            startTime: Long = System.currentTimeMillis() / 1000 - 24 * 60 * 60,
            endTime: Long = System.currentTimeMillis() / 1000
    ) {
        mTraceUtils.queryFenceHistoryAlarmInfo(startTime, endTime)
    }

    /**
     * 在本地查询被监控者状态
     * 查询被监控者是在围栏内或围栏外
     */
    fun queryMonitoredStatus() {
        mTraceUtils.queryMonitoredStatus()
    }

}
