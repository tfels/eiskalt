package de.felsernet.android.eiskalt

import android.util.Log
import android.view.View
import android.widget.ImageView
import coil.load

object IconUtils {
    fun loadAndSetIconAsync(iconInfo: IconInfo, imageViewIcon: ImageView) {
        when (iconInfo.type) {
            IconType.ASSET -> {
                // Load image from assets handled by coil
                imageViewIcon.load("file:///android_asset/${iconInfo.path}") {
                    listener(
                        onSuccess = { _, _ -> imageViewIcon.visibility = View.VISIBLE },
                        onError = { _, result -> imageViewIcon.visibility = View.GONE
                            Log.w("icon", "CoilError: ${result.throwable.message}")
                        }
                    )
                }
            }
            IconType.UNKNOWN -> {
                imageViewIcon.visibility = View.GONE
                error("Unknown icon type. Corrupt DB?")
                // sharedMessageViewModel.showErrorMessage("unknown icon type")
                // return false
            }
        }
    }
}
