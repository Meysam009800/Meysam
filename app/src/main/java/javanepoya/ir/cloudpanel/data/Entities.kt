package javanepoya.ir.cloudpanel.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    val alias: String,
    val email: String,
    val apiToken: String,
    val workspace: String, // "Personal", "Client", "Company"
    val mode: String, // "Simple", "Professional"
    val workerCount: Int = 0,
    val zoneCount: Int = 0,
    val lastActiveTime: Long = System.currentTimeMillis(),
    val connectionStatus: String = "Disconnected", // "Connected", "Disconnected"
    val cookies: String = "", // Serialized WebView cookies for session isolation
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)

@Entity(tableName = "workers")
data class WorkerEntity(
    val accountId: Int,
    val name: String,
    val script: String,
    val routes: String = "*",
    val status: String = "Active", // "Active", "Inactive"
    val updatedAt: Long = System.currentTimeMillis(),
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)

@Entity(tableName = "dns_records")
data class DnsRecordEntity(
    val accountId: Int,
    val zoneName: String,
    val type: String, // A, AAAA, CNAME, TXT, MX, NS, SRV
    val name: String,
    val content: String,
    val ttl: Int = 300,
    val proxied: Boolean = true,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)

@Entity(tableName = "r2_buckets")
data class R2BucketEntity(
    val accountId: Int,
    val name: String,
    val size: String = "0 B",
    val objectCount: Int = 0,
    val creationDate: Long = System.currentTimeMillis(),
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)

@Entity(tableName = "kv_namespaces")
data class KvNamespaceEntity(
    val accountId: Int,
    val name: String,
    val keysJson: String = "{}", // JSON string containing local key-value storage mock/actual data
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)

@Entity(tableName = "d1_databases")
data class D1DatabaseEntity(
    val accountId: Int,
    val name: String,
    val uuid: String,
    val size: String = "128 KB",
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)

@Entity(tableName = "tunnels")
data class TunnelEntity(
    val accountId: Int,
    val name: String,
    val status: String = "Healthy", // "Healthy", "Degraded", "Down"
    val connections: Int = 1,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    val accountId: Int,
    val action: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis(),
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)
