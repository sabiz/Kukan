package jp.sabiz.kukan.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionChecker {

    companion object {
        private var instance: PermissionChecker? = null

        fun get() = instance ?: synchronized(this) {
            instance ?: PermissionChecker().also { instance = it }
        }
    }

    private var requestPermissionLauncher: ActivityResultLauncher<Array<String>>? = null

    fun setupActivityResultLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        requestPermissionLauncher = launcher
    }

    fun activityResultCallback(result: Map<String, Boolean>) {
        val deniedPermissions = emptyList<String>().toMutableList()
        result.keys.forEach { key ->
            if (result[key] == false) {
                deniedPermissions.add(key)
            }
        }
        if (deniedPermissions.size > 0) {
            requestPermissionLauncher?.launch(deniedPermissions.toTypedArray())
        }
    }

    fun check(activity: Activity) {
        val info = activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = info.requestedPermissions

        val showRequestPermission = emptyList<String>().toMutableList()
        val deniedPermissions = emptyList<String>().toMutableList()

        permissions.forEach {
            activity.let { act ->
                val permission = ContextCompat.checkSelfPermission(act, it)
                if (permission == PackageManager.PERMISSION_DENIED) {
                    val rationale = ActivityCompat.shouldShowRequestPermissionRationale(act, it)
                    if (rationale) {
                        showRequestPermission.add(it)
                    } else {
                        deniedPermissions.add(it)
                    }
                }
            }
        }
        if (deniedPermissions.size > 0) {
            // TODO message & finish
        }
        if (showRequestPermission.size > 0) {
            requestPermissionLauncher?.launch(deniedPermissions.toTypedArray())
        }
    }
}