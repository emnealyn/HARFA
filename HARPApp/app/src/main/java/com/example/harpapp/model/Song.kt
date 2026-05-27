package com.example.harpapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.harpapp.R

enum class Difficulty {
    EASY, MEDIUM, HARD
}

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val artist: String,
    val difficulty: Difficulty,
    val duration: String,
    val lyricsWithNotes: List<LyricNote>,
    val midiFilePath: String? = null,
    val coverImage: String? = null
)

fun Song.getCoverResourceId(): Int {
    return when (this.coverImage) {
        "cover_another_love" -> R.drawable.cover_another_love
        "cover_wildest_dreams" -> R.drawable.cover_wildest_dreams
        else -> R.drawable.cover_placeholder
    }
}