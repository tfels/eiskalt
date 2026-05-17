package de.felsernet.android.eiskalt

import android.view.View
import android.widget.ImageButton

class ItemHeaderViewHolder(
    itemView: View,
    private val onToggleExpand: (group: Group) -> Unit
) : BaseViewHolder<Group>(itemView) {
    private val imageButtonFold: ImageButton = itemView.findViewById(R.id.imageButtonFold)

    fun bind(group: Group, isCollapsed: Boolean) {
        super.bind(group)

        imageButtonFold.setImageResource(
            if (isCollapsed)
                R.drawable.ic_expand_more
            else
                R.drawable.ic_expand_less
        )

        imageButtonFold.setOnClickListener {
            onToggleExpand(group)
        }
    }

    override fun isSwipeAllowed(): Boolean = false
}
