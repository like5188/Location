package com.like.location.sample

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.model.LatLng
import com.like.location.SharedLocationUtils
import com.like.location.entity.CircleFenceInfo
import com.like.location.sample.databinding.ActivityMainBinding
import com.like.location.sample.databinding.ActivityShareLocationBinding

class ShareLocationActivity : AppCompatActivity() {
    private val mBinding: ActivityShareLocationBinding by lazy {
        DataBindingUtil.setContentView<ActivityShareLocationBinding>(this, R.layout.activity_share_location)
    }
    private val mSharedLocationUtils: SharedLocationUtils by lazy {
        SharedLocationUtils(mBinding.mapView, 200897, "like")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 在使用地图SDK各组件之前初始化context信息，传入ApplicationContext
        SDKInitializer.initialize(this.applicationContext)
        mBinding
    }

    fun setMarkerList(view: View) {
        // 为我的所有家人添加marker
        val markerInfos = listOf(
//                SharedLocationUtils.MarkerInfo(
//                        "like",
//                        "http://imga5.5054399.com/upload_pic/2019/1/5/4399_10184605542.jpg",
//                        "userId",
//                        "name",
//                        "userNickName",
//                        "13300000000"
//                ),
                SharedLocationUtils.MarkerInfo(
                        "like1",
                        "http://imga5.5054399.com/upload_pic/2019/1/5/4399_10184605542.jpg",
                        "userId1",
                        "name1",
                        "userNickName1",
                        "13311111111"
                ),
                SharedLocationUtils.MarkerInfo(
                        "like2",
                        "http://imga3.5054399.com/upload_pic/2018/12/26/4399_17240206556.jpg",
                        "userId2",
                        "name2",
                        "userNickName2",
                        "13322222222"
                )
        )
        mSharedLocationUtils.setMarkerList(markerInfos)
    }

    fun createFences(view: View) {
        // 为我的所有小区添加围栏
        val circleFenceInfoList = listOf(
                CircleFenceInfo().apply {
                    this.id = 1L
                    this.name = "fenceName1"
                    this.latLng = LatLng(29.533913, 106.493686)
                    this.radius = 50
                },
                CircleFenceInfo().apply {
                    this.id = 2L
                    this.name = "fenceName2"
                    this.latLng = LatLng(29.533993, 106.493886)
                    this.radius = 100
                }
        )
        mSharedLocationUtils.createFences(circleFenceInfoList)
    }

    fun locationFences(view: View) {
        mSharedLocationUtils.setMapCenter(0)
    }

    override fun onPause() {
        super.onPause()
        mSharedLocationUtils.onPause()
    }

    override fun onResume() {
        super.onResume()
        mSharedLocationUtils.onResume()
    }

    override fun onStop() {
        super.onStop()
        mSharedLocationUtils.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mSharedLocationUtils.onDestroy()
    }

}
