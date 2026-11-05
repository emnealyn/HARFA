package com.example.harpapp.data.local

import androidx.room.TypeConverter
import com.example.harpapp.domain.model.LyricNote
import com.example.harpapp.domain.model.Difficulty
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromDomainLyricNoteList(value: List<LyricNote>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toDomainLyricNoteList(value: String): List<LyricNote> {
        val type = object : TypeToken<List<LyricNote>>() {}.type
        return Gson().fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromEntityLyricNoteList(value: List<LyricNoteEntity>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toEntityLyricNoteList(value: String): List<LyricNoteEntity> {
        val type = object : TypeToken<List<LyricNoteEntity>>() {}.type
        return Gson().fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromDifficulty(value: Difficulty): String {
        return value.name
    }

    @TypeConverter
    fun toDifficulty(value: String): Difficulty {
        return Difficulty.valueOf(value)
    }
}
