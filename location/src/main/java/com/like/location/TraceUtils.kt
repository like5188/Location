//package com.like.location
//
//import android.content.Context
//import android.util.Log
//import android.widget.Toast
//import com.baidu.mapapi.NetworkUtil
//import com.baidu.mapapi.map.BaiduMap
//import com.baidu.mapapi.map.MapPoi
//import com.baidu.mapapi.model.LatLng
//import com.baidu.trace.LBSTraceClient
//import com.baidu.trace.Trace
//import com.baidu.trace.api.entity.EntityListRequest
//import com.baidu.trace.api.entity.FilterCondition
//import com.baidu.trace.api.entity.LocRequest
//import com.baidu.trace.api.entity.OnEntityListener
//import com.baidu.trace.api.fence.*
//import com.baidu.trace.api.track.*
//import com.baidu.trace.model.*
//import com.like.livedatabus.LiveDataBus
//import com.like.location.entity.CircleFenceInfo
//import com.like.location.listener.OnFenceListenerAdapter
//import com.like.location.util.LocationConstants
//import com.like.location.util.SPUtils
//import com.like.location.util.SingletonHolder
//import java.util.concurrent.atomic.AtomicInteger
//import kotlin.jvm.functions.FunctionN
//
///**
// * 鹰眼轨迹管理工具类
// * 包括：自己的轨迹上传、围栏的创建、以及自己进出围栏的告警。
// * 还可以查询指定entity列表的实时位置
// * 注意：地图只支持Android v4.0以上系统
// *
// * service：
// * 一个service（即鹰眼轨迹服务）对应一个轨迹管理系统，一个service里可管理多个终端设备（即entity），
// * service的唯一标识符是service_id。
// * 一个开发者最多可创建10个service
// *
// * entity：
// * 一个entity代表现实中一个被追踪轨迹的终端设备，它可以是一个人、一辆车或者任何运动物体。
// * 同一个service中，entity以entity_name作为唯一标识。
// * 一个service至多同时管理100万个entity。
// * 鹰眼Web API提供了entity的增、删、改、查接口。
// *
// * fence：
// * fence即地理围栏，是指一定范围（如：圆形、多边形、线型、行政区）的虚拟地理区域。
// * 客户端围栏：
// * 围栏的创建、计算和报警均在SDK完成，在 GPS 定位成功的情况下，无需联网即可完成围栏运算。
// * 可应用于手机终端网络不稳定情况下，仍需实时获取围栏报警的场景。
// * 无需联网即可利用 GPS 轨迹点在手机本地进行围栏计算，APP 可及时收到报警信息进行相应的业务处理。
// * 仅支持圆形围栏
// * 服务端围栏：
// * 围栏的创建、计算和报警的发起都在鹰眼服务端完成，依赖于轨迹点上传至服务端才能进行围栏进算。
// * 相较于客户端围栏，服务端围栏报警推送方式更多样：支持推送至 SDK，也支持推送至开发者的服务端，同时还支持报警信息批量查询
// * 支持圆形、多边形、线型、行政区
// * 当entity进入/离开该区域时，鹰眼将自动推送报警至开发者。开发者接收到报警后，可进行业务处理。
// * 一个entity最多可创建100个私有地理围栏，一个service可创建1000个公共围栏。
// * 鹰眼API和SDK提供了fence的增删改查接口，以及查询被监控者在围栏内/外、查询历史报警信息等接口。
// *
// * track：
// * entity移动所产生的连续轨迹被称为track，track由一系列轨迹点（point）组成。
// * 轨迹点数量无限制。
// * 鹰眼Web API提供了添加轨迹点、批量添加轨迹点、查询历史轨迹接口。使用鹰眼Android SDK时，SDK会根据开发者设定的频率定位，回传轨迹点。
// */
//class TraceUtils private constructor(private val context: Context) {
//    companion object : SingletonHolder<TraceUtils>(object : FunctionN<TraceUtils> {
//        override val arity: Int = 0 // number of arguments that must be passed to constructor
//
//        override fun invoke(vararg args: Any?): TraceUtils {
//            return TraceUtils((args[0] as Context).applicationContext)
//        }
//    }) {
//        private val TAG = TraceUtils::class.java.simpleName
//        const val KEY_IS_TRACE_STARTED = "is_trace_started"
//        const val KEY_IS_GATHER_STARTED = "is_gather_started"
//        /**
//         * 默认定位周期(单位:秒)
//         */
//        const val DEFAULT_GATHER_INTERVAL = 5
//
//        /**
//         * 默认打包回传周期(单位:秒)
//         */
//        const val DEFAULT_PACK_INTERVAL = 10
//    }
//
//    private lateinit var baiduMap: BaiduMap
//    private var serviceId: Long = 0
//    private var myEntityName: String = ""
//
//    private val mSequenceGenerator = AtomicInteger()
//    private val mFenceInfoList = mutableListOf<CircleFenceInfo>()
//    // 是否需要对象存储服务，默认为：false，关闭对象存储服务。注：鹰眼 Android SDK v3.0以上版本支持随轨迹上传图像等对象数据，若需使用此功能，该参数需设为 true，且需导入bos-android-sdk-1.0.3.jar。
//    private val isNeedObjectStorage = false
//    // 初始化轨迹服务
//    private val mTrace: Trace by lazy { Trace(serviceId, myEntityName, isNeedObjectStorage) }
//    // 轨迹服务客户端
//    private val mTraceClient: LBSTraceClient by lazy { LBSTraceClient(context) }
//    // 初始化轨迹服务监听器
//    private val mTraceListener: OnTraceListener = object : OnTraceListener {
//        /**
//         * 绑定服务回调接口
//         * @param status  状态码
//         * @param message 消息
//         *                <p>
//         *                <pre>0：成功 </pre>
//         *                <pre>1：失败</pre>
//         */
//        override fun onBindServiceCallback(status: Int, message: String?) {}
//
//        override fun onInitBOSCallback(status: Int, message: String?) {}
//
//        /**
//         * 开启服务回调接口
//         * @param status    状态码
//         * @param message   消息
//         *                <p>
//         *                <pre>0：成功 </pre>
//         *                <pre>10000：请求发送失败</pre>
//         *                <pre>10001：服务开启失败</pre>
//         *                <pre>10002：参数错误</pre>
//         *                <pre>10003：网络连接失败</pre>
//         *                <pre>10004：网络未开启</pre>
//         *                <pre>10005：服务正在开启</pre>
//         *                <pre>10006：服务已开启</pre>
//         */
//        override fun onStartTraceCallback(status: Int, message: String) {
//            if (StatusCodes.SUCCESS == status || StatusCodes.START_TRACE_NETWORK_CONNECT_FAILED <= status) {// 开启服务成功后
//                Log.d(TAG, "开启鹰眼服务成功")
//                SPUtils.getInstance().put(KEY_IS_TRACE_STARTED, true)
//                startGather()
//            } else {
//                Log.e(TAG, "开启鹰眼服务失败")
//            }
//        }
//
//        /**
//         * 停止服务回调接口
//         * @param status    状态码
//         * @param message   消息
//         *                <p>
//         *                <pre>0：成功</pre>
//         *                <pre>11000：请求发送失败</pre>
//         *                <pre>11001：服务停止失败</pre>
//         *                <pre>11002：服务未开启</pre>
//         *                <pre>11003：服务正在停止</pre>
//         */
//        override fun onStopTraceCallback(status: Int, message: String) {
//            if (StatusCodes.SUCCESS == status || StatusCodes.CACHE_TRACK_NOT_UPLOAD == status) {
//                Log.d(TAG, "停止鹰眼服务成功")
//                // 停止成功后，直接移除is_trace_started记录（便于区分用户没有停止服务，直接杀死进程的情况）
//                SPUtils.getInstance().remove(KEY_IS_TRACE_STARTED)
//                SPUtils.getInstance().remove(KEY_IS_GATHER_STARTED)
//            } else {
//                Log.e(TAG, "停止鹰眼服务失败")
//            }
//        }
//
//        /**
//         * 开启采集回调接口
//         * @param status    状态码
//         * @param message   消息
//         *                <p>
//         *                <pre>0：成功</pre>
//         *                <pre>12000：请求发送失败</pre>
//         *                <pre>12001：采集开启失败</pre>
//         *                <pre>12002：服务未开启</pre>
//         */
//        override fun onStartGatherCallback(status: Int, message: String) {
//            if (StatusCodes.SUCCESS == status || StatusCodes.GATHER_STARTED == status) {
//                Log.d(TAG, "开启轨迹采集成功")
//                SPUtils.getInstance().put(KEY_IS_GATHER_STARTED, true)
//            } else {
//                Log.e(TAG, "开启轨迹采集失败")
//            }
//        }
//
//        /**
//         * 停止采集回调接口
//         * @param status    状态码
//         * @param message   消息
//         *                <p>
//         *                <pre>0：成功</pre>
//         *                <pre>13000：请求发送失败</pre>
//         *                <pre>13001：采集停止失败</pre>
//         *                <pre>13002：服务未开启</pre>
//         */
//        override fun onStopGatherCallback(status: Int, message: String) {
//            if (StatusCodes.SUCCESS == status || StatusCodes.GATHER_STOPPED == status) {
//                Log.d(TAG, "停止轨迹服务成功")
//                SPUtils.getInstance().remove(KEY_IS_GATHER_STARTED)
//            } else {
//                Log.e(TAG, "停止轨迹服务失败")
//            }
//        }
//
//        /**
//         * 推送消息回调接口
//         *
//         * @param messageNo 状态码
//         * @param message   消息
//         *                  <p>
//         *                  <pre>0x01：配置下发</pre>
//         *                  <pre>0x02：语音消息</pre>
//         *                  <pre>0x03：服务端围栏报警消息</pre>
//         *                  <pre>0x04：本地围栏报警消息</pre>
//         *                  <pre>0x05~0x40：系统预留</pre>
//         *                  <pre>0x41~0xFF：开发者自定义</pre>
//         */
//        override fun onPushCallback(messageNo: Byte, message: PushMessage) {
//            if (messageNo < 0x03 || messageNo > 0x04) {
//                return
//            }
//            Log.i(TAG, "收到围栏报警消息：$messageNo：$message")
//            /**
//             * 获取报警推送消息
//             */
//            val alarmPushInfo = message.fenceAlarmPushInfo
//            alarmPushInfo.fenceId//获取围栏id
//            alarmPushInfo.monitoredPerson//获取监控对象标识
//            alarmPushInfo.fenceName//获取围栏名称
//            alarmPushInfo.prePoint//获取上一个点经度信息
//            val alarmPoint = alarmPushInfo.currentPoint//获取报警点经纬度等信息
//            alarmPoint.createTime//获取此位置上传到服务端时间
//            alarmPoint.locTime//获取定位产生的原始时间
//
//            val curCircleFenceInfo = getCircleFenceInfoByFenceId(alarmPushInfo.fenceId)
//            when (alarmPushInfo.monitoredAction) {
//                MonitoredAction.enter -> {// 进入围栏
//                    LiveDataBus.post(LocationConstants.TAG_MOVE_IN_FENCE, curCircleFenceInfo)
//                }
//                MonitoredAction.exit -> {// 离开围栏
//                    LiveDataBus.post(LocationConstants.TAG_MOVE_OUT_FENCE, curCircleFenceInfo)
//                }
//                else -> {
//                }
//            }
//        }
//    }
//
//    fun isInitialized() = ::baiduMap.isInitialized
//
//    /**
//     * 初始化鹰眼轨迹服务，必须在调用其它方法之前调用
//     *
//     * @param baiduMap
//     * @param serviceId         轨迹服务ID
//     * @param myEntityName      设备标识
//     */
//    fun init(baiduMap: BaiduMap, serviceId: Long, myEntityName: String) {
//        if (isInitialized()) return
//        this.baiduMap = baiduMap
//        this.serviceId = serviceId
//        this.myEntityName = myEntityName
//
//        // 清除缓存
//        SPUtils.getInstance().init(context)
//        SPUtils.getInstance().remove(KEY_IS_TRACE_STARTED)
//        SPUtils.getInstance().remove(KEY_IS_GATHER_STARTED)
//
//        // 设置定位和打包周期
//        mTraceClient.setInterval(DEFAULT_GATHER_INTERVAL, DEFAULT_PACK_INTERVAL)
//        mTraceClient.setOnTraceListener(mTraceListener)
//
//        // 查询轨迹接口提供了HTTP和HTTPS两种协议。使用HTTPS时，可能会降低请求效率。
//        mTraceClient.setProtocolType(ProtocolType.HTTP)
//
//        // 设置点击围栏覆盖物的监听
//        baiduMap.setOnMapClickListener(object : BaiduMap.OnMapClickListener {
//            override fun onMapClick(p0: LatLng?) {
//                mFenceInfoList.forEach {
//                    if (it.isClickedInOverlay(p0)) {
//                        LiveDataBus.post(LocationConstants.TAG_CLICK_FENCE_OVERLAY, it)
//                        return@forEach
//                    }
//                }
//            }
//
//            override fun onMapPoiClick(p0: MapPoi?): Boolean {
//                return false
//            }
//        })
//    }
//
//    /**
//     * 查询指定entity列表的实时位置
//     * 1. 查询某一个 entity 的详细信息，包括实时位置
//     * 2. 查询所有设备信息和实时位置，如轨迹管理台的entity列表面板
//     * 3. 查询在线和离线设备
//     *
//     * @param entityNames   entity标识列表
//     * @param activeTime    指定时间内定位且上传了轨迹点的entity。默认30秒
//     */
//    fun queryEntityList(entityNames: List<String>? = null, activeTime: Int = 30, listener: OnEntityListener) {
//        // 过滤条件
//        val filterCondition = FilterCondition()
//        filterCondition.entityNames = entityNames
//        filterCondition.activeTime = (System.currentTimeMillis() / 1000 - activeTime)// 只查询activeTime秒之内活跃的
//        // 返回结果坐标类型
//        val coordTypeOutput = CoordType.bd09ll
//        // 分页索引
//        val pageIndex = 1
//        // 分页大小
//        val pageSize = 1000
//
//        // 创建Entity列表请求实例
//        val request = EntityListRequest(mSequenceGenerator.incrementAndGet(), serviceId, filterCondition, coordTypeOutput, pageIndex, pageSize)
//
//        // 查询Entity列表
//        mTraceClient.queryEntityList(request, listener)
//    }
//
//    fun destroy() {
//        mFenceInfoList.forEach { it.destroy() }
//        stopTrace()
//    }
//
//    /**
//     * 开启鹰眼服务，启动鹰眼 service
//     *
//     * 调用startTrace()后，SDK会与服务端建立连接，并将已缓存的轨迹数据上传到服务端，但不会进行定位采集，即尚未开始轨迹追踪。
//     */
//    fun startTrace() {
//        mTraceClient.startTrace(mTrace, null)
//    }
//
//    /**
//     * 停止轨迹服务：此方法将同时停止轨迹服务和轨迹采集，完全结束鹰眼轨迹服务。
//     * 若需再次启动轨迹追踪，需重新启动服务和轨迹采集
//     */
//    fun stopTrace() {
//        mTraceClient.stopTrace(mTrace, null)
//    }
//
//    /**
//     * 开启轨迹采集，启动轨迹追踪。至此，正式开启轨迹追踪。
//     *
//     * 在采集过程中，若出现网络中断、连上不可上网的Wi-Fi，或网络频繁切换时，SDK都将自动开启缓存模式，将采集的轨迹数据保存到数据库中，并自动监听网络，待联网时自动回传缓存数据。
//     *
//     * 注意：因为startTrace与startGather是异步执行，且startGather依赖startTrace执行开启服务成功，
//     * 所以建议startGather在public void onStartTraceCallback(int errorNo, String message)回调返回0后，
//     * 再进行调用执行，否则会出现服务开启失败12002的错误。
//     */
//    private fun startGather() {
//        mTraceClient.startGather(null)
//    }
//
//    /**
//     * 停止轨迹采集：此方法将停止轨迹采集，但不停止轨迹服务（即，不再采集轨迹点了，但鹰眼 service 还存活）。
//     * 若需再次启动轨迹追踪，直接调用mTraceClient.startGather()方法开启轨迹采集即可，无需再次启动轨迹服务。
//     * 此方式可应用于频繁中断轨迹追踪的场景，可避免频繁启动服务。
//     */
//    private fun stopGather() {
//        mTraceClient.stopGather(null)
//    }
//
//    /**
//     * 设置轨迹采集和打包上传的间隔。可随时改变。
//     *
//     * @param gatherInterval    定位周期(单位:秒)
//     * 多久定位一次，在定位周期大于15s时，SDK会将定位周期设置为5的倍数，默认为5秒
//     * @param packInterval      打包回传周期(单位:秒)
//     * 鹰眼为节省电量和流量，并不是定位一次就回传一次数据，而是隔段时间将一批定位数据打包压缩回传。
//     * 回传周期最大不要超过定位周期的10倍，回传周期不能小于定位周期，否则回传不生效；回传周期建议设置为定位周期的整数倍。​默认为10秒。
//     */
//    fun setInterval(gatherInterval: Int, packInterval: Int) {
//        mTraceClient.setInterval(gatherInterval, packInterval)
//    }
//
//    /**
//     * 查询历史轨迹
//     * 查询一个被追踪者某时间段的历史轨迹。
//     *
//     * @param startTime         开始时间戳，默认为当前时间以前12小时
//     * @param endTime           结束时间戳，默认为当前时间
//     * @param isProcessed       是否纠偏
//     * @param transportMode     交通方式。默认为驾车
//     * @param supplementMode    里程填充方式。默认为驾车
//     * @param listener          轨迹监听器
//     */
//    fun queryHistoryTrack(
//            startTime: Long = System.currentTimeMillis() / 1000 - 12 * 60 * 60,
//            endTime: Long = System.currentTimeMillis() / 1000,
//            isProcessed: Boolean = false,
//            transportMode: TransportMode = TransportMode.driving,
//            supplementMode: SupplementMode = SupplementMode.driving,
//            listener: OnTrackListener
//    ) {
//        // 创建历史轨迹请求实例
//        val historyTrackRequest = HistoryTrackRequest(getTag(), serviceId, myEntityName)
//
//        // 设置轨迹查询起止时间
//        // 设置开始时间
//        historyTrackRequest.startTime = startTime
//        // 设置结束时间
//        historyTrackRequest.endTime = endTime
//
//        if (isProcessed) {
//            // 设置需要纠偏
//            historyTrackRequest.isProcessed = true
//
//            // 创建纠偏选项实例
//            val processOption = ProcessOption()
//            // 设置需要去噪
//            processOption.isNeedDenoise = true
//            // 设置需要抽稀
//            processOption.isNeedVacuate = true
//            // 设置需要绑路
//            processOption.isNeedMapMatch = true
//            // 设置精度过滤值(定位精度大于100米的过滤掉)
//            processOption.radiusThreshold = 100
//            // 设置交通方式为驾车
//            processOption.transportMode = transportMode
//            // 设置纠偏选项
//            historyTrackRequest.processOption = processOption
//
//            // 设置里程填充方式为驾车
//            historyTrackRequest.supplementMode = supplementMode
//        }
//
//        // 查询历史轨迹
//        mTraceClient.queryHistoryTrack(historyTrackRequest, listener)
//    }
//
//    /**
//     * 计算指定时间段内的轨迹里程
//     *
//     * @param startTime         开始时间戳，默认为当前时间以前12小时
//     * @param endTime           结束时间戳，默认为当前时间
//     * @param isProcessed       是否纠偏
//     * @param transportMode     交通方式。默认为驾车
//     * @param supplementMode    里程填充方式。默认为驾车
//     * @param listener          轨迹监听器
//     */
//    fun queryDistance(
//            startTime: Long = System.currentTimeMillis() / 1000 - 12 * 60 * 60,
//            endTime: Long = System.currentTimeMillis() / 1000,
//            isProcessed: Boolean = false,
//            transportMode: TransportMode = TransportMode.driving,
//            supplementMode: SupplementMode = SupplementMode.driving,
//            listener: OnTrackListener
//    ) {
//        // 创建里程查询请求实例
//        val distanceRequest = DistanceRequest(getTag(), serviceId, myEntityName)
//
//        // 设置开始时间
//        distanceRequest.startTime = startTime
//        // 设置结束时间
//        distanceRequest.endTime = endTime
//
//        if (isProcessed) {
//            // 设置需要纠偏
//            distanceRequest.isProcessed = true
//
//            // 创建纠偏选项实例
//            val processOption = ProcessOption()
//            // 设置需要去噪
//            processOption.isNeedDenoise = true
//            // 设置需要绑路
//            processOption.isNeedMapMatch = true
//            // 设置交通方式为驾车
//            processOption.transportMode = transportMode
//            // 设置纠偏选项
//            distanceRequest.processOption = processOption
//
//            // 设置里程填充方式为驾车
//            distanceRequest.supplementMode = supplementMode
//        }
//
//        // 查询里程
//        mTraceClient.queryDistance(distanceRequest, listener)
//    }
//
//    /**
//     * 获取myEntityName当前位置
//     */
//    fun getCurrentLocation(entityListener: OnEntityListener, trackListener: OnTrackListener) {
//        // 网络连接正常，开启服务及采集，则查询纠偏后的实时位置；否则进行实时定位
//        if (NetworkUtil.isNetworkAvailable(context)
//                && SPUtils.getInstance().get(KEY_IS_TRACE_STARTED, false)
//                && SPUtils.getInstance().get(KEY_IS_GATHER_STARTED, false)) {
//            val request = LatestPointRequest(getTag(), serviceId, myEntityName)
//            val processOption = ProcessOption()
//            processOption.isNeedDenoise = true
//            processOption.radiusThreshold = 100
//            request.processOption = processOption
//            mTraceClient.queryLatestPoint(request, trackListener)
//        } else {
//            mTraceClient.queryRealTimeLoc(LocRequest(serviceId), entityListener)
//        }
//    }
//
//    /**
//     * 查询本地围栏历史告警信息
//     *
//     * @param startTime         开始时间戳，默认为当前时间以前24小时
//     * @param endTime           结束时间戳，默认为当前时间
//     */
//    fun queryFenceHistoryAlarmInfo(
//            startTime: Long = System.currentTimeMillis() / 1000 - 24 * 60 * 60,
//            endTime: Long = System.currentTimeMillis() / 1000
//    ) {
//        val request = HistoryAlarmRequest.buildLocalRequest(
//                getTag(),
//                serviceId,
//                startTime,
//                endTime,
//                myEntityName,
//                getFenceIds()
//        )
//
//        mTraceClient.queryFenceHistoryAlarmInfo(request, object : OnFenceListenerAdapter() {
//            // 查询围栏历史报警信息响应结果
//            override fun onHistoryAlarmCallback(response: HistoryAlarmResponse) {
//                super.onHistoryAlarmCallback(response)
//                //获取报警信息列表，FenceAlarmInfo继承FenceAlarmPushInfo
//                val fenceAlarmInfos = response.fenceAlarmInfos
//
//                val sb = StringBuilder()
//                fenceAlarmInfos.forEach {
//                    sb.append("${it.fenceId}；${it.fenceName}；${it.monitoredPerson}；${it.monitoredAction}\n")
//                }
//                Toast.makeText(context, sb.toString(), Toast.LENGTH_SHORT).show()
//                Log.e(TAG, sb.toString())
//            }
//        })
//    }
//
//    /**
//     * 在本地查询被监控者状态
//     * 查询被监控者是在围栏内或围栏外
//     */
//    fun queryMonitoredStatus() {
//        val request = MonitoredStatusRequest.buildLocalRequest(
//                getTag(),
//                serviceId,
//                myEntityName,
//                getFenceIds()
//        )
//
//        mTraceClient.queryMonitoredStatus(request, object : OnFenceListenerAdapter() {
//            override fun onMonitoredStatusCallback(response: MonitoredStatusResponse) {
//                //查询监控对象状态响应结果
//                val sb = StringBuilder()
//                response.monitoredStatusInfos?.forEach {
//                    when (it.monitoredStatus) {//获取状态
//                        MonitoredStatus.`in` -> {// 监控的设备在围栏内
//                            sb.append("在围栏内(围栏id：${it.fenceId})\n")
//                            Log.e(TAG, "被监控者在围栏内(围栏id：${it.fenceId})")
//                        }
//                        MonitoredStatus.out -> {// 监控的设备在围栏外
//                            sb.append("在围栏外(围栏id：${it.fenceId})\n")
//                            Log.e(TAG, "被监控者在围栏外(围栏id：${it.fenceId})")
//                        }
//                        else -> {
//                            sb.append("状态未知(围栏id：${it.fenceId})\n")
//                            Log.e(TAG, "被监控者状态未知(围栏id：${it.fenceId})")
//                        }
//                    }
//                }
//                Toast.makeText(context, sb.toString(), Toast.LENGTH_SHORT).show()
//            }
//        })
//    }
//
//    fun getFenceInfo(index: Int): CircleFenceInfo? =
//            if (mFenceInfoList.size > index) {
//                mFenceInfoList[index]
//            } else {
//                null
//            }
//
//    /**
//     * 创建本地围栏
//     */
//    fun createLocalFences(circleFenceInfoList: List<CircleFenceInfo>) {
//        if (circleFenceInfoList.isEmpty()) return
//        mFenceInfoList.clear()
//        mFenceInfoList.addAll(circleFenceInfoList)
//
//        val request = FenceListRequest.buildLocalRequest(getTag(), serviceId, myEntityName, null)
//        mTraceClient.queryFenceList(request, object : OnFenceListenerAdapter() {
//            override fun onFenceListCallback(response: FenceListResponse) {
//                if (
//                        StatusCodes.SUCCESS == response.getStatus() &&
//                        response.size != 0 &&
//                        response.fenceType == FenceType.local
//                ) {
//                    mFenceInfoList.forEach { circleFenceInfo ->
//                        val filter = response.fenceInfos.filter { it.circleFence.fenceName == circleFenceInfo.name }
//                        if (filter.isNotEmpty()) {
//                            circleFenceInfo.id = filter[0].circleFence.fenceId// 赋值围栏id
//                            Log.i(TAG, "本地已经存在围栏：$circleFenceInfo")
//                            circleFenceInfo.createOverlay(baiduMap)
//                        } else {
//                            Log.i(TAG, "创建本地围栏：$circleFenceInfo")
//                            createLocalFence(circleFenceInfo)
//                        }
//                    }
//                } else {
//                    mFenceInfoList.forEach {
//                        Log.i(TAG, "创建本地围栏：$it")
//                        createLocalFence(it)
//                    }
//                }
//            }
//        })
//    }
//
//    /**
//     * 创建本地围栏
//     */
//    private fun createLocalFence(circleFenceInfo: CircleFenceInfo) {
//        // 围栏圆心
//        val latitude = circleFenceInfo.latLng?.latitude ?: 0.0
//        val longitude = circleFenceInfo.latLng?.longitude ?: 0.0
//        val center = com.baidu.trace.model.LatLng(latitude, longitude)
//        // 半径
//        val radius = circleFenceInfo.radius.toDouble()
//        // 去噪精度，则定位精度大于denoise米的轨迹点都不会参与围栏计算。
//        val denoise = 30
//        // 坐标类型
//        val coordType = CoordType.bd09ll
//        // 创建本地圆形围栏
//        val localCircleFenceRequest = CreateFenceRequest.buildLocalCircleRequest(
//                getTag(),
//                serviceId,
//                circleFenceInfo.name,
//                myEntityName,
//                center,
//                radius,
//                denoise,
//                coordType
//        )
//        // 创建本地圆形围栏
//        mTraceClient.createFence(localCircleFenceRequest, object : OnFenceListenerAdapter() {
//            override fun onCreateFenceCallback(response: CreateFenceResponse) {
//                // 创建围栏响应结果,能获取围栏的一些信息
//                if (StatusCodes.SUCCESS != response.getStatus()) {
//                    Toast.makeText(context, "创建本地围栏失败", Toast.LENGTH_SHORT).show()
//                    return
//                }
//                circleFenceInfo.id = response.fenceId// 创建的围栏id
//                Log.i(TAG, "创建本地围栏成功：$circleFenceInfo。开始创建覆盖物。")
//                circleFenceInfo.createOverlay(baiduMap)
//            }
//        })
//    }
//
//    private fun getTag() = mSequenceGenerator.incrementAndGet()
//
//    private fun getCircleFenceInfoByFenceId(fenceId: Long): CircleFenceInfo? {
//        val filter = mFenceInfoList.filter { it.id == fenceId }
//        return if (filter.isNotEmpty()) {
//            filter[0]
//        } else {
//            null
//        }
//    }
//
//    private fun getFenceIds(): List<Long> {
//        val fenceIds = mutableListOf<Long>()
//        mFenceInfoList.mapTo(fenceIds) {
//            Log.w(TAG, it.toString())
//            it.id
//        }
//        return fenceIds
//    }
//
//}
