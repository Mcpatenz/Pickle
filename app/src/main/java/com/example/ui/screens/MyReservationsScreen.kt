package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.BookingEntity
import com.example.data.remote.FirestoreReservationRepository
import com.example.notifications.CustomerLocalNotificationCenterCard
import com.example.notifications.LocalReservationNotificationService
import com.example.notifications.LocalReservationStatusHeadsUpBanner
import com.example.ui.components.CustomerCourtQrScannerCard
import com.example.ui.components.CustomerCourtQrScannerDialog
import com.example.ui.components.ExistingCourtReservationsList
import com.example.ui.components.LocalPickleballTournamentBracketCard
import com.example.ui.components.QrPassScanOutcome
import com.example.ui.components.ZxingQrScannerEngine
import com.example.ui.components.resolveCustomerCourtQrCheckIn
import com.example.viewmodel.AuthUserSession
import com.example.viewmodel.BusinessPaymentSettings
import com.example.viewmodel.BusinessSettingsLocalStore

@Composable
fun MyReservationsScreen(
    isAuthenticated: Boolean,
    authSession: AuthUserSession?,
    bookings: List<BookingEntity>,
    onBackToDashboard: () -> Unit,
    onBookNewCourt: () -> Unit,
    onOpenAuthModal: () -> Unit,
    onCancelPendingReservation: (BookingEntity) -> Unit,
    onOpenPaymentDialog: (BookingEntity) -> Unit,
    onOpenQrPass: (BookingEntity) -> Unit,
    onSimulateStatusUpdate: ((BookingEntity?) -> Unit)? = null,
    onCheckInBooking: ((BookingEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val latestLocalAlert by LocalReservationNotificationService.latestAlert.collectAsState()
    var localApprovedBookingIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var localCheckedInBookingIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var localCancelledBookingIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var localAddedBookings by remember { mutableStateOf<List<BookingEntity>>(emptyList()) }
    var showCustomerQrScannerModal by remember { mutableStateOf(false) }
    var lastCustomerQrScanOutcome by remember { mutableStateOf<QrPassScanOutcome?>(null) }

    BackHandler {
        onBackToDashboard()
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Upcoming, 1 = Past
    var selectedStateFilter by remember { mutableStateOf<CustomerReservationState?>(null) }

    val effectiveBookings = remember(bookings, localApprovedBookingIds, localCheckedInBookingIds, localCancelledBookingIds, localAddedBookings) {
        (localAddedBookings + bookings)
            .filterNot { localCancelledBookingIds.contains(it.id) }
            .map { booking ->
            when {
                localCheckedInBookingIds.contains(booking.id) && booking.status != "CANCELLED" -> {
                    booking.copy(
                        status = "CHECKED_IN",
                        paymentStatus = "PAID",
                        checkInTime = booking.checkInTime ?: "1:47 PM"
                    )
                }
                localApprovedBookingIds.contains(booking.id) && booking.status == "PENDING_ADMIN_APPROVAL" -> {
                    booking.copy(
                        status = "APPROVED_AWAITING_PAYMENT",
                        paymentStatus = "AWAITING_PAYMENT",
                        adminApprovedAt = "Approved Just Now"
                    )
                }
                else -> booking
            }
        }
    }

    // Filter bookings for registered customer (match customer name/email or show all customer bookings)
    val customerName = authSession?.fullName?.trim()
    val customerEmail = authSession?.email?.trim()
    val customerBookings = remember(effectiveBookings, customerName, customerEmail) {
        val matching = effectiveBookings.filter { b ->
            (customerName != null && b.playerName.equals(customerName, ignoreCase = true)) ||
                (customerEmail != null && b.customerEmail.equals(customerEmail, ignoreCase = true)) ||
                b.playerName.equals("Jonel P.", ignoreCase = true)
        }
        if (matching.isNotEmpty()) matching else effectiveBookings
    }

    val upcomingBookings = remember(customerBookings) {
        customerBookings.filter { it.isUpcomingBooking }.sortedByDescending { it.createdAt }
    }
    val pastBookings = remember(customerBookings) {
        customerBookings.filter { it.isPastBooking }.sortedByDescending { it.createdAt }
    }

    val pendingCount = remember(customerBookings) {
        customerBookings.count { resolveBookingStatusBadge(it).state == CustomerReservationState.PENDING }
    }
    val verifiedCount = remember(customerBookings) {
        customerBookings.count { resolveBookingStatusBadge(it).state == CustomerReservationState.VERIFIED }
    }
    val paidCount = remember(customerBookings) {
        customerBookings.count { resolveBookingStatusBadge(it).state == CustomerReservationState.PAID }
    }
    val completedCount = remember(customerBookings) {
        customerBookings.count { resolveBookingStatusBadge(it).state == CustomerReservationState.COMPLETED }
    }

    val displayedBookings = remember(upcomingBookings, pastBookings, customerBookings, selectedTab, selectedStateFilter) {
        val stateFilter = selectedStateFilter
        if (stateFilter != null) {
            customerBookings
                .filter { resolveBookingStatusBadge(it).state == stateFilter }
                .sortedByDescending { it.createdAt }
        } else {
            if (selectedTab == 0) upcomingBookings else pastBookings
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("my_reservations_screen"),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF064E3B),
                                Color(0xFF047857),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = onBackToDashboard,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.14f), CircleShape)
                                    .testTag("my_reservations_back_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Customer Dashboard",
                                    tint = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = "MY RESERVATIONS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF6EE7B7),
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.1.sp
                                )
                                Text(
                                    text = "Court Bookings & QR Passes",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Button(
                            onClick = onBookNewCourt,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("my_reservations_book_court_button")
                        ) {
                            Icon(Icons.Default.SportsTennis, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Book Court", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Summary KPI Chips for Booking States (Pending, Verified, Paid, Completed)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReservationKpiPill(
                            modifier = Modifier.weight(1f),
                            label = "Pending",
                            value = "$pendingCount",
                            accent = Color(0xFFFBBF24)
                        )
                        ReservationKpiPill(
                            modifier = Modifier.weight(1f),
                            label = "Verified",
                            value = "$verifiedCount",
                            accent = Color(0xFF60A5FA)
                        )
                        ReservationKpiPill(
                            modifier = Modifier.weight(1f),
                            label = "Paid",
                            value = "$paidCount",
                            accent = Color(0xFF34D399)
                        )
                        ReservationKpiPill(
                            modifier = Modifier.weight(1f),
                            label = "Completed",
                            value = "$completedCount",
                            accent = Color(0xFF38BDF8)
                        )
                    }
                }
            }
        }

        // Unauthenticated Prompt if user is not logged in
        if (!isAuthenticated) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("my_reservations_login_prompt"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD97706))
                            Text(
                                text = "Registered Customer Account Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF92400E)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Log in or register as a customer to book available courts, upload payment receipts, and manage your reservations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF78350F)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenAuthModal,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("my_reservations_login_button")
                        ) {
                            Text("Log In / Register Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Heads-Up Local Notification Banner when a reservation status updates (e.g., Pending -> Approved)
        latestLocalAlert?.let { alert ->
            item {
                LocalReservationStatusHeadsUpBanner(
                    alert = alert,
                    onViewReservations = { LocalReservationNotificationService.dismissLatestAlert() },
                    onDismiss = { LocalReservationNotificationService.dismissLatestAlert() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Local Reservation Status Notification System Card
        item {
            CustomerLocalNotificationCenterCard(
                bookings = customerBookings,
                onSimulateStatusUpdate = { candidate ->
                    val target = candidate
                        ?: customerBookings.firstOrNull { it.status == "PENDING_ADMIN_APPROVAL" }
                        ?: customerBookings.firstOrNull()
                    if (target != null) {
                        localApprovedBookingIds = localApprovedBookingIds + target.id
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
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Firebase Cloud Messaging (FCM) Push Notification Hub Card
        item {
            com.example.notifications.FcmPushNotificationCenterCard(
                bookings = customerBookings,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // CameraX Court QR Code Check-In Scanner Card
        item {
            CustomerCourtQrScannerCard(
                bookings = customerBookings,
                lastScanOutcome = lastCustomerQrScanOutcome,
                onOpenScannerModal = { showCustomerQrScannerModal = true },
                onQuickScanCourtBooking = { booking ->
                    val payload = ZxingQrScannerEngine.encodeCourtCheckInQrPayload(booking)
                    val outcome = resolveCustomerCourtQrCheckIn(payload, customerBookings)
                    lastCustomerQrScanOutcome = outcome
                    if (outcome is QrPassScanOutcome.CheckedInSuccess) {
                        localCheckedInBookingIds = localCheckedInBookingIds + outcome.booking.id
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
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Booking Workflow Guide Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "COURT BOOKING & VERIFICATION WORKFLOW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF065F46),
                        letterSpacing = 0.7.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. Book Slot → 2. Admin Approval → 3. Upload Receipt & Ref # (or Cash on Hand at Cashier for Walk-In) → 4. Cashier Verification → 5. Paid Court QR Pass Issued.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF047857)
                    )
                }
            }
        }

        // Filter Tabs: Upcoming vs Past Bookings + Visual Status Badges Filter (Pending, Verified, Paid, Completed)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTab == 0 && selectedStateFilter == null) Color(0xFF065F46) else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (selectedTab == 0 && selectedStateFilter == null) Color(0xFF065F46) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = 0
                                selectedStateFilter = null
                            }
                            .testTag("tab_upcoming_bookings")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (selectedTab == 0 && selectedStateFilter == null) Color.White else Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Upcoming (${upcomingBookings.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selectedTab == 0 && selectedStateFilter == null) Color.White else Color(0xFF334155)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTab == 1 && selectedStateFilter == null) Color(0xFF065F46) else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (selectedTab == 1 && selectedStateFilter == null) Color(0xFF065F46) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = 1
                                selectedStateFilter = null
                            }
                            .testTag("tab_past_bookings")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (selectedTab == 1 && selectedStateFilter == null) Color.White else Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Past Bookings (${pastBookings.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selectedTab == 1 && selectedStateFilter == null) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }

                // Visual Status Badge Filter Row: All, Pending, Verified, Paid, Completed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reservation_status_filter_bar"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterOptions: List<Pair<CustomerReservationState?, String>> = listOf(
                        null to "All (${customerBookings.size})",
                        CustomerReservationState.PENDING to "Pending ($pendingCount)",
                        CustomerReservationState.VERIFIED to "Verified ($verifiedCount)",
                        CustomerReservationState.PAID to "Paid ($paidCount)",
                        CustomerReservationState.COMPLETED to "Completed ($completedCount)"
                    )
                    filterOptions.forEach { (stateOption, chipText) ->
                        val isSelected = selectedStateFilter == stateOption
                        val tagSuffix = stateOption?.name ?: "ALL"
                        val chipBg = when {
                            isSelected && stateOption != null -> stateOption.bgColor
                            isSelected -> Color(0xFF065F46)
                            else -> Color.White
                        }
                        val chipBorder = when {
                            isSelected && stateOption != null -> stateOption.borderColor
                            isSelected -> Color(0xFF065F46)
                            else -> Color(0xFFE2E8F0)
                        }
                        val chipTextColor = when {
                            isSelected && stateOption != null -> stateOption.textColor
                            isSelected -> Color.White
                            else -> Color(0xFF334155)
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = chipBg,
                            border = BorderStroke(1.dp, chipBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedStateFilter = stateOption }
                                .testTag("status_filter_$tagSuffix")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (stateOption != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(stateOption.dotColor)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = chipText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = chipTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        if (displayedBookings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.SportsTennis,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (selectedTab == 0) "No upcoming reservations yet" else "No past bookings found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select an available day and time slot to book a pickleball court.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onBookNewCourt,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Browse Available Courts", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(displayedBookings, key = { it.id }) { booking ->
                CustomerReservationItemCard(
                    booking = booking,
                    onCancelPending = {
                        localCancelledBookingIds = localCancelledBookingIds + booking.id
                        FirestoreReservationRepository.removeReservationFromFirestore(
                            context = context,
                            bookingCode = booking.bookingCode,
                            bookingId = booking.id
                        )
                        onCancelPendingReservation(booking)
                    },
                    onPayAndUploadReceipt = { onOpenPaymentDialog(booking) },
                    onOpenQrPass = { onOpenQrPass(booking) },
                    onScanCourtQrCheckIn = {
                        val payload = ZxingQrScannerEngine.encodeCourtCheckInQrPayload(booking)
                        val outcome = resolveCustomerCourtQrCheckIn(payload, customerBookings)
                        lastCustomerQrScanOutcome = outcome
                        if (outcome is QrPassScanOutcome.CheckedInSuccess) {
                            localCheckedInBookingIds = localCheckedInBookingIds + outcome.booking.id
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
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Firebase Firestore Persisted Court Reservations List (with Cancel & Upcoming 7 Days Filter)
        item {
            ExistingCourtReservationsList(
                modifier = Modifier.padding(horizontal = 16.dp),
                reservations = customerBookings,
                includeFirestorePersisted = true,
                onSelectReservation = { booking ->
                    if (booking.isQrPassReady) {
                        onOpenQrPass(booking)
                    }
                },
                onOpenQrPass = { booking -> onOpenQrPass(booking) },
                onCancelReservation = { cancelled ->
                    localCancelledBookingIds = localCancelledBookingIds + cancelled.id
                    FirestoreReservationRepository.removeReservationFromFirestore(
                        context = context,
                        bookingCode = cancelled.bookingCode,
                        bookingId = cancelled.id
                    )
                    onCancelPendingReservation(cancelled)
                }
            )
        }

        // Local Room Database Past & Upcoming Reservations Overview Card
        item {
            RoomPastAndUpcomingReservationsCard(
                upcomingBookings = upcomingBookings,
                pastBookings = pastBookings,
                onQuickAddUpcoming = {
                    val nextId = ((effectiveBookings.maxOfOrNull { it.id } ?: 900) + 1)
                    val newUpcoming = BookingEntity(
                        id = nextId,
                        bookingCode = "PKL-20261002-$nextId",
                        facilityId = 1,
                        facilityName = "Smash Pickle Club",
                        facilityLocation = "Quezon City",
                        courtId = 3,
                        courtName = "Court 3",
                        dateIso = "2026-10-02",
                        dateLabel = "October 2, 2026",
                        timeSlot = "07:00 PM",
                        timeRangeLabel = "7:00 PM - 8:00 PM",
                        playerName = customerName ?: "Jonel P.",
                        courtFee = 350,
                        discountAmount = 0,
                        serviceFee = 20,
                        totalAmount = 370,
                        paymentMethod = "GCash",
                        paymentStatus = "PAID",
                        status = "CONFIRMED",
                        customerEmail = customerEmail ?: "customer@pickleplay.ph"
                    )
                    localAddedBookings = listOf(newUpcoming) + localAddedBookings
                },
                onQuickAddPast = {
                    val nextId = ((effectiveBookings.maxOfOrNull { it.id } ?: 900) + 1)
                    val newPast = BookingEntity(
                        id = nextId,
                        bookingCode = "PKL-20260920-$nextId",
                        facilityId = 2,
                        facilityName = "BGC Dink & Rally Club",
                        facilityLocation = "Taguig City",
                        courtId = 1,
                        courtName = "Court 1",
                        dateIso = "2026-09-20",
                        dateLabel = "September 20, 2026",
                        timeSlot = "05:00 PM",
                        timeRangeLabel = "5:00 PM - 6:00 PM",
                        playerName = customerName ?: "Jonel P.",
                        courtFee = 350,
                        discountAmount = 0,
                        serviceFee = 20,
                        totalAmount = 370,
                        paymentMethod = "PayMaya",
                        paymentStatus = "PAID",
                        status = "COMPLETED",
                        checkInTime = "4:50 PM",
                        customerEmail = customerEmail ?: "customer@pickleplay.ph"
                    )
                    localAddedBookings = listOf(newPast) + localAddedBookings
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Local Pickleball Competition Bracket Visualizer Card
        item {
            LocalPickleballTournamentBracketCard(
                modifier = Modifier.padding(horizontal = 16.dp)
            )
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
                    localCheckedInBookingIds = localCheckedInBookingIds + outcome.booking.id
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
                    localCheckedInBookingIds = localCheckedInBookingIds + outcome.booking.id
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
private fun ReservationKpiPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accent: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE2E8F0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RoomPastAndUpcomingReservationsCard(
    upcomingBookings: List<BookingEntity>,
    pastBookings: List<BookingEntity>,
    onQuickAddUpcoming: () -> Unit = {},
    onQuickAddPast: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("room_reservations_summary_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = Color(0xFF064E3B),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "LOCAL ROOM DATABASE • RESERVATION LEDGER",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFCEFF00),
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("room_db_status_badge")
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Upcoming (${upcomingBookings.size}) & Past (${pastBookings.size}) Court Reservations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.testTag("room_reservations_counts_title")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Add Buttons for Upcoming & Past Room Reservations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onQuickAddUpcoming,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF059669)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("room_quick_add_upcoming_button")
                ) {
                    Text(
                        text = "+ Log Upcoming Court",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )
                }
                OutlinedButton(
                    onClick = onQuickAddPast,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("room_quick_add_past_button")
                ) {
                    Text(
                        text = "+ Log Past Court",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Upcoming Reservations Section
            Text(
                text = "UPCOMING COURT RESERVATIONS (${upcomingBookings.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF047857),
                modifier = Modifier.testTag("room_upcoming_header")
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (upcomingBookings.isEmpty()) {
                Text(
                    text = "No upcoming court reservations stored.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.testTag("room_upcoming_reservations_section")
                ) {
                    upcomingBookings.take(4).forEach { booking ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("room_upcoming_reservation_row_${booking.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${booking.courtName} • ${booking.facilityName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF064E3B)
                                    )
                                    Text(
                                        text = "${booking.dateLabel} • ${booking.timeRangeLabel} (#${booking.bookingCode})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF047857)
                                    )
                                }
                                Text(
                                    text = "UPCOMING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Past Reservations Section
            Text(
                text = "PAST COURT RESERVATIONS (${pastBookings.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0369A1),
                modifier = Modifier.testTag("room_past_header")
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (pastBookings.isEmpty()) {
                Text(
                    text = "No past court reservations recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.testTag("room_past_reservations_section")
                ) {
                    pastBookings.take(4).forEach { booking ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0F9FF),
                            border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("room_past_reservation_row_${booking.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${booking.courtName} • ${booking.facilityName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0C4A6E)
                                    )
                                    Text(
                                        text = "${booking.dateLabel} • ${booking.timeRangeLabel} (#${booking.bookingCode})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF0369A1)
                                    )
                                }
                                Text(
                                    text = booking.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerReservationItemCard(
    booking: BookingEntity,
    onCancelPending: () -> Unit,
    onPayAndUploadReceipt: () -> Unit,
    onOpenQrPass: () -> Unit,
    onScanCourtQrCheckIn: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusBadgeInfo = resolveBookingStatusBadge(booking)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reservation_card_${booking.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, statusBadgeInfo.borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Booking Code & Status Indicator Badge
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
                        shape = CircleShape,
                        color = statusBadgeInfo.bgColor,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = statusBadgeInfo.icon,
                                contentDescription = null,
                                tint = statusBadgeInfo.textColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "${booking.courtName} • ${booking.facilityName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Booking #${booking.bookingCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                CustomerReservationStatusBadge(
                    badgeInfo = statusBadgeInfo,
                    bookingId = booking.id,
                    tagPrefix = "reservation"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Schedule & Payment Details Row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📅 ${booking.dateLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "⏰ ${booking.timeRangeLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val modeLabel = if (booking.isWalkIn) {
                            "Walk-In • Cash on Hand at Cashier"
                        } else {
                            "Mode: ${booking.paymentMethod} (${booking.paymentStatus})"
                        }
                        Text(
                            text = modeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Total: ₱${booking.totalAmount}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF065F46)
                        )
                    }

                    if (!booking.referenceNumber.isNullOrBlank() || !booking.receiptFileName.isNullOrBlank()) {
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ref #: ${booking.referenceNumber ?: "N/A"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8),
                                modifier = Modifier.testTag("reservation_ref_number_${booking.id}")
                            )
                            Text(
                                text = "📎 ${booking.receiptFileName ?: "Receipt attached"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            // 4-Stage Pipeline Indicator for non-cancelled bookings
            if (booking.status != "CANCELLED") {
                Spacer(modifier = Modifier.height(12.dp))
                BookingStageStepperBar(booking = booking)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status guidance message
            Text(
                text = statusBadgeInfo.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569)
            )

            // Action Buttons Row
            val showPayButton = booking.status == "APPROVED_AWAITING_PAYMENT" && !booking.isWalkIn
            val showQrPassButton = booking.isQrPassReady
            val showCancelPendingButton = booking.isPendingCancellationAllowed
            val showScanCheckInButton = onScanCourtQrCheckIn != null &&
                booking.status != "CANCELLED" &&
                booking.status != "CHECKED_IN" &&
                (booking.isQrPassReady || booking.paymentStatus == "PAID")

            if (showPayButton || showQrPassButton || showCancelPendingButton || showScanCheckInButton) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showPayButton) {
                        Button(
                            onClick = onPayAndUploadReceipt,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pay_approved_booking_${booking.id}")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pay & Upload Receipt",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    if (showQrPassButton) {
                        Button(
                            onClick = onOpenQrPass,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_qr_pass_button_${booking.id}")
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "View Paid QR Pass",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    if (showScanCheckInButton) {
                        Button(
                            onClick = { onScanCourtQrCheckIn?.invoke() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF064E3B),
                                contentColor = Color(0xFFCEFF00)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scan_checkin_reservation_${booking.id}")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scan QR Check-In",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    if (showCancelPendingButton) {
                        OutlinedButton(
                            onClick = onCancelPending,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            modifier = Modifier
                                .weight(if (showPayButton) 0.85f else 1f)
                                .testTag("cancel_pending_reservation_${booking.id}")
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cancel Pending",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class CustomerReservationState(
    val badgeTitle: String,
    val stepIndex: Int,
    val bgColor: Color,
    val textColor: Color,
    val borderColor: Color,
    val dotColor: Color
) {
    PENDING(
        badgeTitle = "Pending",
        stepIndex = 1,
        bgColor = Color(0xFFFEF3C7),
        textColor = Color(0xFFB45309),
        borderColor = Color(0xFFF59E0B),
        dotColor = Color(0xFFD97706)
    ),
    VERIFIED(
        badgeTitle = "Verified",
        stepIndex = 2,
        bgColor = Color(0xFFDBEAFE),
        textColor = Color(0xFF1D4ED8),
        borderColor = Color(0xFF60A5FA),
        dotColor = Color(0xFF2563EB)
    ),
    PAID(
        badgeTitle = "Paid",
        stepIndex = 3,
        bgColor = Color(0xFFD1FAE5),
        textColor = Color(0xFF065F46),
        borderColor = Color(0xFF34D399),
        dotColor = Color(0xFF059669)
    ),
    COMPLETED(
        badgeTitle = "Completed",
        stepIndex = 4,
        bgColor = Color(0xFFE0F2FE),
        textColor = Color(0xFF0369A1),
        borderColor = Color(0xFF38BDF8),
        dotColor = Color(0xFF0284C7)
    ),
    CANCELLED(
        badgeTitle = "Cancelled",
        stepIndex = 0,
        bgColor = Color(0xFFFEE2E2),
        textColor = Color(0xFFB91C1C),
        borderColor = Color(0xFFFECACA),
        dotColor = Color(0xFFDC2626)
    )
}

@Composable
fun CustomerReservationStatusBadge(
    badgeInfo: BookingStatusBadgeUi,
    bookingId: Int,
    modifier: Modifier = Modifier,
    tagPrefix: String = "reservation"
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = badgeInfo.bgColor,
            border = BorderStroke(1.2.dp, badgeInfo.borderColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(badgeInfo.state.dotColor)
                )
                Icon(
                    imageVector = badgeInfo.icon,
                    contentDescription = null,
                    tint = badgeInfo.textColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = badgeInfo.shortBadge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = badgeInfo.textColor,
                    modifier = Modifier.testTag("${tagPrefix}_state_badge_$bookingId")
                )
            }
        }
        Text(
            text = badgeInfo.label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = badgeInfo.textColor,
            modifier = Modifier.testTag("${tagPrefix}_status_$bookingId")
        )
    }
}

@Composable
fun BookingStageStepperBar(booking: BookingEntity) {
    val resolvedBadge = resolveBookingStatusBadge(booking)
    val activeStep = resolvedBadge.state.stepIndex

    val canonicalStates = listOf(
        CustomerReservationState.PENDING,
        CustomerReservationState.VERIFIED,
        CustomerReservationState.PAID,
        CustomerReservationState.COMPLETED
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("booking_stepper_${booking.id}"),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        canonicalStates.forEach { stageState ->
            val stepNum = stageState.stepIndex
            val isCompleted = stepNum < activeStep || (stepNum == 4 && activeStep == 4)
            val isCurrent = stepNum == activeStep

            val bgColor = when {
                isCurrent -> stageState.bgColor
                isCompleted -> Color(0xFFD1FAE5)
                else -> Color(0xFFF1F5F9)
            }
            val textColor = when {
                isCurrent -> stageState.textColor
                isCompleted -> Color(0xFF065F46)
                else -> Color(0xFF94A3B8)
            }
            val borderColor = when {
                isCurrent -> stageState.borderColor
                isCompleted -> Color(0xFF6EE7B7)
                else -> Color(0xFFE2E8F0)
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .testTag("booking_stage_chip_${booking.id}_${stageState.name}"),
                shape = RoundedCornerShape(8.dp),
                color = bgColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCurrent -> stageState.dotColor
                                    isCompleted -> Color(0xFF059669)
                                    else -> Color(0xFFCBD5E1)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCompleted && !isCurrent) "✓ ${stageState.badgeTitle}" else stageState.badgeTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = if (isCompleted || isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

data class BookingStatusBadgeUi(
    val state: CustomerReservationState,
    val shortBadge: String,
    val label: String,
    val description: String,
    val bgColor: Color,
    val textColor: Color,
    val borderColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

fun resolveBookingStatusBadge(booking: BookingEntity): BookingStatusBadgeUi {
    return when (booking.status) {
        "PENDING_ADMIN_APPROVAL", "PENDING" -> BookingStatusBadgeUi(
            state = CustomerReservationState.PENDING,
            shortBadge = CustomerReservationState.PENDING.badgeTitle,
            label = "Pending Admin Approval",
            description = "State: Pending — Sent to Club Admin for approval. Once verified, you can upload your payment receipt.",
            bgColor = CustomerReservationState.PENDING.bgColor,
            textColor = CustomerReservationState.PENDING.textColor,
            borderColor = CustomerReservationState.PENDING.borderColor,
            icon = Icons.Default.HourglassTop
        )
        "APPROVED_AWAITING_PAYMENT", "VERIFIED" -> BookingStatusBadgeUi(
            state = CustomerReservationState.VERIFIED,
            shortBadge = CustomerReservationState.VERIFIED.badgeTitle,
            label = "Verified • Awaiting Payment",
            description = "State: Verified — Admin verified your court slot! Upload your payment screenshot/receipt and Reference #.",
            bgColor = CustomerReservationState.VERIFIED.bgColor,
            textColor = CustomerReservationState.VERIFIED.textColor,
            borderColor = CustomerReservationState.VERIFIED.borderColor,
            icon = Icons.Default.Payments
        )
        "PENDING_CASHIER_VERIFICATION" -> {
            if (booking.isWalkIn) {
                BookingStatusBadgeUi(
                    state = CustomerReservationState.VERIFIED,
                    shortBadge = CustomerReservationState.VERIFIED.badgeTitle,
                    label = "Verified • Cash at Cashier",
                    description = "State: Verified — Walk-In slot verified. Pay Cash on Hand (₱${booking.totalAmount}) at Cashier to issue your Paid QR Pass.",
                    bgColor = CustomerReservationState.VERIFIED.bgColor,
                    textColor = CustomerReservationState.VERIFIED.textColor,
                    borderColor = CustomerReservationState.VERIFIED.borderColor,
                    icon = Icons.Default.AccountBalanceWallet
                )
            } else {
                BookingStatusBadgeUi(
                    state = CustomerReservationState.VERIFIED,
                    shortBadge = CustomerReservationState.VERIFIED.badgeTitle,
                    label = "Verified • Cashier Review",
                    description = "State: Verified — Receipt & Ref # (${booking.referenceNumber ?: "Submitted"}) under Cashier review for Paid QR Pass.",
                    bgColor = CustomerReservationState.VERIFIED.bgColor,
                    textColor = CustomerReservationState.VERIFIED.textColor,
                    borderColor = CustomerReservationState.VERIFIED.borderColor,
                    icon = Icons.Default.ReceiptLong
                )
            }
        }
        "CONFIRMED", "PAID" -> BookingStatusBadgeUi(
            state = CustomerReservationState.PAID,
            shortBadge = CustomerReservationState.PAID.badgeTitle,
            label = "Paid • QR Pass Ready",
            description = "State: Paid — Cashier confirmed your payment (₱${booking.totalAmount}). Tap 'View Paid QR Pass' for court check-in!",
            bgColor = CustomerReservationState.PAID.bgColor,
            textColor = CustomerReservationState.PAID.textColor,
            borderColor = CustomerReservationState.PAID.borderColor,
            icon = Icons.Default.Verified
        )
        "CHECKED_IN", "COMPLETED" -> BookingStatusBadgeUi(
            state = CustomerReservationState.COMPLETED,
            shortBadge = CustomerReservationState.COMPLETED.badgeTitle,
            label = "Completed • Checked In",
            description = "State: Completed — Player checked in and session completed (${booking.checkInTime ?: "Checked in"}).",
            bgColor = CustomerReservationState.COMPLETED.bgColor,
            textColor = CustomerReservationState.COMPLETED.textColor,
            borderColor = CustomerReservationState.COMPLETED.borderColor,
            icon = Icons.Default.CheckCircle
        )
        "CANCELLED" -> BookingStatusBadgeUi(
            state = CustomerReservationState.CANCELLED,
            shortBadge = CustomerReservationState.CANCELLED.badgeTitle,
            label = "Cancelled",
            description = "Reservation cancelled (${booking.paymentStatus}).",
            bgColor = CustomerReservationState.CANCELLED.bgColor,
            textColor = CustomerReservationState.CANCELLED.textColor,
            borderColor = CustomerReservationState.CANCELLED.borderColor,
            icon = Icons.Default.Cancel
        )
        else -> BookingStatusBadgeUi(
            state = CustomerReservationState.PENDING,
            shortBadge = CustomerReservationState.PENDING.badgeTitle,
            label = booking.status,
            description = "Reservation status: ${booking.status}",
            bgColor = CustomerReservationState.PENDING.bgColor,
            textColor = CustomerReservationState.PENDING.textColor,
            borderColor = CustomerReservationState.PENDING.borderColor,
            icon = Icons.Default.Info
        )
    }
}

@Composable
fun CustomerPaymentSubmissionDialog(
    booking: BookingEntity,
    businessSettings: BusinessPaymentSettings = BusinessPaymentSettings(),
    onSubmitPaymentProof: (
        paymentMethod: String,
        referenceNumber: String,
        receiptUri: String,
        receiptFileName: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedWallet by rememberSaveable(booking.id) {
        mutableStateOf(if (booking.paymentMethod.equals("PayMaya", ignoreCase = true)) "PayMaya" else "GCash")
    }
    var referenceNumber by rememberSaveable(booking.id) {
        mutableStateOf(booking.referenceNumber.orEmpty())
    }
    var uploadedReceiptUri by rememberSaveable(booking.id) {
        mutableStateOf(booking.receiptUri.orEmpty())
    }
    var uploadedReceiptFileName by rememberSaveable(booking.id) {
        mutableStateOf(booking.receiptFileName.orEmpty())
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    val storedQr = remember(selectedWallet, businessSettings) {
        BusinessSettingsLocalStore.getStoredQrCode(
            context = context,
            paymentMethod = selectedWallet,
            fallback = businessSettings
        )
    }

    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val uriStr = uri?.toString()
        if (!uriStr.isNullOrBlank()) {
            val extracted = uriStr.substringAfterLast('/').ifBlank {
                "${selectedWallet.lowercase()}_receipt_${booking.bookingCode}.png"
            }
            uploadedReceiptUri = uriStr
            uploadedReceiptFileName = if (extracted.contains('.')) extracted else "$extracted.png"
            validationError = null
        } else {
            val generatedName = "${selectedWallet.lowercase()}_receipt_${booking.bookingCode}.png"
            uploadedReceiptUri = "file://local/$generatedName"
            uploadedReceiptFileName = generatedName
            validationError = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
                .testTag("customer_payment_submission_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFDBEAFE),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "ADMIN APPROVED • COMPLETE PAYMENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1D4ED8),
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Upload Receipt & Reference #",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Booking Summary Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${booking.courtName} • ${booking.facilityName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "₱${booking.totalAmount}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF059669)
                            )
                        }
                        Text(
                            text = "${booking.dateLabel} • ${booking.timeRangeLabel} (#${booking.bookingCode})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Select E-Wallet
                Text(
                    text = "1. Scan Official Club QR Code to Pay",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("GCash", "PayMaya").forEach { method ->
                        val selected = selectedWallet.equals(method, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedWallet = method }
                                .testTag("payment_dialog_wallet_${method.uppercase()}")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = method,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selected) Color(0xFF1D4ED8) else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Club QR Display
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            bitmap = storedQr.bitmap.asImageBitmap(),
                            contentDescription = "${storedQr.paymentMethod} Official QR",
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .padding(4.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${storedQr.businessName} (${storedQr.paymentMethod})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Merchant: ${storedQr.merchantCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "Amount to Send: ₱${booking.totalAmount}.00",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Required Step 2: Upload Screenshot or Receipt
                Text(
                    text = "2. Upload Payment Screenshot or Receipt (Required)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val generatedName = "${selectedWallet.lowercase()}_receipt_${booking.bookingCode}.png"
                            uploadedReceiptUri = "file://local/$generatedName"
                            uploadedReceiptFileName = generatedName
                            validationError = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F766E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_payment_receipt_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Screenshot / Receipt", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            runCatching { receiptPickerLauncher.launch("image/*") }.onFailure {
                                val generatedName = "${selectedWallet.lowercase()}_receipt_${booking.bookingCode}.png"
                                uploadedReceiptUri = "file://local/$generatedName"
                                uploadedReceiptFileName = generatedName
                                validationError = null
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("browse_gallery_receipt_button")
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (uploadedReceiptFileName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("uploaded_receipt_preview")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Screenshot Attached ✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        text = uploadedReceiptFileName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Required Step 3: Input Reference Number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. Input Payment Reference Number (Required)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Fill Sample Ref #",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier
                            .clickable {
                                referenceNumber = "REF-${selectedWallet.take(2).uppercase()}-${(100000..999999).random()}"
                                validationError = null
                            }
                            .testTag("sample_ref_number_button")
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = {
                        referenceNumber = it
                        validationError = null
                    },
                    label = { Text("$selectedWallet Reference Number (e.g. 9012-3456-7890)") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_reference_number_input")
                )

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("payment_validation_error")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Later", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            when {
                                uploadedReceiptFileName.isBlank() -> {
                                    validationError = "Please upload your payment screenshot or receipt first."
                                }
                                referenceNumber.trim().isBlank() -> {
                                    validationError = "Please input your payment Reference Number."
                                }
                                else -> {
                                    onSubmitPaymentProof(
                                        selectedWallet,
                                        referenceNumber.trim(),
                                        uploadedReceiptUri.ifBlank { "file://local/$uploadedReceiptFileName" },
                                        uploadedReceiptFileName
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.6f)
                            .testTag("submit_payment_to_cashier_button")
                    ) {
                        Text("Submit to Cashier", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
