package de.felsernet.android.eiskalt.item

import android.view.View
import de.felsernet.android.eiskalt.base.BaseViewHolder

class ItemViewHolder(itemView: View,
                     onClick: (item: Item) -> Unit
) : BaseViewHolder<Item>(itemView, onClick) {
}