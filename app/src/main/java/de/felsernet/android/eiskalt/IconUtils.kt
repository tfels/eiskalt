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
            IconType.UNKNOWN -> {
                imageViewIcon.visibility = View.GONE
                error("Unknown icon type. Corrupt DB?")
                // sharedMessageViewModel.showErrorMessage("unknown icon type")
                // return false
            }
        }
    }
}
