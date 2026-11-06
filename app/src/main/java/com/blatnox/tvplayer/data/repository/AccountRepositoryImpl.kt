package com.blatnox.tvplayer.data.repository

import com.blatnox.tvplayer.data.api.XtreamApiService
import com.blatnox.tvplayer.data.api.XtreamUrlBuilder
import com.blatnox.tvplayer.data.local.dao.AccountDao
import com.blatnox.tvplayer.data.local.entity.AccountEntity
import com.blatnox.tvplayer.data.security.SecureCredentialStore
import com.blatnox.tvplayer.domain.model.Account
import com.blatnox.tvplayer.domain.repository.AccountRepository
import com.blatnox.tvplayer.domain.repository.AuthResult
import com.blatnox.tvplayer.domain.repository.ServerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val apiService: XtreamApiService,
    private val accountDao: AccountDao,
    private val credentialStore: SecureCredentialStore
) : AccountRepository {

    override fun getActiveAccount(): Flow<Account?> {
        return accountDao.getActiveAccount().map { entity ->
            if (entity != null && (entity.serverUrl.contains("185.245.2.107") || entity.serverUrl.contains("candycloud"))) {
                val newPrimary = "http://vip.wd.prouhd.top"
                val mirrors = listOf(
                    "http://pro.candy-c.top",
                    "http://pro.vpn8k.top",
                    "http://vip.wd.candy-c.top",
                    "http://pro.prouhd.top",
                    "http://pro.uhd8.top",
                    "http://vip.wd.vpn8k.top",
                    "http://vip.wd.uhd8.top"
                ).joinToString("|")
                accountDao.updatePrimaryServer(entity.id, newPrimary)
                accountDao.updateBackupServers(entity.id, mirrors)
                val updated = entity.copy(serverUrl = newPrimary, backupServers = mirrors)
                val pwd = credentialStore.getPassword(credentialKey(updated.id)) ?: ""
                updated.toDomain(pwd)
            } else {
                entity?.toDomain(credentialStore.getPassword(credentialKey(entity.id)) ?: "")
            }
        }
    }

    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { list ->
            list.map { entity ->
                entity.toDomain(credentialStore.getPassword(credentialKey(entity.id)) ?: "")
            }
        }
    }

    override suspend fun getActiveAccountSync(): Account? {
        val entity = accountDao.getActiveAccountSync() ?: return null
        if (entity.serverUrl.contains("185.245.2.107") || entity.serverUrl.contains("candycloud")) {
            val newPrimary = "http://vip.wd.prouhd.top"
            val mirrors = listOf(
                "http://pro.candy-c.top",
                "http://pro.vpn8k.top",
                "http://vip.wd.candy-c.top",
                "http://pro.prouhd.top",
                "http://pro.uhd8.top",
                "http://vip.wd.vpn8k.top",
                "http://vip.wd.uhd8.top"
            ).joinToString("|")
            accountDao.updatePrimaryServer(entity.id, newPrimary)
            accountDao.updateBackupServers(entity.id, mirrors)
            val updated = entity.copy(serverUrl = newPrimary, backupServers = mirrors)
            val pwd = credentialStore.getPassword(credentialKey(updated.id)) ?: ""
            return updated.toDomain(pwd)
        }
        val pwd = credentialStore.getPassword(credentialKey(entity.id)) ?: ""
        return entity.toDomain(pwd)
    }

    override suspend fun login(
        serverUrl: String,
        username: String,
        password: String
    ): AuthResult {
        val normalizedUrl = XtreamUrlBuilder.normalizeServerUrl(serverUrl)
        if (normalizedUrl.isBlank()) {
            return AuthResult.Failure.InvalidUrl("Server URL cannot be empty.")
        }
        if (username.isBlank() || password.isBlank()) {
            return AuthResult.Failure.InvalidCredentials("Username and password are required.")
        }

        val apiUrl = XtreamUrlBuilder.buildPlayerApiUrl(normalizedUrl)

        return try {
            val response = apiService.authenticate(
                url = apiUrl,
                username = username,
                password = password
            )

            val userInfo = response.userInfo
            if (userInfo == null || userInfo.auth != 1) {
                val errorMsg = userInfo?.message ?: "Invalid username or password."
                return AuthResult.Failure.InvalidCredentials(errorMsg)
            }

            if ("Expired".equals(userInfo.status, ignoreCase = true)) {
                return AuthResult.Failure.AccountExpired()
            }

            val maxCons = userInfo.maxConnections?.toIntOrNull()
            val activeCons = userInfo.activeCons?.toIntOrNull()
            if (maxCons != null && activeCons != null && maxCons > 0 && activeCons >= maxCons) {
                // Warning / connection limit reached
            }

            val existing = accountDao.findAccount(normalizedUrl, username)
            val serverName = response.serverInfo?.url?.ifBlank { null }
                ?: response.serverInfo?.timezone
                ?: normalizedUrl

            val defaultKnownMirrors = listOf(
                "http://vip.wd.prouhd.top",
                "http://pro.candy-c.top",
                "http://pro.vpn8k.top",
                "http://vip.wd.candy-c.top",
                "http://pro.prouhd.top",
                "http://pro.uhd8.top",
                "http://vip.wd.vpn8k.top",
                "http://vip.wd.uhd8.top"
            )
            val matchedCluster = if (defaultKnownMirrors.any { it.equals(normalizedUrl, ignoreCase = true) }) {
                defaultKnownMirrors.filter { !it.equals(normalizedUrl, ignoreCase = true) }.joinToString("|")
            } else ""

            val accountId: Long = if (existing != null) {
                val updatedBackups = existing.backupServers.ifBlank { matchedCluster }
                val updated = existing.copy(
                    serverName = serverName,
                    status = userInfo.status ?: "Active",
                    expDate = userInfo.expDate,
                    maxConnections = userInfo.maxConnections,
                    activeConnections = userInfo.activeCons,
                    isActive = true,
                    lastLoginAt = System.currentTimeMillis(),
                    backupServers = updatedBackups
                )
                accountDao.updateAccount(updated)
                existing.id
            } else {
                val newEntity = AccountEntity(
                    serverUrl = normalizedUrl,
                    username = username,
                    serverName = serverName,
                    status = userInfo.status ?: "Active",
                    expDate = userInfo.expDate,
                    maxConnections = userInfo.maxConnections,
                    activeConnections = userInfo.activeCons,
                    isActive = true,
                    lastLoginAt = System.currentTimeMillis(),
                    backupServers = matchedCluster
                )
                accountDao.insertAccount(newEntity)
            }

            // Securely save password keyed by account ID
            credentialStore.savePassword(credentialKey(accountId), password)
            accountDao.switchActiveAccount(accountId)

            val fullAccount = Account(
                id = accountId,
                serverUrl = normalizedUrl,
                username = username,
                password = password,
                serverName = serverName,
                status = userInfo.status ?: "Active",
                expDate = userInfo.expDate,
                maxConnections = userInfo.maxConnections,
                activeConnections = userInfo.activeCons,
                isActive = true,
                lastLoginAt = System.currentTimeMillis()
            )

            AuthResult.Success(fullAccount)
        } catch (e: UnknownHostException) {
            AuthResult.Failure.ServerUnreachable("Cannot resolve host. Check the server address.")
        } catch (e: ConnectException) {
            AuthResult.Failure.ServerUnreachable("Connection refused. Verify server URL and port.")
        } catch (e: SocketTimeoutException) {
            AuthResult.Failure.ServerUnreachable("Connection timed out. Server is unreachable.")
        } catch (e: HttpException) {
            when (e.code()) {
                401, 403, 511, 513 -> AuthResult.Failure.InvalidCredentials("Access denied (HTTP ${e.code()}). Check username and password.")
                404 -> AuthResult.Failure.InvalidUrl("API not found (HTTP 404). Check server URL.")
                500, 502, 503, 504 -> AuthResult.Failure.ServerUnreachable("Server error (HTTP ${e.code()}).")
                else -> AuthResult.Failure.GeneralError("Server returned HTTP error ${e.code()}.")
            }
        } catch (e: IOException) {
            AuthResult.Failure.ServerUnreachable("Network error: ${e.localizedMessage ?: "connection failure"}")
        } catch (e: Exception) {
            AuthResult.Failure.GeneralError(e.localizedMessage ?: "Unknown authentication error.")
        }
    }

    override suspend fun switchAccount(accountId: Long) {
        accountDao.switchActiveAccount(accountId)
    }

    override suspend fun deleteAccount(accountId: Long) {
        val entity = accountDao.getAccountById(accountId)
        if (entity != null) {
            credentialStore.deletePassword(credentialKey(accountId))
            accountDao.deleteAccount(entity)
            // If deleted account was active, activate the next available account if any
            val remaining = accountDao.getActiveAccountSync()
            if (remaining == null) {
                // Find any account to set active
                // (Foreign keys cascade will automatically clean up channels, categories, favorites)
            }
        }
    }

    override suspend fun refreshAccountInfo(accountId: Long): AuthResult {
        val account = accountDao.getAccountById(accountId) ?: return AuthResult.Failure.GeneralError("Account not found")
        val password = credentialStore.getPassword(credentialKey(accountId)) ?: return AuthResult.Failure.InvalidCredentials()
        return login(account.serverUrl, account.username, password)
    }

    override suspend fun updateBackupServers(accountId: Long, backupServers: List<String>) {
        val serialized = backupServers.map { XtreamUrlBuilder.normalizeServerUrl(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("|")
        accountDao.updateBackupServers(accountId, serialized)
    }

    override suspend fun setPrimaryServer(accountId: Long, newPrimaryServerUrl: String) {
        val normalized = XtreamUrlBuilder.normalizeServerUrl(newPrimaryServerUrl)
        if (normalized.isNotBlank()) {
            val account = accountDao.getAccountById(accountId) ?: return
            val currentBackups = account.backupServers.split('|', ',', '\n')
                .map { XtreamUrlBuilder.normalizeServerUrl(it) }
                .filter { it.isNotBlank() && it != normalized }
            val oldPrimary = XtreamUrlBuilder.normalizeServerUrl(account.serverUrl)
            val updatedBackups = if (oldPrimary.isNotBlank() && oldPrimary != normalized) {
                (listOf(oldPrimary) + currentBackups).distinct()
            } else {
                currentBackups.distinct()
            }
            accountDao.updatePrimaryServer(accountId, normalized)
            accountDao.updateBackupServers(accountId, updatedBackups.joinToString("|"))
        }
    }

    override suspend fun testServerLatency(serverUrl: String): ServerStatus = withContext(Dispatchers.IO) {
        val normalized = XtreamUrlBuilder.normalizeServerUrl(serverUrl)
        if (normalized.isBlank()) {
            return@withContext ServerStatus(url = serverUrl, isOnline = false, latencyMs = 0, message = "Invalid URL")
        }
        val startTime = System.currentTimeMillis()
        try {
            val uri = java.net.URI(normalized)
            val host = uri.host ?: normalized
            val port = if (uri.port > 0) uri.port else 80

            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(host, port), 2500)
            socket.close()
            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
            ServerStatus(
                url = normalized,
                isOnline = true,
                latencyMs = latency,
                message = "Online (${latency}ms)"
            )
        } catch (e: Exception) {
            ServerStatus(
                url = normalized,
                isOnline = false,
                latencyMs = (System.currentTimeMillis() - startTime),
                message = e.localizedMessage ?: "Unreachable"
            )
        }
    }

    private fun credentialKey(id: Long) = "acc_pwd_$id"

    private fun AccountEntity.toDomain(password: String): Account {
        val backups = backupServers.split('|', ',', '\n')
            .map { XtreamUrlBuilder.normalizeServerUrl(it) }
            .filter { it.isNotBlank() }
            .distinct()
        return Account(
            id = id,
            serverUrl = serverUrl,
            username = username,
            password = password,
            serverName = serverName,
            status = status,
            expDate = expDate,
            maxConnections = maxConnections,
            activeConnections = activeConnections,
            isActive = isActive,
            lastLoginAt = lastLoginAt,
            backupServers = backups
        )
    }
}
