package com.like.location.util

import android.view.View
import android.widget.ImageView
import android.widget.ZoomControls
import com.baidu.location.BDLocation
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.like.livedatabus.LiveDataBus
import kotlin.jvm.functions.FunctionN

/**
 * 百度MapView管理工具类
 */
class BaiduMapManager private constructor() {
    companion object : SingletonHolder<BaiduMapManager>(object : FunctionN<BaiduMapManager> {
        override val arity: Int = 0 // number of arguments that must be passed to constructor

        override fun invoke(vararg args: Any?): BaiduMapManager {
            return BaiduMapManager()
        }
    })

    private lateinit var baiduMapView: MapView

    fun getBaiduMap(): BaiduMap = baiduMapView.map

    /**
     * 初始化百度地图
     */
    fun init(baiduMapView: MapView) {
        if (isInitialized()) return
        this.baiduMapView = baiduMapView
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
            LiveDataBus.post(LocationConstants.TAG_CLICK_MARKER, MarkerManager.getInstance().getMarkerInfoByMarker(it))
            true
        }
    }

    fun isInitialized() = ::baiduMapView.isInitialized

    /**
     * 设置自己的位置的图标视图
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
     * 设置自己的位置
     */
    fun setMyLocationData(locationData: MyLocationData) {
        baiduMapView.map.setMyLocationData(locationData)
    }

    fun setMyLocation(location: BDLocation) {
        // 显示自己的位置，包括方向只是图标，精度圈
        val locationData = MyLocationData.Builder()
                .direction(location.direction)// 此处设置开发者获取到的方向信息，顺时针0-360
                .accuracy(location.radius)
                .latitude(location.latitude)
                .longitude(location.longitude)
                .build()
        baiduMapView.map.setMyLocationData(locationData)
    }

    /**
     * 设置指定经纬度为地图中心
     */
    fun setMapCenter(latLng: LatLng?) {
        latLng?.let {
            val builder = MapStatus.Builder().target(latLng).zoom(18.0f).overlook(0f)
            baiduMapView.map.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
        }
    }

    fun pause() {
        baiduMapView.onPause()
    }

    fun resume() {
        baiduMapView.onResume()
    }

    fun destroy() {
        baiduMapView.map.isMyLocationEnabled = false
        baiduMapView.map.clear()// 清除地图上所有覆盖物
        baiduMapView.onDestroy() // 关闭定位图层
    }
}