package com.like.location.sample

import android.app.Application
import com.baidu.mapapi.SDKInitializer

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 在使用 SDK 各组间之前初始化 context 信息，传入 ApplicationContext
        SDKInitializer.initialize(this)
    }
}
