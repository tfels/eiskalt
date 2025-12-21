package de.felsernet.android.eiskalt

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch
import de.felsernet.android.eiskalt.databinding.FragmentInventoryListsBinding

/**
 * Fragment for displaying and managing inventory lists.
 */
class InventoryListsFragment : Fragment() {

    private var _binding: FragmentInventoryListsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ListsAdapter
    private var listNames: MutableList<String> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryListsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        AuthManager.authState.observe(viewLifecycleOwner, Observer { authState ->
            when (authState) {
                is AuthManager.AuthState.Authenticated -> {
                    loadLists()
                }
                is AuthManager.AuthState.Unauthenticated -> {
                    Toast.makeText(requireContext(), "Please sign in to access data", Toast.LENGTH_SHORT).show()
                }
            }
        })

        binding.fabAddList.setOnClickListener {
            showCreateListDialog()
        }
    }

    private fun loadLists() {
        lifecycleScope.launch {
            try {
                val repository = InventoryRepository()
                val names = repository.getAllListNames().toMutableList()
                listNames = names
                adapter = ListsAdapter(listNames)
                binding.recyclerView.adapter = adapter

                val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(adapter))
                itemTouchHelper.attachToRecyclerView(binding.recyclerView)
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Toast.makeText(requireContext(), "Cloud access denied. App cannot load data.", Toast.LENGTH_LONG).show()
                } else {
                    throw e
                }
            }
        }
    }

    private fun showCreateListDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_list, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.editTextListName)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Create New List")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val listName = editText.text.toString().trim()
                if (listName.isNotBlank()) {
                    createList(listName)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        editText.addTextChangedListener {
            val isEnabled = it.toString().trim().isNotBlank()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isEnabled
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }

        dialog.show()
    }

    private fun createList(listName: String) {
        lifecycleScope.launch {
            try {
                val repository = InventoryRepository()
                repository.createList(listName)
                listNames.add(listName)
                adapter.notifyItemInserted(listNames.size - 1)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to create list", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class ListsAdapter(private val listNames: MutableList<String>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<ListsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.textView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val listName = listNames[position]
            holder.textView.text = listName
            holder.itemView.setOnClickListener {
                val bundle = Bundle().apply {
                    putString("listName", listName)
                }
                findNavController().navigate(R.id.action_InventoryListsFragment_to_InventoryListFragment, bundle)
            }
        }

        override fun getItemCount(): Int = listNames.size
    }

    inner class SwipeToDeleteCallback(private val adapter: ListsAdapter) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

        private val deleteIcon: Drawable? = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)
        private val background = ColorDrawable(Color.RED)

        override fun onMove(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, target: androidx.recyclerview.widget.RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val listNameToDelete = listNames[position]

            lifecycleScope.launch {
                try {
                    val repository = InventoryRepository()
                    repository.deleteList(listNameToDelete)
                    listNames.removeAt(position)
                    adapter.notifyItemRemoved(position)
                } catch (e: Exception) {
                    // Revert the swipe if deletion failed
                    adapter.notifyItemChanged(position)
                    Toast.makeText(requireContext(), "Failed to delete list", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: androidx.recyclerview.widget.RecyclerView,
            viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val itemView = viewHolder.itemView
            val iconMargin = (itemView.height - deleteIcon!!.intrinsicHeight) / 2
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
