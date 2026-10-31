package com.example.harpapp.data

import androidx.room.TypeConverter
import com.example.harpapp.model.Difficulty
import com.example.harpapp.model.LyricNote
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromDifficulty(difficulty: Difficulty): String {
        return difficulty.name
    }

    @TypeConverter
    fun toDifficulty(value: String): Difficulty {
        return Difficulty.valueOf(value)
    }

    @TypeConverter
    fun fromLyricNoteList(value: List<LyricNote>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toLyricNoteList(value: String): List<LyricNote> {
        val type = object : TypeToken<List<LyricNote>>() {}.type
        return Gson().fromJson(value, type)
    }
}
