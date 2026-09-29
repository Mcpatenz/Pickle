package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.BookingEntity
import com.example.data.local.CourtEntity
import com.example.data.local.FacilityEntity
import com.example.data.remote.FirestoreReservationRepository
import com.example.data.remote.PersistedReservationFormState
import com.example.ui.theme.AvailableGreen
import com.example.ui.theme.AvailableGreenBg
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GCashBlue
import com.example.ui.theme.GCashBlueBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MaintenanceRed
import com.example.ui.theme.MaintenanceRedBg
import com.example.ui.theme.MayaMint
import com.example.ui.theme.MayaMintBg
import com.example.ui.theme.OpticVolt
import com.example.ui.theme.OpticVoltDarkText
import com.example.ui.theme.PeakAmber
import com.example.viewmodel.BookingDateOption
import com.example.viewmodel.TimeSlotOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Structured data model representing user inputs captured by [CourtReservationForm].
 */
data class CourtReservationFormInput(
    val courtId: Int,
    val courtName: String,
    val dateIso: String,
    val dateLabel: String,
    val timeSlot: String,
    val timeRangeLabel: String,
    val playerName: String = "Jonel P.",
    val playerEmail: String = "jonel.p@pickleplay.ph",
    val durationLabel: String = "1 Hour",
    val paymentMethod: String = "GCash",
    val notes: String = "",
    val courtFee: Int = 250,
    val serviceFee: Int = 20,
    val totalAmount: Int = 270
)

/**
 * Validation result for court reservation form inputs (date, time slot, court ID).
 */
data class CourtReservationValidationResult(
    val isValid: Boolean,
    val parsedCourtId: Int? = null,
    val normalizedDateIso: String = "",
    val normalizedTimeSlot: String = "",
    val errorMessage: String? = null,
    val courtIdError: String? = null,
    val dateError: String? = null,
    val timeSlotError: String? = null
)

/**
 * UI feedback state for the reservation form Snackbar notification.
 */
data class ReservationFormSnackbarFeedback(
    val message: String,
    val isError: Boolean
)

val DefaultReservationFormCourts = listOf(
    CourtEntity(1, 1, "Court 1", "Pro Cushion Indoor", true, "ACTIVE", 200, 250, 350, 300, 380),
    CourtEntity(2, 1, "Court 2", "Pro Cushion Indoor", true, "ACTIVE", 200, 250, 350, 300, 380),
    CourtEntity(3, 1, "Court 3", "Pro Cushion Indoor", true, "ACTIVE", 200, 250, 350, 300, 380),
    CourtEntity(4, 1, "Court 4", "Pro Cushion Indoor", true, "ACTIVE", 200, 250, 350, 300, 380),
    CourtEntity(5, 1, "Court 5", "Pro Acrylic Covered", false, "ACTIVE", 180, 220, 320, 280, 350),
    CourtEntity(6, 1, "Court 6", "Pro Acrylic Covered", false, "ACTIVE", 180, 220, 320, 280, 350)
)

val DefaultReservationFormDates = listOf(
    BookingDateOption("2026-09-27", "Sun", "27", "September 27, 2026", true),
    BookingDateOption("2026-09-28", "Mon", "28", "September 28, 2026", false),
    BookingDateOption("2026-09-29", "Tue", "29", "September 29, 2026", false),
    BookingDateOption("2026-09-30", "Wed", "30", "September 30, 2026", false),
    BookingDateOption("2026-10-01", "Thu", "1", "October 1, 2026", false),
    BookingDateOption("2026-10-02", "Fri", "2", "October 2, 2026", false),
    BookingDateOption("2026-10-03", "Sat", "3", "October 3, 2026", true)
)

val DefaultReservationFormTimeSlots = listOf(
    TimeSlotOption("07:00 AM", "7:00 AM - 8:00 AM", "MORNING"),
    TimeSlotOption("08:00 AM", "8:00 AM - 9:00 AM", "MORNING"),
    TimeSlotOption("09:00 AM", "9:00 AM - 10:00 AM", "MORNING"),
    TimeSlotOption("10:00 AM", "10:00 AM - 11:00 AM", "MIDDAY"),
    TimeSlotOption("11:00 AM", "11:00 AM - 12:00 PM", "MIDDAY"),
    TimeSlotOption("12:00 PM", "12:00 PM - 1:00 PM", "MIDDAY"),
    TimeSlotOption("01:00 PM", "1:00 PM - 2:00 PM", "MIDDAY"),
    TimeSlotOption("02:00 PM", "2:00 PM - 3:00 PM", "MIDDAY"),
    TimeSlotOption("03:00 PM", "3:00 PM - 4:00 PM", "MIDDAY"),
    TimeSlotOption("04:00 PM", "4:00 PM - 5:00 PM", "PEAK"),
    TimeSlotOption("05:00 PM", "5:00 PM - 6:00 PM", "PEAK"),
    TimeSlotOption("06:00 PM", "6:00 PM - 7:00 PM", "PEAK"),
    TimeSlotOption("07:00 PM", "7:00 PM - 8:00 PM", "PEAK"),
    TimeSlotOption("08:00 PM", "8:00 PM - 9:00 PM", "PEAK"),
    TimeSlotOption("09:00 PM", "9:00 PM - 10:00 PM", "NIGHT"),
    TimeSlotOption("10:00 PM", "10:00 PM - 11:00 PM", "NIGHT")
)

