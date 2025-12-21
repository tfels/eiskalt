package de.felsernet.android.eiskalt

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestoreException

object ListFragmentUtils {

    /**
     * Sets up auth state observation for fragments that need authentication
     */
    fun Fragment.setupAuthStateObserver(onAuthenticated: () -> Unit) {
        AuthManager.authState.observe(viewLifecycleOwner, Observer { authState ->
            when (authState) {
                is AuthManager.AuthState.Authenticated -> {
                    onAuthenticated()
                }
                is AuthManager.AuthState.Unauthenticated -> {
                    Toast.makeText(requireContext(), "Please sign in to access data", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    /**
     * Handles Firebase Firestore exceptions with consistent error messages
     */
    fun handleFirestoreException(context: Context, e: FirebaseFirestoreException, operation: String = "load data") {
        when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                Toast.makeText(context, "Cloud access denied. App cannot $operation.", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(context, "Failed to $operation", Toast.LENGTH_SHORT).show()
                throw e
            }
        }
    }


}

/**
 * Base class for swipe-to-delete functionality with visual feedback
 */
abstract class BaseSwipeToDeleteCallback : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {

    private lateinit var deleteIcon: android.graphics.drawable.Drawable
    private lateinit var background: android.graphics.drawable.ColorDrawable

    override fun onMove(
        recyclerView: androidx.recyclerview.widget.RecyclerView,
        viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
        target: androidx.recyclerview.widget.RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    protected fun initVisualFeedback(context: android.content.Context) {
        deleteIcon = androidx.core.content.ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete)!!
        background = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#FFAB91"))
    }

    override fun onChildDraw(
        c: android.graphics.Canvas,
        recyclerView: androidx.recyclerview.widget.RecyclerView,
        viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (!::deleteIcon.isInitialized) {
            initVisualFeedback(recyclerView.context)
        }

        val itemView = viewHolder.itemView
        val iconMargin = (itemView.height - deleteIcon.intrinsicHeight) / 2
        val iconTop = itemView.top + (itemView.height - deleteIcon.intrinsicHeight) / 2
        val iconBottom = iconTop + deleteIcon.intrinsicHeight

        if (dX < 0) { // Swiping to the left
            val iconLeft = itemView.right - iconMargin - deleteIcon.intrinsicWidth
            val iconRight = itemView.right - iconMargin
            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

            background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
        } else { // view is unSwiped
            background.setBounds(0, 0, 0, 0)
        }

        background.draw(c)
        deleteIcon.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
