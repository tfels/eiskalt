package de.felsernet.android.eiskalt

import android.view.View
import android.widget.TextView

class ListViewHolder(itemView: View) : BaseViewHolder<ListInfo>(itemView) {
    val textView: TextView = itemView.findViewById(R.id.textView)
    val textViewItemCount: TextView = itemView.findViewById(R.id.textViewItemCount)

    override fun onBind(holder: BaseViewHolder<ListInfo>, obj: ListInfo) {
        textView.text = obj.name
        textViewItemCount.text = "${obj.itemCount}"
    }
}
