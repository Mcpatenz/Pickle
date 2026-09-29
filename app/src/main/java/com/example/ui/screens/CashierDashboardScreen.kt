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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookingEntity
import com.example.data.local.CourtEntity
import com.example.data.local.FacilityEntity
import com.example.ui.components.CashierQrCodeScannerDialog
import com.example.ui.components.CashierQrScannerStationCard
import com.example.ui.components.PaymentQrCodeDisplayCard
import com.example.ui.components.QrPassScanOutcome
import com.example.ui.components.resolveQrPassScan
import com.example.ui.theme.AvailableGreen
import com.example.ui.theme.AvailableGreenBg
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MaintenanceRed
import com.example.ui.theme.OpticVolt
import com.example.ui.theme.OpticVoltDarkText
import com.example.ui.theme.PeakAmber
import com.example.ui.theme.PeakAmberBg
import com.example.viewmodel.BusinessPaymentSettings
import com.example.viewmodel.ClubProductItem

private val DefaultCashierBookings = listOf(
    BookingEntity(
        id = 5,
        bookingCode = "PKL-20260928-00126",
        facilityId = 1,
        facilityName = "Smash Pickle Club",
        facilityLocation = "Quezon City",
        courtId = 2,
        courtName = "Court 2",
        dateIso = "2026-09-28",
        dateLabel = "September 28, 2026",
        timeSlot = "03:00 PM",
        timeRangeLabel = "3:00 PM - 4:00 PM",
        playerName = "Miguel S.",
        courtFee = 250,
        discountAmount = 0,
        serviceFee = 20,
        totalAmount = 270,
        paymentMethod = "Pay at Venue",
        paymentStatus = "PENDING_VENUE",
        status = "CONFIRMED",
        checkInTime = null
    ),
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
        id = 2,
        bookingCode = "PKL-20260928-00118",
        facilityId = 1,
        facilityName = "Smash Pickle Club",
        facilityLocation = "Quezon City",
        courtId = 1,
        courtName = "Court 1",
        dateIso = "2026-09-28",
        dateLabel = "September 28, 2026",
        timeSlot = "10:00 AM",
        timeRangeLabel = "10:00 AM - 11:00 AM",
        playerName = "Marco V.",
        courtFee = 250,
        discountAmount = 0,
        serviceFee = 20,
        totalAmount = 270,
        paymentMethod = "Maya",
        paymentStatus = "PAID",
        status = "CHECKED_IN",
        checkInTime = "9:48 AM"
    ),
    BookingEntity(
        id = 3,
        bookingCode = "PKL-20260928-00119",
        facilityId = 1,
        facilityName = "Smash Pickle Club",
        facilityLocation = "Quezon City",
        courtId = 1,
        courtName = "Court 1",
        dateIso = "2026-09-28",
        dateLabel = "September 28, 2026",
        timeSlot = "01:00 PM",
        timeRangeLabel = "1:00 PM - 2:00 PM",
        playerName = "Bea R.",
        courtFee = 250,
        discountAmount = 0,
        serviceFee = 20,
        totalAmount = 270,
        paymentMethod = "GCash",
        paymentStatus = "PAID",
        status = "CONFIRMED",
        checkInTime = null
    )
)

data class ProShopCounterItem(
    val id: String,
    val name: String,
    val category: String,
    val price: Int
)

private val DefaultProShopItems = listOf(
    ProShopCounterItem("paddle", "Carbon Paddle Rental", "Rental", 100),
    ProShopCounterItem("balls", "Pro Pickleballs (3-Pack)", "Pro Shop", 150),
    ProShopCounterItem("hydration", "Court Towel + Hydration", "Refreshment", 120),
    ProShopCounterItem("coach", "1-Hr Coach Clinic Add-On", "Coaching", 500)
)

/**
 * Cashier Dashboard Screen (POS Terminal, Walk-In Court Reservations, Pay-at-Venue Collection,
 * Pro Shop Add-Ons, Player Check-In, and Official Receipts).
 */
