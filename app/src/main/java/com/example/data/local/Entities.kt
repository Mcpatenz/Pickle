package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "facilities")
data class FacilityEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val city: String,
    val address: String,
    val distanceKm: Double,
    val rating: Double,
    val reviewCount: Int,
    val courtCount: Int,
    val isIndoor: Boolean,
    val surfaceType: String,
    val minPricePerHour: Int,
    val amenitiesCsv: String, // e.g., "Air-conditioned,Parking,Pro Shop,Café,Locker Rooms"
    val operatingHours: String,
    val imageKey: String, // "indoor_hero", "outdoor_club", "tournament_arena"
    val isFeatured: Boolean = false
)

@Entity(tableName = "courts")
data class CourtEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val facilityId: Int,
    val name: String, // "Court 1", "Court 2", etc.
    val courtType: String, // "Pro Cushion Indoor", "Championship Acrylic", etc.
    val isIndoor: Boolean,
    val status: String, // "ACTIVE" or "MAINTENANCE"
    val morningPrice: Int, // 6 AM - 10 AM (e.g. 200)
    val middayPrice: Int,  // 10 AM - 4 PM (e.g. 250)
    val peakPrice: Int,    // 4 PM - 9 PM (e.g. 350)
    val nightPrice: Int,   // 9 PM - 11 PM (e.g. 250)
    val weekendPrice: Int  // Sat/Sun (e.g. 350)
)

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookingCode: String, // e.g. "PKL-20260928-00125"
    val facilityId: Int,
    val facilityName: String,
    val facilityLocation: String,
    val courtId: Int,
    val courtName: String,
    val dateIso: String, // e.g. "2026-09-28"
    val dateLabel: String, // e.g. "September 28, 2026"
    val timeSlot: String, // e.g. "02:00 PM"
    val timeRangeLabel: String, // e.g. "2:00 PM - 3:00 PM"
    val playerName: String, // "Jonel P."
    val courtFee: Int,
    val discountAmount: Int,
    val serviceFee: Int,
    val totalAmount: Int,
    val paymentMethod: String, // "GCash", "PayMaya", "Cash on Hand", "Pay at Venue"
    val paymentStatus: String, // "AWAITING_APPROVAL", "AWAITING_PAYMENT", "FOR_VERIFICATION", "CASH_ON_HAND_AT_CASHIER", "PAID", "REFUNDED (100%)"
    val status: String, // "PENDING_ADMIN_APPROVAL", "APPROVED_AWAITING_PAYMENT", "PENDING_CASHIER_VERIFICATION", "CONFIRMED", "CHECKED_IN", "COMPLETED", "CANCELLED"
    val checkInTime: String? = null, // e.g. "1:47 PM"
    val userRating: Int? = null,
    val userReview: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val customerEmail: String = "customer@pickleplay.ph",
    val isWalkIn: Boolean = false,
    val referenceNumber: String? = null,
    val receiptUri: String? = null,
    val receiptFileName: String? = null,
    val adminApprovedAt: String? = null,
    val cashierVerifiedAt: String? = null
) {
    val isPendingCancellationAllowed: Boolean
        get() = status == "PENDING_ADMIN_APPROVAL" ||
            status == "APPROVED_AWAITING_PAYMENT" ||
            status == "PENDING_CASHIER_VERIFICATION" ||
            status == "PENDING"

    val isUpcomingBooking: Boolean
        get() = status != "CANCELLED" && status != "CHECKED_IN" && status != "COMPLETED"

    val isPastBooking: Boolean
        get() = status == "CANCELLED" || status == "CHECKED_IN" || status == "COMPLETED"

    val reservationCategory: String
        get() = if (isUpcomingBooking) "UPCOMING" else "PAST"

    val isQrPassReady: Boolean
        get() = paymentStatus == "PAID" && (status == "CONFIRMED" || status == "CHECKED_IN")
}

typealias CourtReservationEntity = BookingEntity

@Entity(tableName = "open_play_games")
data class OpenPlayGameEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String, // "Saturday Open Play"
    val facilityId: Int,
    val facilityName: String,
    val courtName: String,
    val dayLabel: String, // "Saturday"
    val dateLabel: String, // "Oct 3, 2026"
    val timeRange: String, // "5:00 PM - 7:00 PM"
    val skillLevel: String, // "Beginner", "Intermediate", "Advanced", "All Levels"
    val pricePerPlayer: Int, // 150
    val maxPlayers: Int, // 6
    val playersCsv: String, // "Jon,Maria,Carlo"
    val isJoinedByUser: Boolean = false,
    val hostName: String = "Coach Marco"
) {
    val joinedPlayersList: List<String>
        get() = playersCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    val filledSlotsCount: Int
        get() = joinedPlayersList.size.coerceAtMost(maxPlayers.coerceAtLeast(2))

    val openSlotsCount: Int
        get() = (maxPlayers.coerceAtLeast(2) - filledSlotsCount).coerceAtLeast(0)

    val hasOpenSlots: Boolean
        get() = openSlotsCount > 0

    fun matchesSkillFilter(skillFilter: String): Boolean {
        val cleanFilter = skillFilter.trim()
        if (cleanFilter.isEmpty() || cleanFilter.equals("All", ignoreCase = true)) {
            return true
        }
        return skillLevel.equals(cleanFilter, ignoreCase = true)
    }
}

typealias MatchmakerSessionEntity = OpenPlayGameEntity

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // "PicklePlay Open 2026"
    val facilityName: String,
    val dateRange: String,
    val division: String, // "Doubles", "Mixed Doubles", "Singles"
    val format: String, // "Knockout", "Round Robin", "League"
    val skillCap: String, // "DUPR 3.5 - 4.0"
    val entryFee: Int,
    val prizePool: Int,
    val registeredCount: Int,
    val maxTeams: Int,
    val isJoinedByUser: Boolean,
    val activeRound: String, // "Quarter Finals"
    val activeCourt: String, // "Court 2"
    val teamA: String, // "Jonel / Carlo"
    val teamB: String, // "Mark / James"
    val set1A: Int,
    val set1B: Int,
    val set2A: Int,
    val set2B: Int,
    val set3A: Int,
    val set3B: Int,
    val matchSubmitted: Boolean = false
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // "BOOKING", "REMINDER", "PAYMENT", "TOURNAMENT", "OPEN_PLAY"
    val title: String,
    val body: String,
    val timeAgo: String,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String, // "Jonel"
    val fullName: String, // "Jonel P."
    val city: String, // "Quezon City"
    val skillLevel: String, // "Intermediate"
    val duprRating: Double, // 3.5
    val gamesPlayed: Int, // 42
    val wins: Int, // 27
    val winStreak: Int, // 5
    val preferredPosition: String, // "Right Side", "Left Side", "Both Sides"
    val membershipTier: String, // "Basic", "Player", "Pro"
    val membershipPrice: Int, // 0, 499, 999
    val discountPercent: Int // 0, 10, 20
)

@Entity(tableName = "business_settings")
data class BusinessSettingsEntity(
    @PrimaryKey val id: Int = 1,
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
    val updatedAt: Long = System.currentTimeMillis()
)

