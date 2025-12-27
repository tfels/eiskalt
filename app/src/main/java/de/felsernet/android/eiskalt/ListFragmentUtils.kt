package de.felsernet.android.eiskalt

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch

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

    /**
     * Sets up swipe-to-delete functionality with UNDO support and visual feedback
     *
     * @param recyclerView The RecyclerView to attach swipe functionality to
     * @param dataList The mutable list containing the data
     * @param adapter The RecyclerView adapter
     * @param deleteMessage The message to show in the snackbar (e.g., "List deleted")
     * @param deleteFunction The function to call for permanent deletion (database operation)
     */
    fun <T> Fragment.setupSwipeToDelete(
        recyclerView: RecyclerView,
        dataList: MutableList<T>,
        adapter: RecyclerView.Adapter<*>,
        deleteMessage: String,
        deleteFunction: (T) -> Unit
    ) {
        val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)
        val deleteBackground = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.delete_background))

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top

                // Draw the red delete background
                deleteBackground.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                deleteBackground.draw(c)

                // Calculate position for delete icon
                val deleteIconTop = itemView.top + (itemHeight - deleteIcon!!.intrinsicHeight) / 2
                val deleteIconMargin = (itemHeight - deleteIcon.intrinsicHeight) / 2
                val deleteIconLeft = itemView.right - deleteIconMargin - deleteIcon.intrinsicWidth
                val deleteIconRight = itemView.right - deleteIconMargin
                val deleteIconBottom = deleteIconTop + deleteIcon.intrinsicHeight

                // Draw the delete icon
                deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
                deleteIcon.draw(c)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val itemToDelete = dataList[position]

                // Remove from UI immediately
                dataList.removeAt(position)
                adapter.notifyItemRemoved(position)

                // Show UNDO snackbar with enhanced styling
                val snackbar = Snackbar.make(
                    recyclerView,
                    deleteMessage,
                    Snackbar.LENGTH_LONG
                ).setAction("UNDO") {
                    // Undo the deletion
                    dataList.add(position, itemToDelete)
                    adapter.notifyItemInserted(position)
                }

                // Style the snackbar action button
                snackbar.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.white))

                snackbar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        // If snackbar dismissed without UNDO, delete from database
                        if (event != DISMISS_EVENT_ACTION) {
                            lifecycleScope.launch {
                                try {
                                    deleteFunction(itemToDelete)
                                } catch (e: FirebaseFirestoreException) {
                                    handleFirestoreException(requireContext(), e, "delete")
                                }
                            }
                        }
                    }
                })

                snackbar.show()

                // Add haptic feedback
                viewHolder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
}
