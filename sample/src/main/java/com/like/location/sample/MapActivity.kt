package com.like.location.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.like.location.LocationUtils
import com.like.location.MarkerManager
import com.like.location.sample.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity() {
    private val mBinding: ActivityMapBinding by lazy {
        DataBindingUtil.setContentView<ActivityMapBinding>(this, R.layout.activity_map)
    }
    private val mMarkerManager: MarkerManager by lazy {
        MarkerManager(mBinding.mapView.map)
    }
    private val mCollection: MarkerManager.Collection by lazy {
        mMarkerManager.newCollection("1")
    }
    private val mBitmapA: BitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.icon_marka)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding
        mCollection.setOnMarkerClickListener {
            val infoWindow = InfoWindow(mBinding.mapView, it.position, -20)
            mBinding.mapView.map.showInfoWindow(infoWindow)
            false
        }
    }

    fun createMarker(view: View) {
        addMarker(LatLng(29.535044, 106.492255))
    }

    private fun addMarker(latLng: LatLng): Marker? {
        if (latLng.latitude == 0.0 || latLng.longitude == 0.0) {
            return null
        }
        val markerOptionsA = MarkerOptions().position(latLng).yOffset(30).icon(mBitmapA).draggable(true)
        return mCollection.addMarker(markerOptionsA)
    }

    fun removeMarker(view: View) {
        mCollection.clear()
    }

    fun location(view: View) {
        mBinding.mapView.map.isMyLocationEnabled = true
        LocationUtils.getInstance().addOnReceiveLocationListener {
            // 显示自己的位置，包括方向只是图标，精度圈
            val locationData = MyLocationData.Builder()
                    .direction(it.direction)// 此处设置开发者获取到的方向信息，顺时针0-360
                    .accuracy(it.radius)
                    .latitude(it.latitude)
                    .longitude(it.longitude)
                    .build()
            mBinding.mapView.map.setMyLocationData(locationData)
            val latLng = LatLng(it.latitude, it.longitude)
            val mapStatusBuilder = MapStatus.Builder().target(latLng).zoom(18.0f)
            val mapStatusUpdate: MapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatusBuilder.build())
            mBinding.mapView.map.animateMapStatus(mapStatusUpdate)
        }
        val locationOption = LocationClientOption()
        // 设置定位场景，根据定位场景快速生成对应的定位参数  以出行场景为例
        // 1）签到场景：只进行一次定位返回最接近真实位置的定位结果
        // 2）运动场景：高精度连续定位，适用于运动类开发者场景
        // 3）出行场景：高精度连续定位，适用于运动类开发者场景
        locationOption.setLocationPurpose(LocationClientOption.BDLocationPurpose.SignIn)
        LocationUtils.getInstance().setLocationClientOption(locationOption)
        LocationUtils.getInstance().start()
    }

    override fun onResume() {
        super.onResume()
        mBinding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mBinding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocationUtils.getInstance().destroy()
        mBitmapA.recycle()
        mCollection.clear()
        mBinding.mapView.map.isMyLocationEnabled = false
        mBinding.mapView.map.clear()// 清除所有图层
        mBinding.mapView.onDestroy()
    }
}
