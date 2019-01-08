package com.like.location.sample

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.baidu.mapapi.model.LatLng
import com.like.location.SharedLocationUtils
import com.like.location.entity.CircleFenceInfo
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
        mBinding
    }

    fun setMarkerList(view: View) {
        // 为我的所有家人添加marker
        val markerInfos = listOf(
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
                    this.latLng = LatLng(29.0, 106.0)
                    this.radius = 50
                },
                CircleFenceInfo().apply {
                    this.id = 2L
                    this.name = "fenceName2"
                    this.latLng = LatLng(29.5, 106.5)
                    this.radius = 100
                }
        )
        mSharedLocationUtils.createFences(circleFenceInfoList)
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
