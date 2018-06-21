package com.like.location

object LocationConstants {
    /**
     * 默认定位周期(单位:秒)
     */
    const val DEFAULT_GATHER_INTERVAL = 5

    /**
     * 默认打包回传周期(单位:秒)
     */
    const val DEFAULT_PACK_INTERVAL = 10

    /**
     * 实时定位间隔(单位:毫秒)
     */
    const val DEFAULT_LOCATION_INTERVAL = 3000

    /**
     * 循环查询entityList的时间间隔(单位:毫秒)
     */
    const val DEFAULT_QUERY_ENTITY_LIST_INTERVAL = 5000L

    /**
     * 点击了覆盖物
     */
    const val TAG_CLICK_MARKER = "TAG_CLICK_MARKER"
    /**
     * 点击了围栏上的覆盖物
     */
    const val TAG_CLICK_FENCE_OVERLAY = "TAG_CLICK_FENCE_OVERLAY"
    /**
     * 走进围栏
     */
    const val TAG_MOVE_IN_FENCE = "TAG_MOVE_IN_FENCE"
    /**
     * 走出围栏
     */
    const val TAG_MOVE_OUT_FENCE = "TAG_MOVE_OUT_FENCE"
}