@Composable
fun CashierDashboardScreen(
    modifier: Modifier = Modifier,
    facility: FacilityEntity? = null,
    courts: List<CourtEntity> = DefaultDashboardCourts,
    bookings: List<BookingEntity> = DefaultCashierBookings,
    clubProducts: List<ClubProductItem> = emptyList(),
    businessSettings: BusinessPaymentSettings = BusinessPaymentSettings(),
    onCollectPayment: (BookingEntity, String) -> Unit = { _, _ -> },
    onVerifyPayment: (BookingEntity) -> Unit = {},
    onCheckInBooking: (BookingEntity) -> Unit = {},
    onCreateWalkInBooking: (CourtEntity, String, String, String, Int) -> Unit = { _, _, _, _, _ -> },
    onOpenAdminDashboard: (() -> Unit)? = null,
    onOpenCustomerDashboard: (() -> Unit)? = null
) {
    val proShopCounterItems = remember(clubProducts) {
        if (clubProducts.isNotEmpty()) {
            clubProducts.map { ProShopCounterItem(it.id, it.name, it.category, it.price) }
        } else {
            DefaultProShopItems
        }
    }
    var localBookings by remember {
        mutableStateOf(if (bookings.isNotEmpty()) bookings else DefaultCashierBookings)
    }
    LaunchedEffect(bookings) {
        if (bookings.isNotEmpty()) {
            localBookings = bookings
        }
    }

    var posFilter by remember { mutableStateOf("ALL") } // "ALL", "PENDING", "PAID", "CHECKED_IN"
    var proShopExtraSales by remember { mutableIntStateOf(0) }
    var lastProShopReceipt by remember { mutableStateOf<String?>(null) }

    var showWalkInModal by remember { mutableStateOf(false) }
    var showQrScannerModal by remember { mutableStateOf(false) }
    var lastQrScanOutcome by remember { mutableStateOf<QrPassScanOutcome?>(null) }
    var paymentModalBooking by remember { mutableStateOf<BookingEntity?>(null) }
    var receiptModalBooking by remember { mutableStateOf<BookingEntity?>(null) }

    val handleQrCodeScan: (String) -> Unit = { rawCode ->
        val outcome = resolveQrPassScan(rawCode, localBookings)
        lastQrScanOutcome = outcome
        if (outcome is QrPassScanOutcome.CheckedInSuccess) {
            val targetId = outcome.booking.id
            localBookings = localBookings.map {
                if (it.id == targetId) {
                    it.copy(
                        status = "CHECKED_IN",
                        paymentStatus = "PAID",
                        checkInTime = outcome.checkInTime
                    )
                } else {
                    it
                }
            }
            onCheckInBooking(outcome.booking)
        }
    }

    val handleQuickBookingPassScan: (BookingEntity) -> Unit = { passBooking ->
        handleQrCodeScan(passBooking.bookingCode)
    }

    val handleVerifyAndCheckInPending: (BookingEntity) -> Unit = { pending ->
        val isWalkInCash = pending.isWalkIn || pending.paymentMethod.equals("Cash on Hand", ignoreCase = true)
        val verifiedBooking = pending.copy(
            status = "CHECKED_IN",
            paymentStatus = "PAID",
            paymentMethod = if (isWalkInCash) "Cash on Hand" else pending.paymentMethod,
            cashierVerifiedAt = "Verified Just Now",
            checkInTime = "1:47 PM"
        )
        localBookings = localBookings.map {
            if (it.id == pending.id) verifiedBooking else it
        }
        onVerifyPayment(pending)
        onCheckInBooking(verifiedBooking)
        lastQrScanOutcome = QrPassScanOutcome.CheckedInSuccess(
            booking = verifiedBooking,
            checkInTime = "1:47 PM"
        )
    }

    val activeBookings = localBookings.filter { it.status != "CANCELLED" }
    val pendingVenueBookings = activeBookings.filter {
        it.status == "PENDING_CASHIER_VERIFICATION" ||
            it.paymentStatus == "FOR_VERIFICATION" ||
            it.paymentStatus == "CASH_ON_HAND_AT_CASHIER" ||
            it.paymentStatus == "PENDING_VENUE"
    }
    val paidBookings = activeBookings.filter { it.paymentStatus == "PAID" }

    val collectedFromBookings = paidBookings.sumOf { it.totalAmount }
    val totalPosCollected = 17220 + collectedFromBookings + proShopExtraSales
    val cashDrawerTotal = 6850 + proShopExtraSales +
        paidBookings.filter {
            it.paymentMethod.equals("Cash", ignoreCase = true) ||
                it.paymentMethod.equals("Cash on Hand", ignoreCase = true)
        }.sumOf { it.totalAmount }
    val eWalletTotal = 7170 +
        paidBookings.filter {
            it.paymentMethod.equals("GCash", ignoreCase = true) ||
                it.paymentMethod.equals("Maya", ignoreCase = true) ||
                it.paymentMethod.equals("PayMaya", ignoreCase = true)
        }.sumOf { it.totalAmount }
    val pendingVenueTotal = pendingVenueBookings.sumOf { it.totalAmount }

    val filteredBookings = remember(activeBookings, posFilter) {
        when (posFilter) {
            "PENDING" -> pendingVenueBookings
            "PAID" -> paidBookings
            "CHECKED_IN" -> activeBookings.filter { it.status == "CHECKED_IN" }
            else -> activeBookings.sortedByDescending {
                it.status == "PENDING_CASHIER_VERIFICATION" || it.paymentStatus == "PENDING_VENUE"
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("cashier_dashboard_screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Cashier Header & Role Quick Switch
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
                                text = "CASHIER POS TERMINAL • SHIFT #2",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Cashier Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${facility?.name ?: "Smash Pickle Club"} • Walk-In Counter, Collections & Receipts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showQrScannerModal = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldDark,
                                contentColor = OpticVolt
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("cashier_open_qr_scanner_button")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan QR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showWalkInModal = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OpticVolt,
                                contentColor = OpticVoltDarkText
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("cashier_new_walkin_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Walk-In POS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2. Cashier Drawer & POS Collections Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("cashier_metrics_hero_card"),
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
                            Surface(
                                color = OpticVolt.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PointOfSale,
                                        contentDescription = null,
                                        tint = OpticVolt,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "POS Collections Today",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OpticVolt
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "₱%,d".format(totalPosCollected),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp,
                                color = Color.White,
                                modifier = Modifier.testTag("cashier_total_collected")
                            )
                            Text(
                                text = "${paidBookings.size} Paid Receipts • ${pendingVenueBookings.size} Pending at Counter",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD6F5E6)
                            )
                        }

                        Surface(
                            color = if (pendingVenueTotal > 0) PeakAmber.copy(alpha = 0.2f) else AvailableGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (pendingVenueTotal > 0) PeakAmber else AvailableGreen)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TO COLLECT",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pendingVenueTotal > 0) PeakAmber else OpticVolt
                                )
                                Text(
                                    text = "₱%,d".format(pendingVenueTotal),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    modifier = Modifier.testTag("cashier_pending_venue_amount")
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
                        CashierDrawerMetric("Cash Drawer", "₱%,d".format(cashDrawerTotal), "On Hand")
                        CashierDrawerMetric("GCash / Maya", "₱%,d".format(eWalletTotal), "Verified QR")
                        CashierDrawerMetric("Card / POS", "₱3,200", "Settled")
                    }
                }
            }
        }

        // 2-QR. Cashier QR Code Pass Scanner & Customer Check-In Station
        item {
            CashierQrScannerStationCard(
                bookings = localBookings,
                lastScanOutcome = lastQrScanOutcome,
                onScanQrCodeString = handleQrCodeScan,
                onQuickScanBookingPass = handleQuickBookingPassScan,
                onVerifyAndCheckInPending = handleVerifyAndCheckInPending,
                onOpenFullScannerModal = { showQrScannerModal = true },
                onOpenReceipt = { booking -> receiptModalBooking = booking }
            )
        }

        // 2a. Cashier Payment Receipt & Walk-In Cash Verification Queue (Issues Paid Court QR Pass to Customer)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("cashier_verification_queue_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (pendingVenueBookings.isNotEmpty()) PeakAmber else AvailableGreen
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Payment & Walk-In Verification Queue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Verify uploaded receipts + reference numbers or collect Walk-In Cash on Hand to unlock customer QR Passes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = if (pendingVenueBookings.isNotEmpty()) PeakAmberBg else AvailableGreenBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${pendingVenueBookings.size} TO VERIFY",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingVenueBookings.isNotEmpty()) PeakAmber else AvailableGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    if (pendingVenueBookings.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AvailableGreenBg.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✓ All customer payment receipts and walk-in cash payments have been verified.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AvailableGreen,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        pendingVenueBookings.forEach { pending ->
                            val isWalkInCash = pending.isWalkIn || pending.paymentMethod.equals("Cash on Hand", ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isWalkInCash) Color(0xFFFFFBEB) else Color(0xFFEFF6FF),
                                border = BorderStroke(
                                    1.dp,
                                    if (isWalkInCash) Color(0xFFFDE68A) else Color(0xFF93C5FD)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cashier_verify_row_${pending.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isWalkInCash) Color(0xFFFEF3C7) else Color(0xFFDBEAFE)
                                        ) {
                                            Text(
                                                text = if (isWalkInCash) {
                                                    "WALK-IN • CASH ON HAND AT CASHIER"
                                                } else {
                                                    "ONLINE PAYMENT • RECEIPT & REF # UPLOADED"
                                                },
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isWalkInCash) Color(0xFFB45309) else Color(0xFF1D4ED8),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = "₱${pending.totalAmount}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = EmeraldDark
                                        )
                                    }

                                    Text(
                                        text = "${pending.playerName} • ${pending.courtName} (${pending.dateLabel} • ${pending.timeRangeLabel})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )

                                    if (!isWalkInCash) {
                                        Text(
                                            text = "Method: ${pending.paymentMethod} • Ref #: ${pending.referenceNumber ?: "Submitted"} • Receipt: ${pending.receiptFileName ?: "screenshot.png"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E40AF)
                                        )
                                    } else {
                                        Text(
                                            text = "Mode of Payment: Cash on Hand at Cashier Counter (#${pending.bookingCode})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF92400E)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            localBookings = localBookings.map {
                                                if (it.id == pending.id) {
                                                    it.copy(
                                                        status = "CONFIRMED",
                                                        paymentStatus = "PAID",
                                                        paymentMethod = if (isWalkInCash) "Cash on Hand" else it.paymentMethod,
                                                        cashierVerifiedAt = "Verified Just Now"
                                                    )
                                                } else {
                                                    it
                                                }
                                            }
                                            onVerifyPayment(pending)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AvailableGreen,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("cashier_verify_payment_${pending.id}")
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isWalkInCash) {
                                                "Collect Cash on Hand & Issue Paid QR Pass"
                                            } else {
                                                "Verify Receipt & Issue Paid QR Pass"
                                            },
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Pro Shop & Rental Quick-Ring Counter
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Pro Shop & Equipment Rental Counter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "1-tap ring up for paddles, balls, and hydration",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                lastProShopReceipt?.let { receiptMsg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = AvailableGreenBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("cashier_proshop_receipt_banner")
                    ) {
                        Text(
                            text = receiptMsg,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AvailableGreen,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(proShopCounterItems, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .width(176.dp)
                                .testTag("cashier_proshop_item_${item.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = item.category.uppercase(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "₱${item.price}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = EmeraldDark
                                    )
                                    Button(
                                        onClick = {
                                            proShopExtraSales += item.price
                                            lastProShopReceipt = "✓ Rung up ${item.name} (+₱${item.price} Cash Drawer)"
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("cashier_ring_up_${item.id}")
                                    ) {
                                        Text("Ring Up", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Bookings & Payment Collection Queue Header + Filter Chips
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Counter Collections & Check-In Queue",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Collect Pay-at-Venue fees, check in arriving players, and issue official POS receipts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "ALL" to "All (${activeBookings.size})",
                        "PENDING" to "Pay at Venue (${pendingVenueBookings.size})",
                        "PAID" to "Paid (${paidBookings.size})",
                        "CHECKED_IN" to "Checked In"
                    ).forEach { (key, label) ->
                        val selected = posFilter == key
                        FilterChip(
                            selected = selected,
                            onClick = { posFilter = key },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("cashier_filter_$key")
                        )
                    }
                }
            }
        }

        // 5. Bookings Transaction Cards
        items(filteredBookings, key = { it.id }) { booking ->
            val isPendingVenue = booking.paymentStatus == "PENDING_VENUE"
            val isCheckedIn = booking.status == "CHECKED_IN"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("cashier_booking_card_${booking.id}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = if (isPendingVenue) 1.5.dp else 1.dp,
                    color = when {
                        isPendingVenue -> PeakAmber
                        isCheckedIn -> AvailableGreen
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (isPendingVenue) PeakAmberBg else AvailableGreenBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isPendingVenue) "COLLECT AT COUNTER • ₱${booking.totalAmount}" else "PAID • ${booking.paymentMethod.uppercase()}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPendingVenue) PeakAmber else AvailableGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = booking.bookingCode,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                text = booking.playerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${booking.courtName} • ${booking.timeRangeLabel} • ${booking.dateLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "₱${booking.totalAmount}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = EmeraldDark
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isPendingVenue) {
                            Button(
                                onClick = { paymentModalBooking = booking },
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("cashier_collect_payment_${booking.id}"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OpticVolt,
                                    contentColor = OpticVoltDarkText
                                )
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Collect ₱${booking.totalAmount}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (!isCheckedIn) {
                            Button(
                                onClick = {
                                    localBookings = localBookings.map {
                                        if (it.id == booking.id) {
                                            it.copy(status = "CHECKED_IN", checkInTime = "1:55 PM")
                                        } else {
                                            it
                                        }
                                    }
                                    onCheckInBooking(booking)
                                },
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("cashier_checkin_${booking.id}"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check In Player", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                color = AvailableGreenBg,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AvailableGreen,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Checked In (${booking.checkInTime ?: "On Court"})",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AvailableGreen
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { receiptModalBooking = booking },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cashier_receipt_${booking.id}"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Receipt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Collect Payment Modal
    paymentModalBooking?.let { booking ->
        var selectedMethod by remember { mutableStateOf("Cash") }
        AlertDialog(
            onDismissRequest = { paymentModalBooking = null },
            title = { Text("Collect Counter Payment • ₱${booking.totalAmount}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${booking.playerName} • ${booking.courtName} (${booking.timeRangeLabel})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select payment method received at the cashier counter:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf("Cash", "GCash", "PayMaya").forEach { method ->
                        val isSelected = selectedMethod == method
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedMethod = method }
                                .testTag("cashier_pay_method_$method"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldDark else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) OpticVolt else MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = method,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Text(
                                        text = "SELECTED",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OpticVolt
                                    )
                                }
                            }
                        }
                    }
                    if (selectedMethod == "GCash" || selectedMethod == "PayMaya") {
                        PaymentQrCodeDisplayCard(
                            paymentMethod = selectedMethod,
                            amount = booking.totalAmount,
                            businessSettings = businessSettings,
                            tagPrefix = "cashier_collect"
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        localBookings = localBookings.map {
                            if (it.id == booking.id) {
                                it.copy(
                                    paymentMethod = selectedMethod,
                                    paymentStatus = "PAID",
                                    status = "CHECKED_IN",
                                    checkInTime = "2:05 PM"
                                )
                            } else {
                                it
                            }
                        }
                        onCollectPayment(booking, selectedMethod)
                        paymentModalBooking = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("cashier_confirm_collection_button")
                ) {
                    Text("Confirm & Check In")
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentModalBooking = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Walk-In POS Booking Modal
    if (showWalkInModal) {
        val bookableCourts = courts.filter { it.status.equals("ACTIVE", ignoreCase = true) }.ifEmpty { DefaultDashboardCourts }
        var selectedCourt by remember { mutableStateOf(bookableCourts.first()) }
        var guestName by remember { mutableStateOf("Walk-In Player") }
        var selectedSlot by remember { mutableStateOf("5:00 PM - 6:00 PM") }
        var paymentMethod by remember { mutableStateOf("Cash on Hand") }
        var includePaddleRental by remember { mutableStateOf(false) }

        val extraFee = if (includePaddleRental) 100 else 0
        val totalWalkIn = selectedCourt.middayPrice + extraFee

        AlertDialog(
            onDismissRequest = { showWalkInModal = false },
            title = { Text("New Walk-In Court POS Booking") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = guestName,
                        onValueChange = { guestName = it },
                        label = { Text("Customer / Walk-In Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cashier_walkin_name_input")
                    )

                    Text("Select Active Court:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(bookableCourts, key = { it.id }) { c ->
                            FilterChip(
                                selected = selectedCourt.id == c.id,
                                onClick = { selectedCourt = c },
                                label = { Text("${c.name} (₱${c.middayPrice})") }
                            )
                        }
                    }

                    Text("Walk-In Payment Mode (Cash on Hand Required):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Cash on Hand", "GCash", "PayMaya").forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method, fontSize = 11.sp) }
                            )
                        }
                    }
                    if (paymentMethod == "GCash" || paymentMethod == "PayMaya") {
                        PaymentQrCodeDisplayCard(
                            paymentMethod = paymentMethod,
                            amount = totalWalkIn,
                            businessSettings = businessSettings,
                            tagPrefix = "cashier_walkin"
                        )
                    }

                    FilterChip(
                        selected = includePaddleRental,
                        onClick = { includePaddleRental = !includePaddleRental },
                        label = { Text("+ Carbon Paddle Rental (₱100)") }
                    )

                    Surface(
                        color = EmeraldDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Due Now", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                text = "₱$totalWalkIn",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = OpticVolt
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newBooking = BookingEntity(
                            id = (localBookings.maxOfOrNull { it.id } ?: 10) + 1,
                            bookingCode = "POS-20260928-${130 + localBookings.size}",
                            facilityId = facility?.id ?: 1,
                            facilityName = facility?.name ?: "Smash Pickle Club",
                            facilityLocation = facility?.city ?: "Quezon City",
                            courtId = selectedCourt.id,
                            courtName = selectedCourt.name,
                            dateIso = "2026-09-28",
                            dateLabel = "September 28, 2026",
                            timeSlot = "05:00 PM",
                            timeRangeLabel = selectedSlot,
                            playerName = guestName.ifBlank { "Walk-In Guest" },
                            courtFee = selectedCourt.middayPrice,
                            discountAmount = 0,
                            serviceFee = extraFee,
                            totalAmount = totalWalkIn,
                            paymentMethod = paymentMethod,
                            paymentStatus = "PAID",
                            status = "CHECKED_IN",
                            checkInTime = "Just now"
                        )
                        localBookings = listOf(newBooking) + localBookings
                        onCreateWalkInBooking(selectedCourt, selectedSlot, guestName, paymentMethod, extraFee)
                        showWalkInModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("cashier_confirm_walkin_button")
                ) {
                    Text("Collect & Issue Pass")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWalkInModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Official POS Receipt Modal
    receiptModalBooking?.let { booking ->
        AlertDialog(
            onDismissRequest = { receiptModalBooking = null },
            title = {
                Text(
                    text = "Official POS Receipt • OR-${booking.id}0928",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = facility?.name ?: "Smash Pickle Club",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Booking Ref: ${booking.bookingCode}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    ReceiptLine("Customer", booking.playerName)
                    ReceiptLine("Court & Time", "${booking.courtName} (${booking.timeRangeLabel})")
                    ReceiptLine("Court Fee", "₱${booking.courtFee}")
                    ReceiptLine("Service / Add-Ons", "₱${booking.serviceFee}")
                    if (booking.discountAmount > 0) {
                        ReceiptLine("Member Discount", "-₱${booking.discountAmount}")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    ReceiptLine("Total Amount", "₱${booking.totalAmount}", bold = true)
                    ReceiptLine("Payment Method", "${booking.paymentMethod} (${booking.paymentStatus})")
                }
            },
            confirmButton = {
                Button(
                    onClick = { receiptModalBooking = null },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // Cashier QR Code Pass Scanner Modal (Customer Scans QR Pass to Check In)
    if (showQrScannerModal) {
        CashierQrCodeScannerDialog(
            bookings = localBookings,
            lastScanOutcome = lastQrScanOutcome,
            onScanCode = handleQrCodeScan,
            onScanBookingPass = handleQuickBookingPassScan,
            onVerifyAndCheckInPending = handleVerifyAndCheckInPending,
            onDismiss = { showQrScannerModal = false }
        )
    }
}

@Composable
private fun CashierDrawerMetric(label: String, value: String, subtext: String) {
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
            fontSize = 15.sp,
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

@Composable
private fun ReceiptLine(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}
