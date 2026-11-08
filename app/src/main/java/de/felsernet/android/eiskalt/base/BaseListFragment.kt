package de.felsernet.android.eiskalt.base

import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.common.AuthManager
import de.felsernet.android.eiskalt.common.SharedMessageViewModel
import de.felsernet.android.eiskalt.household.HouseholdRepository
import de.felsernet.android.eiskalt.household.HouseholdState
import de.felsernet.android.eiskalt.household.HouseholdSwitchDialogFragment
import kotlinx.coroutines.launch

/**
 * Base fragment class for all list fragments that provides common functionality
 * like loading the data of the active household and swipe-to-delete setup.
 */
abstract class BaseListFragment<T: BaseDataClass> : Fragment() {
    // Shared ViewModel for handling messages across fragments
    protected val sharedMessageViewModel: SharedMessageViewModel by activityViewModels()
    abstract val viewModel: BaseViewModel<T>
    // the household the shown data belongs to, null before the first load
    private var loadedHouseholdId: String? = null

    // UI elements
    protected lateinit var fabAdd: FloatingActionButton
    protected lateinit var recyclerView: RecyclerView
    protected lateinit var adapter: ListAdapter<DisplayItem<T>, BaseViewHolder<T>>

    @get:StringRes
    protected abstract val deleteMessageRes: Int
    @get:LayoutRes
    protected abstract val adapterLayoutId: Int
    protected abstract val adapterViewHolderFactory: (View) -> BaseViewHolder<T>

    protected open fun initializeViewModel() { viewModel.initialize(sharedMessageViewModel) }
    protected open fun loadData() { viewModel.loadData() }
    protected abstract fun onClickAdd()

    protected open fun createAdapter(): ListAdapter<DisplayItem<T>, BaseViewHolder<T>> {
        return GenericListAdapter(
            adapterLayoutId,
            adapterViewHolderFactory,
        )
    }

