package de.felsernet.android.eiskalt.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import de.felsernet.android.eiskalt.log.AppLog

class VersionCheckManager(private val context: Context) {
    companion object {
        private const val TAG = "VersionCheckManager"
        private const val REQUEST_CODE_APP_UPDATE = 1001
    }

    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(context)
    }

    fun checkForUpdates(activity: Activity) {
        // Check if the app is installed from Google Play
        if (!isAppInstalledFromGooglePlay()) {
            Log.d(TAG, "App not installed from Google Play, skipping version check")
            return
        }

        // Check for available updates
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Only immediate updates: Play shows a full screen flow that downloads, installs and
        // restarts the app by itself. (A flexible update would need a listener and a call of
        // completeUpdate() after the download, otherwise it is never installed.)
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val availability = appUpdateInfo.updateAvailability()
            if (availability == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                Log.d(TAG, "Immediate update available")
                startImmediateUpdate(activity, appUpdateInfo)
            } else if (availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // an immediate update was interrupted (e.g. the app was closed), continue it
                Log.d(TAG, "Resuming the interrupted update")
                startImmediateUpdate(activity, appUpdateInfo)
            } else {
                Log.d(TAG, "No update available or update type not allowed")
            }
        }

        appUpdateInfoTask.addOnFailureListener { e ->
            AppLog.e(TAG, "Failed to check for updates", e)
        }
    }

    private fun isAppInstalledFromGooglePlay(): Boolean {
        return try {
            val packageManager = context.packageManager
            val installerPackageName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                // replaced by getInstallSourceInfo() since API 30, still the only way below
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(context.packageName)
            }
            installerPackageName == "com.android.vending"
        } catch (e: Exception) {
            AppLog.e(TAG, "Error checking installer package", e)
            false
        }
    }

    private fun startImmediateUpdate(activity: Activity, appUpdateInfo: AppUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                REQUEST_CODE_APP_UPDATE
            )
        } catch (e: IntentSender.SendIntentException) {
            AppLog.e(TAG, "Failed to start immediate update", e)
        }
    }

    // RESULT_OK is usually not received, the app is restarted by the update.
    // RESULT_CANCELED: the user cancelled the update, the app continues with the old version.
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_APP_UPDATE) {
            when (resultCode) {
                Activity.RESULT_OK -> Log.d(TAG, "Update flow completed successfully")
                Activity.RESULT_CANCELED -> AppLog.i(TAG, "Update cancelled by the user")
                else -> AppLog.e(TAG, "Update flow failed! Result code: $resultCode")
            }
        }
    }
}
