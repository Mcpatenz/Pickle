package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SportsTennis
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookingEntity
import com.example.data.remote.FirestoreReservationRepository
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

/**
 * Filter mode for [ExistingCourtReservationsList] allowing users to toggle between
 * viewing all their reservations or just those scheduled for the upcoming 7 days.
 */
enum class ExistingReservationsFilterMode(val displayLabel: String) {
    ALL_RESERVATIONS("All Reservations"),
    UPCOMING_7_DAYS("Upcoming 7 Days")
}

/**
 * Component that displays a list of existing court reservations to the user,
 * allowing them to view their scheduled slots persisted in Firebase Firestore,
 * filter between All Reservations and the Upcoming 7 Days, and cancel any reservation
 * item (removing the corresponding document from Firestore).
 */
@Composable
fun ExistingCourtReservationsList(
    modifier: Modifier = Modifier,
    reservations: List<BookingEntity>? = null,
    includeFirestorePersisted: Boolean = true,
    initialShowOnlyUpcoming: Boolean = false,
    initialFilterMode: ExistingReservationsFilterMode = if (initialShowOnlyUpcoming) {
        ExistingReservationsFilterMode.UPCOMING_7_DAYS
    } else {
        ExistingReservationsFilterMode.ALL_RESERVATIONS
    },
    referenceDateIso: String = FirestoreReservationRepository.DEFAULT_REFERENCE_DATE_ISO,
    onSelectReservation: (BookingEntity) -> Unit = {},
    onOpenQrPass: (BookingEntity) -> Unit = {},
    onCancelReservation: ((BookingEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FirestoreReservationRepository.ensureFirestoreInstance(context)
    }

    val firestoreReservations by FirestoreReservationRepository.reservationsFlow.collectAsState()
    val removedDocumentCodes by FirestoreReservationRepository.removedDocumentCodes.collectAsState()
    val removedBookingIds by FirestoreReservationRepository.removedBookingIds.collectAsState()
    val lastPersistedBooking by FirestoreReservationRepository.lastPersistedBooking.collectAsState()
    val isFirestoreConfigured by FirestoreReservationRepository.isFirestoreConfigured.collectAsState()

    var activeFilterMode by remember(initialFilterMode) { mutableStateOf(initialFilterMode) }
    var locallyCancelledCodes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var locallyCancelledIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var lastCancelledFeedbackMessage by remember { mutableStateOf<String?>(null) }

    val mergedReservations = remember(
        reservations,
        firestoreReservations,
        includeFirestorePersisted,
        removedDocumentCodes,
        removedBookingIds,
        locallyCancelledCodes,
        locallyCancelledIds
    ) {
        val baseList = when {
            reservations != null && includeFirestorePersisted -> {
                FirestoreReservationRepository.mergeBookingsWithFirestore(
                    passedBookings = reservations,
                    firestoreBookings = firestoreReservations
                )
            }
            reservations != null -> reservations
            else -> firestoreReservations
        }
        baseList
            .filterNot { booking ->
                booking.status == "CANCELLED" ||
                    FirestoreReservationRepository.isDocumentRemoved(booking.bookingCode, booking.id) ||
                    locallyCancelledCodes.contains(booking.bookingCode.uppercase()) ||
                    (booking.id != 0 && locallyCancelledIds.contains(booking.id))
            }
            .distinctBy { it.bookingCode.uppercase() }
            .sortedWith(
                compareBy<BookingEntity> { it.dateIso }
                    .thenBy { it.timeSlot }
                    .thenByDescending { it.createdAt }
            )
    }

    val upcoming7DaysReservations = remember(mergedReservations, referenceDateIso) {
        FirestoreReservationRepository.filterUpcoming7DaysReservations(
            bookings = mergedReservations,
            referenceDateIso = referenceDateIso
        )
    }

    val displayedReservations = remember(mergedReservations, upcoming7DaysReservations, activeFilterMode) {
        when (activeFilterMode) {
            ExistingReservationsFilterMode.ALL_RESERVATIONS -> mergedReservations
            ExistingReservationsFilterMode.UPCOMING_7_DAYS -> upcoming7DaysReservations
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("existing_court_reservations_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .testTag("existing_court_reservations_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Banner with Firebase Firestore Persistence Status
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF064E3B), Color(0xFF047857))
                        )
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = OpticVolt,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("firestore_persistence_status_badge"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Firebase Firestore Persisted",
                                    tint = OpticVoltDarkText,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (isFirestoreConfigured) {
                                        "FIREBASE FIRESTORE • PERSISTED"
                                    } else {
                                        "FIRESTORE LOCAL CACHE"
                                    },
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OpticVoltDarkText
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Existing Court Reservations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.testTag("existing_reservations_header_title")
                        )
                        Text(
                            text = "Showing ${displayedReservations.size} of ${mergedReservations.size} • Next 7 Days: ${upcoming7DaysReservations.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6F5E6),
                            modifier = Modifier.testTag("existing_reservations_upcoming_count")
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OpticVolt.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (activeFilterMode == ExistingReservationsFilterMode.UPCOMING_7_DAYS) {
                                    "NEXT 7 DAYS"
                                } else {
                                    "ALL SLOTS"
                                },
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpticVolt
                            )
                            Text(
                                text = "${displayedReservations.size} SLOTS",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.testTag("upcoming_scheduled_slots_badge_count")
                            )
                        }
                    }
                }
            }

            // Cancellation Confirmation Feedback Banner (Firestore Document Removed)
            lastCancelledFeedbackMessage?.let { feedbackMsg ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("existing_reservations_cancel_feedback_banner"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaintenanceRedBg,
                    border = BorderStroke(1.dp, MaintenanceRed.copy(alpha = 0.75f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Removed from Firestore",
                            tint = MaintenanceRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = feedbackMsg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaintenanceRed,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("existing_reservations_cancel_feedback_message")
                        )
                    }
                }
            }

            // Latest Persisted Firestore Document Banner
            lastPersistedBooking?.let { latest ->
                if (!locallyCancelledCodes.contains(latest.bookingCode.uppercase())) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("firestore_latest_persisted_banner"),
                        shape = RoundedCornerShape(12.dp),
                        color = AvailableGreenBg,
                        border = BorderStroke(1.dp, AvailableGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Saved to Firestore",
                                tint = AvailableGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Synced to Firebase Firestore (${latest.bookingCode})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldDark
                                )
                                Text(
                                    text = "Court ID #${latest.courtId} (${latest.courtName}) • ${latest.dateIso} • ${latest.timeSlot}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.testTag("firestore_latest_persisted_details")
                                )
                            }
                        }
                    }
                }
            }

            // Filter Toggle Header & Mechanism: All Reservations vs Upcoming 7 Days
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("existing_reservations_filter_bar"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter Reservations",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Filter Reservations:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (activeFilterMode == ExistingReservationsFilterMode.UPCOMING_7_DAYS) {
                            "Window: $referenceDateIso + 7 Days"
                        } else {
                            "Showing All Scheduled Dates"
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldPrimary,
                        modifier = Modifier.testTag("existing_reservations_active_filter_label")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isAllSelected = activeFilterMode == ExistingReservationsFilterMode.ALL_RESERVATIONS
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                activeFilterMode = ExistingReservationsFilterMode.ALL_RESERVATIONS
                            }
                            .testTag("existing_reservations_filter_all_chip"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAllSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(
                            1.dp,
                            if (isAllSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .testTag("existing_reservations_filter_all"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAllSelected) Icons.Default.CheckCircle else Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "All Reservations (${mergedReservations.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val isUpcoming7Selected = activeFilterMode == ExistingReservationsFilterMode.UPCOMING_7_DAYS
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                activeFilterMode = ExistingReservationsFilterMode.UPCOMING_7_DAYS
                            }
                            .testTag("existing_reservations_filter_upcoming_7_days_chip"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isUpcoming7Selected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(
                            1.dp,
                            if (isUpcoming7Selected) EmeraldPrimary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .testTag("existing_reservations_filter_upcoming"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isUpcoming7Selected) Icons.Default.CheckCircle else Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = if (isUpcoming7Selected) Color.White else EmeraldPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Upcoming 7 Days (${upcoming7DaysReservations.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isUpcoming7Selected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Reservations List Container
            if (displayedReservations.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("existing_reservations_empty_state"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsTennis,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (activeFilterMode == ExistingReservationsFilterMode.UPCOMING_7_DAYS) {
                                "No court reservations in the upcoming 7 days"
                            } else {
                                "No existing court reservations found"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Switch filter to 'All Reservations' or use the form above to book a court.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upcoming_scheduled_slots_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayedReservations.forEachIndexed { index, booking ->
                        val isWithin7Days = remember(booking.dateIso, referenceDateIso) {
                            FirestoreReservationRepository.isDateWithinUpcoming7Days(
                                dateIso = booking.dateIso,
                                referenceDateIso = referenceDateIso
                            )
                        }
                        ExistingCourtReservationSlotCard(
                            booking = booking,
                            index = index,
                            isWithinUpcoming7Days = isWithin7Days,
                            onSelectReservation = { onSelectReservation(booking) },
                            onOpenQrPass = { onOpenQrPass(booking) },
                            onCancelReservation = {
                                locallyCancelledCodes = locallyCancelledCodes + booking.bookingCode.uppercase()
                                if (booking.id != 0) {
                                    locallyCancelledIds = locallyCancelledIds + booking.id
                                }
                                FirestoreReservationRepository.removeReservationFromFirestore(
                                    context = context,
                                    bookingCode = booking.bookingCode,
                                    bookingId = booking.id
                                )
                                lastCancelledFeedbackMessage =
                                    "Cancelled ${booking.courtName} (${booking.bookingCode}) on ${booking.dateIso} • Document removed from Firestore."
                                onCancelReservation?.invoke(booking)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExistingCourtReservationSlotCard(
    booking: BookingEntity,
    index: Int = 0,
    isWithinUpcoming7Days: Boolean = true,
    onSelectReservation: () -> Unit = {},
    onOpenQrPass: () -> Unit = {},
    onCancelReservation: () -> Unit = {}
) {
    val isConfirmedOrPaid = booking.status == "CONFIRMED" ||
        booking.status == "CONFIRMED_PAID" ||
        booking.paymentStatus == "PAID"
    val statusLabel = when (booking.status) {
        "CONFIRMED", "CONFIRMED_PAID" -> "CONFIRMED"
        "APPROVED_AWAITING_PAYMENT" -> "APPROVED"
        "PENDING_CASHIER_VERIFICATION" -> "VERIFYING"
        "PENDING_ADMIN_APPROVAL" -> "UPCOMING • PENDING"
        "CHECKED_IN" -> "CHECKED IN"
        "COMPLETED" -> "COMPLETED"
        "CANCELLED" -> "CANCELLED"
        else -> booking.status
    }
    val statusColor = when {
        booking.status == "CANCELLED" -> MaintenanceRed
        isConfirmedOrPaid -> AvailableGreen
        booking.status == "APPROVED_AWAITING_PAYMENT" -> GCashBlue
        else -> PeakAmber
    }
    val statusBgColor = when {
        booking.status == "CANCELLED" -> MaintenanceRedBg
        isConfirmedOrPaid -> AvailableGreenBg
        booking.status == "APPROVED_AWAITING_PAYMENT" -> GCashBlueBg
        else -> Color(0xFFFFFBEB)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelectReservation() }
            .testTag("existing_reservation_item_${booking.id}"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("existing_reservation_code_${booking.bookingCode}"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = EmeraldDark,
                        shape = CircleShape,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SportsTennis,
                                contentDescription = null,
                                tint = OpticVolt,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Court ID #${booking.courtId} • ${booking.courtName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("existing_reservation_court_${booking.id}")
                        )
                        Text(
                            text = "${booking.facilityName} • ${booking.bookingCode}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isWithinUpcoming7Days) {
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "NEXT 7D",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .testTag("existing_reservation_next7d_badge_${booking.id}")
                            )
                        }
                    }
                    Surface(
                        color = statusBgColor,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = statusLabel,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("existing_reservation_status_${booking.id}")
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            // Scheduled Date and Time Slot Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Scheduled Date",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${booking.dateIso} (${booking.dateLabel})",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("existing_reservation_date_${booking.id}")
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Scheduled Time Slot",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${booking.timeSlot} • ${booking.timeRangeLabel}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.testTag("existing_reservation_timeslot_${booking.id}")
                    )
                }
            }

            // Player Name, Payment Method, Total Amount, and Cancel / QR Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${booking.playerName} • ${booking.paymentMethod} • ₱${booking.totalAmount}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("existing_reservation_player_${booking.id}")
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (booking.isQrPassReady) {
                        Button(
                            onClick = onOpenQrPass,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("existing_reservation_qr_button_${booking.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("QR Pass", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Cancel button on each item that removes the corresponding document from Firestore
                    OutlinedButton(
                        onClick = onCancelReservation,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaintenanceRed.copy(alpha = 0.75f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaintenanceRedBg.copy(alpha = 0.45f),
                            contentColor = MaintenanceRed
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("existing_reservation_cancel_button_${booking.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Cancel Reservation",
                            tint = MaintenanceRed,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cancel",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaintenanceRed,
                            modifier = Modifier.testTag("existing_reservation_cancel_code_${booking.bookingCode}")
                        )
                    }
                }
            }
        }
    }
}
