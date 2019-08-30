package gr.blackswamp.bmaps.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.services.android.navigation.ui.v5.camera.DynamicCamera
import com.mapbox.services.android.navigation.ui.v5.voice.NavigationSpeechPlayer
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechPlayerProvider
import com.mapbox.services.android.navigation.ui.v5.voice.VoiceInstructionLoader
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine
import com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import gr.blackswamp.bmaps.App
import gr.blackswamp.bmaps.BuildConfig
import gr.blackswamp.bmaps.data.ViewState
import gr.blackswamp.bmaps.ui.callbacks.MainLocationEngineCallback
import gr.blackswamp.bmaps.utils.SingleLiveEvent
import okhttp3.Cache
import timber.log.Timber
import java.io.File
import java.util.*

class MainViewModelImpl(app: Application) : AndroidViewModel(app), MainViewModel {
    companion object {
        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 1000
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 500
        private const val INSTRUCTION_CACHE = "BMaps-navigation-instruction-cache"
        private const val TEN_MEGABYTE_CACHE_SIZE: Long = 10 * 1024 * 1024

    }

    override val destination: MutableLiveData<Point> = MutableLiveData()
    override val location = MutableLiveData<Location>()
    override val routes: MutableLiveData<List<DirectionsRoute>> = MutableLiveData()
    override val progress: MutableLiveData<RouteProgress> = MutableLiveData()
    override val milestone: MutableLiveData<Milestone> = MutableLiveData()
    override val state = MutableLiveData(ViewState.SHOW_LOCATION)
    override val navigation: MapboxNavigation = MapboxNavigation(getApplication(), BuildConfig.ApiKey)
    override val zoomLevel = SingleLiveEvent<Double>()
    private val locationEngine = LocationEngineProvider.getBestLocationEngine(getApplication())
    private val locationEngineCallback = MainLocationEngineCallback(location)
    internal var primaryRoute: DirectionsRoute? = null
    internal var isOffRoute = false
    internal var isMocked = false
    private val speechPlayer: NavigationSpeechPlayer
    private val routeFinder: RouteFinder = RouteFinder(this, BuildConfig.ApiKey, App.preferences.profile, App.preferences.unitType)

    init {
        navigation.locationEngine = locationEngine
        // Initialize the speech player and pass to milestone event listener for instructions
        val english = Locale.US.language
        val cache = Cache(File(getApplication<Application>().cacheDir, INSTRUCTION_CACHE), TEN_MEGABYTE_CACHE_SIZE)
        val voiceInstructionLoader = VoiceInstructionLoader(getApplication(), BuildConfig.ApiKey, cache)
        val speechPlayerProvider = SpeechPlayerProvider(getApplication(), english, true, voiceInstructionLoader)
        speechPlayer = NavigationSpeechPlayer(speechPlayerProvider)
        navigation.addMilestoneEventListener(this::milestoneEvent)
        navigation.addProgressChangeListener(this::progressChanged)
        navigation.addOffRouteListener(this::isOffRoute)
        App.preferences.setListener { updateFromPreferences(it) }
    }

    override fun updateFromPreferences(key: String) {
        when (key) {
            App.preferences.PROFILE_KEY -> routeFinder.profile = App.preferences.profile
            App.preferences.UNIT_TYPE_KEY -> routeFinder.unitType = App.preferences.unitType
        }
    }

    private fun progressChanged(location: Location, progress: RouteProgress) {
        val meters = progress.currentLegProgress().currentStepProgress().distanceRemaining()
        val direction = progress.bannerInstruction()?.primary?.modifier ?: " None"
        Timber.i("Progress $direction in $meters")
        this.location.value = location
        this.progress.value = progress
    }

    @Suppress("UNUSED_PARAMETER")
    private fun milestoneEvent(routeProgress: RouteProgress, instruction: String, milestone: Milestone) {
        val ms = (milestone as? BannerInstructionMilestone)
        if (ms != null) {
            val direction = ms.bannerInstructions?.primary()?.modifier() ?: ""
            val meters = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining()
            Timber.i("Directions $direction in $meters")
        }

        this.milestone.value = milestone
        if (milestone is VoiceInstructionMilestone) {
            play(milestone)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun isOffRoute(location: Location?) {
        isOffRoute = true
        gotoDestination()
    }

    private fun play(milestone: VoiceInstructionMilestone) {
        if (!App.preferences.voice) return
        val announcement = SpeechAnnouncement.builder()
            .voiceInstructionMilestone(milestone)
            .build()
        speechPlayer.play(announcement)
    }

    override fun mapReady(mapboxMap: MapboxMap) {
        enableLocationUpdates(true)
        navigation.cameraEngine = DynamicCamera(mapboxMap)
    }

    override fun startNavigation(mocked: Boolean): Boolean {
        val route = primaryRoute ?: return false
        state.value = ViewState.NAVIGATE
        isMocked = mocked
        if (mocked) {
            val replayRouteLocationEngine = ReplayRouteLocationEngine()
            replayRouteLocationEngine.assign(route)
            navigation.locationEngine = replayRouteLocationEngine
        } else {
            navigation.locationEngine = locationEngine
        }
        navigation.startNavigation(route)
        enableLocationUpdates(false)
        return true
    }

    override fun cancelNavigation() {
        enableLocationUpdates(true)
        state.value = ViewState.SHOW_LOCATION
        navigation.stopNavigation()
    }


    override fun findRouteTo(point: Point) {
        destination.value = point
        gotoDestination()
    }

    private fun gotoDestination() {
        location.value?.let { location ->
            destination.value?.let { destination ->
                routeFinder.findRoute(location, destination)
            }
        }
    }

    override fun backPressed(): Boolean {
        return if (state.value != ViewState.SHOW_LOCATION) {
            state.value = ViewState.SHOW_LOCATION
            true
        } else {
            false
        }
    }

    private fun enableLocationUpdates(enable: Boolean) {
        if (enable) {
            val request = LocationEngineRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
                .build()
            locationEngine.requestLocationUpdates(request, locationEngineCallback, null)
        } else {
            locationEngine.removeLocationUpdates(locationEngineCallback)
        }
    }

    override fun newRouteSelected(route: DirectionsRoute) {
        this.primaryRoute = route
    }


    override fun onCleared() {
        super.onCleared()
        val cameraEngine = navigation.cameraEngine
        if (cameraEngine is DynamicCamera) {
            cameraEngine.clearMap()
        }
        navigation.onDestroy()
        speechPlayer.onDestroy()
        enableLocationUpdates(false)
    }
}


