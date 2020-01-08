package com.like.location.sample

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.baidu.location.LocationClientOption
import com.like.location.LocationUtils
import com.like.location.NavigationUtils
import com.like.location.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val mBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
    }
    private val mLocationUtils: LocationUtils by lazy {
        LocationUtils()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding
        mLocationUtils.init(this)
    }

    fun getLocation(view: View) {
        val locationOption = LocationClientOption()
        // 设置定位场景，根据定位场景快速生成对应的定位参数  以出行场景为例
        // 1）签到场景：只进行一次定位返回最接近真实位置的定位结果
        // 2）运动场景：高精度连续定位，适用于运动类开发者场景
        // 3）出行场景：高精度连续定位，适用于运动类开发者场景
        locationOption.setLocationPurpose(LocationClientOption.BDLocationPurpose.SignIn)
        mLocationUtils.setLocationClientOption(locationOption)
        mLocationUtils.start()
    }

    fun start(view: View) {
        mLocationUtils.setLocationClientOption(mLocationUtils.getDefaultLocationClientOption())
        mLocationUtils.start()
    }

    fun stop(view: View) {
        mLocationUtils.stop()
    }

    fun reStart(view: View) {
        mLocationUtils.restart()
    }

    fun navigation(view: View) {
        NavigationUtils.navigation(this, 29.0, 106.0)
    }

    fun map(view: View) {
        startActivity(Intent(this, MapActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        mLocationUtils.onDestroy()
    }
}
