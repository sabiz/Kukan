package jp.sabiz.kukan

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import jp.sabiz.kukan.common.Kiosk

class AdminReceiver: DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Kiosk(context).setLockTaskPackage()
    }
}