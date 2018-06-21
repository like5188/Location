package com.like.location

import android.content.Context
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.MapPoi
import com.baidu.mapapi.model.LatLng
import com.baidu.trace.LBSTraceClient
import com.baidu.trace.Trace
import com.baidu.trace.api.entity.EntityListRequest
import com.baidu.trace.api.entity.FilterCondition
import com.baidu.trace.api.entity.LocRequest
import com.baidu.trace.api.entity.OnEntityListener
import com.baidu.trace.api.fence.*
import com.baidu.trace.api.track.HistoryTrackRequest
import com.baidu.trace.api.track.LatestPointRequest
import com.baidu.trace.api.track.OnTrackListener
import com.baidu.trace.model.*
import com.like.common.util.NetWorkUtils
import com.like.common.util.SPUtils
import com.like.logger.Logger
import com.like.rxbus.RxBus
import com.like.toast.longToastCenter
import com.like.toast.shortToastCenter
import java.util.concurrent.atomic.AtomicInteger


/**
 * 鹰眼轨迹管理工具类
 * 注意：地图只支持Android v4.0以上系统
 *
 * @param context
 * @param baiduMap
 * @param serviceId 轨迹服务ID
 * @param myEntityName
 * @param gatherInterval 定位周期(单位:秒)，5的倍数，默认为5秒
 * @param packInterval 打包回传周期(单位:秒)，5的倍数，回传周期最大不要超过定位周期的10倍，建议设置为定位周期的整数倍，默认为10秒
 */