private val ValidClubTimeSlotRegex = Regex(
    pattern = """^(0?[6-9]|1[0-1]):(00|30)\s*AM$|^12:(00|30)\s*PM$|^(0?[1-9]|10):(00|30)\s*PM$""",
    option = RegexOption.IGNORE_CASE
)

/**
 * Parses a user-entered date string (either `YYYY-MM-DD` or human-readable label like `September 30, 2026`)
 * into a [LocalDate], or returns null if invalid.
 */
fun parseReservationDateOrNull(
    rawDate: String,
    dateOptions: List<BookingDateOption> = DefaultReservationFormDates
): LocalDate? {
    val trimmed = rawDate.trim()
    if (trimmed.isEmpty()) return null

    // Match against known dateOptions first
    val matchedOption = dateOptions.find {
        it.isoDate.equals(trimmed, ignoreCase = true) ||
            it.fullLabel.equals(trimmed, ignoreCase = true)
    }
    if (matchedOption != null) {
        runCatching { return LocalDate.parse(matchedOption.isoDate) }
    }

    // Try standard ISO_LOCAL_DATE (YYYY-MM-DD)
    runCatching { return LocalDate.parse(trimmed) }

    // Try flexible formats such as "September 30, 2026" or "Sep 30, 2026"
    val formatters = listOf(
        DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH)
    )
    for (formatter in formatters) {
        val parsed = runCatching { LocalDate.parse(trimmed, formatter) }.getOrNull()
        if (parsed != null) return parsed
    }
    return null
}

/**
 * Resolves and validates a time slot string against available [TimeSlotOption]s and operating hours.
 */
fun resolveValidTimeSlotOrNull(
    rawTimeSlot: String,
    timeSlotOptions: List<TimeSlotOption> = DefaultReservationFormTimeSlots
): String? {
    val trimmed = rawTimeSlot.trim()
    if (trimmed.isEmpty()) return null

    val normalizedNoLeadingZero = trimmed.removePrefix("0").trim()
    val matchedOption = timeSlotOptions.find { opt ->
        opt.startTime.equals(trimmed, ignoreCase = true) ||
            opt.startTime.removePrefix("0").equals(normalizedNoLeadingZero, ignoreCase = true) ||
            opt.rangeLabel.equals(trimmed, ignoreCase = true) ||
            opt.rangeLabel.removePrefix("0").equals(normalizedNoLeadingZero, ignoreCase = true)
    }
    if (matchedOption != null) {
        return matchedOption.startTime
    }

    if (ValidClubTimeSlotRegex.matches(trimmed)) {
        val upper = trimmed.uppercase(Locale.ENGLISH).replace(Regex("\\s+"), " ")
        val parts = upper.split(":")
        val hourPart = parts.firstOrNull()?.padStart(2, '0') ?: return upper
        val rest = parts.drop(1).joinToString(":")
        return "$hourPart:$rest"
    }
    return null
}

/**
 * Validates raw user inputs for date, time slot, and court ID:
 * - Ensures Court ID is not empty, is a valid positive integer, exists in available courts, and is not under maintenance.
 * - Ensures Date is not empty, is a valid calendar date, and is in the future (strictly after [referenceDateIso]).
 * - Ensures Time Slot is not empty, is a valid operating time slot, and does not conflict with an existing reservation.
 */
