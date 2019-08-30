package gr.blackswamp.bmaps

import androidx.lifecycle.ViewModelProvider
import androidx.multidex.MultiDexApplication
import com.mapbox.android.search.MapboxSearch
import com.mapbox.android.search.MapboxSearchOptions
import com.mapbox.mapboxsdk.Mapbox
import gr.blackswamp.bmaps.data.IPreferences
import gr.blackswamp.bmaps.data.Prefs
import timber.log.Timber

class App : MultiDexApplication() {
    companion object {
        const val NAVIGATION_INSTRUCTIONS = "gr.blackswamp.bmaps.NAVIGATION"
        const val NAVIGATION_INSTRUCTIONS_DISTANCE = "gr.blackswamp.bmaps.NAVIGATION.DISTANCE"
        const val NAVIGATION_INSTRUCTIONS_DIRECTIONS = "gr.blackswamp.bmaps.NAVIGATION.DIRECTIONS"

        lateinit var viewModelFactory: ViewModelProvider.Factory
            private set
        lateinit var app: App
            private set
        val preferences: IPreferences = Prefs
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        viewModelFactory = ViewModelProvider.AndroidViewModelFactory(this)
        Prefs.initialize(this)
        setupTimber()
        setupMapbox()

    }

    private fun setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun setupMapbox() {
        val cachingMode = MapboxSearchOptions().setCachingEnabled(true)
        MapboxSearch.getInstance(applicationContext, BuildConfig.ApiKey, cachingMode)
        Mapbox.getInstance(applicationContext, BuildConfig.ApiKey)
    }
}