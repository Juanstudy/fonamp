package com.fonamp.feature.podcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fonamp.provider.api.AudioItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastScreen(
    viewModel: PodcastViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            is PodcastUiState.Discover -> {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.search(it)
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("Search podcasts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                if (s.results.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(s.results) { show ->
                            PodcastRow(show = show, onClick = { viewModel.openShow(show) })
                        }
                    }
                } else if (searchQuery.isBlank() && subscriptions.isNotEmpty()) {
                    Text(
                        "Your Subscriptions",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(subscriptions) { sub ->
                            val fakeShow = AudioItem(
                                sourceId = "podcast",
                                stableId = sub.feedUrl,
                                title = sub.title,
                                subtitle = sub.author,
                                streamUri = sub.feedUrl,
                                artworkUri = sub.artworkUri
                            )
                            PodcastRow(show = fakeShow, onClick = { viewModel.openShow(fakeShow) })
                        }
                    }
                }
            }
            is PodcastUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PodcastUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.backToDiscover() }) {
                            Text("Back")
                        }
                    }
                }
            }
            is PodcastUiState.ShowDetails -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = { Text(s.show.title, maxLines = 1) },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.backToDiscover() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.toggleSubscription(s.show) }) {
                                Icon(
                                    if (s.isSubscribed) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = "Subscribe"
                                )
                            }
                        }
                    )
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(s.episodes) { ep ->
                            EpisodeRow(episode = ep, onClick = { viewModel.playEpisode(ep) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PodcastRow(show: AudioItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = show.artworkUri,
            contentDescription = null,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(show.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            if (show.subtitle != null) {
                Text(show.subtitle!!, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
        }
    }
}

@Composable
fun EpisodeRow(episode: AudioItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClick) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(episode.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
            if (episode.durationMs != null) {
                val mins = episode.durationMs!! / 60000
                Text("$mins min", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}