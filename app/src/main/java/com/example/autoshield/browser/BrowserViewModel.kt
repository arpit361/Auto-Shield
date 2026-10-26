package com.example.autoshield.browser

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.example.autoshield.media.CarMediaSessionManager
import com.example.autoshield.shield.BraveShieldEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SpeedDialItem(
    val id: String,
    val title: String,
    val url: String,
    val brandColorHex: String = "#FF5500",
    val isDefault: Boolean = false
)

data class BrowserTab(
    val id: String,
    val title: String = "New Tab",
    val url: String = "brave://newtab",
    val isHomePage: Boolean = true
)

data class Bookmark(
    val title: String,
    val url: String,
    val iconName: String = "web",
    val isDefault: Boolean = false
)

data class BrowserUiState(
    val isHomePage: Boolean = true,
    val currentUrl: String = "brave://newtab",
    val pageTitle: String = "New Tab",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isDesktopMode: Boolean = false,
    val isCarCockpitMode: Boolean = false,
    val isFullscreenVideo: Boolean = false,
    val isScreenLocked: Boolean = false,
    val showShieldsDialog: Boolean = false,
    val showPassengerNoticeDialog: Boolean = false,
    val showBookmarksDialog: Boolean = false,
    val showAuditDialog: Boolean = false,
    val showAndroidAutoSetupDialog: Boolean = false,
    val showAddSpeedDialDialog: Boolean = false,
    val showTabsDialog: Boolean = false,
    val showMoreMenu: Boolean = false,
    val speedDials: List<SpeedDialItem> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val tabs: List<BrowserTab> = listOf(BrowserTab("tab_init", "New Tab", "brave://newtab", true)),
    val activeTabId: String = "tab_init"
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences("autoshield_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    val shieldStats = BraveShieldEngine.stats
    val activeVideo = CarMediaSessionManager.videoState

    init {
        loadSpeedDials()
        loadBookmarks()
    }

    private fun loadSpeedDials() {
        val defaultSpeedDials = listOf(
            SpeedDialItem("sd_yt", "YouTube", "https://m.youtube.com", "#FF0000", true),
            SpeedDialItem("sd_gg", "Google", "https://www.google.com", "#4285F4", true),
            SpeedDialItem("sd_jio", "JioCinema", "https://www.jiocinema.com", "#0A58CA", true),
            SpeedDialItem("sd_hot", "Hotstar", "https://www.hotstar.com", "#111827", true),
            SpeedDialItem("sd_nflx", "Netflix", "https://www.netflix.com", "#E50914", true),
            SpeedDialItem("sd_prm", "Prime Video", "https://www.primevideo.com", "#00A8E1", true),
            SpeedDialItem("sd_tw", "Twitch", "https://m.twitch.tv", "#9146FF", true),
            SpeedDialItem("sd_tata", "Tata Motors", "https://cars.tatamotors.com", "#005A9E", true),
            SpeedDialItem("sd_rd", "Reddit", "https://www.reddit.com", "#FF4500", true),
            SpeedDialItem("sd_wiki", "Wikipedia", "https://www.wikipedia.org", "#3366CC", true)
        )

        val savedJson = prefs.getString("custom_speed_dials", null)
        val loadedList = mutableListOf<SpeedDialItem>().apply { addAll(defaultSpeedDials) }

        if (!savedJson.isNullOrBlank()) {
            try {
                val array = JSONArray(savedJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    loadedList.add(
                        SpeedDialItem(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            url = obj.getString("url"),
                            brandColorHex = obj.optString("color", "#FF5500"),
                            isDefault = false
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }

        _uiState.update { it.copy(speedDials = loadedList) }
    }

    fun addSpeedDial(title: String, url: String, colorHex: String = "#FF5500") {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        val newItem = SpeedDialItem(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            url = cleanUrl,
            brandColorHex = colorHex,
            isDefault = false
        )

        val updated = _uiState.value.speedDials + newItem
        _uiState.update { it.copy(speedDials = updated, showAddSpeedDialDialog = false) }

        saveSpeedDialsToPrefs(updated)
    }

    fun removeSpeedDial(id: String) {
        val updated = _uiState.value.speedDials.filter { it.id != id || it.isDefault }
        _uiState.update { it.copy(speedDials = updated) }
        saveSpeedDialsToPrefs(updated)
    }

    private fun saveSpeedDialsToPrefs(list: List<SpeedDialItem>) {
        val customOnly = list.filter { !it.isDefault }
        val array = JSONArray()
        customOnly.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            obj.put("url", it.url)
            obj.put("color", it.brandColorHex)
            array.put(obj)
        }
        prefs.edit().putString("custom_speed_dials", array.toString()).apply()
    }

    private fun loadBookmarks() {
        val defaultBookmarks = listOf(
            Bookmark("YouTube", "https://m.youtube.com", "youtube", true),
            Bookmark("JioCinema", "https://www.jiocinema.com", "movie", true),
            Bookmark("Hotstar", "https://www.hotstar.com", "tv", true),
            Bookmark("Netflix", "https://www.netflix.com", "movie", true),
            Bookmark("Prime Video", "https://www.primevideo.com", "play", true),
            Bookmark("Twitch", "https://m.twitch.tv", "videogame_asset", true),
            Bookmark("Tata Motors", "https://cars.tatamotors.com", "directions_car", true)
        )
        _uiState.update { it.copy(bookmarks = defaultBookmarks) }
    }

    fun saveBookmark(title: String, url: String) {
        val newBookmark = Bookmark(title = title, url = url, iconName = "bookmark", isDefault = false)
        val updated = _uiState.value.bookmarks + newBookmark
        _uiState.update { it.copy(bookmarks = updated) }
    }

    fun removeBookmark(url: String) {
        val updated = _uiState.value.bookmarks.filter { it.url != url || it.isDefault }
        _uiState.update { it.copy(bookmarks = updated) }
    }

    fun navigateTo(queryOrUrl: String) {
        var clean = queryOrUrl.trim()
        if (clean == "brave://newtab" || clean.isEmpty()) {
            goHome()
            return
        }

        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = if (clean.contains(".") && !clean.contains(" ")) {
                "https://$clean"
            } else {
                "https://www.google.com/search?q=${clean.replace(" ", "+")}"
            }
        }

        val activeTabId = _uiState.value.activeTabId
        val updatedTabs = _uiState.value.tabs.map { tab ->
            if (tab.id == activeTabId) {
                tab.copy(url = clean, isHomePage = false, title = clean)
            } else tab
        }

        _uiState.update {
            it.copy(
                isHomePage = false,
                currentUrl = clean,
                isLoading = true,
                tabs = updatedTabs,
                showMoreMenu = false
            )
        }
    }

    fun goHome() {
        val activeTabId = _uiState.value.activeTabId
        val updatedTabs = _uiState.value.tabs.map { tab ->
            if (tab.id == activeTabId) {
                tab.copy(url = "brave://newtab", isHomePage = true, title = "New Tab")
            } else tab
        }

        _uiState.update {
            it.copy(
                isHomePage = true,
                currentUrl = "brave://newtab",
                pageTitle = "New Tab",
                isLoading = false,
                tabs = updatedTabs,
                showMoreMenu = false
            )
        }
    }

    fun openNewTab(url: String = "brave://newtab") {
        val newId = UUID.randomUUID().toString()
        val isHome = url == "brave://newtab"
        val newTab = BrowserTab(newId, if (isHome) "New Tab" else url, url, isHome)
        val updatedTabs = _uiState.value.tabs + newTab

        _uiState.update {
            it.copy(
                tabs = updatedTabs,
                activeTabId = newId,
                isHomePage = isHome,
                currentUrl = url,
                pageTitle = newTab.title,
                showTabsDialog = false,
                showMoreMenu = false
            )
        }
    }

    fun closeTab(id: String) {
        val currentTabs = _uiState.value.tabs
        if (currentTabs.size <= 1) {
            // Keep at least one tab open, reset to home
            val resetTab = BrowserTab("tab_single", "New Tab", "brave://newtab", true)
            _uiState.update {
                it.copy(
                    tabs = listOf(resetTab),
                    activeTabId = "tab_single",
                    isHomePage = true,
                    currentUrl = "brave://newtab",
                    pageTitle = "New Tab"
                )
            }
            return
        }

        val remainingTabs = currentTabs.filter { it.id != id }
        val newActiveTab = if (_uiState.value.activeTabId == id) {
            remainingTabs.last()
        } else {
            remainingTabs.first { it.id == _uiState.value.activeTabId }
        }

        _uiState.update {
            it.copy(
                tabs = remainingTabs,
                activeTabId = newActiveTab.id,
                isHomePage = newActiveTab.isHomePage,
                currentUrl = newActiveTab.url,
                pageTitle = newActiveTab.title
            )
        }
    }

    fun selectTab(id: String) {
        val target = _uiState.value.tabs.firstOrNull { it.id == id } ?: return
        _uiState.update {
            it.copy(
                activeTabId = target.id,
                isHomePage = target.isHomePage,
                currentUrl = target.url,
                pageTitle = target.title,
                showTabsDialog = false
            )
        }
    }

    fun updatePageStatus(title: String, url: String, canBack: Boolean, canForward: Boolean) {
        val activeId = _uiState.value.activeTabId
        val updatedTabs = _uiState.value.tabs.map { tab ->
            if (tab.id == activeId) {
                tab.copy(
                    title = if (title.isBlank()) url else title,
                    url = url,
                    isHomePage = false
                )
            } else tab
        }

        _uiState.update {
            it.copy(
                pageTitle = if (title.isBlank()) url else title,
                currentUrl = url,
                canGoBack = canBack,
                canGoForward = canForward,
                tabs = updatedTabs
            )
        }
    }

    fun setProgress(progress: Int) {
        _uiState.update {
            it.copy(
                progress = progress,
                isLoading = progress in 1..99
            )
        }
    }

    fun toggleDesktopMode() {
        _uiState.update { it.copy(isDesktopMode = !it.isDesktopMode, showMoreMenu = false) }
    }

    fun toggleCarCockpitMode() {
        _uiState.update { it.copy(isCarCockpitMode = !it.isCarCockpitMode) }
    }

    fun setFullscreenVideo(fullscreen: Boolean) {
        _uiState.update { it.copy(isFullscreenVideo = fullscreen) }
    }

    fun toggleScreenLock() {
        _uiState.update { it.copy(isScreenLocked = !it.isScreenLocked) }
    }

    fun showShieldsDialog(show: Boolean) {
        _uiState.update { it.copy(showShieldsDialog = show, showMoreMenu = false) }
    }

    fun showPassengerNoticeDialog(show: Boolean) {
        _uiState.update { it.copy(showPassengerNoticeDialog = show, showMoreMenu = false) }
    }

    fun showBookmarksDialog(show: Boolean) {
        _uiState.update { it.copy(showBookmarksDialog = show, showMoreMenu = false) }
    }

    fun showAuditDialog(show: Boolean) {
        _uiState.update { it.copy(showAuditDialog = show, showMoreMenu = false) }
    }

    fun showAndroidAutoSetupDialog(show: Boolean) {
        _uiState.update { it.copy(showAndroidAutoSetupDialog = show, showMoreMenu = false) }
    }

    fun showAddSpeedDialDialog(show: Boolean) {
        _uiState.update { it.copy(showAddSpeedDialDialog = show) }
    }

    fun showTabsDialog(show: Boolean) {
        _uiState.update { it.copy(showTabsDialog = show, showMoreMenu = false) }
    }

    fun showMoreMenu(show: Boolean) {
        _uiState.update { it.copy(showMoreMenu = show) }
    }
}
