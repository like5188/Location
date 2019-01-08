package com.like.location.listener

import com.baidu.location.BDLocation

interface MyLocationListener {
    fun onReceiveLocation(location: BDLocation?)
}