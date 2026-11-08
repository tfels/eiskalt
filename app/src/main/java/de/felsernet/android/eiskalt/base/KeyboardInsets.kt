package de.felsernet.android.eiskalt.base

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import kotlin.math.max

/**
 * Keep a scrolling screen with input fields above the navigation bar and the keyboard (call on the
 * root view of the screen, instead of fitsSystemWindows). With edge-to-edge the window is not made
 * smaller for the keyboard any more, so the view gets a bottom margin of the height of the keyboard
 * (or of the navigation bar, if the keyboard is hidden). A margin and not a padding: the ScrollView
 * gets smaller and then scrolls the focused field into view by itself.
 */
fun View.keepAboveKeyboard() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val navigationBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = max(navigationBar, keyboard) }
        insets
    }
}
