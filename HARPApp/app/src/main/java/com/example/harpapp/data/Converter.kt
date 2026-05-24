package com.example.harpapp.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromDifficulty(difficulty: Difficulty): String {
        return difficulty.name
    }

    @TypeConverter
    fun toDifficulty(value: String): Difficulty {
        return Difficulty.valueOf(value)
    }
}