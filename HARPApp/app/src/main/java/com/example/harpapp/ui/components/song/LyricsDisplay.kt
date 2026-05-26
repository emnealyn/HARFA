package com.example.harpapp.ui.components.song

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.harpapp.model.LyricNote

@Composable
fun LyricsDisplay(
    lyrics: List<LyricNote>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {

        lyrics.chunked(6).forEach { chunk ->

            Row {

                chunk.forEach { item ->

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.wrapContentWidth().padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = item.syllable,
                            maxLines = 1,
                            softWrap = false
                        )

                        Text(
                            text = item.note,
                            maxLines = 1,
                            softWrap = false
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}