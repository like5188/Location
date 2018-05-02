package com.like.location

import android.content.Context
import com.baidu.trace.LBSTraceClient
import com.baidu.trace.Trace
import com.baidu.trace.api.entity.EntityListRequest
import com.baidu.trace.api.entity.FilterCondition
import com.baidu.trace.api.entity.OnEntityListener
import com.baidu.trace.api.track.HistoryTrackRequest
import com.baidu.trace.api.track.OnTrackListener
import com.baidu.trace.model.CoordType
import com.baidu.trace.model.OnTraceListener
import com.baidu.trace.model.PushMessage


/**
 * 鹰眼轨迹管理工具类
 *
 * @param context
 * @param serviceId 轨迹服务ID
 * @param gatherInterval 定位周期(单位:秒)，5的倍数，默认为5秒
 * @param packInterval 打包回传周期(单位:秒)，5的倍数，回传周期最大不要超过定位周期的10倍，建议设置为定位周期的整数倍，默认为10秒
 */
class TraceUtils(private val context: Context, private val serviceId: Long, private val gatherInterval: Int = 5, private val packInterval: Int = 10) {
    // 设备标识
    private val entityName = "myTrace"
    // 是否需要对象存储服务，默认为：false，关闭对象存储服务。注：鹰眼 Android SDK v3.0以上版本支持随轨迹上传图像等对象数据，若需使用此功能，该参数需设为 true，且需导入bos-android-sdk-1.0.2.jar。
    private val isNeedObjectStorage = false
    // 初始化轨迹服务
    private val mTrace = Trace(serviceId, entityName, isNeedObjectStorage)
    // 初始化轨迹服务客户端
    private val mTraceClient = LBSTraceClient(context.applicationContext)
    // 初始化轨迹服务监听器
    private var mTraceListener: OnTraceListener = object : OnTraceListener {
        override fun onBindServiceCallback(p0: Int, p1: String?) {}

        override fun onInitBOSCallback(p0: Int, p1: String?) {}

        // 开启服务回调
        override fun onStartTraceCallback(status: Int, message: String) {
            if (status == 0) {// 开启服务成功后
                startGather()
            }
        }

        // 停止服务回调
        override fun onStopTraceCallback(status: Int, message: String) {}

        // 开启采集回调
        override fun onStartGatherCallback(status: Int, message: String) {}

        // 停止采集回调
        override fun onStopGatherCallback(status: Int, message: String) {}

        // 推送回调
        override fun onPushCallback(messageNo: Byte, message: PushMessage) {}
    }

    init {
        // 设置定位和打包周期
        mTraceClient.setInterval(gatherInterval, packInterval)
    }

    /**
     * 开启鹰眼服务，启动鹰眼 service
     */
    fun startTrace() {
        mTraceClient.startTrace(mTrace, mTraceListener)
    }

    /**
     * 开启轨迹采集，启动轨迹追踪。至此，正式开启轨迹追踪。
     * 注意：因为startTrace与startGather是异步执行，且startGather依赖startTrace执行开启服务成功，所以建议startGather在public void onStartTraceCallback(int errorNo, String message)回调返回错误码为0后，再进行调用执行，否则会出现服务开启失败12002的错误。
     */
    fun startGather() {
        mTraceClient.startGather(mTraceListener)
    }

    /**
     * 停止轨迹服务：此方法将同时停止轨迹服务和轨迹采集，完全结束鹰眼轨迹服务。若需再次启动轨迹追踪，需重新启动服务和轨迹采集
     */
    fun stopTrace() {
        mTraceClient.stopTrace(mTrace, mTraceListener)
    }

    /**
     * 停止轨迹采集：此方法将停止轨迹采集，但不停止轨迹服务（即，不再采集轨迹点了，但鹰眼 service 还存活）。若需再次启动轨迹追踪，直接调用mTraceClient.startGather()方法开启轨迹采集即可，无需再次启动轨迹服务。此方式可应用于频繁中断轨迹追踪的场景，可避免频繁启动服务。
     */
    fun stopGather() {
        mTraceClient.stopGather(mTraceListener)
    }

    /**
     * @param tag 请求标识
     * @param startTime 开始时间戳(单位：秒)，默认为当前时间以前12小时
     * @param endTime 结束时间戳(单位：秒)，默认为当前时间
     * @param listener 轨迹监听器
     */
    fun queryHistoryTrack(tag: Int = 1, startTime: Long = System.currentTimeMillis() / 1000 - 12 * 60 * 60, endTime: Long = System.currentTimeMillis() / 1000, listener: OnTrackListener) {
        // 创建历史轨迹请求实例
        val historyTrackRequest = HistoryTrackRequest(tag, serviceId, entityName)

        //设置轨迹查询起止时间
        // 设置开始时间
        historyTrackRequest.startTime = startTime
        // 设置结束时间
        historyTrackRequest.endTime = endTime

        // 查询历史轨迹
        mTraceClient.queryHistoryTrack(historyTrackRequest, listener)
    }

    fun setInterval(gatherInterval: Int, packInterval: Int) {
        mTraceClient.setInterval(gatherInterval, packInterval)
    }

    fun queryEntityList(tag: Int = 1, entityNames: List<String>? = null, listener: OnEntityListener) {
        // 过滤条件
        val filterCondition = FilterCondition()
        filterCondition.entityNames = entityNames
        // 返回结果坐标类型
        val coordTypeOutput = CoordType.bd09ll
        // 分页索引
        val pageIndex = 1
        // 分页大小
        val pageSize = 100

        // 创建Entity列表请求实例
        val request = EntityListRequest(tag, serviceId, filterCondition, coordTypeOutput, pageIndex, pageSize)

        // 查询Entity列表
        mTraceClient.queryEntityList(request, listener)
    }

}
