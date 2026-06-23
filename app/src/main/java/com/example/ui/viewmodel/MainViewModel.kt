package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.data.model.*
import com.example.data.repository.IptvRepository
import com.example.data.repository.ValidatorWorker
import com.example.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: IptvRepository = ServiceLocator.getIptvRepository(application)
    private val sharedPrefs = application.getSharedPreferences("m4ditv_prefs", Context.MODE_PRIVATE)

    // Current State
    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    private val _themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _language = MutableStateFlow(sharedPrefs.getString("language", "id") ?: "id")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _bufferSizeMs = MutableStateFlow(sharedPrefs.getInt("buffer_size_ms", 50000))
    val bufferSizeMs: StateFlow<Int> = _bufferSizeMs.asStateFlow()

    val profilesState: StateFlow<List<Profile>> = repository.allProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Search and Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL") // ALL, ONLINE, OFFLINE
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    // Playlists & Channels depending on current profile
    val playlists: StateFlow<List<Playlist>> = _currentProfile
        .flatMapLatest { profile ->
            if (profile != null) repository.getPlaylistsFlow(profile.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChannels: StateFlow<List<Channel>> = _currentProfile
        .flatMapLatest { profile ->
            if (profile != null) repository.getChannelsFlow(profile.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = _currentProfile
        .flatMapLatest { profile ->
            if (profile != null) repository.getCategoriesFlow(profile.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered channel output
    val filteredChannels: StateFlow<List<Channel>> = combine(
        allChannels,
        searchQuery,
        selectedCategory,
        statusFilter
    ) { channels, query, category, statusValue ->
        channels.filter { channel ->
            val matchesQuery = query.isEmpty() || channel.title.contains(query, ignoreCase = true) ||
                    (channel.category?.contains(query, ignoreCase = true) == true)
            val matchesCategory = category == null || channel.category == category
            val matchesStatus = when (statusValue) {
                "ONLINE" -> channel.status == "ONLINE"
                "OFFLINE" -> channel.status == "OFFLINE"
                else -> true
            }
            matchesQuery && matchesCategory && matchesStatus
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorites & Bookmarks & History
    val favorites: StateFlow<List<Channel>> = _currentProfile
        .flatMapLatest { profile ->
            if (profile != null) repository.getFavoritesFlow(profile.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarked: StateFlow<List<Channel>> = _currentProfile
        .flatMapLatest { profile ->
            if (profile != null) repository.getBookmarkedFlow(profile.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryItem>> = _currentProfile
        .flatMapLatest { profile ->
            if (profile != null) repository.getHistoryFlow(profile.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Validation Progress State
    private val _validationProgress = MutableStateFlow(0f)
    val validationProgress: StateFlow<Float> = _validationProgress.asStateFlow()

    private val _isCheckingUrl = MutableStateFlow(false)
    val isCheckingUrl: StateFlow<Boolean> = _isCheckingUrl.asStateFlow()

    // EPG list
    val allEPGPrograms: StateFlow<List<EPGProgram>> = repository.getAllProgramsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureDefaultProfilesExist()
            val profiles = repository.getProfiles()
            if (profiles.isNotEmpty()) {
                selectProfile(profiles.first())
            }
            val pList = repository.getPlaylists(currentProfile.value?.id ?: 1)
            if (pList.isEmpty()) {
                fetchDefaultPlaylist()
            }
        }
    }

    private fun fetchDefaultPlaylist() {
        viewModelScope.launch {
            val profId = currentProfile.value?.id ?: 1
            repository.addPlaylistFromUrl(
                "M4DiTV Default",
                "https://rizkyevory.github.io/merged_iptv_simple.m3u",
                profId
            )
        }
    }

    fun selectProfile(profile: Profile) {
        _currentProfile.value = profile
        sharedPrefs.edit().putInt("selected_profile_id", profile.id).apply()
    }

    fun createProfile(name: String, pin: String?, isKid: Boolean) {
        viewModelScope.launch {
            repository.createProfile(Profile(name = name, pin = pin, isKid = isKid))
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            if (_currentProfile.value?.id == profile.id) {
                val rem = repository.getProfiles()
                if (rem.isNotEmpty()) {
                    selectProfile(rem.first())
                }
            }
        }
    }

    // Playlist Add Methods
    fun addPlaylistUrl(name: String, url: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val profId = currentProfile.value?.id ?: 1
            val success = repository.addPlaylistFromUrl(name, url, profId)
            onComplete(success)
        }
    }

    fun addPlaylistPaste(name: String, text: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val profId = currentProfile.value?.id ?: 1
            val success = repository.addPlaylistFromRawText(name, text, profId)
            onComplete(success)
        }
    }

    fun mergePlaylists(selectedIds: List<Int>, mergedName: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val profId = currentProfile.value?.id ?: 1
            val success = repository.mergePlaylists(selectedIds, profId, mergedName)
            onComplete(success)
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun exportPlaylist(playlistId: Int, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val profId = currentProfile.value?.id ?: 1
            val m3uStr = repository.exportPlaylistToM3u(playlistId, profId)
            onComplete(m3uStr)
        }
    }

    // Settings
    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode).apply()
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        sharedPrefs.edit().putString("language", lang).apply()
    }

    fun setBufferSize(sizeMs: Int) {
        _bufferSizeMs.value = sizeMs
        sharedPrefs.edit().putInt("buffer_size_ms", sizeMs).apply()
    }

    fun clearCache() {
        viewModelScope.launch {
            val profId = currentProfile.value?.id ?: 1
            repository.clearHistory(profId)
        }
    }

    // Search and Filters
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun setStatusFilter(status: String) {
        _statusFilter.value = status
    }

    // Favorites & Bookmarks & Watch History helpers
    fun toggleFavorite(channelId: Int, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(channelId, isFav)
        }
    }

    fun toggleBookmark(channelId: Int, isBookmarked: Boolean) {
        viewModelScope.launch {
            repository.toggleBookmark(channelId, isBookmarked)
        }
    }

    fun recordHistory(channel: Channel) {
        viewModelScope.launch {
            val profId = currentProfile.value?.id ?: 1
            repository.addHistoryItem(
                HistoryItem(
                    channelId = channel.id,
                    channelTitle = channel.title,
                    url = channel.url,
                    logoUrl = channel.logoUrl,
                    profileId = profId
                )
            )
        }
    }

    // Manual bulk validator with Real-Time UI status progress updates!
    fun runUrlValidator() {
        if (_isCheckingUrl.value) return
        viewModelScope.launch(Dispatchers.Default) {
            _isCheckingUrl.value = true
            _validationProgress.value = 0f
            val channels = filteredChannels.value
            val total = channels.size
            if (total > 0) {
                channels.forEachIndexed { index, channel ->
                    repository.validateChannelUrl(channel)
                    _validationProgress.value = (index + 1) / total.toFloat()
                }
            }
            _validationProgress.value = 1f
            _isCheckingUrl.value = false
        }
    }

    fun clearOfflineChannels() {
        viewModelScope.launch {
            val profId = currentProfile.value?.id ?: 1
            repository.deleteOfflineChannels(profId)
        }
    }

    // Interactive XMLTV Guide downloader
    fun reloadXmlEPG(url: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.fetchAndParseEPG(url)
            onComplete(success)
        }
    }

    // Background Scheduler configuration for WorkManager
    fun configurePeriodicValidation(hoursInterval: Int) {
        val workManager = WorkManager.getInstance(getApplication())
        workManager.cancelAllWorkByTag("ValidationWorker")
        
        if (hoursInterval > 0) {
            val validationRequest = PeriodicWorkRequestBuilder<ValidatorWorker>(
                hoursInterval.toLong(), TimeUnit.HOURS
            )
            .addTag("ValidationWorker")
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
            
            workManager.enqueueUniquePeriodicWork(
                "PeriodicChannelAudit",
                ExistingPeriodicWorkPolicy.UPDATE,
                validationRequest
            )
        }
    }
}
