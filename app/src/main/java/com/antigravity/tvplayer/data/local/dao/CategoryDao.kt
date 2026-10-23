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

    @Query("SELECT * FROM categories WHERE accountId = :accountId AND isHidden = 0 ORDER BY categoryName ASC")
    fun getVisibleCategoriesForAccount(accountId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE accountId = :accountId ORDER BY categoryName ASC")
    fun getAllCategoriesForAccount(accountId: Long): Flow<List<CategoryEntity>>

    @Query("UPDATE categories SET isHidden = :isHidden WHERE accountId = :accountId AND categoryId = :categoryId")
    suspend fun setCategoryHidden(accountId: Long, categoryId: String, isHidden: Boolean)

    @Query("UPDATE categories SET isHidden = :isHidden WHERE accountId = :accountId")
    suspend fun setAllCategoriesHidden(accountId: Long, isHidden: Boolean)

    @Query("SELECT categoryId FROM categories WHERE accountId = :accountId AND isHidden = 1")
    suspend fun getHiddenCategoryIds(accountId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE accountId = :accountId")
    suspend fun deleteCategoriesForAccount(accountId: Long)

    @Transaction
    suspend fun replaceCategories(accountId: Long, categories: List<CategoryEntity>) {
        val hiddenIds = getHiddenCategoryIds(accountId).toSet()
        deleteCategoriesForAccount(accountId)
        val toInsert = categories.map {
            if (it.categoryId in hiddenIds) it.copy(isHidden = true) else it
        }
        insertCategories(toInsert)
    }
}
