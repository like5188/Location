package com.like.location.sample

import android.Manifest
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.like.location.LocationUtils
import com.like.location.listener.MyLocationListener
import com.like.location.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val mBinding: ActivityMainBinding by lazy { DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding
        checkPermissionsAndRun(
                "hahaha",
                1,
                {
                    LocationUtils.getInstance(this).addListener()
                },
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun start(view: View) {
        LocationUtils.getInstance(this).start()
    }

    fun stop(view: View) {
        LocationUtils.getInstance(this).stop()
    }

    fun reStart(view: View) {
        LocationUtils.getInstance(this).restart()
    }

}