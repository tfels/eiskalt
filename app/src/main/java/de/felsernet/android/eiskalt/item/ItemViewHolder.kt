package de.felsernet.android.eiskalt.item

import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import de.felsernet.android.eiskalt.base.BaseViewHolder

/**
 * This represents a row of an item.
 * Recently deleted items:
 * - are detectable by the value `deletedAt` being set
 * - are templates for new items
 * - are shown grey and can't be swiped away
 */
class ItemViewHolder(itemView: View,
                     onClick: (item: Item) -> Unit,
                     onLongClick: (item: Item) -> Boolean
) : BaseViewHolder<Item>(itemView, onClick, onLongClick) {

    private var isTemplate = false

    override fun bind(obj: Item) {
        super.bind(obj)
        isTemplate = obj.deletedAt != null
        // not on the row itself: its alpha is used by the RecyclerView animations (fade in when unfolded)
        val alpha = if (isTemplate) 0.5f else 1f
        (itemView as ViewGroup).children.forEach { it.alpha = alpha }
    }

    override fun isSwipeAllowed(): Boolean = !isTemplate
}
