package gr.blackswamp.bmaps

import androidx.lifecycle.ViewModelProvider
import androidx.multidex.MultiDexApplication
import com.mapbox.android.search.MapboxSearch
import com.mapbox.android.search.MapboxSearchOptions
import com.mapbox.mapboxsdk.Mapbox
import timber.log.Timber

class App : MultiDexApplication() {
    companion object {
        lateinit var viewModelFactory: ViewModelProvider.Factory
        lateinit var app: App
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        viewModelFactory = ViewModelProvider.AndroidViewModelFactory(this)
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