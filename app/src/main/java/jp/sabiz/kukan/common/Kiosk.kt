package jp.sabiz.kukan.common

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import jp.sabiz.kukan.AdminReceiver

class Kiosk(private val context:Context) {
    private val deviceAdmin = ComponentName(context, AdminReceiver::class.java)
    private val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)

    fun start(activity: Activity) {
        activity.startLockTask()
        if (hasDeviceOwnerPermission()){
            addHomeActivity(activity)
        }
    }

    fun stop(activity: Activity) {
        activity.stopLockTask()
        if(hasDeviceOwnerPermission()) {
            clearHomeActivity()
        }
    }

    fun setLockTaskPackage() {
        devicePolicyManager.setLocationEnabled(deviceAdmin, true)
        devicePolicyManager.setLockTaskPackages(deviceAdmin, arrayOf(context.packageName))
    }

    private fun hasDeviceOwnerPermission() =
        devicePolicyManager.isAdminActive(deviceAdmin) &&
                devicePolicyManager.isDeviceOwnerApp(context.packageName)

    private fun addHomeActivity(activity: Activity) {
        val intentFilter = IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_HOME)
        }
        val home = ComponentName(context, activity::class.java)
        devicePolicyManager.addPersistentPreferredActivity(deviceAdmin, intentFilter, home)
    }

    fun clearHomeActivity() =
        devicePolicyManager.clearPackagePersistentPreferredActivities(
            deviceAdmin, context.packageName)
}