package com.youxiang8727.googlenearbychatroom.presentation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.youxiang8727.googlenearbychatroom.presentation.chatroom.*

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val viewModel: ChatroomViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChatroomContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ChatroomContract.Effect.NavigateToCreateRoom -> {
                    navController.navigate("create_room")
                }
                is ChatroomContract.Effect.NavigateToChat -> {
                    navController.navigate("chat") {
                        popUpTo("discovery") { inclusive = false }
                    }
                }
                is ChatroomContract.Effect.NavigateBack -> {
                    navController.popBackStack()
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "discovery",
    ) {
        composable("discovery") {
            DiscoveryScreen(
                state = state,
                onEvent = viewModel::setEvent,
            ) {
                viewModel.setEvent(ChatroomContract.Event.OnCreateRoomClick)
            }
        }
        composable("create_room") {
            CreateRoomScreen(
                state = state,
                onEvent = viewModel::setEvent,
            ) {
                viewModel.setEvent(ChatroomContract.Event.StopAdvertising)
                navController.popBackStack()
            }
        }
        composable("chat") {
            ChatScreen(
                state = state,
                onEvent = viewModel::setEvent,
            ) { 
                viewModel.setEvent(ChatroomContract.Event.Disconnect)
                navController.popBackStack("discovery", inclusive = false)
            }
        }
    }

    // Observe state to trigger navigation to ChatScreen when connected
    LaunchedEffect(state.connectedEndpoints, state.isAdvertising) {
        val isConnected = state.connectedEndpoints.isNotEmpty()
        val isAdvertising = state.isAdvertising
        val currentRoute = navController.currentDestination?.route
        
        if ((isConnected || isAdvertising) && (currentRoute != "chat")) {
            navController.navigate("chat") {
                popUpTo("discovery") { inclusive = false }
            }
        } else if (!isConnected && !isAdvertising && (currentRoute == "chat")) {
            navController.popBackStack("discovery", inclusive = false)
        }
    }
}
