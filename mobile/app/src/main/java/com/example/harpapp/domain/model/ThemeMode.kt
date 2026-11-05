package com.example.harpapp.domain.model

enum class ThemeMode(val value: Int) {
    LIGHT(0),
    DARK(1);

    companion object {
        fun fromInt(value: Int?): ThemeMode =
            values().firstOrNull { it.value == value } ?: LIGHT
    }
}
