package com.like.location

import com.baidu.trace.api.fence.*

abstract class OnFenceListenerAdapter : OnFenceListener {
    /**
     * 创建围栏回调
     */
    override fun onCreateFenceCallback(response: CreateFenceResponse) {}

    /**
     * 删除围栏回调
     */
    override fun onDeleteFenceCallback(response: DeleteFenceResponse) {}

    /**
     * 查询监控状态回调
     */
    override fun onMonitoredStatusCallback(response: MonitoredStatusResponse) {}

    /**
     * 查询围栏列表回调
     */
    override fun onFenceListCallback(response: FenceListResponse) {}

    /**
     * 更新围栏回调
     */
    override fun onUpdateFenceCallback(response: UpdateFenceResponse) {}

    /**
     * 查询指定位置监控状态回调
     */
    override fun onMonitoredStatusByLocationCallback(response: MonitoredStatusByLocationResponse) {}

    /**
     * 查询历史报警回调
     */
    override fun onHistoryAlarmCallback(response: HistoryAlarmResponse) {}

}
