package gr.blackswamp.bmaps.viewmodel

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import gr.blackswamp.bmaps.App
import gr.blackswamp.bmaps.data.ViewState
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.*

class RouteFinder(
    private val viewModel: MainViewModelImpl,
    private val accessToken: String,
    var profile: String,
    var unitType: String
) : Callback<DirectionsResponse> {
    companion object {
        private const val BEARING_TOLERANCE = 90.0
    }

    internal fun findRoute(location: Location, destination: Point) {
        val origin = Point.fromLngLat(location.longitude, location.latitude)
        val bearing = location.bearing.toDouble()
        NavigationRoute.builder(App.app)
            .accessToken(accessToken)
            .origin(origin, bearing, BEARING_TOLERANCE)
            .profile(profile)
            .language(Locale("el", "GR"))
            .voiceUnits(unitType)
            .destination(destination)
            .alternatives(true)
            .build()
            .getRoute(this)
    }

    override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
        response.body()?.routes()?.let {
            if (it.isNotEmpty()) {
                if (viewModel.state.value == ViewState.SHOW_LOCATION) {
                    viewModel.state.value = ViewState.SHOW_ROUTE
                }
                viewModel.routes.value = it
                viewModel.primaryRoute = it.first()

                // Handle off-route scenarios
                if (viewModel.isOffRoute) {
                    viewModel.isOffRoute = false
                    viewModel.startNavigation(viewModel.isMocked)
                }
            }
        }
    }

    override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
        Timber.e(throwable)
    }
}
