package com.like.location.sample

import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.model.LatLng
import com.like.location.SharedLocationUtils
import com.like.location.databinding.ViewMapMarkerBinding
import com.like.location.entity.CircleFenceInfo
import com.like.location.entity.MarkerInfo
import com.like.location.sample.databinding.ActivityShareLocationBinding

class ShareLocationActivity : AppCompatActivity() {
    private val mBinding: ActivityShareLocationBinding by lazy {
        DataBindingUtil.setContentView<ActivityShareLocationBinding>(this, R.layout.activity_share_location)
    }
    private val mSharedLocationUtils: SharedLocationUtils by lazy {
        SharedLocationUtils.getInstance(this)
    }
    private val mGlideUtils: GlideUtils by lazy { GlideUtils(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 在使用地图SDK各组件之前初始化context信息，传入ApplicationContext
        SDKInitializer.initialize(this.applicationContext)
        mBinding
        mSharedLocationUtils.init(mBinding.mapView, 200897, "like")
        mSharedLocationUtils.setMyLocationIconView(
                wrapMarkerView(BitmapFactory.decodeResource(resources, R.drawable.icon_marker_default))
        )
    }

    fun setMarkerList(view: View) {
        mGlideUtils.downloadImages(
                listOf(
                        "http://imga5.5054399.com/upload_pic/2019/1/5/4399_10184605542.jpg",
                        "http://imga3.5054399.com/upload_pic/2018/12/26/4399_17240206556.jpg"
                ),
                { map ->
                    Log.d("ShareLocationActivity", "图标下载成功")
                    // 为我的所有家人添加marker
                    val markerInfos = listOf(
                            MarkerInfo(
                                    "like1",
                                    wrapMarkerView(map["http://imga5.5054399.com/upload_pic/2019/1/5/4399_10184605542.jpg"]),
                                    Bundle().apply {
                                        putString("userId", "userId1")
                                        putString("name", "name1")
                                        putString("userNickName", "userNickName1")
                                        putString("phone", "13311111111")
                                    }),
                            MarkerInfo(
                                    "like2",
                                    wrapMarkerView(map["http://imga3.5054399.com/upload_pic/2018/12/26/4399_17240206556.jpg"]),
                                    Bundle().apply {
                                        putString("userId", "userId2")
                                        putString("name", "name2")
                                        putString("userNickName", "userNickName2")
                                        putString("phone", "13322222222")
                                    })
                    )
                    mSharedLocationUtils.setMarkerList(markerInfos)
                },
                {
                    Log.e("ShareLocationActivity", "图标下载失败：${it.message}")
                }
        )
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

    /**
     * 对marker图标进行了一层外圈包装
     */
    private fun wrapMarkerView(bitmap: Bitmap? = null): View {
        val binding = DataBindingUtil.inflate<ViewMapMarkerBinding>(LayoutInflater.from(this), com.like.location.R.layout.view_map_marker, null, false)
        if (bitmap != null) {
            binding.iv.setImageBitmap(bitmap)
        } else {
            binding.iv.setImageResource(R.drawable.icon_marker_default)
        }
        return binding.root
    }
}
