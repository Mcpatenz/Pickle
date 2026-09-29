package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookingEntity
import com.example.data.local.UserProfileEntity
import com.example.ui.theme.AvailableGreen
import com.example.ui.theme.AvailableGreenBg
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GCashBlue
import com.example.ui.theme.GCashBlueBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MaintenanceRed
import com.example.ui.theme.MaintenanceRedBg
import com.example.ui.theme.OpticVolt
import com.example.ui.theme.OpticVoltDarkText
import com.example.ui.theme.PeakAmber
import java.util.Locale

/**
 * Standard pickleball DUPR skill level options (e.g., 2.5, 3.0, 3.5, 4.0, 4.5, 5.0).
 */
data class SkillLevelOption(
    val rating: Double,
    val ratingFormatted: String,
    val tierTitle: String,
    val description: String
)

val DefaultSkillLevelOptions: List<SkillLevelOption> = listOf(
    SkillLevelOption(2.5, "2.5", "Beginner", "Learning court positioning & basic rally rules"),
    SkillLevelOption(3.0, "3.0", "Intermediate", "Consistent serves, returns, and dink rallies"),
    SkillLevelOption(3.5, "3.5", "Intermediate+", "Third-shot drops, kitchen control & directional play"),
    SkillLevelOption(4.0, "4.0", "Advanced", "Fast hands at NVZ, resets, spin & strategic placement"),
    SkillLevelOption(4.5, "4.5", "Pro / Open", "High-percentage unattackable shots & tournament pace"),
    SkillLevelOption(5.0, "5.0", "Elite Pro", "Mastery of all shot types & championship competition")
)

/**
 * Default seeded history of past court reservations for the user profile screen.
 */
val DefaultUserPastReservations: List<BookingEntity> = listOf(
    BookingEntity(
        id = 8,
        bookingCode = "PKL-20260927-00104",
        facilityId = 1,
        facilityName = "Smash Pickle Club",
        facilityLocation = "Quezon City",
        courtId = 4,
        courtName = "Court 4",
        dateIso = "2026-09-27",
        dateLabel = "September 27, 2026",
        timeSlot = "09:00 AM",
        timeRangeLabel = "9:00 AM - 10:00 AM",
        playerName = "Jonel P.",
        courtFee = 350,
        discountAmount = 35,
        serviceFee = 20,
        totalAmount = 335,
        paymentMethod = "PayMaya",
        paymentStatus = "PAID",
        status = "CHECKED_IN",
        checkInTime = "8:50 AM",
        userRating = 5,
        userReview = "Smooth morning session on Court 4!",
        createdAt = 1758963600000L,
        customerEmail = "customer@pickleplay.ph"
    ),
    BookingEntity(
        id = 9,
        bookingCode = "PKL-20260920-00089",
        facilityId = 2,
        facilityName = "BGC Dink & Rally Club",
        facilityLocation = "Taguig City",
        courtId = 2,
        courtName = "Court 2",
        dateIso = "2026-09-20",
        dateLabel = "September 20, 2026",
        timeSlot = "04:00 PM",
        timeRangeLabel = "4:00 PM - 5:00 PM",
        playerName = "Jonel P.",
        courtFee = 450,
        discountAmount = 45,
        serviceFee = 20,
        totalAmount = 425,
        paymentMethod = "GCash",
        paymentStatus = "PAID",
        status = "COMPLETED",
        checkInTime = "3:52 PM",
        userRating = 5,
        userReview = "Awesome cushion surface and LED lighting!",
        createdAt = 1758358800000L,
        customerEmail = "customer@pickleplay.ph"
    ),
    BookingEntity(
        id = 11,
        bookingCode = "PKL-20260914-00063",
        facilityId = 1,
        facilityName = "Smash Pickle Club",
        facilityLocation = "Quezon City",
        courtId = 1,
        courtName = "Court 1",
        dateIso = "2026-09-14",
        dateLabel = "September 14, 2026",
        timeSlot = "06:00 PM",
        timeRangeLabel = "6:00 PM - 7:00 PM",
        playerName = "Jonel P.",
        courtFee = 350,
        discountAmount = 0,
        serviceFee = 20,
        totalAmount = 370,
        paymentMethod = "GCash",
        paymentStatus = "PAID",
        status = "COMPLETED",
        checkInTime = "5:50 PM",
        userRating = 4,
        userReview = "Great evening doubles match!",
        createdAt = 1757840400000L,
        customerEmail = "customer@pickleplay.ph"
    )
)

