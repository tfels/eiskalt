package de.felsernet.android.eiskalt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Sealed class representing list items in the grouped item list.
 */
sealed class ListItem {
    data class Header(val groupName: String) : ListItem()
    data class ItemRow(val item: Item) : ListItem()
}

/**
 * ViewHolder for group header rows.
 */
class GroupHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textViewGroupName: TextView = itemView.findViewById(R.id.textViewGroupName)

    fun bind(header: ListItem.Header) {
        textViewGroupName.text = header.groupName
    }
}

/**
 * Adapter for the item list that supports grouped sections with headers.
 * The grouped display list is built from the source objectsList on demand.
 */
class ItemListAdapter(
    private val sourceList: MutableList<Item>,
    private val groupNameMapProvider: () -> Map<String, String>,
    private val onClick: (item: Item) -> Unit,
    private val onLongClick: ((item: Item) -> Boolean)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val displayItems: MutableList<ListItem> = mutableListOf()

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    /**
     * Rebuild the grouped display list from sourceList using the current group names.
     */
    fun refreshDisplayList() {
        displayItems.clear()
        val groupNameMap = groupNameMapProvider()

        val grouped = sourceList.groupBy { it.groupId ?: "" }
        val sortedGroupEntries = grouped.toList().sortedBy { (groupId, _) ->
            if (groupId.isEmpty()) ""
            else groupNameMap[groupId]?.lowercase() ?: groupId.lowercase()
        }

        for ((groupId, items) in sortedGroupEntries) {
            val groupName = if (groupId.isEmpty()) {
                "Other"
            } else {
                groupNameMap[groupId] ?: "Unknown"
            }
            displayItems.add(ListItem.Header(groupName))
            items.sortedBy { it.name.lowercase() }.forEach { item ->
                displayItems.add(ListItem.ItemRow(item))
            }
        }
    }

    fun getDisplayItem(position: Int): ListItem = displayItems[position]

    fun removeDisplayItem(position: Int): ListItem = displayItems.removeAt(position)

    override fun getItemViewType(position: Int): Int {
        return when (displayItems[position]) {
            is ListItem.Header -> VIEW_TYPE_HEADER
            is ListItem.ItemRow -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.group_header_row, parent, false)
                GroupHeaderViewHolder(view)
            }
            VIEW_TYPE_ITEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_row, parent, false)
                ItemViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val listItem = displayItems[position]) {
            is ListItem.Header -> {
                (holder as GroupHeaderViewHolder).bind(listItem)
            }
            is ListItem.ItemRow -> {
                val itemHolder = holder as ItemViewHolder
                itemHolder.bind(itemHolder, listItem.item)

                itemHolder.itemView.setOnClickListener {
                    val adapterPosition = itemHolder.bindingAdapterPosition
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        val clickedItem = (displayItems[adapterPosition] as ListItem.ItemRow).item
                        onClick(clickedItem)
                    }
                }

                itemHolder.itemView.setOnLongClickListener {
                    val adapterPosition = itemHolder.bindingAdapterPosition
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        val longClickedItem = (displayItems[adapterPosition] as ListItem.ItemRow).item
                        return@setOnLongClickListener onLongClick?.invoke(longClickedItem) ?: false
                    }
                    false
                }
            }
        }
    }

    override fun getItemCount(): Int = displayItems.size
}
