package com.like.location

import android.content.Context
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption

class LocationUtils(context: Context, listener: MyLocationListener?) {
    private val mLocationClient: LocationClient by lazy { LocationClient(context.applicationContext) } // 声明LocationClient类

    init {
        if (listener != null)
            mLocationClient.registerLocationListener(listener) // 注册监听函数
        initLocation()
    }

    /**
     * 启动定位SDK
     */
    fun start() {
        mLocationClient.start()
    }

    /**
     * 关闭定位SDK
     */
    fun stop() {
        mLocationClient.stop()
    }

    private fun initLocation() {
        val option = LocationClientOption()
        // GPS定位精度均值为10米，WIFI定位精度均值为24米，基站定位精度均值为210米。
        // 高精度定位模式：这种定位模式下，会同时使用网络定位和GPS定位，优先返回最高精度的定位结果；
        // 低功耗定位模式：这种定位模式下，不会使用GPS，只会使用网络定位（Wi-Fi和基站定位）；
        // 仅用设备定位模式：这种定位模式下，不需要连接网络，只使用GPS进行定位，这种模式下不支持室内环境的定位。
        option.locationMode = LocationClientOption.LocationMode.Hight_Accuracy//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll")//可选，默认gcj02，设置返回的定位结果坐标系
        option.setScanSpan(LocationConstants.DEFAULT_LOCATION_INTERVAL)//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true)//可选，设置是否需要地址信息，默认不需要
        option.setNeedDeviceDirect(false)//可选，设置是否需要设备方向结果
        option.setIsNeedAltitude(false)//可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        option.isOpenGps = true//可选，默认false,设置是否使用gps
        option.isLocationNotify = true//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true)//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(false)//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false)//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false)//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false)//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        mLocationClient.locOption = option
    }
}
