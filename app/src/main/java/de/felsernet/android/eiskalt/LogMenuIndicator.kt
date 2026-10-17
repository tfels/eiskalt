package de.felsernet.android.eiskalt

import android.graphics.drawable.LayerDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

/**
 * Shows a red dot on the overflow icon and on the "Log" menu item
 * while there are unseen errors in the AppLog.
 */
class LogMenuIndicator(private val activity: AppCompatActivity, private val toolbar: Toolbar) {

    private val defaultIcon = toolbar.overflowIcon
    private val indicatorIcon = LayerDrawable(arrayOf(defaultIcon, ContextCompat.getDrawable(activity, R.drawable.menu_indicator_dot))).apply {
        setLayerGravity(1, Gravity.TOP or Gravity.END)
    }

    fun attach() {
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppLog.hasUnseenErrors.collect { hasUnseenErrors ->
                    toolbar.overflowIcon = if (hasUnseenErrors) indicatorIcon else defaultIcon
                    activity.invalidateOptionsMenu()
                }
            }
        }
    }

    /**
     * Call from onPrepareOptionsMenu: appends a colored dot to the "Log" menu title
     */
    fun updateMenu(menu: Menu) {
        val logItem = menu.findItem(R.id.action_log) ?: return
        if (AppLog.hasUnseenErrors.value) {
            val title = SpannableString(activity.getString(R.string.action_log) + " ●")
            val color = ContextCompat.getColor(activity, R.color.log_error)
            title.setSpan(ForegroundColorSpan(color), title.length - 1, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            logItem.title = title
        } else {
            logItem.setTitle(R.string.action_log)
        }
    }
}
