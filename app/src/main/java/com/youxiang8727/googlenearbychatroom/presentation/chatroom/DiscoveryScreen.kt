package com.youxiang8727.googlenearbychatroom.presentation.chatroom

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.youxiang8727.googlenearbychatroom.R
import com.youxiang8727.googlenearbychatroom.ui.theme.GoogleNearbyChatroomTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    state: ChatroomContract.State,
    onEvent: (ChatroomContract.Event) -> Unit,
    onCreateRoom: () -> Unit,
) {
    val context = LocalContext.current

    val nearbyPermissions = if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    } else if (Build.VERSION.SDK_INT >= 31) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allNearbyGranted = nearbyPermissions.all {
            if (it == Manifest.permission.POST_NOTIFICATIONS) true // Skip notification for mandatory check
            else permissions[it] == true
        }

        if (allNearbyGranted) {
            Toast.makeText(context, R.string.permissions_granted, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Nearby functionality requires these permissions", Toast.LENGTH_SHORT).show()
            // Jump to app settings ONLY for mandatory nearby permissions
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    fun checkAndRequestNearbyPermissions(onGranted: () -> Unit) {
        val allCriticalGranted = nearbyPermissions.all {
            if (Build.VERSION.SDK_INT >= 33 && it == Manifest.permission.POST_NOTIFICATIONS) true
            else ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (allCriticalGranted) {
            onGranted()
        } else {
            launcher.launch(nearbyPermissions)
        }
    }

    LaunchedEffect(Unit) {
        val allCriticalGranted = nearbyPermissions.all {
            if (Build.VERSION.SDK_INT >= 33 && it == Manifest.permission.POST_NOTIFICATIONS) true
            else ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!allCriticalGranted) {
            launcher.launch(nearbyPermissions)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    checkAndRequestNearbyPermissions { onCreateRoom() }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Create Room") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            // User Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.userName.ifEmpty { "Loading..." },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (state.userId.isNotEmpty()) {
                            Text(
                                text = "#${state.userId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    IconButton(onClick = { onEvent(ChatroomContract.Event.OnEditingNameChange(isEditing = true)) }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (state.isEditingName) {
                EditNameDialog(
                    currentName = state.userName,
                    onDismiss = { onEvent(ChatroomContract.Event.OnEditingNameChange(isEditing = false)) }
                ) { newName ->
                    onEvent(ChatroomContract.Event.SetUserName(newName))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Discovery Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Nearby Rooms",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (state.isDiscovering) {
                    TextButton(onClick = { onEvent(ChatroomContract.Event.StopDiscovery) }) {
                        Text(stringResource(R.string.stop), color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Button(
                        onClick = {
                            checkAndRequestNearbyPermissions { onEvent(ChatroomContract.Event.StartDiscovery) }
                        },
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.start_discovery))
                    }
                }
            }

            if (state.isDiscovering) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.discoveredChatrooms.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (state.isDiscovering) "Searching for nearby rooms..." else "No rooms found. Try scanning!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items(state.discoveredChatrooms) { chatroom ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEvent(ChatroomContract.Event.ConnectToChatroom(chatroom)) }
                    ) {
                        ListItem(
                            headlineContent = { Text(chatroom.name, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { 
                                Text(
                                    if (chatroom.hostId.isNotEmpty()) "Host ID: #${chatroom.hostId}" 
                                    else "ID: ${chatroom.id.take(8)}"
                                ) 
                            },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DiscoveryScreenPreview() {
    GoogleNearbyChatroomTheme {
        DiscoveryScreen(
            state = ChatroomContract.State(
                userName = "David",
                isDiscovering = true,
                discoveredChatrooms = listOf(
                    com.youxiang8727.googlenearbychatroom.domain.model.Chatroom("1", "Developer Room"),
                    com.youxiang8727.googlenearbychatroom.domain.model.Chatroom("2", "Nearby Fun")
                )
            ),
            onEvent = {},
            onCreateRoom = {}
        )
    }
}
