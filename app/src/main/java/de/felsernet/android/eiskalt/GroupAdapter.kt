package de.felsernet.android.eiskalt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupAdapter(
    private var groups: MutableList<Group>,
    private val onActionClick: (Group, String) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewGroupName: TextView = itemView.findViewById(R.id.textViewGroupName)
        val buttonRename: Button = itemView.findViewById(R.id.buttonRenameGroup)
        val buttonDelete: Button = itemView.findViewById(R.id.buttonDeleteGroup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.textViewGroupName.text = group.name

        holder.buttonRename.setOnClickListener {
            onActionClick(group, "rename")
        }

        holder.buttonDelete.setOnClickListener {
            onActionClick(group, "delete")
        }
    }

    override fun getItemCount(): Int = groups.size

    fun updateGroups(newGroups: MutableList<Group>) {
        groups.clear()
        groups.addAll(newGroups)
        notifyDataSetChanged()
    }
}
