package com.example.data.remote

import android.content.Context
import com.example.data.local.BookingEntity
import com.example.data.local.FacilityEntity
import com.example.data.local.PicklePlayDatabase
import com.example.ui.components.CourtReservationFormInput
import com.example.ui.components.buildBookingFromReservationForm
import com.example.ui.components.parseReservationDateOrNull
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persisted reservation form state stored in Firebase Firestore (`court_reservation_form_state/active_form`)
 * to replace ephemeral in-memory form state handling.
 */
data class PersistedReservationFormState(
    val courtId: Int = 5,
    val courtName: String = "Court 5",
    val dateIso: String = "2026-09-29",
    val dateLabel: String = "September 29, 2026",
    val timeSlot: String = "02:00 PM",
    val timeRangeLabel: String = "2:00 PM - 3:00 PM",
    val playerName: String = "Jonel P.",
    val paymentMethod: String = "GCash",
    val lastSubmittedBookingCode: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Repository managing Firebase Firestore persistence for court reservations (`court_reservations` collection)
 * and reservation form state (`court_reservation_form_state` collection), including document deletion on
 * cancellation and filtering for upcoming 7-day schedules.
 */
object FirestoreReservationRepository {

    const val COLLECTION_COURT_RESERVATIONS = "court_reservations"
    const val COLLECTION_FORM_STATE = "court_reservation_form_state"
    const val DOCUMENT_ACTIVE_FORM_STATE = "active_form"
    const val DEFAULT_PROJECT_ID = "pickleplay-court-reservations"
    const val DEFAULT_REFERENCE_DATE_ISO = "2026-09-28"
    private const val DEFAULT_APP_ID = "1:760107643277:android:094fd62b8df7437b"
    private const val DEFAULT_API_KEY = "AIzaSyPicklePlayFirestoreKey094fd62b8df7437b"

    private const val PREFS_NAME = "pickleplay_firestore_reservations_prefs"
    private const val KEY_RESERVATIONS_JSON = "firestore_reservations_json"
    private const val KEY_REMOVED_CODES_JSON = "firestore_removed_codes_json"
    private const val KEY_FORM_STATE_JSON = "firestore_form_state_json"
    private const val KEY_INITIALIZED = "firestore_seeded_v2"

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var snapshotListenerRegistration: ListenerRegistration? = null
    private var isFirestoreSettingsApplied = false

    val defaultUpcomingSeededReservations: List<BookingEntity> = listOf(
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
            createdAt = 1759052000000L,
            customerEmail = "customer@pickleplay.ph",
            isWalkIn = false
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
            createdAt = 1759051000000L,
            customerEmail = "customer@pickleplay.ph",
            isWalkIn = false,
            adminApprovedAt = "15 mins ago"
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
            createdAt = 1759050000000L,
            customerEmail = "customer@pickleplay.ph",
            isWalkIn = false
        ),
        BookingEntity(
            id = 10,
            bookingCode = "PKL-20261012-00188",
            facilityId = 1,
            facilityName = "Smash Pickle Club",
            facilityLocation = "Quezon City",
            courtId = 2,
            courtName = "Court 2",
            dateIso = "2026-10-12",
            dateLabel = "October 12, 2026",
            timeSlot = "04:00 PM",
            timeRangeLabel = "4:00 PM - 5:00 PM",
            playerName = "Jonel P.",
            courtFee = 350,
            discountAmount = 0,
            serviceFee = 20,
            totalAmount = 370,
            paymentMethod = "GCash",
            paymentStatus = "PAID",
            status = "CONFIRMED",
            createdAt = 1759049000000L,
            customerEmail = "customer@pickleplay.ph",
            isWalkIn = false
        )
    )

    private val _removedDocumentCodes = MutableStateFlow<Set<String>>(emptySet())
    val removedDocumentCodes: StateFlow<Set<String>> = _removedDocumentCodes.asStateFlow()

    private val _removedBookingIds = MutableStateFlow<Set<Int>>(emptySet())
    val removedBookingIds: StateFlow<Set<Int>> = _removedBookingIds.asStateFlow()

    private val _lastCancelledBookingCode = MutableStateFlow<String?>(null)
    val lastCancelledBookingCode: StateFlow<String?> = _lastCancelledBookingCode.asStateFlow()

    private val _reservationsFlow = MutableStateFlow<List<BookingEntity>>(defaultUpcomingSeededReservations)
    val reservationsFlow: StateFlow<List<BookingEntity>> = _reservationsFlow.asStateFlow()

    private val _upcomingReservationsFlow = MutableStateFlow<List<BookingEntity>>(
        filterUpcomingReservations(defaultUpcomingSeededReservations)
    )
    val upcomingReservationsFlow: StateFlow<List<BookingEntity>> = _upcomingReservationsFlow.asStateFlow()

    private val _upcoming7DaysReservationsFlow = MutableStateFlow<List<BookingEntity>>(
        filterUpcoming7DaysReservations(defaultUpcomingSeededReservations)
    )
    val upcoming7DaysReservationsFlow: StateFlow<List<BookingEntity>> = _upcoming7DaysReservationsFlow.asStateFlow()

    private val _persistedFormState = MutableStateFlow(PersistedReservationFormState())
    val persistedFormState: StateFlow<PersistedReservationFormState> = _persistedFormState.asStateFlow()

    private val _lastPersistedBooking = MutableStateFlow<BookingEntity?>(null)
    val lastPersistedBooking: StateFlow<BookingEntity?> = _lastPersistedBooking.asStateFlow()

    private val _isFirestoreConfigured = MutableStateFlow(true)
    val isFirestoreConfigured: StateFlow<Boolean> = _isFirestoreConfigured.asStateFlow()

    /**
     * Initializes FirebaseApp and configures FirebaseFirestore instance with local cache settings
     * and real-time collection snapshot listeners.
     */
    fun ensureFirestoreInstance(context: Context? = null): FirebaseFirestore? {
        return runCatching {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(DEFAULT_PROJECT_ID)
                    .setApplicationId(DEFAULT_APP_ID)
                    .setApiKey(DEFAULT_API_KEY)
                    .build()
                FirebaseApp.initializeApp(context.applicationContext ?: context, options)
            }
            val firestore = FirebaseFirestore.getInstance()
            if (!isFirestoreSettingsApplied) {
                runCatching {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                        .build()
                    firestore.firestoreSettings = settings
                }
                isFirestoreSettingsApplied = true
            }
            _isFirestoreConfigured.value = true
            if (context != null) {
                loadFromLocalMirrorIfAvailable(context)
            }
            attachRealtimeListenerIfNeeded(firestore, context)
            firestore
        }.getOrElse {
            if (context != null) {
                loadFromLocalMirrorIfAvailable(context)
            }
            null
        }
    }

    private fun attachRealtimeListenerIfNeeded(firestore: FirebaseFirestore, context: Context?) {
        if (snapshotListenerRegistration != null) return
        runCatching {
            snapshotListenerRegistration = firestore
                .collection(COLLECTION_COURT_RESERVATIONS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val remoteDocs = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        fromFirestoreDocumentMap(data, doc.id)
                    }
                    if (remoteDocs.isNotEmpty()) {
                        mergeAndPublishReservations(remoteDocs, context)
                    }
                }
        }
    }

    /**
     * Converts a [BookingEntity] into a Firestore document map for persistence in `court_reservations`.
     */
    fun toFirestoreDocumentMap(booking: BookingEntity): Map<String, Any?> {
        return mapOf(
            "id" to booking.id,
            "bookingCode" to booking.bookingCode,
            "facilityId" to booking.facilityId,
            "facilityName" to booking.facilityName,
            "facilityLocation" to booking.facilityLocation,
            "courtId" to booking.courtId,
            "courtName" to booking.courtName,
            "dateIso" to booking.dateIso,
            "dateLabel" to booking.dateLabel,
            "timeSlot" to booking.timeSlot,
            "timeRangeLabel" to booking.timeRangeLabel,
            "playerName" to booking.playerName,
            "courtFee" to booking.courtFee,
            "discountAmount" to booking.discountAmount,
            "serviceFee" to booking.serviceFee,
            "totalAmount" to booking.totalAmount,
            "paymentMethod" to booking.paymentMethod,
            "paymentStatus" to booking.paymentStatus,
            "status" to booking.status,
            "checkInTime" to booking.checkInTime,
            "userRating" to booking.userRating,
            "userReview" to booking.userReview,
            "createdAt" to booking.createdAt,
            "customerEmail" to booking.customerEmail,
            "isWalkIn" to booking.isWalkIn,
            "referenceNumber" to booking.referenceNumber,
            "receiptUri" to booking.receiptUri,
            "receiptFileName" to booking.receiptFileName,
            "adminApprovedAt" to booking.adminApprovedAt,
            "cashierVerifiedAt" to booking.cashierVerifiedAt,
            "isUpcomingBooking" to booking.isUpcomingBooking
        )
    }

    /**
     * Reconstructs a [BookingEntity] from a Firebase Firestore document map.
     */
    fun fromFirestoreDocumentMap(map: Map<String, Any?>, documentId: String = ""): BookingEntity? {
        val code = (map["bookingCode"] as? String)?.takeIf { it.isNotBlank() }
            ?: documentId.takeIf { it.isNotBlank() }
            ?: return null
        val courtId = (map["courtId"] as? Number)?.toInt() ?: 1
        val dateIso = (map["dateIso"] as? String) ?: "2026-09-29"
        val timeSlot = (map["timeSlot"] as? String) ?: "02:00 PM"
        return BookingEntity(
            id = (map["id"] as? Number)?.toInt() ?: (code.hashCode() and 0x7FFFFFFF),
            bookingCode = code,
            facilityId = (map["facilityId"] as? Number)?.toInt() ?: 1,
            facilityName = (map["facilityName"] as? String) ?: "Smash Pickle Club",
            facilityLocation = (map["facilityLocation"] as? String) ?: "Quezon City",
            courtId = courtId,
            courtName = (map["courtName"] as? String) ?: "Court $courtId",
            dateIso = dateIso,
            dateLabel = (map["dateLabel"] as? String) ?: dateIso,
            timeSlot = timeSlot,
            timeRangeLabel = (map["timeRangeLabel"] as? String) ?: timeSlot,
            playerName = (map["playerName"] as? String) ?: "Jonel P.",
            courtFee = (map["courtFee"] as? Number)?.toInt() ?: 250,
            discountAmount = (map["discountAmount"] as? Number)?.toInt() ?: 0,
            serviceFee = (map["serviceFee"] as? Number)?.toInt() ?: 20,
            totalAmount = (map["totalAmount"] as? Number)?.toInt() ?: 270,
            paymentMethod = (map["paymentMethod"] as? String) ?: "GCash",
            paymentStatus = (map["paymentStatus"] as? String) ?: "AWAITING_APPROVAL",
            status = (map["status"] as? String) ?: "PENDING_ADMIN_APPROVAL",
            checkInTime = map["checkInTime"] as? String,
            userRating = (map["userRating"] as? Number)?.toInt(),
            userReview = map["userReview"] as? String,
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            customerEmail = (map["customerEmail"] as? String) ?: "customer@pickleplay.ph",
            isWalkIn = (map["isWalkIn"] as? Boolean) ?: false,
            referenceNumber = map["referenceNumber"] as? String,
            receiptUri = map["receiptUri"] as? String,
            receiptFileName = map["receiptFileName"] as? String,
            adminApprovedAt = map["adminApprovedAt"] as? String,
            cashierVerifiedAt = map["cashierVerifiedAt"] as? String
        )
    }

    /**
     * Persists a reservation submitted from [com.example.ui.components.CourtReservationForm]
     * into Firebase Firestore (`court_reservations` collection) and updates persisted form state.
     */
    fun persistReservationFromForm(
        context: Context?,
        input: CourtReservationFormInput,
        facility: FacilityEntity? = null,
        bookingCodeOverride: String? = null
    ): BookingEntity {
        val currentList = _reservationsFlow.value
        val nextId = (currentList.maxOfOrNull { it.id } ?: 100) + 1
        val booking = buildBookingFromReservationForm(
            input = input,
            facility = facility,
            newId = nextId,
            bookingCodeOverride = bookingCodeOverride
        )
        saveReservation(context, booking)
        saveFormStateToFirestore(
            context = context,
            state = PersistedReservationFormState(
                courtId = input.courtId,
                courtName = input.courtName,
                dateIso = input.dateIso,
                dateLabel = input.dateLabel,
                timeSlot = input.timeSlot,
                timeRangeLabel = input.timeRangeLabel,
                playerName = input.playerName,
                paymentMethod = input.paymentMethod,
                lastSubmittedBookingCode = booking.bookingCode,
                updatedAt = System.currentTimeMillis()
            )
        )
        _lastPersistedBooking.value = booking
        com.example.notifications.FcmPushNotificationManager.sendUpcomingReservationPush(
            context = context,
            booking = booking,
            reminderLeadText = "Scheduled • ${booking.dateLabel} at ${booking.timeSlot}"
        )
        return booking
    }

    /**
     * Saves or updates a [BookingEntity] in Firebase Firestore (`court_reservations/{bookingCode}`),
     * local mirror store, and Room database.
     */
    fun saveReservation(context: Context?, booking: BookingEntity): BookingEntity {
        _removedDocumentCodes.value = _removedDocumentCodes.value - booking.bookingCode.uppercase()
        if (booking.id != 0) {
            _removedBookingIds.value = _removedBookingIds.value - booking.id
        }

        val docMap = toFirestoreDocumentMap(booking)
        val firestore = ensureFirestoreInstance(context)
        runCatching {
            firestore?.collection(COLLECTION_COURT_RESERVATIONS)
                ?.document(booking.bookingCode)
                ?.set(docMap, SetOptions.merge())
        }

        val updatedList = buildList {
            add(booking)
            addAll(
                _reservationsFlow.value.filterNot { existing ->
                    existing.bookingCode.equals(booking.bookingCode, ignoreCase = true) ||
                        (existing.id == booking.id && booking.id != 0)
                }
            )
        }
        publishReservations(updatedList, context)
        _lastPersistedBooking.value = booking

        if (context != null) {
            repositoryScope.launch {
                runCatching {
                    val dao = PicklePlayDatabase.getDatabase(context).picklePlayDao()
                    dao.insertBooking(booking)
                }
            }
        }
        return booking
    }

    /**
     * Syncs a batch of bookings (e.g., from Room seed or repository updates) with Firebase Firestore.
     */
    fun syncBookingsList(context: Context?, bookings: List<BookingEntity>) {
        if (bookings.isEmpty()) return
        val firestore = ensureFirestoreInstance(context)
        val activeBookings = bookings.filterNot { isDocumentRemoved(it.bookingCode, it.id) }
        runCatching {
            activeBookings.forEach { b ->
                firestore?.collection(COLLECTION_COURT_RESERVATIONS)
                    ?.document(b.bookingCode)
                    ?.set(toFirestoreDocumentMap(b), SetOptions.merge())
            }
        }
        mergeAndPublishReservations(activeBookings, context)
    }

    /**
     * Persists the current reservation form state in Firebase Firestore (`court_reservation_form_state/active_form`).
     */
    fun saveFormStateToFirestore(context: Context?, state: PersistedReservationFormState) {
        _persistedFormState.value = state
        val firestore = ensureFirestoreInstance(context)
        val stateMap = mapOf(
            "courtId" to state.courtId,
            "courtName" to state.courtName,
            "dateIso" to state.dateIso,
            "dateLabel" to state.dateLabel,
            "timeSlot" to state.timeSlot,
            "timeRangeLabel" to state.timeRangeLabel,
            "playerName" to state.playerName,
            "paymentMethod" to state.paymentMethod,
            "lastSubmittedBookingCode" to state.lastSubmittedBookingCode,
            "updatedAt" to state.updatedAt
        )
        runCatching {
            firestore?.collection(COLLECTION_FORM_STATE)
                ?.document(DOCUMENT_ACTIVE_FORM_STATE)
                ?.set(stateMap, SetOptions.merge())
        }
        if (context != null) {
            runCatching {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val json = JSONObject().apply {
                    put("courtId", state.courtId)
                    put("courtName", state.courtName)
                    put("dateIso", state.dateIso)
                    put("dateLabel", state.dateLabel)
                    put("timeSlot", state.timeSlot)
                    put("timeRangeLabel", state.timeRangeLabel)
                    put("playerName", state.playerName)
                    put("paymentMethod", state.paymentMethod)
                    put("lastSubmittedBookingCode", state.lastSubmittedBookingCode ?: "")
                    put("updatedAt", state.updatedAt)
                }
                prefs.edit().putString(KEY_FORM_STATE_JSON, json.toString()).apply()
            }
        }
    }

    /**
     * Removes a reservation document from Firebase Firestore (`court_reservations/{bookingCode}`)
     * and removes the corresponding reservation from local and Room state.
     */
    fun removeReservationFromFirestore(context: Context?, bookingCode: String, bookingId: Int = 0) {
        val matchedExisting = _reservationsFlow.value.find {
            (bookingCode.isNotBlank() && it.bookingCode.equals(bookingCode, ignoreCase = true)) ||
                (bookingId != 0 && it.id == bookingId)
        }
        val resolvedCode = bookingCode.ifBlank { matchedExisting?.bookingCode.orEmpty() }
        val resolvedId = if (bookingId != 0) bookingId else (matchedExisting?.id ?: 0)

        if (resolvedCode.isNotBlank()) {
            _removedDocumentCodes.value = _removedDocumentCodes.value + resolvedCode.uppercase()
            _lastCancelledBookingCode.value = resolvedCode
        }
        if (resolvedId != 0) {
            _removedBookingIds.value = _removedBookingIds.value + resolvedId
        }

        val firestore = ensureFirestoreInstance(context)
        runCatching {
            if (resolvedCode.isNotBlank()) {
                firestore?.collection(COLLECTION_COURT_RESERVATIONS)
                    ?.document(resolvedCode)
                    ?.delete()
            }
            if (resolvedId != 0) {
                firestore?.collection(COLLECTION_COURT_RESERVATIONS)
                    ?.document(resolvedId.toString())
                    ?.delete()
            }
        }

        val filtered = _reservationsFlow.value.filterNot {
            (resolvedCode.isNotBlank() && it.bookingCode.equals(resolvedCode, ignoreCase = true)) ||
                (resolvedId != 0 && it.id == resolvedId)
        }
        if (_lastPersistedBooking.value?.bookingCode?.equals(resolvedCode, ignoreCase = true) == true ||
            (resolvedId != 0 && _lastPersistedBooking.value?.id == resolvedId)
        ) {
            _lastPersistedBooking.value = null
        }
        publishReservations(filtered, context)

        if (context != null && resolvedId != 0) {
            repositoryScope.launch {
                runCatching {
                    val dao = PicklePlayDatabase.getDatabase(context).picklePlayDao()
                    dao.deleteBookingById(resolvedId)
                }
            }
        }
    }

    /**
     * Checks whether a given reservation document has been removed from Firestore.
     */
    fun isDocumentRemoved(bookingCode: String, bookingId: Int = 0): Boolean {
        val codeRemoved = bookingCode.isNotBlank() && _removedDocumentCodes.value.contains(bookingCode.uppercase())
        val idRemoved = bookingId != 0 && _removedBookingIds.value.contains(bookingId)
        return codeRemoved || idRemoved
    }

    /**
     * Checks whether a reservation document currently exists in the persisted Firestore reservation flow.
     */
    fun hasReservationDocument(bookingCode: String): Boolean {
        if (bookingCode.isBlank()) return false
        if (isDocumentRemoved(bookingCode)) return false
        return _reservationsFlow.value.any { it.bookingCode.equals(bookingCode, ignoreCase = true) }
    }

    /**
     * Checks whether [dateIso] falls within the upcoming 7 days window
     * (`[referenceDate, referenceDate + 7 days]` inclusive).
     */
    fun isDateWithinUpcoming7Days(
        dateIso: String,
        referenceDateIso: String = DEFAULT_REFERENCE_DATE_ISO
    ): Boolean {
        val referenceDate = parseReservationDateOrNull(referenceDateIso)
            ?: LocalDate.of(2026, 9, 28)
        val bookingDate = parseReservationDateOrNull(dateIso) ?: return false
        val endOfWindow = referenceDate.plusDays(7)
        return !bookingDate.isBefore(referenceDate) && !bookingDate.isAfter(endOfWindow)
    }

    /**
     * Filters reservations to only those scheduled within the upcoming 7 days
     * (`[referenceDateIso, referenceDateIso + 7 days]` inclusive) that are not cancelled.
     */
    fun filterUpcoming7DaysReservations(
        bookings: List<BookingEntity>,
        referenceDateIso: String = DEFAULT_REFERENCE_DATE_ISO
    ): List<BookingEntity> {
        return bookings
            .filter { booking ->
                booking.status != "CANCELLED" &&
                    !isDocumentRemoved(booking.bookingCode, booking.id) &&
                    isDateWithinUpcoming7Days(booking.dateIso, referenceDateIso)
            }
            .distinctBy { it.bookingCode.uppercase() }
            .sortedWith(
                compareBy<BookingEntity> { it.dateIso }
                    .thenBy { it.timeSlot }
                    .thenByDescending { it.createdAt }
            )
    }

    /**
     * Filters and sorts upcoming court reservations so the user can view their upcoming scheduled slots.
     */
    fun filterUpcomingReservations(bookings: List<BookingEntity>): List<BookingEntity> {
        return bookings
            .filter { it.isUpcomingBooking && !isDocumentRemoved(it.bookingCode, it.id) }
            .distinctBy { it.bookingCode.uppercase() }
            .sortedWith(
                compareByDescending<BookingEntity> { it.createdAt }
                    .thenBy { it.dateIso }
                    .thenBy { it.timeSlot }
            )
    }

    /**
     * Combines passed bookings with Firestore-persisted reservations without duplicates,
     * excluding any documents that were cancelled/removed from Firestore.
     */
    fun mergeBookingsWithFirestore(
        passedBookings: List<BookingEntity>,
        firestoreBookings: List<BookingEntity> = _reservationsFlow.value
    ): List<BookingEntity> {
        val byCode = linkedMapOf<String, BookingEntity>()
        firestoreBookings.forEach { b ->
            if (!isDocumentRemoved(b.bookingCode, b.id) && b.status != "CANCELLED") {
                byCode[b.bookingCode.uppercase()] = b
            }
        }
        passedBookings.forEach { b ->
            if (!isDocumentRemoved(b.bookingCode, b.id) && b.status != "CANCELLED") {
                val key = b.bookingCode.uppercase()
                if (!byCode.containsKey(key)) {
                    byCode[key] = b
                } else {
                    val existing = byCode[key]!!
                    if (b.status != existing.status && b.status != "PENDING_ADMIN_APPROVAL") {
                        byCode[key] = b
                    }
                }
            }
        }
        return byCode.values.toList()
    }

    /**
     * Convenience overload to initialize Firebase Firestore instance.
     */
    fun initialize(context: Context? = null): FirebaseFirestore? {
        return ensureFirestoreInstance(context)
    }

    /**
     * Convenience overload to remove a [BookingEntity] document from Firebase Firestore.
     */
    fun removeReservationFromFirestore(context: Context?, booking: BookingEntity) {
        removeReservationFromFirestore(
            context = context,
            bookingCode = booking.bookingCode,
            bookingId = booking.id
        )
    }

    /**
     * Convenience overload to sync a list of [BookingEntity] items to Firebase Firestore.
     */
    fun syncReservationsToFirestore(context: Context?, bookings: List<BookingEntity>) {
        syncBookingsList(context, bookings)
    }

    /**
     * Clears repository state to an empty reservation list for isolated unit/Robolectric tests.
     */
    fun clearForTesting(context: Context? = null) {
        _removedDocumentCodes.value = emptySet()
        _removedBookingIds.value = emptySet()
        _lastCancelledBookingCode.value = null
        _lastPersistedBooking.value = null
        _persistedFormState.value = PersistedReservationFormState()
        if (context != null) {
            runCatching {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
            }
        }
        publishReservations(emptyList(), context)
    }

    /**
     * Resets repository state to default seeded reservations (useful for deterministic test setups).
     */
    fun resetToDefaultSeeded(context: Context? = null) {
        _removedDocumentCodes.value = emptySet()
        _removedBookingIds.value = emptySet()
        _lastCancelledBookingCode.value = null
        _lastPersistedBooking.value = null
        _persistedFormState.value = PersistedReservationFormState()
        if (context != null) {
            runCatching {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
            }
        }
        publishReservations(defaultUpcomingSeededReservations, context)
    }

    private fun mergeAndPublishReservations(incoming: List<BookingEntity>, context: Context?) {
        val merged = mergeBookingsWithFirestore(
            passedBookings = _reservationsFlow.value,
            firestoreBookings = incoming
        )
        publishReservations(merged, context)
    }

    private fun publishReservations(list: List<BookingEntity>, context: Context?) {
        val deduplicated = list
            .filterNot { isDocumentRemoved(it.bookingCode, it.id) }
            .distinctBy { it.bookingCode.uppercase() }
        _reservationsFlow.value = deduplicated
        _upcomingReservationsFlow.value = filterUpcomingReservations(deduplicated)
        _upcoming7DaysReservationsFlow.value = filterUpcoming7DaysReservations(deduplicated)
        if (context != null) {
            saveToLocalMirror(context, deduplicated)
        }
    }

    private fun saveToLocalMirror(context: Context, list: List<BookingEntity>) {
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val arr = JSONArray()
            list.forEach { b ->
                val obj = JSONObject()
                toFirestoreDocumentMap(b).forEach { (k, v) ->
                    if (v != null) obj.put(k, v)
                }
                arr.put(obj)
            }
            val removedArr = JSONArray()
            _removedDocumentCodes.value.forEach { removedArr.put(it) }
            prefs.edit()
                .putBoolean(KEY_INITIALIZED, true)
                .putString(KEY_RESERVATIONS_JSON, arr.toString())
                .putString(KEY_REMOVED_CODES_JSON, removedArr.toString())
                .apply()
        }
    }

    fun loadFromLocalMirrorIfAvailable(context: Context): List<BookingEntity> {
        return runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val rawRemoved = prefs.getString(KEY_REMOVED_CODES_JSON, null)
            if (!rawRemoved.isNullOrBlank()) {
                val rArr = JSONArray(rawRemoved)
                val codes = mutableSetOf<String>()
                for (i in 0 until rArr.length()) {
                    val code = rArr.optString(i)
                    if (code.isNotBlank()) codes.add(code.uppercase())
                }
                _removedDocumentCodes.value = _removedDocumentCodes.value + codes
            }

            val rawForm = prefs.getString(KEY_FORM_STATE_JSON, null)
            if (!rawForm.isNullOrBlank()) {
                val formObj = JSONObject(rawForm)
                _persistedFormState.value = PersistedReservationFormState(
                    courtId = formObj.optInt("courtId", 5),
                    courtName = formObj.optString("courtName", "Court 5"),
                    dateIso = formObj.optString("dateIso", "2026-09-29"),
                    dateLabel = formObj.optString("dateLabel", "September 29, 2026"),
                    timeSlot = formObj.optString("timeSlot", "02:00 PM"),
                    timeRangeLabel = formObj.optString("timeRangeLabel", "2:00 PM - 3:00 PM"),
                    playerName = formObj.optString("playerName", "Jonel P."),
                    paymentMethod = formObj.optString("paymentMethod", "GCash"),
                    lastSubmittedBookingCode = formObj.optString("lastSubmittedBookingCode").takeIf { it.isNotBlank() },
                    updatedAt = formObj.optLong("updatedAt", System.currentTimeMillis())
                )
            }

            if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
                saveToLocalMirror(context, _reservationsFlow.value)
                return _reservationsFlow.value
            }
            val rawJson = prefs.getString(KEY_RESERVATIONS_JSON, null) ?: return _reservationsFlow.value
            val arr = JSONArray(rawJson)
            val parsed = mutableListOf<BookingEntity>()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val map = mutableMapOf<String, Any?>()
                val keys = item.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = item.opt(key)
                }
                fromFirestoreDocumentMap(map)?.let {
                    if (!isDocumentRemoved(it.bookingCode, it.id)) {
                        parsed.add(it)
                    }
                }
            }
            val merged = mergeBookingsWithFirestore(emptyList(), parsed)
            _reservationsFlow.value = merged
            _upcomingReservationsFlow.value = filterUpcomingReservations(merged)
            _upcoming7DaysReservationsFlow.value = filterUpcoming7DaysReservations(merged)
            _reservationsFlow.value
        }.getOrElse { _reservationsFlow.value }
    }
}