fun validateCourtReservationForm(
    dateInput: String,
    timeSlotInput: String,
    courtIdInput: String,
    availableCourts: List<CourtEntity> = DefaultReservationFormCourts,
    existingBookings: List<BookingEntity> = emptyList(),
    dateOptions: List<BookingDateOption> = DefaultReservationFormDates,
    timeSlotOptions: List<TimeSlotOption> = DefaultReservationFormTimeSlots,
    referenceDateIso: String = "2026-09-28"
): CourtReservationValidationResult {
    // 1. Validate Court ID (must not be empty, must be a valid positive number, and match an active court)
    val rawCourtText = courtIdInput.trim()
    val cleanCourtIdText = rawCourtText
        .removePrefix("Court")
        .removePrefix("court")
        .removePrefix("#")
        .trim()

    var parsedCourtId: Int? = null
    val courtIdError: String? = when {
        rawCourtText.isEmpty() || cleanCourtIdText.isEmpty() -> {
            "Court ID cannot be empty. Please enter or select a Court ID."
        }
        else -> {
            val idNum = cleanCourtIdText.toIntOrNull()
            if (idNum == null || idNum <= 0) {
                "Please enter a valid numeric Court ID (e.g., 1, 2, 3)."
            } else {
                parsedCourtId = idNum
                val matchedCourt = availableCourts.find { it.id == idNum }
                when {
                    availableCourts.isNotEmpty() && matchedCourt == null -> {
                        "Court ID #$idNum is not a valid club court (choose 1–${availableCourts.maxOf { it.id }})."
                    }
                    matchedCourt != null && matchedCourt.status.equals("MAINTENANCE", ignoreCase = true) -> {
                        "${matchedCourt.name} (ID: $idNum) is currently under maintenance."
                    }
                    else -> null
                }
            }
        }
    }

    // 2. Validate Date (must not be empty, must be valid, and must be in the future)
    val cleanDate = dateInput.trim()
    val referenceDate = runCatching { LocalDate.parse(referenceDateIso) }
        .getOrElse { LocalDate.of(2026, 9, 28) }
    val parsedLocalDate = parseReservationDateOrNull(cleanDate, dateOptions)
    val normalizedDateIso = parsedLocalDate?.toString() ?: cleanDate

    val dateError: String? = when {
        cleanDate.isEmpty() -> {
            "Reservation date cannot be empty. Please select a future date."
        }
        parsedLocalDate == null -> {
            "Invalid date format '$cleanDate'. Please enter a valid date (YYYY-MM-DD)."
        }
        !parsedLocalDate.isAfter(referenceDate) -> {
            "Reservation date must be in the future (after $referenceDateIso)."
        }
        else -> null
    }

    // 3. Validate Time Slot (must not be empty, must be a valid club time slot, and not double-booked)
    val cleanTimeSlot = timeSlotInput.trim()
    val resolvedSlot = resolveValidTimeSlotOrNull(cleanTimeSlot, timeSlotOptions)
    val normalizedTimeSlot = resolvedSlot ?: cleanTimeSlot

    val timeSlotError: String? = when {
        cleanTimeSlot.isEmpty() -> {
            "Time slot cannot be empty. Please select a valid time slot."
        }
        resolvedSlot == null -> {
            "Invalid time slot '$cleanTimeSlot'. Please select a valid time slot (07:00 AM – 10:00 PM)."
        }
        else -> {
            val hasConflict = parsedCourtId != null && existingBookings.any { booking ->
                booking.courtId == parsedCourtId &&
                    booking.status != "CANCELLED" &&
                    (booking.dateIso.equals(normalizedDateIso, ignoreCase = true) ||
                        booking.dateLabel.equals(cleanDate, ignoreCase = true)) &&
                    (booking.timeSlot.equals(normalizedTimeSlot, ignoreCase = true) ||
                        booking.timeRangeLabel.equals(cleanTimeSlot, ignoreCase = true))
            }
            if (hasConflict) {
                "Court $parsedCourtId is already reserved on $normalizedDateIso at $normalizedTimeSlot."
            } else {
                null
            }
        }
    }

    val allErrors = listOfNotNull(courtIdError, dateError, timeSlotError)
    val isValid = allErrors.isEmpty() && parsedCourtId != null

    return CourtReservationValidationResult(
        isValid = isValid,
        parsedCourtId = parsedCourtId,
        normalizedDateIso = normalizedDateIso,
        normalizedTimeSlot = normalizedTimeSlot,
        errorMessage = if (allErrors.isEmpty()) null else allErrors.joinToString(" • "),
        courtIdError = courtIdError,
        dateError = dateError,
        timeSlotError = timeSlotError
    )
}

/**
 * Converts a [CourtReservationFormInput] into a Room [BookingEntity] ready for local persistence.
 */
fun buildBookingFromReservationForm(
    input: CourtReservationFormInput,
    facility: FacilityEntity? = null,
    newId: Int = 0,
    bookingCodeOverride: String? = null
): BookingEntity {
    val cleanDateCompact = input.dateIso.replace(Regex("[^0-9]"), "").take(8).ifBlank { "20260929" }
    val code = bookingCodeOverride ?: "PKL-$cleanDateCompact-${(10000..99999).random()}"
    val isWalkIn = input.paymentMethod.equals("Cash on Hand", ignoreCase = true) ||
        input.paymentMethod.equals("Pay at Venue", ignoreCase = true)
    return BookingEntity(
        id = newId,
        bookingCode = code,
        facilityId = facility?.id ?: 1,
        facilityName = facility?.name ?: "Smash Pickle Club",
        facilityLocation = facility?.city ?: "Quezon City",
        courtId = input.courtId,
        courtName = input.courtName,
        dateIso = input.dateIso,
        dateLabel = input.dateLabel,
        timeSlot = input.timeSlot,
        timeRangeLabel = input.timeRangeLabel,
        playerName = input.playerName.trim().ifBlank { "Jonel P." },
        courtFee = input.courtFee,
        discountAmount = 0,
        serviceFee = input.serviceFee,
        totalAmount = input.totalAmount,
        paymentMethod = input.paymentMethod,
        paymentStatus = if (isWalkIn) "CASH_ON_HAND_AT_CASHIER" else "AWAITING_APPROVAL",
        status = if (isWalkIn) "PENDING_ADMIN_APPROVAL" else "PENDING_ADMIN_APPROVAL",
        customerEmail = input.playerEmail.trim().ifBlank { "jonel.p@pickleplay.ph" },
        isWalkIn = isWalkIn
    )
}

