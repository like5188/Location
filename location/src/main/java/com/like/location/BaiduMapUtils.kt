package com.like.location

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.ZoomControls
import com.baidu.location.BDLocation
import com.baidu.mapapi.CoordType
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng

/**
 * [MapView]、[BaiduMap]管理工具类
 */
class BaiduMapUtils(private val mMapView: MapView) {
    companion object {
        /**
         * 初始化百度地图，在Application中调用
         */
        fun init(context: Context) {
            // 在使用 SDK 各组间之前初始化
            // 默认本地个性化地图初始化方法
            SDKInitializer.initialize(context.applicationContext)

            //自4.3.0起，百度地图SDK所有接口均支持百度坐标和国测局坐标，用此方法设置您使用的坐标类型.
            //包括BD09LL和GCJ02两种坐标，默认是BD09LL坐标。
            SDKInitializer.setCoordType(CoordType.BD09LL)
        }
    }

    init {
        // 隐藏百度地图的logo
        val child = mMapView.getChildAt(1)
        if (child != null && (child is ImageView || child is ZoomControls)) {
            child.visibility = View.GONE
        }
        mMapView.showScaleControl(false)// 隐藏地图上的比例尺
        mMapView.showZoomControls(false)// 隐藏地图上的缩放控件
    }

    fun getMapView(): MapView = mMapView

    fun getBaiduMap(): BaiduMap = mMapView.map

    /**
     * 自定义自己位置的图标
     *
     * @param customMarker  如果为null，则恢复默认图标
     */
    fun setMyLocationConfiguration(
            locationMode: MyLocationConfiguration.LocationMode = MyLocationConfiguration.LocationMode.NORMAL,
            enableDirection: Boolean = true,
            customMarker: BitmapDescriptor? = null,
            accuracyCircleFillColor: Int = 0x00ffffff,
            accuracyCircleStrokeColor: Int = 0x00ffffff
    ) {
        mMapView.map.setMyLocationConfiguration(MyLocationConfiguration(locationMode, enableDirection, customMarker, accuracyCircleFillColor, accuracyCircleStrokeColor))
        customMarker?.recycle()
    }

    /**
     * 设置自己的位置
     */
    fun setMyLocationData(location: BDLocation) {
        // 显示自己的位置，包括方向只是图标，精度圈
        val locationData = MyLocationData.Builder()
                .direction(location.direction)// 此处设置开发者获取到的方向信息，顺时针0-360
                .accuracy(location.radius)
                .latitude(location.latitude)
                .longitude(location.longitude)
                .build()
        setMyLocationData(locationData)
    }

    fun setMyLocationData(myLocationData: MyLocationData) {
        // 开启定位图层
        if (!mMapView.map.isMyLocationEnabled) {
            mMapView.map.isMyLocationEnabled = true
        }
        mMapView.map.setMyLocationData(myLocationData)
    }

    /**
     * 设置指定经纬度为地图中心
     */
    fun setMapCenter(latLng: LatLng) {
        val mapStatusBuilder = MapStatus.Builder().target(latLng).zoom(18.0f).overlook(0f)
        val mapStatusUpdate: MapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatusBuilder.build())
        mMapView.map.animateMapStatus(mapStatusUpdate)
    }

    fun showInfoWindow(infoWindow: InfoWindow) {
        mMapView.map.showInfoWindow(infoWindow)
    }

    fun hideInfoWindow(infoWindow: InfoWindow) {
        mMapView.map.hideInfoWindow(infoWindow)
    }

    fun onPause() {
        mMapView.onPause()
    }

    fun onResume() {
        mMapView.onResume()
    }

    fun onDestroy() {
        if (mMapView.map.isMyLocationEnabled) {
            mMapView.map.isMyLocationEnabled = false
        }
        mMapView.map.clear()// 清除地图上所有覆盖物。包括Overlay、Masker、InfoWindow等等
        mMapView.onDestroy() // 关闭定位图层
    }
}