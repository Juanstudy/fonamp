package com.fonamp.feature.podcast

import com.fonamp.core.database.PodcastSubscription
import com.fonamp.core.database.PodcastSubscriptionDao
import com.fonamp.core.player.PlayerManager
import com.fonamp.provider.api.AudioItem
import com.fonamp.provider.api.BrowseQuery
import com.fonamp.provider.api.Source
import com.fonamp.provider.api.SourceResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface PodcastUiState {
    data object Loading : PodcastUiState
    data class Error(val message: String) : PodcastUiState
    data class Discover(val results: List<AudioItem>) : PodcastUiState
    data class ShowDetails(val show: AudioItem, val episodes: List<AudioItem>, val isSubscribed: Boolean) : PodcastUiState
}

class PodcastViewModel(
    private val source: Source,
    private val dao: PodcastSubscriptionDao,
    private val player: PlayerManager,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    private val _state = MutableStateFlow<PodcastUiState>(PodcastUiState.Discover(emptyList()))
    val state: StateFlow<PodcastUiState> = _state.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<PodcastSubscription>>(emptyList())
    val subscriptions: StateFlow<List<PodcastSubscription>> = _subscriptions.asStateFlow()

    private var searchJob: Job? = null

    init {
        scope.launch(io) {
            dao.getAllSubscriptions().collectLatest { subs ->
                _subscriptions.value = subs
                val curr = _state.value
                if (curr is PodcastUiState.ShowDetails) {
                    val subbed = subs.any { it.feedUrl == curr.show.streamUri }
                    _state.value = curr.copy(isSubscribed = subbed)
                }
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _state.value = PodcastUiState.Discover(emptyList())
            return
        }
        searchJob?.cancel()
        searchJob = scope.launch(io) {
            _state.value = PodcastUiState.Loading
            delay(500)
            val result = source.search(query)
            if (result is SourceResult.Ok) {
                _state.value = PodcastUiState.Discover(result.v)
            } else {
                _state.value = PodcastUiState.Error("Failed to search podcasts")
            }
        }
    }

    fun openShow(show: AudioItem) {
        scope.launch(io) {
            _state.value = PodcastUiState.Loading
            val feedUrl = show.streamUri
            val result = source.browse(BrowseQuery(feedUrl = feedUrl))
            if (result is SourceResult.Ok) {
                val isSubbed = _subscriptions.value.any { it.feedUrl == feedUrl }
                _state.value = PodcastUiState.ShowDetails(show, result.v, isSubbed)
            } else {
                _state.value = PodcastUiState.Error("Failed to load episodes")
            }
        }
    }

    fun backToDiscover() {
        _state.value = PodcastUiState.Discover(emptyList())
    }

    fun toggleSubscription(show: AudioItem) {
        scope.launch(io) {
            val feedUrl = show.streamUri
            val isSubbed = _subscriptions.value.any { it.feedUrl == feedUrl }
            if (isSubbed) {
                dao.unsubscribe(feedUrl)
            } else {
                dao.subscribe(
                    PodcastSubscription(
                        feedUrl = feedUrl,
                        title = show.title,
                        artworkUri = show.artworkUri,
                        author = show.subtitle
                    )
                )
            }
        }
    }

    fun playEpisode(episode: AudioItem) {
        player.playSingle(source.streamOf(episode))
    }
}