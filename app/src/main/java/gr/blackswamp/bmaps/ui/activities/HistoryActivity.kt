package gr.blackswamp.bmaps.ui.activities

import androidx.appcompat.app.AppCompatActivity
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import gr.blackswamp.bmaps.utils.StoreHistoryTask
import java.text.SimpleDateFormat
import java.util.*

open class HistoryActivity : AppCompatActivity() {
    companion object {
        private const val JSON_EXTENSION = ".json"
    }

    private val DATE_FORMAT = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
    private lateinit var navigation: MapboxNavigation
    private lateinit var filename: String
    private val progressHistoryListener = ProgressChangeListener()
    { _, _ -> executeStoreHistoryTask() }


    protected fun addNavigationForHistory(navigation: MapboxNavigation?) {
        if (navigation == null) {
            throw IllegalArgumentException("MapboxNavigation cannot be null")
        }
        this.navigation = navigation
        navigation.addProgressChangeListener(progressHistoryListener)
        navigation.toggleHistory(true)
        filename = buildFileName()
    }

    protected fun executeStoreHistoryTask() {
        StoreHistoryTask(navigation, filename).execute()
    }

    override fun onDestroy() {
        super.onDestroy()
        navigation.toggleHistory(false)
    }

    private fun buildFileName(): String {
        return StringBuilder()
            .append(obtainCurrentTimeStamp())
            .append(JSON_EXTENSION)
            .toString()
    }

    private fun obtainCurrentTimeStamp(): String {
        return DATE_FORMAT.format(Date())
    }
}