package de.felsernet.android.eiskalt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class GenericListAdapter<T: BaseDataClass, VH : BaseViewHolder<T>>(
    @LayoutRes private val layoutId: Int,
    private val viewHolderFactory: (View) -> VH
) : ListAdapter<T, VH>(BaseDiffCallback<T>()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return viewHolderFactory(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}

class BaseDiffCallback<T : BaseDataClass> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean = oldItem.id == newItem.id
    
    // We rely on the implementing classes being data classes for proper equality check
    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = oldItem == newItem
}
