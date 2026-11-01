package com.example.harpapp.presentation.ui.components.home

import com.example.harpapp.R
import com.example.harpapp.domain.model.Song

fun Song.getCoverResourceId(): Int {
    return when (coverImage) {
        "cover_another_love" -> R.drawable.cover_another_love
        "cover_wildest_dreams" -> R.drawable.cover_wildest_dreams
        "cover_lovely" -> R.drawable.cover_lovely
        "cover_hallelujah" -> R.drawable.cover_hallelujah
        else -> R.drawable.cover_placeholder
    }
}
