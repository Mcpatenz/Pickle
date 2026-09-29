package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.data.local.BookingEntity
import com.example.data.local.UserProfileEntity
import com.example.ui.theme.AvailableGreen
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MaintenanceRed
import com.example.ui.theme.OpticVolt
import com.example.ui.theme.OpticVoltDarkText
import com.example.ui.theme.PeakAmber

@Composable
fun ProfileAndPassScreen(
    userProfile: UserProfileEntity?,
    bookings: List<BookingEntity>,
    onOpenBookingPass: (BookingEntity) -> Unit,
    onSelectMembership: (String, Int, Int) -> Unit,
    onUpdatePreferences: (String, String) -> Unit,
    onOpenAdminPortal: () -> Unit
) {
    UserProfileScreen(
        userProfile = userProfile,
        allBookings = bookings,
        onUpdatePreferences = onUpdatePreferences,
        onSelectMembership = onSelectMembership,
        onOpenBookingPass = onOpenBookingPass
    )
}

@Composable
private fun LegacyProfileAndPassContent(
    userProfile: UserProfileEntity?,
    bookings: List<BookingEntity>,
    onOpenBookingPass: (BookingEntity) -> Unit,
    onSelectMembership: (String, Int, Int) -> Unit,
    onUpdatePreferences: (String, String) -> Unit
) {
    val profile = userProfile ?: UserProfileEntity(
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen_list"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Player Identity & DUPR Rating Hero Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(22.dp),
                color = EmeraldDark
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = OpticVolt,
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = profile.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = OpticVoltDarkText
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "🏓 ${profile.skillLevel} • ${profile.city}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OpticVolt
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Stats Bento Row (Games Played 42, Wins 27, Rating 3.5)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricBox(
                            label = "Games Played",
                            value = "${profile.gamesPlayed}",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricBox(
                            label = "Wins",
                            value = "${profile.wins}",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricBox(
                            label = "DUPR Rating",
                            value = "${profile.duprRating}",
                            highlight = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Preferred Position & Skill Level Selector
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preferred Position & Skill",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val positions = listOf("Right Side", "Left Side", "Both Sides")
                        positions.forEach { pos ->
                            FilterChip(
                                selected = profile.preferredPosition == pos,
                                onClick = { onUpdatePreferences(pos, profile.skillLevel) },
                                label = { Text("☑ $pos") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Achievements Showcase (🏆 10 Games, 🔥 5 Win Streak, 🎯 25 Games)
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Achievements",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AchievementBadgeCard(
                        emoji = "🏆",
                        title = "10 Games",
                        subtitle = "Club Regular",
                        modifier = Modifier.weight(1f)
                    )
                    AchievementBadgeCard(
                        emoji = "🔥",
                        title = "${profile.winStreak} Win Streak",
                        subtitle = "On Fire",
                        modifier = Modifier.weight(1f)
                    )
                    AchievementBadgeCard(
                        emoji = "🎯",
                        title = "25 Games",
                        subtitle = "Kitchen Master",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Club Membership Plans (Basic ₱0, Player ₱499, Pro ₱999)
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Club Membership Plans",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Unlock automatic court booking discounts & free open-play sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MembershipPlanCard(
                        tier = "Basic",
                        priceText = "₱0/month",
                        perks = listOf("Standard online booking access", "Pay per court session"),
                        isCurrent = profile.membershipTier == "Basic",
                        onSelect = { onSelectMembership("Basic", 0, 0) }
                    )
                    MembershipPlanCard(
                        tier = "Player",
                        priceText = "₱499/month",
                        perks = listOf("10% court booking discount", "Priority booking window", "Member-only Open Play games"),
                        isCurrent = profile.membershipTier == "Player",
                        onSelect = { onSelectMembership("Player", 499, 10) }
                    )
                    MembershipPlanCard(
                        tier = "Pro",
                        priceText = "₱999/month",
                        perks = listOf("20% court booking discount", "Free open-play sessions", "Tournament entry discounts"),
                        isCurrent = profile.membershipTier == "Pro",
                        isProHighlight = true,
                        onSelect = { onSelectMembership("Pro", 999, 20) }
                    )
                }
            }
        }

        // 5. My Bookings & Digital QR Passes
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "My Bookings & Digital QR Passes (${bookings.size})",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Tap any booking to open your QR code pass, check in, or manage cancellation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(bookings, key = { it.id }) { booking ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenBookingPass(booking) }
                    .testTag("profile_booking_item_${booking.id}"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = when (booking.status) {
                                    "CHECKED_IN" -> AvailableGreen.copy(alpha = 0.15f)
                                    "CANCELLED" -> MaintenanceRed.copy(alpha = 0.15f)
                                    else -> EmeraldPrimary.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (booking.status == "CHECKED_IN") {
                                        "CHECKED IN (${booking.checkInTime})"
                                    } else {
                                        booking.status
                                    },
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = when (booking.status) {
                                        "CHECKED_IN" -> AvailableGreen
                                        "CANCELLED" -> MaintenanceRed
                                        else -> EmeraldPrimary
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "#${booking.bookingCode}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${booking.facilityName} • ${booking.courtName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${booking.dateLabel} • ${booking.timeRangeLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₱${booking.totalAmount} • ${booking.paymentMethod} (${booking.paymentStatus})",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "Open QR Pass",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMetricBox(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (highlight) OpticVolt else Color(0xFF123A2A),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = if (highlight) OpticVoltDarkText else Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = if (highlight) OpticVoltDarkText.copy(alpha = 0.8f) else Color(0xFFA3B8B0)
            )
        }
    }
}

@Composable
private fun AchievementBadgeCard(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MembershipPlanCard(
    tier: String,
    priceText: String,
    perks: List<String>,
    isCurrent: Boolean,
    isProHighlight: Boolean = false,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelect() }
            .testTag("membership_plan_$tier"),
        shape = RoundedCornerShape(18.dp),
        color = if (isProHighlight) EmeraldDark else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isCurrent) 2.dp else 1.dp,
            color = if (isCurrent) OpticVolt else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tier,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isProHighlight) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = OpticVolt,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ACTIVE PLAN",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpticVoltDarkText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = priceText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isProHighlight) OpticVolt else EmeraldPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            perks.forEach { perk ->
                Text(
                    text = "• $perk",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isProHighlight) Color(0xFFD6F5E6) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