/**
 * Composable form that captures user inputs for court reservations including:
 * - Date (`dateIso` / `dateLabel` via text field + interactive date chips, validated to be in the future)
 * - Time Slot (`timeSlot` / `timeRangeLabel` via text field + interactive time slot chips, validated against club hours & conflicts)
 * - Court ID (`courtId` via text field + interactive court ID selector cards, validated to be non-empty and active)
 * - Visual loading indicator and success/error Snackbar message upon submission attempt
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CourtReservationForm(
    modifier: Modifier = Modifier,
    facility: FacilityEntity? = null,
    courts: List<CourtEntity> = DefaultReservationFormCourts,
    bookings: List<BookingEntity> = emptyList(),
    dateOptions: List<BookingDateOption> = DefaultReservationFormDates,
    timeSlotOptions: List<TimeSlotOption> = DefaultReservationFormTimeSlots,
    initialDateIso: String = "2026-09-29",
    initialTimeSlot: String = "02:00 PM",
    initialCourtId: Int = 5,
    initialPlayerName: String = "Jonel P.",
    initialPaymentMethod: String = "GCash",
    referenceDateIso: String = "2026-09-28",
    isLoading: Boolean = false,
    onDateChanged: (BookingDateOption) -> Unit = {},
    onTimeSlotChanged: (TimeSlotOption) -> Unit = {},
    onCourtIdChanged: (Int) -> Unit = {},
    onPaymentMethodChanged: (String) -> Unit = {},
    onSubmitReservation: (CourtReservationFormInput) -> Unit = {}
) {
    val context = LocalContext.current
    val effectiveCourts = courts.ifEmpty { DefaultReservationFormCourts }
    val effectiveDates = dateOptions.ifEmpty { DefaultReservationFormDates }
    val effectiveTimeSlots = timeSlotOptions.ifEmpty { DefaultReservationFormTimeSlots }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        FirestoreReservationRepository.ensureFirestoreInstance(context)
    }

    var dateInput by remember(initialDateIso) { mutableStateOf(initialDateIso) }
    var timeSlotInput by remember(initialTimeSlot) { mutableStateOf(initialTimeSlot) }
    var courtIdInput by remember(initialCourtId) { mutableStateOf(initialCourtId.toString()) }
    var playerNameInput by remember(initialPlayerName) { mutableStateOf(initialPlayerName) }
    var paymentMethodInput by remember(initialPaymentMethod) { mutableStateOf(initialPaymentMethod) }
    var notesInput by remember { mutableStateOf("") }

    LaunchedEffect(courtIdInput, dateInput, timeSlotInput, playerNameInput, paymentMethodInput) {
        val cid = courtIdInput.trim()
            .removePrefix("Court")
            .removePrefix("court")
            .removePrefix("#")
            .trim()
            .toIntOrNull() ?: initialCourtId
        val cname = effectiveCourts.find { it.id == cid }?.name ?: "Court $cid"
        val dLabel = effectiveDates.find { it.isoDate.equals(dateInput.trim(), ignoreCase = true) }?.fullLabel
            ?: dateInput.trim()
        val tRange = effectiveTimeSlots.find { it.startTime.equals(timeSlotInput.trim(), ignoreCase = true) }?.rangeLabel
            ?: timeSlotInput.trim()
        FirestoreReservationRepository.saveFormStateToFirestore(
            context = context,
            state = PersistedReservationFormState(
                courtId = cid,
                courtName = cname,
                dateIso = dateInput.trim(),
                dateLabel = dLabel,
                timeSlot = timeSlotInput.trim(),
                timeRangeLabel = tRange,
                playerName = playerNameInput.trim().ifBlank { "Jonel P." },
                paymentMethod = paymentMethodInput
            )
        )
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    var courtIdFieldError by remember { mutableStateOf<String?>(null) }
    var dateFieldError by remember { mutableStateOf<String?>(null) }
    var timeSlotFieldError by remember { mutableStateOf<String?>(null) }

    var isSubmittingInternal by remember { mutableStateOf(false) }
    val showLoadingIndicator = isLoading || isSubmittingInternal

    var snackbarFeedback by remember { mutableStateOf<ReservationFormSnackbarFeedback?>(null) }
    var lastSubmittedInput by remember { mutableStateOf<CourtReservationFormInput?>(null) }

    val resolvedDateOption = remember(dateInput, effectiveDates) {
        effectiveDates.find {
            it.isoDate.equals(dateInput.trim(), ignoreCase = true) ||
                it.fullLabel.equals(dateInput.trim(), ignoreCase = true)
        }
    }

    val resolvedTimeSlotOption = remember(timeSlotInput, effectiveTimeSlots) {
        val clean = timeSlotInput.trim()
        val noZero = clean.removePrefix("0").trim()
        effectiveTimeSlots.find {
            it.startTime.equals(clean, ignoreCase = true) ||
                it.startTime.removePrefix("0").equals(noZero, ignoreCase = true) ||
                it.rangeLabel.equals(clean, ignoreCase = true) ||
                it.rangeLabel.removePrefix("0").equals(noZero, ignoreCase = true)
        }
    }

    val parsedCourtId = remember(courtIdInput) {
        courtIdInput.trim()
            .removePrefix("Court")
            .removePrefix("court")
            .removePrefix("#")
            .trim()
            .toIntOrNull()
    }

    val resolvedCourt = remember(parsedCourtId, effectiveCourts) {
        effectiveCourts.find { it.id == parsedCourtId }
    }

    val liveValidation = remember(
        dateInput,
        timeSlotInput,
        courtIdInput,
        effectiveCourts,
        bookings,
        effectiveDates,
        effectiveTimeSlots,
        referenceDateIso
    ) {
        validateCourtReservationForm(
            dateInput = dateInput,
            timeSlotInput = timeSlotInput,
            courtIdInput = courtIdInput,
            availableCourts = effectiveCourts,
            existingBookings = bookings,
            dateOptions = effectiveDates,
            timeSlotOptions = effectiveTimeSlots,
            referenceDateIso = referenceDateIso
        )
    }

    val calculatedCourtFee = remember(resolvedCourt, resolvedDateOption, resolvedTimeSlotOption) {
        val c = resolvedCourt
        val d = resolvedDateOption
        val s = resolvedTimeSlotOption
        when {
            c == null -> 250
            d?.isWeekend == true -> c.weekendPrice
            s?.periodTier == "MORNING" -> c.morningPrice
            s?.periodTier == "PEAK" -> c.peakPrice
            s?.periodTier == "NIGHT" -> c.nightPrice
            else -> c.middayPrice
        }
    }
    val serviceFee = 20
    val totalAmount = calculatedCourtFee + serviceFee

    fun isSlotBookedForCourt(courtId: Int?, dateStr: String, slotStartTime: String): Boolean {
        if (courtId == null) return false
        return bookings.any { b ->
            b.courtId == courtId &&
                b.status != "CANCELLED" &&
                (b.dateIso.equals(dateStr, ignoreCase = true) || b.dateLabel.equals(dateStr, ignoreCase = true)) &&
                b.timeSlot.equals(slotStartTime, ignoreCase = true)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("court_reservation_form_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(EmeraldDark, EmeraldPrimary)
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
                            Text(
                                text = "COURT RESERVATION FORM",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OpticVoltDarkText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Book by Date, Time Slot & Court ID",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "${facility?.name ?: "Smash Pickle Club"} • Instant Slot Lock & QR Pass",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6F5E6)
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OpticVolt.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TOTAL",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpticVolt
                            )
                            Text(
                                text = "₱$totalAmount",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.testTag("reservation_form_header_total")
                            )
                        }
                    }
                }
            }

            // 1. COURT ID INPUT & QUICK SELECTOR
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. Court ID",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = resolvedCourt?.let { "${it.name} (${it.courtType})" }
                            ?: parsedCourtId?.let { "Court $it" }
                            ?: "Select or enter Court ID",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (resolvedCourt != null) EmeraldPrimary else PeakAmber,
                        modifier = Modifier.testTag("reservation_form_resolved_court_label")
                    )
                }

                OutlinedTextField(
                    value = courtIdInput,
                    onValueChange = { newValue ->
                        courtIdInput = newValue
                        courtIdFieldError = null
                        validationError = null
                        isSubmittingInternal = false
                        val parsed = newValue.trim()
                            .removePrefix("Court")
                            .removePrefix("court")
                            .removePrefix("#")
                            .trim()
                            .toIntOrNull()
                        if (parsed != null) {
                            onCourtIdChanged(parsed)
                        }
                    },
                    label = { Text("Court ID (e.g., 1, 2, 3, 4, 5, 6)") },
                    placeholder = { Text("Enter Court ID number") },
                    isError = courtIdFieldError != null,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = "Court ID",
                            tint = if (courtIdFieldError != null) MaintenanceRed else EmeraldPrimary
                        )
                    },
                    trailingIcon = {
                        if (courtIdInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    courtIdInput = ""
                                    isSubmittingInternal = false
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Court ID")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reservation_form_court_id_input")
                )

                if (courtIdFieldError != null) {
                    Text(
                        text = courtIdFieldError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaintenanceRed,
                        modifier = Modifier.testTag("reservation_form_court_id_error")
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(effectiveCourts, key = { it.id }) { court ->
                        val isSelected = parsedCourtId == court.id
                        val isMaintenance = court.status.equals("MAINTENANCE", ignoreCase = true)
                        val isSlotConflict = isSlotBookedForCourt(court.id, dateInput.trim(), timeSlotInput.trim())
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isMaintenance) {
                                    courtIdInput = court.id.toString()
                                    courtIdFieldError = null
                                    validationError = null
                                    isSubmittingInternal = false
                                    onCourtIdChanged(court.id)
                                }
                                .testTag("reservation_form_court_chip_${court.id}"),
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isMaintenance -> MaintenanceRedBg
                                isSelected -> GCashBlue
                                isSlotConflict -> MaintenanceRedBg.copy(alpha = 0.55f)
                                else -> GCashBlueBg.copy(alpha = 0.65f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = when {
                                    isMaintenance -> MaintenanceRed
                                    isSelected -> OpticVolt
                                    isSlotConflict -> MaintenanceRed
                                    else -> GCashBlue
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "ID #${court.id} • ${court.name}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isMaintenance -> MaintenanceRed
                                        isSelected -> Color.White
                                        else -> GCashBlue
                                    }
                                )
                                Text(
                                    text = if (court.isIndoor) "Indoor" else "Outdoor",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2. DATE INPUT & QUICK DATE CHIPS
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Reservation Date (Future Date)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = resolvedDateOption?.fullLabel ?: dateInput.ifBlank { "Select Date" },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (liveValidation.dateError == null) EmeraldPrimary else MaintenanceRed,
                        modifier = Modifier.testTag("reservation_form_resolved_date_label")
                    )
                }

                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { newDate ->
                        dateInput = newDate
                        dateFieldError = null
                        validationError = null
                        isSubmittingInternal = false
                        effectiveDates.find {
                            it.isoDate.equals(newDate.trim(), ignoreCase = true) ||
                                it.fullLabel.equals(newDate.trim(), ignoreCase = true)
                        }?.let { onDateChanged(it) }
                    },
                    label = { Text("Reservation Date (YYYY-MM-DD)") },
                    placeholder = { Text("e.g., 2026-09-29") },
                    isError = dateFieldError != null,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Reservation Date",
                            tint = if (dateFieldError != null) MaintenanceRed else EmeraldPrimary
                        )
                    },
                    trailingIcon = {
                        if (dateInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    dateInput = ""
                                    isSubmittingInternal = false
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Date")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reservation_form_date_input")
                )

                if (dateFieldError != null) {
                    Text(
                        text = dateFieldError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaintenanceRed,
                        modifier = Modifier.testTag("reservation_form_date_error")
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(effectiveDates, key = { it.isoDate }) { dOpt ->
                        val isSelected = dateInput.trim().equals(dOpt.isoDate, ignoreCase = true) ||
                            dateInput.trim().equals(dOpt.fullLabel, ignoreCase = true)
                        val isFutureOption = dOpt.isoDate > referenceDateIso
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    dateInput = dOpt.isoDate
                                    dateFieldError = null
                                    validationError = null
                                    isSubmittingInternal = false
                                    onDateChanged(dOpt)
                                }
                                .testTag("reservation_form_date_chip_${dOpt.isoDate}"),
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isSelected && isFutureOption -> EmeraldPrimary
                                isSelected && !isFutureOption -> MaintenanceRed
                                !isFutureOption -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = when {
                                    isSelected -> OpticVolt
                                    !isFutureOption -> MaintenanceRed.copy(alpha = 0.45f)
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${dOpt.dayShort} ${dOpt.dayNumber}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = dOpt.isoDate.substringAfter("-"),
                                    fontSize = 9.sp,
                                    color = if (isSelected) Color(0xFFD6F5E6) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. TIME SLOT INPUT & QUICK TIME SLOT CHIPS
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. Time Slot",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = resolvedTimeSlotOption?.rangeLabel ?: timeSlotInput.ifBlank { "Select Time Slot" },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (liveValidation.timeSlotError == null) EmeraldPrimary else MaintenanceRed,
                        modifier = Modifier.testTag("reservation_form_resolved_timeslot_label")
                    )
                }

                OutlinedTextField(
                    value = timeSlotInput,
                    onValueChange = { newSlot ->
                        timeSlotInput = newSlot
                        timeSlotFieldError = null
                        validationError = null
                        isSubmittingInternal = false
                        effectiveTimeSlots.find {
                            it.startTime.equals(newSlot.trim(), ignoreCase = true) ||
                                it.rangeLabel.equals(newSlot.trim(), ignoreCase = true)
                        }?.let { onTimeSlotChanged(it) }
                    },
                    label = { Text("Time Slot (e.g., 02:00 PM or 06:00 PM)") },
                    placeholder = { Text("Enter or tap a time slot below") },
                    isError = timeSlotFieldError != null,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Time Slot",
                            tint = if (timeSlotFieldError != null) MaintenanceRed else EmeraldPrimary
                        )
                    },
                    trailingIcon = {
                        if (timeSlotInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    timeSlotInput = ""
                                    isSubmittingInternal = false
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Time Slot")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reservation_form_time_slot_input")
                )

                if (timeSlotFieldError != null) {
                    Text(
                        text = timeSlotFieldError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaintenanceRed,
                        modifier = Modifier.testTag("reservation_form_time_slot_error")
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(effectiveTimeSlots, key = { it.startTime }) { slotOpt ->
                        val isBooked = isSlotBookedForCourt(parsedCourtId, dateInput.trim(), slotOpt.startTime)
                        val isSelected = timeSlotInput.trim().equals(slotOpt.startTime, ignoreCase = true) ||
                            timeSlotInput.trim().equals(slotOpt.rangeLabel, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isBooked) {
                                    timeSlotInput = slotOpt.startTime
                                    timeSlotFieldError = null
                                    validationError = null
                                    isSubmittingInternal = false
                                    onTimeSlotChanged(slotOpt)
                                }
                                .testTag("reservation_form_timeslot_chip_${slotOpt.startTime.replace(" ", "_")}"),
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isBooked -> MaintenanceRedBg.copy(alpha = 0.6f)
                                isSelected -> EmeraldPrimary
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = when {
                                    isBooked -> MaintenanceRed.copy(alpha = 0.5f)
                                    isSelected -> OpticVolt
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = slotOpt.startTime,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isBooked -> MaintenanceRed
                                        isSelected -> Color.White
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Text(
                                    text = if (isBooked) "BOOKED" else slotOpt.periodTier,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        isBooked -> MaintenanceRed
                                        isSelected -> OpticVolt
                                        else -> AvailableGreen
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 4. PLAYER NAME & PAYMENT METHOD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = playerNameInput,
                    onValueChange = {
                        playerNameInput = it
                        validationError = null
                    },
                    label = { Text("Player Name") },
                    placeholder = { Text("Your Full Name") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Player Name",
                            tint = EmeraldPrimary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reservation_form_player_name_input")
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("GCash", "GCASH", GCashBlue to GCashBlueBg),
                    Triple("PayMaya", "PAYMAYA", MayaMint to MayaMintBg),
                    Triple("Pay at Venue", "VENUE", EmeraldPrimary to AvailableGreenBg)
                ).forEach { (methodName, badge, colors) ->
                    val (accent, bg) = colors
                    val isSelected = paymentMethodInput.equals(methodName, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                paymentMethodInput = methodName
                                onPaymentMethodChanged(methodName)
                            }
                            .testTag("reservation_form_payment_chip_$badge"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) accent else bg.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, accent)
                    ) {
                        Text(
                            text = methodName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else accent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // 5. LIVE CAPTURED INPUTS SUMMARY & VALIDATION STATUS
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reservation_form_live_summary"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CAPTURED RESERVATION SUMMARY",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Surface(
                            color = if (liveValidation.isValid) AvailableGreenBg else MaintenanceRedBg,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                1.dp,
                                if (liveValidation.isValid) AvailableGreen else MaintenanceRed
                            )
                        ) {
                            Text(
                                text = if (liveValidation.isValid) "VALIDATED" else "CHECK FIELDS",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (liveValidation.isValid) AvailableGreen else MaintenanceRed,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .testTag("reservation_form_validation_badge")
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Court ID:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = parsedCourtId?.let { "ID #$it (${resolvedCourt?.name ?: "Court $it"})" } ?: "Not set",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("reservation_form_summary_court_id")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Date:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = resolvedDateOption?.let { "${it.isoDate} (${it.fullLabel})" } ?: dateInput.ifBlank { "Not set" },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("reservation_form_summary_date")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Time Slot:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = resolvedTimeSlotOption?.let { "${it.startTime} (${it.rangeLabel})" } ?: timeSlotInput.ifBlank { "Not set" },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            modifier = Modifier.testTag("reservation_form_summary_time_slot")
                        )
                    }
                }
            }

            // 6. VISUAL LOADING INDICATOR
            if (showLoadingIndicator) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reservation_form_loading_indicator"),
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldDark.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .testTag("reservation_form_loading_spinner"),
                                color = EmeraldPrimary,
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = "Submitting court reservation & locking slot...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark,
                                modifier = Modifier.testTag("reservation_form_loading_text")
                            )
                        }
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .testTag("reservation_form_loading_progress_bar"),
                            color = EmeraldPrimary,
                            trackColor = EmeraldPrimary.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            // 7. VALIDATION ERROR BANNER
            validationError?.let { errMsg ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reservation_form_error_banner"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaintenanceRedBg,
                    border = BorderStroke(1.dp, MaintenanceRed)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Validation Error",
                            tint = MaintenanceRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errMsg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaintenanceRed
                        )
                    }
                }
            }

            // 8. SUCCESS / ERROR SNACKBAR FEEDBACK
            snackbarFeedback?.let { feedback ->
                val specificSnackbarTag = if (feedback.isError) {
                    "reservation_form_error_snackbar"
                } else {
                    "reservation_form_success_snackbar"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(specificSnackbarTag)
                ) {
                    Snackbar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reservation_form_snackbar"),
                        shape = RoundedCornerShape(14.dp),
                        containerColor = if (feedback.isError) MaintenanceRed else EmeraldDark,
                        contentColor = Color.White,
                        actionContentColor = OpticVolt,
                        action = {
                            TextButton(
                                onClick = { snackbarFeedback = null },
                                modifier = Modifier.testTag("reservation_form_snackbar_dismiss")
                            ) {
                                Text(
                                    text = "DISMISS",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = if (feedback.isError) Color.White else OpticVolt
                                )
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (feedback.isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                contentDescription = if (feedback.isError) "Error Snackbar" else "Success Snackbar",
                                tint = if (feedback.isError) Color.White else OpticVolt,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = feedback.message,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.testTag("reservation_form_snackbar_message")
                            )
                        }
                    }
                }
            }

            // 9. CONFIRMATION BANNER ON SUBMISSION
            lastSubmittedInput?.let { submitted ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reservation_form_submitted_banner"),
                    shape = RoundedCornerShape(14.dp),
                    color = AvailableGreenBg,
                    border = BorderStroke(1.5.dp, AvailableGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Reservation Submitted",
                                tint = AvailableGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Reservation Captured & Submitted ✓",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldDark
                                )
                                Text(
                                    text = "Court ID #${submitted.courtId} (${submitted.courtName}) • ${submitted.dateIso} • ${submitted.timeSlot} • ₱${submitted.totalAmount}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.testTag("reservation_form_submitted_details")
                                )
                            }
                        }
                        IconButton(
                            onClick = { lastSubmittedInput = null },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss confirmation",
                                tint = EmeraldDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 10. SUBMIT BUTTON
            Button(
                onClick = {
                    val validation = validateCourtReservationForm(
                        dateInput = dateInput,
                        timeSlotInput = timeSlotInput,
                        courtIdInput = courtIdInput,
                        availableCourts = effectiveCourts,
                        existingBookings = bookings,
                        dateOptions = effectiveDates,
                        timeSlotOptions = effectiveTimeSlots,
                        referenceDateIso = referenceDateIso
                    )

                    courtIdFieldError = validation.courtIdError
                    dateFieldError = validation.dateError
                    timeSlotFieldError = validation.timeSlotError

                    if (!validation.isValid || validation.parsedCourtId == null) {
                        val errText = validation.errorMessage ?: "Please fix the highlighted reservation fields."
                        validationError = errText
                        isSubmittingInternal = false
                        lastSubmittedInput = null
                        snackbarFeedback = ReservationFormSnackbarFeedback(
                            message = "Submission failed: $errText",
                            isError = true
                        )
                        return@Button
                    }

                    val finalCourtId = validation.parsedCourtId
                    val courtObj = effectiveCourts.find { it.id == finalCourtId }
                    val finalCourtName = courtObj?.name ?: "Court $finalCourtId"
                    val matchedDateOpt = effectiveDates.find {
                        it.isoDate.equals(validation.normalizedDateIso, ignoreCase = true)
                    } ?: resolvedDateOption
                    val finalDateIso = matchedDateOpt?.isoDate ?: validation.normalizedDateIso
                    val finalDateLabel = matchedDateOpt?.fullLabel ?: validation.normalizedDateIso
                    val matchedSlotOpt = effectiveTimeSlots.find {
                        it.startTime.equals(validation.normalizedTimeSlot, ignoreCase = true)
                    } ?: resolvedTimeSlotOption
                    val finalTimeSlot = matchedSlotOpt?.startTime ?: validation.normalizedTimeSlot
                    val finalTimeRange = matchedSlotOpt?.rangeLabel ?: "${validation.normalizedTimeSlot} (1 Hour)"
                    val finalPlayer = playerNameInput.trim().ifBlank { "Jonel P." }

                    val captured = CourtReservationFormInput(
                        courtId = finalCourtId,
                        courtName = finalCourtName,
                        dateIso = finalDateIso,
                        dateLabel = finalDateLabel,
                        timeSlot = finalTimeSlot,
                        timeRangeLabel = finalTimeRange,
                        playerName = finalPlayer,
                        paymentMethod = paymentMethodInput,
                        notes = notesInput.trim(),
                        courtFee = calculatedCourtFee,
                        serviceFee = serviceFee,
                        totalAmount = totalAmount
                    )

                    validationError = null
                    isSubmittingInternal = true
                    lastSubmittedInput = captured
                    val persistedBooking = FirestoreReservationRepository.persistReservationFromForm(
                        context = context,
                        input = captured,
                        facility = facility
                    )
                    snackbarFeedback = ReservationFormSnackbarFeedback(
                        message = "Reservation confirmed! $finalCourtName (ID #$finalCourtId) reserved for $finalDateIso at $finalTimeSlot. Persisted to Firestore (${persistedBooking.bookingCode}).",
                        isError = false
                    )
                    onSubmitReservation(captured)

                    coroutineScope.launch {
                        delay(1200)
                        isSubmittingInternal = false
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("reservation_form_submit_button")
            ) {
                if (showLoadingIndicator) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = OpticVolt,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.SportsTennis,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (showLoadingIndicator) {
                        "Submitting Reservation..."
                    } else {
                        "Reserve Court (ID #${parsedCourtId ?: "?"} • ₱$totalAmount)"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Modal Dialog wrapper around [CourtReservationForm] for quick reservation creation from any screen.
 */
