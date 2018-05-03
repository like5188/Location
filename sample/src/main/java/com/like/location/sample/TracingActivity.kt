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
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

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

    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBaiduMap(mBinding.tracingMapView.map)
        mLocationUtils.start()

        mTraceUtils.startTrace()

        queryMarkersPeriodically()
    }

    private fun queryMarkersPeriodically() {
        // 延迟一段时间，然后以固定周期循环执行某一任务
        Observable.create(
                ObservableOnSubscribe<Any> {
                    disposable = Schedulers.newThread().createWorker()
                            .schedulePeriodically({
                                queryMarkers()
                            },
                                    1000,
                                    5000,
                                    TimeUnit.MILLISECONDS
                            )
                })
                .subscribeOn(Schedulers.io()) // 指定 subscribe() 发生在 scheduler 线程
                .observeOn(AndroidSchedulers.mainThread()) // 指定 Subscriber 的回调发生在主线程
                .subscribe()
    }

    private fun queryMarkers() {
        mTraceUtils.queryEntityList(listOf("like1", "like2"), object : OnEntityListener() {
            override fun onEntityListCallback(p0: EntityListResponse?) {
                p0?.entities?.forEach {
                    val lat = it.latestLocation.location.latitude
                    val lng = it.latestLocation.location.longitude
                    val iconUrl = "http://imga.5054399.com/upload_pic/2016/8/19/4399_15460229024.jpg"
                    Logger.i("queryEntityList success")
                    addMarker(mBinding.tracingMapView.map, lat, lng, iconUrl)
                }

                RxJavaUtils.timer(3000) {
                    if (mMarkers.isNotEmpty())
                        changeMarkerPosition(mMarkers[0], 29.592481933243, 106.52389432074)

                    RxJavaUtils.timer(3000) {
                        if (mMarkers.isNotEmpty())
                            removeMarker(mMarkers[0])
                    }

                }
            }
        })
    }

    private fun addMarker(baiduMap: BaiduMap, lat: Double, lng: Double, iconUrl: String) {
        mGlideUtils.downloadImage(iconUrl).subscribe(
                { bitmap ->
                    val overlayOptions = MarkerOptions().position(LatLng(lat, lng)).icon(BitmapDescriptorFactory.fromBitmap(bitmap)).zIndex(9).draggable(false)
                    mMarkers.add(baiduMap.addOverlay(overlayOptions) as Marker)
                },
                {
                    val overlayOptions = MarkerOptions().position(LatLng(lat, lng)).icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_marker_default)).zIndex(9).draggable(false)
                    mMarkers.add(baiduMap.addOverlay(overlayOptions) as Marker)
                }
        )
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
        disposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }

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