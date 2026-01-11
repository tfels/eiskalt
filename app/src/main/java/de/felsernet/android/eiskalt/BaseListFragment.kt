package de.felsernet.android.eiskalt

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Base fragment class for all list fragments that provides common functionality
 * like authentication state observation and swipe-to-delete setup.
 */
abstract class BaseListFragment<T> : Fragment() {

    protected var hasDataLoaded = false
    protected var objectsList: MutableList<T> = mutableListOf()
    protected lateinit var adapter: RecyclerView.Adapter<*>
    protected abstract val recyclerView: RecyclerView
    protected abstract val fabView: View
    protected abstract val deleteMessage: String
    @get:LayoutRes
    protected abstract val adapterLayoutId: Int
    protected abstract val adapterViewHolderFactory: (View) -> BaseViewHolder<T>

    protected abstract fun loadData()
    protected abstract fun onClickAdd()
    protected abstract suspend fun onSwipeDelete(item: T)
    protected abstract fun onClickObject(item: T)

    /**
     * Set up list functionality including FAB click listener, adapter assignment, and swipe-to-delete functionality
     */
    protected fun setupListFunctionality() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = GenericListAdapter(
            objectsList,
            adapterLayoutId,
            adapterViewHolderFactory,
            onClick = ::onClickObject
        )

        // Assign adapter to RecyclerView
        recyclerView.adapter = adapter

        fabView.setOnClickListener {
            onClickAdd()
        }

        // Set up swipe-to-delete functionality
        setupSwipeToDelete()
    }

    override fun onStart() {
        super.onStart()
        setupAuthStateObserver {
            if (!hasDataLoaded) {
                loadData()
                hasDataLoaded = true
            }
        }
    }

    override fun onStop() {
        super.onStop()
        AuthManager.authState.removeObservers(viewLifecycleOwner)
    }

    /**
     * Set up auth state observation
     */
    private fun setupAuthStateObserver(onAuthenticated: () -> Unit) {
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
     * Handle Firebase Firestore exceptions with consistent error messages
     */
    fun handleFirestoreException(context: Context, e: FirebaseFirestoreException, operation: String = "load data") {
        AppUtils.handleFirestoreException(context, e, operation)
    }

    /**
     * Set up swipe-to-delete functionality with UNDO support and visual feedback
     */
    private fun setupSwipeToDelete() {
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
                val itemToDelete = objectsList[position]

                // Remove from UI immediately
                objectsList.removeAt(position)
                adapter.notifyItemRemoved(position)

                // Show UNDO snackbar with enhanced styling
                val snackbar = Snackbar.make(
                    recyclerView,
                    deleteMessage,
                    Snackbar.LENGTH_LONG
                ).setAction("UNDO") {
                    // Undo the deletion
                    objectsList.add(position, itemToDelete)
                    adapter.notifyItemInserted(position)
                }

                // Style the snackbar action button
                snackbar.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.white))

                snackbar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        // If snackbar dismissed without UNDO, delete from database
                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                            requireActivity().lifecycleScope.launch {
                                try {
                                    onSwipeDelete(itemToDelete)
                                } catch (e: FirebaseFirestoreException) {
                                    try {
                                        requireActivity().runOnUiThread {
                                            handleFirestoreException(requireActivity(), e, "delete")
                                        }
                                    } catch (e: Exception) {
                                        // Fragment is detached, can't show Toast
                                    }
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
