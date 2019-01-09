package com.like.location.entity

import android.os.Bundle
import android.view.View
import com.baidu.mapapi.map.BitmapDescriptor
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.baidu.mapapi.map.Marker
import java.io.Serializable

/**
 * @param entityName
 * @param iconView      marker的图标视图
 * @param extraInfo     额外的数据
 */
class MarkerInfo(val entityName: String, private val iconView: View, val extraInfo: Bundle) : Serializable {
    var marker: Marker? = null
    var lat = 0.0// 经度
    var lng = 0.0// 纬度

    fun getBitmapDescriptor(): BitmapDescriptor? = BitmapDescriptorFactory.fromView(iconView)
}