package com.like.location.entity

import android.graphics.Color
import android.os.Bundle
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.CircleOptions
import com.baidu.mapapi.map.Overlay
import com.baidu.mapapi.map.Stroke
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.utils.DistanceUtil

/**
 * 圆形围栏信息
 */
class CircleFenceInfo {
    /**
     * 围栏id
     */
    var id = 0L// 对应fenceId
    /**
     * 围栏名字
     */
    var name = ""
    /**
     * 围栏中心经纬度
     */
    var latLng: LatLng? = null
    /**
     * 围栏半径
     */
    var radius = 0
    /**
     * 围栏覆盖物
     */
    var fenceOverlay: Overlay? = null
    var extra: Bundle? = null

    /**
     * 是否点击在围栏覆盖物范围内
     */
    fun isClickedInOverlay(latLng: LatLng?): Boolean {
        if (this.latLng == null || latLng == null) {
            return false
        }
        return DistanceUtil.getDistance(latLng, this.latLng) <= radius
    }

    /**
     * 销毁围栏覆盖物
     */
    fun destroy() {
        fenceOverlay?.remove()
    }

    /**
     * 创建一个围栏覆盖物
     */
    fun createOverlay(baiduMap: BaiduMap, bgColor: Int = 0x6600A7FF, strokeWidth: Int = 1, strokeColor: Int = Color.rgb(0x00, 0xA7, 0xFF)) {
        if (fenceOverlay == null) {
            fenceOverlay = baiduMap.addOverlay(
                    CircleOptions().fillColor(bgColor)
                            .stroke(Stroke(strokeWidth, strokeColor))
                            .center(latLng)
                            .radius(radius)
            )
        }
    }

    override fun toString(): String {
        return "CircleFenceInfo(id=$id, name='$name', latLng=$latLng, radius=$radius, fenceOverlay=$fenceOverlay, extra=$extra)"
    }

}
