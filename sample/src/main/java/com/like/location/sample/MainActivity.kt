package com.like.location.sample

import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.like.location.LocationUtils
import com.like.location.MyLocationListener
import com.like.location.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val mBinding: ActivityMainBinding by lazy { DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main) }
    private val mLocationUtils: LocationUtils by lazy {
        LocationUtils(this, object : MyLocationListener() {

        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding
    }

    fun startLocation(view: View) {
        mLocationUtils.start()
    }

    fun stopLocation(view: View) {
        mLocationUtils.stop()
    }

    fun trace(view: View) {
        startActivity(Intent(this, TracingActivity::class.java))
    }
}
