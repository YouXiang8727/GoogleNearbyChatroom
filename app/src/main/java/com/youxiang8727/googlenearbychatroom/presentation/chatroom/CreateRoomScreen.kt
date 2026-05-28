package com.youxiang8727.googlenearbychatroom.presentation.chatroom

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.youxiang8727.googlenearbychatroom.R
import com.youxiang8727.googlenearbychatroom.ui.theme.GoogleNearbyChatroomTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomScreen(
    state: ChatroomContract.State,
    onEvent: (ChatroomContract.Event) -> Unit,
    onBack: () -> Unit,
) {
    var chatroomName by remember { mutableStateOf(state.chatroomName) }

    BackHandler {
        onEvent(ChatroomContract.Event.StopAdvertising)
        onBack()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.create_chatroom), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.AddHome,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Host a New Chatroom",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Nearby users will be able to discover and join your room.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            OutlinedTextField(
                value = chatroomName,
                onValueChange = { chatroomName = it },
                label = { Text(stringResource(R.string.chatroom_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onEvent(ChatroomContract.Event.StartAdvertising(chatroomName)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                enabled = chatroomName.isNotBlank() && !state.isAdvertising && !state.isLoading
            ) {
                if (state.isLoading || state.isAdvertising) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(if (state.isLoading) "Starting..." else "Hosting...")
                } else {
                    Text("Start Hosting", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (state.isAdvertising) {
                Text(
                    text = "Room created! Waiting for connections...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateRoomScreenPreview() {
    GoogleNearbyChatroomTheme {
        CreateRoomScreen(
            state = ChatroomContract.State(),
            onEvent = {},
        ) {
            // onBack
        }
    }
}
