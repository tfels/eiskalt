package de.felsernet.android.eiskalt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.ListAdapter

/**
 * A generic ListAdapter for [DisplayItem]s that use a single layout and view holder.
 */
open class GenericListAdapter<T: BaseDataClass, VH : BaseViewHolder<T>>(
    @LayoutRes private val layoutId: Int,
    private val viewHolderFactory: (View) -> VH
) : ListAdapter<DisplayItem<T>, VH>(DisplayItemDiffCallback<T>()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        if(viewType != 0)
            throw IllegalArgumentException("Unknown viewType type: ${viewType}")

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return viewHolderFactory(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        if (item is DisplayItem.Content) {
            holder.bind(item.obj)
        } else {
            throw IllegalArgumentException("Unknown item type: ${item::class.java.name}")
            // for other cases overwrite this method in derived class
        }
    }
}
