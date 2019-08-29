//package gr.blackswamp.bmaps.ui.activities
//
//import android.app.Activity
//import android.content.Intent
//import android.content.SharedPreferences
//import android.os.Bundle
//import android.os.Environment
//import android.preference.ListPreference
//import android.preference.PreferenceActivity
//import android.preference.PreferenceFragment
//import android.preference.PreferenceManager
//import gr.blackswamp.bmaps.R
//
//class SettingsActivity : PreferenceActivity() {
//
//    companion object {
//        const val UNIT_TYPE_CHANGED = "unit_type_changed"
//        const val LANGUAGE_CHANGED = "language_changed"
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        if (savedInstanceState == null) {
//            fragmentManager
//                .beginTransaction()
//                .replace(R.id.settings, SettingsFragment())
//                .commit()
//        }
//
//        //register a listener that will return a result from this activity
//        PreferenceManager.getDefaultSharedPreferences(this)
//            .registerOnSharedPreferenceChangeListener { _, key ->
//                val resultIntent = Intent()
//                resultIntent.putExtra(UNIT_TYPE_CHANGED, key == getString(R.string.unit_type_key))
//                resultIntent.putExtra(LANGUAGE_CHANGED, key == getString(R.string.language_key))
//                setResult(Activity.RESULT_OK, resultIntent)
//            }
//
//    }
//
//    class SettingsFragment : PreferenceFragment() {
//        override fun onCreate(savedInstanceState: Bundle?) {
//            addPreferencesFromResource(R.xml.settings)
//        }
//
//        override fun onResume() {
//            super.onResume()
//            PreferenceManager.setDefaultValues(
//                activity,
//                R.xml.settings,
//                false
//            )
//        }
//    }
//
//}