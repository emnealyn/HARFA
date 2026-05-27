package com.example.harpapp.navigation

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val SPLASH = "splash"
    const val SONG = "song"
    const val SONG_PATTERN = "song/{songId}"
    fun createSongRoute(songId: Int): String = "song/$songId"
    const val BLUETOOTH = "bluetooth"
}
