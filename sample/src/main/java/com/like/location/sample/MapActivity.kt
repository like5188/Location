package com.like.location.sample

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.map.*
import com.baidu.mapapi.map.InfoWindow.OnInfoWindowClickListener
import com.baidu.mapapi.model.LatLng
import com.like.location.BaiduMapUtils
import com.like.location.LocationUtils
import com.like.location.MarkerUtils
import com.like.location.sample.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity() {
    private val mBinding: ActivityMapBinding by lazy {
        DataBindingUtil.setContentView<ActivityMapBinding>(this, R.layout.activity_map)
    }
    private val mBaiduMapUtils: BaiduMapUtils by lazy {
        BaiduMapUtils(mBinding.mapView)
    }
    private val mMarkerUtils: MarkerUtils by lazy {
        MarkerUtils(mBinding.mapView.map)
    }
    private val mLocationUtils: LocationUtils by lazy {
        LocationUtils(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding
        mMarkerUtils.setOnMarkerClickListener(BaiduMap.OnMarkerClickListener { marker ->
            val button = Button(applicationContext)
            button.text = "更改位置"
            button.setTextColor(Color.BLACK)
            button.width = 300
            // InfoWindow点击事件监听接口
            val listener = OnInfoWindowClickListener {
                val latLng = marker.position
                val latLngNew = LatLng(latLng.latitude + 0.005, latLng.longitude + 0.005)
                marker.position = latLngNew
                // 隐藏地图上的所有InfoWindow
                mBaiduMapUtils.hideInfoWindow()
            }
            val infoWindow = InfoWindow(BitmapDescriptorFactory.fromView(button), marker.position, -150, listener)
            mBaiduMapUtils.showInfoWindow(infoWindow)
            true
        })
        mLocationUtils.setOnReceiveLocationListener(object : BDAbstractLocationListener() {
            override fun onReceiveLocation(location: BDLocation) {
                mLocationUtils.stop()
                // 显示自己的位置，包括方向、图标、精度圈
                mBaiduMapUtils.setMyLocationData(location)
                // 把地图移动到自己的位置
                mBaiduMapUtils.setMapCenter(LatLng(location.latitude, location.longitude))
            }
        })
        location(mBinding.mapView)
    }

    fun createMarker(view: View) {
        addMarker(LatLng(29.535044, 106.492255))
    }

    private fun addMarker(latLng: LatLng): Marker? {
        if (latLng.latitude == 0.0 || latLng.longitude == 0.0) {
            return null
        }
        val bitmapA: BitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.icon_marka)
        val markerOptionsA = MarkerOptions().position(latLng).yOffset(30).icon(bitmapA).draggable(true)
        return mMarkerUtils.addMarker(markerOptionsA)
    }

    fun removeMarker(view: View) {
        mMarkerUtils.clear()
        mBaiduMapUtils.hideInfoWindow()
    }

    fun location(view: View) {
        val locationOption = LocationClientOption()
        // 设置定位场景，根据定位场景快速生成对应的定位参数  以出行场景为例
        // 1）签到场景：只进行一次定位返回最接近真实位置的定位结果
        // 2）运动场景：高精度连续定位，适用于运动类开发者场景
        // 3）出行场景：高精度连续定位，适用于运动类开发者场景
        locationOption.setLocationPurpose(LocationClientOption.BDLocationPurpose.SignIn)
        mLocationUtils.setLocationClientOption(locationOption)
        mLocationUtils.start()
    }

    override fun onResume() {
        super.onResume()
        mBaiduMapUtils.onResume()
    }

    override fun onPause() {
        super.onPause()
        mBaiduMapUtils.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mLocationUtils.onDestroy()
        mMarkerUtils.onDestroy()
        mBaiduMapUtils.onDestroy()
    }
}
