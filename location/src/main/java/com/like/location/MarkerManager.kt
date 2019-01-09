package com.like.location

import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.Marker
import com.baidu.mapapi.map.MarkerOptions
import com.baidu.mapapi.model.LatLng
import com.like.location.entity.MarkerInfo
import com.like.location.util.SingletonHolder
import kotlin.jvm.functions.FunctionN

/**
 * Marker管理工具类
 */
class MarkerManager private constructor(){
    companion object : SingletonHolder<MarkerManager>(object : FunctionN<MarkerManager> {
        override val arity: Int = 0 // number of arguments that must be passed to constructor

        override fun invoke(vararg args: Any?): MarkerManager {
            return MarkerManager()
        }
    })

    private val markerInfos = mutableListOf<MarkerInfo>()// 需要显示在地图上的marker，不包括自己

    fun addMarkerList(markerInfos: List<MarkerInfo>) {
        this.markerInfos.addAll(markerInfos)
    }

    fun createMarker(baiduMap: BaiduMap, entityName: String) {
        getMarkerInfoByEntityName(entityName)?.apply {
            val overlayOptions = MarkerOptions()
                    .position(LatLng(lat, lng))
                    .zIndex(9)
                    .draggable(false)
            getBitmapDescriptor()?.let { bitmapDescriptor ->
                overlayOptions.icon(bitmapDescriptor)
            }
            marker = baiduMap.addOverlay(overlayOptions) as Marker
        }
    }

    fun changeMarkerPosition(entityName: String) {
        getMarkerInfoByEntityName(entityName)?.apply {
            this.marker?.let {
                it.position = LatLng(lat, lng)
            }
        }
    }

    fun getEntityNames() = markerInfos.map { it.entityName }

    fun getMarkerInfos() = markerInfos

    fun getMarkerInfoByMarker(marker: Marker): MarkerInfo? {
        val filter = markerInfos.filter { it.marker == marker }
        return if (filter.isNotEmpty()) filter[0] else null
    }

    fun getMarkerInfoByEntityName(entityName: String): MarkerInfo? {
        val filter = markerInfos.filter { it.entityName == entityName }
        return if (filter.isNotEmpty()) filter[0] else null
    }

    fun removeMarkerInfo(entityName: String) {
        val listIterator = markerInfos.listIterator()
        run breaking@{
            listIterator.forEach continuing@{ markerInfo ->
                if (markerInfo.entityName == entityName) {
                    markerInfo.marker?.remove()
                    listIterator.remove()
                    return@breaking
                }
            }
        }
    }

    fun clearMarkerInfo() {
        if (markerInfos.isNotEmpty()) {
            markerInfos.forEach {
                it.marker?.remove()
            }
            markerInfos.clear()
        }
    }
}