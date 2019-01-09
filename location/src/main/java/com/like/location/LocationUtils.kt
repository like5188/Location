package com.like.location

import android.content.Context
import android.util.Log
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.like.location.listener.MyLocationListener
import com.like.location.util.SingletonHolder
import kotlin.jvm.functions.FunctionN

/**
 * 定位工具类
 */
class LocationUtils private constructor(context: Context) {
    companion object : SingletonHolder<LocationUtils>(object : FunctionN<LocationUtils> {
        override val arity: Int = 1 // number of arguments that must be passed to constructor

        override fun invoke(vararg args: Any?): LocationUtils {
            return LocationUtils(args[0] as Context)
        }
    })

    // 定位服务的客户端。宿主程序在客户端声明此类，并调用，目前只支持在主线程中启动
    private val mLocationClient: LocationClient by lazy { LocationClient(context.applicationContext) } // 声明LocationClient类

    init {
        val locationOption = LocationClientOption()
        // GPS定位精度均值为10米，WIFI定位精度均值为24米，基站定位精度均值为210米。
        // 高精度定位模式：这种定位模式下，会同时使用网络定位和GPS定位，优先返回最高精度的定位结果；
        // 低功耗定位模式：这种定位模式下，不会使用GPS，只会使用网络定位（Wi-Fi和基站定位）；
        // 仅用设备定位模式：这种定位模式下，不需要连接网络，只使用GPS进行定位，这种模式下不支持室内环境的定位。
        locationOption.locationMode = LocationClientOption.LocationMode.Hight_Accuracy//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        locationOption.setCoorType("bd09ll")//可选，默认gcj02，设置返回的定位结果坐标系。如果配合百度地图使用，建议设置为bd09ll;
        locationOption.setIsNeedAddress(true)//可选，设置是否需要地址信息，默认不需要
        locationOption.setNeedDeviceDirect(false)//可选，设置是否需要设备方向结果
        locationOption.setIsNeedAltitude(false)//可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        locationOption.isOpenGps = true//可选，默认false,设置是否使用gps
        locationOption.isLocationNotify = true//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        locationOption.setIsNeedLocationDescribe(true)//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        locationOption.setIsNeedLocationPoiList(false)//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        locationOption.setIgnoreKillProcess(false)//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        locationOption.SetIgnoreCacheException(false)//可选，默认false，设置是否收集CRASH信息，默认收集
        locationOption.setEnableSimulateGps(false)//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        locationOption.setOpenAutoNotifyMode()//设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者，该模式下开发者无需再关心定位间隔是多少，定位SDK本身发现位置变化就会及时回调给开发者
        mLocationClient.locOption = locationOption
    }

    fun addListener(listener: MyLocationListener? = null): LocationUtils {
        mLocationClient.registerLocationListener(object : BDAbstractLocationListener() {
            override fun onReceiveLocation(location: BDLocation?) {
                printLocation(location)
                listener?.onReceiveLocation(location)
            }
        })
        return this
    }

    /**
     * 重新启动定位SDK
     *
     * 您可以在用户确认授予App定位权限之后，调用该接口，定位SDK将会进行重新初始化的操作
     */
    fun restart() {
        mLocationClient.restart()
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

    private fun printLocation(location: BDLocation?) {
        if (location == null) {
            Log.i("Location", "null")
            return
        }
        val sb = StringBuffer(256)
        sb.append("time : ")
        sb.append(location.time)
        sb.append("\nerror code : ")
        sb.append(location.locType)
        sb.append("\nlatitude : ")
        sb.append(location.latitude)
        sb.append("\nlongitude : ")
        sb.append(location.longitude)
        sb.append("\nradius : ")
        sb.append(location.radius)
        // POI数据
        when (location.locType) {
            BDLocation.TypeGpsLocation -> {// GPS定位结果
                sb.append("\nspeed : ")
                sb.append(location.speed)// 单位：公里每小时
                sb.append("\nsatellite : ")
                sb.append(location.satelliteNumber)
                sb.append("\nheight : ")
                sb.append(location.altitude)// 单位：米
                sb.append("\ndirection : ")
                sb.append(location.direction)// 单位度
                sb.append("\naddr : ")
                sb.append(location.addrStr)
                sb.append("\ndescribe : ")
                sb.append("gps定位成功")

            }
            BDLocation.TypeNetWorkLocation -> {// 网络定位结果
                sb.append("\naddr : ")
                sb.append(location.addrStr)
                //运营商信息
                sb.append("\noperationers : ")
                sb.append(location.operators)
                sb.append("\ndescribe : ")
                sb.append("网络定位成功")
            }
            BDLocation.TypeOffLineLocation -> {// 离线定位结果
                sb.append("\ndescribe : ")
                sb.append("离线定位成功，离线定位结果也是有效的")
            }
            BDLocation.TypeServerError -> {
                sb.append("\ndescribe : ")
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因")
            }
            BDLocation.TypeNetWorkException -> {
                sb.append("\ndescribe : ")
                sb.append("网络不通导致定位失败，请检查网络是否通畅")
            }
            BDLocation.TypeCriteriaException -> {
                sb.append("\ndescribe : ")
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机")
            }
        }
        sb.append("\nlocationDescribe : ")
        sb.append(location.locationDescribe)
        location.poiList?.apply {
            sb.append("\npoiList size = : ")
            sb.append(size)
            for (p in this) {
                sb.append("\npoi= : ")
                sb.append(p.id + " " + p.name + " " + p.rank)
            }
        }
        Log.i("Location", sb.toString())
    }

}
