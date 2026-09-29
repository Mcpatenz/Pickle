package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookingEntity
import com.example.data.local.BusinessSettingsEntity
import com.example.data.local.CourtEntity
import com.example.data.local.FacilityEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.OpenPlayGameEntity
import com.example.data.local.PicklePlayDatabase
import com.example.data.local.TournamentEntity
import com.example.data.local.UserProfileEntity
import com.example.data.remote.FirestoreReservationRepository
import com.example.data.remote.FirestoreTournamentsAndMatchmakingRepository
import com.example.data.repository.PicklePlayRepository
import com.example.notifications.LocalReservationNotificationService
import com.example.notifications.ReservationStatusLocalAlert
import com.example.ui.components.CourtReservationFormInput
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppSection {
    HOME, CUSTOMER_DASHBOARD, MY_RESERVATIONS, COURTS, COURT_OVERVIEW, GAMES, PROFILE, CASHIER, ADMIN
}

enum class CourtOccupancyStatus(val displayLabel: String) {
    BOOKED("Already Booked"),
    IN_USE("Already In Use"),
    AVAILABLE("Available")
}

data class BookingDateOption(
    val isoDate: String,
    val dayShort: String,
    val dayNumber: String,
    val fullLabel: String,
    val isWeekend: Boolean
)

data class TimeSlotOption(
    val startTime: String,
    val rangeLabel: String,
    val periodTier: String, // "MORNING", "MIDDAY", "PEAK", "NIGHT"
    val defaultBookedCourts: Set<Int> = emptySet()
)

data class ReservationDurationOption(
    val id: String,
    val label: String,
    val subtitle: String,
    val durationSeconds: Int,
    val hoursMultiplier: Double
)

data class ClubProductItem(
    val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val stock: Int = 25
)

data class BusinessPaymentSettings(
    val businessName: String = "Smash Pickle Club",
    val address: String = "Tomas Morato Ave, Quezon City",
    val contactNumber: String = "+63 917 882 4500",
    val gcashQrUploaded: Boolean = true,
    val gcashQrUri: String? = "file://local/gcash_smash_pickle_qr.png",
    val gcashFileName: String = "gcash_smash_pickle_qr.png",
    val gcashAccountName: String = "Smash Pickle Club",
    val gcashMerchantCode: String = "GCASH-PH-8824500",
    val paymayaQrUploaded: Boolean = true,
    val paymayaQrUri: String? = "file://local/paymaya_smash_pickle_qr.png",
    val paymayaFileName: String = "paymaya_smash_pickle_qr.png",
    val paymayaAccountName: String = "Smash Pickle Club",
    val paymayaMerchantCode: String = "PAYMAYA-PH-8824500",
    val lastSavedTimestamp: Long = 0L
) {
    val businessAddress: String
        get() = address

    val gcashQrFileName: String
        get() = gcashFileName

    val paymayaQrFileName: String
        get() = paymayaFileName

    val lastSavedAt: Long
        get() = lastSavedTimestamp

    fun toEntity(): BusinessSettingsEntity = BusinessSettingsEntity(
        id = 1,
        businessName = businessName,
        address = address,
        contactNumber = contactNumber,
        gcashQrUploaded = gcashQrUploaded,
        gcashQrUri = gcashQrUri,
        gcashFileName = gcashFileName,
        gcashAccountName = gcashAccountName,
        gcashMerchantCode = gcashMerchantCode,
        paymayaQrUploaded = paymayaQrUploaded,
        paymayaQrUri = paymayaQrUri,
        paymayaFileName = paymayaFileName,
        paymayaAccountName = paymayaAccountName,
        paymayaMerchantCode = paymayaMerchantCode,
        updatedAt = if (lastSavedTimestamp > 0L) lastSavedTimestamp else System.currentTimeMillis()
    )

    companion object {
        fun fromEntity(entity: BusinessSettingsEntity): BusinessPaymentSettings = BusinessPaymentSettings(
            businessName = entity.businessName,
            address = entity.address,
            contactNumber = entity.contactNumber,
            gcashQrUploaded = entity.gcashQrUploaded,
            gcashQrUri = entity.gcashQrUri,
            gcashFileName = entity.gcashFileName,
            gcashAccountName = entity.gcashAccountName,
            gcashMerchantCode = entity.gcashMerchantCode,
            paymayaQrUploaded = entity.paymayaQrUploaded,
            paymayaQrUri = entity.paymayaQrUri,
            paymayaFileName = entity.paymayaFileName,
            paymayaAccountName = entity.paymayaAccountName,
            paymayaMerchantCode = entity.paymayaMerchantCode,
            lastSavedTimestamp = entity.updatedAt
        )
    }
}

data class StoredBusinessQrCode(
    val paymentMethod: String,
    val businessName: String,
    val businessAddress: String,
    val contactNumber: String,
    val merchantCode: String,
    val qrUri: String?,
    val qrFileName: String,
    val isUploaded: Boolean,
    val bitmap: Bitmap
)

object BusinessSettingsLocalStore {
    private const val PREFS_NAME = "pickleplay_business_settings_prefs"
    private const val KEY_HAS_SAVED = "has_saved_settings"
    private const val KEY_BUSINESS_NAME = "business_name"
    private const val KEY_ADDRESS = "business_address"
    private const val KEY_CONTACT = "contact_number"
    private const val KEY_GCASH_UPLOADED = "gcash_qr_uploaded"
    private const val KEY_GCASH_URI = "gcash_qr_uri"
    private const val KEY_GCASH_FILENAME = "gcash_file_name"
    private const val KEY_GCASH_MERCHANT = "gcash_merchant_code"
    private const val KEY_PAYMAYA_UPLOADED = "paymaya_qr_uploaded"
    private const val KEY_PAYMAYA_URI = "paymaya_qr_uri"
    private const val KEY_PAYMAYA_FILENAME = "paymaya_file_name"
    private const val KEY_PAYMAYA_MERCHANT = "paymaya_merchant_code"
    private const val KEY_UPDATED_AT = "updated_at"

    fun save(context: Context, settings: BusinessPaymentSettings): BusinessPaymentSettings {
        val timestamp = if (settings.lastSavedTimestamp > 0L) settings.lastSavedTimestamp else System.currentTimeMillis()
        val updated = settings.copy(lastSavedTimestamp = timestamp)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_HAS_SAVED, true)
            .putString(KEY_BUSINESS_NAME, updated.businessName)
            .putString(KEY_ADDRESS, updated.address)
            .putString(KEY_CONTACT, updated.contactNumber)
            .putBoolean(KEY_GCASH_UPLOADED, updated.gcashQrUploaded)
            .putString(KEY_GCASH_URI, updated.gcashQrUri)
            .putString(KEY_GCASH_FILENAME, updated.gcashFileName)
            .putString(KEY_GCASH_MERCHANT, updated.gcashMerchantCode)
            .putBoolean(KEY_PAYMAYA_UPLOADED, updated.paymayaQrUploaded)
            .putString(KEY_PAYMAYA_URI, updated.paymayaQrUri)
            .putString(KEY_PAYMAYA_FILENAME, updated.paymayaFileName)
            .putString(KEY_PAYMAYA_MERCHANT, updated.paymayaMerchantCode)
            .putLong(KEY_UPDATED_AT, timestamp)
            .apply()

