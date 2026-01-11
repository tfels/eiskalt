package de.felsernet.android.eiskalt

import android.view.View
import android.widget.TextView

class ItemViewHolder(itemView: View) : BaseViewHolder<Item>(itemView) {
    val textView: TextView = itemView.findViewById(R.id.textView)

    override fun onBind(holder: BaseViewHolder<Item>, obj: Item) {
        textView.text = obj.name
    }
}