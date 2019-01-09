package com.like.location

import android.content.Context
import com.baidu.trace.LBSTraceClient
import com.baidu.trace.api.entity.EntityListRequest
import com.baidu.trace.api.entity.FilterCondition
import com.baidu.trace.api.entity.OnEntityListener
import com.baidu.trace.model.CoordType
import com.like.location.util.SingletonHolder
import java.util.concurrent.atomic.AtomicInteger
import kotlin.jvm.functions.FunctionN

/**
 * 鹰眼轨迹管理工具类
 * 注意：地图只支持Android v4.0以上系统
 *
 * service：
 * 一个service（即鹰眼轨迹服务）对应一个轨迹管理系统，一个service里可管理多个终端设备（即entity），
 * service的唯一标识符是service_id。
 * 一个开发者最多可创建10个service
 *
 * entity：
 * 一个entity代表现实中一个被追踪轨迹的终端设备，它可以是一个人、一辆车或者任何运动物体。
 * 同一个service中，entity以entity_name作为唯一标识。
 * 一个service至多同时管理100万个entity。
 * 鹰眼Web API提供了entity的增、删、改、查接口。
 *
 * fence：
 * fence即地理围栏，是指一定范围（如：圆形、多边形、线型、行政区）的虚拟地理区域。
 * 客户端围栏：
 * 围栏的创建、计算和报警均在SDK完成，在 GPS 定位成功的情况下，无需联网即可完成围栏运算。
 * 可应用于手机终端网络不稳定情况下，仍需实时获取围栏报警的场景。
 * 无需联网即可利用 GPS 轨迹点在手机本地进行围栏计算，APP 可及时收到报警信息进行相应的业务处理。
 * 仅支持圆形围栏
 * 服务端围栏：
 * 围栏的创建、计算和报警的发起都在鹰眼服务端完成，依赖于轨迹点上传至服务端才能进行围栏进算。
 * 相较于客户端围栏，服务端围栏报警推送方式更多样：支持推送至 SDK，也支持推送至开发者的服务端，同时还支持报警信息批量查询
 * 支持圆形、多边形、线型、行政区
 * 当entity进入/离开该区域时，鹰眼将自动推送报警至开发者。开发者接收到报警后，可进行业务处理。
 * 一个entity最多可创建100个私有地理围栏，一个service可创建1000个公共围栏。
 * 鹰眼API和SDK提供了fence的增删改查接口，以及查询被监控者在围栏内/外、查询历史报警信息等接口。
 *
 * track：
 * entity移动所产生的连续轨迹被称为track，track由一系列轨迹点（point）组成。
 * 轨迹点数量无限制。
 * 鹰眼Web API提供了添加轨迹点、批量添加轨迹点、查询历史轨迹接口。使用鹰眼Android SDK时，SDK会根据开发者设定的频率定位，回传轨迹点。
 *
 * @param context
 * @param baiduMap
 * @param serviceId         轨迹服务ID
 * @param myEntityName      设备标识
 * @param gatherInterval    定位周期(单位:秒)
 * 多久定位一次，在定位周期大于15s时，SDK会将定位周期设置为5的倍数，默认为5秒
 * @param packInterval      打包回传周期(单位:秒)
 * 鹰眼为节省电量和流量，并不是定位一次就回传一次数据，而是隔段时间将一批定位数据打包压缩回传。
 * 回传周期最大不要超过定位周期的10倍，回传周期不能小于定位周期，否则回传不生效；回传周期建议设置为定位周期的整数倍。​默认为10秒。
 */
class TraceUtils private constructor(context: Context) {
    companion object : SingletonHolder<TraceUtils>(object : FunctionN<TraceUtils> {
        override val arity: Int = 0 // number of arguments that must be passed to constructor

        override fun invoke(vararg args: Any?): TraceUtils {
            return TraceUtils((args[0] as Context).applicationContext)
        }
    })

    private val mSequenceGenerator = AtomicInteger()
    // 轨迹服务客户端
    val mTraceClient: LBSTraceClient by lazy { LBSTraceClient(context) }

    /**
     * 查询实时位置
     * 根据筛选条件（FilterCondition）查找符合条件的entity列表
     *
     * 1. 查询某一个 entity 的详细信息，包括实时位置
     * 2. 查询所有设备信息和实时位置，如轨迹管理台的entity列表面板
     * 3. 查询在线和离线设备
     *
     * @param entityNames   entity标识列表
     * @param activeTime    指定时间内定位且上传了轨迹点的entity。默认30秒
     */
    fun queryEntityList(serviceId: Long, entityNames: List<String>? = null, activeTime: Int = 30, listener: OnEntityListener) {
        // 过滤条件
        val filterCondition = FilterCondition()
        filterCondition.entityNames = entityNames
        filterCondition.activeTime = (System.currentTimeMillis() / 1000 - activeTime)// 只查询activeTime秒之内活跃的
        // 返回结果坐标类型
        val coordTypeOutput = CoordType.bd09ll
        // 分页索引
        val pageIndex = 1
        // 分页大小
        val pageSize = 1000

        // 创建Entity列表请求实例
        val request = EntityListRequest(mSequenceGenerator.incrementAndGet(), serviceId, filterCondition, coordTypeOutput, pageIndex, pageSize)

        // 查询Entity列表
        mTraceClient.queryEntityList(request, listener)
    }

}
