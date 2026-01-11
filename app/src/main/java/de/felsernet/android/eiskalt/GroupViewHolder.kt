package de.felsernet.android.eiskalt

import android.view.View
import android.widget.TextView

class GroupViewHolder(itemView: View) : BaseViewHolder<Group>(itemView) {
    val textViewGroupName: TextView = itemView.findViewById(R.id.textViewGroupName)

    override fun onBind(holder: BaseViewHolder<Group>, obj: Group) {
        textViewGroupName.text = obj.name
    }
}