        runCatching {
            ensureLocalQrBitmapFile(
                context = context,
                fileName = updated.gcashFileName,
                seed = "GCash|${updated.businessName}|${updated.contactNumber}|${updated.gcashFileName}|${updated.gcashQrUri}",
                isGcash = true
            )
            ensureLocalQrBitmapFile(
                context = context,
                fileName = updated.paymayaFileName,
                seed = "PayMaya|${updated.businessName}|${updated.contactNumber}|${updated.paymayaFileName}|${updated.paymayaQrUri}",
                isGcash = false
            )
        }
        return updated
    }

    fun hasSaved(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAS_SAVED, false)
    }

    fun load(context: Context, fallback: BusinessPaymentSettings = BusinessPaymentSettings()): BusinessPaymentSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_HAS_SAVED, false)) {
            return fallback
        }
        val name = prefs.getString(KEY_BUSINESS_NAME, fallback.businessName) ?: fallback.businessName
        val addr = prefs.getString(KEY_ADDRESS, fallback.address) ?: fallback.address
        val contact = prefs.getString(KEY_CONTACT, fallback.contactNumber) ?: fallback.contactNumber
        return BusinessPaymentSettings(
            businessName = name,
            address = addr,
            contactNumber = contact,
            gcashQrUploaded = prefs.getBoolean(KEY_GCASH_UPLOADED, fallback.gcashQrUploaded),
            gcashQrUri = prefs.getString(KEY_GCASH_URI, fallback.gcashQrUri),
            gcashFileName = prefs.getString(KEY_GCASH_FILENAME, fallback.gcashFileName) ?: fallback.gcashFileName,
            gcashAccountName = name,
            gcashMerchantCode = prefs.getString(KEY_GCASH_MERCHANT, fallback.gcashMerchantCode) ?: fallback.gcashMerchantCode,
            paymayaQrUploaded = prefs.getBoolean(KEY_PAYMAYA_UPLOADED, fallback.paymayaQrUploaded),
            paymayaQrUri = prefs.getString(KEY_PAYMAYA_URI, fallback.paymayaQrUri),
            paymayaFileName = prefs.getString(KEY_PAYMAYA_FILENAME, fallback.paymayaFileName) ?: fallback.paymayaFileName,
            paymayaAccountName = name,
            paymayaMerchantCode = prefs.getString(KEY_PAYMAYA_MERCHANT, fallback.paymayaMerchantCode) ?: fallback.paymayaMerchantCode,
            lastSavedTimestamp = prefs.getLong(KEY_UPDATED_AT, fallback.lastSavedTimestamp)
        )
    }

    fun resolveSettings(
        context: Context,
        provided: BusinessPaymentSettings = BusinessPaymentSettings()
    ): BusinessPaymentSettings {
        val localStored = load(context, fallback = provided)
        return if (
            provided != BusinessPaymentSettings() &&
            (!hasSaved(context) || provided.lastSavedTimestamp >= localStored.lastSavedTimestamp)
        ) {
            provided
        } else {
            localStored
        }
    }

    fun getStoredQrCode(
        context: Context,
        paymentMethod: String,
        fallback: BusinessPaymentSettings = BusinessPaymentSettings()
    ): StoredBusinessQrCode {
        val resolved = resolveSettings(context, fallback)
        val isGcash = paymentMethod.equals("GCash", ignoreCase = true)
        val normalizedMethod = if (isGcash) "GCash" else "PayMaya"
        val qrUri = if (isGcash) resolved.gcashQrUri else resolved.paymayaQrUri
        val qrFileName = if (isGcash) resolved.gcashFileName else resolved.paymayaFileName
        val merchantCode = if (isGcash) resolved.gcashMerchantCode else resolved.paymayaMerchantCode
        val isUploaded = if (isGcash) resolved.gcashQrUploaded else resolved.paymayaQrUploaded
        val seed = "$normalizedMethod|${resolved.businessName}|${resolved.contactNumber}|$qrFileName|${qrUri ?: "OFFICIAL"}"

        val decodedBitmap = loadBitmapFromUriOrLocalFile(context, qrUri, qrFileName)
            ?: generateQrBitmap(seedString = seed, isGcash = isGcash)

        return StoredBusinessQrCode(
            paymentMethod = normalizedMethod,
            businessName = resolved.businessName,
            businessAddress = resolved.businessAddress,
            contactNumber = resolved.contactNumber,
            merchantCode = merchantCode,
            qrUri = qrUri,
            qrFileName = qrFileName,
            isUploaded = isUploaded,
            bitmap = decodedBitmap
        )
    }

    private fun loadBitmapFromUriOrLocalFile(
        context: Context,
        qrUri: String?,
        qrFileName: String
    ): Bitmap? {
        if (!qrUri.isNullOrBlank()) {
            val fromUri = runCatching {
                when {
                    qrUri.startsWith("content://") -> {
                        context.contentResolver.openInputStream(Uri.parse(qrUri))?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                    qrUri.startsWith("file://") -> {
                        val path = Uri.parse(qrUri).path
                        if (!path.isNullOrBlank() && File(path).exists()) {
                            BitmapFactory.decodeFile(path)
                        } else null
                    }
                    File(qrUri).exists() -> {
                        BitmapFactory.decodeFile(qrUri)
                    }
                    else -> null
                }
            }.getOrNull()
            if (fromUri != null) return fromUri
        }
        if (qrFileName.isNotBlank()) {
            val localFile = File(context.filesDir, qrFileName.substringAfterLast('/'))
            if (localFile.exists()) {
                val fromLocalFile = runCatching { BitmapFactory.decodeFile(localFile.absolutePath) }.getOrNull()
                if (fromLocalFile != null) return fromLocalFile
            }
        }
        return null
    }

    private fun ensureLocalQrBitmapFile(
        context: Context,
        fileName: String,
        seed: String,
        isGcash: Boolean
    ): File {
        val cleanName = fileName.substringAfterLast('/').ifBlank {
            if (isGcash) "gcash_smash_pickle_qr.png" else "paymaya_smash_pickle_qr.png"
        }
        val targetFile = File(context.filesDir, cleanName)
        if (!targetFile.exists() || targetFile.length() == 0L) {
            val bmp = generateQrBitmap(seedString = seed, isGcash = isGcash)
            runCatching {
                targetFile.outputStream().use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
        return targetFile
    }

    fun generateQrBitmap(seedString: String, isGcash: Boolean): Bitmap {
        val sizePx = 210
        val gridSize = 21
        val cellPx = sizePx / gridSize
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(AndroidColor.WHITE)

        val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isGcash) AndroidColor.rgb(0, 87, 231) else AndroidColor.rgb(6, 78, 59)
            style = Paint.Style.FILL
        }
        val seed = seedString.hashCode()

        fun isFinderPattern(r: Int, c: Int): Boolean {
            val topLeft = r in 0..6 && c in 0..6
            val topRight = r in 0..6 && c in (gridSize - 7) until gridSize
            val bottomLeft = r in (gridSize - 7) until gridSize && c in 0..6
            return topLeft || topRight || bottomLeft
        }

        fun isFinderDark(r: Int, c: Int): Boolean {
            val lr = when {
                r in 0..6 -> r
                r in (gridSize - 7) until gridSize -> r - (gridSize - 7)
                else -> -1
            }
            val lc = when {
                c in 0..6 -> c
                c in (gridSize - 7) until gridSize -> c - (gridSize - 7)
                else -> -1
            }
            if (lr == -1 || lc == -1) return false
            val onBorder = lr == 0 || lr == 6 || lc == 0 || lc == 6
            val inCenter = lr in 2..4 && lc in 2..4
            return onBorder || inCenter
        }

        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val dark = if (isFinderPattern(r, c)) {
                    isFinderDark(r, c)
                } else {
                    val hash = abs((r * 37 + c * 19 + seed) xor (seed ushr ((r + c) % 16)))
                    hash % 3 != 0
                }
                if (dark) {
                    canvas.drawRect(
                        (c * cellPx).toFloat(),
                        (r * cellPx).toFloat(),
                        ((c + 1) * cellPx).toFloat(),
                        ((r + 1) * cellPx).toFloat(),
                        darkPaint
                    )
                }
            }
        }
        return bitmap
    }

    fun copyQrUriToLocalFile(context: Context, uriString: String?, walletPrefix: String): Pair<String, String> {
        val cleanInput = uriString?.trim().orEmpty()
        val isGcash = walletPrefix.equals("gcash", ignoreCase = true)
        if (cleanInput.isEmpty()) {
            val defaultFileName = "${walletPrefix.lowercase()}_qr_${System.currentTimeMillis() % 10000}.png"
            val localFile = ensureLocalQrBitmapFile(context, defaultFileName, "$walletPrefix|$defaultFileName", isGcash)
            return Uri.fromFile(localFile).toString() to defaultFileName
        }
        if (cleanInput.startsWith("content://")) {
            runCatching {
                val parsedUri = Uri.parse(cleanInput)
                val targetFile = File(context.filesDir, "${walletPrefix.lowercase()}_qr_saved.png")
                context.contentResolver.openInputStream(parsedUri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (targetFile.exists() && targetFile.length() > 0L) {
                    return Uri.fromFile(targetFile).toString() to targetFile.name
                }
            }
        }
        val extractedName = cleanInput
            .substringAfterLast('/')
            .substringAfterLast(':')
            .ifBlank { "${walletPrefix.lowercase()}_qr_code.png" }
            .let { if (it.contains('.')) it else "$it.png" }
        ensureLocalQrBitmapFile(context, extractedName, "$walletPrefix|$cleanInput|$extractedName", isGcash)
        val normalizedUri = if (cleanInput.contains("://")) cleanInput else "file://local/$extractedName"
        return normalizedUri to extractedName
    }
}

data class ReservationEmailSummary(
    val emailId: String,
    val bookingCode: String,
    val recipientName: String,
    val recipientEmail: String,
    val senderEmail: String = "reservations@pickleplay.ph",
    val subject: String,
    val facilityName: String,
    val facilityAddress: String,
    val contactNumber: String,
    val courtName: String,
    val courtType: String,
    val dateLabel: String,
    val durationLabel: String,
    val timeRangeLabel: String,
    val paymentMethod: String,
    val paymentStatus: String,
    val courtFee: Int,
    val serviceFee: Int,
    val discountAmount: Int = 0,
    val totalAmount: Int,
    val status: String = "SENT",
    val sentTimestampLabel: String = "Just now",
    val sentAtMillis: Long = System.currentTimeMillis(),
    val bodyPreview: String
)

object MockEmailNotificationService {
    private val _sentEmails = MutableStateFlow<List<ReservationEmailSummary>>(emptyList())
    val sentEmails: StateFlow<List<ReservationEmailSummary>> = _sentEmails.asStateFlow()

    private val _latestSentEmail = MutableStateFlow<ReservationEmailSummary?>(null)
    val latestSentEmail: StateFlow<ReservationEmailSummary?> = _latestSentEmail.asStateFlow()

    fun formatDefaultEmail(playerName: String): String {
        val slug = playerName
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), ".")
            .trim('.')
            .ifBlank { "player" }
        return "$slug@pickleplay.ph"
    }

    fun sendReservationConfirmationEmail(
        recipientName: String,
        recipientEmail: String = "",
        bookingCode: String? = null,
        facilityName: String = "Smash Pickle Club",
        facilityAddress: String = "Tomas Morato Ave, Quezon City",
        contactNumber: String = "+63 917 882 4500",
        courtName: String = "Court 1",
        courtType: String = "Pro Cushion Indoor",
        dateLabel: String = "Monday, Sep 28, 2026",
        durationLabel: String = "1 Hour",
        timeRangeLabel: String = "2:00 PM • 1 Hour",
        paymentMethod: String = "GCash",
        courtFee: Int = 250,
        serviceFee: Int = 20,
        discountAmount: Int = 0,
        totalAmount: Int = (courtFee - discountAmount) + serviceFee
    ): ReservationEmailSummary {
        val cleanName = recipientName.trim().ifBlank { "Jonel P." }
        val cleanEmail = recipientEmail.trim().ifBlank { formatDefaultEmail(cleanName) }
        val resolvedCode = bookingCode?.trim()?.ifBlank { null }
            ?: "PKL-20260928-${(10000..99999).random()}"
        val cleanCourtType = courtType.replace("Championship", "Pro Cushion", ignoreCase = true)
        val paymentStatus = if (paymentMethod.equals("Pay at Venue", ignoreCase = true)) {
            "PAY AT VENUE"
        } else {
            "PAID"
        }
        val subject = "Reservation Confirmed: $courtName at $facilityName ($resolvedCode)"
        val body = buildString {
            append("Hi $cleanName, your pickleball court reservation is confirmed! ")
            append("Booking Code: $resolvedCode | ")
            append("Club: $facilityName ($facilityAddress • $contactNumber) | ")
            append("Court: $courtName ($cleanCourtType) | ")
            append("Schedule: $dateLabel • $timeRangeLabel ($durationLabel) | ")
            append("Payment: $paymentMethod ($paymentStatus) | ")
            append("Court Fee: ₱$courtFee + Service Fee: ₱$serviceFee")
            if (discountAmount > 0) {
                append(" - Discount: ₱$discountAmount")
            }
            append(" = Total: ₱$totalAmount.")
        }

        val summary = ReservationEmailSummary(
            emailId = "EMAIL-${System.currentTimeMillis()}-${(100..999).random()}",
            bookingCode = resolvedCode,
            recipientName = cleanName,
            recipientEmail = cleanEmail,
            subject = subject,
            facilityName = facilityName,
            facilityAddress = facilityAddress,
            contactNumber = contactNumber,
            courtName = courtName,
            courtType = cleanCourtType,
            dateLabel = dateLabel,
            durationLabel = durationLabel,
            timeRangeLabel = timeRangeLabel,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            courtFee = courtFee,
            serviceFee = serviceFee,
            discountAmount = discountAmount,
            totalAmount = totalAmount,
            status = "SENT",
            sentTimestampLabel = "Just now",
            sentAtMillis = System.currentTimeMillis(),
            bodyPreview = body
        )

        _latestSentEmail.value = summary
        _sentEmails.value = listOf(summary) + _sentEmails.value.filterNot { it.bookingCode == resolvedCode }
        return summary
    }

    fun resendEmail(summary: ReservationEmailSummary): ReservationEmailSummary {
        val resent = summary.copy(
            status = "RESENT ✓",
            sentTimestampLabel = "Resent just now",
            sentAtMillis = System.currentTimeMillis()
        )
        _latestSentEmail.value = resent
        _sentEmails.value = listOf(resent) + _sentEmails.value.filterNot { it.emailId == summary.emailId }
        return resent
    }

    fun dismissLatestEmail() {
        _latestSentEmail.value = null
    }

    fun clearAll() {
        _latestSentEmail.value = null
        _sentEmails.value = emptyList()
    }
}

class PicklePlayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PicklePlayRepository

    val facilities: StateFlow<List<FacilityEntity>>
    val courts: StateFlow<List<CourtEntity>>
    val bookings: StateFlow<List<BookingEntity>>
    val upcomingBookings: StateFlow<List<BookingEntity>>
    val pastBookings: StateFlow<List<BookingEntity>>
    val openPlayGames: StateFlow<List<OpenPlayGameEntity>>
    val tournaments: StateFlow<List<TournamentEntity>>
    val notifications: StateFlow<List<NotificationEntity>>
    val userProfile: StateFlow<UserProfileEntity?>

    // Navigation & Mode State
    private val _currentSection = MutableStateFlow(AppSection.HOME)
    val currentSection: StateFlow<AppSection> = _currentSection.asStateFlow()

    // Dedicated Auth State Holder tracking authentication status and role (Admin, Cashier, Player)
    val authStateHolder = AuthStateHolder()
    val authState: StateFlow<AuthUiState> = authStateHolder.uiState
    val isAuthenticated: StateFlow<Boolean> = authStateHolder.isAuthenticated
    val userRole: StateFlow<UserRole?> = authStateHolder.userRole
    val authSession: StateFlow<AuthUserSession?> = authStateHolder.authSession
    val showAuthModal: StateFlow<Boolean> = authStateHolder.showAuthModal
    val authModalInitialTab: StateFlow<Int> = authStateHolder.authModalInitialTab
    val registeredAccounts: StateFlow<List<RegisteredAccount>> = authStateHolder.registeredAccounts

    // Discovery Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCityFilter = MutableStateFlow("All Cities")
    val selectedCityFilter: StateFlow<String> = _selectedCityFilter.asStateFlow()

    private val _indoorFilter = MutableStateFlow<Boolean?>(null) // null = All, true = Indoor, false = Outdoor
    val indoorFilter: StateFlow<Boolean?> = _indoorFilter.asStateFlow()

    private val _selectedAmenityFilter = MutableStateFlow<String?>(null)
    val selectedAmenityFilter: StateFlow<String?> = _selectedAmenityFilter.asStateFlow()

    // Matchmaker Skill-Level Filter State
    private val _matchmakerSkillFilter = MutableStateFlow("All")
    val matchmakerSkillFilter: StateFlow<String> = _matchmakerSkillFilter.asStateFlow()

    // Real-Time Court Reservation State
    val dateOptions = listOf(
        BookingDateOption("2026-09-27", "Sun", "27", "September 27, 2026", true),
        BookingDateOption("2026-09-28", "Mon", "28", "September 28, 2026", false),
        BookingDateOption("2026-09-29", "Tue", "29", "September 29, 2026", false),
        BookingDateOption("2026-09-30", "Wed", "30", "September 30, 2026", false),
        BookingDateOption("2026-10-01", "Thu", "1", "October 1, 2026", false),
        BookingDateOption("2026-10-02", "Fri", "2", "October 2, 2026", false),
        BookingDateOption("2026-10-03", "Sat", "3", "October 3, 2026", true)
    )

    val timeSlotOptions = listOf(
        TimeSlotOption("07:00 AM", "7:00 AM - 8:00 AM", "MORNING", setOf(2)),
        TimeSlotOption("08:00 AM", "8:00 AM - 9:00 AM", "MORNING", setOf(1, 4)),
        TimeSlotOption("09:00 AM", "9:00 AM - 10:00 AM", "MORNING", emptySet()),
        TimeSlotOption("10:00 AM", "10:00 AM - 11:00 AM", "MIDDAY", setOf(1, 2, 5)),
        TimeSlotOption("11:00 AM", "11:00 AM - 12:00 PM", "MIDDAY", emptySet()),
        TimeSlotOption("12:00 PM", "12:00 PM - 1:00 PM", "MIDDAY", setOf(4)),
        TimeSlotOption("01:00 PM", "1:00 PM - 2:00 PM", "MIDDAY", setOf(1, 2, 6)),
        TimeSlotOption("02:00 PM", "2:00 PM - 3:00 PM", "MIDDAY", emptySet()),
        TimeSlotOption("03:00 PM", "3:00 PM - 4:00 PM", "MIDDAY", emptySet()),
        TimeSlotOption("04:00 PM", "4:00 PM - 5:00 PM", "PEAK", setOf(1, 2, 4, 5)),
        TimeSlotOption("05:00 PM", "5:00 PM - 6:00 PM", "PEAK", setOf(2)),
        TimeSlotOption("06:00 PM", "6:00 PM - 7:00 PM", "PEAK", setOf(1, 5)),
        TimeSlotOption("07:00 PM", "7:00 PM - 8:00 PM", "PEAK", setOf(2, 4)),
        TimeSlotOption("08:00 PM", "8:00 PM - 9:00 PM", "PEAK", emptySet()),
        TimeSlotOption("09:00 PM", "9:00 PM - 10:00 PM", "NIGHT", setOf(1)),
        TimeSlotOption("10:00 PM", "10:00 PM - 11:00 PM", "NIGHT", emptySet())
    )

    val reservationDurationOptions: List<ReservationDurationOption> = listOf(
        ReservationDurationOption("30m", "30 Mins", "Quick Warmup", 1800, 0.5),
        ReservationDurationOption("1h", "1 Hour", "Standard Session", 3600, 1.0),
        ReservationDurationOption("1.5h", "1.5 Hours", "Extended Match", 5400, 1.5),
        ReservationDurationOption("2h", "2 Hours", "Club Doubles", 7200, 2.0),
        ReservationDurationOption("3h", "3 Hours", "Group Session", 10800, 3.0)
    )

    private val _selectedFacilityId = MutableStateFlow(1)
    val selectedFacilityId: StateFlow<Int> = _selectedFacilityId.asStateFlow()

    private val _selectedDate = MutableStateFlow(dateOptions[1]) // Sept 28
    val selectedDate: StateFlow<BookingDateOption> = _selectedDate.asStateFlow()

    private val _selectedCourtId = MutableStateFlow(5)
    val selectedCourtId: StateFlow<Int> = _selectedCourtId.asStateFlow()

    private val _selectedTimeSlot = MutableStateFlow<TimeSlotOption?>(timeSlotOptions[7]) // 02:00 PM
    val selectedTimeSlot: StateFlow<TimeSlotOption?> = _selectedTimeSlot.asStateFlow()

    // Per-court booking time limit countdown (in seconds).
    // Courts with remainingSeconds > 0 are BOOKED (Red) and disabled during the booking time limit,
    // and automatically re-enable as AVAILABLE (Blue) once the time limit finishes.
    private val _courtBookingTimeLimits = MutableStateFlow(
        mapOf(
            2 to 90,   // Court 2: Already booked (1m 30s remaining in booking time limit)
            3 to 1800  // Court 3: Already booked (30m 00s remaining in booking time limit)
        )
    )
    val courtBookingTimeLimits: StateFlow<Map<Int, Int>> = _courtBookingTimeLimits.asStateFlow()

    private val _expiredCourtIds = MutableStateFlow<Set<Int>>(emptySet())
    val expiredCourtIds: StateFlow<Set<Int>> = _expiredCourtIds.asStateFlow()

    private val _inUseCourtIds = MutableStateFlow(setOf(1, 4))
    val inUseCourtIds: StateFlow<Set<Int>> = _inUseCourtIds.asStateFlow()

    // Interactive Available (Blue) Court Reservation Sheet/Dialog state
    private val _showAvailableCourtSheet = MutableStateFlow(false)
    val showAvailableCourtSheet: StateFlow<Boolean> = _showAvailableCourtSheet.asStateFlow()

    private val _selectedAvailableCourtId = MutableStateFlow<Int?>(null)
    val selectedAvailableCourtId: StateFlow<Int?> = _selectedAvailableCourtId.asStateFlow()

    // Temporary 10-minute Slot Lock Timer
    private val _lockSecondsRemaining = MutableStateFlow(585) // 09:45 remaining
    val lockSecondsRemaining: StateFlow<Int> = _lockSecondsRemaining.asStateFlow()
    private var lockTimerJob: Job? = null
    private var bookingTimeLimitJob: Job? = null

    // Checkout & Modals State
    private val _selectedPaymentMethod = MutableStateFlow("GCash")
    val selectedPaymentMethod: StateFlow<String> = _selectedPaymentMethod.asStateFlow()

    private val _isWalkInBooking = MutableStateFlow(false)
    val isWalkInBooking: StateFlow<Boolean> = _isWalkInBooking.asStateFlow()

    private val _promoCodeInput = MutableStateFlow("")
    val promoCodeInput: StateFlow<String> = _promoCodeInput.asStateFlow()

    private val _appliedPromoDiscount = MutableStateFlow(0)
    val appliedPromoDiscount: StateFlow<Int> = _appliedPromoDiscount.asStateFlow()

    private val _showCheckoutSheet = MutableStateFlow(false)
    val showCheckoutSheet: StateFlow<Boolean> = _showCheckoutSheet.asStateFlow()

    private val _bookingForPaymentUpload = MutableStateFlow<BookingEntity?>(null)
    val bookingForPaymentUpload: StateFlow<BookingEntity?> = _bookingForPaymentUpload.asStateFlow()

    private val _activeBookingPass = MutableStateFlow<BookingEntity?>(null)
    val activeBookingPass: StateFlow<BookingEntity?> = _activeBookingPass.asStateFlow()

    private var pendingBookingAction: (() -> Unit)? = null

    private val _lastReservedPlayerName = MutableStateFlow<String?>(null)
    val lastReservedPlayerName: StateFlow<String?> = _lastReservedPlayerName.asStateFlow()

    private val _showNotificationsSheet = MutableStateFlow(false)
    val showNotificationsSheet: StateFlow<Boolean> = _showNotificationsSheet.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Mock Email Notification Service state exposed to UI
    val latestEmailNotification: StateFlow<ReservationEmailSummary?> = MockEmailNotificationService.latestSentEmail
    val sentEmailNotifications: StateFlow<List<ReservationEmailSummary>> = MockEmailNotificationService.sentEmails

    // Admin Business Settings (Business Name, Address, Contact Number, GCash QR Code, PayMaya QR Code)
    private val _businessSettings = MutableStateFlow(
        BusinessSettingsLocalStore.load(application.applicationContext)
    )
    val businessSettings: StateFlow<BusinessPaymentSettings> = _businessSettings.asStateFlow()

    // Admin Products Directory (Only Admin can add products)
    private val _clubProducts = MutableStateFlow(
        listOf(
            ClubProductItem("paddle", "Carbon Paddle Rental", "Rental", 100, 18),
            ClubProductItem("balls", "Pro Pickleballs (3-Pack)", "Pro Shop", 150, 40),
            ClubProductItem("hydration", "Court Towel + Hydration", "Refreshment", 120, 35),
            ClubProductItem("coach", "1-Hr Coach Clinic Add-On", "Coaching", 500, 10)
        )
    )
    val clubProducts: StateFlow<List<ClubProductItem>> = _clubProducts.asStateFlow()

    val localStatusAlerts: StateFlow<List<ReservationStatusLocalAlert>> =
        LocalReservationNotificationService.alerts
    val latestLocalStatusAlert: StateFlow<ReservationStatusLocalAlert?> =
        LocalReservationNotificationService.latestAlert

    init {
        LocalReservationNotificationService.ensureNotificationChannel(application.applicationContext)
        com.example.notifications.FcmPushNotificationManager.ensureInitialized(application.applicationContext)
        FirestoreReservationRepository.initialize(application.applicationContext)
        FirestoreTournamentsAndMatchmakingRepository.ensureInitialized(application.applicationContext)
        val db = PicklePlayDatabase.getDatabase(application)
        repository = PicklePlayRepository(db.picklePlayDao())

        facilities = repository.facilities.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        courts = repository.courts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        bookings = repository.bookings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        upcomingBookings = repository.upcomingBookings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        pastBookings = repository.pastBookings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        openPlayGames = repository.openPlayGames.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        tournaments = repository.tournaments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        notifications = repository.notifications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        userProfile = repository.userProfile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        viewModelScope.launch {
            repository.ensureSeeded()
            if (BusinessSettingsLocalStore.hasSaved(getApplication())) {
                val localPrefSettings = BusinessSettingsLocalStore.load(getApplication())
                _businessSettings.value = localPrefSettings
                repository.saveBusinessSettingsEntity(localPrefSettings.toEntity())
            }
            repository.businessSettings.collectLatest { entity ->
                if (entity != null) {
                    val mapped = BusinessPaymentSettings.fromEntity(entity)
                    _businessSettings.value = mapped
                    BusinessSettingsLocalStore.save(getApplication(), mapped)
                }
            }
        }
        startSlotLockTimer()
        startCourtBookingTimeLimitTicker()
    }

    private fun startSlotLockTimer() {
        lockTimerJob?.cancel()
        _lockSecondsRemaining.value = 600 // 10 minutes
        lockTimerJob = viewModelScope.launch {
            while (_lockSecondsRemaining.value > 0) {
                delay(1000L)
                _lockSecondsRemaining.value -= 1
            }
        }
    }

    private fun startCourtBookingTimeLimitTicker() {
        bookingTimeLimitJob?.cancel()
        bookingTimeLimitJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val currentLimits = _courtBookingTimeLimits.value
                if (currentLimits.isNotEmpty()) {
                    val updated = mutableMapOf<Int, Int>()
                    val newlyExpired = mutableListOf<Int>()
                    for ((courtId, remaining) in currentLimits) {
                        val next = remaining - 1
                        if (next > 0) {
                            updated[courtId] = next
                        } else {
                            newlyExpired.add(courtId)
                        }
                    }
                    _courtBookingTimeLimits.value = updated
                    if (newlyExpired.isNotEmpty()) {
                        _expiredCourtIds.value = _expiredCourtIds.value + newlyExpired
                        val expiredNames = newlyExpired.joinToString(", ") { "Court $it" }
                        _toastMessage.value = "Booking time limit finished for $expiredNames. Court is now enabled and Available!"
                    }
                }
            }
        }
    }

    fun resolveCourtOccupancyStatus(
        court: CourtEntity,
        allBookings: List<BookingEntity> = bookings.value,
        dateIso: String = _selectedDate.value.isoDate,
        timeSlot: String? = _selectedTimeSlot.value?.startTime
    ): CourtOccupancyStatus {
        val expired = _expiredCourtIds.value.contains(court.id)
        if (expired) {
            return CourtOccupancyStatus.AVAILABLE
        }
        val activeTimeLimit = _courtBookingTimeLimits.value[court.id] ?: 0
        if (activeTimeLimit > 0) {
            return CourtOccupancyStatus.BOOKED
        }
        if (_inUseCourtIds.value.contains(court.id)) {
            return CourtOccupancyStatus.IN_USE
        }
        val matchingBookings = allBookings.filter {
            it.courtId == court.id &&
                it.dateIso == dateIso &&
                it.status != "CANCELLED" &&
                (timeSlot == null || it.timeSlot == timeSlot)
        }
        return when {
            matchingBookings.any { it.status == "CHECKED_IN" } -> CourtOccupancyStatus.IN_USE
            matchingBookings.any {
                it.status == "CONFIRMED" ||
                    it.status == "PENDING_ADMIN_APPROVAL" ||
                    it.status == "APPROVED_AWAITING_PAYMENT" ||
                    it.status == "PENDING_CASHIER_VERIFICATION"
            } -> CourtOccupancyStatus.BOOKED
            else -> CourtOccupancyStatus.AVAILABLE
        }
    }

    fun getBookedTimeSlotsForCourt(
        courtId: Int,
        dateIso: String = _selectedDate.value.isoDate,
        allBookings: List<BookingEntity> = bookings.value
    ): Set<String> {
        val fromDb = allBookings
            .filter { it.courtId == courtId && it.dateIso == dateIso && it.status != "CANCELLED" }
            .map { it.timeSlot }
            .toSet()
        val fromDefaults = timeSlotOptions
            .filter { it.defaultBookedCourts.contains(courtId) && dateIso == "2026-09-28" }
            .map { it.startTime }
            .toSet()
        return fromDb + fromDefaults
    }

    fun setWalkInBookingMode(walkIn: Boolean) {
        _isWalkInBooking.value = walkIn
        if (walkIn) {
            _selectedPaymentMethod.value = "Cash on Hand"
        } else if (_selectedPaymentMethod.value == "Cash on Hand") {
            _selectedPaymentMethod.value = "GCash"
        }
    }

    fun isCourtDisabledByBooking(
        court: CourtEntity,
        allBookings: List<BookingEntity> = bookings.value,
        dateIso: String = _selectedDate.value.isoDate,
        timeSlot: String? = _selectedTimeSlot.value?.startTime
    ): Boolean {
        return resolveCourtOccupancyStatus(court, allBookings, dateIso, timeSlot) == CourtOccupancyStatus.BOOKED
    }

    fun expireCourtBookingTimeLimit(courtId: Int) {
        val updated = _courtBookingTimeLimits.value.toMutableMap()
        updated.remove(courtId)
        _courtBookingTimeLimits.value = updated
        _expiredCourtIds.value = _expiredCourtIds.value + courtId
        _toastMessage.value = "Booking time limit ended for Court $courtId — Court is now Available!"
    }

    fun navigateTo(section: AppSection) {
        _currentSection.value = section
    }

    fun openMyReservationsScreen() {
        _currentSection.value = AppSection.MY_RESERVATIONS
    }

    fun requestOpenCheckoutSheet() {
        setShowCheckoutSheet(true)
    }

    fun openAuthModal(isRegister: Boolean = false) {
        authStateHolder.openAuthModal(isRegister)
    }

    fun closeAuthModal() {
        authStateHolder.closeAuthModal()
    }

    fun navigateToRoleDashboard(role: UserRole) {
        _currentSection.value = authStateHolder.resolveDashboardSection(role)
    }

    fun openRoleBasedDashboard() {
        val targetSection = authStateHolder.resolveLandingNavigationTarget()
        if (targetSection != null) {
            _currentSection.value = targetSection
        }
    }

    private fun consumePendingBookingActionOrNavigate(session: AuthUserSession, toastMsg: String) {
        val pending = pendingBookingAction
        if (pending != null && session.role == UserRole.CUSTOMER) {
            pendingBookingAction = null
            _toastMessage.value = "$toastMsg Continuing your court booking..."
            pending.invoke()
        } else {
            pendingBookingAction = null
            navigateToRoleDashboard(session.role)
            _toastMessage.value = toastMsg
        }
    }

    fun loginWithCredentials(email: String, password: String) {
        val fallbackName = userProfile.value?.fullName ?: "Jonel P."
        when (val result = authStateHolder.submitSignIn(email, password, fallbackName)) {
            is AuthResult.Success -> {
                consumePendingBookingActionOrNavigate(result.session, result.toastMessage)
            }
            is AuthResult.Error -> {
                _toastMessage.value = result.errorMessage
            }
        }
    }

    fun loginWithRole(email: String, password: String, selectedRole: UserRole? = null) {
        val fallbackName = userProfile.value?.fullName ?: "Jonel P."
        val session = authStateHolder.login(email, password, selectedRole, fallbackName)
        consumePendingBookingActionOrNavigate(
            session,
            "Welcome ${session.fullName}! Opening ${session.role.dashboardLabel}."
        )
    }

    fun registerCustomerAccount(fullName: String, email: String, password: String) {
        when (val result = authStateHolder.submitCustomerRegistration(fullName, email, password)) {
            is AuthResult.Success -> {
                consumePendingBookingActionOrNavigate(result.session, result.toastMessage)
            }
            is AuthResult.Error -> {
                _toastMessage.value = result.errorMessage
            }
        }
    }

    fun registerAccount(fullName: String, email: String, password: String, role: UserRole = UserRole.CUSTOMER) {
        val session = authStateHolder.register(fullName, email, password, UserRole.CUSTOMER)
        consumePendingBookingActionOrNavigate(
            session,
            "Registration successful! Welcome ${session.fullName} (Customer)."
        )
    }

    fun addCashierFromAdmin(
        fullName: String,
        email: String,
        password: String,
        staffRole: StaffRole = StaffRole.CASHIER
    ) {
        val created = authStateHolder.addCashierAccount(fullName, email, password, staffRole)
        _toastMessage.value = "Staff added (${created.staffRole.displayName}): ${created.fullName} (${created.email})"
    }

    fun addStaffFromAdmin(
        fullName: String,
        email: String,
        password: String,
        staffRole: StaffRole
    ) {
        addCashierFromAdmin(fullName, email, password, staffRole)
    }

    fun removeCashierFromAdmin(email: String) {
        authStateHolder.removeCashierAccount(email)
        _toastMessage.value = "Staff account ($email) removed."
    }

    fun switchUserRole(role: UserRole) {
        val fallbackName = userProfile.value?.fullName ?: "Jonel P."
        val session = authStateHolder.switchRole(role, fallbackName)
        navigateToRoleDashboard(session.role)
        _toastMessage.value = "Switched to ${session.role.dashboardLabel}"
    }

    fun logout() {
        pendingBookingAction = null
        authStateHolder.logout()
        _currentSection.value = AppSection.HOME
        _toastMessage.value = "Logged out successfully."
    }

    fun selectFacilityForBooking(facilityId: Int, navigateToCourts: Boolean = true) {
        _selectedFacilityId.value = facilityId
        val facilityCourts = courts.value.filter { it.facilityId == facilityId }
        val firstActive = facilityCourts.firstOrNull { it.status == "ACTIVE" } ?: facilityCourts.firstOrNull()
        if (firstActive != null) {
            _selectedCourtId.value = firstActive.id
        }
        if (navigateToCourts) {
            if (!isAuthenticated.value) {
                pendingBookingAction = {
                    _currentSection.value = AppSection.COURTS
                }
                authStateHolder.openAuthModal(isRegister = false)
                _toastMessage.value = "Please log in first to book a court."
                return
            }
            _currentSection.value = AppSection.COURTS
        }
    }

    fun selectDate(date: BookingDateOption) {
        _selectedDate.value = date
        startSlotLockTimer()
    }

    fun selectCourt(courtId: Int) {
        _selectedCourtId.value = courtId
        startSlotLockTimer()
        val targetCourt = courts.value.find { it.id == courtId }
        val status = if (targetCourt != null) {
            resolveCourtOccupancyStatus(targetCourt)
        } else {
            CourtOccupancyStatus.AVAILABLE
        }
        if (status == CourtOccupancyStatus.AVAILABLE) {
            openAvailableCourtSheet(courtId)
        }
    }

    private fun openAvailableCourtSheetInternal(courtId: Int) {
        _businessSettings.value = BusinessSettingsLocalStore.resolveSettings(
            getApplication(),
            _businessSettings.value
        )
        _selectedFacilityId.value = 1
        _selectedCourtId.value = courtId
        _selectedAvailableCourtId.value = courtId
        // Ensure a non-booked slot is selected for this court & date if current is booked
        val bookedForCourt = getBookedTimeSlotsForCourt(courtId, _selectedDate.value.isoDate)
        val currentSlot = _selectedTimeSlot.value
        if (currentSlot == null || bookedForCourt.contains(currentSlot.startTime)) {
            val firstFree = timeSlotOptions.firstOrNull { !bookedForCourt.contains(it.startTime) }
            if (firstFree != null) {
                _selectedTimeSlot.value = firstFree
            }
        }
        _showAvailableCourtSheet.value = true
        startSlotLockTimer()
    }

    fun openAvailableCourtSheet(courtId: Int) {
        if (!isAuthenticated.value) {
            _selectedCourtId.value = courtId
            pendingBookingAction = { openAvailableCourtSheetInternal(courtId) }
            authStateHolder.openAuthModal(isRegister = false)
            _toastMessage.value = "Please log in to book Court $courtId."
            return
        }
        openAvailableCourtSheetInternal(courtId)
    }

    fun closeAvailableCourtSheet() {
        _showAvailableCourtSheet.value = false
        _selectedAvailableCourtId.value = null
    }

    fun confirmAvailableCourtQuickReservation(
        court: CourtEntity,
        playerName: String,
        durationOption: ReservationDurationOption,
        paymentMethod: String = _selectedPaymentMethod.value,
        recipientEmail: String = "",
        isWalkIn: Boolean = _isWalkInBooking.value
    ) {
        val trimmedName = playerName.trim().ifBlank {
            authSession.value?.fullName ?: userProfile.value?.fullName ?: "Jonel P."
        }
        val walkInFlag = isWalkIn || paymentMethod.equals("Cash on Hand", ignoreCase = true)
        val resolvedMethod = if (walkInFlag) "Cash on Hand" else paymentMethod
        _selectedPaymentMethod.value = resolvedMethod
        _isWalkInBooking.value = walkInFlag
        val resolvedBusiness = BusinessSettingsLocalStore.resolveSettings(
            getApplication(),
            _businessSettings.value
        )
        _businessSettings.value = resolvedBusiness
        val facility = facilities.value.find { it.id == court.facilityId } ?: facilities.value.firstOrNull() ?: FacilityEntity(
            id = 1,
            name = resolvedBusiness.businessName,
            city = "Quezon City",
            address = resolvedBusiness.address,
            distanceKm = 1.2,
            rating = 4.9,
            reviewCount = 248,
            courtCount = 6,
            isIndoor = true,
            surfaceType = "Pro Cushion Acrylic",
            minPricePerHour = 200,
            amenitiesCsv = "Air-conditioned,Parking,Pro Shop,Café",
            operatingHours = "6:00 AM – 11:00 PM",
            imageKey = "indoor_hero"
        )
        val date = _selectedDate.value
        val slot = _selectedTimeSlot.value ?: timeSlotOptions[7]
        val baseHourlyFee = calculateSlotPrice(court, date, slot)
        val rawCourtFee = (baseHourlyFee * durationOption.hoursMultiplier).toInt().coerceAtLeast(100)
        val serviceFee = 20
        val finalTotal = rawCourtFee + serviceFee
        val recentSheetEmail = MockEmailNotificationService.latestSentEmail.value?.takeIf {
            it.recipientName.equals(trimmedName, ignoreCase = true) &&
                it.courtName.equals(court.name, ignoreCase = true) &&
                (System.currentTimeMillis() - it.sentAtMillis) < 3000L
        }
        val generatedCode = recentSheetEmail?.bookingCode
            ?: "PKL-${date.isoDate.replace("-", "")}-${(10000..99999).random()}"
        val resolvedRecipientEmail = recipientEmail.trim().ifBlank {
            recentSheetEmail?.recipientEmail
                ?: authSession.value?.email
                ?: MockEmailNotificationService.formatDefaultEmail(trimmedName)
        }

        // Trigger Mock Email Notification Service immediately upon confirmation in the bottom sheet
        val emailSummary = MockEmailNotificationService.sendReservationConfirmationEmail(
            recipientName = trimmedName,
            recipientEmail = resolvedRecipientEmail,
            bookingCode = generatedCode,
            facilityName = resolvedBusiness.businessName.ifBlank { facility.name },
            facilityAddress = resolvedBusiness.businessAddress.ifBlank { facility.address },
            contactNumber = resolvedBusiness.contactNumber,
            courtName = court.name,
            courtType = court.courtType,
            dateLabel = date.fullLabel,
            durationLabel = durationOption.label,
            timeRangeLabel = "${slot.startTime} • ${durationOption.label}",
            paymentMethod = resolvedMethod,
            courtFee = rawCourtFee,
            serviceFee = serviceFee,
            discountAmount = 0,
            totalAmount = finalTotal
        )

        _lastReservedPlayerName.value = trimmedName
        _expiredCourtIds.value = _expiredCourtIds.value - court.id
        _inUseCourtIds.value = _inUseCourtIds.value - court.id
        _courtBookingTimeLimits.value = _courtBookingTimeLimits.value + (court.id to durationOption.durationSeconds)
        _showAvailableCourtSheet.value = false
        _selectedAvailableCourtId.value = null
        _toastMessage.value = if (walkInFlag) {
            "Walk-In booking created for ${court.name}! Please pay Cash on Hand (₱$finalTotal) at the Cashier."
        } else {
            "Booking requested for ${court.name}! Sent to Admin for approval. Email sent to ${emailSummary.recipientEmail}."
        }

        viewModelScope.launch {
            repository.createReservation(
                facility = facility,
                court = court,
                dateIso = date.isoDate,
                dateLabel = date.fullLabel,
                timeSlot = slot.startTime,
                timeRangeLabel = "${slot.startTime} • ${durationOption.label}",
                playerName = trimmedName,
                courtFee = rawCourtFee,
                discountAmount = 0,
                serviceFee = serviceFee,
                totalAmount = finalTotal,
                paymentMethod = resolvedMethod,
                bookingCodeOverride = generatedCode,
                recipientEmail = emailSummary.recipientEmail,
                isWalkIn = walkInFlag
            )
        }
    }

    fun confirmAvailableCourtReservation(
        playerName: String,
        durationOption: ReservationDurationOption,
        paymentMethod: String = _selectedPaymentMethod.value,
        recipientEmail: String = "",
        isWalkIn: Boolean = _isWalkInBooking.value
    ) {
        val courtId = _selectedAvailableCourtId.value ?: _selectedCourtId.value
        val court = courts.value.find { it.id == courtId } ?: CourtEntity(
            id = courtId,
            facilityId = 1,
            name = "Court $courtId",
            courtType = "Pro Cushion Indoor",
            isIndoor = true,
            status = "ACTIVE",
            morningPrice = 200,
            middayPrice = 250,
            peakPrice = 350,
            nightPrice = 250,
            weekendPrice = 350
        )
        confirmAvailableCourtQuickReservation(court, playerName, durationOption, paymentMethod, recipientEmail, isWalkIn)
    }

    fun dismissLatestEmailNotification() {
        MockEmailNotificationService.dismissLatestEmail()
    }

    fun resendLatestEmailNotification() {
        val current = MockEmailNotificationService.latestSentEmail.value ?: return
        val resent = MockEmailNotificationService.resendEmail(current)
        _toastMessage.value = "✓ Reservation summary email resent to ${resent.recipientEmail}"
    }

    fun selectTimeSlot(slot: TimeSlotOption) {
        _selectedTimeSlot.value = slot
        startSlotLockTimer()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCityFilter(city: String) {
        _selectedCityFilter.value = city
    }

    fun updateIndoorFilter(indoor: Boolean?) {
        _indoorFilter.value = indoor
    }

    fun updateAmenityFilter(amenity: String?) {
        _selectedAmenityFilter.value = if (_selectedAmenityFilter.value == amenity) null else amenity
    }

    fun selectPaymentMethod(method: String) {
        _selectedPaymentMethod.value = method
        _businessSettings.value = BusinessSettingsLocalStore.resolveSettings(
            getApplication(),
            _businessSettings.value
        )
    }

    fun getSelectedPaymentQrCode(): StoredBusinessQrCode {
        return BusinessSettingsLocalStore.getStoredQrCode(
            context = getApplication(),
            paymentMethod = _selectedPaymentMethod.value,
            fallback = _businessSettings.value
        )
    }

    fun updatePromoCode(code: String) {
        _promoCodeInput.value = code
        val normalized = code.trim().uppercase()
        _appliedPromoDiscount.value = when (normalized) {
            "PICKLE50" -> 50
            "FIRSTDINK" -> 40
            "QC2026" -> 30
            else -> 0
        }
    }

    fun setShowCheckoutSheet(show: Boolean) {
        if (show && !isAuthenticated.value) {
            pendingBookingAction = {
                _businessSettings.value = BusinessSettingsLocalStore.resolveSettings(
                    getApplication(),
                    _businessSettings.value
                )
                _showCheckoutSheet.value = true
            }
            authStateHolder.openAuthModal(isRegister = false)
            _toastMessage.value = "Please log in first to complete your court reservation."
            return
        }
        if (show) {
            _businessSettings.value = BusinessSettingsLocalStore.resolveSettings(
                getApplication(),
                _businessSettings.value
            )
        }
        _showCheckoutSheet.value = show
    }

    fun openCustomerPaymentDialog(booking: BookingEntity) {
        _businessSettings.value = BusinessSettingsLocalStore.resolveSettings(
            getApplication(),
            _businessSettings.value
        )
        _selectedPaymentMethod.value = if (booking.paymentMethod == "PayMaya") "PayMaya" else "GCash"
        _bookingForPaymentUpload.value = booking
    }

    fun closeCustomerPaymentDialog() {
        _bookingForPaymentUpload.value = null
    }

    fun openBookingPass(booking: BookingEntity?) {
        if (booking != null && !booking.isQrPassReady) {
            _toastMessage.value = "QR Pass will unlock once payment is verified by the Cashier."
            return
        }
        _activeBookingPass.value = booking
    }

    fun setShowNotificationsSheet(show: Boolean) {
        _showNotificationsSheet.value = show
        if (show) {
            viewModelScope.launch {
                repository.markAllNotificationsRead()
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun postToast(msg: String) {
        _toastMessage.value = msg
    }

    fun calculateSlotPrice(court: CourtEntity?, date: BookingDateOption, slot: TimeSlotOption?): Int {
        if (court == null || slot == null) return 250
        if (date.isWeekend) return court.weekendPrice
        return when (slot.periodTier) {
            "MORNING" -> court.morningPrice
            "MIDDAY" -> court.middayPrice
            "PEAK" -> court.peakPrice
            "NIGHT" -> court.nightPrice
            else -> court.middayPrice
        }
    }

    fun confirmCourtReservation(isWalkInOverride: Boolean? = null) {
        val facility = facilities.value.find { it.id == _selectedFacilityId.value } ?: facilities.value.firstOrNull() ?: return
        val court = courts.value.find { it.id == _selectedCourtId.value } ?: return
        val slot = _selectedTimeSlot.value ?: return
        val date = _selectedDate.value
        val profile = userProfile.value

        val walkInFlag = isWalkInOverride
            ?: _isWalkInBooking.value
            || _selectedPaymentMethod.value.equals("Cash on Hand", ignoreCase = true)
        val resolvedMethod = if (walkInFlag) "Cash on Hand" else _selectedPaymentMethod.value

        val rawCourtFee = calculateSlotPrice(court, date, slot)
        val memberDiscountPct = profile?.discountPercent ?: 0
        val memberDiscount = (rawCourtFee * memberDiscountPct) / 100
        val totalDiscount = (memberDiscount + _appliedPromoDiscount.value).coerceAtMost(rawCourtFee - 50)
        val serviceFee = 20
        val finalTotal = (rawCourtFee - totalDiscount) + serviceFee
        val reservingUserName = authSession.value?.fullName ?: profile?.fullName ?: "Jonel P."
        val reservingEmail = authSession.value?.email ?: MockEmailNotificationService.formatDefaultEmail(reservingUserName)
        val generatedCode = "PKL-${date.isoDate.replace("-", "")}-${(10000..99999).random()}"
        val resolvedBusiness = BusinessSettingsLocalStore.resolveSettings(getApplication(), _businessSettings.value)

        val emailSummary = MockEmailNotificationService.sendReservationConfirmationEmail(
            recipientName = reservingUserName,
            recipientEmail = reservingEmail,
            bookingCode = generatedCode,
            facilityName = resolvedBusiness.businessName.ifBlank { facility.name },
            facilityAddress = resolvedBusiness.businessAddress.ifBlank { facility.address },
            contactNumber = resolvedBusiness.contactNumber,
            courtName = court.name,
            courtType = court.courtType,
            dateLabel = date.fullLabel,
            durationLabel = "1 Hour",
            timeRangeLabel = slot.rangeLabel,
            paymentMethod = resolvedMethod,
            courtFee = rawCourtFee,
            serviceFee = serviceFee,
            discountAmount = totalDiscount,
            totalAmount = finalTotal
        )

        _lastReservedPlayerName.value = reservingUserName
        _expiredCourtIds.value = _expiredCourtIds.value - court.id
        _courtBookingTimeLimits.value = _courtBookingTimeLimits.value + (court.id to 3600)
        _showCheckoutSheet.value = false
        _toastMessage.value = if (walkInFlag) {
            "Walk-In booking created for $reservingUserName! Please pay Cash on Hand (₱$finalTotal) at the Cashier."
        } else {
            "Booking submitted for $reservingUserName! Sent to Admin for approval."
        }

        viewModelScope.launch {
            repository.createReservation(
                facility = facility,
                court = court,
                dateIso = date.isoDate,
                dateLabel = date.fullLabel,
                timeSlot = slot.startTime,
                timeRangeLabel = slot.rangeLabel,
                playerName = reservingUserName,
                courtFee = rawCourtFee,
                discountAmount = totalDiscount,
                serviceFee = serviceFee,
                totalAmount = finalTotal,
                paymentMethod = resolvedMethod,
                bookingCodeOverride = generatedCode,
                recipientEmail = emailSummary.recipientEmail,
                isWalkIn = walkInFlag
            )
        }
    }

    fun submitCourtReservationForm(input: CourtReservationFormInput) {
        val facility = facilities.value.find { it.id == _selectedFacilityId.value }
            ?: facilities.value.firstOrNull()
            ?: FacilityEntity(
                id = 1,
                name = _businessSettings.value.businessName,
                city = "Quezon City",
                address = _businessSettings.value.address,
                distanceKm = 1.2,
                rating = 4.9,
                reviewCount = 248,
                courtCount = 6,
                isIndoor = true,
                surfaceType = "Pro Cushion Acrylic",
                minPricePerHour = 200,
                amenitiesCsv = "Air-conditioned,Parking,Pro Shop,Café",
                operatingHours = "6:00 AM – 11:00 PM",
                imageKey = "indoor_hero"
            )
        val court = courts.value.find { it.id == input.courtId } ?: CourtEntity(
            id = input.courtId,
            facilityId = facility.id,
            name = input.courtName.ifBlank { "Court ${input.courtId}" },
            courtType = "Pro Cushion Indoor",
            isIndoor = true,
            status = "ACTIVE",
            morningPrice = 200,
            middayPrice = 250,
            peakPrice = 350,
            nightPrice = 300,
            weekendPrice = 380
        )

        _selectedCourtId.value = court.id
        dateOptions.find { it.isoDate.equals(input.dateIso, ignoreCase = true) }?.let {
            _selectedDate.value = it
        }
        timeSlotOptions.find { it.startTime.equals(input.timeSlot, ignoreCase = true) }?.let {
            _selectedTimeSlot.value = it
        }
        _selectedPaymentMethod.value = input.paymentMethod

        val walkInFlag = input.paymentMethod.equals("Cash on Hand", ignoreCase = true) ||
            input.paymentMethod.equals("Pay at Venue", ignoreCase = true)
        val cleanDateCompact = input.dateIso.replace(Regex("[^0-9]"), "").take(8).ifBlank { "20260928" }
        val generatedCode = "PKL-$cleanDateCompact-${(10000..99999).random()}"
        val resolvedBusiness = BusinessSettingsLocalStore.resolveSettings(getApplication(), _businessSettings.value)
        val reservingName = input.playerName.trim().ifBlank {
            authSession.value?.fullName ?: userProfile.value?.fullName ?: "Jonel P."
        }
        val reservingEmail = input.playerEmail.trim().ifBlank {
            authSession.value?.email ?: MockEmailNotificationService.formatDefaultEmail(reservingName)
        }

        val emailSummary = MockEmailNotificationService.sendReservationConfirmationEmail(
            recipientName = reservingName,
            recipientEmail = reservingEmail,
            bookingCode = generatedCode,
            facilityName = resolvedBusiness.businessName.ifBlank { facility.name },
            facilityAddress = resolvedBusiness.businessAddress.ifBlank { facility.address },
            contactNumber = resolvedBusiness.contactNumber,
            courtName = court.name,
            courtType = court.courtType,
            dateLabel = input.dateLabel,
            durationLabel = input.durationLabel,
            timeRangeLabel = input.timeRangeLabel,
            paymentMethod = input.paymentMethod,
            courtFee = input.courtFee,
            serviceFee = input.serviceFee,
            discountAmount = 0,
            totalAmount = input.totalAmount
        )

        _lastReservedPlayerName.value = reservingName
        _expiredCourtIds.value = _expiredCourtIds.value - court.id
        _courtBookingTimeLimits.value = _courtBookingTimeLimits.value + (court.id to 3600)
        _toastMessage.value = "Reserved ${court.name} (ID #${court.id}) on ${input.dateIso} at ${input.timeSlot}!"

        viewModelScope.launch {
            FirestoreReservationRepository.persistReservationFromForm(
                context = getApplication(),
                input = input.copy(playerName = reservingName, playerEmail = reservingEmail),
                facility = facility
            )
            repository.createReservation(
                facility = facility,
                court = court,
                dateIso = input.dateIso,
                dateLabel = input.dateLabel,
                timeSlot = input.timeSlot,
                timeRangeLabel = input.timeRangeLabel,
                playerName = reservingName,
                courtFee = input.courtFee,
                discountAmount = 0,
                serviceFee = input.serviceFee,
                totalAmount = input.totalAmount,
                paymentMethod = input.paymentMethod,
                bookingCodeOverride = generatedCode,
                recipientEmail = emailSummary.recipientEmail,
                isWalkIn = walkInFlag
            )
        }
    }

    fun approveBookingByAdmin(booking: BookingEntity) {
        viewModelScope.launch {
            val updated = repository.approveBookingByAdmin(booking)
            LocalReservationNotificationService.notifyReservationStatusChanged(
                context = getApplication(),
                booking = updated,
                previousStatus = "Pending",
                newStatus = "Approved"
            )
            _toastMessage.value = if (booking.isWalkIn) {
                "Approved walk-in booking ${booking.bookingCode} — routed to Cashier for Cash on Hand."
            } else {
                "Approved ${booking.bookingCode}! Local notification sent (Pending → Approved) to ${booking.playerName}."
            }
        }
    }

    fun triggerStatusUpdateAlert(booking: BookingEntity? = null) {
        val target = booking
            ?: bookings.value.firstOrNull { it.status == "PENDING_ADMIN_APPROVAL" }
            ?: bookings.value.firstOrNull()
            ?: BookingEntity(
                id = 99,
                bookingCode = "PKL-20260928-00142",
                facilityId = 1,
                facilityName = "Smash Pickle Club",
                facilityLocation = "Quezon City",
                courtId = 4,
                courtName = "Court 4",
                dateIso = "2026-09-28",
                dateLabel = "Monday, Sep 28, 2026",
                timeSlot = "4:00 PM",
                timeRangeLabel = "4:00 PM - 5:00 PM",
                playerName = authSession.value?.fullName ?: userProfile.value?.fullName ?: "Jonel P.",
                courtFee = 250,
                discountAmount = 0,
                serviceFee = 20,
                totalAmount = 270,
                paymentMethod = "GCash",
                paymentStatus = "AWAITING_PAYMENT",
                status = "APPROVED_AWAITING_PAYMENT"
            )
        viewModelScope.launch {
            val updatedBooking = if (target.status == "PENDING_ADMIN_APPROVAL") {
                repository.approveBookingByAdmin(target)
            } else {
                target.copy(
                    status = "APPROVED_AWAITING_PAYMENT",
                    paymentStatus = "AWAITING_PAYMENT",
                    adminApprovedAt = "Approved Just Now"
                )
            }
            val alert = LocalReservationNotificationService.notifyReservationStatusChanged(
                context = getApplication(),
                booking = updatedBooking,
                previousStatus = "Pending",
                newStatus = "Approved"
            )
            _toastMessage.value = "🔔 ${alert.title} (#${alert.bookingCode})"
        }
    }

    fun dismissLatestLocalStatusAlert() {
        LocalReservationNotificationService.dismissLatestAlert()
    }

    fun rejectBookingByAdmin(booking: BookingEntity) {
        viewModelScope.launch {
            val updated = repository.rejectBookingByAdmin(booking)
            LocalReservationNotificationService.notifyReservationStatusChanged(
                context = getApplication(),
                booking = updated,
                previousStatus = booking.status,
                newStatus = "Cancelled"
            )
            val updatedLimits = _courtBookingTimeLimits.value.toMutableMap()
            updatedLimits.remove(booking.courtId)
            _courtBookingTimeLimits.value = updatedLimits
            _toastMessage.value = "Declined booking request ${booking.bookingCode}."
        }
    }

    fun submitCustomerPaymentProof(
        booking: BookingEntity,
        paymentMethod: String,
        referenceNumber: String,
        receiptUri: String,
        receiptFileName: String
    ) {
        val cleanRef = referenceNumber.trim()
        if (cleanRef.isEmpty()) {
            _toastMessage.value = "Please enter your payment Reference Number."
            return
        }
        if (receiptFileName.isBlank() && receiptUri.isBlank()) {
            _toastMessage.value = "Please upload a screenshot or receipt of your payment."
            return
        }
        viewModelScope.launch {
            val updated = repository.submitCustomerPaymentProof(
                booking = booking,
                paymentMethod = paymentMethod,
                referenceNumber = cleanRef,
                receiptUri = receiptUri,
                receiptFileName = receiptFileName
            )
            LocalReservationNotificationService.notifyReservationStatusChanged(
                context = getApplication(),
                booking = updated,
                previousStatus = "Approved",
                newStatus = "Verified"
            )
            _bookingForPaymentUpload.value = null
            _toastMessage.value = "Payment receipt & Ref #$cleanRef submitted! Sent to Cashier for verification."
        }
    }

    fun verifyBookingPaymentByCashier(booking: BookingEntity) {
        viewModelScope.launch {
            val verified = repository.verifyPaymentByCashier(booking)
            LocalReservationNotificationService.notifyReservationStatusChanged(
                context = getApplication(),
                booking = verified,
                previousStatus = "Verified",
                newStatus = "Paid"
            )
            _toastMessage.value = "✓ Verified ${verified.paymentMethod} payment (₱${verified.totalAmount})! QR Pass issued to ${verified.playerName}."
        }
    }

    fun cancelPendingCustomerReservation(booking: BookingEntity) {
        viewModelScope.launch {
            FirestoreReservationRepository.removeReservationFromFirestore(
                context = getApplication(),
                booking = booking
            )
            val updated = repository.cancelPendingReservation(booking)
            LocalReservationNotificationService.notifyReservationStatusChanged(
                context = getApplication(),
                booking = updated,
                previousStatus = "Pending",
                newStatus = "Cancelled"
            )
            val updatedLimits = _courtBookingTimeLimits.value.toMutableMap()
            updatedLimits.remove(booking.courtId)
            _courtBookingTimeLimits.value = updatedLimits
            if (_activeBookingPass.value?.id == booking.id) {
                _activeBookingPass.value = null
            }
            _toastMessage.value = "Pending reservation ${booking.bookingCode} cancelled."
        }
    }

    fun performQrCheckIn(booking: BookingEntity) {
        viewModelScope.launch {
            val checkInTimeStr = "1:47 PM"
            repository.checkInBooking(booking, checkInTimeStr)
            val refreshed = booking.copy(
                status = "CHECKED_IN",
                paymentStatus = "PAID",
                checkInTime = checkInTimeStr
            )
            LocalReservationNotificationService.notifyReservationStatusChanged(
                context = getApplication(),
                booking = refreshed,
                previousStatus = "Paid",
                newStatus = "Completed"
            )
            val updatedLimits = _courtBookingTimeLimits.value.toMutableMap()
            updatedLimits.remove(booking.courtId)
            _courtBookingTimeLimits.value = updatedLimits
            _inUseCourtIds.value = _inUseCourtIds.value + booking.courtId
            if (_activeBookingPass.value?.id == booking.id) {
                _activeBookingPass.value = refreshed
            }
            _toastMessage.value = "✓ Check-in recorded at $checkInTimeStr for ${booking.bookingCode}"
        }
    }

    fun cancelBookingWithPolicy(booking: BookingEntity, hoursBeforeNotice: Int) {
        viewModelScope.launch {
            FirestoreReservationRepository.removeReservationFromFirestore(
                context = getApplication(),
                booking = booking
            )
            repository.cancelBooking(booking, hoursBeforeNotice)
            LocalReservationNotificationService.notifyReservationStatusChanged(
                context = getApplication(),
                booking = booking.copy(status = "CANCELLED"),
                previousStatus = booking.status,
                newStatus = "Cancelled"
            )
            val updatedLimits = _courtBookingTimeLimits.value.toMutableMap()
            updatedLimits.remove(booking.courtId)
            _courtBookingTimeLimits.value = updatedLimits
            _activeBookingPass.value = null
            val policyDesc = when {
                hoursBeforeNotice >= 24 -> "100% refund issued"
                hoursBeforeNotice >= 12 -> "50% refund issued"
                else -> "No refund (<12h policy)"
            }
            _toastMessage.value = "Reservation cancelled ($policyDesc)"
        }
    }

    fun rateBooking(booking: BookingEntity, stars: Int, reviewText: String) {
        viewModelScope.launch {
            repository.submitBookingReview(booking, stars, reviewText)
            if (_activeBookingPass.value?.id == booking.id) {
                _activeBookingPass.value = booking.copy(userRating = stars, userReview = reviewText)
            }
            _toastMessage.value = "Thank you for rating ${booking.facilityName} ($stars ★)!"
        }
    }

    fun toggleJoinOpenPlay(game: OpenPlayGameEntity) {
        val userName = userProfile.value?.name ?: "Jonel"
        viewModelScope.launch {
            repository.toggleJoinOpenPlay(game, userName)
            val action = if (game.isJoinedByUser) "Left" else "Joined"
            _toastMessage.value = "$action ${game.title}!"
        }
    }

    fun setMatchmakerSkillFilter(skillLevel: String) {
        _matchmakerSkillFilter.value = skillLevel.ifBlank { "All" }
    }

    fun hostNewOpenPlay(
        title: String,
        facilityName: String,
        courtName: String,
        dayLabel: String,
        timeRange: String,
        skillLevel: String,
        pricePerPlayer: Int
    ) {
        val hostName = userProfile.value?.name ?: "Jonel"
        viewModelScope.launch {
            repository.createOpenPlayGame(
                title = title,
                facilityName = facilityName,
                courtName = courtName,
                dayLabel = dayLabel,
                timeRange = timeRange,
                skillLevel = skillLevel,
                pricePerPlayer = pricePerPlayer,
                hostName = hostName,
                maxPlayers = 6
            )
            _toastMessage.value = "Created '$title' open play session!"
        }
    }

    fun postOpenSlotsForGame(
        title: String,
        facilityName: String,
        courtName: String,
        dayLabel: String,
        timeRange: String,
        skillLevel: String,
        pricePerPlayer: Int,
        openSlotsNeeded: Int,
        maxPlayers: Int = 6
    ) {
        val hostName = userProfile.value?.name ?: "Jonel"
        val resolvedMax = maxPlayers.coerceAtLeast(openSlotsNeeded + 1)
        val initialFilledCount = (resolvedMax - openSlotsNeeded).coerceAtLeast(1)
        val companionNames = listOf("Carlo", "Maria", "Jon", "Sofia", "Miggy")
        val initialRoster = buildList {
            add(hostName)
            for (i in 0 until (initialFilledCount - 1)) {
                add(companionNames.getOrElse(i) { "Player ${i + 2}" })
            }
        }.joinToString(",")
        viewModelScope.launch {
            repository.createOpenPlayGame(
                title = title,
                facilityName = facilityName,
                courtName = courtName,
                dayLabel = dayLabel,
                timeRange = timeRange,
                skillLevel = skillLevel,
                pricePerPlayer = pricePerPlayer,
                hostName = hostName,
                maxPlayers = resolvedMax,
                initialPlayersCsv = initialRoster
            )
            _toastMessage.value = "Posted '$title' ($skillLevel) with $openSlotsNeeded open slot(s)!"
        }
    }

    fun registerForTournament(tournament: TournamentEntity, partnerName: String) {
        val userName = userProfile.value?.name ?: "Jonel"
        viewModelScope.launch {
            FirestoreTournamentsAndMatchmakingRepository.registerForTournamentInFirestore(
                context = getApplication(),
                tournament = tournament,
                playerName = userName,
                partnerName = partnerName.ifBlank { "Carlo" }
            )
            repository.joinTournament(tournament, partnerName.ifBlank { "Carlo" }, userName)
            _toastMessage.value = "Registered for ${tournament.name}!"
        }
    }

    fun submitMatchScore(
        tournament: TournamentEntity,
        s1A: Int,
        s1B: Int,
        s2A: Int,
        s2B: Int,
        s3A: Int,
        s3B: Int
    ) {
        viewModelScope.launch {
            repository.submitTournamentScore(
                tournament = tournament,
                s1A = s1A,
                s1B = s1B,
                s2A = s2A,
                s2B = s2B,
                s3A = s3A,
                s3B = s3B,
                profile = userProfile.value
            )
            _toastMessage.value = "Official match score submitted for ${tournament.name}!"
        }
    }

    fun createTournamentFromAdmin(
        name: String,
        division: String,
        format: String,
        skillCap: String,
        dateRange: String,
        entryFee: Int,
        prizePool: Int,
        maxTeams: Int,
        activeCourt: String,
        teamA: String,
        teamB: String
    ) {
        viewModelScope.launch {
            repository.createTournamentByAdmin(
                name = name,
                division = division,
                format = format,
                skillCap = skillCap,
                dateRange = dateRange,
                entryFee = entryFee,
                prizePool = prizePool,
                maxTeams = maxTeams,
                activeCourt = activeCourt,
                teamA = teamA,
                teamB = teamB
            )
            _toastMessage.value = "Created tournament bracket '$name' ($division • $format)!"
        }
    }

    fun updateTournamentBracketFromAdmin(
        tournament: TournamentEntity,
        activeRound: String,
        activeCourt: String,
        teamA: String,
        teamB: String
    ) {
        viewModelScope.launch {
            repository.updateTournamentBracketByAdmin(
                tournament = tournament,
                activeRound = activeRound,
                activeCourt = activeCourt,
                teamA = teamA,
                teamB = teamB
            )
            _toastMessage.value = "Updated bracket for ${tournament.name} ($activeRound • $activeCourt)!"
        }
    }

    fun selectMembershipTier(tier: String, price: Int, discountPct: Int) {
        val current = userProfile.value ?: return
        viewModelScope.launch {
            repository.updateMembership(current, tier, price, discountPct)
            _toastMessage.value = "Switched to $tier Membership ($discountPct% booking discount)!"
        }
    }

    fun updatePlayerPreferences(position: String, skillLevel: String) {
        val current = userProfile.value ?: return
        viewModelScope.launch {
            repository.updatePlayerPreferences(current, position, skillLevel)
            _toastMessage.value = "Updated player profile ($skillLevel • $position)"
        }
    }

    fun updateUserSkillLevel(skillLabel: String, duprRating: Double) {
        val current = userProfile.value ?: return
        viewModelScope.launch {
            repository.updateUserSkillLevel(current, skillLabel, duprRating)
            _toastMessage.value = "Updated skill level to $duprRating ($skillLabel)!"
        }
    }

    fun updateUserDisplayName(fullName: String) {
        val current = userProfile.value ?: return
        viewModelScope.launch {
            repository.updateUserName(current, fullName)
            _toastMessage.value = "Updated profile name to $fullName!"
        }
    }

    fun selectCourtToBook(courtId: Int) {
        _selectedFacilityId.value = 1
        _selectedCourtId.value = courtId
        startSlotLockTimer()
        if (!isAuthenticated.value) {
            pendingBookingAction = {
                val targetCourt = courts.value.find { it.id == courtId }
                val status = if (targetCourt != null) {
                    resolveCourtOccupancyStatus(targetCourt)
                } else {
                    CourtOccupancyStatus.AVAILABLE
                }
                if (status == CourtOccupancyStatus.AVAILABLE) {
                    openAvailableCourtSheetInternal(courtId)
                } else {
                    _currentSection.value = AppSection.COURTS
                }
            }
            authStateHolder.openAuthModal(isRegister = false)
            _toastMessage.value = "Please log in first to book Court $courtId."
            return
        }
        val targetCourt = courts.value.find { it.id == courtId }
        val status = if (targetCourt != null) {
            resolveCourtOccupancyStatus(targetCourt)
        } else {
            CourtOccupancyStatus.AVAILABLE
        }
        if (status == CourtOccupancyStatus.AVAILABLE) {
            openAvailableCourtSheet(courtId)
        } else {
            _currentSection.value = AppSection.COURTS
        }
    }

    fun openFullScheduleForCourt(courtId: Int) {
        _selectedFacilityId.value = 1
        _selectedCourtId.value = courtId
        _showAvailableCourtSheet.value = false
        _selectedAvailableCourtId.value = null
        startSlotLockTimer()
        if (!isAuthenticated.value) {
            pendingBookingAction = {
                _currentSection.value = AppSection.COURTS
            }
            authStateHolder.openAuthModal(isRegister = false)
            _toastMessage.value = "Please log in first to view and book court schedules."
            return
        }
        _currentSection.value = AppSection.COURTS
    }

    // Admin Dashboard & Club Owner Actions
    fun saveMyClubProfile(
        facility: FacilityEntity,
        name: String,
        city: String,
        address: String,
        operatingHours: String,
        surfaceType: String,
        isIndoor: Boolean,
        amenitiesCsv: String
    ) {
        viewModelScope.launch {
            repository.updateClubDetails(
                facility = facility,
                name = name,
                city = city,
                address = address,
                operatingHours = operatingHours,
                surfaceType = surfaceType,
                isIndoor = isIndoor,
                amenitiesCsv = amenitiesCsv
            )
            _toastMessage.value = "Updated club profile for ${name.ifBlank { facility.name }}!"
        }
    }

    fun toggleCourtStatus(court: CourtEntity) {
        viewModelScope.launch {
            repository.toggleCourtMaintenance(court)
            val newState = if (court.status == "ACTIVE") "MAINTENANCE" else "ACTIVE"
            _toastMessage.value = "${court.name} is now $newState"
        }
    }

    fun saveCourtPricing(
        court: CourtEntity,
        name: String,
        courtType: String,
        morning: Int,
        midday: Int,
        peak: Int,
        night: Int,
        weekend: Int
    ) {
        viewModelScope.launch {
            repository.updateCourtPricing(court, name, courtType, morning, midday, peak, night, weekend)
            _toastMessage.value = "Updated settings & rates for ${name.ifBlank { court.name }}"
        }
    }

    fun saveCourtStatusAndPricing(
        court: CourtEntity,
        status: String,
        name: String,
        courtType: String,
        morning: Int,
        midday: Int,
        peak: Int,
        night: Int,
        weekend: Int
    ) {
        viewModelScope.launch {
            repository.updateCourtStatusAndPricing(
                court = court,
                status = status,
                name = name,
                courtType = courtType,
                morning = morning,
                midday = midday,
                peak = peak,
                night = night,
                weekend = weekend
            )
            _toastMessage.value = "Saved ${name.ifBlank { court.name }} ($status • ₱$midday–₱$peak/hr)"
        }
    }

    fun collectCashierPayment(booking: BookingEntity, paymentMethod: String, checkInNow: Boolean = true) {
        viewModelScope.launch {
            repository.collectBookingPayment(booking, paymentMethod, checkInNow)
            _toastMessage.value = "Collected ₱${booking.totalAmount} ($paymentMethod) for ${booking.playerName}"
        }
    }

    fun createWalkInPosBooking(
        court: CourtEntity,
        timeRangeLabel: String,
        playerName: String,
        paymentMethod: String = "Cash on Hand",
        extraItemsTotal: Int = 0
    ) {
        val facility = facilities.value.firstOrNull() ?: return
        val date = _selectedDate.value
        val baseRate = court.middayPrice
        val total = baseRate + extraItemsTotal
        val resolvedMethod = if (paymentMethod.isBlank()) "Cash on Hand" else paymentMethod
        viewModelScope.launch {
            val created = repository.createReservation(
                facility = facility,
                court = court,
                dateIso = date.isoDate,
                dateLabel = date.fullLabel,
                timeSlot = timeRangeLabel.substringBefore(" -").ifBlank { "02:00 PM" },
                timeRangeLabel = timeRangeLabel,
                playerName = playerName.ifBlank { "Walk-In Guest" },
                courtFee = baseRate,
                discountAmount = 0,
                serviceFee = extraItemsTotal,
                totalAmount = total,
                paymentMethod = resolvedMethod,
                isWalkIn = true,
                initialStatus = "CONFIRMED"
            )
            repository.verifyPaymentByCashier(created, resolvedMethod)
            _toastMessage.value = "Walk-in POS booking verified for ${court.name} (₱$total via $resolvedMethod) — QR Pass ready!"
        }
    }

    fun addCourtToFacility(facilityId: Int, isIndoor: Boolean, courtType: String) {
        val facility = facilities.value.find { it.id == facilityId } ?: facilities.value.firstOrNull()
        val existingCount = courts.value.count { it.facilityId == (facility?.id ?: facilityId) }
        viewModelScope.launch {
            repository.addNewCourt(facility, existingCount + 1, isIndoor, courtType)
            _toastMessage.value = "Added Court ${existingCount + 1} to ${facility?.name ?: _businessSettings.value.businessName}!"
        }
    }

    fun saveBusinessSettings(
        businessName: String,
        address: String,
        contactNumber: String
    ) {
        val cleanName = businessName.trim().ifBlank { _businessSettings.value.businessName }
        val cleanAddress = address.trim().ifBlank { _businessSettings.value.address }
        val cleanContact = contactNumber.trim().ifBlank { _businessSettings.value.contactNumber }
        val updatedSettings = _businessSettings.value.copy(
            businessName = cleanName,
            address = cleanAddress,
            contactNumber = cleanContact,
            gcashAccountName = cleanName,
            paymayaAccountName = cleanName,
            lastSavedTimestamp = System.currentTimeMillis()
        )
        _businessSettings.value = updatedSettings
        BusinessSettingsLocalStore.save(getApplication(), updatedSettings)
        viewModelScope.launch {
            repository.saveBusinessSettingsEntity(updatedSettings.toEntity())
            val currentFacility = facilities.value.firstOrNull()
            if (currentFacility != null) {
                repository.updateClubDetails(
                    facility = currentFacility,
                    name = cleanName,
                    city = currentFacility.city,
                    address = cleanAddress,
                    operatingHours = currentFacility.operatingHours,
                    surfaceType = currentFacility.surfaceType,
                    isIndoor = currentFacility.isIndoor,
                    amenitiesCsv = currentFacility.amenitiesCsv
                )
            }
        }
        _toastMessage.value = "Saved business settings locally: $cleanName ($cleanContact)"
    }

    fun uploadGcashQrCode(uriString: String?) {
        val (savedUri, fileName) = BusinessSettingsLocalStore.copyQrUriToLocalFile(
            context = getApplication(),
            uriString = uriString,
            walletPrefix = "gcash"
        )
        val updatedSettings = _businessSettings.value.copy(
            gcashQrUploaded = true,
            gcashQrUri = savedUri,
            gcashFileName = fileName,
            gcashMerchantCode = "GCASH-QR-${System.currentTimeMillis() % 100000}",
            lastSavedTimestamp = System.currentTimeMillis()
        )
        _businessSettings.value = updatedSettings
        BusinessSettingsLocalStore.save(getApplication(), updatedSettings)
        viewModelScope.launch {
            repository.saveBusinessSettingsEntity(updatedSettings.toEntity())
        }
        _toastMessage.value = "GCash QR Code ($fileName) saved locally for customer payments!"
    }

    fun uploadPayMayaQrCode(uriString: String?) {
        val (savedUri, fileName) = BusinessSettingsLocalStore.copyQrUriToLocalFile(
            context = getApplication(),
            uriString = uriString,
            walletPrefix = "paymaya"
        )
        val updatedSettings = _businessSettings.value.copy(
            paymayaQrUploaded = true,
            paymayaQrUri = savedUri,
            paymayaFileName = fileName,
            paymayaMerchantCode = "PAYMAYA-QR-${System.currentTimeMillis() % 100000}",
            lastSavedTimestamp = System.currentTimeMillis()
        )
        _businessSettings.value = updatedSettings
        BusinessSettingsLocalStore.save(getApplication(), updatedSettings)
        viewModelScope.launch {
            repository.saveBusinessSettingsEntity(updatedSettings.toEntity())
        }
        _toastMessage.value = "PayMaya QR Code ($fileName) saved locally for customer payments!"
    }

    fun addClubProduct(name: String, category: String, price: Int, stock: Int) {
        val cleanName = name.trim().ifBlank { return }
        val cleanCategory = category.trim().ifBlank { "Pro Shop" }
        val validPrice = price.coerceAtLeast(10)
        val validStock = stock.coerceAtLeast(1)
        val id = cleanName.lowercase().replace(Regex("[^a-z0-9]"), "_").take(18) + "_${_clubProducts.value.size + 1}"
        val newProduct = ClubProductItem(
            id = id,
            name = cleanName,
            category = cleanCategory,
            price = validPrice,
            stock = validStock
        )
        _clubProducts.value = listOf(newProduct) + _clubProducts.value
        _toastMessage.value = "Added product: $cleanName (₱$validPrice • Stock: $validStock)"
    }

    fun removeClubProduct(productId: String) {
        val target = _clubProducts.value.find { it.id == productId }
        _clubProducts.value = _clubProducts.value.filterNot { it.id == productId }
        if (target != null) {
            _toastMessage.value = "Removed product: ${target.name}"
        }
    }
}
