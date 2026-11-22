package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        ServiceEntity::class,
        OfferEntity::class,
        DiscountCodeEntity::class,
        ContestEntity::class,
        ContestEntryEntity::class,
        OrderEntity::class,
        TopUpEntity::class,
        AuditLogEntity::class,
        SettingsEntity::class,
        AuthRateLimitEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TwinCashDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun serviceDao(): ServiceDao
    abstract fun offerDao(): OfferDao
    abstract fun discountCodeDao(): DiscountCodeDao
    abstract fun contestDao(): ContestDao
    abstract fun contestEntryDao(): ContestEntryDao
    abstract fun orderDao(): OrderDao
    abstract fun topUpDao(): TopUpDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun settingsDao(): SettingsDao
    abstract fun authRateLimitDao(): AuthRateLimitDao

    companion object {
        @Volatile
        private var INSTANCE: TwinCashDatabase? = null

        fun getInstance(context: Context): TwinCashDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TwinCashDatabase::class.java,
                    "twincash_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
