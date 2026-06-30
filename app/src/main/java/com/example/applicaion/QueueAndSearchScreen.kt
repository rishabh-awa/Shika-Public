package com.example.applicaion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.chaquo.python.Python
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QueueAndSearchScreen(
    roomRef: DatabaseReference,
    currentQueue: List<QueueItem>
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<QueueItem>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- LAYER 1: BASE BACKING CONTROLS & LIVE QUEUE ---
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search tracks via engine...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Input")
                        }
                    }
                }
            )

            Button(
                onClick = {
                    if (searchQuery.isNotBlank()) {
                        isSearching = true
                        searchResults = emptyList()

                        // 🚀 CALL LOCAL PYTHON ENGINE VIA COROUTINES: Restores high speed, localized extraction loops
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val python = Python.getInstance()
                                val module = python.getModule("yt_engine")
                                val pyList = module.callAttr("search_youtube_tracks", searchQuery).asList()

                                val models = pyList.map { pyItem ->
                                    // 🚀 FIXED: Call asMap() to safely bind Python dict properties to explicit strings
                                    @Suppress("UNCHECKED_CAST")
                                    val dictMap = pyItem.asMap() as Map<String, Any?>
                                    val idStr = dictMap["id"]?.toString() ?: ""
                                    QueueItem(
                                        id = idStr,
                                        title = dictMap["title"]?.toString() ?: "Unknown Title",
                                        artist = dictMap["artist"]?.toString() ?: "Unknown Artist",
                                        streamUrl = idStr
                                    )
                                }

                                withContext(Dispatchers.Main) {
                                    searchResults = models
                                    isSearching = false
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("QueueAndSearchScreen", "Python search context crash tracking", e)
                                withContext(Dispatchers.Main) { isSearching = false }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.End),
                enabled = !isSearching
            ) {
                Text(if (isSearching) "Searching..." else "Search")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Up Next in Queue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(currentQueue) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            IconButton(
                                onClick = { roomRef.child("queue").child(item.id).removeValue() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Drop item",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- LAYER 2: FLOATING OVERLAY DROPDOWN RESULTS VIEWBAR ---
        if (searchResults.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 125.dp)
                    .heightIn(max = 280.dp)
                    .zIndex(15f),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(searchResults) { track ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newTrackRef = roomRef.child("queue").push()

                                    // Construct clean parameter block matching your exact mapping structure requirements
                                    val dynamicItemMap = mapOf(
                                        "title" to track.title,
                                        "artist" to track.artist,
                                        "streamUrl" to track.streamUrl
                                    )
                                    newTrackRef.setValue(dynamicItemMap)

                                    searchResults = emptyList()
                                    searchQuery = ""
                                }
                                .padding(16.dp)
                        ) {
                            Text(track.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}