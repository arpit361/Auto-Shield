package com.example.autoshield.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.autoshield.browser.BrowserViewModel
import com.example.autoshield.browser.SpeedDialItem
import com.example.autoshield.shield.ShieldStats
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
fun BraveHomeScreen(
    viewModel: BrowserViewModel,
    shieldStats: ShieldStats,
    speedDials: List<SpeedDialItem>,
    activeTabCount: Int,
    onNavigate: (String) -> Unit
) {
    var searchInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Brave Navigation & Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lion Logo + Brand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { viewModel.showPassengerNoticeDialog(true) }
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CockpitSurfaceVariant)
                        .border(1.dp, CockpitBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_brave_lion),
                        contentDescription = "Brave Lion",
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Brave",
                            color = CockpitTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(BraveOrange.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SHIELDS UP",
                                color = BraveOrange,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Top Action Icons: Shields, Tabs, Menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Brave Shields Button
                IconButton(onClick = { viewModel.showShieldsDialog(true) }) {
                    BadgedBox(
                        badge = {
                            if (shieldStats.adsBlocked > 0) {
                                Badge(
                                    containerColor = if (shieldStats.shieldsActive) BraveOrange else Color.Gray,
                                    contentColor = Color.White
                                ) {
                                    Text(text = "${shieldStats.adsBlocked}", fontSize = 9.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shields",
                            tint = if (shieldStats.shieldsActive) BraveOrange else Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Tab Switcher Button [N]
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.showTabsDialog(true) },
                    color = CockpitSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$activeTabCount",
                            color = CockpitTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // More Menu Button
                Box {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = CockpitTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(CockpitSurface)
                            .border(1.dp, CockpitBorder, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Tab", color = CockpitTextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                viewModel.openNewTab()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bookmarks", color = CockpitTextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                viewModel.showBookmarksDialog(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Android Auto Setup", color = CockpitTextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                viewModel.showAndroidAutoSetupDialog(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Security & Privacy Audit", color = CockpitTextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                viewModel.showAuditDialog(true)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Iconic Brave Shield Stats Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = BraveOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Brave Shields • OkHttp Active",
                            color = CockpitTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = if (shieldStats.shieldsActive) "SHIELDS UP" else "OFF",
                        color = if (shieldStats.shieldsActive) ShieldGreen else BraveAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    BraveMetricColumn(
                        value = "${shieldStats.adsBlocked + shieldStats.trackersBlocked}",
                        label = "Trackers & Ads Blocked",
                        color = BraveOrange
                    )

                    val savedMb = String.format("%.1f MB", shieldStats.dataSavedBytes / (1024.0 * 1024.0))
                    BraveMetricColumn(
                        value = savedMb,
                        label = "Bandwidth Saved",
                        color = ShieldCyan
                    )

                    val timeSaved = if (shieldStats.timeSavedSeconds < 60) "${shieldStats.timeSavedSeconds}s" else "${shieldStats.timeSavedSeconds / 60}m"
                    BraveMetricColumn(
                        value = timeSaved,
                        label = "Est. Time Saved",
                        color = ShieldGreen
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = CockpitBorder.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Clean URLs: ${shieldStats.queryParamsStripped} tracking params stripped",
                        color = CockpitTextSecondary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "OkHttp Interceptor • Active",
                        color = ShieldGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Central Brave Search / URL Omnibox
        OutlinedTextField(
            value = searchInput,
            onValueChange = { searchInput = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            placeholder = { Text("Search or type URL...", color = CockpitTextMuted, fontSize = 14.sp) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = BraveOrange,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchInput.isNotBlank()) {
                    IconButton(onClick = { searchInput = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = CockpitTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                if (searchInput.isNotBlank()) {
                    focusManager.clearFocus()
                    onNavigate(searchInput)
                }
            }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BraveOrange,
                unfocusedBorderColor = CockpitBorder,
                focusedContainerColor = CockpitSurface,
                unfocusedContainerColor = CockpitSurface,
                cursorColor = BraveOrange,
                focusedTextColor = CockpitTextPrimary,
                unfocusedTextColor = CockpitTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Speed Dial Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Speed Dial",
                color = CockpitTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.showAddSpeedDialDialog(true) }
                    .background(CockpitSurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = BraveOrange,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add Site",
                    color = BraveOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Speed Dial Grid (4 columns, just like Brave!)
        Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(speedDials) { item ->
                    SpeedDialTile(
                        item = item,
                        onClick = { onNavigate(item.url) },
                        onDelete = { viewModel.removeSpeedDial(item.id) }
                    )
                }

                // Add Site Tile Button
                item {
                    AddSpeedDialTile(
                        onClick = { viewModel.showAddSpeedDialDialog(true) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // In-Cabin Car / Tata Tiago Quick Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.showPassengerNoticeDialog(true) },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ShieldGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = ShieldGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Tata Tiago Harman Audio Synced",
                            color = CockpitTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Steering wheel buttons & background audio active",
                            color = CockpitTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    text = "READY",
                    color = ShieldGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BraveMetricColumn(
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = CockpitTextSecondary,
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SpeedDialTile(
    item: SpeedDialItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val brandColor = try {
        Color(android.graphics.Color.parseColor(item.brandColorHex))
    } catch (e: Exception) {
        BraveOrange
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CockpitSurfaceVariant)
                .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(brandColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.title.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Remove button for custom shortcuts
            if (!item.isDefault) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            color = CockpitTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AddSpeedDialTile(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CockpitSurfaceVariant.copy(alpha = 0.5f))
                .border(1.dp, BraveOrange.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Site",
                tint = BraveOrange,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Add Site",
            color = BraveOrange,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}
