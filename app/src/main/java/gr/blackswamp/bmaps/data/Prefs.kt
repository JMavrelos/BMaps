package gr.blackswamp.bmaps.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import gr.blackswamp.bmaps.R

object Prefs : IPreferences {
    private var mListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    override lateinit var UNIT_TYPE_KEY: String
        private set
    override lateinit var VOICE_KEY: String
        private set
    override lateinit var PROFILE_KEY: String
        private set
    private lateinit var preferences: SharedPreferences
    private lateinit var defaultProfile: String
        private set
    private lateinit var defaultUnitType: String
        private set
    private const val defaultVoice: Boolean = false

    fun initialize(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
        UNIT_TYPE_KEY = context.getString(R.string.unit_type_key)
        VOICE_KEY = context.getString(R.string.voice_key)
        PROFILE_KEY = context.getString(R.string.profile_key)

        defaultProfile = context.getString(R.string.default_route_profile)
        defaultUnitType = context.getString(R.string.device_default)

    }

    override var unitType: String
        get() = preferences.getString(UNIT_TYPE_KEY, null) ?: defaultUnitType
        set(value) = preferences.edit().putString(UNIT_TYPE_KEY, value).apply()

    override var profile: String
        get() = preferences.getString(PROFILE_KEY, null) ?: defaultProfile
        set(value) = preferences.edit().putString(PROFILE_KEY, value).apply()

    override var voice: Boolean
        get() = preferences.getBoolean(VOICE_KEY, defaultVoice)
        set(value) = preferences.edit().putBoolean(VOICE_KEY, value).apply()

    override fun setListener(listener: ((String) -> Unit)) {
        synchronized(this) {
            mListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                listener.invoke(key)
            }
            preferences.registerOnSharedPreferenceChangeListener(mListener!!)
        }
    }

    override fun clearListener() {
        synchronized(this) {
            mListener?.let {
                preferences.unregisterOnSharedPreferenceChangeListener(it)
            }
        }
    }

}