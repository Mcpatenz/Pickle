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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookingEntity
import com.example.data.local.CourtEntity
import com.example.data.local.FacilityEntity
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
import com.example.viewmodel.BookingDateOption
import com.example.viewmodel.CourtOccupancyStatus
import com.example.viewmodel.TimeSlotOption

import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.data.remote.FirestoreReservationRepository
import com.example.ui.components.CourtReservationForm
import com.example.ui.components.CourtReservationFormInput
import com.example.ui.components.ExistingCourtReservationsList
import com.example.ui.components.PaymentQrCodeDisplayCard
import com.example.ui.components.QrCodeMatrixCanvas
import com.example.ui.components.buildBookingFromReservationForm
import com.example.ui.theme.MayaMint
import com.example.ui.theme.MayaMintBg
import com.example.viewmodel.BusinessPaymentSettings
import com.example.viewmodel.BusinessSettingsLocalStore

@Composable
fun CourtsAndBookingScreen(
    facilities: List<FacilityEntity>,
    courts: List<CourtEntity>,
    bookings: List<BookingEntity>,
    selectedCourtId: Int,
    selectedDate: BookingDateOption,
    selectedTimeSlot: TimeSlotOption?,
    dateOptions: List<BookingDateOption>,
    timeSlotOptions: List<TimeSlotOption>,
    lockSecondsRemaining: Int,
    courtBookingTimeLimits: Map<Int, Int> = emptyMap(),
    expiredCourtIds: Set<Int> = emptySet(),
    inUseCourtIds: Set<Int> = setOf(1, 4),
    currentPlayerName: String? = null,
    selectedPaymentMethod: String = "GCash",
    businessSettings: BusinessPaymentSettings = BusinessPaymentSettings(),
    onSelectPaymentMethod: (String) -> Unit = {},
    onOpenBookingPass: (BookingEntity) -> Unit = {},
    onExpireCourtTimeLimit: (Int) -> Unit = {},
    onSelectDate: (BookingDateOption) -> Unit,
    onSelectCourt: (Int) -> Unit,
    onSelectTimeSlot: (TimeSlotOption) -> Unit,
    calculateSlotPrice: (CourtEntity?, BookingDateOption, TimeSlotOption?) -> Int,
    onOpenCheckoutSheet: () -> Unit,
    onSubmitReservationForm: ((CourtReservationFormInput) -> Unit)? = null
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FirestoreReservationRepository.ensureFirestoreInstance(context)
    }
    val firestoreReservations by FirestoreReservationRepository.reservationsFlow.collectAsState()
    var localSubmittedBookings by remember { mutableStateOf<List<BookingEntity>>(emptyList()) }
    val effectiveBookings = remember(bookings, localSubmittedBookings, firestoreReservations) {
        FirestoreReservationRepository.mergeBookingsWithFirestore(
            passedBookings = localSubmittedBookings + bookings,
            firestoreBookings = firestoreReservations
        )
    }
    var activePaymentMethod by remember(selectedPaymentMethod) {
        val initial = when {
            selectedPaymentMethod.equals("Maya", ignoreCase = true) ||
                selectedPaymentMethod.equals("PayMaya", ignoreCase = true) -> "PayMaya"
            selectedPaymentMethod.equals("Pay at Venue", ignoreCase = true) -> "Pay at Venue"
            else -> "GCash"
        }
        mutableStateOf(initial)
    }
    val effectiveBusinessSettings = remember(businessSettings, activePaymentMethod) {
        BusinessSettingsLocalStore.resolveSettings(context, businessSettings)
    }
    val myClub = facilities.firstOrNull()
    val userReservedBooking = remember(effectiveBookings, currentPlayerName) {
        val active = effectiveBookings.filter { it.status != "CANCELLED" }
        if (!currentPlayerName.isNullOrBlank()) {
            active.find { it.playerName.equals(currentPlayerName, ignoreCase = true) }
        } else {
            active.firstOrNull()
        }
    }
    val allClubCourts = courts.filter { it.facilityId == (myClub?.id ?: 1) }.ifEmpty { DefaultClubCourts }

    fun getOccupancy(court: CourtEntity): CourtOccupancyStatus {
        val remaining = courtBookingTimeLimits[court.id] ?: 0
        return when {
            expiredCourtIds.contains(court.id) -> CourtOccupancyStatus.AVAILABLE
            remaining > 0 -> CourtOccupancyStatus.BOOKED
            inUseCourtIds.contains(court.id) -> CourtOccupancyStatus.IN_USE
            effectiveBookings.any {
                it.courtId == court.id &&
                    it.dateIso == selectedDate.isoDate &&
                    it.status == "CHECKED_IN"
            } -> CourtOccupancyStatus.IN_USE
            effectiveBookings.any {
                it.courtId == court.id &&
                    it.dateIso == selectedDate.isoDate &&
                    (it.status == "CONFIRMED" || it.status == "PENDING_ADMIN_APPROVAL" || it.status == "PENDING_CASHIER_PAYMENT") &&
                    (selectedTimeSlot == null || it.timeSlot == selectedTimeSlot.startTime)
            } -> CourtOccupancyStatus.BOOKED
            else -> CourtOccupancyStatus.AVAILABLE
        }
    }

    var courtSearchQuery by remember { mutableStateOf("") }
    var courtFilter by remember { mutableStateOf("All") }
    val filteredCourts = remember(
        allClubCourts,
        courtSearchQuery,
        courtFilter,
        courtBookingTimeLimits,
        expiredCourtIds,
        inUseCourtIds,
        effectiveBookings,
        selectedDate,
        selectedTimeSlot
    ) {
        filterCourtsBySearchAndStatus(
            courts = allClubCourts,
            searchQuery = courtSearchQuery,
            statusFilter = courtFilter,
            statusResolver = ::getOccupancy
        )
    }

    val activeCourt = allClubCourts.find { it.id == selectedCourtId && getOccupancy(it) != CourtOccupancyStatus.BOOKED }
        ?: allClubCourts.firstOrNull { getOccupancy(it) == CourtOccupancyStatus.AVAILABLE }
        ?: allClubCourts.firstOrNull()
    val courtNumberIndex = allClubCourts.indexOf(activeCourt) + 1

    val mins = lockSecondsRemaining / 60
    val secs = lockSecondsRemaining % 60
    val lockCountdown = "%02d:%02d".format(mins, secs)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("courts_booking_list"),
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Club Reservation Header & Facility Summary
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Reserve a Pickleball Court",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Select your date, court, and time slot at ${myClub?.name ?: "Smash Pickle Club"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (myClub != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🏓 ${myClub.name}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "📍 ${myClub.address} • ${myClub.operatingHours}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = EmeraldDark,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${allClubCourts.count { it.status == "ACTIVE" }}/${allClubCourts.size} ACTIVE",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OpticVolt,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "🏓 ${allClubCourts.size} Courts  •  ❄️ Air-conditioned  •  🚗 Parking  •  🛍️ Pro Shop  •  ☕ Café",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (userReservedBooking != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenBookingPass(userReservedBooking) }
                                .testTag("reserved_user_qr_pass_card"),
                            shape = RoundedCornerShape(16.dp),
                            color = EmeraldDark,
                            border = BorderStroke(1.5.dp, OpticVolt)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "YOUR RESERVED COURT QR PASS",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OpticVolt
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${userReservedBooking.courtName} • ${userReservedBooking.timeRangeLabel}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Reserved by ${userReservedBooking.playerName} • ${userReservedBooking.paymentMethod}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD6F5E6)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = { onOpenBookingPass(userReservedBooking) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = OpticVolt,
                                        contentColor = OpticVoltDarkText
                                    ),
                                    modifier = Modifier.testTag("open_reserved_user_qr_pass_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("QR Pass", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 1b. Composable Court Reservation Form (Date, Time Slot, and Court ID)
            item {
                val formInitialDateIso = if (selectedDate.isoDate > "2026-09-28") {
                    selectedDate.isoDate
                } else {
                    "2026-09-29"
                }
                CourtReservationForm(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    facility = myClub,
                    courts = allClubCourts,
                    bookings = effectiveBookings,
                    dateOptions = dateOptions,
                    timeSlotOptions = timeSlotOptions,
                    initialDateIso = formInitialDateIso,
                    initialTimeSlot = selectedTimeSlot?.startTime ?: "02:00 PM",
                    initialCourtId = activeCourt?.id ?: selectedCourtId,
                    initialPlayerName = currentPlayerName ?: "Jonel P.",
                    initialPaymentMethod = activePaymentMethod,
                    onDateChanged = { onSelectDate(it) },
                    onTimeSlotChanged = { onSelectTimeSlot(it) },
                    onCourtIdChanged = { newCourtId -> onSelectCourt(newCourtId) },
                    onPaymentMethodChanged = { method ->
                        activePaymentMethod = method
                        onSelectPaymentMethod(method)
                    },
                    onSubmitReservation = { formInput ->
                        val createdBooking = buildBookingFromReservationForm(
                            input = formInput,
                            facility = myClub,
                            newId = (effectiveBookings.maxOfOrNull { it.id } ?: 100) + 1
                        )
                        localSubmittedBookings = listOf(createdBooking) + localSubmittedBookings
                        dateOptions.find { it.isoDate.equals(formInput.dateIso, ignoreCase = true) }?.let {
                            onSelectDate(it)
                        }
                        timeSlotOptions.find { it.startTime.equals(formInput.timeSlot, ignoreCase = true) }?.let {
                            onSelectTimeSlot(it)
                        }
                        onSelectPaymentMethod(formInput.paymentMethod)
                        if (onSubmitReservationForm != null) {
                            onSubmitReservationForm(formInput)
                        } else {
                            onOpenCheckoutSheet()
                        }
                    }
                )
            }

            // 1c. Existing Court Reservations List (Persisted in Firebase Firestore with 7-Day Filter & Cancel)
            item {
                ExistingCourtReservationsList(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    reservations = effectiveBookings,
                    includeFirestorePersisted = true,
                    onSelectReservation = { booking ->
                        onSelectCourt(booking.courtId)
                        dateOptions.find { it.isoDate.equals(booking.dateIso, ignoreCase = true) }?.let {
                            onSelectDate(it)
                        }
                        timeSlotOptions.find { it.startTime.equals(booking.timeSlot, ignoreCase = true) }?.let {
                            onSelectTimeSlot(it)
                        }
                    },
                    onOpenQrPass = { booking -> onOpenBookingPass(booking) },
                    onCancelReservation = { cancelledBooking ->
                        localSubmittedBookings = localSubmittedBookings.filterNot {
                            it.bookingCode.equals(cancelledBooking.bookingCode, ignoreCase = true) ||
                                it.id == cancelledBooking.id
                        }
                        FirestoreReservationRepository.removeReservationFromFirestore(
                            context = context,
                            bookingCode = cancelledBooking.bookingCode,
                            bookingId = cancelledBooking.id
                        )
                    }
                )
            }

            // 2. Date Strip Selector (September 28: Sun 27, Mon 28, Tue 29, Wed 30, Thu 1, Fri 2, Sat 3)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Select Date",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedDate.fullLabel,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dateOptions, key = { it.isoDate }) { dateItem ->
                            val isSelected = dateItem.isoDate == selectedDate.isoDate
                            Surface(
                                modifier = Modifier
                                    .width(62.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectDate(dateItem) }
                                    .testTag("date_chip_${dateItem.dayNumber}"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = dateItem.dayShort,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) Color(0xFFD6F5E6) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dateItem.dayNumber,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Court Selector (Court 1 .. Court 6)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "2. Select Your Court",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CourtSearchAndFilterBar(
                        searchQuery = courtSearchQuery,
                        onSearchQueryChange = { courtSearchQuery = it },
                        selectedFilter = courtFilter,
                        onFilterSelected = { courtFilter = it },
                        allCount = allClubCourts.size,
                        availableCount = allClubCourts.count { getOccupancy(it) == CourtOccupancyStatus.AVAILABLE },
                        bookedCount = allClubCourts.count { getOccupancy(it) == CourtOccupancyStatus.BOOKED },
                        inUseCount = allClubCourts.count { getOccupancy(it) == CourtOccupancyStatus.IN_USE },
                        tagPrefix = "booking_court"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (filteredCourts.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "No courts match filter ($courtFilter)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(
                                    onClick = {
                                        courtSearchQuery = ""
                                        courtFilter = "All"
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCourts, key = { it.id }) { court ->
                            val occupancy = getOccupancy(court)
                            val isBooked = occupancy == CourtOccupancyStatus.BOOKED
                            val isInUse = occupancy == CourtOccupancyStatus.IN_USE
                            val isCourtEnabled = !isBooked
                            val isSelected = court.id == activeCourt?.id && isCourtEnabled
                            val remainingSec = courtBookingTimeLimits[court.id] ?: 0
                            val remainingFormatted = "%02d:%02d".format(remainingSec / 60, remainingSec % 60)

                            val cardBg = when {
                                isBooked -> MaintenanceRedBg
                                isInUse -> AvailableGreenBg
                                isSelected -> GCashBlue
                                else -> GCashBlueBg.copy(alpha = 0.7f)
                            }
                            val borderColor = when {
                                isBooked -> MaintenanceRed
                                isInUse -> AvailableGreen
                                isSelected -> OpticVolt
                                else -> GCashBlue
                            }
                            val primaryTextColor = when {
                                isBooked -> MaintenanceRed
                                isInUse -> AvailableGreen
                                isSelected -> Color.White
                                else -> GCashBlue
                            }
                            val sanitizedCourtType = court.courtType.replace("Championship", "Pro Cushion", ignoreCase = true)

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(enabled = isCourtEnabled) { onSelectCourt(court.id) }
                                    .testTag("court_chip_${court.name.replace(" ", "_")}"),
                                shape = RoundedCornerShape(14.dp),
                                color = cardBg,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.5.dp,
                                    color = borderColor
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = court.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryTextColor
                                    )
                                    Text(
                                        text = sanitizedCourtType,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when (occupancy) {
                                            CourtOccupancyStatus.BOOKED -> if (remainingSec > 0) "BOOKED ($remainingFormatted)" else "ALREADY BOOKED"
                                            CourtOccupancyStatus.IN_USE -> "ALREADY IN USE"
                                            CourtOccupancyStatus.AVAILABLE -> "AVAILABLE"
                                        },
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) OpticVolt else primaryTextColor
                                    )
                                    if (isBooked) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Tap to End Limit",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GCashBlue,
                                            modifier = Modifier
                                                .clickable { onExpireCourtTimeLimit(court.id) }
                                                .padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Dynamic Pricing Engine Tier Strip
            if (activeCourt != null) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Court Rate Schedule (${activeCourt.name} • ${activeCourt.courtType})",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                PricingTierPill("6–10 AM", "₱${activeCourt.morningPrice}")
                                PricingTierPill("10 AM–4 PM", "₱${activeCourt.middayPrice}")
                                PricingTierPill("4–9 PM Peak", "₱${activeCourt.peakPrice}", isPeak = true)
                                PricingTierPill("9–11 PM", "₱${activeCourt.nightPrice}")
                                PricingTierPill("Sat/Sun", "₱${activeCourt.weekendPrice}", isPeak = true)
                            }
                        }
                    }
                }
            }

            // 5. Real-Time Available Times Matrix with Temporary Slot Lock
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. Available Time Slots",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = PeakAmber,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Slot lock: $lockCountdown",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PeakAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (activeCourt?.status == "MAINTENANCE") {
                        Surface(
                            color = MaintenanceRedBg,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaintenanceRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = MaintenanceRed
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${activeCourt.name} is currently under Maintenance",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7F1D1D)
                                    )
                                    Text(
                                        text = "Select another court above, or switch to Admin Mode to set ${activeCourt.name} back to ACTIVE.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF991B1B)
                                    )
                                }
                            }
                        }
                    } else {
                        // 2-Column Grid of Time Slots
                        val chunkedSlots = timeSlotOptions.chunked(2)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            chunkedSlots.forEach { rowSlots ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowSlots.forEach { slot ->
                                        val isBookedInDb = effectiveBookings.any {
                                            it.facilityId == (myClub?.id ?: 1) &&
                                                it.courtId == activeCourt?.id &&
                                                it.dateIso == selectedDate.isoDate &&
                                                it.timeSlot == slot.startTime &&
                                                it.status != "CANCELLED"
                                        }
                                        val isBookedDefault = slot.defaultBookedCourts.contains(courtNumberIndex)
                                        val isBooked = isBookedInDb || isBookedDefault
                                        val isSelected = selectedTimeSlot?.startTime == slot.startTime && !isBooked
                                        val slotPrice = calculateSlotPrice(activeCourt, selectedDate, slot)

                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable(enabled = !isBooked) { onSelectTimeSlot(slot) }
                                                .testTag("timeslot_${slot.startTime.replace(" ", "_")}"),
                                            shape = RoundedCornerShape(14.dp),
                                            color = when {
                                                isBooked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                                isSelected -> EmeraldPrimary
                                                else -> MaterialTheme.colorScheme.surface
                                            },
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = when {
                                                    isBooked -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                    isSelected -> OpticVolt
                                                    else -> MaterialTheme.colorScheme.outline
                                                }
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = slot.startTime,
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = when {
                                                            isBooked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                            isSelected -> Color.White
                                                            else -> MaterialTheme.colorScheme.onSurface
                                                        }
                                                    )
                                                    Text(
                                                        text = "₱$slotPrice",
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = when {
                                                            isBooked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                            isSelected -> OpticVolt
                                                            else -> EmeraldPrimary
                                                        }
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = when {
                                                        isBooked -> "BOOKED"
                                                        isSelected -> "LOCKED ($lockCountdown)"
                                                        else -> "AVAILABLE"
                                                    },
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    color = when {
                                                        isBooked -> MaintenanceRed.copy(alpha = 0.65f)
                                                        isSelected -> OpticVolt
                                                        else -> AvailableGreen
                                                    }
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

            // 6. Payment Method Selection & Stored Business QR Code Display
            item {
                val previewPrice = calculateSlotPrice(activeCourt, selectedDate, selectedTimeSlot) + 20
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "4. Select Payment Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select GCash or PayMaya to display the club's stored payment QR code",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val methods = listOf(
                        Triple("GCash", "GCASH", GCashBlue to GCashBlueBg),
                        Triple("PayMaya", "PAYMAYA", MayaMint to MayaMintBg),
                        Triple("Pay at Venue", "VENUE", EmeraldPrimary to AvailableGreenBg)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        methods.forEach { (name, badge, colors) ->
                            val (accent, bgTint) = colors
                            val isSelected = activePaymentMethod == name
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        activePaymentMethod = name
                                        onSelectPaymentMethod(name)
                                    }
                                    .testTag("booking_payment_option_$badge"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) accent else bgTint.copy(alpha = 0.55f),
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, accent)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else accent
                                    )
                                }
                            }
                        }
                    }

                    if (activePaymentMethod == "GCash" || activePaymentMethod == "PayMaya") {
                        Spacer(modifier = Modifier.height(10.dp))
                        PaymentQrCodeDisplayCard(
                            paymentMethod = activePaymentMethod,
                            amount = previewPrice,
                            businessSettings = effectiveBusinessSettings,
                            tagPrefix = "booking"
                        )
                    }
                }
            }
        }

        // Sticky Bottom Reservation Bar
        if (myClub != null && activeCourt != null && activeCourt.status == "ACTIVE" && selectedTimeSlot != null) {
            val currentPrice = calculateSlotPrice(activeCourt, selectedDate, selectedTimeSlot)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = EmeraldDark,
                tonalElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${activeCourt.name} • ${myClub.name}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedDate.dayShort} ${selectedDate.dayNumber} • ${selectedTimeSlot.rangeLabel} • ₱$currentPrice",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = OpticVolt
                        )
                    }

                    Button(
                        onClick = onOpenCheckoutSheet,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OpticVolt,
                            contentColor = OpticVoltDarkText
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("reserve_court_checkout_button")
                    ) {
                        Text(
                            text = "Reserve Court",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PricingTierPill(
    label: String,
    price: String,
    isPeak: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = price,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (isPeak) PeakAmber else EmeraldPrimary
        )
    }
}

