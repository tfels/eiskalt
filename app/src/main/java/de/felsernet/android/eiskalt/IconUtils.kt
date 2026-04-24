package de.felsernet.android.eiskalt

import android.util.Log
import android.view.View
import android.widget.ImageView
import coil.load
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult

object IconUtils {
    private fun createCoilLoadListener(imageView: View) = object : ImageRequest.Listener {
        override fun onSuccess(request: ImageRequest, result: SuccessResult) {
            imageView.visibility = View.VISIBLE
        }
        override fun onError(request: ImageRequest, result: ErrorResult) {
            imageView.visibility = View.GONE
            Log.w("icon", "CoilError: ${result.throwable.message}")
        }
    }

    fun loadAndSetIconAsync(iconInfo: IconInfo, imageViewIcon: ImageView) {
        when (iconInfo.type) {
            IconType.ASSET -> {
                // Load image from assets handled by coil
                imageViewIcon.load("file:///android_asset/${iconInfo.path}") {
                    listener(createCoilLoadListener(imageViewIcon))
                }
            }
            IconType.R_DRAWABLE -> {
                // Load image from res/drawable
                val resId = iconInfo.path.toIntOrNull()
                if (resId != null && resId != 0) {
                    // 0 means the resource was not found
                    imageViewIcon.load(resId) {
                        listener(createCoilLoadListener(imageViewIcon))
                    }
                } else {
                    imageViewIcon.load(R.drawable.ic_not_found)
                    imageViewIcon.visibility = View.VISIBLE
                    Log.w("icon", "ResourceError: Could not find drawable: $resId")
                }
            }
            IconType.UNKNOWN -> {
                imageViewIcon.load(R.drawable.ic_not_found)
                imageViewIcon.visibility = View.VISIBLE
                Log.w("icon", "Unknown icon type. Corrupt DB?")
            }
        }
    }
}
