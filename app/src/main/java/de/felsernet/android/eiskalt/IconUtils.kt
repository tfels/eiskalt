package de.felsernet.android.eiskalt

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView

object IconUtils {
    fun loadAndSetIcon(iconInfo: IconInfo, context: Context, imageViewIcon: ImageView): Boolean {
        when (iconInfo.type) {
            IconType.ASSET -> {
                // Load image from assets handled by Android's native BitmapFactory
                val input = context.assets.open(iconInfo.path)
                val bitmap = BitmapFactory.decodeStream(input)
                imageViewIcon.setImageBitmap(bitmap)
                return true
            }

            IconType.UNKNOWN -> {
                error("Unknown icon type. Corrupt DB?")
                // sharedMessageViewModel.showErrorMessage("unknown icon type")
                // return false
            }
        }
    }
}