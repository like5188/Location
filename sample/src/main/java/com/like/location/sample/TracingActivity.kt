package com.like.location.sample

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.like.location.SharedLocationUtils
import com.like.location.sample.databinding.ActivityTracingBinding

class TracingActivity : AppCompatActivity() {
    companion object {
        const val SERVICE_ID = 164746L
    }

    private val mBinding: ActivityTracingBinding by lazy { DataBindingUtil.setContentView<ActivityTracingBinding>(this, R.layout.activity_tracing) }
    private val mSharedLocationUtils: SharedLocationUtils by lazy {
        SharedLocationUtils(mBinding.tracingMapView,
                SERVICE_ID,
                listOf(SharedLocationUtils.MarkerInfo("like1", "http://imga.5054399.com/upload_pic/2016/8/19/4399_15460229024.jpg")))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSharedLocationUtils.start()
    }

    override fun onPause() {
        mSharedLocationUtils.onPause()
        super.onPause()
    }

    override fun onResume() {
        mSharedLocationUtils.onResume()
        super.onResume()
    }

    override fun onStop() {
        mSharedLocationUtils.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        mSharedLocationUtils.onDestroy()
        super.onDestroy()
    }

}