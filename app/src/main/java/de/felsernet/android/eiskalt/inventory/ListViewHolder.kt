package de.felsernet.android.eiskalt.inventory

import android.view.View
import android.widget.TextView
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.base.BaseViewHolder

class ListViewHolder(itemView: View,
                     onClick: (item: ListInfo) -> Unit,
                     onLongClick: (item: ListInfo) -> Boolean
) : BaseViewHolder<ListInfo>(itemView, onClick, onLongClick) {
    val textViewItemCount: TextView = itemView.findViewById(R.id.textViewItemCount)

    override fun bind(obj: ListInfo) {
        super.bind(obj)
        textViewItemCount.text = "${obj.itemCount}"
    }
}
