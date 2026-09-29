package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FacilityEntity::class,
        CourtEntity::class,
        BookingEntity::class,
        OpenPlayGameEntity::class,
        TournamentEntity::class,
        NotificationEntity::class,
        UserProfileEntity::class,
        BusinessSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PicklePlayDatabase : RoomDatabase() {
    abstract fun picklePlayDao(): PicklePlayDao

    companion object {
        @Volatile
        private var INSTANCE: PicklePlayDatabase? = null

        fun getDatabase(context: Context): PicklePlayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PicklePlayDatabase::class.java,
                    "pickleplay_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
