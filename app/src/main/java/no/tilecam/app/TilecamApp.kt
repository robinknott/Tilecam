package no.tilecam.app

import android.app.Application
import no.tilecam.app.data.AppContainer

class TilecamApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
