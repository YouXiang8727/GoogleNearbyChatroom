package com.youxiang8727.googlenearbychatroom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.youxiang8727.googlenearbychatroom.presentation.NavGraph
import com.youxiang8727.googlenearbychatroom.ui.theme.GoogleNearbyChatroomTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoogleNearbyChatroomTheme {
                NavGraph()
            }
        }
    }
}
