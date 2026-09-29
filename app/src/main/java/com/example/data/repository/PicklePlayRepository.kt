package com.example.data.repository

import com.example.data.local.BookingEntity
import com.example.data.local.BusinessSettingsEntity
import com.example.data.local.CourtEntity
import com.example.data.local.FacilityEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.OpenPlayGameEntity
import com.example.data.local.PicklePlayDao
import com.example.data.local.TournamentEntity
import com.example.data.local.UserProfileEntity
import kotlinx.coroutines.flow.Flow

class PicklePlayRepository(private val dao: PicklePlayDao) {

    val facilities: Flow<List<FacilityEntity>> = dao.getAllFacilities()
    val courts: Flow<List<CourtEntity>> = dao.getAllCourts()
    val bookings: Flow<List<BookingEntity>> = dao.getAllBookings()
    val upcomingBookings: Flow<List<BookingEntity>> = dao.getUpcomingBookings()
    val pastBookings: Flow<List<BookingEntity>> = dao.getPastBookings()
    val openPlayGames: Flow<List<OpenPlayGameEntity>> = dao.getAllOpenPlayGames()
    val joinedOpenPlayGames: Flow<List<OpenPlayGameEntity>> = dao.getJoinedOpenPlayGames()
    val tournaments: Flow<List<TournamentEntity>> = dao.getAllTournaments()
    val notifications: Flow<List<NotificationEntity>> = dao.getAllNotifications()
    val userProfile: Flow<UserProfileEntity?> = dao.getUserProfile()
    val businessSettings: Flow<BusinessSettingsEntity?> = dao.getBusinessSettings()

    fun getUserReservations(playerName: String, customerEmail: String): Flow<List<BookingEntity>> =
        dao.getUserReservations(playerName, customerEmail)

    fun getUserUpcomingReservations(playerName: String, customerEmail: String): Flow<List<BookingEntity>> =
        dao.getUserUpcomingReservations(playerName, customerEmail)

    fun getUserPastReservations(playerName: String, customerEmail: String): Flow<List<BookingEntity>> =
        dao.getUserPastReservations(playerName, customerEmail)

    suspend fun insertReservation(booking: BookingEntity): Long =
        dao.insertBooking(booking)

    suspend fun deleteReservation(bookingId: Int) =
        dao.deleteBookingById(bookingId)

    fun getOpenPlayGamesBySkillLevel(skillLevel: String): Flow<List<OpenPlayGameEntity>> =
        dao.getOpenPlayGamesBySkillLevel(skillLevel)

