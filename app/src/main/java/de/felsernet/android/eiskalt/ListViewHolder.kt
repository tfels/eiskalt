package de.felsernet.android.eiskalt

import android.view.View
import android.widget.TextView

class ListViewHolder(itemView: View) : BaseViewHolder<ListInfo>(itemView) {
    val textViewItemCount: TextView = itemView.findViewById(R.id.textViewItemCount)

    override fun bind(holder: BaseViewHolder<ListInfo>, obj: ListInfo) {
        super.bind(holder, obj)
        textViewItemCount.text = "${obj.itemCount}"
    }
}
