package de.felsernet.android.eiskalt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupListAdapter(
    private val groups: MutableList<Group>,
    private val onClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.onBind(holder, group)

        // Set click listener for the entire item (for renaming)
        holder.itemView.setOnClickListener {
            onClick(group)
        }
    }

    override fun getItemCount(): Int = groups.size
}
