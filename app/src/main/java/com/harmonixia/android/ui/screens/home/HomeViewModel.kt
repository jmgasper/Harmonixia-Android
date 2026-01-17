package com.harmonixia.android.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.repository.LocalMediaRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import com.harmonixia.android.domain.usecase.PlayTrackUseCase
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.util.ImageQualityManager
import com.harmonixia.android.util.Logger
import com.harmonixia.android.util.NetworkConnectivityManager
import com.harmonixia.android.util.PrefetchScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicAssistantRepository,
    private val localMediaRepository: LocalMediaRepository,
    getConnectionStateUseCase: GetConnectionStateUseCase,
    private val networkConnectivityManager: NetworkConnectivityManager,
    private val prefetchScheduler: PrefetchScheduler,
    private val playTrackUseCase: PlayTrackUseCase,
    val imageQualityManager: ImageQualityManager
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
    private var refreshJob: Job? = null
    private var recommendationsJob: Job? = null

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
                    is ConnectionState.Connected -> loadHomeDataSafely(isUserRefresh = false)
                    is ConnectionState.Connecting -> _uiState.value = HomeUiState.Loading
                    is ConnectionState.Disconnected -> _uiState.value = HomeUiState.Loading
                    is ConnectionState.Error -> _uiState.value = HomeUiState.Error(state.message)
                }
            }
        }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _isRefreshing.value = true
            try {
                loadHomeDataSafely(isUserRefresh = true)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            if (isOfflineMode.value) return@launch
            playTrackUseCase(track)
        }
    }

    private suspend fun loadHomeData(isUserRefresh: Boolean) {
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
        if (!isUserRefresh) {
            _uiState.value = HomeUiState.Loading
        }
        supervisorScope {
            val recentlyPlayedDeferred = async(Dispatchers.IO) {
                repository.fetchRecentlyPlayed(HOME_LIST_LIMIT)
            }
            val recentlyAddedDeferred = async(Dispatchers.IO) {
                repository.fetchRecentlyAdded(HOME_LIST_LIMIT)
            }
            val recentlyPlayed = recentlyPlayedDeferred.await()
            val recentlyAdded = recentlyAddedDeferred.await()
            val error = recentlyPlayed.exceptionOrNull() ?: recentlyAdded.exceptionOrNull()
            if (error != null) {
                _uiState.value = HomeUiState.Error(error.message ?: "Unknown error")
                return@supervisorScope
            }
            val recentlyPlayedList = recentlyPlayed.getOrDefault(emptyList())
            val recentlyAddedList = recentlyAdded.getOrDefault(emptyList())
            if (isUserRefresh) {
                val previousSuccess = _uiState.value as? HomeUiState.Success
                _uiState.value = HomeUiState.Success(
                    recentlyPlayed = recentlyPlayedList,
                    recentlyAdded = recentlyAddedList,
                    recommendations = previousSuccess?.recommendations.orEmpty(),
                    recommendationsLoadError = previousSuccess?.recommendationsLoadError ?: false
                )
                scheduleRecommendationsRefresh(recentlyPlayedList, recentlyAddedList)
            } else {
                val recommendations = async(Dispatchers.IO) {
                    repository.fetchRecommendations()
                }.await()
                val recommendationsList = recommendations.getOrDefault(emptyList())
                _uiState.value = HomeUiState.Success(
                    recentlyPlayed = recentlyPlayedList,
                    recentlyAdded = recentlyAddedList,
                    recommendations = recommendationsList,
                    recommendationsLoadError = recommendations.isFailure
                )
            }
            prefetchScheduler.scheduleAlbumTrackPrefetch(recentlyPlayedList)
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.fetchAlbums(ALBUM_PREFETCH_LIMIT, 0)
        }
    }

    private suspend fun loadHomeDataSafely(isUserRefresh: Boolean) {
        try {
            loadHomeData(isUserRefresh)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Logger.e(TAG, "Failed to load home data", error)
            _uiState.value = HomeUiState.Error(error.message ?: "Unknown error")
        }
    }

    private fun scheduleRecommendationsRefresh(
        recentlyPlayed: List<Album>,
        recentlyAdded: List<Album>
    ) {
        recommendationsJob?.cancel()
        recommendationsJob = viewModelScope.launch {
            val recommendations = withContext(Dispatchers.IO) {
                repository.fetchRecommendations()
            }
            if (isOfflineMode.value) return@launch
            val currentState = _uiState.value as? HomeUiState.Success ?: return@launch
            if (currentState.recentlyPlayed != recentlyPlayed ||
                currentState.recentlyAdded != recentlyAdded
            ) {
                return@launch
            }
            _uiState.value = currentState.copy(
                recommendations = recommendations.getOrDefault(emptyList()),
                recommendationsLoadError = recommendations.isFailure
            )
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
        private const val HOME_LIST_LIMIT = 9
        private const val ALBUM_PREFETCH_LIMIT = 50
    }
}
