package com.like.location

import android.app.Notification
import android.content.Context
import android.location.LocationManager
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.baidu.location.*
import com.like.location.util.PermissionUtils

/**
 * [LocationClient]实现的定位工具类。
 */
class LocationUtils {
    companion object {
        private const val TAG = "LocationUtils"
    }

    private var mContext: Context? = null
    // 定位服务的客户端。宿主程序在客户端声明此类，并调用，目前只支持在主线程中启动
    private val mLocationClient: LocationClient by lazy {
        mContext ?: throw UnsupportedOperationException("you must call init() first")
        LocationClient(mContext).apply { locOption = getDefaultLocationClientOption() }
    }
    private var mLocation: BDLocation? = null
    // 位置到达监听。定位SDK支持位置提醒功能，位置提醒最多提醒3次，3次过后将不再提醒。假如需要再次提醒、或者要修改提醒点坐标，都可通过函数SetNotifyLocation()来实现。
    private var mBDNotifyListener1: BDNotifyListener? = null
    private val mBDNotifyListener: BDNotifyListener = object : BDNotifyListener() {
        /**
         * 位置提醒功能，可供地理围栏需求比较小的开发者使用
         * 位置提醒回调函数
         * @param location 位置坐标
         * @param distance 当前位置跟设定提醒点的距离
         */
        override fun onNotify(location: BDLocation, distance: Float) {
            super.onNotify(location, distance)
            Log.d(TAG, "onNotify distance=$distance")
            mBDNotifyListener1?.onNotify(location, distance)
        }
    }
    // 接收到位置信息监听
    private var mBDAbstractLocationListener1: BDAbstractLocationListener? = null
    private val mBDAbstractLocationListener: BDAbstractLocationListener = object : BDAbstractLocationListener() {
        /**
         * 定位请求回调函数
         *
         *61 ： GPS定位结果，GPS定位成功。
         *62 ： 无法获取有效定位依据，定位失败，请检查运营商网络或者wifi网络是否正常开启，尝试重新请求定位。
         *63 ： 网络异常，没有成功向服务器发起请求，请确认当前测试手机网络是否通畅，尝试重新请求定位。
         *65 ： 定位缓存的结果。
         *66 ： 离线定位结果。通过requestOfflineLocaiton调用时对应的返回结果。
         *67 ： 离线定位失败。通过requestOfflineLocaiton调用时对应的返回结果。
         *68 ： 网络连接失败时，查找本地离线定位时对应的返回结果。
         *161： 网络定位结果，网络定位定位成功。
         *162： 请求串密文解析失败。
         *167： 服务端定位失败，请您检查是否禁用获取位置信息权限，尝试重新请求定位。
         *502： key参数错误，请按照说明文档重新申请KEY。
         *505： key不存在或者非法，请按照说明文档重新申请KEY。
         *601： key服务被开发者自己禁用，请按照说明文档重新申请KEY。
         *602： key mcode不匹配，您的ak配置过程中安全码设置有问题，请确保：sha1正确，“;”分号是英文状态；且包名是您当前运行应用的包名，请按照说明文档重新申请KEY。
         *501～700：key验证失败，请按照说明文档重新申请KEY。
         * @param location 定位结果
         */
        override fun onReceiveLocation(location: BDLocation?) {
            //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
            //以下只列举部分获取地址相关的结果信息
            //更多结果信息获取说明，请参照类参考中BDLocation类中的说明
            printLocation(location)
            location ?: return
            mLocation = location
            mBDAbstractLocationListener1?.onReceiveLocation(location)
        }

        override fun onConnectHotSpotMessage(connectWifiMac: String?, hotSpotState: Int) {
            super.onConnectHotSpotMessage(connectWifiMac, hotSpotState)
            //在这个回调中，可获取当前设备所链接网络的类型、状态信息
            //connectWifiMac：表示连接WI-FI的MAC地址，无连接或者异常时返回NULL
            //hotSpotState有以下三种情况
            //LocationClient.CONNECT_HPT_SPOT_TRUE：连接的是移动热点
            //LocationClient.CONNECT_HPT_SPOT_FALSE：连接的非移动热点
            //LocationClient.CONNECT_HPT_SPOT_UNKNOWN：连接状态未知
            mBDAbstractLocationListener1?.onConnectHotSpotMessage(connectWifiMac, hotSpotState)
        }

        /**
         * 回调定位诊断信息，开发者可以根据相关信息解决定位遇到的一些问题
         * @param locType 当前定位类型
         * @param diagnosticType 诊断类型（1~9）
         * @param diagnosticMessage 具体的诊断信息释义
         */
        override fun onLocDiagnosticMessage(locType: Int, diagnosticType: Int, diagnosticMessage: String?) {
            super.onLocDiagnosticMessage(locType, diagnosticType, diagnosticMessage)
            printLocDiagnosticMessage(locType, diagnosticType, diagnosticMessage)
            mBDAbstractLocationListener1?.onLocDiagnosticMessage(locType, diagnosticType, diagnosticMessage)
        }
    }
    private var mPermissionUtils: PermissionUtils? = null

