package gr.blackswamp.bmaps.ui.callbacks

import com.mapbox.api.directions.v5.models.DirectionsRoute

interface OnRoutesFoundCallback {

  fun onRoutesFound(routes: List<DirectionsRoute>)

  fun onError(error: String)
}