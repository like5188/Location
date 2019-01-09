package com.like.location

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * 导航工具。支持百度、高德导航
 */
object NavigationUtils {
    //map app包名
    private const val BAIDU_MAP_APP = "com.baidu.BaiduMap"
    private const val GAODE_MAP_APP = "com.autonavi.minimap"

    fun navigation(context: Context, endlatitude: Double, endlongitude: Double) {
        when {
            isAvailable(context, BAIDU_MAP_APP) -> // 启动百度地图导航
                context.startActivity(Intent().apply {
                    this.data = Uri.parse("baidumap://map/direction?destination=$endlatitude,$endlongitude")
                })
            isAvailable(context, GAODE_MAP_APP) -> // 启动高德地图导航
                context.startActivity(Intent().apply {
                    this.data = Uri.parse("amapuri://route/plan/?dlat=$endlatitude&dlon=$endlongitude&dev=0&t=0")
                })
            else -> Toast.makeText(context, "您的手机尚未安装百度地图或高德地图", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 验证各种导航地图是否安装
     */
    private fun isAvailable(context: Context, packageName: String): Boolean {
        //获取所有已安装程序的包信息
        val packageInfos = context.packageManager.getInstalledPackages(0)
        return packageInfos?.any { it.packageName == packageName } ?: false
    }

}
