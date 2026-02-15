package de.felsernet.android.eiskalt

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.widget.ImageView
import coil.imageLoader
import coil.load
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult

object IconUtils {
    private fun createCoilLoadListener(imageView: View, onSuccessAction: (() -> Unit)?) = object : ImageRequest.Listener {
        override fun onSuccess(request: ImageRequest, result: SuccessResult) {
            imageView.visibility = View.VISIBLE
            onSuccessAction?.invoke()
        }
        override fun onError(request: ImageRequest, result: ErrorResult) {
            imageView.visibility = View.GONE
            Log.w("icon", "CoilError: ${result.throwable.message}")
        }
    }

    fun loadAndSetIconAsync(iconInfo: IconInfo,
                            imageViewIcon: ImageView,
                            onIconLoaded: (() -> Unit)? = null // Optional lambda
        ) {
        when (iconInfo.type) {
            IconType.ASSET -> {
                // Load image from assets handled by coil
                imageViewIcon.load("file:///android_asset/${iconInfo.path}") {
                    listener(createCoilLoadListener(imageViewIcon, onIconLoaded))
                }
            }
            IconType.UNKNOWN -> {
                imageViewIcon.visibility = View.GONE
                error("Unknown icon type. Corrupt DB?")
            }
        }
    }

    /**
     * Load an icon drawable directly without an ImageView.
     * Use this when you need the drawable for non-ImageView purposes (e.g., ActionBar icon).
     */
    fun loadIconDrawableAsync(
        context: Context,
        iconInfo: IconInfo,
        onIconLoaded: (Drawable?) -> Unit
    ) {
        when (iconInfo.type) {
            IconType.ASSET -> {
                val request = ImageRequest.Builder(context)
                    .data("file:///android_asset/${iconInfo.path}")
                    .target(
                        onSuccess = { drawable -> onIconLoaded(drawable) },
                        onError = { _ ->
                            Log.w("icon", "Failed to load drawable")
                            onIconLoaded(null)
                        }
                    )
                    .build()
                context.imageLoader.enqueue(request)
            }
            IconType.UNKNOWN -> {
                Log.w("icon", "Unknown icon type")
                onIconLoaded(null)
            }
        }
    }
}
