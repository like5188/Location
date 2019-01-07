package com.like.location.listener

import android.util.Log
import com.baidu.location.BDLocation
import com.baidu.location.BDLocationListener

open class MyLocationListener : BDLocationListener {
    override fun onReceiveLocation(location: BDLocation?) {
        location?.let {
            //Receive Location
            val sb = StringBuffer(256)
            sb.append("time : ")
            sb.append(it.time)
            sb.append("\nerror code : ")
            sb.append(it.locType)
            sb.append("\nlatitude : ")
            sb.append(it.latitude)
            sb.append("\nlontitude : ")
            sb.append(it.longitude)
            sb.append("\nradius : ")
            sb.append(it.radius)
            if (it.locType == BDLocation.TypeGpsLocation) {// GPS定位结果
                sb.append("\nspeed : ")
                sb.append(it.speed)// 单位：公里每小时
                sb.append("\nsatellite : ")
                sb.append(it.satelliteNumber)
                sb.append("\nheight : ")
                sb.append(it.altitude)// 单位：米
                sb.append("\ndirection : ")
                sb.append(it.direction)// 单位度
                sb.append("\naddr : ")
                sb.append(it.addrStr)
                sb.append("\ndescribe : ")
                sb.append("gps定位成功")

            } else if (it.locType == BDLocation.TypeNetWorkLocation) {// 网络定位结果
                sb.append("\naddr : ")
                sb.append(it.addrStr)
                //运营商信息
                sb.append("\noperationers : ")
                sb.append(it.operators)
                sb.append("\ndescribe : ")
                sb.append("网络定位成功")
            } else if (it.locType == BDLocation.TypeOffLineLocation) {// 离线定位结果
                sb.append("\ndescribe : ")
                sb.append("离线定位成功，离线定位结果也是有效的")
            } else if (it.locType == BDLocation.TypeServerError) {
                sb.append("\ndescribe : ")
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因")
            } else if (it.locType == BDLocation.TypeNetWorkException) {
                sb.append("\ndescribe : ")
                sb.append("网络不通导致定位失败，请检查网络是否通畅")
            } else if (it.locType == BDLocation.TypeCriteriaException) {
                sb.append("\ndescribe : ")
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机")
            }
            sb.append("\nlocationdescribe : ")
            sb.append(it.locationDescribe)// 位置语义化信息
            val list = it.poiList// POI数据
            if (list != null) {
                sb.append("\npoilist size = : ")
                sb.append(list.size)
                for (p in list) {
                    sb.append("\npoi= : ")
                    sb.append(p.id + " " + p.name + " " + p.rank)
                }
            }
            Log.i("Location", sb.toString())
        }
    }

}