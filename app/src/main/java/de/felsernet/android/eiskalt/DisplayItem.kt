package de.felsernet.android.eiskalt

import androidx.recyclerview.widget.DiffUtil

/**
 * Sealed class representing either a group header or a data item in a list
 */
sealed class DisplayItem<out T : BaseDataClass> {
    // we add this member, so we can access the base data independent of the used type
    abstract val data: BaseDataClass

    data class Content<T : BaseDataClass>(val obj: T) : DisplayItem<T>() {
        override val data: BaseDataClass = obj
    }

    // "Nothing" is a subclass of all, so it is compatible to DisplayItem<T>
    data class Header<T_H : BaseDataClass>(val obj: T_H) : DisplayItem<Nothing>() {
        override val data: BaseDataClass = obj
    }
}

class DisplayItemDiffCallback<T : BaseDataClass> : DiffUtil.ItemCallback<DisplayItem<T>>() {
    override fun areItemsTheSame(oldItem: DisplayItem<T>, newItem: DisplayItem<T>): Boolean = oldItem.data.id == newItem.data.id

    // We rely on the implementing classes being data classes for proper equality check
    override fun areContentsTheSame(oldItem: DisplayItem<T>, newItem: DisplayItem<T>): Boolean = oldItem == newItem
}
