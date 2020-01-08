package com.like.location

import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.BaiduMap.OnMarkerClickListener
import com.baidu.mapapi.map.BaiduMap.OnMarkerDragListener
import com.baidu.mapapi.map.Marker
import com.baidu.mapapi.map.MarkerOptions

/**
 * [Marker]管理工具类
 */
class MarkerUtils(private val mBaiduMap: BaiduMap) {
    private val mMarkers = mutableSetOf<Marker>()
    private var mMarkerClickListener1: OnMarkerClickListener? = null
    private val mMarkerClickListener: OnMarkerClickListener = object : OnMarkerClickListener {
        override fun onMarkerClick(marker: Marker?): Boolean {
            marker ?: return false
            return mMarkerClickListener1?.onMarkerClick(marker) ?: false
        }
    }
    private var mMarkerDragListener1: OnMarkerDragListener? = null
    private val mMarkerDragListener: OnMarkerDragListener = object : OnMarkerDragListener {
        override fun onMarkerDragEnd(marker: Marker?) {
            marker ?: return
            mMarkerDragListener1?.onMarkerDragEnd(marker)
        }

        override fun onMarkerDragStart(marker: Marker?) {
            marker ?: return
            mMarkerDragListener1?.onMarkerDragStart(marker)
        }

        override fun onMarkerDrag(marker: Marker?) {
            marker ?: return
            mMarkerDragListener1?.onMarkerDrag(marker)
        }

    }

    init {
        mBaiduMap.setOnMarkerClickListener(mMarkerClickListener)
        mBaiduMap.setOnMarkerDragListener(mMarkerDragListener)
    }

    fun addMarker(opts: MarkerOptions): Marker {
        val marker = mBaiduMap.addOverlay(opts) as Marker
        mMarkers.add(marker)
        return marker
    }

    fun remove(marker: Marker): Boolean {
        if (mMarkers.remove(marker)) {
            marker.icon.recycle()
            marker.remove()
            return true
        }
        return false
    }

    fun clear() {
        for (marker in mMarkers) {
            marker.icon.recycle()
            marker.remove()
        }
        mMarkers.clear()
    }

    fun getMarkers() = mMarkers

    fun setOnMarkerClickListener(listener: OnMarkerClickListener) {
        mMarkerClickListener1 = listener
    }

    fun setOnMarkerDragListener(listener: OnMarkerDragListener) {
        mMarkerDragListener1 = listener
    }

    fun onDestroy() {
        mBaiduMap.removeMarkerClickListener(mMarkerClickListener)
        mBaiduMap.setOnMarkerDragListener(null)
        clear()
    }

}