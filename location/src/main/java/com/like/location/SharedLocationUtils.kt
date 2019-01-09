package com.like.location

import android.content.Context
import android.hardware.SensorManager
import android.util.Log
import android.view.View
import com.baidu.location.BDLocation
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.MyLocationConfiguration
import com.baidu.mapapi.map.MyLocationData
import com.baidu.mapapi.model.LatLng
import com.baidu.trace.api.entity.EntityListResponse
import com.baidu.trace.api.entity.OnEntityListener
import com.like.location.entity.CircleFenceInfo
import com.like.location.entity.MarkerInfo
import com.like.location.listener.MyLocationListener
import com.like.location.util.LocationConstants
import com.like.location.util.RxJavaUtils
import com.like.location.util.SingletonHolder
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlin.jvm.functions.FunctionN

/**
 * 共享位置管理工具类
 * 注意：需要在Activity的相应生命周期调用相应的方法
 */
class SharedLocationUtils(private val context: Context) {
    companion object : SingletonHolder<SharedLocationUtils>(object : FunctionN<SharedLocationUtils> {
        override val arity: Int = 1 // number of arguments that must be passed to constructor

        override fun invoke(vararg args: Any?): SharedLocationUtils {
            return SharedLocationUtils(args[0] as Context)
        }
    }) {
        private val TAG = SharedLocationUtils::class.java.simpleName
    }

    private var serviceId: Long = 0L
    private val mBaiduMapManager: BaiduMapManager by lazy { BaiduMapManager.getInstance() }
    private val mMarkerManager: MarkerManager by lazy { MarkerManager.getInstance() }
    private val mMyTraceUtils: MyTraceUtils by lazy { MyTraceUtils.getInstance(context) }
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
                        if (location == null) {
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
                        mBaiduMapManager.setMyLocationData(locData)
                    }
                }
        )
    }

    /**
     * @param baiduMapView          百度的MapView
     * @param serviceId             轨迹服务id
     * @param myEntityName          自己的entityName，一般为userId
     */
    fun init(baiduMapView: MapView, serviceId: Long, myEntityName: String) {
        this.serviceId = serviceId
        mBaiduMapManager.init(baiduMapView)
        mMyTraceUtils.init(baiduMapView.map, serviceId, myEntityName)
        mMyTraceUtils.startTrace()
        mLocationUtils.start()
    }

    /**
     * 设置Marker
     *
     * @param markerInfos
     * @param period        查询好友数据的周期，毫秒，默认5000
     */
    fun setMarkerList(markerInfos: List<MarkerInfo>, period: Long = LocationConstants.DEFAULT_QUERY_ENTITY_LIST_INTERVAL) {
        if (markerInfos.isEmpty()) return
        mMarkerManager.addMarkerList(markerInfos)
        disposable = RxJavaUtils.interval(period, Schedulers.io()) { _ ->
            // 查询指定entityName的Entity，并添加到地图上
            val entityNames = mMarkerManager.getEntityNames()
            if (entityNames.isNotEmpty()) {
                TraceUtils.getInstance(context).queryEntityList(serviceId, entityNames, 30000, listener = object : OnEntityListener() {
                    override fun onEntityListCallback(entityListResponse: EntityListResponse?) {
                        Log.d(TAG, "onEntityListCallback ${entityListResponse?.entities}")
                        if (entityListResponse == null || entityListResponse.entities == null || entityListResponse.entities.isEmpty()) {
                            Log.d(TAG, "没有查到entity，清除所有marker")
                            mMarkerManager.clearMarkerInfo()
                        } else {
                            // 下线的。subtract()差集
                            entityNames.subtract(entityListResponse.entities.map { it.entityName })
                                    .forEach {
                                        Log.d(TAG, "entity（$it）离线，删除")
                                        mMarkerManager.removeMarkerInfo(it)
                                    }
                            // 在线的
                            entityListResponse.entities.forEach {
                                val markerInfo = mMarkerManager.getMarkerInfoByEntityName(it.entityName)
                                        ?: return@forEach
                                markerInfo.lat = it.latestLocation.location.latitude
                                markerInfo.lng = it.latestLocation.location.longitude

                                if (markerInfo.marker != null) {// 已经存在了
                                    Log.d(TAG, "entity（${it.entityName}）已经存在了，改变位置")
                                    mMarkerManager.changeMarkerPosition(it.entityName)
                                } else {
                                    Log.d(TAG, "entity（${it.entityName}）不存在，创建")
                                    mMarkerManager.createMarker(mBaiduMapManager.getBaiduMap(), it.entityName)
                                }
                            }
                        }
                    }
                })
            }
        }
    }

    /**
     * 定位到自己的位置
     */
    fun locationMyPosition() {
        mBaiduMapManager.setMapCenter(LatLng(mCurrentLat, mCurrentLng))
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
            mBaiduMapManager.setMapCenter(circleFenceInfoList[0].latLng)
        }
    }

    // 设置指定index的围栏为地图中心
    fun setMapCenter(index: Int) {
        circleFenceInfoList?.get(index)?.let {
            mBaiduMapManager.setMapCenter(it.latLng)
        }
    }

    fun getMyLat() = mCurrentLat

    fun getMyLng() = mCurrentLng

    /**
     * 设置自己的位置的图标视图
     */
    fun setMyLocationIconView(view: View) {
        mBaiduMapManager.setMyLocationIconView(view)
    }

    fun onPause() {
        mBaiduMapManager.pause()
    }

    fun onResume() {
        mBaiduMapManager.resume()
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

        mMarkerManager.clearMarkerInfo()

        mBaiduMapManager.destroy()
    }

}
