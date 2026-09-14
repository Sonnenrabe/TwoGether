package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CoupleProfile
import com.example.ui.util.AppStrings
import com.example.ui.viewmodel.OwnerFilter

@Composable
fun FilterChipRow(
    currentFilter: OwnerFilter,
    profile: CoupleProfile,
    onFilterSelected: (OwnerFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = profile.appLanguage

    val myColor = try {
        Color(android.graphics.Color.parseColor(profile.myColorHex))
    } catch (e: Exception) {
        Color(0xFF3B82F6)
    }

    val partnerColor = try {
        Color(android.graphics.Color.parseColor(profile.partnerColorHex))
    } catch (e: Exception) {
        Color(0xFFEC4899)
    }

    val togetherColor = try {
        Color(android.graphics.Color.parseColor(profile.togetherColorHex))
    } catch (e: Exception) {
        Color(0xFF8B5CF6)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OwnerFilter.entries.forEach { filter ->
            val (label, chipColor, isPerson) = when (filter) {
                OwnerFilter.ALL -> Triple("${AppStrings.get(lang, "filter_all")} ✨", MaterialTheme.colorScheme.primary, false)
                OwnerFilter.TOGETHER -> Triple(AppStrings.get(lang, "filter_together"), togetherColor, true)
                OwnerFilter.ME -> Triple(profile.myName, myColor, true)
                OwnerFilter.PARTNER -> Triple(profile.partnerName, partnerColor, true)
            }
            val isSelected = currentFilter == filter

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                leadingIcon = if (isPerson) {
                    {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(chipColor)
                        )
                    }
                } else null,
                label = {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = chipColor.copy(alpha = 0.22f),
                    selectedLabelColor = chipColor,
                    selectedLeadingIconColor = chipColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = chipColor.copy(alpha = 0.35f),
                    selectedBorderColor = chipColor,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 2.dp
                ),
                modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
            )
        }
    }
}

