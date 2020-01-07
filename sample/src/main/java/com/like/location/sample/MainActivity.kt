package com.like.location.sample

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
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

    fun start(view: View) {
        LocationUtils.getInstance().addOnReceiveLocationListener {

        }
        LocationUtils.getInstance().addOnNotifyListener(29.535044, 106.492255, 40.0f) { location, distance ->

        }
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
