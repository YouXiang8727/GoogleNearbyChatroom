package com.youxiang8727.googlenearbychatroom.presentation.chatroom

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.youxiang8727.googlenearbychatroom.R
import com.youxiang8727.googlenearbychatroom.domain.model.ChatMessage
import com.youxiang8727.googlenearbychatroom.domain.model.MessageType
import com.youxiang8727.googlenearbychatroom.ui.theme.GoogleNearbyChatroomTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatroomContract.State,
    onEvent: (ChatroomContract.Event) -> Unit,
    onBack: () -> Unit,
) {
    var messageText by remember { mutableStateOf("") }
    var playingVideoUri by remember { mutableStateOf<String?>(null) }
    var expandedImageUri by remember { mutableStateOf<String?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current
    
    val mediaPermissions = if (android.os.Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            // Permission granted, you might need to re-click or just inform
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(it)
            val type = when {
                mimeType?.startsWith("video/") == true -> MessageType.VIDEO
                mimeType == "image/gif" -> MessageType.GIF
                else -> MessageType.IMAGE
            }
            onEvent(ChatroomContract.Event.SendMedia(it.toString(), type))
        }
    }

    BackHandler {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            showExitDialog = true
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        modifier = Modifier.width(320.dp),
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                        drawerTonalElevation = 4.dp,
                        drawerShape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            // Header
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                    .padding(vertical = 32.dp, horizontal = 24.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Chatroom",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Members",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Members List
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(state.chatroomUsers) { user ->
                                    val isMe = user.id == state.userId
                                    Surface(
                                        color = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        ListItem(
                                            headlineContent = { 
                                                Column {
                                                    // Status Row (Host / You) above the name
                                                    if (user.isHost || isMe) {
                                                        Row(
                                                            modifier = Modifier.padding(bottom = 2.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            if (user.isHost) {
                                                                Surface(
                                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                                                ) {
                                                                    Text(
                                                                        text = "HOST",
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = MaterialTheme.colorScheme.primary
                                                                    )
                                                                }
                                                            }
                                                            if (isMe) {
                                                                Surface(
                                                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                                                                ) {
                                                                    Text(
                                                                        text = "YOU",
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = MaterialTheme.colorScheme.secondary
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = user.name,
                                                        fontWeight = if (user.isHost) FontWeight.Bold else FontWeight.Medium,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                            },
                                            supportingContent = { 
                                                Text(
                                                    text = "#${user.id}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                ) 
                                            },
                                            leadingContent = { 
                                                Surface(
                                                    modifier = Modifier.size(40.dp),
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = if (user.isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = if (user.isHost) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                    }
                                                }
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                        )
                                    }
                                }
                            }
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            
                            // Bottom area (room for more info)
                            Box(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Connected via Nearby Connections",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { 
                                        scope.launch { drawerState.open() } 
                                    }
                                ) {
                                    Text(
                                        text = state.chatroomName.ifEmpty { stringResource(R.string.app_name) },
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = stringResource(R.string.connected_peers_label, state.connectedEndpoints.size),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { showExitDialog = true }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                IconButton(onClick = { 
                                    scope.launch { drawerState.open() }
                                }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            // Messages List
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.messages) { message ->
                                    MessageBubble(
                                        message = message,
                                        onVideoClick = { playingVideoUri = it },
                                        onImageClick = { expandedImageUri = it }
                                    )
                                }
                            }

                            // Input Area
                            Surface(
                                tonalElevation = 8.dp,
                                shadowElevation = 16.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    IconButton(
                                        onClick = { 
                                            val allGranted = mediaPermissions.all {
                                                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                                            }
                                            if (allGranted) {
                                                mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                            } else {
                                                mediaPermissionLauncher.launch(mediaPermissions)
                                            }
                                        },
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Media")
                                    }
                                    
                                    TextField(
                                        value = messageText,
                                        onValueChange = { messageText = it },
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(28.dp)),
                                        placeholder = { Text(stringResource(R.string.enter_message_hint)) },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent
                                        ),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    FloatingActionButton(
                                        onClick = {
                                            if (messageText.isNotBlank()) {
                                                onEvent(ChatroomContract.Event.SendMessage(messageText))
                                                messageText = ""
                                            }
                                        },
                                        modifier = Modifier.size(52.dp),
                                        shape = RoundedCornerShape(26.dp),
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send, 
                                            contentDescription = "Send",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (state.isAdvertising && state.connectedEndpoints.isEmpty()) {
                            AlertDialog(
                                onDismissRequest = { /* Prevent dismiss */ },
                                title = { Text("Waiting for Members") },
                                text = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Your chatroom \"${state.chatroomName}\" is live!", fontWeight = FontWeight.Bold)
                                        Text("Waiting for someone to join...", style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { onEvent(ChatroomContract.Event.Disconnect) }) {
                                        Text("Cancel Hosting", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    playingVideoUri?.let { uri ->
        VideoPlayerDialog(
            videoUri = uri,
            onDismiss = { playingVideoUri = null },
            onDownload = { onEvent(ChatroomContract.Event.DownloadMedia(uri, MessageType.VIDEO)) }
        )
    }

    expandedImageUri?.let { uri ->
        // Find the message to determine if it's IMAGE or GIF
        val message = state.messages.find { it.mediaUri == uri }
        val type = message?.type ?: MessageType.IMAGE
        
        FullScreenImageDialog(
            imageUri = uri,
            onDismiss = { expandedImageUri = null },
            onDownload = { onEvent(ChatroomContract.Event.DownloadMedia(uri, type)) }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Chatroom") },
            text = { Text("Do you want to keep the chat history for this room?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onEvent(ChatroomContract.Event.Disconnect)
                    onBack()
                }) {
                    Text("Keep History")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    val roomToClear = if (state.isAdvertising) state.userId else state.chatroomId
                    onEvent(ChatroomContract.Event.DeleteChatHistory(roomToClear))
                    onEvent(ChatroomContract.Event.Disconnect)
                    onBack()
                }) {
                    Text("Delete History", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@Composable
fun VideoPlayerDialog(
    videoUri: String, 
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                    }
                    
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenImageDialog(
    imageUri: String, 
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Expanded Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onVideoClick: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isExpired = remember(message.mediaUri) {
        if (message.type == MessageType.VIDEO && message.mediaUri != null) {
            try {
                val uri = Uri.parse(message.mediaUri)
                if (uri.scheme == "file") {
                    !java.io.File(uri.path!!).exists()
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        } else false
    }
    if (message.isSystemMessage) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val isMe = message.isFromMe
    
    val bubbleColor = if (isMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (isMe) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (isMe) 20.dp else 4.dp,
        bottomEnd = if (isMe) 4.dp else 20.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
            ) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (message.senderId.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "#${message.senderId}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        Surface(
            color = bubbleColor,
            shape = bubbleShape,
            tonalElevation = if (isMe) 2.dp else 0.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                if ((message.type == MessageType.IMAGE || message.type == MessageType.GIF || message.type == MessageType.VIDEO) && (message.mediaUri != null)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.then(
                            when {
                                message.type == MessageType.VIDEO && !isExpired -> {
                                    Modifier.clickable { onVideoClick(message.mediaUri) }
                                }
                                message.type == MessageType.IMAGE || message.type == MessageType.GIF -> {
                                    Modifier.clickable { onImageClick(message.mediaUri) }
                                }
                                else -> Modifier
                            }
                        )
                    ) {
                        AsyncImage(
                            model = if (message.type == MessageType.VIDEO) message.thumbnailUri ?: message.mediaUri else message.mediaUri,
                            contentDescription = null,
                            modifier = Modifier
                                .sizeIn(maxWidth = 200.dp, maxHeight = 300.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit,
                            alpha = if (isExpired) 0.5f else 1f
                        )
                        if (message.type == MessageType.VIDEO) {
                            if (isExpired) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "EXPIRED",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                                    contentDescription = "Video",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
            }
        }
        
        Text(
            text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )

        if (isMe) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                val (statusIcon, statusColor) = when (message.status) {
                    com.youxiang8727.googlenearbychatroom.domain.model.MessageStatus.SENDING -> 
                        Icons.Default.Schedule to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    com.youxiang8727.googlenearbychatroom.domain.model.MessageStatus.SENT -> 
                        Icons.Default.DoneAll to MaterialTheme.colorScheme.primary
                    com.youxiang8727.googlenearbychatroom.domain.model.MessageStatus.FAILED -> 
                        Icons.Default.Error to MaterialTheme.colorScheme.error
                }
                Icon(
                    imageVector = statusIcon,
                    contentDescription = message.status.name,
                    modifier = Modifier.size(12.dp),
                    tint = statusColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when (message.status) {
                        com.youxiang8727.googlenearbychatroom.domain.model.MessageStatus.SENDING -> "Sending..."
                        com.youxiang8727.googlenearbychatroom.domain.model.MessageStatus.SENT -> "Sent"
                        com.youxiang8727.googlenearbychatroom.domain.model.MessageStatus.FAILED -> "Failed"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = statusColor
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    GoogleNearbyChatroomTheme {
        ChatScreen(
            state = ChatroomContract.State(
                chatroomName = "Nearby Developers",
                connectedEndpoints = listOf("1", "2", "3"),
                messages = listOf(
                    ChatMessage(content = "Hey! Is the Nearby Connections working?", senderName = "Alice", senderId = "A1B2", isFromMe = false),
                    ChatMessage(content = "Bob joined the chatroom", senderName = "System", isFromMe = false, isSystemMessage = true),
                    ChatMessage(content = "Yes, it's super fast!", senderName = "Bob", senderId = "C3D4", isFromMe = false),
                    ChatMessage(content = "Awesome! I'm testing the new UI design now.", senderName = "David", senderId = "E5F6", isFromMe = true),
                    ChatMessage(content = "Looks great! Love the bubbles.", senderName = "Alice", senderId = "A1B2", isFromMe = false)
                )
            ),
            onEvent = {},
        ) {
            // onBack
        }
    }
}
