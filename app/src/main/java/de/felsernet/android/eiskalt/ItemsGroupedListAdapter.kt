package de.felsernet.android.eiskalt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

class ItemsGroupedListAdapter<T : BaseDataClass, VH : BaseViewHolder<T>>(
    @LayoutRes private val itemLayoutId: Int,
    private val itemViewHolderFactory: (View) -> VH
) : GenericListAdapter<T, VH>(
        layoutId = itemLayoutId,
        viewHolderFactory = itemViewHolderFactory
    ) {

    @LayoutRes private val headerLayoutId: Int = R.layout.item_header_row
    private val headerViewHolderFactory: (View) -> BaseViewHolder<Group> = ::ItemHeaderViewHolder

    companion object {
        private const val VIEW_TYPE_CONTENT = 0
        private const val VIEW_TYPE_HEADER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DisplayItem.Content -> VIEW_TYPE_CONTENT
            is DisplayItem.Header  -> VIEW_TYPE_HEADER
        }
    }

    //@Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return when (viewType) {
            VIEW_TYPE_CONTENT -> {
                super.onCreateViewHolder(parent, 0)
            }
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(headerLayoutId, parent, false)
                headerViewHolderFactory(view) as VH
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        when (val item = getItem(position)) {
            is DisplayItem.Content -> holder.bind(item.obj)
            is DisplayItem.Header -> (holder as ItemHeaderViewHolder).bind(item.obj)
        }
    }
}
