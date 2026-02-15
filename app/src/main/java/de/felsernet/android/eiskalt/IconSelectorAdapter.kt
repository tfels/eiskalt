package de.felsernet.android.eiskalt

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for displaying icon options in a RecyclerView.
 * Uses Coil to load images from assets with support for SVG, ICO, and WebP.
 */
class IconSelectorAdapter(
    private val prefix: String
) : RecyclerView.Adapter<IconSelectorAdapter.IconViewHolder>() {
    private var iconList: List<IconInfo> = emptyList()
    private var selectedPosition: Int = -1

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        // Get all list icons from assets at runtime
        iconList = getAssetListIcons(recyclerView.context, prefix)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.icon_selector, parent, false)

        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(iconList[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = iconList.size

    fun setSelectedIcon(iconInfo: IconInfo) {
        val position = iconList.indexOfFirst { it.path == iconInfo.path }
        if (position != -1 && position != selectedPosition) {
            val previousPosition = selectedPosition
            selectedPosition = position
            if (previousPosition != -1) {
                notifyItemChanged(previousPosition)
            }
            notifyItemChanged(selectedPosition)
        }
    }

    fun getSelectedIcon(): IconInfo? {
        return if (selectedPosition != -1) iconList[selectedPosition] else null
    }

    // get a list of IconInfo object
    private fun getAssetListIcons(context: Context, prefix: String): List<IconInfo> {
        val icons = mutableListOf<IconInfo>()
        val assetManager = context.assets

        try {
            // List all files in the icons directory at runtime
            val iconFiles = assetManager.list("icons") ?: emptyArray()

            // Filter for list icons (files starting with prefix)
            iconFiles.filter { it.startsWith(prefix) }
                .sorted()
                .forEach { filename ->
                    val assetPath = "icons/$filename"
                    icons.add(IconInfo(IconType.ASSET, assetPath))
                }
        } catch (e: Exception) {
            // Error reading assets, return empty list
        }

        return icons
    }

    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewIcon: ImageView = itemView.findViewById(R.id.imageViewIcon)
        private val viewSelectionBorder: View = itemView.findViewById(R.id.viewSelectionBorder)

        fun bind(iconInfo: IconInfo, isSelected: Boolean) {

            IconUtils.loadAndSetIcon(iconInfo, itemView.context, imageViewIcon)

            // Show/hide selection border
            viewSelectionBorder.visibility = if (isSelected) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = bindingAdapterPosition
                if (previousPosition != -1) {
                    notifyItemChanged(previousPosition)
                }
                notifyItemChanged(selectedPosition)
            }
        }
    }
}