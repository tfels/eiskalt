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

    // Type safe grouping key for items
    sealed class GroupKey {
        data object NoGroup : GroupKey()
        data object UnknownGroup : GroupKey()
        data class ValidGroup(val id: String) : GroupKey()
    }

    /**
     * Rebuild the grouped display list from source objectList using the current group names.
     */
    fun updateGroupedRowList() {
        displayRows.clear()

        // create a map with grouped items by their groupId using type safe keys
        val grouped = objectList.groupBy { obj ->
            when (val id = obj.groupId) {
                null         -> GroupKey.NoGroup
                !in groupMap -> GroupKey.UnknownGroup
                else         -> GroupKey.ValidGroup(id)
            }
        }

        // Sort the groups properly: first NoGroup, then named groups sorted by name, then UnknownGroup last
        val sortedGroupEntries = grouped.toList().sortedBy { (key, _) ->
            when (key) {
                GroupKey.NoGroup -> ""          // first
                GroupKey.UnknownGroup -> "zzz"  // last
                is GroupKey.ValidGroup -> groupMap[key.id]?.name?.lowercase() ?: "zzz"
            }
        }

        // add to our list
        for ((groupKey, items) in sortedGroupEntries) {
            val group = when(groupKey) {
                GroupKey.NoGroup -> Group("no group")
                GroupKey.UnknownGroup -> Group("unknown")
                is GroupKey.ValidGroup -> groupMap[groupKey.id]!!
            }

            displayRows.add(ListRow.HeaderRow(group))
            items.sortedBy { it.name.lowercase() }.forEach { item ->
                displayRows.add(ListRow.ItemRow(item))
            }
        }
    }
}