    suspend fun ensureSeeded() {
        dao.removeChampionshipFromCourtTypes()
        if (dao.getBusinessSettingsOnce() == null) {
            dao.upsertBusinessSettings(BusinessSettingsEntity())
        }
        if (dao.getFacilityCount() > 0) return

        // 1. Seed User Profile (Jonel, Intermediate, 42 Games, 27 Wins, 3.5 Rating, Right Side)
        dao.insertUserProfile(
            UserProfileEntity(
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
        )

        // 2. Seed Facilities across Metro Manila
        val seededFacilities = listOf(
            FacilityEntity(
                id = 1,
                name = "Smash Pickle Club",
                city = "Quezon City",
                address = "Tomas Morato Ave, Scout Area, Quezon City",
                distanceKm = 1.2,
                rating = 4.8,
                reviewCount = 318,
                courtCount = 6,
                isIndoor = true,
                surfaceType = "Pro Cushion Acrylic",
                minPricePerHour = 200,
                amenitiesCsv = "Air-conditioned,Parking,Pro Shop,Café,Showers,Paddle Rental",
                operatingHours = "6:00 AM – 11:00 PM",
                imageKey = "indoor_hero",
                isFeatured = true
            ),
            FacilityEntity(
                id = 2,
                name = "BGC Dink & Rally Club",
                city = "Taguig City",
                address = "32nd Street, Bonifacio Global City, Taguig",
                distanceKm = 6.4,
                rating = 4.9,
                reviewCount = 442,
                courtCount = 8,
                isIndoor = true,
                surfaceType = "USA Pickleball Certified Mat",
                minPricePerHour = 300,
                amenitiesCsv = "Air-conditioned,Parking,Pro Shop,Café,VIP Lounge,Video Replay",
                operatingHours = "6:00 AM – 12:00 AM",
                imageKey = "tournament_arena",
                isFeatured = true
            ),
            FacilityEntity(
                id = 3,
                name = "Palms Rooftop Pickleball",
                city = "Pasig City",
                address = "Capitol Commons, Ortigas Center, Pasig",
                distanceKm = 4.5,
                rating = 4.7,
                reviewCount = 195,
                courtCount = 4,
                isIndoor = false,
                surfaceType = "Covered Outdoor Acrylic",
                minPricePerHour = 200,
                amenitiesCsv = "Parking,Café,Paddle Rental,Night Floodlights",
                operatingHours = "6:00 AM – 10:00 PM",
                imageKey = "outdoor_club",
                isFeatured = false
            ),
            FacilityEntity(
                id = 4,
                name = "Katipunan Kitchen Yard",
                city = "Quezon City",
                address = "Katipunan Ave, Loyola Heights, Quezon City",
                distanceKm = 2.8,
                rating = 4.6,
                reviewCount = 154,
                courtCount = 6,
                isIndoor = true,
                surfaceType = "Hardwood Cushion Hybrid",
                minPricePerHour = 220,
                amenitiesCsv = "Air-conditioned,Parking,Pro Shop,Coach Clinic",
                operatingHours = "7:00 AM – 11:00 PM",
                imageKey = "indoor_hero",
                isFeatured = false
            )
        )
        dao.insertFacilities(seededFacilities)

        // 3. Seed Courts (Smash Pickle Club Courts 1-6 without Championship label)
        val seededCourts = mutableListOf<CourtEntity>()
        for (courtNum in 1..6) {
            seededCourts.add(
                CourtEntity(
                    facilityId = 1,
                    name = "Court $courtNum",
                    courtType = "Pro Cushion Indoor",
                    isIndoor = true,
                    status = "ACTIVE",
                    morningPrice = 200, // 6 AM - 10 AM
                    middayPrice = 250,  // 10 AM - 4 PM
                    peakPrice = 350,    // 4 PM - 9 PM
                    nightPrice = 250,   // 9 PM - 11 PM
                    weekendPrice = 350  // Sat / Sun
                )
            )
        }
        // Courts for Facility 2 (BGC Dink & Rally Club)
        for (courtNum in 1..4) {
            seededCourts.add(
                CourtEntity(
                    facilityId = 2,
                    name = "Court $courtNum",
                    courtType = "Pro Stadium Acrylic",
                    isIndoor = true,
                    status = "ACTIVE",
                    morningPrice = 300,
                    middayPrice = 350,
                    peakPrice = 450,
                    nightPrice = 350,
                    weekendPrice = 450
                )
            )
        }
        // Courts for Facility 3 (Palms Rooftop)
        for (courtNum in 1..4) {
            seededCourts.add(
                CourtEntity(
                    facilityId = 3,
                    name = "Court $courtNum",
                    courtType = "Covered Outdoor Court",
                    isIndoor = false,
                    status = "ACTIVE",
                    morningPrice = 200,
                    middayPrice = 220,
                    peakPrice = 300,
                    nightPrice = 250,
                    weekendPrice = 300
                )
            )
        }
        // Courts for Facility 4 (Katipunan Kitchen Yard)
        for (courtNum in 1..4) {
            seededCourts.add(
                CourtEntity(
                    facilityId = 4,
                    name = "Court $courtNum",
                    courtType = "Indoor Cushion Court",
                    isIndoor = true,
                    status = "ACTIVE",
                    morningPrice = 220,
                    middayPrice = 250,
                    peakPrice = 340,
                    nightPrice = 250,
                    weekendPrice = 340
                )
            )
        }
        dao.insertCourts(seededCourts)

        // 4. Seed Bookings (including the exact Digital Booking Pass from the prompt + busy slots for realistic availability)
        dao.insertBookings(
            listOf(
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
                    checkInTime = null,
                    createdAt = System.currentTimeMillis() - 3600_000L
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
                    checkInTime = "9:48 AM",
                    createdAt = System.currentTimeMillis() - 7200_000L
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
                    checkInTime = null,
                    createdAt = System.currentTimeMillis() - 5400_000L
                ),
                BookingEntity(
                    id = 4,
                    bookingCode = "PKL-20260928-00120",
                    facilityId = 1,
                    facilityName = "Smash Pickle Club",
                    facilityLocation = "Quezon City",
                    courtId = 1,
                    courtName = "Court 1",
                    dateIso = "2026-09-28",
                    dateLabel = "September 28, 2026",
                    timeSlot = "04:00 PM",
                    timeRangeLabel = "4:00 PM - 5:00 PM",
                    playerName = "Paolo D.",
                    courtFee = 350,
                    discountAmount = 0,
                    serviceFee = 20,
                    totalAmount = 370,
                    paymentMethod = "Credit/Debit Card",
                    paymentStatus = "PAID",
                    status = "CONFIRMED",
                    checkInTime = null,
                    createdAt = System.currentTimeMillis() - 4000_000L
                ),
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
                    paymentMethod = "Cash on Hand",
                    paymentStatus = "CASH_ON_HAND_AT_CASHIER",
                    status = "PENDING_CASHIER_VERIFICATION",
                    checkInTime = null,
                    createdAt = System.currentTimeMillis() - 1800_000L,
                    isWalkIn = true
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
                    checkInTime = null,
                    createdAt = System.currentTimeMillis() - 900_000L,
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
                    checkInTime = null,
                    createdAt = System.currentTimeMillis() - 1200_000L,
                    customerEmail = "customer@pickleplay.ph",
                    isWalkIn = false,
                    adminApprovedAt = "15 mins ago"
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
                    checkInTime = "8:50 AM",
                    createdAt = System.currentTimeMillis() - 86_400_000L,
                    customerEmail = "customer@pickleplay.ph",
                    isWalkIn = false,
                    referenceNumber = "PM-8829-4410",
                    receiptFileName = "paymaya_receipt_00104.png",
                    adminApprovedAt = "Yesterday",
                    cashierVerifiedAt = "Yesterday"
                ),
                BookingEntity(
                    id = 9,
                    bookingCode = "PKL-20260920-00089",
                    facilityId = 2,
                    facilityName = "BGC Dink & Rally Club",
                    facilityLocation = "Taguig City",
                    courtId = 2,
                    courtName = "Court 2",
                    dateIso = "2026-09-20",
                    dateLabel = "September 20, 2026",
                    timeSlot = "04:00 PM",
                    timeRangeLabel = "4:00 PM - 5:00 PM",
                    playerName = "Jonel P.",
                    courtFee = 450,
                    discountAmount = 45,
                    serviceFee = 20,
                    totalAmount = 425,
                    paymentMethod = "GCash",
                    paymentStatus = "PAID",
                    status = "COMPLETED",
                    checkInTime = "3:52 PM",
                    userRating = 5,
                    userReview = "Awesome cushion surface and LED lighting!",
                    createdAt = System.currentTimeMillis() - 172_800_000L,
                    customerEmail = "customer@pickleplay.ph",
                    isWalkIn = false,
                    referenceNumber = "GC-7712-9031",
                    receiptFileName = "gcash_receipt_00089.png",
                    adminApprovedAt = "Sep 20",
                    cashierVerifiedAt = "Sep 20"
                )
            )
        )

