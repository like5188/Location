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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding
        LocationUtils.getInstance().init(this)
    }

    fun getLocation(view: View) {
        LocationUtils.getInstance().addOnReceiveLocationListener {

        }
        val locationOption = LocationClientOption()
        // 设置定位场景，根据定位场景快速生成对应的定位参数  以出行场景为例
        // 1）签到场景：只进行一次定位返回最接近真实位置的定位结果
        // 2）运动场景：高精度连续定位，适用于运动类开发者场景
        // 3）出行场景：高精度连续定位，适用于运动类开发者场景
        locationOption.setLocationPurpose(LocationClientOption.BDLocationPurpose.SignIn)
        LocationUtils.getInstance().setLocationClientOption(locationOption)
        LocationUtils.getInstance().start()
    }

    fun start(view: View) {
        LocationUtils.getInstance().addOnReceiveLocationListener {

        }
        LocationUtils.getInstance().addOnNotifyListener(29.535044, 106.492255, 40.0f) { location, distance ->

        }
        LocationUtils.getInstance().setLocationClientOption(LocationUtils.getInstance().getDefaultLocationClientOption())
        LocationUtils.getInstance().start()
    }

    fun stop(view: View) {
        LocationUtils.getInstance().removeOnReceiveLocationListener()
        LocationUtils.getInstance().removeOnNotifyListener()
        LocationUtils.getInstance().stop()
    }

    fun reStart(view: View) {
        LocationUtils.getInstance().addOnReceiveLocationListener {

        }
        LocationUtils.getInstance().addOnNotifyListener(29.535044, 106.492255, 40.0f) { location, distance ->

        }
        LocationUtils.getInstance().restart()
    }

    fun navigation(view: View) {
        NavigationUtils.navigation(this, 29.0, 106.0)
    }

    fun shareLocation(view: View) {
        startActivity(Intent(this, ShareLocationActivity::class.java))
    }

}
