package de.felsernet.android.eiskalt.item

import android.view.View
import android.widget.ImageButton
import de.felsernet.android.eiskalt.R
import de.felsernet.android.eiskalt.base.BaseViewHolder
import de.felsernet.android.eiskalt.group.Group

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
