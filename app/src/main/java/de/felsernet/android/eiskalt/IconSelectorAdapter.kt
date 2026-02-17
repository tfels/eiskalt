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
    companion object {
        private val ICON_INFO_NO_SELECTION = IconInfo(IconType.R_DRAWABLE, R.drawable.ic_no_selection.toString())
    }

    private var iconList: List<IconInfo> = emptyList()
    private var selectedPosition: Int = -1
    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        // Get all list icons from assets at runtime
        iconList = getAssetListIcons(recyclerView.context, prefix)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
        super.onDetachedFromRecyclerView(recyclerView)
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

    fun setSelectedIcon(iconInfo: IconInfo?) {
        // Create a local shadow variable
        val finalIconInfo = iconInfo ?: ICON_INFO_NO_SELECTION
        val position = iconList.indexOf(finalIconInfo)
        if (position != -1 && position != selectedPosition) {
            val previousPosition = selectedPosition
            selectedPosition = position
            if (previousPosition != -1) {
                notifyItemChanged(previousPosition)
            }
            notifyItemChanged(selectedPosition)
            // Scroll to make the selected item visible
            recyclerView?.scrollToPosition(selectedPosition)
        }
    }

    fun getSelectedIcon(): IconInfo? {
        return if (selectedPosition > 0) iconList[selectedPosition] else null
    }

    // get a list of IconInfo object
    private fun getAssetListIcons(context: Context, prefix: String): List<IconInfo> {
        val icons = mutableListOf<IconInfo>()

        // Add "null" icon at the front to allow selecting no icon
        icons.add(ICON_INFO_NO_SELECTION)

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

            IconUtils.loadAndSetIconAsync(iconInfo, imageViewIcon)

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