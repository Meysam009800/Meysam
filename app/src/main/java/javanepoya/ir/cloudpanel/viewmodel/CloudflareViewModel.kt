package javanepoya.ir.cloudpanel.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import javanepoya.ir.cloudpanel.data.*
import javanepoya.ir.cloudpanel.api.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

class CloudflareViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val securityManager = SecurityManager(application)
    private val cookieStorage = CookieStorage(application, securityManager)
    private val sessionManager = SessionManager(application, securityManager, cookieStorage)

    private val accountDao = database.accountDao()
    private val workerDao = database.workerDao()
    private val dnsRecordDao = database.dnsRecordDao()
    private val r2BucketDao = database.r2BucketDao()
    private val kvNamespaceDao = database.kvNamespaceDao()
    private val d1DatabaseDao = database.d1DatabaseDao()
    private val tunnelDao = database.tunnelDao()
    private val auditLogDao = database.auditLogDao()

    // API Engine Integration
    private val authManager = AuthenticationManager(application, securityManager, sessionManager)
    private val cloudflareClient = CloudflareClient(authManager)
    private val apiService = cloudflareClient.apiService

    val tokenValidator = TokenValidator(apiService)
    val zoneRepository = ZoneRepository(apiService, accountDao, auditLogDao)
    val workerRepository = WorkerRepository(apiService, workerDao, accountDao, auditLogDao)
    val dnsRepository = DnsRepository(apiService, dnsRecordDao, accountDao, auditLogDao)
    val analyticsRepository = AnalyticsRepository(apiService, auditLogDao)

    // Sync States
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    fun clearSyncError() {
        _syncError.value = null
    }

    // Zone Settings states (reactive to currentAccount)
    private val _zoneSslMode = MutableStateFlow("Full (Strict)")
    val zoneSslMode: StateFlow<String> = _zoneSslMode.asStateFlow()

    private val _zoneSecurityLevel = MutableStateFlow("Medium")
    val zoneSecurityLevel: StateFlow<String> = _zoneSecurityLevel.asStateFlow()

    private val _zoneFirewallActive = MutableStateFlow(true)
    val zoneFirewallActive: StateFlow<Boolean> = _zoneFirewallActive.asStateFlow()

    private val _zoneWafActive = MutableStateFlow(true)
    val zoneWafActive: StateFlow<Boolean> = _zoneWafActive.asStateFlow()

    private val _zoneProxyDefault = MutableStateFlow(true)
    val zoneProxyDefault: StateFlow<Boolean> = _zoneProxyDefault.asStateFlow()

    private val _zoneCacheLevel = MutableStateFlow("Standard") // Bypass, Standard, Aggressive
    val zoneCacheLevel: StateFlow<String> = _zoneCacheLevel.asStateFlow()

    private fun loadZoneSettings(accountId: Int) {
        val prefs = getApplication<Application>().getSharedPreferences("zone_settings_$accountId", Context.MODE_PRIVATE)
        _zoneSslMode.value = prefs.getString("ssl_mode", "Full (Strict)") ?: "Full (Strict)"
        _zoneSecurityLevel.value = prefs.getString("security_level", "Medium") ?: "Medium"
        _zoneFirewallActive.value = prefs.getBoolean("firewall_active", true)
        _zoneWafActive.value = prefs.getBoolean("waf_active", true)
        _zoneProxyDefault.value = prefs.getBoolean("proxy_default", true)
        _zoneCacheLevel.value = prefs.getString("cache_level", "Standard") ?: "Standard"
    }

    fun updateZoneSslMode(mode: String) {
        val account = _currentAccount.value ?: return
        _zoneSslMode.value = mode
        val prefs = getApplication<Application>().getSharedPreferences("zone_settings_${account.id}", Context.MODE_PRIVATE)
        prefs.edit().putString("ssl_mode", mode).apply()
        viewModelScope.launch {
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "SSL Mode updated to $mode", "Success"))
        }
    }

    fun updateZoneSecurityLevel(lvl: String) {
        val account = _currentAccount.value ?: return
        _zoneSecurityLevel.value = lvl
        val prefs = getApplication<Application>().getSharedPreferences("zone_settings_${account.id}", Context.MODE_PRIVATE)
        prefs.edit().putString("security_level", lvl).apply()
        viewModelScope.launch {
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "Security Level updated to $lvl", "Success"))
        }
    }

    fun toggleZoneFirewall(active: Boolean) {
        val account = _currentAccount.value ?: return
        _zoneFirewallActive.value = active
        val prefs = getApplication<Application>().getSharedPreferences("zone_settings_${account.id}", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("firewall_active", active).apply()
        viewModelScope.launch {
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "Firewall ${if (active) "enabled" else "disabled"}", "Success"))
        }
    }

    fun toggleZoneWaf(active: Boolean) {
        val account = _currentAccount.value ?: return
        _zoneWafActive.value = active
        val prefs = getApplication<Application>().getSharedPreferences("zone_settings_${account.id}", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("waf_active", active).apply()
        viewModelScope.launch {
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "WAF ${if (active) "enabled" else "disabled"}", "Success"))
        }
    }

    fun toggleZoneProxy(active: Boolean) {
        val account = _currentAccount.value ?: return
        _zoneProxyDefault.value = active
        val prefs = getApplication<Application>().getSharedPreferences("zone_settings_${account.id}", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("proxy_default", active).apply()
        viewModelScope.launch {
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "Global Proxy Default ${if (active) "enabled" else "disabled"}", "Success"))
        }
    }

    fun updateZoneCacheLevel(level: String) {
        val account = _currentAccount.value ?: return
        _zoneCacheLevel.value = level
        val prefs = getApplication<Application>().getSharedPreferences("zone_settings_${account.id}", Context.MODE_PRIVATE)
        prefs.edit().putString("cache_level", level).apply()
        viewModelScope.launch {
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "Cache Level updated to $level", "Success"))
        }
    }


    // Active Account States
    val allAccounts: StateFlow<List<AccountEntity>> = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentAccount = MutableStateFlow<AccountEntity?>(null)
    val currentAccount: StateFlow<AccountEntity?> = _currentAccount.asStateFlow()

    // Workspace & Global Search
    private val _selectedWorkspace = MutableStateFlow("All") // "All", "Personal", "Client", "Company"
    val selectedWorkspace: StateFlow<String> = _selectedWorkspace.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // UI States
    private val _currentScreen = MutableStateFlow("dashboard") // "dashboard", "workers", "dns", "r2", "kv", "d1", "analytics", "tunnels", "notifications", "settings", "zone_manager"
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _appLanguage = MutableStateFlow(securityManager.getLanguage())
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _isLocked = MutableStateFlow(securityManager.hasPin())
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(securityManager.isBiometricEnabled())
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    // Dynamic Lists (scoped to active account)
    val currentWorkers: StateFlow<List<WorkerEntity>> = _currentAccount
        .flatMapLatest { account ->
            if (account != null) workerDao.getWorkersForAccount(account.id)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentDnsRecords: StateFlow<List<DnsRecordEntity>> = _currentAccount
        .flatMapLatest { account ->
            if (account != null) dnsRecordDao.getDnsRecordsForAccount(account.id)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentR2Buckets: StateFlow<List<R2BucketEntity>> = _currentAccount
        .flatMapLatest { account ->
            if (account != null) r2BucketDao.getR2BucketsForAccount(account.id)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentKvNamespaces: StateFlow<List<KvNamespaceEntity>> = _currentAccount
        .flatMapLatest { account ->
            if (account != null) kvNamespaceDao.getKvNamespacesForAccount(account.id)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentD1Databases: StateFlow<List<D1DatabaseEntity>> = _currentAccount
        .flatMapLatest { account ->
            if (account != null) d1DatabaseDao.getD1DatabasesForAccount(account.id)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentTunnels: StateFlow<List<TunnelEntity>> = _currentAccount
        .flatMapLatest { account ->
            if (account != null) tunnelDao.getTunnelsForAccount(account.id)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentAuditLogs: StateFlow<List<AuditLogEntity>> = _currentAccount
        .flatMapLatest { account ->
            if (account != null) auditLogDao.getAuditLogsForAccount(account.id)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active worker editor states
    private val _editorCode = MutableStateFlow("")
    val editorCode: StateFlow<String> = _editorCode.asStateFlow()

    private val _editorWorker = MutableStateFlow<WorkerEntity?>(null)
    val editorWorker: StateFlow<WorkerEntity?> = _editorWorker.asStateFlow()

    private val _editorIsProfessional = MutableStateFlow(false)
    val editorIsProfessional: StateFlow<Boolean> = _editorIsProfessional.asStateFlow()

    // Undo/Redo stacks for editor
    private var undoStack = mutableListOf<String>()
    private var redoStack = mutableListOf<String>()

    init {
        // Automatically select the last active account (or first available) on startup
        viewModelScope.launch {
            allAccounts.collect { accounts ->
                if (_currentAccount.value == null && accounts.isNotEmpty()) {
                    val sharedPrefs = application.getSharedPreferences("cf_switcher_secure_prefs", android.content.Context.MODE_PRIVATE)
                    val lastActiveId = sharedPrefs.getInt("last_active_account_id", 0)
                    val targetAccount = accounts.find { it.id == lastActiveId } ?: accounts.first()
                    selectAccount(targetAccount)
                }
            }
        }
    }

    // Security Unlock
    fun unlockWithPin(pin: String): Boolean {
        val success = securityManager.verifyPin(pin)
        if (success) {
            _isLocked.value = false
        }
        return success
    }

    fun lockApp() {
        if (securityManager.hasPin()) {
            _isLocked.value = true
        }
    }

    fun configurePin(pin: String) {
        securityManager.setPin(pin)
        _isLocked.value = false
    }

    fun disablePin() {
        securityManager.clearPin()
        _isLocked.value = false
    }

    fun toggleBiometric(enabled: Boolean) {
        securityManager.setBiometricEnabled(enabled)
        _biometricEnabled.value = enabled
    }

    // Language switcher
    fun setLanguage(lang: String) {
        securityManager.setLanguage(lang)
        _appLanguage.value = lang
    }

    // Workspace filter switcher
    fun selectWorkspace(workspace: String) {
        _selectedWorkspace.value = workspace
    }

    // Screen navigation
    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // Account Management (isolated switching, cookies extraction)
    fun selectAccount(account: AccountEntity) {
        viewModelScope.launch {
            val oldAccountId = _currentAccount.value?.id ?: -1
            val toAccountId = account.id

            // Load persistent Zone settings
            loadZoneSettings(toAccountId)

            // Switch session via SessionManager
            sessionManager.switchSession(oldAccountId, toAccountId)

            // Update account's last active timestamp
            val updated = account.copy(lastActiveTime = System.currentTimeMillis(), connectionStatus = "Connected")
            accountDao.updateAccount(updated)
            _currentAccount.value = updated

            // Load audit logs
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = account.id,
                    action = "Account Connected",
                    status = "Success"
                )
            )
        }
    }

    fun saveActiveSession() {
        _currentAccount.value?.let { account ->
            sessionManager.saveSession(account.id)
        }
    }

    fun createAccount(alias: String, email: String, apiToken: String, workspace: String, mode: String) {
        viewModelScope.launch {
            val encryptedToken = securityManager.encrypt(apiToken)
            val newAccount = AccountEntity(
                alias = alias,
                email = email,
                apiToken = encryptedToken,
                workspace = workspace,
                mode = mode,
                connectionStatus = "Connected"
            )
            val accountId = accountDao.insertAccount(newAccount).toInt()
            
            // Auto populate mock resources per account for authentic testing/playground
            prepopulateAccountData(accountId, alias)

            // Select this account immediately
            val created = accountDao.getAccountById(accountId)
            if (created != null) {
                selectAccount(created)
            }
        }
    }

    fun renameAccount(accountId: Int, newAlias: String) {
        viewModelScope.launch {
            val account = accountDao.getAccountById(accountId)
            if (account != null) {
                val updated = account.copy(alias = newAlias)
                accountDao.updateAccount(updated)
                if (_currentAccount.value?.id == accountId) {
                    _currentAccount.value = updated
                }
            }
        }
    }

    fun removeAccount(account: AccountEntity) {
        viewModelScope.launch {
            sessionManager.clearSession(account.id)
            accountDao.deleteAccount(account)
            if (_currentAccount.value?.id == account.id) {
                val nextAccount = allAccounts.value.firstOrNull { it.id != account.id }
                if (nextAccount != null) {
                    selectAccount(nextAccount)
                } else {
                    _currentAccount.value = null
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun switchAccountMode(mode: String) {
        viewModelScope.launch {
            _currentAccount.value?.let { account ->
                val updated = account.copy(mode = mode)
                accountDao.updateAccount(updated)
                _currentAccount.value = updated
            }
        }
    }

    // Workers Studio
    fun selectWorkerForEditing(worker: WorkerEntity, isProfessional: Boolean) {
        _editorWorker.value = worker
        _editorCode.value = worker.script
        _editorIsProfessional.value = isProfessional
        undoStack.clear()
        redoStack.clear()
    }

    fun setEditorCode(newCode: String) {
        val current = _editorCode.value
        if (current != newCode) {
            undoStack.add(current)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
            _editorCode.value = newCode
        }
    }

    fun editorUndo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(_editorCode.value)
            _editorCode.value = prev
        }
    }

    fun editorRedo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(_editorCode.value)
            _editorCode.value = next
        }
    }

    fun saveAndDeployWorker(name: String, script: String) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            val existing = _editorWorker.value
            _isSyncing.value = true
            _syncError.value = null
            try {
                val token = authManager.getActiveToken()
                if (!token.isNullOrEmpty() && token != "mock_token") {
                    val cfAccountIdentifier = if (account.workspace.isNotEmpty()) account.workspace else "personal"
                    workerRepository.deployWorker(
                        accountId = account.id,
                        cfAccountIdentifier = cfAccountIdentifier,
                        scriptName = name,
                        scriptContent = script,
                        existingWorkerId = existing?.id ?: 0
                    )
                } else {
                    if (existing != null) {
                        val updated = existing.copy(name = name, script = script, updatedAt = System.currentTimeMillis())
                        workerDao.updateWorker(updated)
                        auditLogDao.insertAuditLog(AuditLogEntity(account.id, "Worker deployed (Local): $name", "Success"))
                    } else {
                        val newWorker = WorkerEntity(accountId = account.id, name = name, script = script)
                        workerDao.insertWorker(newWorker)
                        auditLogDao.insertAuditLog(AuditLogEntity(account.id, "Worker created & deployed (Local): $name", "Success"))
                    }
                }
            } catch (e: Exception) {
                _syncError.value = "Cloudflare API deploy failed: ${e.localizedMessage}. Saved locally."
                // Fallback local save anyway
                if (existing != null) {
                    val updated = existing.copy(name = name, script = script, updatedAt = System.currentTimeMillis())
                    workerDao.updateWorker(updated)
                } else {
                    val newWorker = WorkerEntity(accountId = account.id, name = name, script = script)
                    workerDao.insertWorker(newWorker)
                }
            } finally {
                _isSyncing.value = false
                updateAccountCounts(account.id)
                _editorWorker.value = null
                _editorCode.value = ""
            }
        }
    }

    fun duplicateWorker(worker: WorkerEntity) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            val copy = worker.copy(id = 0, name = "${worker.name}-copy", updatedAt = System.currentTimeMillis())
            workerDao.insertWorker(copy)
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "Worker duplicated: ${worker.name}", "Success"))
            updateAccountCounts(account.id)
        }
    }

    fun deleteWorker(worker: WorkerEntity) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            _isSyncing.value = true
            _syncError.value = null
            try {
                val token = authManager.getActiveToken()
                if (!token.isNullOrEmpty() && token != "mock_token") {
                    val cfAccountIdentifier = if (account.workspace.isNotEmpty()) account.workspace else "personal"
                    workerRepository.deleteWorker(account.id, cfAccountIdentifier, worker)
                } else {
                    workerDao.deleteWorker(worker)
                    auditLogDao.insertAuditLog(AuditLogEntity(account.id, "Worker deleted (Local): ${worker.name}", "Success"))
                }
            } catch (e: Exception) {
                _syncError.value = "Cloudflare API delete failed: ${e.localizedMessage}. Removed locally."
                workerDao.deleteWorker(worker)
            } finally {
                _isSyncing.value = false
                updateAccountCounts(account.id)
            }
        }
    }

    // DNS management
    fun addDnsRecord(type: String, name: String, content: String, ttl: Int, proxied: Boolean) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            _isSyncing.value = true
            _syncError.value = null
            try {
                val token = authManager.getActiveToken()
                val zoneName = account.alias + ".com"
                if (!token.isNullOrEmpty() && token != "mock_token") {
                    dnsRepository.createDnsRecord(account.id, zoneName, type, name, content, ttl, proxied)
                } else {
                    val record = DnsRecordEntity(
                        accountId = account.id,
                        zoneName = zoneName,
                        type = type,
                        name = name,
                        content = content,
                        ttl = ttl,
                        proxied = proxied
                    )
                    dnsRecordDao.insertDnsRecord(record)
                    auditLogDao.insertAuditLog(AuditLogEntity(account.id, "DNS Record added (Local): $type $name", "Success"))
                }
            } catch (e: Exception) {
                _syncError.value = "Cloudflare API DNS add failed: ${e.localizedMessage}. Added locally."
                val record = DnsRecordEntity(
                    accountId = account.id,
                    zoneName = account.alias + ".com",
                    type = type,
                    name = name,
                    content = content,
                    ttl = ttl,
                    proxied = proxied
                )
                dnsRecordDao.insertDnsRecord(record)
            } finally {
                _isSyncing.value = false
                updateAccountCounts(account.id)
            }
        }
    }

    fun updateDnsRecord(record: DnsRecordEntity) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            _isSyncing.value = true
            _syncError.value = null
            try {
                val token = authManager.getActiveToken()
                if (!token.isNullOrEmpty() && token != "mock_token") {
                    dnsRepository.updateDnsRecord(
                        accountId = account.id,
                        zoneName = record.zoneName,
                        localRecord = record,
                        newType = record.type,
                        newName = record.name,
                        newContent = record.content,
                        newTtl = record.ttl,
                        newProxied = record.proxied
                    )
                } else {
                    dnsRecordDao.updateDnsRecord(record)
                    auditLogDao.insertAuditLog(AuditLogEntity(account.id, "DNS Record updated (Local): ${record.type} ${record.name}", "Success"))
                }
            } catch (e: Exception) {
                _syncError.value = "Cloudflare API DNS update failed: ${e.localizedMessage}. Updated locally."
                dnsRecordDao.updateDnsRecord(record)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun deleteDnsRecord(record: DnsRecordEntity) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            _isSyncing.value = true
            _syncError.value = null
            try {
                val token = authManager.getActiveToken()
                if (!token.isNullOrEmpty() && token != "mock_token") {
                    dnsRepository.deleteDnsRecord(account.id, record.zoneName, record)
                } else {
                    dnsRecordDao.deleteDnsRecord(record)
                    auditLogDao.insertAuditLog(AuditLogEntity(account.id, "DNS Record deleted (Local): ${record.type} ${record.name}", "Success"))
                }
            } catch (e: Exception) {
                _syncError.value = "Cloudflare API DNS delete failed: ${e.localizedMessage}. Deleted locally."
                dnsRecordDao.deleteDnsRecord(record)
            } finally {
                _isSyncing.value = false
                updateAccountCounts(account.id)
            }
        }
    }

    // R2 Bucket Operations
    fun createR2Bucket(name: String) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            val bucket = R2BucketEntity(accountId = account.id, name = name, size = "0 B", objectCount = 0)
            r2BucketDao.insertR2Bucket(bucket)
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "R2 Bucket created: $name", "Success"))
        }
    }

    fun deleteR2Bucket(bucket: R2BucketEntity) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            r2BucketDao.deleteR2Bucket(bucket)
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "R2 Bucket deleted: ${bucket.name}", "Success"))
        }
    }

    // KV Namespaces Operations
    fun createKvNamespace(name: String) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            val namespace = KvNamespaceEntity(accountId = account.id, name = name, keysJson = "{}")
            kvNamespaceDao.insertKvNamespace(namespace)
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "KV Namespace created: $name", "Success"))
        }
    }

    fun setKvValue(namespace: KvNamespaceEntity, key: String, value: String) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            val json = if (namespace.keysJson.isEmpty()) JSONObject() else JSONObject(namespace.keysJson)
            json.put(key, value)
            val updated = namespace.copy(keysJson = json.toString())
            kvNamespaceDao.updateKvNamespace(updated)
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "KV key set: $key in ${namespace.name}", "Success"))
        }
    }

    fun deleteKvKey(namespace: KvNamespaceEntity, key: String) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            val json = if (namespace.keysJson.isEmpty()) JSONObject() else JSONObject(namespace.keysJson)
            json.remove(key)
            val updated = namespace.copy(keysJson = json.toString())
            kvNamespaceDao.updateKvNamespace(updated)
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "KV key deleted: $key in ${namespace.name}", "Success"))
        }
    }

    fun deleteKvNamespace(namespace: KvNamespaceEntity) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            kvNamespaceDao.deleteKvNamespace(namespace)
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "KV Namespace deleted: ${namespace.name}", "Success"))
        }
    }

    // D1 Databases Operations
    fun createD1Database(name: String) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            val uuid = java.util.UUID.randomUUID().toString()
            val database = D1DatabaseEntity(accountId = account.id, name = name, uuid = uuid, size = "128 KB")
            d1DatabaseDao.insertD1Database(database)
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "D1 DB created: $name", "Success"))
        }
    }

    fun deleteD1Database(database: D1DatabaseEntity) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            d1DatabaseDao.deleteD1Database(database)
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "D1 DB deleted: ${database.name}", "Success"))
        }
    }

    // Tunnel Operations
    fun createTunnel(name: String) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            val tunnel = TunnelEntity(accountId = account.id, name = name, status = "Healthy", connections = 1)
            tunnelDao.insertTunnel(tunnel)
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "Tunnel created: $name", "Success"))
        }
    }

    fun toggleTunnelStatus(tunnel: TunnelEntity) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            val newStatus = if (tunnel.status == "Healthy") "Down" else "Healthy"
            val updated = tunnel.copy(status = newStatus, connections = if (newStatus == "Healthy") 1 else 0)
            tunnelDao.updateTunnel(updated)
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "Tunnel status toggled: ${tunnel.name} ($newStatus)", "Success"))
        }
    }

    fun deleteTunnel(tunnel: TunnelEntity) {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            tunnelDao.deleteTunnel(tunnel)
            auditLogDao.insertAuditLog(AuditLogEntity(account.id, "Tunnel deleted: ${tunnel.name}", "Success"))
        }
    }

    // Purge cache simulation and API call
    fun purgeCache(all: Boolean, url: String = "") {
        viewModelScope.launch {
            val account = _currentAccount.value ?: return@launch
            _isSyncing.value = true
            _syncError.value = null
            try {
                val token = authManager.getActiveToken()
                if (!token.isNullOrEmpty() && token != "mock_token") {
                    val zoneId = account.alias // assume zone alias
                    val files = if (all) null else listOf(url)
                    zoneRepository.purgeZoneCache(account.id, zoneId, all, files)
                } else {
                    val text = if (all) "Complete Cache Purged" else "Purged URL: $url"
                    auditLogDao.insertAuditLog(AuditLogEntity(account.id, text, "Success"))
                }
            } catch (e: Exception) {
                _syncError.value = "Cache purge failed: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Triggers active dynamic synchronization with the Cloudflare API for the selected account.
     * Fetches Zones, Workers, and DNS records, writing them into local cache.
     */
    fun syncCurrentAccountData() {
        val account = _currentAccount.value ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            try {
                // 1. Sync zones to count
                val zones = zoneRepository.fetchZonesAndUpdateCount(account.id)
                
                // 2. Resolve account identifier
                val cfAccountIdentifier = if (account.workspace.isNotEmpty()) account.workspace else "personal"
                
                // Sync workers
                try {
                    workerRepository.syncWorkers(account.id, cfAccountIdentifier)
                } catch (e: Exception) {
                    // Fail silently or log
                }

                // Sync DNS records for each zone
                for (zone in zones) {
                    try {
                        dnsRepository.syncDnsRecords(account.id, zone.name)
                    } catch (e: Exception) {
                        // Fail silently or log
                    }
                }

                // Refresh selected account reference
                val updated = accountDao.getAccountById(account.id)
                if (updated != null) {
                    _currentAccount.value = updated
                }
            } catch (e: Exception) {
                _syncError.value = e.localizedMessage ?: "Synchronization failed"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun updateAccountCounts(accountId: Int) {
        val account = accountDao.getAccountById(accountId) ?: return
        val workerCount = workerDao.getWorkersForAccount(accountId).first().size
        val zoneCount = dnsRecordDao.getDnsRecordsForAccount(accountId).first().map { it.zoneName }.distinct().size
        accountDao.updateAccount(account.copy(workerCount = workerCount, zoneCount = if (zoneCount > 0) zoneCount else 1))
        
        // Refresh the selected account reference
        val updated = accountDao.getAccountById(accountId)
        if (_currentAccount.value?.id == accountId) {
            _currentAccount.value = updated
        }
    }

    private suspend fun prepopulateAccountData(accountId: Int, alias: String) {
        // 1. Initial DNS Records
        dnsRecordDao.insertDnsRecord(DnsRecordEntity(accountId, "$alias.com", "A", "@", "104.21.32.140", 300, true))
        dnsRecordDao.insertDnsRecord(DnsRecordEntity(accountId, "$alias.com", "A", "api", "172.67.140.23", 300, true))
        dnsRecordDao.insertDnsRecord(DnsRecordEntity(accountId, "$alias.com", "CNAME", "www", "$alias.com", 300, true))
        dnsRecordDao.insertDnsRecord(DnsRecordEntity(accountId, "$alias.com", "TXT", "@", "google-site-verification=cf_switcher_mock_verification", 3600, false))

        // 2. Initial Workers
        workerDao.insertWorker(WorkerEntity(accountId, "api-router", """
            // Cloudflare Worker: Simple API Router template
            addEventListener('fetch', event => {
              event.respondWith(handleRequest(event.request))
            })
            
            async fun handleRequest(request) {
              const url = new URL(request.url)
              if (url.pathname === '/api/user') {
                return new Response(JSON.stringify({ id: 1, name: 'Developer User' }), {
                  headers: { 'content-type': 'application/json' }
                })
              }
              return new Response('CF Switcher Worker Active!', { status: 200 })
            }
        """.trimIndent()))

        workerDao.insertWorker(WorkerEntity(accountId, "security-gateway", """
            // Cloudflare Worker: Headers Security Gateway
            export default {
              async fetch(request, env, ctx) {
                const response = await fetch(request);
                const newHeaders = new Headers(response.headers);
                
                // Inject Security Headers
                newHeaders.set("X-Frame-Options", "DENY");
                newHeaders.set("X-Content-Type-Options", "nosniff");
                newHeaders.set("Referrer-Policy", "strict-origin-when-cross-origin");
                
                return new Response(response.body, {
                  status: response.status,
                  statusText: response.statusText,
                  headers: newHeaders
                });
              }
            }
        """.trimIndent()))

        // 3. Initial R2 Bucket
        r2BucketDao.insertR2Bucket(R2BucketEntity(accountId, "media-assets", "14.2 GB", 1250))
        r2BucketDao.insertR2Bucket(R2BucketEntity(accountId, "database-backups", "512.4 MB", 12))

        // 4. Initial KV Namespace
        kvNamespaceDao.insertKvNamespace(KvNamespaceEntity(accountId, "app-sessions", """{"session_10924":"{\"user_id\":102}","session_29384":"{\"user_id\":504}"}"""))

        // 5. Initial D1 Database
        d1DatabaseDao.insertD1Database(D1DatabaseEntity(accountId, "users-db", "d1-3bca9-856c-482a-bc7a-ae7682810018", "1.2 MB"))

        // 6. Initial Tunnels
        tunnelDao.insertTunnel(TunnelEntity(accountId, "office-network-tunnel", "Healthy", 2))

        // 7. Initial Audit logs
        auditLogDao.insertAuditLog(AuditLogEntity(accountId, "Database instantiated", "Success"))
        auditLogDao.insertAuditLog(AuditLogEntity(accountId, "Domain proxy activated", "Success"))

        updateAccountCounts(accountId)
    }
}