class TraceUtils(private val context: Context,
                 private val baiduMap: BaiduMap,
                 private val serviceId: Long,
                 private val myEntityName: String,
                 private val gatherInterval: Int = LocationConstants.DEFAULT_GATHER_INTERVAL,
                 private val packInterval: Int = LocationConstants.DEFAULT_PACK_INTERVAL) {
    companion object {
        const val KEY_IS_TRACE_STARTED = "is_trace_started"
        const val KEY_IS_GATHER_STARTED = "is_gather_started"
    }

    private val mSequenceGenerator = AtomicInteger()
    private val locRequest: LocRequest by lazy { LocRequest(serviceId) }
    // 是否需要对象存储服务，默认为：false，关闭对象存储服务。注：鹰眼 Android SDK v3.0以上版本支持随轨迹上传图像等对象数据，若需使用此功能，该参数需设为 true，且需导入bos-android-sdk-1.0.2.jar。
    private val isNeedObjectStorage = false
    // 初始化轨迹服务
    private val mTrace: Trace by lazy { Trace(serviceId, myEntityName, isNeedObjectStorage) }
    // 初始化轨迹服务客户端
    private val mTraceClient: LBSTraceClient by lazy { LBSTraceClient(context.applicationContext) }
    // 初始化轨迹服务监听器
    private val mTraceListener: OnTraceListener = object : OnTraceListener {
        /**
         * 绑定服务回调接口
         * @param errorNo  状态码
         * @param message 消息
         *                <p>
         *                <pre>0：成功 </pre>
         *                <pre>1：失败</pre>
         */
        override fun onBindServiceCallback(p0: Int, p1: String?) {}

        override fun onInitBOSCallback(p0: Int, p1: String?) {}

        /**
         * 开启服务回调接口
         * @param errorNo 状态码
         * @param message 消息
         *                <p>
         *                <pre>0：成功 </pre>
         *                <pre>10000：请求发送失败</pre>
         *                <pre>10001：服务开启失败</pre>
         *                <pre>10002：参数错误</pre>
         *                <pre>10003：网络连接失败</pre>
         *                <pre>10004：网络未开启</pre>
         *                <pre>10005：服务正在开启</pre>
         *                <pre>10006：服务已开启</pre>
         */
        override fun onStartTraceCallback(status: Int, message: String) {
            if (StatusCodes.SUCCESS == status || StatusCodes.START_TRACE_NETWORK_CONNECT_FAILED <= status) {// 开启服务成功后
                Logger.d("开启鹰眼服务成功")
                SPUtils.getInstance(context).put(KEY_IS_TRACE_STARTED, true)
                startGather()
            } else {
                Logger.e("开启鹰眼服务失败")
            }
        }

        /**
         * 停止服务回调接口
         * @param errorNo 状态码
         * @param message 消息
         *                <p>
         *                <pre>0：成功</pre>
         *                <pre>11000：请求发送失败</pre>
         *                <pre>11001：服务停止失败</pre>
         *                <pre>11002：服务未开启</pre>
         *                <pre>11003：服务正在停止</pre>
         */
        override fun onStopTraceCallback(status: Int, message: String) {
            if (StatusCodes.SUCCESS == status || StatusCodes.CACHE_TRACK_NOT_UPLOAD == status) {
                Logger.d("停止鹰眼服务成功")
                // 停止成功后，直接移除is_trace_started记录（便于区分用户没有停止服务，直接杀死进程的情况）
                SPUtils.getInstance(context).remove(KEY_IS_TRACE_STARTED)
                SPUtils.getInstance(context).remove(KEY_IS_GATHER_STARTED)
            } else {
                Logger.e("停止鹰眼服务失败")
            }
        }

        /**
         * 开启采集回调接口
         * @param errorNo 状态码
         * @param message 消息
         *                <p>
         *                <pre>0：成功</pre>
         *                <pre>12000：请求发送失败</pre>
         *                <pre>12001：采集开启失败</pre>
         *                <pre>12002：服务未开启</pre>
         */
        override fun onStartGatherCallback(status: Int, message: String) {
            if (StatusCodes.SUCCESS == status || StatusCodes.GATHER_STARTED == status) {
                Logger.d("开启轨迹采集成功")
                SPUtils.getInstance(context).put(KEY_IS_GATHER_STARTED, true)
            } else {
                Logger.e("开启轨迹采集失败")
            }
        }

        /**
         * 停止采集回调接口
         * @param errorNo 状态码
         * @param message 消息
         *                <p>
         *                <pre>0：成功</pre>
         *                <pre>13000：请求发送失败</pre>
         *                <pre>13001：采集停止失败</pre>
         *                <pre>13002：服务未开启</pre>
         */
        override fun onStopGatherCallback(status: Int, message: String) {
            if (StatusCodes.SUCCESS == status || StatusCodes.GATHER_STOPPED == status) {
                Logger.d("停止轨迹服务成功")
                SPUtils.getInstance(context).remove(KEY_IS_GATHER_STARTED)
            } else {
                Logger.e("停止轨迹服务失败")
            }
        }

        /**
         * 推送消息回调接口
         *
         * @param messageType 状态码
         * @param pushMessage 消息
         *                  <p>
         *                  <pre>0x01：配置下发</pre>
         *                  <pre>0x02：语音消息</pre>
         *                  <pre>0x03：服务端围栏报警消息</pre>
         *                  <pre>0x04：本地围栏报警消息</pre>
         *                  <pre>0x05~0x40：系统预留</pre>
         *                  <pre>0x41~0xFF：开发者自定义</pre>
         */
        override fun onPushCallback(messageNo: Byte, message: PushMessage) {
            if (messageNo < 0x03 || messageNo > 0x04) {
                return
            }
            Logger.i("收到围栏报警消息：$messageNo：$message")
            /**
             * 获取报警推送消息
             */
            val alarmPushInfo = message.getFenceAlarmPushInfo()
            alarmPushInfo.getFenceId()//获取围栏id
            alarmPushInfo.getMonitoredPerson()//获取监控对象标识
            alarmPushInfo.getFenceName()//获取围栏名称
            alarmPushInfo.getPrePoint()//获取上一个点经度信息
            val alarmPoin = alarmPushInfo.getCurrentPoint()//获取报警点经纬度等信息
            alarmPoin.getCreateTime()//获取此位置上传到服务端时间
            alarmPoin.getLocTime()//获取定位产生的原始时间

            val curCircleFenceInfo = getCircleFenceInfoByFenceId(alarmPushInfo.fenceId)
            when (alarmPushInfo.monitoredAction) {
                MonitoredAction.enter -> {// 进入围栏
                    RxBus.post(LocationConstants.TAG_MOVE_IN_FENCE, curCircleFenceInfo)
                }
                MonitoredAction.exit -> {// 离开围栏
                    RxBus.post(LocationConstants.TAG_MOVE_OUT_FENCE, curCircleFenceInfo)
                }
                else -> {
                }
            }
        }
    }

    private val mFenceInfoList = mutableListOf<CircleFenceInfo>()

    init {
        // 设置定位和打包周期
        mTraceClient.setInterval(gatherInterval, packInterval)
        mTraceClient.setOnTraceListener(mTraceListener)
        SPUtils.getInstance(context).remove(KEY_IS_TRACE_STARTED)
        SPUtils.getInstance(context).remove(KEY_IS_GATHER_STARTED)

        // 点击围栏监听
        baiduMap.setOnMapClickListener(object : BaiduMap.OnMapClickListener {
            override fun onMapClick(p0: LatLng?) {
                mFenceInfoList.forEach {
                    if (it.clickInOverlay(p0)) {
                        RxBus.post(LocationConstants.TAG_CLICK_FENCE_OVERLAY, it)
                        return@forEach
                    }
                }
            }

            override fun onMapPoiClick(p0: MapPoi?): Boolean {
                return false
            }
        })
    }

    fun destory() {
        mFenceInfoList.forEach { it.destroy() }
        stopGather()
        stopTrace()
    }

    /**
     * 开启鹰眼服务，启动鹰眼 service
     */
    fun startTrace() {
        mTraceClient.startTrace(mTrace, null)
    }

    /**
     * 开启轨迹采集，启动轨迹追踪。至此，正式开启轨迹追踪。
     * 注意：因为startTrace与startGather是异步执行，且startGather依赖startTrace执行开启服务成功，所以建议startGather在public void onStartTraceCallback(int errorNo, String message)回调返回错误码为0后，再进行调用执行，否则会出现服务开启失败12002的错误。
     */
    fun startGather() {
        mTraceClient.startGather(null)
    }

    /**
     * 停止轨迹服务：此方法将同时停止轨迹服务和轨迹采集，完全结束鹰眼轨迹服务。若需再次启动轨迹追踪，需重新启动服务和轨迹采集
     */
    fun stopTrace() {
        mTraceClient.stopTrace(mTrace, null)
    }

    /**
     * 停止轨迹采集：此方法将停止轨迹采集，但不停止轨迹服务（即，不再采集轨迹点了，但鹰眼 service 还存活）。若需再次启动轨迹追踪，直接调用mTraceClient.startGather()方法开启轨迹采集即可，无需再次启动轨迹服务。此方式可应用于频繁中断轨迹追踪的场景，可避免频繁启动服务。
     */
    fun stopGather() {
        mTraceClient.stopGather(null)
    }

    /**
     * 查询myEntityName当前时间以前12小时的历史轨迹
     *
     * @param tag 请求标识
     * @param startTime 开始时间戳(单位：秒)，默认为当前时间以前12小时
     * @param endTime 结束时间戳(单位：秒)，默认为当前时间
     * @param listener 轨迹监听器
     */
    fun queryHistoryTrack(tag: Int = 1, startTime: Long = System.currentTimeMillis() / 1000 - 12 * 60 * 60, endTime: Long = System.currentTimeMillis() / 1000, listener: OnTrackListener) {
        // 创建历史轨迹请求实例
        val historyTrackRequest = HistoryTrackRequest(tag, serviceId, myEntityName)

        // 设置轨迹查询起止时间
        // 设置开始时间
        historyTrackRequest.startTime = startTime
        // 设置结束时间
        historyTrackRequest.endTime = endTime

        // 查询历史轨迹
        mTraceClient.queryHistoryTrack(historyTrackRequest, listener)
    }

    /**
     * 设置轨迹采集和打包上传的间隔
     */
    fun setInterval(gatherInterval: Int, packInterval: Int) {
        mTraceClient.setInterval(gatherInterval, packInterval)
    }

    /**
     * 获取myEntityName当前位置
     */
    fun getCurrentLocation(entityListener: OnEntityListener, trackListener: OnTrackListener) {
        // 网络连接正常，开启服务及采集，则查询纠偏后实时位置；否则进行实时定位
        if (NetWorkUtils.isConnected(context)
                && SPUtils.getInstance(context).get(KEY_IS_TRACE_STARTED, false)
                && SPUtils.getInstance(context).get(KEY_IS_GATHER_STARTED, false)) {
            val request = LatestPointRequest(getTag(), serviceId, myEntityName)
            val processOption = ProcessOption()
            processOption.isNeedDenoise = true
            processOption.radiusThreshold = 100
            request.processOption = processOption
            mTraceClient.queryLatestPoint(request, trackListener)
        } else {
            mTraceClient.queryRealTimeLoc(locRequest, entityListener)
        }
    }

    /**
     * 查询其它设备
     */
    fun queryEntityList(entityNames: List<String>? = null, listener: OnEntityListener) {
        // 过滤条件
        val filterCondition = FilterCondition()
        filterCondition.entityNames = entityNames
        filterCondition.activeTime = (System.currentTimeMillis() / 1000 - 30)// 只查询30秒之内活跃的
        // 返回结果坐标类型
        val coordTypeOutput = CoordType.bd09ll
        // 分页索引
        val pageIndex = 1
        // 分页大小
        val pageSize = 100

        // 创建Entity列表请求实例
        val request = EntityListRequest(getTag(), serviceId, filterCondition, coordTypeOutput, pageIndex, pageSize)

        // 查询Entity列表
        mTraceClient.queryEntityList(request, listener)
    }

    private fun getTag() = mSequenceGenerator.incrementAndGet()

    fun createFences(circleFenceInfoList: List<CircleFenceInfo>) {
        mFenceInfoList.clear()
        mFenceInfoList.addAll(circleFenceInfoList)
        if (mFenceInfoList.isNotEmpty())
            queryFenceList()
    }

    /**
     * 查询围栏
     */
    private fun queryFenceList() {
        // 请求标识
        val tag = getTag()
        val request = FenceListRequest.buildLocalRequest(tag, serviceId, myEntityName, null)
        mTraceClient.queryFenceList(request, object : OnFenceListenerAdapter() {
            override fun onFenceListCallback(response: FenceListResponse) {
                if (StatusCodes.SUCCESS == response.getStatus() && response.size != 0 && response.fenceType == FenceType.local) {
                    mFenceInfoList.forEach {
                        val circleFenceInfo = it
                        val filter = response.fenceInfos.filter { it.circleFence.fenceName == circleFenceInfo.name }
                        if (filter.isNotEmpty()) {
                            circleFenceInfo.id = filter[0].circleFence.fenceId// 赋值围栏id
                            Logger.i("已经存在围栏：$circleFenceInfo")
                            circleFenceInfo.createOverlay(baiduMap)
                        } else {
                            Logger.i("创建围栏：$circleFenceInfo")
                            createFence(circleFenceInfo)
                        }
                    }
                } else {
                    mFenceInfoList.forEach {
                        Logger.i("创建围栏：$it")
                        createFence(it)
                    }
                }
            }
        })
    }

    /**
     * 创建围栏
     *
     * @param fenceName 围栏名称
     * @param lat 围栏圆心纬度
     * @param lng 围栏圆心经度
     * @param radius 围栏半径（单位 : 米）
     */
    private fun createFence(circleFenceInfo: CircleFenceInfo) {
        // 请求标识
        val tag = getTag()
        // 围栏圆心
        val center = com.baidu.trace.model.LatLng(circleFenceInfo.latLng?.latitude ?: 0.0, circleFenceInfo.latLng?.longitude ?: 0.0)
        // 去噪精度，则定位精度大于denoise米的轨迹点都不会参与围栏计算。
        val denoise = 30
        // 坐标类型
        val coordType = CoordType.bd09ll
        // 创建本地圆形围栏
        val localCircleFenceRequest = CreateFenceRequest.buildLocalCircleRequest(tag, serviceId, circleFenceInfo.name, myEntityName, center, circleFenceInfo.radius.toDouble(), denoise, coordType)
        // 创建本地圆形围栏
        mTraceClient.createFence(localCircleFenceRequest, object : OnFenceListenerAdapter() {
            override fun onCreateFenceCallback(response: CreateFenceResponse) {
                //创建围栏响应结果,能获取围栏的一些信息
                if (StatusCodes.SUCCESS != response.getStatus()) {
                    context.shortToastCenter("创建围栏失败")
                    return
                }
                circleFenceInfo.id = response.fenceId//创建的围栏id
                Logger.i("创建围栏成功：$circleFenceInfo")
                circleFenceInfo.createOverlay(baiduMap)
            }
        })
    }

    private fun getCircleFenceInfoByFenceId(fenceId: Long): CircleFenceInfo? {
        val filter = mFenceInfoList.filter { it.id == fenceId }
        return if (filter.isNotEmpty()) {
            filter[0]
        } else {
            null
        }
    }

    private fun getFenceIds(): List<Long> {
        val fenceIds = mutableListOf<Long>()
        mFenceInfoList.mapTo(fenceIds) {
            Logger.w(it)
            it.id
        }
        return fenceIds
    }

    // 查询围栏历史告警信息
    fun queryFenceHistoryAlarmInfo() {
        val request = HistoryAlarmRequest.buildLocalRequest(
                getTag(),
                serviceId,
                (System.currentTimeMillis() / 1000 - 60 * 60 * 24),
                System.currentTimeMillis(),
                myEntityName,
                getFenceIds()
        )

        mTraceClient.queryFenceHistoryAlarmInfo(request, object : OnFenceListenerAdapter() {
            // 查询围栏历史报警信息响应结果
            override fun onHistoryAlarmCallback(response: HistoryAlarmResponse) {
                super.onHistoryAlarmCallback(response)
                //获取报警信息列表，FenceAlarmInfo继承FenceAlarmPushInfo
                val fenceAlarmInfos = response.fenceAlarmInfos

                val sb = StringBuilder()
                fenceAlarmInfos.forEach {
                    sb.append("${it.fenceId}；${it.fenceName}；${it.monitoredPerson}；${it.monitoredAction}\n")
                }
                context.longToastCenter(sb.toString())
                Logger.e(sb.toString())
            }
        })
    }

    // 查询被监控者状态
    fun queryMonitoredStatus() {
        val request = MonitoredStatusRequest.buildLocalRequest(
                getTag(),
                serviceId,
                myEntityName,
                getFenceIds()
        )

        mTraceClient.queryMonitoredStatus(request, object : OnFenceListenerAdapter() {
            override fun onMonitoredStatusCallback(response: MonitoredStatusResponse) {
                //查询监控对象状态响应结果
                val sb = StringBuilder()
                response.monitoredStatusInfos?.forEach {
                    when (it.monitoredStatus) {//获取状态
                        MonitoredStatus.`in` -> {// 监控的设备在围栏内
                            sb.append("在围栏内(围栏id：${it.fenceId})\n")
                            Logger.e("监控的设备在围栏内(围栏id：${it.fenceId})")
                        }
                        MonitoredStatus.out -> {// 监控的设备在围栏外
                            sb.append("在围栏外(围栏id：${it.fenceId})\n")
                            Logger.e("监控的设备在围栏外(围栏id：${it.fenceId})")
                        }
                        else -> {
                            sb.append("状态未知(围栏id：${it.fenceId})\n")
                            Logger.e("监控的设备状态未知(围栏id：${it.fenceId})")
                        }
                    }
                }
                context.longToastCenter(sb.toString())
            }
        })
    }

}
