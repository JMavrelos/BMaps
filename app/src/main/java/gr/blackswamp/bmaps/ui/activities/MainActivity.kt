package gr.blackswamp.bmaps.ui.activities

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap
import com.mapbox.services.android.navigation.ui.v5.route.OnRouteSelectionChangeListener
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import gr.blackswamp.bmaps.App
import gr.blackswamp.bmaps.R
import gr.blackswamp.bmaps.data.ViewState
import gr.blackswamp.bmaps.ui.adapters.AutoCompleteAdapter
import gr.blackswamp.bmaps.utils.hideKeyboard
import gr.blackswamp.bmaps.viewmodel.MainViewModel
import gr.blackswamp.bmaps.viewmodel.MainViewModelImpl
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

class MainActivity : HistoryActivity(), OnMapReadyCallback,
    OnRouteSelectionChangeListener {
    companion object {
        private const val NAVIGATION_PERMISSIONS_REQUEST = 12345
        private const val DEFAULT_ZOOM = 12.0
        private const val DEFAULT_BEARING = 0.0
        private const val DEFAULT_TILT = 0.0
        private const val TWO_SECONDS = 2000
        private const val ONE_SECOND = 1000
        private const val ZERO_PADDING = 0
        private const val BOTTOM_SHEET_MULTIPLIER = 4
    }

    private var map: NavigationMapboxMap? = null
    private val viewModel: MainViewModel by lazy { ViewModelProvider(this, App.viewModelFactory).get(MainViewModelImpl::class.java) }
    private lateinit var behavior: BottomSheetBehavior<FrameLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupWith(savedInstanceState)
        addNavigationForHistory(viewModel.navigation)
    }

    //region lifecycle methods
    override fun onStart() {
        super.onStart()
        map_view.onStart()
        map?.onStart()
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_view.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map_view.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
    }

    override fun onStop() {
        super.onStop()
        map_view.onStop()
        map?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        map_view.onDestroy()
    }

    override fun onBackPressed() {
        if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else if (!viewModel.backPressed()) {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == NAVIGATION_PERMISSIONS_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults.count { it != PackageManager.PERMISSION_GRANTED } == 0) {
                initialize()
            }
        }
    }
    //endregion

    private fun setupWith(savedInstanceState: Bundle?) {
        map_view.onCreate(savedInstanceState)
        //update view state
        setUpBottomSheet()
        setUpListeners()

        val permissions = mutableListOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val totalPermissions = permissions.size - 1
        for (idx in totalPermissions downTo 0) {
            if (ContextCompat.checkSelfPermission(application, permissions[idx]) == PackageManager.PERMISSION_GRANTED) {
                permissions.removeAt(idx)
            }
        }
        if (permissions.size == 0) {
            initialize()
        } else {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), NAVIGATION_PERMISSIONS_REQUEST)
        }
    }

    private fun setUpListeners() {
        autocompleteView.setOnClickListener {
            if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                if (autocompleteView.text.isNotEmpty()) {
                    autocompleteView.selectAll()
                }
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        autocompleteView.setAdapter(AutoCompleteAdapter(this))
        autocompleteView.setFeatureClickListener { feature ->
            feature.center()?.let { point ->
                hideKeyboard()
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                viewModel.findRouteTo(point)
            }
        }
        settings.setOnClickListener(this::showSettings)
        my_location.setOnClickListener(this::goToMyLocation)
        navigate.setOnClickListener { viewModel.startNavigation(false) }
        navigate.setOnLongClickListener { viewModel.startNavigation(true) }
        cancel.setOnClickListener { viewModel.cancelNavigation() }
    }

    private fun setUpBottomSheet() {
        behavior = BottomSheetBehavior.from(autoCompleteFrame)
        behavior.peekHeight = resources.getDimension(R.dimen.bottom_sheet_peek_height).toInt()
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun initialize() {
        viewModel.location.observe(this, Observer { locationUpdated(it) })
        viewModel.state.observe(this, Observer { updateState(it) })
        viewModel.routes.observe(this, Observer { routesFound(it) })
        viewModel.progress.observe(this, Observer { updateProgress(it) })
        viewModel.milestone.observe(this, Observer { updateMilestone(it) })
        viewModel.zoomLevel.observe(this, Observer {
            it?.let { changeZoomLevel(it) }
        })
        map_view.getMapAsync(this)
    }
    //region listeners

    @Suppress("UNUSED_PARAMETER")
    private fun showSettings(view: View) {
        startActivity(SettingsActivity.getIntent(this))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun goToMyLocation(view: View) {
        val location = viewModel.location.value ?: return
        updateMapCamera(buildCameraUpdateFrom(location, DEFAULT_ZOOM), ONE_SECOND)
    }

    private fun routesFound(routes: List<DirectionsRoute>?) {
        if (routes == null || viewModel.state.value != ViewState.SHOW_ROUTE)
            return
        map?.drawRoutes(routes)
    }

    private fun updateProgress(progress: RouteProgress?) {
        progress?.let {
            instructions.updateDistanceWith(progress)
        }
    }

    private fun updateMilestone(milestone: Milestone?) {
        milestone?.let {
            instructions.updateBannerInstructionsWith(it)
        }
    }

    private fun locationUpdated(location: Location?) {
        location?.let {
            if (viewModel.state.value == ViewState.SHOW_LOCATION)
                updateMapCamera(buildCameraUpdateFrom(location, null), TWO_SECONDS)
            autocompleteView.updateProximity(location)
            map?.updateLocation(location)
        }
    }

    //endregion

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.Builder().fromUri(getString(R.string.navigation_guidance_day))) {
            map = NavigationMapboxMap(map_view, mapboxMap)
            map?.setOnRouteSelectionChangeListener(this)
            map?.updateLocationLayerRenderMode(RenderMode.NORMAL)
            mapboxMap.addOnMapLongClickListener { point ->
                viewModel.findRouteTo(Point.fromLngLat(point.longitude, point.latitude))
                true
            }
            viewModel.mapReady(mapboxMap)
            setMapPadding(false)
        }
    }


    override fun onNewPrimaryRouteSelected(directionsRoute: DirectionsRoute) {
        viewModel.newRouteSelected(directionsRoute)
    }

    private fun updateState(state: ViewState) {
        Timber.d("new state $state")
        when (state) {
            ViewState.SHOW_LOCATION -> {
                instructions.retrieveFeedbackButton().hide()
                instructions.retrieveSoundButton().hide()
                map?.removeRoute()
                map?.clearMarkers()
                map?.updateLocationLayerRenderMode(RenderMode.NORMAL)
                map?.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE)
                my_location.show()
                navigate.hide()
                settings.show()
                cancel.hide()
                instructions.visibility = View.INVISIBLE
                setMapPadding(false)
//                if (state == ViewState.SHOW_LOCATION) {
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
//                }
            }
            ViewState.SHOW_ROUTE -> {
                map?.clearMarkers()
                TransitionManager.beginDelayedTransition(mainLayout)
                map?.showAlternativeRoutes(true)
                my_location.hide()
                settings.hide()
                navigate.show()
                cancel.hide()
                viewModel.destination.value?.let { destination ->
                    map?.addDestinationMarker(destination)
                    moveCameraToInclude(destination)
                }
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
            ViewState.NAVIGATE -> {
                map?.showAlternativeRoutes(false)
                map?.addProgressChangeListener(viewModel.navigation)
                navigate.hide()
                cancel.show()
                my_location.hide()
                settings.hide()
                instructions.visibility = View.VISIBLE
                map?.updateLocationLayerRenderMode(RenderMode.GPS)
                map?.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                setMapPadding(true)
                behavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
    }

    private fun updateMapCamera(cameraUpdate: CameraUpdate, duration: Int) {
        map?.retrieveMap()?.animateCamera(cameraUpdate, duration)
    }


    private fun buildCameraUpdateFrom(location: Location, zoomLevel: Double?): CameraUpdate {
//        return CameraUpdateFactory.newCameraPosition(
//            CameraPosition.Builder()
//                .zoom(DEFAULT_ZOOM)
//                .target(LatLng(location.latitude, location.longitude))
//                .bearing(DEFAULT_BEARING)
//                .tilt(DEFAULT_TILT)
//                .build()
//        )
        val builder = CameraPosition.Builder()
            .target(LatLng(location.latitude, location.longitude))
            .bearing(DEFAULT_BEARING)
            .tilt(DEFAULT_TILT)
        if (zoomLevel != null) {
            builder.zoom(zoomLevel)
        }

        return CameraUpdateFactory.newCameraPosition(builder.build())
    }

    private fun setMapPadding(shown: Boolean) {
        if (shown) {
            val bottomSheetHeight = resources.getDimension(R.dimen.bottom_sheet_peek_height).toInt()
            val topPadding = map_view.height - bottomSheetHeight * BOTTOM_SHEET_MULTIPLIER
            val customPadding = intArrayOf(ZERO_PADDING, topPadding, ZERO_PADDING, ZERO_PADDING)
            map?.adjustLocationIconWith(customPadding)
        } else {
            map?.adjustLocationIconWith(intArrayOf(ZERO_PADDING, ZERO_PADDING, ZERO_PADDING, ZERO_PADDING))
        }

    }

    private fun moveCameraToInclude(destination: Point) {
        val location = viewModel.location.value ?: return
        val mapboxMap = map?.retrieveMap() ?: return
        val origin = LatLng(location)
        val bounds = LatLngBounds.Builder()
            .include(origin)
            .include(LatLng(destination.latitude(), destination.longitude()))
            .build()

        val left = resources.getDimension(R.dimen.route_overview_padding_left).toInt()
        val top = resources.getDimension(R.dimen.route_overview_padding_top).toInt()
        val right = resources.getDimension(R.dimen.route_overview_padding_right).toInt()
        val bottom = resources.getDimension(R.dimen.route_overview_padding_bottom).toInt()
        val padding = intArrayOf(left, top, right, bottom)

        val position = mapboxMap.getCameraForLatLngBounds(bounds, padding) ?: return

        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), TWO_SECONDS)
    }

    private fun changeZoomLevel(change: Double) {
        val mapbox = map?.retrieveMap() ?: return
        val position = mapbox.cameraPosition
        val newZoom = max(min(position.zoom + change, mapbox.maxZoomLevel), mapbox.minZoomLevel)
        if (newZoom == position.zoom)
            return
        mapbox.cameraPosition =
            CameraPosition.Builder(position)
                .zoom(newZoom).build()
    }

}
