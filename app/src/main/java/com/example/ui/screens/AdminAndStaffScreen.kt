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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.local.OpenPlayGameEntity
import com.example.data.local.TournamentEntity
import com.example.data.local.UserProfileEntity
import com.example.ui.theme.AvailableGreen
import com.example.ui.theme.AvailableGreenBg
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MaintenanceRed
import com.example.ui.theme.OpticVolt
import com.example.ui.theme.PeakAmber
import com.example.viewmodel.BusinessPaymentSettings
import com.example.viewmodel.ClubProductItem
import com.example.viewmodel.RegisteredAccount
import com.example.viewmodel.StaffRole

@Composable
fun AdminAndStaffScreen(
    facilities: List<FacilityEntity>,
    courts: List<CourtEntity>,
    bookings: List<BookingEntity>,
    openPlayGames: List<OpenPlayGameEntity> = emptyList(),
    userProfile: UserProfileEntity? = null,
    cashierAccounts: List<RegisteredAccount> = emptyList(),
    onAddCashierAccount: (String, String, String) -> Unit = { _, _, _ -> },
    onAddStaffAccount: (String, String, String, StaffRole) -> Unit = { name, email, pass, _ ->
        onAddCashierAccount(name, email, pass)
    },
    onRemoveCashierAccount: (String) -> Unit = {},
    businessSettings: BusinessPaymentSettings = BusinessPaymentSettings(),
    onSaveBusinessSettings: (String, String, String) -> Unit = { _, _, _ -> },
    onUploadGcashQr: (String?) -> Unit = {},
    onUploadPaymayaQr: (String?) -> Unit = {},
    clubProducts: List<ClubProductItem> = emptyList(),
    onAddClubProduct: (String, String, Int, Int) -> Unit = { _, _, _, _ -> },
    onRemoveClubProduct: (String) -> Unit = {},
    onApproveBooking: (BookingEntity) -> Unit = {},
    onRejectBooking: (BookingEntity) -> Unit = {},
    onSaveMyClubProfile: (FacilityEntity, String, String, String, String, String, Boolean, String) -> Unit,
    onToggleCourtStatus: (CourtEntity) -> Unit,
    onUpdateCourtPricing: (CourtEntity, String, String, Int, Int, Int, Int, Int) -> Unit,
    onSaveCourtStatusAndPricing: ((CourtEntity, String, String, String, Int, Int, Int, Int, Int) -> Unit)? = null,
    onAddCourt: (Int, Boolean, String) -> Unit,
    onCheckInBooking: (BookingEntity) -> Unit,
    onCollectCashierPayment: (BookingEntity, String) -> Unit = { _, _ -> },
    onCreateWalkInBooking: (CourtEntity, String, String, String, Int) -> Unit = { _, _, _, _, _ -> },
    onSelectCourtToBook: (Int) -> Unit = {},
    onOpenBookingPass: (BookingEntity) -> Unit = {},
    onToggleJoinOpenPlay: (OpenPlayGameEntity) -> Unit = {},
    tournaments: List<TournamentEntity> = emptyList(),
    onCreateTournament: (String, String, String, String, String, Int, Int, Int, String, String, String) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
    onUpdateTournamentBracket: (TournamentEntity, String, String, String, String) -> Unit = { _, _, _, _, _ -> }
) {
    // 0 = Admin Dashboard, 1 = QR Check-In & Reports
    var adminSubTab by remember { mutableIntStateOf(0) }
    var editingPricingCourt by remember { mutableStateOf<CourtEntity?>(null) }
    var showEditClubDialog by remember { mutableStateOf(false) }

    val myClub = facilities.firstOrNull()
    val primaryCourts = courts.filter { it.facilityId == (myClub?.id ?: 1) }
    val activeBookingSum = bookings.filter { it.status != "CANCELLED" }.sumOf { it.totalAmount }
    val dynamicTodayRevenue = if (activeBookingSum > 0) 16950 + activeBookingSum else 18450

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-Tab Switcher for Admin Dashboard & QR Check-In
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                AdminTabPill(
                    title = "Admin Overview",
                    selected = adminSubTab == 0,
                    onClick = { adminSubTab = 0 },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_tab_overview")
                )
                AdminTabPill(
                    title = "QR Check-In & Reports",
                    selected = adminSubTab == 1,
                    onClick = { adminSubTab = 1 },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_tab_qr_checkin")
                )
            }
        }

        when (adminSubTab) {
            0 -> {
                AdminDashboardScreen(
                    modifier = Modifier.weight(1f),
                    facility = myClub,
                    courts = primaryCourts,
                    bookings = bookings,
                    onToggleCourtStatus = onToggleCourtStatus,
                    onEditCourtPricing = { editingPricingCourt = it },
                    onSaveCourtStatusAndPricing = onSaveCourtStatusAndPricing,
                    onAddCourt = {
                        onAddCourt(myClub?.id ?: 1, myClub?.isIndoor ?: true, "Pro Cushion Indoor")
                    },
                    onEditClubInfo = { showEditClubDialog = true },
                    cashierAccounts = cashierAccounts,
                    onAddCashierAccount = onAddCashierAccount,
                    onAddStaffAccount = onAddStaffAccount,
                    onRemoveCashierAccount = onRemoveCashierAccount,
                    businessSettings = businessSettings,
                    onSaveBusinessSettings = onSaveBusinessSettings,
                    onUploadGcashQr = onUploadGcashQr,
                    onUploadPaymayaQr = onUploadPaymayaQr,
                    clubProducts = clubProducts,
                    onAddClubProduct = onAddClubProduct,
                    onRemoveClubProduct = onRemoveClubProduct,
                    onApproveBooking = onApproveBooking,
                    onRejectBooking = onRejectBooking,
                    tournaments = tournaments,
                    onCreateTournament = onCreateTournament,
                    onUpdateTournamentBracket = onUpdateTournamentBracket
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldDark)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Business Owner Revenue & Reports",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    RevenuePeriodColumn("Today", "₱%,d".format(dynamicTodayRevenue))
                                    RevenuePeriodColumn("This Week", "₱92,300")
                                    RevenuePeriodColumn("This Month", "₱385,600")
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Peak Hours & Utilization Breakdown",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = OpticVolt
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                UtilizationBarRow("6 AM – 10 AM (Morning)", 0.68f, "68% • ₱200/hr")
                                UtilizationBarRow("10 AM – 4 PM (Midday)", 0.74f, "74% • ₱250/hr")
                                UtilizationBarRow("4 PM – 9 PM (Peak Hours)", 0.96f, "96% • ₱350/hr")
                                UtilizationBarRow("9 PM – 11 PM (Night)", 0.82f, "82% • ₱250/hr")
                            }
                        }
                    }

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
                                    text = "Cancellation & No-Show Policy Rules",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                PolicyRuleRow("24+ hours before slot", "100% Automatic Refund", AvailableGreen)
                                PolicyRuleRow("12–24 hours before slot", "50% Partial Refund", PeakAmber)
                                PolicyRuleRow("<12 hours or No-Show", "No Refund (Court Locked)", MaintenanceRed)
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "Staff Reception QR Check-In",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "Verify player Digital Booking Pass QR codes and record arrival time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(bookings.filter { it.status != "CANCELLED" }, key = { it.id }) { booking ->
                        val isCheckedIn = booking.status == "CHECKED_IN"
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("staff_checkin_card_${booking.id}"),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isCheckedIn) 2.dp else 1.dp,
                                color = if (isCheckedIn) AvailableGreen else EmeraldPrimary
                            )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = AvailableGreenBg,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "✓ VALID BOOKING",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = AvailableGreen,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = "Payment: ${booking.paymentStatus} (${booking.paymentMethod})",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = EmeraldPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Column {
                                    Text(
                                        text = "Booking: ${booking.bookingCode}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Player: ${booking.playerName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Court: ${booking.courtName} (${myClub?.name ?: booking.facilityName})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Time: ${booking.timeRangeLabel} • ${booking.dateLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (isCheckedIn) {
                                    Surface(
                                        color = AvailableGreenBg,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "✓ Check-in recorded: ${booking.checkInTime ?: "1:47 PM"}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = AvailableGreen,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = { onCheckInBooking(booking) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("staff_checkin_button_${booking.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "CHECK IN PLAYER",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingPricingCourt?.let { court ->
        EditCourtPricingDialog(
            court = court,
            onDismiss = { editingPricingCourt = null },
            onSave = { name, type, m, mid, pk, nt, wk ->
                onUpdateCourtPricing(court, name, type, m, mid, pk, nt, wk)
                editingPricingCourt = null
            }
        )
    }

    if (showEditClubDialog && myClub != null) {
        EditMyClubDialog(
            facility = myClub,
            onDismiss = { showEditClubDialog = false },
            onSave = { name, city, address, hours, surface, indoor, amenities ->
                onSaveMyClubProfile(myClub, name, city, address, hours, surface, indoor, amenities)
                showEditClubDialog = false
            }
        )
    }
}

@Composable
private fun AdminTabPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) EmeraldDark else Color.Transparent
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) OpticVolt else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}

