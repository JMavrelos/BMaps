package gr.blackswamp.bmaps.ui.callbacks

import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import timber.log.Timber

class MainLocationEngineCallback(private val location: MutableLiveData<Location>) :
    LocationEngineCallback<LocationEngineResult> {

    override fun onSuccess(result: LocationEngineResult) {
        location.value = result.lastLocation
    }

    override fun onFailure(exception: Exception) {
        Timber.e(exception)
    }
}