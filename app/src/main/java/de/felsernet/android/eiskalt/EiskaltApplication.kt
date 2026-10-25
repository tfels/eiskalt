package de.felsernet.android.eiskalt

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.decode.SvgDecoder
import de.felsernet.android.eiskalt.common.AuthManager
import de.felsernet.android.eiskalt.settings.UserSettingsRepository

/**
 * App-wide setup, done once per process before any activity is created.
 */
class EiskaltApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // setup Coil image library
        // add a SVG loader
        val loader = ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
        Coil.setImageLoader(loader)

        AuthManager.initialize()
        UserSettingsRepository.initialize(this)
    }
}
