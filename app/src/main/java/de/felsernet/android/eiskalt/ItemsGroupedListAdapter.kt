package de.felsernet.android.eiskalt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.runBlocking
import kotlin.text.lowercase

class ItemsGroupedListAdapter (
    private val objectList: MutableList<Item>,
    private val onClick: (item: Item) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val displayRows: MutableList<ListRow> = mutableListOf()
    @LayoutRes private val layoutId: Int = R.layout.item_row
    private val viewHolderFactory: (View) -> ItemViewHolder = { view: View ->
        ItemViewHolder(view, onClick)
    }

    /**
     * Sealed class representing either a group header or an item in the list
     */
    sealed class ListRow {
        data class HeaderRow(val header: Group) : ListRow()
        data class ItemRow(val item: Item) : ListRow()
    }

    @LayoutRes private val headerLayoutId: Int = R.layout.item_header_row
    private val headerViewHolderFactory: (View) -> RecyclerView.ViewHolder = ::ItemHeaderViewHolder

    private val groupMap: Map<String, Group> = runBlocking  {
        GroupRepository.getInstance().getAll()
            .associate { it.id to it }
    }

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (displayRows[position]) {
            is ListRow.HeaderRow -> VIEW_TYPE_HEADER
            is ListRow.ItemRow -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(headerLayoutId, parent, false)
                headerViewHolderFactory(view)
            }
            VIEW_TYPE_ITEM -> {
                val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
                viewHolderFactory(view)
            }
        else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val listRow = displayRows[position]) {
            is ListRow.HeaderRow -> {
                (holder as ItemHeaderViewHolder).bind(listRow.header)
            }

            is ListRow.ItemRow -> {
                (holder as ItemViewHolder).bind(listRow.item)
            }
        }
    }

    override fun getItemCount(): Int = displayRows.size


    /**
     * Rebuild the grouped display list from source objectList using the current group names.
     */
    fun updateGroupedRowList() {
        displayRows.clear()

        // create a map with grouped items by their groupId
        val grouped = objectList.groupBy { obj ->
            val id = obj.groupId
            if (id == null) ""
            else if (id !in groupMap) "unknown"
            else id
        }

        // Sort the groups by group name
        val sortedGroupEntries = grouped.toList().sortedBy { (groupId, _) ->
            if (groupId.isEmpty()) ""
            else if (groupId == "Unknown") "zzz" // push Unknown to bottom
            else groupMap[groupId]?.name?.lowercase() ?: "zzz"
        }

        // add to our list
        for ((groupId, items) in sortedGroupEntries) {
            val group = if (groupId.isEmpty()) Group("no group")
                        else groupMap[groupId] ?: Group("unknown")
            displayRows.add(ListRow.HeaderRow(group))
            items.sortedBy { it.name.lowercase() }.forEach { item ->
                displayRows.add(ListRow.ItemRow(item))
            }
        }
    }
}