enum class PastReservationHistoryFilter(val label: String) {
    ALL("All Past"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

/**
 * UserProfile screen that displays the user's name, skill level (e.g., 3.0, 3.5, 4.0),
 * and a comprehensive history of their past court reservations.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileScreen(
    modifier: Modifier = Modifier,
    userProfile: UserProfileEntity? = null,
    userNameOverride: String? = null,
    skillLevelOverride: String? = null,
    pastReservations: List<BookingEntity>? = null,
    allBookings: List<BookingEntity> = emptyList(),
    onUpdateSkillLevel: (skillLabel: String, duprRating: Double) -> Unit = { _, _ -> },
    onUpdateUserName: (String) -> Unit = {},
    onUpdatePreferences: (position: String, skillLevel: String) -> Unit = { _, _ -> },
    onSelectMembership: (tier: String, price: Int, discountPct: Int) -> Unit = { _, _, _ -> },
    onOpenBookingPass: (BookingEntity) -> Unit = {},
    onRebookCourt: (BookingEntity) -> Unit = {}
) {
    val defaultProfile = remember(userProfile) {
        userProfile ?: UserProfileEntity(
            id = 1,
            name = "Jonel",
            fullName = "Jonel P.",
            city = "Quezon City",
            skillLevel = "Intermediate",
            duprRating = 3.5,
            gamesPlayed = 42,
            wins = 27,
            winStreak = 5,
            preferredPosition = "Right Side",
            membershipTier = "Player",
            membershipPrice = 499,
            discountPercent = 10
        )
    }

    val initialDisplayName = remember(defaultProfile, userNameOverride) {
        userNameOverride?.takeIf { it.isNotBlank() }
            ?: defaultProfile.fullName.ifBlank { defaultProfile.name }
    }

    val initialNumericRating = remember(defaultProfile, skillLevelOverride) {
        val parsedOverride = skillLevelOverride
            ?.let { Regex("(\\d+\\.\\d+)").find(it)?.groupValues?.getOrNull(1)?.toDoubleOrNull() }
        parsedOverride ?: defaultProfile.duprRating
    }

    val initialSkillLabel = remember(defaultProfile, skillLevelOverride, initialNumericRating) {
        skillLevelOverride?.takeIf { it.isNotBlank() }
            ?: DefaultSkillLevelOptions.find { it.rating == initialNumericRating }?.tierTitle
            ?: defaultProfile.skillLevel
    }

    var currentUserName by remember(initialDisplayName) { mutableStateOf(initialDisplayName) }
    var currentDuprRating by remember(initialNumericRating) { mutableDoubleStateOf(initialNumericRating) }
    var currentSkillTier by remember(initialSkillLabel) { mutableStateOf(initialSkillLabel) }
    var currentPosition by remember(defaultProfile.preferredPosition) {
        mutableStateOf(defaultProfile.preferredPosition)
    }
    var isEditingName by remember { mutableStateOf(false) }
    var nameDraftInput by remember(currentUserName) { mutableStateOf(currentUserName) }
    var historyFilter by remember { mutableStateOf(PastReservationHistoryFilter.ALL) }

    val formattedRating = remember(currentDuprRating) {
        String.format(Locale.US, "%.1f", currentDuprRating)
    }

    // Resolve history of past court reservations
    val resolvedPastReservations = remember(pastReservations, allBookings) {
        when {
            pastReservations != null -> pastReservations
            allBookings.isNotEmpty() -> {
                val filteredPast = allBookings.filter { booking ->
                    booking.isPastBooking ||
                        booking.status.equals("COMPLETED", ignoreCase = true) ||
                        booking.status.equals("CHECKED_IN", ignoreCase = true) ||
                        booking.status.equals("CANCELLED", ignoreCase = true) ||
                        booking.dateIso < "2026-09-28"
                }
                if (filteredPast.isNotEmpty()) {
                    filteredPast.sortedByDescending { it.dateIso }
                } else {
                    DefaultUserPastReservations
                }
            }
            else -> DefaultUserPastReservations
        }
    }

    val filteredHistoryList = remember(resolvedPastReservations, historyFilter) {
        when (historyFilter) {
            PastReservationHistoryFilter.ALL -> resolvedPastReservations
            PastReservationHistoryFilter.COMPLETED -> resolvedPastReservations.filter {
                it.status.equals("COMPLETED", ignoreCase = true) ||
                    it.status.equals("CHECKED_IN", ignoreCase = true)
            }
            PastReservationHistoryFilter.CANCELLED -> resolvedPastReservations.filter {
                it.status.equals("CANCELLED", ignoreCase = true)
            }
        }
    }

    val totalHoursPlayed = remember(resolvedPastReservations) {
        resolvedPastReservations.count { !it.status.equals("CANCELLED", ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("user_profile_screen"),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. User Profile Identity & Skill Level Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("user_profile_header_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF064E3B), Color(0xFF03281E))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Top Row: Avatar + Name + Verified Skill Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = OpticVolt,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(62.dp)
                                        .testTag("user_profile_avatar")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = currentUserName.trim().take(1).uppercase(),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = OpticVoltDarkText
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = currentUserName,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.testTag("user_profile_name")
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified Player",
                                            tint = OpticVolt,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFFA7F3D0),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "${defaultProfile.city} • ${defaultProfile.membershipTier} Member",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFD6F5E6),
                                            modifier = Modifier.testTag("user_profile_city")
                                        )
                                    }
                                }
                            }

                            // Prominent Skill Level Badge (e.g., 3.0, 3.5, 4.0)
                            Surface(
                                color = OpticVolt,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("user_profile_skill_level_badge")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "SKILL LEVEL",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = OpticVoltDarkText.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = formattedRating,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = OpticVoltDarkText,
                                        modifier = Modifier.testTag("user_profile_skill_level_value")
                                    )
                                    Text(
                                        text = currentSkillTier.uppercase(),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OpticVoltDarkText,
                                        modifier = Modifier.testTag("user_profile_skill_tier_label")
                                    )
                                }
                            }
                        }

                        // Detailed Skill Summary Pill
                        Surface(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, OpticVolt.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SportsTennis,
                                        contentDescription = null,
                                        tint = OpticVolt,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "DUPR $formattedRating ($currentSkillTier) • $currentPosition",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.testTag("user_profile_skill_summary_text")
                                    )
                                }
                                Text(
                                    text = "Edit Name",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OpticVolt,
                                    modifier = Modifier
                                        .clickable {
                                            nameDraftInput = currentUserName
                                            isEditingName = !isEditingName
                                        }
                                        .testTag("user_profile_edit_name_button")
                                )
                            }
                        }

                        if (isEditingName) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = nameDraftInput,
                                        onValueChange = { nameDraftInput = it },
                                        label = { Text("Player Name") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("user_profile_name_input")
                                    )
                                    Button(
                                        onClick = {
                                            val clean = nameDraftInput.trim()
                                            if (clean.isNotEmpty()) {
                                                currentUserName = clean
                                                onUpdateUserName(clean)
                                                isEditingName = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("user_profile_save_name_button")
                                    ) {
                                        Text("Save", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Key Player Stats Bento Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProfileStatBentoBox(
                                label = "Skill Rating",
                                value = formattedRating,
                                highlight = true,
                                testTag = "user_profile_stat_skill_rating",
                                modifier = Modifier.weight(1f)
                            )
                            ProfileStatBentoBox(
                                label = "Past Bookings",
                                value = "${resolvedPastReservations.size}",
                                testTag = "user_profile_past_reservations_count_stat",
                                modifier = Modifier.weight(1f)
                            )
                            ProfileStatBentoBox(
                                label = "Court Hours",
                                value = "${totalHoursPlayed}h",
                                testTag = "user_profile_stat_hours_played",
                                modifier = Modifier.weight(1f)
                            )
                            ProfileStatBentoBox(
                                label = "Win Record",
                                value = "${defaultProfile.wins}/${defaultProfile.gamesPlayed}",
                                testTag = "user_profile_stat_win_record",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Interactive Skill Level (e.g., 3.0, 3.5, 4.0) & Preferred Court Side Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("user_profile_skill_selector_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Player Skill Level (DUPR)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Select your pickleball skill rating (e.g., 3.0, 3.5, 4.0) for matchmaking & court play",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DefaultSkillLevelOptions.forEach { option ->
                            val isSelected = formattedRating == option.ratingFormatted
                            val tagSuffix = option.ratingFormatted.replace(".", "_")
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        currentDuprRating = option.rating
                                        currentSkillTier = option.tierTitle
                                        onUpdateSkillLevel(option.tierTitle, option.rating)
                                        onUpdatePreferences(
                                            currentPosition,
                                            "${option.ratingFormatted} (${option.tierTitle})"
                                        )
                                    }
                                    .testTag("user_profile_skill_chip_$tagSuffix"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = option.ratingFormatted,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) OpticVolt else EmeraldPrimary
                                    )
                                    Text(
                                        text = option.tierTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Active skill description banner
                    val activeOption = DefaultSkillLevelOptions.find { it.ratingFormatted == formattedRating }
                    if (activeOption != null) {
                        Surface(
                            color = AvailableGreenBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AvailableGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Skill ${activeOption.ratingFormatted} (${activeOption.tierTitle}): ${activeOption.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldDark,
                                    modifier = Modifier.testTag("user_profile_active_skill_description")
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    // Preferred Position chips
                    Text(
                        text = "Preferred Court Position",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Right Side", "Left Side", "Both Sides").forEach { pos ->
                            FilterChip(
                                selected = currentPosition == pos,
                                onClick = {
                                    currentPosition = pos
                                    onUpdatePreferences(pos, currentSkillTier)
                                },
                                label = { Text(pos) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("user_profile_position_chip_${pos.replace(" ", "_")}")
                            )
                        }
                    }
                }
            }
        }

        // 3. History of Past Court Reservations Header & Filter Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("user_profile_past_reservations_section"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.14f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Past Reservations History",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Past Court Reservations History",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("user_profile_past_reservations_header")
                            )
                            Text(
                                text = "${filteredHistoryList.size} of ${resolvedPastReservations.size} past court sessions recorded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("user_profile_past_reservations_subtitle")
                            )
                        }
                    }

                    Surface(
                        color = EmeraldDark,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${resolvedPastReservations.size} TOTAL",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OpticVolt,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("user_profile_past_reservations_badge")
                        )
                    }
                }

                // Filter Tabs for Past Reservations History
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PastReservationHistoryFilter.entries.forEach { filterOption ->
                        val isSelected = historyFilter == filterOption
                        val tag = when (filterOption) {
                            PastReservationHistoryFilter.ALL -> "user_profile_history_filter_all"
                            PastReservationHistoryFilter.COMPLETED -> "user_profile_history_filter_completed"
                            PastReservationHistoryFilter.CANCELLED -> "user_profile_history_filter_cancelled"
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { historyFilter = filterOption }
                                .testTag(tag),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filterOption.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Past Court Reservations List Items
        if (filteredHistoryList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("user_profile_past_reservations_empty"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No past court reservations in this filter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Completed and checked-in court sessions will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredHistoryList, key = { "${it.id}_${it.bookingCode}" }) { booking ->
                PastCourtReservationHistoryCard(
                    booking = booking,
                    onOpenBookingPass = { onOpenBookingPass(booking) },
                    onRebookCourt = { onRebookCourt(booking) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun PastCourtReservationHistoryCard(
    booking: BookingEntity,
    onOpenBookingPass: () -> Unit = {},
    onRebookCourt: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isCancelled = booking.status.equals("CANCELLED", ignoreCase = true)
    val isCompletedOrCheckedIn = booking.status.equals("COMPLETED", ignoreCase = true) ||
        booking.status.equals("CHECKED_IN", ignoreCase = true)

    val statusText = when {
        booking.status.equals("CHECKED_IN", ignoreCase = true) -> {
            if (!booking.checkInTime.isNullOrBlank()) "CHECKED IN • ${booking.checkInTime}" else "CHECKED IN"
        }
        booking.status.equals("COMPLETED", ignoreCase = true) -> "COMPLETED"
        isCancelled -> "CANCELLED"
        else -> "PAST SESSION • ${booking.status}"
    }

    val statusColor = when {
        isCancelled -> MaintenanceRed
        isCompletedOrCheckedIn -> AvailableGreen
        else -> GCashBlue
    }

    val statusBgColor = when {
        isCancelled -> MaintenanceRedBg
        isCompletedOrCheckedIn -> AvailableGreenBg
        else -> GCashBlueBg
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("user_profile_past_reservation_item_${booking.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .testTag("user_profile_past_reservation_code_${booking.bookingCode}"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Court & Facility + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = EmeraldDark,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SportsTennis,
                                contentDescription = null,
                                tint = OpticVolt,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "${booking.courtName} (Court ID #${booking.courtId}) • ${booking.facilityName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("user_profile_past_reservation_court_${booking.id}")
                        )
                        Text(
                            text = "Booking #${booking.bookingCode} • ${booking.facilityLocation}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = statusBgColor,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = statusText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("user_profile_past_reservation_status_${booking.id}")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Date & Time Slot Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Reservation Date",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${booking.dateIso} (${booking.dateLabel})",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("user_profile_past_reservation_date_${booking.id}")
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Reservation Time",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${booking.timeSlot} • ${booking.timeRangeLabel}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.testTag("user_profile_past_reservation_time_${booking.id}")
                    )
                }
            }

            // Optional User Rating / Review from Past Session
            if (booking.userRating != null || !booking.userReview.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PeakAmber.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = PeakAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = buildString {
                                booking.userRating?.let { append("${it}.0 ★ ") }
                                if (!booking.userReview.isNullOrBlank()) {
                                    append("\"${booking.userReview}\"")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF92400E),
                            modifier = Modifier.testTag("user_profile_past_reservation_review_${booking.id}")
                        )
                    }
                }
            }

            // Bottom Row: Payment Summary & Rebook / Pass Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Paid ₱${booking.totalAmount} via ${booking.paymentMethod} (${booking.paymentStatus})",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onRebookCourt,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("user_profile_rebook_button_${booking.id}")
                    ) {
                        Text(
                            text = "Rebook Court",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }

                    Button(
                        onClick = onOpenBookingPass,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("user_profile_view_pass_button_${booking.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Receipt",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatBentoBox(
    label: String,
    value: String,
    highlight: Boolean = false,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag(testTag),
        color = if (highlight) OpticVolt else Color(0xFF0F3D2E),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (highlight) OpticVolt else Color.White.copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = if (highlight) OpticVoltDarkText else Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = if (highlight) OpticVoltDarkText.copy(alpha = 0.85f) else Color(0xFFA7F3D0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
