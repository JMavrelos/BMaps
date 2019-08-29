package gr.blackswamp.bmaps.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import gr.blackswamp.bmaps.data.ViewState

interface MainViewModel {

    val destination: LiveData<Point>
    val progress: LiveData<RouteProgress>
    val milestone: LiveData<Milestone>
    val routes: LiveData<List<DirectionsRoute>>
    val location: LiveData<Location>
    val state: LiveData<ViewState>
    val navigation: MapboxNavigation

    fun backPressed(): Boolean
    fun findRouteTo(point: Point)
    fun startNavigation(mocked: Boolean): Boolean
    fun cancelNavigation()
    fun bottomSheetStateChanged(newState: Int)
    fun mapReady(mapboxMap: MapboxMap)
    fun newRouteSelected(route: DirectionsRoute)

}