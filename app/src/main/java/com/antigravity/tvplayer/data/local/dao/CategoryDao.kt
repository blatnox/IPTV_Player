package com.antigravity.tvplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.antigravity.tvplayer.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE accountId = :accountId ORDER BY categoryName ASC")
    fun getCategoriesForAccount(accountId: Long): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE accountId = :accountId")
    suspend fun deleteCategoriesForAccount(accountId: Long)

    @Transaction
    suspend fun replaceCategories(accountId: Long, categories: List<CategoryEntity>) {
        deleteCategoriesForAccount(accountId)
        insertCategories(categories)
    }
}
