package com.example.autoshield.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.autoshield.browser.AutoWebView
import com.example.autoshield.browser.BrowserViewModel
import com.example.autoshield.media.CarMediaSessionManager
import com.example.autoshield.media.CarPlaybackService
import com.example.ui.theme.BraveAmber
import com.example.ui.theme.BraveOrange
import com.example.ui.theme.CockpitBackground
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.CockpitTextMuted
import com.example.ui.theme.CockpitTextPrimary
import com.example.ui.theme.CockpitTextSecondary
import com.example.ui.theme.ShieldCyan
import com.example.ui.theme.ShieldGreen

@Composable
fun AutoShieldHomeScreen(
    viewModel: BrowserViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val shieldStats by viewModel.shieldStats.collectAsState()
    val activeVideo by viewModel.activeVideo.collectAsState()

    var customView by remember { mutableStateOf<View?>(null) }
    var urlInput by remember { mutableStateOf(uiState.currentUrl) }
    val focusManager = LocalFocusManager.current

    // Sync input when page url updates
    if (!uiState.isHomePage && urlInput != uiState.currentUrl && !uiState.isLoading) {
        urlInput = uiState.currentUrl
    }

    // Handle back navigation
    BackHandler(enabled = customView != null || !uiState.isHomePage || uiState.canGoBack) {
        if (customView != null) {
            customView = null
            viewModel.setFullscreenVideo(false)
        } else if (uiState.canGoBack) {
            CarMediaSessionManager.onSkipBackwardCommand?.invoke()
        } else if (!uiState.isHomePage) {
            viewModel.goHome()
        }
    }

    // Start background car playback service
    remember {
        try {
            val serviceIntent = Intent(context, CarPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // Service already running
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // View Mode 1: Brave New Tab Page (Home Screen with Speed Dial)
            if (uiState.isHomePage && customView == null) {
                BraveHomeScreen(
                    viewModel = viewModel,
                    shieldStats = shieldStats,
                    speedDials = uiState.speedDials,
                    activeTabCount = uiState.tabs.size,
                    onNavigate = { url ->
                        urlInput = url
                        viewModel.navigateTo(url)
                    }
                )
            } else {
                // View Mode 2: Authentic Browser Chrome (when visiting a site like YouTube)
                if (customView == null && !uiState.isScreenLocked) {
                    BraveBrowserTopBar(
                        urlInput = urlInput,
                        onUrlInputChange = { urlInput = it },
                        onNavigate = {
                            focusManager.clearFocus()
                            viewModel.navigateTo(urlInput)
                        },
                        onGoHome = { viewModel.goHome() },
                        adsBlocked = shieldStats.adsBlocked,
                        shieldsActive = shieldStats.shieldsActive,
                        onOpenShields = { viewModel.showShieldsDialog(true) },
                        tabCount = uiState.tabs.size,
                        onOpenTabs = { viewModel.showTabsDialog(true) },
                        isDesktopMode = uiState.isDesktopMode,
                        onToggleDesktop = { viewModel.toggleDesktopMode() },
                        onOpenBookmarks = { viewModel.showBookmarksDialog(true) },
                        onOpenAndroidAutoSetup = { viewModel.showAndroidAutoSetupDialog(true) },
                        onOpenAudit = { viewModel.showAuditDialog(true) }
                    )

                    // Loading progress indicator
                    if (uiState.isLoading) {
                        LinearProgressIndicator(
                            progress = { uiState.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp),
                            color = BraveOrange,
                            trackColor = CockpitSurfaceVariant
                        )
                    }
                }

                // Active Web Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AutoWebView(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                        onCustomViewChanged = { view, _ ->
                            customView = view
                        }
                    )

                    // HTML5 Video Fullscreen Overlay
                    if (customView != null) {
                        AndroidView(
                            factory = { ctx ->
                                FrameLayout(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    addView(customView)
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )
                    }

                    // Pothole Lock Overlay
                    if (uiState.isScreenLocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("touch_lock_overlay"),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Surface(
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { viewModel.toggleScreenLock() },
                                color = CockpitSurface.copy(alpha = 0.9f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BraveOrange)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = BraveOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Screen Locked (Pothole Guard) • Tap to Unlock",
                                        color = CockpitTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Car Playback Strip
                if (customView == null && activeVideo.title.isNotBlank() && activeVideo.title != "No Active Video") {
                    CockpitMediaBar(
                        videoTitle = activeVideo.title,
                        isPlaying = activeVideo.isPlaying,
                        isLocked = uiState.isScreenLocked,
                        onToggleLock = { viewModel.toggleScreenLock() },
                        onPlayPause = {
                            if (activeVideo.isPlaying) {
                                CarMediaSessionManager.onPauseCommand?.invoke()
                            } else {
                                CarMediaSessionManager.onPlayCommand?.invoke()
                            }
                        },
                        onSkipBack = { CarMediaSessionManager.onSkipBackwardCommand?.invoke() },
                        onSkipForward = { CarMediaSessionManager.onSkipForwardCommand?.invoke() },
                        onEnterPip = {
                            val activity = context.findActivity()
                            if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val params = PictureInPictureParams.Builder()
                                    .setAspectRatio(Rational(16, 9))
                                    .build()
                                activity.enterPictureInPictureMode(params)
                            }
                        }
                    )
                }

                // Bottom Browser Navigation Bar (Brave Mobile layout)
                if (customView == null && !uiState.isScreenLocked) {
                    BraveBottomNavigationBar(
                        onBack = { CarMediaSessionManager.onSkipBackwardCommand?.invoke() },
                        onForward = { CarMediaSessionManager.onSkipForwardCommand?.invoke() },
                        onHome = { viewModel.goHome() },
                        onNewTab = { viewModel.openNewTab() },
                        tabCount = uiState.tabs.size,
                        onOpenTabs = { viewModel.showTabsDialog(true) },
                        onOpenShields = { viewModel.showShieldsDialog(true) }
                    )
                }
            }
        }

        // Active Dialogs & Sheets
        if (uiState.showAddSpeedDialDialog) {
            AddSpeedDialDialog(
                onAdd = { title, url, color ->
                    viewModel.addSpeedDial(title, url, color)
                },
                onDismiss = { viewModel.showAddSpeedDialDialog(false) }
            )
        }

        if (uiState.showTabsDialog) {
            TabsOverviewDialog(
                tabs = uiState.tabs,
                activeTabId = uiState.activeTabId,
                onSelectTab = { id -> viewModel.selectTab(id) },
                onCloseTab = { id -> viewModel.closeTab(id) },
                onNewTab = { viewModel.openNewTab() },
                onDismiss = { viewModel.showTabsDialog(false) }
            )
        }

        if (uiState.showShieldsDialog) {
            BraveShieldsDialog(
                stats = shieldStats,
                onDismiss = { viewModel.showShieldsDialog(false) }
            )
        }

        if (uiState.showPassengerNoticeDialog) {
            PassengerNoticeDialog(
                onDismiss = { viewModel.showPassengerNoticeDialog(false) }
            )
        }

        if (uiState.showBookmarksDialog) {
            BookmarksDialog(
                bookmarks = uiState.bookmarks,
                onSelectBookmark = { url ->
                    urlInput = url
                    viewModel.navigateTo(url)
                },
                onAddBookmark = { title, url ->
                    viewModel.saveBookmark(title, url)
                },
                onRemoveBookmark = { url ->
                    viewModel.removeBookmark(url)
                },
                onDismiss = { viewModel.showBookmarksDialog(false) }
            )
        }

        if (uiState.showAndroidAutoSetupDialog) {
            AndroidAutoSetupDialog(
                onDismiss = { viewModel.showAndroidAutoSetupDialog(false) }
            )
        }

        if (uiState.showAuditDialog) {
            AuditPrivacyDialog(
                onDismiss = { viewModel.showAuditDialog(false) }
            )
        }
    }
}

@Composable
private fun BraveBrowserTopBar(
    urlInput: String,
    onUrlInputChange: (String) -> Unit,
    onNavigate: () -> Unit,
    onGoHome: () -> Unit,
    adsBlocked: Int,
    shieldsActive: Boolean,
    onOpenShields: () -> Unit,
    tabCount: Int,
    onOpenTabs: () -> Unit,
    isDesktopMode: Boolean,
    onToggleDesktop: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenAndroidAutoSetup: () -> Unit,
    onOpenAudit: () -> Unit
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CockpitSurface,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Button with Brave Lion Emblem
            IconButton(onClick = onGoHome, modifier = Modifier.size(36.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_brave_lion),
                    contentDescription = "Brave Home",
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Brave Omnibox with inside Shield Badge & Padlock
            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlInputChange,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("url_input"),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = CockpitTextPrimary),
                leadingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        // Padlock icon
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secure",
                            tint = ShieldGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Authentic Brave Shield Pill with Lion & live counter
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenShields() },
                            color = if (shieldsActive) BraveOrange.copy(alpha = 0.2f) else Color.DarkGray,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (shieldsActive) BraveOrange else Color.Gray)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_brave_lion),
                                    contentDescription = "Brave Shields",
                                    modifier = Modifier.size(13.dp)
                                )
                                if (adsBlocked > 0) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "$adsBlocked",
                                        color = if (shieldsActive) BraveOrange else Color.LightGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                trailingIcon = {
                    if (urlInput.isNotBlank()) {
                        IconButton(onClick = { onUrlInputChange("") }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = CockpitTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onNavigate() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BraveOrange,
                    unfocusedBorderColor = CockpitBorder,
                    focusedContainerColor = CockpitSurfaceVariant,
                    unfocusedContainerColor = CockpitSurfaceVariant,
                    cursorColor = BraveOrange
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Tabs button [N]
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenTabs() },
                color = CockpitSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder)
            ) {
                Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "$tabCount",
                        color = CockpitTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // More Menu
            Box {
                IconButton(onClick = { showMoreMenu = !showMoreMenu }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = CockpitTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    modifier = Modifier
                        .background(CockpitSurface)
                        .border(1.dp, CockpitBorder, RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("New Tab", color = CockpitTextPrimary, fontSize = 13.sp) },
                        onClick = {
                            showMoreMenu = false
                            onGoHome()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Desktop Site", color = CockpitTextPrimary, fontSize = 13.sp)
                                if (isDesktopMode) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = BraveOrange, modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        onClick = {
                            showMoreMenu = false
                            onToggleDesktop()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Bookmarks", color = CockpitTextPrimary, fontSize = 13.sp) },
                        onClick = {
                            showMoreMenu = false
                            onOpenBookmarks()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Android Auto Setup", color = CockpitTextPrimary, fontSize = 13.sp) },
                        onClick = {
                            showMoreMenu = false
                            onOpenAndroidAutoSetup()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Privacy & Security Audit", color = CockpitTextPrimary, fontSize = 13.sp) },
                        onClick = {
                            showMoreMenu = false
                            onOpenAudit()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BraveBottomNavigationBar(
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onNewTab: () -> Unit,
    tabCount: Int,
    onOpenTabs: () -> Unit,
    onOpenShields: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CockpitSurface,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CockpitTextPrimary, modifier = Modifier.size(20.dp))
            }

            IconButton(onClick = onForward) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = CockpitTextPrimary, modifier = Modifier.size(20.dp))
            }

            IconButton(onClick = onHome) {
                Image(
                    painter = painterResource(id = R.drawable.ic_brave_lion),
                    contentDescription = "Brave Home",
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = onNewTab) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Tab", tint = CockpitTextPrimary, modifier = Modifier.size(20.dp))
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenTabs() },
                color = CockpitSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder)
            ) {
                Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                    Text(text = "$tabCount", color = CockpitTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            IconButton(onClick = onOpenShields) {
                Icon(imageVector = Icons.Default.Shield, contentDescription = "Shields", tint = BraveOrange, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun CockpitMediaBar(
    videoTitle: String,
    isPlaying: Boolean,
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onEnterPip: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CockpitSurfaceVariant,
        tonalElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Track title info
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = videoTitle,
                    color = CockpitTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tata Tiago Harman Audio Synced",
                    color = ShieldGreen,
                    fontSize = 9.sp
                )
            }

            // Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onSkipBack, modifier = Modifier.size(34.dp)) {
                    Icon(imageVector = Icons.Default.Replay10, contentDescription = "Rewind", tint = CockpitTextPrimary, modifier = Modifier.size(18.dp))
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(BraveOrange)
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onSkipForward, modifier = Modifier.size(34.dp)) {
                    Icon(imageVector = Icons.Default.Forward10, contentDescription = "Forward", tint = CockpitTextPrimary, modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onEnterPip, modifier = Modifier.size(34.dp)) {
                    Icon(imageVector = Icons.Default.PictureInPicture, contentDescription = "PiP", tint = ShieldCyan, modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onToggleLock, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock",
                        tint = if (isLocked) BraveOrange else CockpitTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
