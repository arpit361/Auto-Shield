package com.example.autoshield.ui

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Https
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.autoshield.shield.BraveShieldEngine
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
fun BraveShieldsDialog(
    stats: ShieldStats,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            shape = RoundedCornerShape(24.dp),
            color = CockpitSurface,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Shield Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (stats.shieldsActive) BraveOrange.copy(alpha = 0.2f) else Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield",
                                tint = if (stats.shieldsActive) BraveOrange else Color.Gray,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Brave Shields",
                                color = CockpitTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                            Text(
                                text = if (stats.shieldsActive) "SHIELDS UP • MAXIMUM PRIVACY" else "SHIELDS DOWN",
                                color = if (stats.shieldsActive) ShieldGreen else BraveAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Switch(
                        checked = stats.shieldsActive,
                        onCheckedChange = { BraveShieldEngine.toggleShields(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BraveOrange,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = CockpitSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // OkHttp Content Blocker Interceptor Status Pill
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = CockpitSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ShieldGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "OkHttp Content Blocker Active",
                                color = CockpitTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Brave Interceptor • Ad sinkhole • URL tracking parameter cleaner",
                                color = CockpitTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Realtime Protection Metrics (3 Rows of Stat Cards)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Ads Blocked",
                        value = "${stats.adsBlocked}",
                        icon = Icons.Default.Block,
                        color = BraveOrange,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Trackers Stopped",
                        value = "${stats.trackersBlocked}",
                        icon = Icons.Default.Security,
                        color = ShieldCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Fingerprints Deflected",
                        value = "${stats.fingerprintProbesBlocked}",
                        icon = Icons.Default.Fingerprint,
                        color = ShieldGreen,
                        modifier = Modifier.weight(1f)
                    )
                    val savedMb = String.format("%.1f MB", stats.dataSavedBytes / (1024.0 * 1024.0))
                    StatCard(
                        title = "Bandwidth Saved",
                        value = savedMb,
                        icon = Icons.Default.FlashOn,
                        color = BraveAmber,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "URLs Cleaned",
                        value = "${stats.queryParamsStripped}",
                        icon = Icons.Default.Refresh,
                        color = BraveOrange,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "OkHttp Intercepted",
                        value = "${stats.okHttpInterceptions}",
                        icon = Icons.Default.Shield,
                        color = ShieldCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = CockpitBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Granular Protection Toggles
                Text(
                    text = "Brave-Core Privacy & Farbling Shields",
                    color = CockpitTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                FeatureToggleRow(
                    title = "Fingerprinting Protection (Farbling)",
                    subtitle = "Spoofs Canvas, WebGL GPU, Audio & Battery API",
                    checked = stats.fingerprintingProtectionActive,
                    onCheckedChange = { BraveShieldEngine.toggleFingerprintingProtection(it) }
                )

                FeatureToggleRow(
                    title = "Block 3rd-Party Tracking Cookies",
                    subtitle = "Stops cross-site ad networks tracking your sessions",
                    checked = stats.blockThirdPartyCookies,
                    onCheckedChange = { BraveShieldEngine.toggleBlockThirdPartyCookies(it) }
                )

                FeatureToggleRow(
                    title = "Strict HTTPS Everywhere",
                    subtitle = "Forces secure encrypted connections for all websites",
                    checked = stats.httpsEverywhereActive,
                    onCheckedChange = { BraveShieldEngine.toggleHttpsEverywhere(it) }
                )

                FeatureToggleRow(
                    title = "Clean URLs (Strip Tracking Parameters)",
                    subtitle = "Removes utm_*, fbclid, gclid from links with OkHttp Interceptor",
                    checked = stats.stripTrackingParamsActive,
                    onCheckedChange = { BraveShieldEngine.toggleStripTrackingParams(it) }
                )

                FeatureToggleRow(
                    title = "YouTube Video Ad Fast-Skip",
                    subtitle = "16x playback speedup & auto-click skip buttons",
                    checked = stats.videoAdSpeedup,
                    onCheckedChange = { BraveShieldEngine.toggleVideoAdSpeedup(it) }
                )

                FeatureToggleRow(
                    title = "Cosmetic Element Hider",
                    subtitle = "Collapses blank ad spaces & promo banners",
                    checked = stats.cosmeticFilterActive,
                    onCheckedChange = { BraveShieldEngine.toggleCosmeticFilter(it) }
                )

                FeatureToggleRow(
                    title = "Moving Car Passenger Mode",
                    subtitle = "Bypasses car movement freeze for passengers",
                    checked = stats.passengerMotionMode,
                    onCheckedChange = { BraveShieldEngine.togglePassengerMotionMode(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Instant Data Wipe / Nuke Cache
                OutlinedButton(
                    onClick = {
                        try {
                            CookieManager.getInstance().removeAllCookies(null)
                            WebStorage.getInstance().deleteAllData()
                            BraveShieldEngine.resetStats()
                            Toast.makeText(context, "All cookies, cache & local sessions wiped clean!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            // ignore safe errors
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nuke All Cookies & Cache Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { BraveShieldEngine.resetStats() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CockpitTextSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Stats", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = BraveOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save & Close", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CockpitSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = CockpitTextSecondary, fontSize = 10.sp, maxLines = 1)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = CockpitTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun FeatureToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = title, color = CockpitTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = CockpitTextMuted, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BraveOrange
            )
        )
    }
}