@Composable
fun CourtsOverviewScreen(
    facilities: List<FacilityEntity>,
    courts: List<CourtEntity>,
    bookings: List<BookingEntity>,
    courtBookingTimeLimits: Map<Int, Int>,
    expiredCourtIds: Set<Int>,
    inUseCourtIds: Set<Int>,
    onSelectCourtToBook: (Int) -> Unit,
    onExpireCourtTimeLimit: (Int) -> Unit
) {
    val myClub = facilities.firstOrNull()
    val clubCourts = courts.filter { it.facilityId == (myClub?.id ?: 1) }.ifEmpty {
        courts.ifEmpty { DefaultClubCourts }
    }

    var courtSearchQuery by remember { mutableStateOf("") }
    var courtStatusFilter by remember { mutableStateOf("All") }

    fun resolveOverviewOccupancy(court: CourtEntity): CourtOccupancyStatus {
        val remainingSeconds = courtBookingTimeLimits[court.id] ?: 0
        return when {
            expiredCourtIds.contains(court.id) -> CourtOccupancyStatus.AVAILABLE
            remainingSeconds > 0 -> CourtOccupancyStatus.BOOKED
            inUseCourtIds.contains(court.id) -> CourtOccupancyStatus.IN_USE
            bookings.any { it.courtId == court.id && it.status == "CHECKED_IN" } -> CourtOccupancyStatus.IN_USE
            bookings.any { it.courtId == court.id && it.status == "CONFIRMED" } -> CourtOccupancyStatus.BOOKED
            else -> CourtOccupancyStatus.AVAILABLE
        }
    }

    val filteredOverviewCourts = remember(
        clubCourts,
        courtSearchQuery,
        courtStatusFilter,
        courtBookingTimeLimits,
        expiredCourtIds,
        inUseCourtIds,
        bookings
    ) {
        filterCourtsBySearchAndStatus(
            courts = clubCourts,
            searchQuery = courtSearchQuery,
            statusFilter = courtStatusFilter,
            statusResolver = ::resolveOverviewOccupancy
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("court_overview_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Court Status Directory (${clubCourts.size} Courts)",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Search or filter courts below and tap any available court to reserve",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                CourtSearchAndFilterBar(
                    searchQuery = courtSearchQuery,
                    onSearchQueryChange = { courtSearchQuery = it },
                    selectedFilter = courtStatusFilter,
                    onFilterSelected = { courtStatusFilter = it },
                    allCount = clubCourts.size,
                    availableCount = clubCourts.count { resolveOverviewOccupancy(it) == CourtOccupancyStatus.AVAILABLE },
                    bookedCount = clubCourts.count { resolveOverviewOccupancy(it) == CourtOccupancyStatus.BOOKED },
                    inUseCount = clubCourts.count { resolveOverviewOccupancy(it) == CourtOccupancyStatus.IN_USE },
                    tagPrefix = "court"
                )
            }
        }

        if (filteredOverviewCourts.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No courts match \"$courtSearchQuery\" ($courtStatusFilter)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                courtSearchQuery = ""
                                courtStatusFilter = "All"
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("court_reset_filters_button")
                        ) {
                            Text("Reset Search & Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(filteredOverviewCourts, key = { it.id }) { court ->
                val remainingSeconds = courtBookingTimeLimits[court.id] ?: 0
                val occupancyStatus = resolveOverviewOccupancy(court)
                MyClubCourtCard(
                    court = court,
                    occupancyStatus = occupancyStatus,
                    remainingBookingSeconds = remainingSeconds,
                    onExpireTimeLimit = { onExpireCourtTimeLimit(court.id) },
                    onBookCourt = { onSelectCourtToBook(court.id) }
                )
            }
        }
    }
}
