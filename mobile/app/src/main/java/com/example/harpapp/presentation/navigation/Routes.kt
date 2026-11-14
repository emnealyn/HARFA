package com.example.harpapp.presentation.navigation

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val SPLASH = "splash"
    const val SONG = "song"
    const val SONG_PATTERN = "song/{songId}"
    fun createSongRoute(songId: String): String = "song/$songId"
    const val BLUETOOTH = "bluetooth"
    const val HARP = "harp"
    const val HARP_PATTERN = "harp?songId={songId}"
    fun createHarpRoute(songId: String): String = "harp?songId=$songId"

    const val DEV_SIMULATOR = "dev_simulator"
}
