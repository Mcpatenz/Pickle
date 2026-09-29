package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookingEntity
import com.example.data.local.CourtEntity
import com.example.data.local.FacilityEntity
import com.example.data.local.OpenPlayGameEntity
import com.example.data.local.UserProfileEntity
import com.example.notifications.CustomerLocalNotificationCenterCard
import com.example.notifications.LocalReservationNotificationService
import com.example.notifications.LocalReservationStatusHeadsUpBanner
import com.example.ui.components.CustomerCourtQrScannerCard
import com.example.ui.components.CustomerCourtQrScannerDialog
import com.example.ui.components.QrCodeMatrixCanvas
import com.example.ui.components.QrPassScanOutcome
import com.example.ui.components.ZxingQrScannerEngine
import com.example.ui.components.resolveCustomerCourtQrCheckIn
import com.example.ui.theme.AvailableGreen
import com.example.ui.theme.AvailableGreenBg
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MaintenanceRed
import com.example.ui.theme.MaintenanceRedBg
import com.example.ui.theme.OpticVolt
import com.example.ui.theme.OpticVoltDarkText
import com.example.ui.theme.PeakAmber
import com.example.viewmodel.AuthUserSession

private val DefaultCustomerProfile = UserProfileEntity(
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

private val DefaultCustomerBookings = listOf(
    BookingEntity(
        id = 1,
        bookingCode = "PKL-20260928-00125",
        facilityId = 1,
        facilityName = "Smash Pickle Club",
        facilityLocation = "Quezon City",
        courtId = 3,
        courtName = "Court 3",
        dateIso = "2026-09-28",
        dateLabel = "September 28, 2026",
        timeSlot = "02:00 PM",
        timeRangeLabel = "2:00 PM - 3:00 PM",
        playerName = "Jonel P.",
        courtFee = 300,
        discountAmount = 0,
        serviceFee = 20,
        totalAmount = 320,
        paymentMethod = "GCash",
        paymentStatus = "PAID",
        status = "CONFIRMED",
        checkInTime = null
    ),
    BookingEntity(
        id = 6,
        bookingCode = "PKL-20260929-00142",
        facilityId = 1,
        facilityName = "Smash Pickle Club",
        facilityLocation = "Quezon City",
        courtId = 5,
        courtName = "Court 5",
        dateIso = "2026-09-29",
        dateLabel = "September 29, 2026",
        timeSlot = "05:00 PM",
        timeRangeLabel = "5:00 PM - 6:00 PM",
        playerName = "Jonel P.",
        courtFee = 350,
        discountAmount = 35,
        serviceFee = 20,
        totalAmount = 335,
        paymentMethod = "GCash",
        paymentStatus = "AWAITING_APPROVAL",
        status = "PENDING_ADMIN_APPROVAL",
        checkInTime = null
    ),
    BookingEntity(
        id = 7,
        bookingCode = "PKL-20260930-00158",
        facilityId = 1,
        facilityName = "Smash Pickle Club",
        facilityLocation = "Quezon City",
        courtId = 6,
        courtName = "Court 6",
        dateIso = "2026-09-30",
        dateLabel = "September 30, 2026",
        timeSlot = "06:00 PM",
        timeRangeLabel = "6:00 PM - 7:00 PM",
        playerName = "Jonel P.",
        courtFee = 350,
        discountAmount = 35,
        serviceFee = 20,
        totalAmount = 335,
        paymentMethod = "GCash",
        paymentStatus = "AWAITING_PAYMENT",
        status = "APPROVED_AWAITING_PAYMENT",
        checkInTime = null
    ),
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
        checkInTime = "8:50 AM"
    )
)

private val DefaultCustomerOpenPlay = listOf(
    OpenPlayGameEntity(
        id = 1,
        title = "Saturday Open Play",
        facilityId = 1,
        facilityName = "Smash Pickle Club",
        courtName = "Court 1 & Court 2",
        dayLabel = "Saturday",
        dateLabel = "Oct 3, 2026",
        timeRange = "5:00 PM - 7:00 PM",
        skillLevel = "Intermediate",
        pricePerPlayer = 150,
        maxPlayers = 6,
        playersCsv = "Jon,Maria,Carlo",
        isJoinedByUser = false,
        hostName = "Coach Anton"
    )
)

