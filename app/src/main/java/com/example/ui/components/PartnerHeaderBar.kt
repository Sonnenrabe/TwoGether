package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CoupleProfile
import com.example.data.model.SyncState

@Composable
fun PartnerHeaderBar(
    profile: CoupleProfile,
    syncState: SyncState,
    onSyncClick: () -> Unit,
    onPairingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = profile.appLanguage
    fun t(key: String, vararg args: Any): String = com.example.ui.util.AppStrings.get(lang, key, *args)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("partner_header_bar"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Couple Avatars & Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPairingClick() }
                    .padding(4.dp)
            ) {
                // Intersecting Couple Avatars
                Box(
                    modifier = Modifier.size(width = 54.dp, height = 36.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Partner Avatar (Behind/Right)
                    val partnerColor = try {
                        Color(android.graphics.Color.parseColor(profile.partnerColorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.secondary
                    }
                    val myColor = try {
                        Color(android.graphics.Color.parseColor(profile.myColorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(partnerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.partnerName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // My Avatar (Front/Left)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(myColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.myName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Heart badge between them
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${profile.myName} & ${profile.partnerName}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = if (profile.isPaired) "${t("pairing_code_label")}: ${profile.coupleCode}" else t("tap_to_pair"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (profile.isPaired) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = if (profile.isPaired) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }

            // Right: Sync Pill & Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Live Sync Status Chip
                val transition = rememberInfiniteTransition(label = "sync_spin")
                val rotation by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1000, easing = LinearEasing)
                    ),
                    label = "sync_spin_angle"
                )

                Surface(
                    onClick = onSyncClick,
                    shape = RoundedCornerShape(14.dp),
                    color = when (syncState) {
                        SyncState.SYNCING -> MaterialTheme.colorScheme.primaryContainer
                        SyncState.OFFLINE -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier.testTag("btn_sync_now")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (syncState) {
                                SyncState.SYNCING -> Icons.Filled.Sync
                                SyncState.OFFLINE -> Icons.Outlined.CloudOff
                                else -> Icons.Outlined.CloudDone
                            },
                            contentDescription = "Sync with partner",
                            tint = when (syncState) {
                                SyncState.SYNCING -> MaterialTheme.colorScheme.primary
                                SyncState.OFFLINE -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier
                                .size(16.dp)
                                .then(if (syncState == SyncState.SYNCING) Modifier.rotate(rotation) else Modifier)
                        )
                        Text(
                            text = when (syncState) {
                                SyncState.SYNCING -> t("syncing")
                                SyncState.OFFLINE -> t("offline")
                                else -> t("synced")
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                // Pairing & Profile Settings Button
                IconButton(
                    onClick = onPairingClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_couple_settings")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Couple Pairing Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
