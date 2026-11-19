package com.stasybell.os.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        OwnerSessionEntity::class,
        ClientEntity::class,
        ServiceEntity::class,
        WorkingWindowEntity::class,
        CalendarEntryEntity::class,
        CalendarEventEntity::class,
        FinanceTransactionEntity::class,
        MonthClosureEntity::class,
        MaterialEntity::class,
        MaterialMovementEntity::class,
        LegacyVisitEntity::class,
        LegacyMonthlySummaryEntity::class,
        LegacyCatalogItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StasyBellDatabase : RoomDatabase() {
    abstract fun dao(): StasyBellDao

    companion object {
        @Volatile
        private var instance: StasyBellDatabase? = null

        fun getInstance(context: Context): StasyBellDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StasyBellDatabase::class.java,
                    "stasy_bell_os_demo.db"
                ).build().also { instance = it }
            }
        }
    }
}
