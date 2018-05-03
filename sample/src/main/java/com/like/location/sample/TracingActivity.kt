package com.like.location.sample

import android.content.Context
import android.databinding.DataBindingUtil
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.baidu.location.BDLocation
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.trace.api.entity.EntityListResponse
import com.baidu.trace.api.entity.OnEntityListener
import com.like.common.util.GlideUtils
import com.like.common.util.RxJavaUtils
import com.like.location.LocationUtils
import com.like.location.MyLocationListener
import com.like.location.TraceUtils
import com.like.location.sample.databinding.ActivityTracingBinding
import com.like.logger.Logger

class TracingActivity : AppCompatActivity(), SensorEventListener {
    companion object {
        const val SERVICE_ID = 164746L
    }

    private val mBinding: ActivityTracingBinding by lazy { DataBindingUtil.setContentView<ActivityTracingBinding>(this, R.layout.activity_tracing) }
    private var lastX: Double = 0.0
    private var mCurrentDirection = 0
    private var mCurrentLat = 0.0
    private var mCurrentLon = 0.0
    private var mCurrentAccracy = 0f
    private var locData: MyLocationData? = null
    private var isFirstLoc = true // 是否首次定位
    private val mLocationUtils: LocationUtils by lazy {
        LocationUtils(this, object : MyLocationListener() {
            override fun onReceiveLocation(location: BDLocation?) {
                super.onReceiveLocation(location)
                // map view 销毁后不在处理新接收的位置
                if (location == null || mBinding.tracingMapView == null || mBinding.tracingMapView.map == null) {
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
                mBinding.tracingMapView.map.setMyLocationData(locData)
                if (isFirstLoc) {
                    isFirstLoc = false
                    val ll = LatLng(location.latitude, location.longitude)
                    val builder = MapStatus.Builder()
                    builder.target(ll).zoom(18.0f)
                    mBinding.tracingMapView.map.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
                }
            }
        })
    }

    private val mSensorManager: SensorManager by lazy { getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    private val mMarkers = mutableListOf<Marker>()

    private val mTraceUtils: TraceUtils by lazy { TraceUtils(this, SERVICE_ID) }

    private val mGlideUtils: GlideUtils by lazy { GlideUtils(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBaiduMap(mBinding.tracingMapView.map)
        initMarker(mBinding.tracingMapView.map)
        mLocationUtils.start()

        mTraceUtils.startTrace()
        RxJavaUtils.timer(15000) {
            mTraceUtils.queryEntityList(listOf("like1", "like2", "like3"), object : OnEntityListener() {
                override fun onEntityListCallback(p0: EntityListResponse?) {
                    Logger.e(p0)
                }
            })
        }
    }

    private fun initMarker(baiduMap: BaiduMap) {
        addMarker(baiduMap, 29.618074, 106.510019, "http://imga.5054399.com/upload_pic/2016/8/19/4399_15460229024.jpg")
        addMarker(baiduMap, 29.618074, 106.510819, "http://imga1.5054399.com/upload_pic/2016/8/9/4399_16441724474.jpg")
        addMarker(baiduMap, 29.618374, 106.510819, "http://imga2.5054399.com/upload_pic/2018/2/13/4399_15205910344.jpg")
        addMarker(baiduMap, 29.618374, 106.510019, "http://imga1.5054399.com/upload_pic/2018/2/11/4399_16365641436.jpg")

        RxJavaUtils.timer(3000) {
            changeMarkerPosition(mMarkers[0], 29.618974, 106.510099)

            RxJavaUtils.timer(3000) {
                removeMarker(mMarkers[0])
            }
        }
    }

    private fun addMarker(baiduMap: BaiduMap, lat: Double, lng: Double, icon: String) {
        mGlideUtils.downloadImage(icon).subscribe(
                { t ->
                    val overlayOptions = MarkerOptions().position(LatLng(lat, lng)).icon(BitmapDescriptorFactory.fromBitmap(t)).zIndex(9).draggable(false)
                    mMarkers.add(baiduMap.addOverlay(overlayOptions) as Marker)
                },
                {
                    val overlayOptions = MarkerOptions().position(LatLng(lat, lng)).icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_marker_default)).zIndex(9).draggable(false)
                    mMarkers.add(baiduMap.addOverlay(overlayOptions) as Marker)
                })
    }

    private fun showInfoWindow(baiduMap: BaiduMap, view: View, marker: Marker, listener: InfoWindow.OnInfoWindowClickListener) {
        val infoWindow = InfoWindow(BitmapDescriptorFactory.fromView(view), marker.position, -47, listener)
        baiduMap.showInfoWindow(infoWindow)
    }

    private fun changeMarkerPosition(marker: Marker, lat: Double, lng: Double) {
        marker.position = LatLng(lat, lng)
    }

    private fun changeMarkerIcon(marker: Marker, bitmapDescriptor: BitmapDescriptor) {
        marker.icon = bitmapDescriptor
    }

    private fun removeMarker(marker: Marker) {
        marker.remove()
    }

    private fun initBaiduMap(baiduMap: BaiduMap) {
        setLocationMode(baiduMap, MyLocationConfiguration.LocationMode.FOLLOWING)
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

    override fun onPause() {
        mBinding.tracingMapView.onPause()
        super.onPause()
    }

    override fun onResume() {
        mBinding.tracingMapView.onResume()
        super.onResume()
        //为系统的方向传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI)
    }

    override fun onStop() {
        //取消注册传感器监听
        mSensorManager.unregisterListener(this)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mBinding.tracingMapView.map.clear()
        mMarkers.clear()

        mLocationUtils.stop()

        mTraceUtils.stopTrace()

        mBinding.tracingMapView.map.isMyLocationEnabled = false
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mBinding.tracingMapView.onDestroy() // 关闭定位图层
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
            mBinding.tracingMapView.map.setMyLocationData(locData)
        }
        lastX = x
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}