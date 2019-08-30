package gr.blackswamp.bmaps.data

interface IPreferences {
    val UNIT_TYPE_KEY: String
    val VOICE_KEY: String
    val PROFILE_KEY: String

    val voice: Boolean
    val profile: String
    val unitType: String
    fun clearListener()
    fun setListener(listener: (String) -> Unit)
}
