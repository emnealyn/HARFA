package com.example.harpapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.harpapp.navigation.AppNavGraph
import com.example.harpapp.ui.theme.HARPAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HARPAppTheme {
                AppNavGraph()
            }
        }
    }
}