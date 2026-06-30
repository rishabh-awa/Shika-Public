
package com.example.applicaion


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.chaquo.python.Python
import com.google.firebase.database.*
import com.example.applicaion.ui.theme.ApplicaionTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var database: FirebaseDatabase

    private var currentRoomRef: DatabaseReference? = null
    private var queueListener: ValueEventListener? = null
    private var playbackListener: ValueEventListener? = null
    private var usersListener: ValueEventListener? = null
    private var myUserRef: DatabaseReference? = null

    private var currentTrackUrl = ""
    private var extractionJob: kotlinx.coroutines.Job? = null

    // Dynamic UI States
    var activeRoomId by mutableStateOf("")
    var isPlayingGlobal by mutableStateOf(false)
    val currentQueueState = mutableStateListOf<QueueItem>()
    var activeUsersCount by mutableStateOf(1)

    // Track current active details to show on Screen 1
    var currentTrackTitle by mutableStateOf("No Track Playing")
    var currentTrackArtist by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!com.chaquo.python.Python.isStarted()) {
            com.chaquo.python.Python.start(com.chaquo.python.android.AndroidPlatform(this))
        }
        database = FirebaseDatabase.getInstance("x")
        exoPlayer = ExoPlayer.Builder(this)
            .build()

        exoPlayer.addListener(object : androidx.media3.common.Player.Listener{
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_ENDED){
                    skipToNextTrack()
                }
            }
        })

        setContent {
            ApplicaionTheme {
                if (activeRoomId.isEmpty()) {
                    RoomSelectionScreen(onRoomSelected = { selectedId -> switchRoom(selectedId) })
                } else {
                    val roomRef = currentRoomRef ?: return@ApplicaionTheme
                    ShikaAppMainContainer(
                        roomId = activeRoomId,
                        roomRef = roomRef,
                        queueList = currentQueueState,
                        isPlaying = isPlayingGlobal,
                        activeUsers = activeUsersCount,
                        trackTitle = currentTrackTitle,
                        trackArtist = currentTrackArtist,
                        onTogglePlay = { targetPlayState ->
                            roomRef.child("playbackState").child("isPlaying").setValue(targetPlayState)
                        },
                        onSkipTrack = { skipToNextTrack() },
                        onLeaveRoom = { leaveCurrentRoom() }
                    )
                }
            }
        }
    }

    private fun skipToNextTrack() {
        val roomRef = currentRoomRef ?: return
        if (currentQueueState.isNotEmpty()) {
            val nextTrack = currentQueueState.first()
            roomRef.child("queue").child(nextTrack.id).removeValue()

            val updates = mapOf(
                "currentStreamUrl" to nextTrack.streamUrl, // This stores the raw video ID key safely now
                "currentTitle" to nextTrack.title,
                "currentArtist" to nextTrack.artist,
                "isPlaying" to true
            )
            roomRef.child("playbackState").updateChildren(updates)
        } else {
            val clears = mapOf(
                "currentStreamUrl" to "",
                "currentTitle" to "No Track Playing",
                "currentArtist" to "",
                "isPlaying" to false
            )
            roomRef.child("playbackState").updateChildren(clears)
        }
    }

    private fun switchRoom(newRoomId: String) {
        leaveCurrentRoom()
        activeRoomId = newRoomId
        val roomRef = database.reference.child("rooms").child(newRoomId)
        currentRoomRef = roomRef

        val usersRef = roomRef.child("users")

        roomRef.child("playbackState").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val initialPlaybackState = mapOf(
                        "isPlaying" to false,
                        "currentStreamUrl" to "",
                        "currentTitle" to "No Track Playing",
                        "currentArtist" to ""
                    )
                    roomRef.child("playbackState").setValue(initialPlaybackState)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        val userKey = usersRef.push().key ?: "user_${System.currentTimeMillis()}"
        myUserRef = usersRef.child(userKey)
        myUserRef?.setValue(true)
        myUserRef?.onDisconnect()?.removeValue()

        usersListener = usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                if (count == 0) roomRef.removeValue() else activeUsersCount = count
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        queueListener = roomRef.child("queue").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentQueueState.clear()
                for (child in snapshot.children) {
                    val map = child.value as? Map<*, *> ?: continue
                    currentQueueState.add(QueueItem(id = child.key ?: "", map = map))
                }

                if (currentQueueState.isNotEmpty() && currentTrackUrl.isEmpty()) {
                    skipToNextTrack()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        playbackListener = roomRef.child("playbackState").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val isPlaying = snapshot.child("isPlaying").getValue(Boolean::class.java) ?: false
                val streamUrl = snapshot.child("currentStreamUrl").getValue(String::class.java) ?: ""
                currentTrackTitle = snapshot.child("currentTitle").getValue(String::class.java) ?: "No Track Playing"
                currentTrackArtist = snapshot.child("currentArtist").getValue(String::class.java) ?: ""

                isPlayingGlobal = isPlaying

                if (streamUrl.isNotEmpty() && streamUrl != currentTrackUrl) {
                    currentTrackUrl = streamUrl

                    extractionJob?.cancel()

                    // 🚀 NATIVE REPLACEMENT: Run local Python audio extraction outside main UI lock threads

                    extractionJob = lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val python = Python.getInstance()
                            val module = python.getModule("yt_engine")
                            val pyUrlResult = module.callAttr("get_clean_stream_url", streamUrl)
                            val dynamicResolvedUrl = pyUrlResult?.toString()

                            if (!dynamicResolvedUrl.isNullOrBlank() && isActive) {
                                withContext(Dispatchers.Main) {
                                    val mediaItem = MediaItem.fromUri(android.net.Uri.parse(dynamicResolvedUrl))
                                    exoPlayer.setMediaItem(mediaItem)
                                    exoPlayer.prepare()
                                    if (isPlayingGlobal) exoPlayer.play()
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ShikaPlayer", "Local CPython integration tracking error", e)
                        }
                    }
                } else if (streamUrl.isEmpty()) {
                    extractionJob?.cancel()
                    exoPlayer.stop()
                    currentTrackUrl = ""
                }

                if (streamUrl.isNotEmpty()) {
                    if (isPlaying) exoPlayer.play() else exoPlayer.pause()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun leaveCurrentRoom() {
        myUserRef?.removeValue()
        exoPlayer.stop()
        currentTrackUrl = ""
        isPlayingGlobal = false
        currentQueueState.clear()
        currentTrackTitle = "No Track Playing"
        currentTrackArtist = ""

        queueListener?.let { currentRoomRef?.child("queue")?.removeEventListener(it) }
        playbackListener?.let { currentRoomRef?.child("playbackState")?.removeEventListener(it) }
        usersListener?.let { currentRoomRef?.child("users")?.removeEventListener(it) }

        queueListener = null; playbackListener = null; usersListener = null
        myUserRef = null; currentRoomRef = null; activeRoomId = ""
        activeUsersCount = 1
    }

    override fun onDestroy() {
        super.onDestroy()
        leaveCurrentRoom()
        exoPlayer.release()
    }
}

// ==================== NAVIGATION COMPOSABLE BLOCKS ====================

@Composable
fun RoomSelectionScreen(onRoomSelected: (String) -> Unit) {
    var roomInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Join a Room",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = roomInput,
                onValueChange = { roomInput = it },
                label = { Text("Enter Room Name / ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = androidx.compose.ui.graphics.Color(0xFF000000),
                    unfocusedTextColor = androidx.compose.ui.graphics.Color(0xFF000000),
                    focusedBorderColor = androidx.compose.ui.graphics.Color(0xFF00E5FF),
                    focusedLabelColor = androidx.compose.ui.graphics.Color(0xFF00E5FF),
                    cursorColor = androidx.compose.ui.graphics.Color(0xFF000000)
                )
            )

            Button(
                onClick = { if (roomInput.isNotBlank()) onRoomSelected(roomInput.trim()) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = roomInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF00E5FF),
                    disabledContainerColor = androidx.compose.ui.graphics.Color(0xFF002D3A),
                    contentColor = androidx.compose.ui.graphics.Color(0xFF000000),
                    disabledContentColor = androidx.compose.ui.graphics.Color(0xFF00A3B8)
                )
            ) {
                Text(text="Connect", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun ShikaAppMainContainer(
    roomId: String,
    roomRef: DatabaseReference,
    queueList: List<QueueItem>,
    isPlaying: Boolean,
    activeUsers: Int,
    trackTitle: String,
    trackArtist: String,
    onTogglePlay: (Boolean) -> Unit,
    onSkipTrack: () -> Unit,
    onLeaveRoom: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Player") },
                    label = { Text("Player") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Queue") },
                    label = { Text("Search & Queue") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (selectedTab == 0) {
                RoomDashboardScreen(
                    roomId = roomId,
                    activeUsersCount = activeUsers,
                    isPlaying = isPlaying,
                    trackTitle = trackTitle,
                    trackArtist = trackArtist,
                    onTogglePlay = onTogglePlay,
                    onSkipTrack = onSkipTrack,
                    onLeaveRoom = onLeaveRoom
                )
            } else {
                QueueAndSearchScreen(roomRef = roomRef, currentQueue = queueList)
            }
        }
    }
}