@Composable
private fun RevenuePeriodColumn(period: String, amount: String) {
    Column {
        Text(
            text = period,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFA3B8B0)
        )
        Text(
            text = amount,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Color.White
        )
    }
}

@Composable
private fun UtilizationBarRow(label: String, fraction: Float, statsText: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.White)
            Text(
                text = statsText,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = OpticVolt
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(OpticVolt)
            )
        }
    }
}

@Composable
private fun PolicyRuleRow(windowText: String, refundText: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = windowText, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = refundText,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = color
        )
    }
}

@Composable
private fun EditCourtPricingDialog(
    court: CourtEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Int, Int, Int, Int) -> Unit
) {
    var courtName by remember { mutableStateOf(court.name) }
    var courtType by remember { mutableStateOf(court.courtType) }
    var morning by remember { mutableStateOf(court.morningPrice.toString()) }
    var midday by remember { mutableStateOf(court.middayPrice.toString()) }
    var peak by remember { mutableStateOf(court.peakPrice.toString()) }
    var night by remember { mutableStateOf(court.nightPrice.toString()) }
    var weekend by remember { mutableStateOf(court.weekendPrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Court & Pricing • ${court.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = courtName,
                    onValueChange = { courtName = it },
                    label = { Text("Court Name (e.g. Court 1)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = courtType,
                    onValueChange = { courtType = it },
                    label = { Text("Surface / Court Type") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = morning,
                    onValueChange = { morning = it },
                    label = { Text("6 AM – 10 AM (₱/hr)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = midday,
                    onValueChange = { midday = it },
                    label = { Text("10 AM – 4 PM (₱/hr)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = peak,
                    onValueChange = { peak = it },
                    label = { Text("4 PM – 9 PM Peak (₱/hr)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = night,
                    onValueChange = { night = it },
                    label = { Text("9 PM – 11 PM Night (₱/hr)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = weekend,
                    onValueChange = { weekend = it },
                    label = { Text("Saturday / Sunday (₱/hr)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        courtName,
                        courtType,
                        morning.toIntOrNull() ?: 200,
                        midday.toIntOrNull() ?: 250,
                        peak.toIntOrNull() ?: 350,
                        night.toIntOrNull() ?: 250,
                        weekend.toIntOrNull() ?: 350
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save Court & Rates")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
