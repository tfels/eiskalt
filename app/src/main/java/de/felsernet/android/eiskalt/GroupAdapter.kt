package de.felsernet.android.eiskalt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupAdapter(
    private val groups: MutableList<Group>,
    private val onGroupClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewGroupName: TextView = itemView.findViewById(R.id.textViewGroupName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.textViewGroupName.text = group.name

        // Set click listener for the entire item (for renaming)
        holder.itemView.setOnClickListener {
            onGroupClick(group)
        }
    }

    override fun getItemCount(): Int = groups.size
}