        // 5. Seed Open Play Games
        dao.insertOpenPlayGames(
            listOf(
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
                ),
                OpenPlayGameEntity(
                    id = 2,
                    title = "Morning Dink & Coffee Social",
                    facilityId = 1,
                    facilityName = "Smash Pickle Club",
                    courtName = "Court 4",
                    dayLabel = "Sunday",
                    dateLabel = "September 27, 2026",
                    timeRange = "8:00 AM - 10:00 AM",
                    skillLevel = "All Levels",
                    pricePerPlayer = 120,
                    maxPlayers = 6,
                    playersCsv = "Jonel,Sofia,Miggy,Tricia",
                    isJoinedByUser = true,
                    hostName = "Jonel P."
                ),
                OpenPlayGameEntity(
                    id = 3,
                    title = "BGC DUPR 3.5+ Competitive Rotation",
                    facilityId = 2,
                    facilityName = "BGC Dink & Rally Club",
                    courtName = "Center Court 1",
                    dayLabel = "Monday",
                    dateLabel = "September 28, 2026",
                    timeRange = "7:00 PM - 9:00 PM",
                    skillLevel = "Advanced",
                    pricePerPlayer = 200,
                    maxPlayers = 6,
                    playersCsv = "Mark,James,Kevin,Rico,Luis",
                    isJoinedByUser = false,
                    hostName = "Mark S."
                ),
                OpenPlayGameEntity(
                    id = 4,
                    title = "Beginner Kitchen Rules & Rally",
                    facilityId = 3,
                    facilityName = "Palms Rooftop Pickleball",
                    courtName = "Court 2",
                    dayLabel = "Tuesday",
                    dateLabel = "September 29, 2026",
                    timeRange = "6:00 PM - 8:00 PM",
                    skillLevel = "Beginner",
                    pricePerPlayer = 130,
                    maxPlayers = 6,
                    playersCsv = "Aya,Dan",
                    isJoinedByUser = false,
                    hostName = "Coach Bea"
                )
            )
        )

        // 6. Seed Tournaments
        dao.insertTournaments(
            listOf(
                TournamentEntity(
                    id = 1,
                    name = "PicklePlay Open 2026",
                    facilityName = "Smash Pickle Club • Quezon City",
                    dateRange = "Sept 28 – 30, 2026",
                    division = "Doubles",
                    format = "Knockout",
                    skillCap = "DUPR 3.5 – 4.0",
                    entryFee = 850,
                    prizePool = 50000,
                    registeredCount = 16,
                    maxTeams = 16,
                    isJoinedByUser = true,
                    activeRound = "Quarter Finals",
                    activeCourt = "Court 2",
                    teamA = "Jonel / Carlo",
                    teamB = "Mark / James",
                    set1A = 11,
                    set1B = 8,
                    set2A = 8,
                    set2B = 11,
                    set3A = 11,
                    set3B = 7,
                    matchSubmitted = false
                ),
                TournamentEntity(
                    id = 2,
                    name = "Metro Manila Mixed Doubles Cup",
                    facilityName = "BGC Dink & Rally Club • Taguig",
                    dateRange = "Oct 10 – 11, 2026",
                    division = "Mixed Doubles",
                    format = "Round Robin",
                    skillCap = "Intermediate 3.0+",
                    entryFee = 750,
                    prizePool = 35000,
                    registeredCount = 11,
                    maxTeams = 16,
                    isJoinedByUser = false,
                    activeRound = "Group Stage Pool A",
                    activeCourt = "Court 1",
                    teamA = "Maria / Jon",
                    teamB = "Sofia / Miggy",
                    set1A = 11,
                    set1B = 9,
                    set2A = 11,
                    set2B = 6,
                    set3A = 0,
                    set3B = 0,
                    matchSubmitted = true
                ),
                TournamentEntity(
                    id = 3,
                    name = "QC Night Singles Ladder League",
                    facilityName = "Katipunan Kitchen Yard • QC",
                    dateRange = "Oct 18, 2026",
                    division = "Singles",
                    format = "League",
                    skillCap = "Open Division",
                    entryFee = 500,
                    prizePool = 20000,
                    registeredCount = 9,
                    maxTeams = 12,
                    isJoinedByUser = false,
                    activeRound = "Round 1",
                    activeCourt = "Court 3",
                    teamA = "Paolo D.",
                    teamB = "Rico T.",
                    set1A = 11,
                    set1B = 4,
                    set2A = 11,
                    set2B = 7,
                    set3A = 0,
                    set3B = 0,
                    matchSubmitted = true
                )
            )
        )

        // 7. Seed Push Notifications
        dao.insertNotifications(
            listOf(
                NotificationEntity(
                    category = "REMINDER",
                    title = "🏓 Your game starts in 30 minutes!",
                    body = "Smash Pickle Club • Court 3 • 2:00 PM. Show your QR Booking Pass at reception.",
                    timeAgo = "Just now",
                    isRead = false
                ),
                NotificationEntity(
                    category = "BOOKING",
                    title = "Booking Confirmed ✓",
                    body = "Booking #PKL-20260928-00125 for Court 3 at Smash Pickle Club is confirmed.",
                    timeAgo = "1h ago",
                    isRead = false
                ),
                NotificationEntity(
                    category = "PAYMENT",
                    title = "GCash Payment Successful",
                    body = "₱320.00 paid via GCash for Smash Pickle Club reservation.",
                    timeAgo = "1h ago",
                    isRead = true
                ),
                NotificationEntity(
                    category = "OPEN_PLAY",
                    title = "Open-Play Invitation",
                    body = "Jon, Maria, and Carlo joined Saturday Open Play (5:00 PM - 7:00 PM). 3 spots left!",
                    timeAgo = "3h ago",
                    isRead = false
                ),
                NotificationEntity(
                    category = "TOURNAMENT",
                    title = "Tournament Quarter Finals Assigned",
                    body = "PicklePlay Open 2026: Jonel / Carlo vs Mark / James on Court 2.",
                    timeAgo = "5h ago",
                    isRead = true
                )
            )
        )
    }

    suspend fun createReservation(
        facility: FacilityEntity,
        court: CourtEntity,
        dateIso: String,
        dateLabel: String,
        timeSlot: String,
        timeRangeLabel: String,
        playerName: String,
        courtFee: Int,
        discountAmount: Int,
        serviceFee: Int,
        totalAmount: Int,
        paymentMethod: String,
        bookingCodeOverride: String? = null,
        recipientEmail: String? = null,
        isWalkIn: Boolean = false,
        initialStatus: String? = null
    ): BookingEntity {
        val randomSuffix = (10000..99999).random()
        val compactDate = dateIso.replace("-", "")
        val code = bookingCodeOverride ?: "PKL-$compactDate-$randomSuffix"
        val normalizedEmail = recipientEmail?.trim()?.ifBlank { null }
            ?: "${playerName.lowercase().replace(Regex("[^a-z0-9]+"), ".").trim('.').ifBlank { "player" }}@pickleplay.ph"
        val walkInFlag = isWalkIn || paymentMethod.equals("Cash on Hand", ignoreCase = true)
        val resolvedMethod = if (walkInFlag) "Cash on Hand" else paymentMethod
        val resolvedStatus = initialStatus ?: if (walkInFlag) {
            "PENDING_CASHIER_VERIFICATION"
        } else {
            "PENDING_ADMIN_APPROVAL"
        }
        val resolvedPaymentStatus = when (resolvedStatus) {
            "CONFIRMED", "CHECKED_IN" -> "PAID"
            "PENDING_CASHIER_VERIFICATION" -> if (walkInFlag) "CASH_ON_HAND_AT_CASHIER" else "FOR_VERIFICATION"
            "APPROVED_AWAITING_PAYMENT" -> "AWAITING_PAYMENT"
            else -> "AWAITING_APPROVAL"
        }

        val booking = BookingEntity(
            bookingCode = code,
            facilityId = facility.id,
            facilityName = facility.name,
            facilityLocation = facility.city,
            courtId = court.id,
            courtName = court.name,
            dateIso = dateIso,
            dateLabel = dateLabel,
            timeSlot = timeSlot,
            timeRangeLabel = timeRangeLabel,
            playerName = playerName,
            courtFee = courtFee,
            discountAmount = discountAmount,
            serviceFee = serviceFee,
            totalAmount = totalAmount,
            paymentMethod = resolvedMethod,
            paymentStatus = resolvedPaymentStatus,
            status = resolvedStatus,
            checkInTime = null,
            customerEmail = normalizedEmail,
            isWalkIn = walkInFlag
        )
        val id = dao.insertBooking(booking).toInt()
        val notificationTitle = if (walkInFlag) {
            "Walk-In Booking Created ($code) — Pay Cash at Cashier"
        } else {
            "Booking Request Sent to Admin ($code)"
        }
        val notificationBody = if (walkInFlag) {
            "${facility.name} • ${court.name} on $dateLabel ($timeRangeLabel). Mode: Cash on Hand (₱$totalAmount) at Cashier."
        } else {
            "${facility.name} • ${court.name} on $dateLabel ($timeRangeLabel) • Awaiting Admin Approval (₱$totalAmount)."
        }
        dao.insertNotification(
            NotificationEntity(
                category = "BOOKING",
                title = notificationTitle,
                body = notificationBody,
                timeAgo = "Just now",
                isRead = false
            )
        )
        dao.insertNotification(
            NotificationEntity(
                category = "EMAIL",
                title = "📧 Reservation Email Sent ($code)",
                body = "To: $normalizedEmail • ${facility.name} (${court.name}) on $dateLabel ($timeRangeLabel) • $resolvedMethod ($resolvedPaymentStatus) • Total: ₱$totalAmount.",
                timeAgo = "Just now",
                isRead = false
            )
        )
        return booking.copy(id = id)
    }

    suspend fun approveBookingByAdmin(booking: BookingEntity): BookingEntity {
        val nextStatus = if (booking.isWalkIn) "PENDING_CASHIER_VERIFICATION" else "APPROVED_AWAITING_PAYMENT"
        val nextPaymentStatus = if (booking.isWalkIn) "CASH_ON_HAND_AT_CASHIER" else "AWAITING_PAYMENT"
        val updated = booking.copy(
            status = nextStatus,
            paymentStatus = nextPaymentStatus,
            adminApprovedAt = "Approved Just Now"
        )
        dao.updateBooking(updated)
        dao.insertNotification(
            NotificationEntity(
                category = "BOOKING",
                title = "✓ Admin Approved Booking (${booking.bookingCode})",
                body = if (booking.isWalkIn) {
                    "${booking.courtName} (${booking.dateLabel} • ${booking.timeRangeLabel}) approved! Please pay Cash on Hand (₱${booking.totalAmount}) at the Cashier."
                } else {
                    "${booking.courtName} (${booking.dateLabel} • ${booking.timeRangeLabel}) approved! Please upload your payment screenshot/receipt and reference number."
                },
                timeAgo = "Just now",
                isRead = false
            )
        )
        return updated
    }

    suspend fun rejectBookingByAdmin(booking: BookingEntity): BookingEntity {
        val updated = booking.copy(
            status = "CANCELLED",
            paymentStatus = "DECLINED_BY_ADMIN"
        )
        dao.updateBooking(updated)
        dao.insertNotification(
            NotificationEntity(
                category = "BOOKING",
                title = "Booking Declined by Admin (${booking.bookingCode})",
                body = "Reservation request for ${booking.courtName} on ${booking.dateLabel} (${booking.timeRangeLabel}) was declined.",
                timeAgo = "Just now",
                isRead = false
            )
        )
        return updated
    }

    suspend fun submitCustomerPaymentProof(
        booking: BookingEntity,
        paymentMethod: String,
        referenceNumber: String,
        receiptUri: String,
        receiptFileName: String
    ): BookingEntity {
        val cleanRef = referenceNumber.trim()
        val cleanFile = receiptFileName.trim().ifBlank { "payment_receipt_${booking.bookingCode}.png" }
        val cleanUri = receiptUri.trim().ifBlank { "file://local/$cleanFile" }
        val updated = booking.copy(
            paymentMethod = paymentMethod,
            paymentStatus = "FOR_VERIFICATION",
            status = "PENDING_CASHIER_VERIFICATION",
            referenceNumber = cleanRef,
            receiptUri = cleanUri,
            receiptFileName = cleanFile
        )
        dao.updateBooking(updated)
        dao.insertNotification(
            NotificationEntity(
                category = "PAYMENT",
                title = "Payment Sent to Cashier (${booking.bookingCode})",
                body = "${booking.playerName} uploaded receipt ($cleanFile) with Ref #$cleanRef via $paymentMethod (₱${booking.totalAmount}). Awaiting Cashier verification.",
                timeAgo = "Just now",
                isRead = false
            )
        )
        return updated
    }

    suspend fun verifyPaymentByCashier(
        booking: BookingEntity,
        paymentMethodOverride: String? = null
    ): BookingEntity {
        val method = paymentMethodOverride ?: if (booking.isWalkIn) "Cash on Hand" else booking.paymentMethod
        val updated = booking.copy(
            paymentMethod = method,
            paymentStatus = "PAID",
            status = "CONFIRMED",
            cashierVerifiedAt = "Verified Just Now"
        )
        dao.updateBooking(updated)
        dao.insertNotification(
            NotificationEntity(
                category = "PAYMENT",
                title = "🎫 Payment Verified — Court QR Pass Ready (${booking.bookingCode})",
                body = "Cashier verified ₱${booking.totalAmount} ($method) for ${booking.courtName} (${booking.dateLabel} • ${booking.timeRangeLabel}). Show your Paid QR Pass at the court!",
                timeAgo = "Just now",
                isRead = false
            )
        )
        return updated
    }

    suspend fun cancelPendingReservation(booking: BookingEntity): BookingEntity {
        val updated = booking.copy(
            status = "CANCELLED",
            paymentStatus = if (booking.paymentStatus == "PAID") "REFUNDED (100%)" else "CANCELLED_BY_CUSTOMER"
        )
        dao.updateBooking(updated)
        dao.insertNotification(
            NotificationEntity(
                category = "BOOKING",
                title = "Pending Reservation Cancelled (${booking.bookingCode})",
                body = "Cancelled pending reservation for ${booking.courtName} on ${booking.dateLabel} (${booking.timeRangeLabel}).",
                timeAgo = "Just now",
                isRead = false
            )
        )
        return updated
    }

    suspend fun checkInBooking(booking: BookingEntity, checkInTimeStr: String) {
        val updated = booking.copy(
            status = "CHECKED_IN",
            paymentStatus = "PAID",
            checkInTime = checkInTimeStr
        )
        dao.updateBooking(updated)
        dao.insertNotification(
            NotificationEntity(
                category = "BOOKING",
                title = "✓ QR Check-In Verified",
                body = "${booking.playerName} checked in to ${booking.courtName} at $checkInTimeStr (${booking.bookingCode}).",
                timeAgo = "Just now",
                isRead = false
            )
        )
    }

    suspend fun cancelBooking(booking: BookingEntity, hoursBeforeNotice: Int) {
        val refundLabel = when {
            hoursBeforeNotice >= 24 -> "REFUNDED (100%)"
            hoursBeforeNotice >= 12 -> "REFUNDED (50%)"
            else -> "NO REFUND (<12h)"
        }
        val refundAmount = when {
            hoursBeforeNotice >= 24 -> booking.totalAmount
            hoursBeforeNotice >= 12 -> booking.totalAmount / 2
            else -> 0
        }
        dao.updateBooking(
            booking.copy(
                status = "CANCELLED",
                paymentStatus = refundLabel
            )
        )
        dao.insertNotification(
            NotificationEntity(
                category = "BOOKING",
                title = "Court Cancellation Processed",
                body = "Booking ${booking.bookingCode} cancelled. Policy applied: $refundLabel (₱$refundAmount returned).",
                timeAgo = "Just now",
                isRead = false
            )
        )
    }

    suspend fun submitBookingReview(booking: BookingEntity, stars: Int, comment: String) {
        dao.updateBooking(
            booking.copy(
                userRating = stars,
                userReview = comment.ifBlank { "Great court surface and lighting!" }
            )
        )
    }

    suspend fun toggleJoinOpenPlay(game: OpenPlayGameEntity, userName: String): OpenPlayGameEntity {
        val currentPlayers = game.playersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        val updatedJoined: Boolean
        if (game.isJoinedByUser) {
            currentPlayers.removeAll {
                it.equals(userName, ignoreCase = true) ||
                    it.startsWith("$userName|", ignoreCase = true) ||
                    it.startsWith("Jonel", ignoreCase = true)
            }
            updatedJoined = false
        } else {
            val alreadyInList = currentPlayers.any {
                it.equals(userName, ignoreCase = true) ||
                    it.startsWith("$userName|", ignoreCase = true)
            }
            if (currentPlayers.size < game.maxPlayers && !alreadyInList) {
                currentPlayers.add(userName)
            }
            updatedJoined = true
        }
        val updatedGame = game.copy(
            playersCsv = currentPlayers.joinToString(","),
            isJoinedByUser = updatedJoined
        )
        dao.updateOpenPlayGame(updatedGame)
        if (updatedJoined) {
            dao.insertNotification(
                NotificationEntity(
                    category = "OPEN_PLAY",
                    title = "Joined ${game.title} 🏓",
                    body = "You're in for ${game.dayLabel} (${game.timeRange}) at ${game.facilityName}. Fee: ₱${game.pricePerPlayer}.",
                    timeAgo = "Just now",
                    isRead = false
                )
            )
        }
        return updatedGame
    }

    suspend fun createOpenPlayGame(
        title: String,
        facilityName: String,
        courtName: String,
        dayLabel: String,
        timeRange: String,
        skillLevel: String,
        pricePerPlayer: Int,
        hostName: String,
        maxPlayers: Int = 6,
        initialPlayersCsv: String? = null
    ): OpenPlayGameEntity {
        val resolvedMaxPlayers = maxPlayers.coerceAtLeast(2)
        val resolvedPlayersCsv = initialPlayersCsv?.takeIf { it.isNotBlank() } ?: hostName
        val filledCount = resolvedPlayersCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.size
        val openSlots = (resolvedMaxPlayers - filledCount).coerceAtLeast(0)
        val entity = OpenPlayGameEntity(
            title = title,
            facilityId = 1,
            facilityName = facilityName,
            courtName = courtName,
            dayLabel = dayLabel,
            dateLabel = "Upcoming $dayLabel",
            timeRange = timeRange,
            skillLevel = skillLevel,
            pricePerPlayer = pricePerPlayer,
            maxPlayers = resolvedMaxPlayers,
            playersCsv = resolvedPlayersCsv,
            isJoinedByUser = true,
            hostName = hostName
        )
        val insertedId = dao.insertOpenPlayGame(entity).toInt()
        dao.insertNotification(
            NotificationEntity(
                category = "OPEN_PLAY",
                title = "New Matchmaker Open Play Session Live",
                body = "$title ($skillLevel) at $facilityName ($courtName) is now open with $openSlots open slot(s).",
                timeAgo = "Just now",
                isRead = false
            )
        )
        return entity.copy(id = insertedId)
    }

    suspend fun joinTournament(tournament: TournamentEntity, partnerName: String, userName: String) {
        if (tournament.isJoinedByUser) return
        val newTeamName = if (tournament.division == "Singles") userName else "$userName / $partnerName"
        dao.updateTournament(
            tournament.copy(
                isJoinedByUser = true,
                registeredCount = (tournament.registeredCount + 1).coerceAtMost(tournament.maxTeams),
                teamA = newTeamName
            )
        )
        dao.insertNotification(
            NotificationEntity(
                category = "TOURNAMENT",
                title = "Registered for ${tournament.name}",
                body = "Entry fee ₱${tournament.entryFee} confirmed. Assigned to ${tournament.activeCourt} (${tournament.activeRound}).",
                timeAgo = "Just now",
                isRead = false
            )
        )
    }

    suspend fun submitTournamentScore(
        tournament: TournamentEntity,
        s1A: Int,
        s1B: Int,
        s2A: Int,
        s2B: Int,
        s3A: Int,
        s3B: Int,
        profile: UserProfileEntity?
    ) {
        dao.updateTournament(
            tournament.copy(
                set1A = s1A,
                set1B = s1B,
                set2A = s2A,
                set2B = s2B,
                set3A = s3A,
                set3B = s3B,
                matchSubmitted = true
            )
        )
        val setsWonByA = listOf(s1A > s1B, s2A > s2B, s3A > s3B).count { it }
        val wonMatch = setsWonByA >= 2
        if (profile != null) {
            dao.updateUserProfile(
                profile.copy(
                    gamesPlayed = profile.gamesPlayed + 1,
                    wins = if (wonMatch) profile.wins + 1 else profile.wins,
                    winStreak = if (wonMatch) profile.winStreak + 1 else 0,
                    duprRating = ((profile.duprRating + if (wonMatch) 0.04 else -0.01) * 100).toInt() / 100.0
                )
            )
        }
        dao.insertNotification(
            NotificationEntity(
                category = "TOURNAMENT",
                title = "Match Score Submitted (${tournament.name})",
                body = "${tournament.teamA} vs ${tournament.teamB}: $s1A-$s1B, $s2A-$s2B, $s3A-$s3B.",
                timeAgo = "Just now",
                isRead = false
            )
        )
    }

    suspend fun createTournamentByAdmin(
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
    ): TournamentEntity {
        val created = TournamentEntity(
            name = name,
            facilityName = "Smash Pickle Club • Quezon City",
            dateRange = dateRange,
            division = division,
            format = format,
            skillCap = skillCap,
            entryFee = entryFee,
            prizePool = prizePool,
            registeredCount = 4,
            maxTeams = maxTeams,
            isJoinedByUser = false,
            activeRound = "Quarter Finals",
            activeCourt = activeCourt,
            teamA = teamA,
            teamB = teamB,
            set1A = 0,
            set1B = 0,
            set2A = 0,
            set2B = 0,
            set3A = 0,
            set3B = 0,
            matchSubmitted = false
        )
        val id = dao.insertTournament(created).toInt()
        dao.insertNotification(
            NotificationEntity(
                category = "TOURNAMENT",
                title = "🏆 New Tournament Bracket Published: $name",
                body = "$division ($format • $skillCap) on $dateRange. Prize Pool: ₱$prizePool. Registration & schedule live!",
                timeAgo = "Just now",
                isRead = false
            )
        )
        return created.copy(id = id)
    }

    suspend fun updateTournamentBracketByAdmin(
        tournament: TournamentEntity,
        activeRound: String,
        activeCourt: String,
        teamA: String,
        teamB: String
    ) {
        dao.updateTournament(
            tournament.copy(
                activeRound = activeRound.ifBlank { tournament.activeRound },
                activeCourt = activeCourt.ifBlank { tournament.activeCourt },
                teamA = teamA.ifBlank { tournament.teamA },
                teamB = teamB.ifBlank { tournament.teamB }
            )
        )
        dao.insertNotification(
            NotificationEntity(
                category = "TOURNAMENT",
                title = "Bracket Updated: ${tournament.name}",
                body = "Stage: $activeRound • $teamA vs $teamB on $activeCourt.",
                timeAgo = "Just now",
                isRead = false
            )
        )
    }

    suspend fun updateMembership(profile: UserProfileEntity, tier: String, price: Int, discountPct: Int) {
        dao.updateUserProfile(
            profile.copy(
                membershipTier = tier,
                membershipPrice = price,
                discountPercent = discountPct
            )
        )
        dao.insertNotification(
            NotificationEntity(
                category = "PAYMENT",
                title = "Membership Upgraded to $tier",
                body = "You now enjoy $discountPct% court booking discount and priority club benefits!",
                timeAgo = "Just now",
                isRead = false
            )
        )
    }

    suspend fun updatePlayerPreferences(profile: UserProfileEntity, position: String, skill: String) {
        dao.updateUserProfile(
            profile.copy(
                preferredPosition = position,
                skillLevel = skill
            )
        )
    }

    suspend fun updateUserSkillLevel(profile: UserProfileEntity, skillLabel: String, duprRating: Double) {
        dao.updateUserProfile(
            profile.copy(
                skillLevel = skillLabel,
                duprRating = duprRating
            )
        )
    }

    suspend fun updateUserName(profile: UserProfileEntity, fullName: String) {
        val clean = fullName.trim().ifBlank { profile.fullName }
        val firstName = clean.substringBefore(" ").ifBlank { clean }
        dao.updateUserProfile(
            profile.copy(
                name = firstName,
                fullName = clean
            )
        )
    }

    // Admin Operations
    suspend fun toggleCourtMaintenance(court: CourtEntity) {
        val nextStatus = if (court.status == "ACTIVE") "MAINTENANCE" else "ACTIVE"
        dao.updateCourt(court.copy(status = nextStatus))
    }

    suspend fun updateClubDetails(
        facility: FacilityEntity,
        name: String,
        city: String,
        address: String,
        operatingHours: String,
        surfaceType: String,
        isIndoor: Boolean,
        amenitiesCsv: String
    ) {
        dao.updateFacility(
            facility.copy(
                name = name.trim().ifBlank { facility.name },
                city = city.trim().ifBlank { facility.city },
                address = address.trim().ifBlank { facility.address },
                operatingHours = operatingHours.trim().ifBlank { facility.operatingHours },
                surfaceType = surfaceType.trim().ifBlank { facility.surfaceType },
                isIndoor = isIndoor,
                amenitiesCsv = amenitiesCsv.trim().ifBlank { facility.amenitiesCsv }
            )
        )
    }

    suspend fun updateCourtPricing(
        court: CourtEntity,
        name: String,
        courtType: String,
        morning: Int,
        midday: Int,
        peak: Int,
        night: Int,
        weekend: Int
    ) {
        dao.updateCourt(
            court.copy(
                name = name.trim().ifBlank { court.name },
                courtType = courtType.trim().ifBlank { court.courtType },
                morningPrice = morning,
                middayPrice = midday,
                peakPrice = peak,
                nightPrice = night,
                weekendPrice = weekend
            )
        )
    }

    suspend fun updateCourtStatusAndPricing(
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
        dao.updateCourt(
            court.copy(
                status = status,
                name = name.trim().ifBlank { court.name },
                courtType = courtType.trim().ifBlank { court.courtType },
                morningPrice = morning,
                middayPrice = midday,
                peakPrice = peak,
                nightPrice = night,
                weekendPrice = weekend
            )
        )
    }

    suspend fun collectBookingPayment(booking: BookingEntity, paymentMethod: String, checkInNow: Boolean = true) {
        val updated = booking.copy(
            paymentMethod = paymentMethod,
            paymentStatus = "PAID",
            status = if (checkInNow) "CHECKED_IN" else booking.status,
            checkInTime = if (checkInNow && booking.checkInTime == null) "2:05 PM" else booking.checkInTime
        )
        dao.updateBooking(updated)
        dao.insertNotification(
            NotificationEntity(
                category = "PAYMENT",
                title = "Cashier POS Payment Collected (₱${booking.totalAmount})",
                body = "Received ₱${booking.totalAmount} via $paymentMethod for ${booking.playerName} (${booking.courtName}).",
                timeAgo = "Just now",
                isRead = false
            )
        )
    }

    suspend fun addNewCourt(facility: FacilityEntity?, courtNumber: Int, isIndoor: Boolean, courtType: String) {
        val targetFacilityId = facility?.id ?: 1
        dao.insertCourt(
            CourtEntity(
                facilityId = targetFacilityId,
                name = "Court $courtNumber",
                courtType = courtType,
                isIndoor = isIndoor,
                status = "ACTIVE",
                morningPrice = 200,
                middayPrice = 250,
                peakPrice = 350,
                nightPrice = 250,
                weekendPrice = 350
            )
        )
        if (facility != null) {
            dao.updateFacility(facility.copy(courtCount = courtNumber))
        }
    }

    suspend fun markAllNotificationsRead() {
        dao.markAllNotificationsRead()
    }

    suspend fun saveBusinessSettingsEntity(settings: BusinessSettingsEntity) {
        dao.upsertBusinessSettings(settings)
    }

    suspend fun getBusinessSettingsOnce(): BusinessSettingsEntity? {
        return dao.getBusinessSettingsOnce()
    }
}

