package de.felsernet.android.eiskalt

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

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

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                // Flexible update is available
                Log.d(TAG, "Flexible update available")
                startFlexibleUpdate(activity, appUpdateInfo)
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                // Immediate update is available
                Log.d(TAG, "Immediate update available")
                startImmediateUpdate(activity, appUpdateInfo)
            } else {
                Log.d(TAG, "No update available or update type not allowed")
            }
        }

        appUpdateInfoTask.addOnFailureListener { e ->
            Log.e(TAG, "Failed to check for updates", e)
        }
    }

    private fun isAppInstalledFromGooglePlay(): Boolean {
        return try {
            val packageManager = context.packageManager
            val installerPackageName = packageManager.getInstallerPackageName(context.packageName)
            installerPackageName == "com.android.vending"
        } catch (e: Exception) {
            Log.e(TAG, "Error checking installer package", e)
            false
        }
    }

    private fun startFlexibleUpdate(activity: Activity, appUpdateInfo: AppUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                REQUEST_CODE_APP_UPDATE
            )
        } catch (e: IntentSender.SendIntentException) {
            Log.e(TAG, "Failed to start flexible update", e)
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
            Log.e(TAG, "Failed to start immediate update", e)
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_APP_UPDATE) {
            if (resultCode != Activity.RESULT_OK) {
                Log.e(TAG, "Update flow failed! Result code: $resultCode")
            } else {
                Log.d(TAG, "Update flow completed successfully")
            }
        }
    }

    // Note: Listener functionality removed for simplicity
    // The core version check functionality is implemented above
}
