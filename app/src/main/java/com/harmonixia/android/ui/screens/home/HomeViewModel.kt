package com.harmonixia.android.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.repository.LocalMediaRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import com.harmonixia.android.util.NetworkConnectivityManager
import com.harmonixia.android.util.PrefetchScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicAssistantRepository,
    private val localMediaRepository: LocalMediaRepository,
    getConnectionStateUseCase: GetConnectionStateUseCase,
    private val networkConnectivityManager: NetworkConnectivityManager,
    private val prefetchScheduler: PrefetchScheduler
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = getConnectionStateUseCase()
    val isOfflineMode: StateFlow<Boolean> = networkConnectivityManager.networkAvailabilityFlow
        .map { networkConnectivityManager.isOfflineMode() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            networkConnectivityManager.isOfflineMode()
        )
    private val offlineModeChanges = isOfflineMode

    private val offlineStatsFlow: StateFlow<HomeUiState.Offline> = combine(
        localMediaRepository.getAllAlbums().map { it.size },
        localMediaRepository.getAllArtists().map { it.size },
        localMediaRepository.getAllTracks().map { it.size }
    ) { albumCount, artistCount, trackCount ->
        HomeUiState.Offline(albumCount, artistCount, trackCount)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState.Offline(0, 0, 0)
    )

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            combine(offlineModeChanges, offlineStatsFlow) { offline, offlineState ->
                if (offline) offlineState else null
            }.collect { offlineState ->
                if (offlineState != null) {
                    _uiState.value = offlineState
                }
            }
        }
        viewModelScope.launch {
            combine(connectionState, offlineModeChanges) { state, offline ->
                state to offline
            }.collect { (state, offline) ->
                if (offline) {
                    return@collect
                }
                when (state) {
                    is ConnectionState.Connected -> loadHomeData()
                    is ConnectionState.Connecting -> _uiState.value = HomeUiState.Loading
                    is ConnectionState.Disconnected -> _uiState.value = HomeUiState.Loading
                    is ConnectionState.Error -> _uiState.value = HomeUiState.Error(state.message)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadHomeData()
            _isRefreshing.value = false
        }
    }

    private suspend fun loadHomeData() {
        if (isOfflineMode.value) {
            return
        }
        when (val state = connectionState.value) {
            is ConnectionState.Connected -> Unit
            is ConnectionState.Error -> {
                _uiState.value = HomeUiState.Error(state.message)
                return
            }
            else -> {
                _uiState.value = HomeUiState.Loading
                return
            }
        }
        if (!_isRefreshing.value) {
            _uiState.value = HomeUiState.Loading
        }
        supervisorScope {
            val recentlyPlayedDeferred = async { repository.fetchRecentlyPlayed(HOME_LIST_LIMIT) }
            val recentlyAddedDeferred = async { repository.fetchRecentlyAdded(HOME_LIST_LIMIT) }
            val recentlyPlayed = recentlyPlayedDeferred.await()
            val recentlyAdded = recentlyAddedDeferred.await()
            val error = recentlyPlayed.exceptionOrNull() ?: recentlyAdded.exceptionOrNull()
            if (error != null) {
                _uiState.value = HomeUiState.Error(error.message ?: "Unknown error")
                return@supervisorScope
            }
            val recentlyPlayedList = recentlyPlayed.getOrDefault(emptyList())
            val recentlyAddedList = recentlyAdded.getOrDefault(emptyList())
            _uiState.value = HomeUiState.Success(
                recentlyPlayed = recentlyPlayedList,
                recentlyAdded = recentlyAddedList
            )
            prefetchScheduler.scheduleAlbumTrackPrefetch(recentlyPlayedList)
        }
        viewModelScope.launch {
            repository.fetchAlbums(ALBUM_PREFETCH_LIMIT, 0)
        }
    }

    companion object {
        private const val HOME_LIST_LIMIT = 9
        private const val ALBUM_PREFETCH_LIMIT = 50
    }
}
