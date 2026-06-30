package com.example.applicaion

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDashboardScreen(
    roomId: String,
    activeUsersCount: Int,
    isPlaying: Boolean,
    trackTitle: String,
    trackArtist: String,
    onTogglePlay: (Boolean) -> Unit,
    onSkipTrack: () -> Unit,
    onLeaveRoom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HOOK: App Title Header followed by Session Metadata Sub-toolbar
        Column(modifier = Modifier.fillMaxWidth()) {
            CenterAlignedTopAppBar(
                title = { Text("Shika", fontWeight = FontWeight.Black, letterSpacing = 1.5.sp) }
            )

            // Sub-Toolbar card row layout containing Room info counters
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Room ID: ", style = MaterialTheme.typography.bodyMedium)
                            Text(roomId, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("In Room: $activeUsersCount active users", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onLeaveRoom) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Disconnect Session",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // MID HORIZONTAL VIEWSPACE: Renders metadata about the currently playing stream
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = trackTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            if (trackArtist.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trackArtist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        // LOWER FOOTER AREA: Centered circular controllers anchored right above bottom tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(56.dp)) // Offsets skip icon symmetry perfectly

            // Centered circular Play/Pause Ring
            FilledIconButton(
                onClick = { onTogglePlay(!isPlaying) },
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Toggle Player State",
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Skip right hand side attachment layout
            IconButton(
                onClick = onSkipTrack,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Advance Track forward",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}