@Composable
fun CourtReservationFormDialog(
    facility: FacilityEntity? = null,
    courts: List<CourtEntity> = DefaultReservationFormCourts,
    bookings: List<BookingEntity> = emptyList(),
    dateOptions: List<BookingDateOption> = DefaultReservationFormDates,
    timeSlotOptions: List<TimeSlotOption> = DefaultReservationFormTimeSlots,
    initialDateIso: String = "2026-09-29",
    initialTimeSlot: String = "02:00 PM",
    initialCourtId: Int = 5,
    initialPlayerName: String = "Jonel P.",
    initialPaymentMethod: String = "GCash",
    onSubmitReservation: (CourtReservationFormInput) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .testTag("court_reservation_form_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Court Reservation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_court_reservation_form_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Reservation Form")
                    }
                }
                CourtReservationForm(
                    facility = facility,
                    courts = courts,
                    bookings = bookings,
                    dateOptions = dateOptions,
                    timeSlotOptions = timeSlotOptions,
                    initialDateIso = initialDateIso,
                    initialTimeSlot = initialTimeSlot,
                    initialCourtId = initialCourtId,
                    initialPlayerName = initialPlayerName,
                    initialPaymentMethod = initialPaymentMethod,
                    onSubmitReservation = { input ->
                        onSubmitReservation(input)
                    }
                )
            }
        }
    }
}
