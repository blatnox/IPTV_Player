package com.antigravity.tvplayer.domain.repository

import com.antigravity.tvplayer.domain.model.Account
import kotlinx.coroutines.flow.Flow

sealed class AuthResult {
    data class Success(val account: Account) : AuthResult()
    sealed class Failure(val message: String) : AuthResult() {
        data class InvalidCredentials(val msg: String = "Invalid username or password.") : Failure(msg)
        data class AccountExpired(val msg: String = "Your account subscription has expired.") : Failure(msg)
        data class ServerUnreachable(val msg: String = "Cannot connect to server. Check URL and connection.") : Failure(msg)
        data class InvalidUrl(val msg: String = "Invalid server URL. Please check formatting.") : Failure(msg)
        data class MaxConnectionsReached(val msg: String = "Maximum concurrent connections reached.") : Failure(msg)
        data class GeneralError(val msg: String) : Failure(msg)
    }
}

data class ServerStatus(
    val url: String,
    val isOnline: Boolean,
    val latencyMs: Long,
    val httpCode: Int = 0,
    val message: String = ""
)

interface AccountRepository {
    fun getActiveAccount(): Flow<Account?>
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun getActiveAccountSync(): Account?
    suspend fun login(serverUrl: String, username: String, password: String): AuthResult
    suspend fun switchAccount(accountId: Long)
    suspend fun deleteAccount(accountId: Long)
    suspend fun refreshAccountInfo(accountId: Long): AuthResult
    suspend fun updateBackupServers(accountId: Long, backupServers: List<String>)
    suspend fun setPrimaryServer(accountId: Long, newPrimaryServerUrl: String)
    suspend fun testServerLatency(serverUrl: String): ServerStatus
}
