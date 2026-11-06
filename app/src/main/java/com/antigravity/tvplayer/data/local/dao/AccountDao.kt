package com.antigravity.tvplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.antigravity.tvplayer.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY lastLoginAt DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 LIMIT 1")
    fun getActiveAccount(): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveAccountSync(): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE serverUrl = :serverUrl AND username = :username LIMIT 1")
    suspend fun findAccount(serverUrl: String, username: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET backup_servers = :backupServers WHERE id = :id")
    suspend fun updateBackupServers(id: Long, backupServers: String)

    @Query("UPDATE accounts SET serverUrl = :serverUrl WHERE id = :id")
    suspend fun updatePrimaryServer(id: Long, serverUrl: String)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("UPDATE accounts SET isActive = 0")
    suspend fun clearActiveAccounts()

    @Query("UPDATE accounts SET isActive = 1, lastLoginAt = :loginTime WHERE id = :id")
    suspend fun markAccountActive(id: Long, loginTime: Long = System.currentTimeMillis())

    @Transaction
    suspend fun switchActiveAccount(id: Long) {
        clearActiveAccounts()
        markAccountActive(id)
    }
}