    /**
     * Set up list functionality including FAB click listener, adapter assignment, and swipe-to-delete functionality
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fabAdd = view.findViewById<FloatingActionButton>(R.id.fabAdd)
        recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        // optional: shown while the data is loaded, or a message if the household can't be used
        val loadingView = view.findViewById<View?>(R.id.loadingView)
        val loadingProgress = view.findViewById<View?>(R.id.loadingProgress)
        val loadingText = view.findViewById<TextView?>(R.id.loadingText)
        val loadingAction = view.findViewById<Button?>(R.id.loadingAction)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = createAdapter()

        // Assign adapter to RecyclerView
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            onClickAdd()
        }

        // Set up swipe-to-delete functionality
        setupSwipeToDelete()

        // Collect messages from the shared ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // collect concurrently so both collectors run
                launch {
                    sharedMessageViewModel.errorMessage.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                }
                launch {
                    sharedMessageViewModel.successMessage.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }

                // Hide the loading view only when the loaded items are really shown:
                // called when the adapter has taken over a submitted list (the changes are computed
                // in the background), the rows are drawn in the next frame.
                // Every submitList() gets this callback: if a newer list is submitted before the
                // adapter is done, only the callback of the newest one is called.
                fun hideLoadingViewWhenShown() {
                    recyclerView.doOnPreDraw {
                        if (!viewModel.isLoading.value && HouseholdRepository.state.value is HouseholdState.Active)
                            loadingView?.isVisible = false
                    }
                }

                // As long as the household can't be used, show its state instead of the list:
                // the spinner while it is loaded, a message without household, if one has to be
                // chosen (with a button to choose it) or after an error (with a button to retry)
                fun showHouseholdState(state: HouseholdState) {
                    val message = when (state) {
                        is HouseholdState.None -> getString(R.string.household_none_message)
                        is HouseholdState.Choose -> getString(R.string.household_choose_message)
                        is HouseholdState.Error -> getString(R.string.household_error_message)
                        else -> null
                    }
                    loadingProgress?.isVisible = message == null
                    loadingText?.text = message ?: getString(R.string.list_loading)
                    when (state) {
                        is HouseholdState.Choose -> loadingAction?.apply {
                            setText(R.string.household_choose)
                            // same FragmentManager as MainActivity, so the dialog is shown only once
                            setOnClickListener { HouseholdSwitchDialogFragment.show(requireActivity().supportFragmentManager) }
                        }
                        is HouseholdState.Error -> loadingAction?.apply {
                            setText(R.string.retry)
                            setOnClickListener {
                                viewLifecycleOwner.lifecycleScope.launch { HouseholdRepository.loadActiveHousehold() }
                            }
                        }
                        else -> {}
                    }
                    loadingAction?.isVisible = state is HouseholdState.Choose || state is HouseholdState.Error
                    fabAdd.isVisible = state is HouseholdState.Active
                    if (state !is HouseholdState.Active)
                        loadingView?.isVisible = true
                }

                // Load the data as soon as the household of the user is known
                // (loaded by MainActivity after signing in)
                launch {
                    HouseholdRepository.state.collect { state ->
                        showHouseholdState(state)
                        if (state !is HouseholdState.Active)
                            return@collect

                        if (state.household.id != loadedHouseholdId) {
                            // first load, or another household (e.g. signed in with another account)
                            if (loadedHouseholdId != null)
                                viewModel.clearData()
                            initializeViewModel()
                            loadData()
                            loadedHouseholdId = state.household.id
                        } else {
                            // the same household again (e.g. after signing in again): show the list
                            adapter.submitList(viewModel.displayList.value) { hideLoadingViewWhenShown() }
                        }
                    }
                }

                // Collect list changes from ViewModel
                launch {
                    viewModel.displayList.collect { displayItems ->
                        adapter.submitList(displayItems) { hideLoadingViewWhenShown() }
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading)
                            loadingView?.isVisible = true
                        else
                            // submitting the current list again calls back when it is taken over,
                            // also if the loaded list is unchanged (displayList does not emit then)
                            adapter.submitList(viewModel.displayList.value) { hideLoadingViewWhenShown() }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setupAuthStateObserver()
    }

    override fun onStop() {
        super.onStop()
        AuthManager.authState.removeObservers(viewLifecycleOwner)
    }

    /**
     * Set up auth state observation: ask the user to sign in
     * (the data is loaded when the household is known, see onViewCreated)
     */
    private fun setupAuthStateObserver() {
        AuthManager.authState.observe(viewLifecycleOwner, Observer { authState ->
            if (authState is AuthManager.AuthState.Unauthenticated) {
                Toast.makeText(requireContext(), R.string.auth_sign_in_required, Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Set up swipe-to-delete functionality with UNDO support and visual feedback
     */
    private fun setupSwipeToDelete() {
        val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)
        val deleteBackground = ContextCompat.getColor(requireContext(), R.color.delete_background).toDrawable()

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                var swipeFlags = ItemTouchHelper.LEFT
                if (viewHolder is BaseViewHolder<*> && !viewHolder.isSwipeAllowed()) {
                    swipeFlags = 0
                }
                return makeMovementFlags(0, swipeFlags)
            }

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
                // Double check if swipe is actually allowed for this view holder
                if (viewHolder is BaseViewHolder<*> && !viewHolder.isSwipeAllowed()) {
                    adapter.notifyItemChanged(viewHolder.bindingAdapterPosition)
                    return
                }

                val position = viewHolder.bindingAdapterPosition
                val listItem = adapter.currentList[position]

                if (listItem !is DisplayItem.Content) {
                    // Don't swipe headers
                    adapter.notifyItemChanged(position)
                    return
                }
                val itemToDelete = listItem.obj

                // ViewModel handles the "temporary" removal from the list for UI feedback
                viewModel.deleteFromList(itemToDelete)

                // Show UNDO snackbar with enhanced styling
                val snackbar = Snackbar.make(
                    recyclerView,
                    getString(deleteMessageRes),
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.undo) {
                    // Undo the deletion via ViewModel
                    viewModel.undoDeleteFromList(itemToDelete)
                }

                // Style the snackbar action button
                snackbar.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.white))

                snackbar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        // If snackbar dismissed without UNDO, delete from database
                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                            // Perform the delete operation using ViewModel (already removed from list)
                            viewModel.deleteFromDb(itemToDelete)
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
