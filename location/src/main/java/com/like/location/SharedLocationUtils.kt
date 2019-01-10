package com.like.location

import android.content.Context
import android.util.Log
import com.baidu.mapapi.map.MapView
import com.baidu.trace.api.entity.EntityListResponse
import com.baidu.trace.api.entity.OnEntityListener
import com.like.location.entity.MarkerInfo
import com.like.location.util.*
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

    private val mBaiduMapManager: BaiduMapManager by lazy { BaiduMapManager.getInstance() }
    private val mMyTraceUtils: MyTraceUtils by lazy { MyTraceUtils.getInstance(context) }
    private val mMarkerManager: MarkerManager by lazy { MarkerManager.getInstance() }
    private val mLocationUtils: LocationUtils by lazy { LocationUtils.getInstance(context) }
    private var serviceId: Long = 0L
    private var disposable: Disposable? = null

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
        mLocationUtils.setMapView(baiduMapView)
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

    fun onPause() {
        mBaiduMapManager.pause()
    }

    fun onResume() {
        mBaiduMapManager.resume()
        mLocationUtils.resume()
    }

    fun onStop() {
        mLocationUtils.stop()
    }

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
