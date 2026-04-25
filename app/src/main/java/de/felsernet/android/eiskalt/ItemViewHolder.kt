package de.felsernet.android.eiskalt

import android.view.View

class ItemViewHolder(itemView: View,
                     onClick: (item: Item) -> Unit
) : BaseViewHolder<Item>(itemView, onClick) {
}