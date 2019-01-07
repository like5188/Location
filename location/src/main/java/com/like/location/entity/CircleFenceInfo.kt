package com.like.location.entity

import android.graphics.Color
import android.os.Bundle
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.CircleOptions
import com.baidu.mapapi.map.Overlay
import com.baidu.mapapi.map.Stroke
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.utils.DistanceUtil

class CircleFenceInfo {
    var id = 0L// 对应fenceId
    var name = ""
    var latLng: LatLng? = null
    var radius = 0
    var fenceOverlay: Overlay? = null
    var extra: Bundle? = null

    fun clickInOverlay(latLng: LatLng?): Boolean {
        if (this.latLng == null || latLng == null) {
            return false
        }
        return DistanceUtil.getDistance(latLng, this.latLng) <= radius
    }

    fun destroy() {
        fenceOverlay?.remove()
    }

    /**
     * 创建围栏的覆盖物，一个蓝色圆圈
     */
    fun createOverlay(baiduMap: BaiduMap) {
        if (fenceOverlay == null) {
            val options = CircleOptions().fillColor(0x6600A7FF)
                    .stroke(Stroke(1, Color.rgb(0x00, 0xA7, 0xFF)))
                    .center(latLng)
                    .radius(radius)
            fenceOverlay = baiduMap.addOverlay(options)
        }
    }

    override fun toString(): String {
        return "CircleFenceInfo(id=$id, name='$name', latLng=$latLng, radius=$radius, fenceOverlay=$fenceOverlay, extra=$extra)"
    }

}
