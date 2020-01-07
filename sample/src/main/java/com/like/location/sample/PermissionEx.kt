package com.like.location.sample

import android.app.Activity
import androidx.core.app.ActivityCompat
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

/**
 * 运行时动态请求危险权限。包括：<br/>
 * CALENDAR     |   READ_CALENDAR、WRITE_CALENDAR<br/>
 * CAMERA       |   CAMERA<br/>
 * CONTACTS     |   READ_CONTACTS、WRITE_CONTACTS、GET_ACCOUNTS<br/>
 * LOCATION     |   ACCESS_FINE_LOCATION、ACCESS_COARSE_LOCATION<br/>
 * MICROPHONE   |   RECORD_AUDIO<br/>
 * PHONE        |   READ_PHONE_STATE、CALL_PHONE、READ_CALL_LOG、WRITE_CALL_LOG、ADD_VOICEMAIL、USE_SIP、PROCESS_OUTGOING_CALLS<br/>
 * SENSORS      |   BODY_SENSORS<br/>
 * SMS          |   SEND_SMS、RECEIVE_SMS、READ_SMS、RECEIVE_WAP_PUSH、RECEIVE_MMS<br/>
 * STORAGE      |   READ_EXTERNAL_STORAGE、WRITE_EXTERNAL_STORAGE<br/>
 * <p>
 * 注意：<br/>
 * ①同一组权限中，申请了其中一个，则此组中所有权限都申请了。<br/>
 * ②申请的权限必须在AndroidManifest.xml中申明，否则不会弹出系统权限授权的对话框。<br/>
 */

/**
 * 检查全新并执行代码
 *
 * @param rationale         需要权限的理由
 * @param requestCode       请求码
 * @param block             权限通过后需要执行的代码
 * @param permissions       需要申请的权限
 */
fun Activity.checkPermissionsAndRun(rationale: String, requestCode: Int, block: () -> Unit, vararg permissions: String) {
    if (!EasyPermissions.hasPermissions(this, *permissions)) {
        // 选择了拒绝并且不再提醒
        if (permissions.any { !ActivityCompat.shouldShowRequestPermissionRationale(this, it) }) {
            AppSettingsDialog.Builder(this)
                    .setTitle("权限申请失败")
                    .setRationale("我们需要的一些权限被您拒绝，导致功能无法正常使用。请您到设置页面手动授权！")
                    .build()
                    .show()
        } else {
            EasyPermissions.requestPermissions(this, rationale, requestCode, *permissions)
        }
    } else {
        block.invoke()
    }
}