/**
 * Customer Dashboard Screen displaying:
 * - Player profile summary, DUPR rating, Membership tier discount, and Quick Book CTA
 * - Upcoming court reservations & Digital QR Booking Pass access
 * - Live court availability & hourly rates across all club courts
 * - Open Play sessions and club promo codes
 */
@Composable
fun CustomerDashboardScreen(
    modifier: Modifier = Modifier,
    facility: FacilityEntity? = null,
    courts: List<CourtEntity> = DefaultDashboardCourts,
    bookings: List<BookingEntity> = DefaultCustomerBookings,
    openPlayGames: List<OpenPlayGameEntity> = DefaultCustomerOpenPlay,
    userProfile: UserProfileEntity? = DefaultCustomerProfile,
    authSession: AuthUserSession? = null,
    lastReservedPlayerName: String? = null,
    onSelectCourtToBook: (Int) -> Unit = {},
    onOpenBookingPass: (BookingEntity) -> Unit = {},
    onOpenMyReservations: () -> Unit = {},
    onOpenPaymentDialog: (BookingEntity) -> Unit = {},
    onCancelPendingReservation: (BookingEntity) -> Unit = {},
    onToggleJoinOpenPlay: (OpenPlayGameEntity) -> Unit = {},
    onApplyPromoCode: (String) -> Unit = {},
    onSimulateStatusUpdate: ((BookingEntity?) -> Unit)? = null,
    onCheckInBooking: ((BookingEntity) -> Unit)? = null,
    onOpenAdminDashboard: (() -> Unit)? = null,
    onOpenCashierDashboard: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val latestLocalAlert by LocalReservationNotificationService.latestAlert.collectAsState()
    var localApprovedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var localCheckedInIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showCustomerQrScannerModal by remember { mutableStateOf(false) }
    var lastCustomerQrScanOutcome by remember { mutableStateOf<QrPassScanOutcome?>(null) }

    val profile = userProfile ?: DefaultCustomerProfile
    val activePlayerName = lastReservedPlayerName?.takeIf { it.isNotBlank() }
        ?: authSession?.fullName?.takeIf { it.isNotBlank() }
        ?: profile.fullName
    val displayCourts = if (courts.isNotEmpty()) courts else DefaultDashboardCourts
    val activeCourtsCount = displayCourts.count { it.status.equals("ACTIVE", ignoreCase = true) }
    val allActiveBookings = (if (bookings.isNotEmpty()) bookings else DefaultCustomerBookings)
        .filter { it.status != "CANCELLED" }
        .map { b ->
            when {
                localCheckedInIds.contains(b.id) -> {
                    b.copy(
                        status = "CHECKED_IN",
                        paymentStatus = "PAID",
                        checkInTime = b.checkInTime ?: "1:47 PM"
                    )
                }
                localApprovedIds.contains(b.id) && b.status == "PENDING_ADMIN_APPROVAL" -> {
                    b.copy(
                        status = "APPROVED_AWAITING_PAYMENT",
                        paymentStatus = "AWAITING_PAYMENT",
                        adminApprovedAt = "Approved Just Now"
                    )
                }
                else -> b
            }
        }
    val customerBookings = allActiveBookings.filter {
        it.playerName.equals(activePlayerName, ignoreCase = true) ||
            it.playerName.equals(profile.fullName, ignoreCase = true) ||
            it.playerName.equals("Jonel P.", ignoreCase = true)
    }.ifEmpty { allActiveBookings }
    val displayGames = if (openPlayGames.isNotEmpty()) openPlayGames else DefaultCustomerOpenPlay

    var promoAppliedBanner by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("customer_dashboard_screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Customer Dashboard Header & Portal Switcher
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "PLAYER PORTAL • ${profile.membershipTier.uppercase()} MEMBER",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Customer Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Welcome back, ${profile.fullName} • ${facility?.name ?: "Smash Pickle Club"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val firstActive = displayCourts.firstOrNull { it.status.equals("ACTIVE", ignoreCase = true) }
                                onSelectCourtToBook(firstActive?.id ?: 1)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OpticVolt,
                                contentColor = OpticVoltDarkText
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("customer_book_court_cta")
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Book a Court", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenMyReservations,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("nav_my_reservations_button")
                        ) {
                            Text(
                                text = "My Reservations (${customerBookings.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }
        }

        // 2. Customer Stats & Membership Perks Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("customer_stats_hero_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldDark)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = OpticVolt,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${profile.membershipTier} Tier • ${profile.discountPercent}% Court Discount",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OpticVolt
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = profile.fullName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${profile.skillLevel} • Preferred: ${profile.preferredPosition}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD6F5E6)
                            )
                        }

                        Surface(
                            color = OpticVolt.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, OpticVolt)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "DUPR",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OpticVolt
                                )
                                Text(
                                    text = "%.2f".format(profile.duprRating),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CustomerStatColumn("Games Played", "${profile.gamesPlayed}", "${profile.wins} Wins")
                        CustomerStatColumn("Win Streak", "${profile.winStreak} 🔥", "Active Streak")
                        CustomerStatColumn("Courts Open", "$activeCourtsCount / ${displayCourts.size}", "Available Today")
                    }
                }
            }
        }

        // 2b. Local Reservation Status Notification System & Heads-Up Alert
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                latestLocalAlert?.let { alert ->
                    LocalReservationStatusHeadsUpBanner(
                        alert = alert,
                        onViewReservations = onOpenMyReservations,
                        onDismiss = { LocalReservationNotificationService.dismissLatestAlert() }
                    )
                }

                CustomerLocalNotificationCenterCard(
                    bookings = customerBookings,
                    onSimulateStatusUpdate = { candidate ->
                        val target = candidate
                            ?: customerBookings.firstOrNull { it.status == "PENDING_ADMIN_APPROVAL" }
                            ?: customerBookings.firstOrNull()
                        if (target != null) {
                            localApprovedIds = localApprovedIds + target.id
                        }
                        if (onSimulateStatusUpdate != null) {
                            onSimulateStatusUpdate(target)
                        } else if (target != null) {
                            LocalReservationNotificationService.notifyReservationStatusChanged(
                                context = context,
                                booking = target.copy(
                                    status = "APPROVED_AWAITING_PAYMENT",
                                    paymentStatus = "AWAITING_PAYMENT",
                                    adminApprovedAt = "Approved Just Now"
                                ),
                                previousStatus = "Pending",
                                newStatus = "Approved"
                            )
                        }
                    }
                )

                CustomerCourtQrScannerCard(
                    bookings = customerBookings,
                    lastScanOutcome = lastCustomerQrScanOutcome,
                    onOpenScannerModal = { showCustomerQrScannerModal = true },
                    onQuickScanCourtBooking = { booking ->
                        val payload = ZxingQrScannerEngine.encodeCourtCheckInQrPayload(booking)
                        val outcome = resolveCustomerCourtQrCheckIn(payload, customerBookings)
                        lastCustomerQrScanOutcome = outcome
                        if (outcome is QrPassScanOutcome.CheckedInSuccess) {
                            localCheckedInIds = localCheckedInIds + outcome.booking.id
                            if (onCheckInBooking != null) {
                                onCheckInBooking(outcome.booking)
                            } else {
                                LocalReservationNotificationService.notifyReservationStatusChanged(
                                    context = context,
                                    booking = outcome.booking,
                                    previousStatus = "Paid",
                                    newStatus = "Checked In"
                                )
                            }
                        }
                    }
                )
            }
        }

        // 3. Live Court Booking Tracking & Paid QR Pass Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("live_booking_tracking_section"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Live Court Booking Tracking",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Real-time status: Admin Approval → Payment → Cashier Verify → QR Pass",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldPrimary.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clickable { onOpenMyReservations() }
                            .testTag("view_all_reservations_button")
                    ) {
                        Text(
                            text = "View All (${customerBookings.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Booking Status Badges Legend & Live Counts (Pending, Verified, Paid, Completed)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_status_summary_strip"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        CustomerReservationState.PENDING,
                        CustomerReservationState.VERIFIED,
                        CustomerReservationState.PAID,
                        CustomerReservationState.COMPLETED
                    ).forEach { stateItem ->
                        val count = customerBookings.count { resolveBookingStatusBadge(it).state == stateItem }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_status_badge_${stateItem.name}"),
                            shape = RoundedCornerShape(10.dp),
                            color = stateItem.bgColor,
                            border = BorderStroke(1.dp, stateItem.borderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(stateItem.dotColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${stateItem.badgeTitle} ($count)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = stateItem.textColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                val trackedBookings = customerBookings.take(4)
                trackedBookings.forEachIndexed { index, tracked ->
                    val badge = resolveBookingStatusBadge(tracked)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(
                                if (index == 0) "customer_upcoming_booking_card"
                                else "live_tracking_card_${tracked.id}"
                            ),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, badge.borderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${tracked.bookingCode}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                CustomerReservationStatusBadge(
                                    badgeInfo = badge,
                                    bookingId = tracked.id,
                                    tagPrefix = "dashboard_reservation"
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${tracked.facilityName} • ${tracked.courtName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Reserved by: ${tracked.playerName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldDark
                                    )
                                    Text(
                                        text = "${tracked.dateLabel} • ${tracked.timeRangeLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val paymentText = if (tracked.isWalkIn) {
                                        "Mode: Cash on Hand at Cashier (₱${tracked.totalAmount})"
                                    } else {
                                        "Total: ₱${tracked.totalAmount} via ${tracked.paymentMethod} (${tracked.paymentStatus})"
                                    }
                                    Text(
                                        text = paymentText,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (tracked.isQrPassReady) {
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clickable { onOpenBookingPass(tracked) }
                                            .testTag("customer_inline_qr_preview")
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            QrCodeMatrixCanvas(
                                                seedString = tracked.bookingCode,
                                                modifier = Modifier.size(50.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            BookingStageStepperBar(booking = tracked)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = badge.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (tracked.status == "APPROVED_AWAITING_PAYMENT" && !tracked.isWalkIn) {
                                    Button(
                                        onClick = { onOpenPaymentDialog(tracked) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("dashboard_pay_booking_${tracked.id}")
                                    ) {
                                        Text(
                                            text = "Pay & Upload Receipt",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                if (tracked.isQrPassReady) {
                                    Button(
                                        onClick = { onOpenBookingPass(tracked) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag(
                                                if (index == 0) "customer_view_qr_pass_button"
                                                else "dashboard_open_qr_pass_${tracked.id}"
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode2,
                                            contentDescription = null,
                                            tint = OpticVolt,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Paid Court QR Pass",
                                            color = OpticVolt,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                if (tracked.status != "CANCELLED" && tracked.status != "CHECKED_IN" && (tracked.isQrPassReady || tracked.paymentStatus == "PAID")) {
                                    OutlinedButton(
                                        onClick = {
                                            val payload = ZxingQrScannerEngine.encodeCourtCheckInQrPayload(tracked)
                                            val outcome = resolveCustomerCourtQrCheckIn(payload, customerBookings)
                                            lastCustomerQrScanOutcome = outcome
                                            if (outcome is QrPassScanOutcome.CheckedInSuccess) {
                                                localCheckedInIds = localCheckedInIds + outcome.booking.id
                                                if (onCheckInBooking != null) {
                                                    onCheckInBooking(outcome.booking)
                                                } else {
                                                    LocalReservationNotificationService.notifyReservationStatusChanged(
                                                        context = context,
                                                        booking = outcome.booking,
                                                        previousStatus = "Paid",
                                                        newStatus = "Checked In"
                                                    )
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, EmeraldPrimary),
                                        modifier = Modifier.testTag("dashboard_scan_qr_checkin_${tracked.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Scan QR Check-In",
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (tracked.isPendingCancellationAllowed) {
                                    OutlinedButton(
                                        onClick = { onCancelPendingReservation(tracked) },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaintenanceRed),
                                        modifier = Modifier.testTag("dashboard_cancel_booking_${tracked.id}")
                                    ) {
                                        Text(
                                            text = "Cancel Pending",
                                            color = MaintenanceRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Live Court Availability & Instant Booking Cards
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Live Court Availability & Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select an available court to choose your preferred day and time slot",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(displayCourts, key = { it.id }) { court ->
            val isActive = court.status.equals("ACTIVE", ignoreCase = true)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("customer_court_card_${court.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.outline else MaintenanceRed.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isActive) AvailableGreen else MaintenanceRed)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = court.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = if (isActive) AvailableGreenBg else MaintenanceRedBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isActive) "Available" else "Maintenance",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) AvailableGreen else MaintenanceRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${court.courtType} • ₱${court.morningPrice}–₱${court.peakPrice}/hr",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { onSelectCourtToBook(court.id) },
                        enabled = isActive,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("customer_reserve_court_${court.id}")
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.SportsTennis else Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isActive) "Reserve" else "Offline",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 5. Open Play & Promo Voucher Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Open Play & Club Vouchers",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                displayGames.firstOrNull()?.let { game ->
                    val playersList = game.playersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val vacantSpots = (game.maxPlayers - playersList.size).coerceAtLeast(0)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_open_play_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = game.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${game.dayLabel} • ${game.timeRange} • Skill: ${game.skillLevel} • ₱${game.pricePerPlayer}/player",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Vacancy: $vacantSpots Spots Open (${playersList.size}/${game.maxPlayers} Filled)",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = AvailableGreen,
                                    modifier = Modifier.testTag("customer_open_play_vacancy_badge")
                                )
                            }

                            OutlinedButton(
                                onClick = { onToggleJoinOpenPlay(game) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("customer_join_open_play_btn")
                            ) {
                                Text(
                                    text = if (game.isJoinedByUser) "Joined ✓" else "Join Open Play",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Promo Voucher Card
                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            promoAppliedBanner = "Promo code PICKLE50 (-₱50) applied to your checkout!"
                            onApplyPromoCode("PICKLE50")
                        }
                        .testTag("customer_apply_promo_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Promo Code: PICKLE50",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = EmeraldDark
                                )
                                Text(
                                    text = promoAppliedBanner ?: "Tap to apply ₱50 OFF your next court reservation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCustomerQrScannerModal) {
        CustomerCourtQrScannerDialog(
            bookings = customerBookings,
            lastScanOutcome = lastCustomerQrScanOutcome,
            onScanQrCode = { rawCode ->
                val outcome = resolveCustomerCourtQrCheckIn(rawCode, customerBookings)
                lastCustomerQrScanOutcome = outcome
                if (outcome is QrPassScanOutcome.CheckedInSuccess) {
                    localCheckedInIds = localCheckedInIds + outcome.booking.id
                    if (onCheckInBooking != null) {
                        onCheckInBooking(outcome.booking)
                    } else {
                        LocalReservationNotificationService.notifyReservationStatusChanged(
                            context = context,
                            booking = outcome.booking,
                            previousStatus = "Paid",
                            newStatus = "Checked In"
                        )
                    }
                }
            },
            onScanReservedCourtBooking = { booking ->
                val payload = ZxingQrScannerEngine.encodeCourtCheckInQrPayload(booking)
                val outcome = resolveCustomerCourtQrCheckIn(payload, customerBookings)
                lastCustomerQrScanOutcome = outcome
                if (outcome is QrPassScanOutcome.CheckedInSuccess) {
                    localCheckedInIds = localCheckedInIds + outcome.booking.id
                    if (onCheckInBooking != null) {
                        onCheckInBooking(outcome.booking)
                    } else {
                        LocalReservationNotificationService.notifyReservationStatusChanged(
                            context = context,
                            booking = outcome.booking,
                            previousStatus = "Paid",
                            newStatus = "Checked In"
                        )
                    }
                }
            },
            onDismiss = { showCustomerQrScannerModal = false }
        )
    }
}

@Composable
private fun CustomerStatColumn(label: String, value: String, subtext: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFA3B8B0),
            fontSize = 11.sp
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White
        )
        Text(
            text = subtext,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = OpticVolt
        )
    }
}