    fun init(fragmentActivity: FragmentActivity) {
        mContext = fragmentActivity.applicationContext
        mPermissionUtils = PermissionUtils(fragmentActivity)
        isProviderEnabled()
        init()
    }

    fun init(fragment: Fragment) {
        mContext = fragment.context?.applicationContext
        mPermissionUtils = PermissionUtils(fragment)
        isProviderEnabled()
        init()
    }

    fun init() {
        mLocationClient.registerNotify(mBDNotifyListener)
        mLocationClient.registerLocationListener(mBDAbstractLocationListener)
    }

    private fun isProviderEnabled() {
        val locManager = mContext?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        if (locManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) != true) { // 未打开位置开关，可能导致定位失败或定位不准，提示用户或做相应处理
            Toast.makeText(mContext, "未打开位置开关，可能导致定位失败或定位不准", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Android 8.0系统增加了很多新特性，对位置影响较大的特性就是增强了对后台定位的限制。
     * Android 8.0系统为实现降低功耗，无论应用的目标SDK版本为多少，Android 8.0系统都会对后台应用获取用户当前位置的频率进行限制，只允许后台应用每小时接收几次位置更新。
     * 根据Android 8.0的开发指引，为了适配这一系统特性，百度地图定位SDK新增加接口。它的实现原理就是开发者通过新接口生成一个前台服务通知，使得开发者自己的应用退到后台的时候，仍有前台通知在，规避了Android 8.0系统对后台定位的限制。
     * 注意：如果您的App在退到后台时本身就有前台服务通知，则无需按照本章节的介绍做适配。
     * 注意：本接口不一定只在Android 8.0系统上使用，在其他版本Android系统中使用也可最大程度的增加定位进程的存活率，提升后台定位效果。
     */
    fun enableLocInForeground(id: Int, notification: Notification) {
        mLocationClient.enableLocInForeground(id, notification) // 调起前台定位
    }

    /**
     * 关闭前台定位，同时移除通知栏
     */
    fun disableLocInForeground() {
        mLocationClient.disableLocInForeground(true)
    }

    /**
     * 设置定位参数
     */
    @Synchronized
    fun setLocationClientOption(option: LocationClientOption) {
        if (mLocationClient.isStarted) {
            mLocationClient.stop()
        }
        mLocationClient.locOption = option
    }

    /**
     * 位置提醒监听
     */
    fun setOnNotifyListener(latitude: Double, longitude: Double, radius: Float, listener: BDNotifyListener): LocationUtils {
        mBDNotifyListener.SetNotifyLocation(latitude, longitude, radius, mLocationClient.locOption.getCoorType())
        mBDNotifyListener1 = listener
        return this
    }

    /**
     * 接收到位置信息或者方向信息监听
     */
    fun setOnReceiveLocationListener(listener: BDAbstractLocationListener): LocationUtils {
        mBDAbstractLocationListener1 = listener
        return this
    }

    /**
     * 开发者应用如果有H5页面使用了百度JS接口，该接口可以辅助百度JS更好的进行定位
     *
     * @param webView 传入webView控件
     */
    fun enableAssistantLocation(webView: WebView) {
        mLocationClient.enableAssistantLocation(webView)
    }

    /**
     * 停止H5辅助定位
     */
    fun disableAssistantLocation() {
        mLocationClient.disableAssistantLocation()
    }

    /**
     * 重新启动定位SDK
     * 自V7.2版本起，新增LocationClient.reStart()方法，用于在某些特定的异常环境下重启定位。
     * 您可以在用户确认授予App定位权限之后，调用该接口，定位SDK将会进行重新初始化的操作
     *
     * 在Android 6.0之后，Android系统增加了动态权限授予的控制，定位权限需用户确认后，App才能拿到如基站、WIFI等信息，从而实现定位。
     * 在Android系统升级到7.0之后，我们发现，即使用户授予了App定位权限，App依然存在无法定位成功的问题。追查原因为：授予权限与初始化位置相关类之间存在时续逻辑问题，即如果先初始化如WifiManager、TelephonyManager，再请求确认定位权限，则即使用户确认可以授予App定位权限，App后续仍然拿不到基站、WIFI等信息，从而无法定位；反之，则可以在授予权限之后正常使用定位。
     * 针对这个情况，定位SDK自v7.2版本起，新增加了重启接口，LocationClient.reStart()，您可以在用户确认授予App定位权限之后，调用该接口，定位SDK将会进行重新初始化的操作，从而规避上述问题。您如果存在长时间后台定位的需求，推荐在应用回到前台的时候调用一次该接口，我们了解到有些手机系统会回收长时间后台获取用户位置的位置权限。
     */
    @Synchronized
    fun restart() {
        mPermissionUtils?.checkLocationPermissionGroup {
            mLocationClient.restart()
        }
    }

    /**
     * 启动定位SDK
     * 开发者定位场景如果是单次定位的场景，在收到定位结果之后直接调用stop()函数即可。
     */
    @Synchronized
    fun start() {
        mPermissionUtils?.checkLocationPermissionGroup {
            mLocationClient.start()
        }
    }

    /**
     * 关闭定位SDK
     */
    @Synchronized
    fun stop() {
        if (mLocationClient.isStarted) {
            mLocationClient.stop()
        }
    }

    fun onDestroy() {
        mLocationClient.removeNotifyEvent(mBDNotifyListener)
        mLocationClient.unRegisterLocationListener(mBDAbstractLocationListener)
        stop()
    }

    fun getDefaultLocationClientOption(): LocationClientOption {
        val locationOption = LocationClientOption()
        // GPS定位精度均值为10米，WIFI定位精度均值为24米，基站定位精度均值为210米。
        // 高精度定位模式：这种定位模式下，会同时使用网络定位和GPS定位，优先返回最高精度的定位结果；
        // 低功耗定位模式：这种定位模式下，不会使用GPS，只会使用网络定位（Wi-Fi和基站定位）；
        // 仅用设备定位模式：这种定位模式下，不需要连接网络，只使用GPS进行定位，这种模式下不支持室内环境的定位。
        locationOption.locationMode = LocationClientOption.LocationMode.Hight_Accuracy//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        //可选，设置返回经纬度坐标类型，默认GCJ02
        //GCJ02：国测局坐标；
        //BD09ll：百度经纬度坐标；
        //BD09：百度墨卡托坐标；
        //海外地区定位，无需设置坐标类型，统一返回WGS84类型坐标
        locationOption.setCoorType("bd09ll")//可选，默认gcj02，设置返回的定位结果坐标系。如果配合百度地图使用，建议设置为bd09ll;
        //可选，设置发起定位请求的间隔，int类型，单位ms
        //如果设置为0，则代表单次定位，即仅定位一次，默认为0
        //如果设置非0，需设置1000ms以上才有效
        locationOption.setScanSpan(3000)
        locationOption.setIsNeedAddress(true)//可选，设置是否需要地址信息，默认不需要
        locationOption.setIsNeedLocationDescribe(true)//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        locationOption.setNeedDeviceDirect(false)//可选，设置是否需要设备方向结果
        locationOption.isLocationNotify = false//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        locationOption.setIgnoreKillProcess(false)//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        locationOption.setIsNeedLocationPoiList(true)//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        locationOption.SetIgnoreCacheException(false)//可选，默认false，设置是否收集CRASH信息，默认收集
        locationOption.isOpenGps = true////可选，设置是否使用gps，默认false 使用高精度和仅用设备两种定位模式的，参数必须设置为true
        locationOption.setIsNeedAltitude(false)//可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        locationOption.setEnableSimulateGps(false)//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
//        locationOption.setOpenAutoNotifyMode()//设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者，该模式下开发者无需再关心定位间隔是多少，定位SDK本身发现位置变化就会及时回调给开发者
//        locationOption.setWifiCacheTimeOut(5 * 60 * 1000) //可选，V7.2版本新增能力 如果设置了该接口，首次启动定位时，会先判断当前Wi-Fi是否超出有效期，若超出有效期，会先重新扫描Wi-Fi，然后定位
        // 设置定位场景，根据定位场景快速生成对应的定位参数  以出行场景为例
        // 1）签到场景：只进行一次定位返回最接近真实位置的定位结果
        // 2）运动场景：高精度连续定位，适用于运动类开发者场景
        // 3）出行场景：高精度连续定位，适用于运动类开发者场景
//        locationOption.setLocationPurpose(LocationClientOption.BDLocationPurpose.Sport)
        return locationOption
    }

    private fun printLocation(location: BDLocation?) {
        if (location == null || location.locType == BDLocation.TypeServerError) {
            Log.i(TAG, "location is null or error")
            return
        }
        val sb = StringBuffer(256)
        sb.append("time : ")
        /**
         * 时间也可以使用systemClock.elapsedRealtime()方法 获取的是自从开机以来，每次回调的时间；
         * location.getTime() 是指服务端出本次结果的时间，如果位置不发生变化，则时间不变
         */
        sb.append(location.time)
        sb.append("\nlocType : ") // 定位类型
        sb.append(location.locType)
        sb.append("\nlocType description : ") // *****对应的定位类型说明*****
        sb.append(location.locTypeDescription)
        sb.append("\nlatitude : ") // 纬度
        sb.append(location.latitude)
        sb.append("\nlongtitude : ") // 经度
        sb.append(location.longitude)
        sb.append("\nradius : ") // 半径
        sb.append(location.radius)
        sb.append("\nCountryCode : ") // 国家码
        sb.append(location.countryCode)
        sb.append("\nProvince : ") // 获取省份
        sb.append(location.province)
        sb.append("\nCountry : ") // 国家名称
        sb.append(location.country)
        sb.append("\ncitycode : ") // 城市编码
        sb.append(location.cityCode)
        sb.append("\ncity : ") // 城市
        sb.append(location.city)
        sb.append("\nDistrict : ") // 区
        sb.append(location.district)
        sb.append("\nTown : ") // 获取镇信息
        sb.append(location.town)
        sb.append("\nStreet : ") // 街道
        sb.append(location.street)
        sb.append("\naddr : ") // 地址信息
        sb.append(location.addrStr)
        sb.append("\nStreetNumber : ") // 获取街道号码
        sb.append(location.streetNumber)
        sb.append("\nUserIndoorState: ") // *****返回用户室内外判断结果*****
        sb.append(location.userIndoorState)
        sb.append("\nDirection(not all devices have value): ")
        sb.append(location.direction) // 方向
        sb.append("\nlocationdescribe: ")
        sb.append(location.locationDescribe) // 位置语义化信息
        sb.append("\nPoi: ") // POI信息
        if (location.poiList != null && location.poiList.isNotEmpty()) {
            for (i in location.poiList.indices) {
                val poi = location.poiList[i] as Poi
                sb.append("poiName:")
                sb.append(poi.name + ", ")
                sb.append("poiTag:")
                sb.append(poi.tags + "\n")
            }
        }
        if (location.poiRegion != null) {
            sb.append("PoiRegion: ") // 返回定位位置相对poi的位置关系，仅在开发者设置需要POI信息时才会返回，在网络不通或无法获取时有可能返回null
            val poiRegion = location.poiRegion
            sb.append("DerectionDesc:") // 获取POIREGION的位置关系，ex:"内"
            sb.append(poiRegion.derectionDesc + "; ")
            sb.append("Name:") // 获取POIREGION的名字字符串
            sb.append(poiRegion.name + "; ")
            sb.append("Tags:") // 获取POIREGION的类型
            sb.append(poiRegion.tags + "; ")
        }
        sb.append("\nSDK版本: ")
        sb.append(mLocationClient.version) // 获取SDK版本
        when (location.locType) {
            BDLocation.TypeGpsLocation -> { // GPS定位结果
                sb.append("\nspeed : ")
                sb.append(location.speed) // 速度 单位：km/h
                sb.append("\nsatellite : ")
                sb.append(location.satelliteNumber) // 卫星数目
                sb.append("\nheight : ")
                sb.append(location.altitude) // 海拔高度 单位：米
                sb.append("\ngps status : ")
                sb.append(location.gpsAccuracyStatus) // *****gps质量判断*****
                sb.append("\ndescribe : ")
                sb.append("gps定位成功")
            }
            BDLocation.TypeNetWorkLocation -> { // 网络定位结果
                // 运营商信息
                if (location.hasAltitude()) { // *****如果有海拔高度*****
                    sb.append("\nheight : ")
                    sb.append(location.altitude) // 单位：米
                }
                sb.append("\noperationers : ") // 运营商信息
                sb.append(location.operators)
                sb.append("\ndescribe : ")
                sb.append("网络定位成功")
            }
            BDLocation.TypeOffLineLocation -> { // 离线定位结果
                sb.append("\ndescribe : ")
                sb.append("离线定位成功，离线定位结果也是有效的")
            }
            BDLocation.TypeServerError -> {
                sb.append("\ndescribe : ")
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因")
            }
            BDLocation.TypeNetWorkException -> {
                sb.append("\ndescribe : ")
                sb.append("网络不同导致定位失败，请检查网络是否通畅")
            }
            BDLocation.TypeCriteriaException -> {
                sb.append("\ndescribe : ")
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机")
            }
        }
        Log.i(TAG, sb.toString())
    }

    private fun printLocDiagnosticMessage(locType: Int, diagnosticType: Int, diagnosticMessage: String?) {
        val sb = StringBuffer(256)
        sb.append("诊断结果: ")
        if (locType == BDLocation.TypeNetWorkLocation) {
            if (diagnosticType == 1) {
                sb.append("网络定位成功，没有开启GPS，建议打开GPS会更好")
                sb.append("\n" + diagnosticMessage)
            } else if (diagnosticType == 2) {
                sb.append("网络定位成功，没有开启Wi-Fi，建议打开Wi-Fi会更好")
                sb.append("\n" + diagnosticMessage)
            }
        } else if (locType == BDLocation.TypeOffLineLocationFail) {
            if (diagnosticType == 3) {
                sb.append("定位失败，请您检查您的网络状态")
                sb.append("\n" + diagnosticMessage)
            }
        } else if (locType == BDLocation.TypeCriteriaException) {
            if (diagnosticType == 4) {
                sb.append("定位失败，无法获取任何有效定位依据")
                sb.append("\n" + diagnosticMessage)
            } else if (diagnosticType == 5) {
                sb.append("定位失败，无法获取有效定位依据，请检查运营商网络或者Wi-Fi网络是否正常开启，尝试重新请求定位")
                sb.append(diagnosticMessage)
            } else if (diagnosticType == 6) {
                sb.append("定位失败，无法获取有效定位依据，请尝试插入一张sim卡或打开Wi-Fi重试")
                sb.append("\n" + diagnosticMessage)
            } else if (diagnosticType == 7) {
                sb.append("定位失败，飞行模式下无法获取有效定位依据，请关闭飞行模式重试")
                sb.append("\n" + diagnosticMessage)
            } else if (diagnosticType == 9) {
                sb.append("定位失败，无法获取任何有效定位依据")
                sb.append("\n" + diagnosticMessage)
            }
        } else if (locType == BDLocation.TypeServerError) {
            if (diagnosticType == 8) {
                sb.append("定位失败，请确认您定位的开关打开状态，是否赋予APP定位权限")
                sb.append("\n" + diagnosticMessage)
            }
        }
        Log.v(TAG, sb.toString())
    }
}
