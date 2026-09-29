package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PicklePlayDao {

    // Facilities
    @Query("SELECT * FROM facilities ORDER BY isFeatured DESC, distanceKm ASC")
    fun getAllFacilities(): Flow<List<FacilityEntity>>

    @Query("SELECT COUNT(*) FROM facilities")
    suspend fun getFacilityCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacilities(facilities: List<FacilityEntity>)

    @Update
    suspend fun updateFacility(facility: FacilityEntity)

    // Courts
    @Query("SELECT * FROM courts ORDER BY facilityId ASC, id ASC")
    fun getAllCourts(): Flow<List<CourtEntity>>

    @Query("SELECT * FROM courts WHERE facilityId = :facilityId ORDER BY id ASC")
    fun getCourtsForFacility(facilityId: Int): Flow<List<CourtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourts(courts: List<CourtEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourt(court: CourtEntity): Long

    @Update
    suspend fun updateCourt(court: CourtEntity)

    @Query("UPDATE courts SET courtType = 'Pro Cushion Indoor', status = 'ACTIVE' WHERE courtType LIKE '%Championship%'")
    suspend fun removeChampionshipFromCourtTypes()

    // Bookings / Court Reservations (Upcoming & Past)
    @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
    fun getAllBookings(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE status NOT IN ('CANCELLED', 'CHECKED_IN', 'COMPLETED') ORDER BY dateIso ASC, createdAt DESC")
    fun getUpcomingBookings(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE status IN ('CANCELLED', 'CHECKED_IN', 'COMPLETED') ORDER BY dateIso DESC, createdAt DESC")
    fun getPastBookings(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE LOWER(playerName) = LOWER(:playerName) OR LOWER(customerEmail) = LOWER(:customerEmail) ORDER BY createdAt DESC")
    fun getUserReservations(playerName: String, customerEmail: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE (LOWER(playerName) = LOWER(:playerName) OR LOWER(customerEmail) = LOWER(:customerEmail)) AND status NOT IN ('CANCELLED', 'CHECKED_IN', 'COMPLETED') ORDER BY dateIso ASC, createdAt DESC")
    fun getUserUpcomingReservations(playerName: String, customerEmail: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE (LOWER(playerName) = LOWER(:playerName) OR LOWER(customerEmail) = LOWER(:customerEmail)) AND status IN ('CANCELLED', 'CHECKED_IN', 'COMPLETED') ORDER BY dateIso DESC, createdAt DESC")
    fun getUserPastReservations(playerName: String, customerEmail: String): Flow<List<BookingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookings(bookings: List<BookingEntity>)

    @Update
    suspend fun updateBooking(booking: BookingEntity)

    @Query("UPDATE bookings SET status = 'COMPLETED', paymentStatus = 'PAID', checkInTime = :completedTime WHERE id = :bookingId")
    suspend fun markBookingCompleted(bookingId: Int, completedTime: String = "Completed")

    @Query("DELETE FROM bookings WHERE id = :bookingId")
    suspend fun deleteBookingById(bookingId: Int)

    // Open Play Games / Matchmaker Sessions
    @Query("SELECT * FROM open_play_games ORDER BY id ASC")
    fun getAllOpenPlayGames(): Flow<List<OpenPlayGameEntity>>

    @Query("SELECT * FROM open_play_games WHERE (:skillLevel = 'All' OR LOWER(skillLevel) = LOWER(:skillLevel)) ORDER BY id ASC")
    fun getOpenPlayGamesBySkillLevel(skillLevel: String): Flow<List<OpenPlayGameEntity>>

    @Query("SELECT * FROM open_play_games WHERE isJoinedByUser = 1 ORDER BY id ASC")
    fun getJoinedOpenPlayGames(): Flow<List<OpenPlayGameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpenPlayGames(games: List<OpenPlayGameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpenPlayGame(game: OpenPlayGameEntity): Long

    @Update
    suspend fun updateOpenPlayGame(game: OpenPlayGameEntity)

    @Query("DELETE FROM open_play_games WHERE id = :gameId")
    suspend fun deleteOpenPlayGameById(gameId: Int)

    // Tournaments
    @Query("SELECT * FROM tournaments ORDER BY id ASC")
    fun getAllTournaments(): Flow<List<TournamentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournaments(tournaments: List<TournamentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity): Long

    @Update
    suspend fun updateTournament(tournament: TournamentEntity)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateUserProfile(profile: UserProfileEntity)

    // Business Settings (Admin Settings & Payment QR Codes)
    @Query("SELECT * FROM business_settings WHERE id = 1")
    fun getBusinessSettings(): Flow<BusinessSettingsEntity?>

    @Query("SELECT * FROM business_settings WHERE id = 1")
    suspend fun getBusinessSettingsOnce(): BusinessSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBusinessSettings(settings: BusinessSettingsEntity)
}

