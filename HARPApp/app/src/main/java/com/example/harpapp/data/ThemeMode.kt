package com.example.harpapp.data

enum class ThemeMode(val value: Int) {
    LIGHT(0),
    DARK(1);

    companion object {
        fun fromInt(value: Int?): ThemeMode =
            values().firstOrNull { it.value == value } ?: LIGHT
    }
}

fun ThemeMode.toAppCompatMode(): Int = when(this) {
    ThemeMode.LIGHT -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
    ThemeMode.DARK -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
}
