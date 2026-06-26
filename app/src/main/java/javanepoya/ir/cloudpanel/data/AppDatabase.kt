package javanepoya.ir.cloudpanel.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY alias ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Int): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)
}

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers WHERE accountId = :accountId ORDER BY name ASC")
    fun getWorkersForAccount(accountId: Int): Flow<List<WorkerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity): Long

    @Update
    suspend fun updateWorker(worker: WorkerEntity)

    @Delete
    suspend fun deleteWorker(worker: WorkerEntity)
}

@Dao
interface DnsRecordDao {
    @Query("SELECT * FROM dns_records WHERE accountId = :accountId ORDER BY name ASC")
    fun getDnsRecordsForAccount(accountId: Int): Flow<List<DnsRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDnsRecord(record: DnsRecordEntity): Long

    @Update
    suspend fun updateDnsRecord(record: DnsRecordEntity)

    @Delete
    suspend fun deleteDnsRecord(record: DnsRecordEntity)
}

@Dao
interface R2BucketDao {
    @Query("SELECT * FROM r2_buckets WHERE accountId = :accountId ORDER BY name ASC")
    fun getR2BucketsForAccount(accountId: Int): Flow<List<R2BucketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertR2Bucket(bucket: R2BucketEntity): Long

    @Delete
    suspend fun deleteR2Bucket(bucket: R2BucketEntity)
}

@Dao
interface KvNamespaceDao {
    @Query("SELECT * FROM kv_namespaces WHERE accountId = :accountId ORDER BY name ASC")
    fun getKvNamespacesForAccount(accountId: Int): Flow<List<KvNamespaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKvNamespace(namespace: KvNamespaceEntity): Long

    @Update
    suspend fun updateKvNamespace(namespace: KvNamespaceEntity)

    @Delete
    suspend fun deleteKvNamespace(namespace: KvNamespaceEntity)
}

@Dao
interface D1DatabaseDao {
    @Query("SELECT * FROM d1_databases WHERE accountId = :accountId ORDER BY name ASC")
    fun getD1DatabasesForAccount(accountId: Int): Flow<List<D1DatabaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertD1Database(database: D1DatabaseEntity): Long

    @Delete
    suspend fun deleteD1Database(database: D1DatabaseEntity)
}

@Dao
interface TunnelDao {
    @Query("SELECT * FROM tunnels WHERE accountId = :accountId ORDER BY name ASC")
    fun getTunnelsForAccount(accountId: Int): Flow<List<TunnelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTunnel(tunnel: TunnelEntity): Long

    @Update
    suspend fun updateTunnel(tunnel: TunnelEntity)

    @Delete
    suspend fun deleteTunnel(tunnel: TunnelEntity)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs WHERE accountId = :accountId ORDER BY timestamp DESC LIMIT 50")
    fun getAuditLogsForAccount(accountId: Int): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long
}

@Database(
    entities = [
        AccountEntity::class,
        WorkerEntity::class,
        DnsRecordEntity::class,
        R2BucketEntity::class,
        KvNamespaceEntity::class,
        D1DatabaseEntity::class,
        TunnelEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun workerDao(): WorkerDao
    abstract fun dnsRecordDao(): DnsRecordDao
    abstract fun r2BucketDao(): R2BucketDao
    abstract fun kvNamespaceDao(): KvNamespaceDao
    abstract fun d1DatabaseDao(): D1DatabaseDao
    abstract fun tunnelDao(): TunnelDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cf_switcher_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
