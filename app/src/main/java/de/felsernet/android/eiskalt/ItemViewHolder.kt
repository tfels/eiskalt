package de.felsernet.android.eiskalt

import android.view.View
import android.widget.TextView

class ItemViewHolder(itemView: View,
                     onClick: (item: Item) -> Unit
) : BaseViewHolder<Item>(itemView, onClick) {
    val textViewItemQuantity: TextView = itemView.findViewById(R.id.textViewCount)

    override fun bind(obj: Item) {
        super.bind(obj)
        textViewItemQuantity.text = "${obj.quantity}"
    }
}