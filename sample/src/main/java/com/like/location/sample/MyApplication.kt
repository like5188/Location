package com.like.location.sample

import android.app.Application
import com.like.location.BaiduMapUtils

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BaiduMapUtils.init(this)
    }
}