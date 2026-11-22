package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    fun observeUserById(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUserById(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE whatsapp = :whatsapp LIMIT 1")
    suspend fun getUserByWhatsapp(whatsapp: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services ORDER BY displayOrder ASC, createdAt ASC")
    fun observeAllServices(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE id = :id LIMIT 1")
    suspend fun getServiceById(id: String): ServiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertService(service: ServiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(services: List<ServiceEntity>)

    @Query("DELETE FROM services WHERE id = :id")
    suspend fun deleteService(id: String)

    @Query("SELECT COUNT(*) FROM services")
    suspend fun countServices(): Int
}

@Dao
interface OfferDao {
    @Query("SELECT * FROM offers ORDER BY createdAt DESC")
    fun observeAllOffers(): Flow<List<OfferEntity>>

    @Query("SELECT * FROM offers WHERE id = :id LIMIT 1")
    suspend fun getOfferById(id: String): OfferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOffer(offer: OfferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(offers: List<OfferEntity>)

    @Query("DELETE FROM offers WHERE id = :id")
    suspend fun deleteOffer(id: String)
}

@Dao
interface DiscountCodeDao {
    @Query("SELECT * FROM discount_codes ORDER BY createdAt DESC")
    fun observeAllCodes(): Flow<List<DiscountCodeEntity>>

    @Query("SELECT * FROM discount_codes WHERE isPublic = 1 OR targetCustomerId = :customerId ORDER BY createdAt DESC")
    fun observeCodesForCustomer(customerId: String): Flow<List<DiscountCodeEntity>>

    @Query("SELECT * FROM discount_codes WHERE UPPER(code) = UPPER(:code) LIMIT 1")
    suspend fun getByCode(code: String): DiscountCodeEntity?

    @Query("SELECT * FROM discount_codes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DiscountCodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCode(entity: DiscountCodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<DiscountCodeEntity>)

    @Query("DELETE FROM discount_codes WHERE id = :id")
    suspend fun deleteCode(id: String)
}

@Dao
interface ContestDao {
    @Query("SELECT * FROM contests ORDER BY createdAt DESC")
    fun observeAllContests(): Flow<List<ContestEntity>>

    @Query("SELECT * FROM contests WHERE id = :id LIMIT 1")
    suspend fun getContestById(id: String): ContestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContest(contest: ContestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contests: List<ContestEntity>)

    @Query("DELETE FROM contests WHERE id = :id")
    suspend fun deleteContest(id: String)

    @Query("UPDATE contests SET isWinnerNotified = 1 WHERE winnerCustomerId = :customerId AND isPrizeApproved = 1")
    suspend fun markWinnerNotified(customerId: String)
}

@Dao
interface ContestEntryDao {
    @Query("SELECT * FROM contest_entries ORDER BY createdAt DESC")
    fun observeAllEntries(): Flow<List<ContestEntryEntity>>

    @Query("SELECT * FROM contest_entries WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun observeEntriesForCustomer(customerId: String): Flow<List<ContestEntryEntity>>

    @Query("SELECT * FROM contest_entries WHERE contestId = :contestId ORDER BY createdAt ASC")
    suspend fun getEntriesForContest(contestId: String): List<ContestEntryEntity>

    @Query("SELECT * FROM contest_entries WHERE contestId = :contestId AND customerId = :customerId LIMIT 1")
    suspend fun getEntry(contestId: String, customerId: String): ContestEntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: ContestEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<ContestEntryEntity>)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun observeOrdersByCustomer(customerId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE status = 'pending' OR merchantId = :merchantId ORDER BY createdAt DESC")
    fun observeOrdersForMerchant(merchantId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<OrderEntity>)

    @Query("UPDATE orders SET isCustomerNotified = 1 WHERE customerId = :customerId AND isCustomerNotified = 0 AND status != 'pending'")
    suspend fun markCustomerOrdersNotified(customerId: String)
}

@Dao
interface TopUpDao {
    @Query("SELECT * FROM topups ORDER BY createdAt DESC")
    fun observeAllTopUps(): Flow<List<TopUpEntity>>

    @Query("SELECT * FROM topups WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun observeTopUpsByCustomer(customerId: String): Flow<List<TopUpEntity>>

    @Query("SELECT * FROM topups WHERE id = :id LIMIT 1")
    suspend fun getTopUpById(id: String): TopUpEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTopUp(topUp: TopUpEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(topUps: List<TopUpEntity>)

    @Query("UPDATE topups SET isCustomerNotified = 1 WHERE customerId = :customerId AND isCustomerNotified = 0 AND status != 'pending'")
    suspend fun markCustomerTopUpsNotified(customerId: String)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC")
    fun observeAllLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<AuditLogEntity>)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 'global' LIMIT 1")
    fun observeSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 'global' LIMIT 1")
    suspend fun getSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: SettingsEntity)
}

@Dao
interface AuthRateLimitDao {
    @Query("SELECT * FROM auth_rate_limits WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getRateLimit(email: String): AuthRateLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRateLimit(entity: AuthRateLimitEntity)

    @Query("DELETE FROM auth_rate_limits WHERE LOWER(email) = LOWER(:email)")
    suspend fun clearRateLimit(email: String)
}
