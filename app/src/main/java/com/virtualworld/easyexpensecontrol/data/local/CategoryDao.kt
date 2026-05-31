package com.virtualworld.easyexpensecontrol.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addCategory(categoryEntity: Category): Long

    @Query("Select * from `Category`")
    abstract fun getAllCategories(): Flow<List<Category>>

    @Update
    abstract suspend fun updateCategory(categoryEntity: Category)

    @Delete
    abstract suspend fun deleteCategory(category: Category)

    @Query("Select * from `Category` where id=:id")
    abstract fun getCategoryById(id: Long): Flow<Category>

    @Query("SELECT * FROM `Category` WHERE name=:name LIMIT 1")
    abstract fun getCategoryByName(name: String): Flow<Category>

    @Query("SELECT * FROM `Category` WHERE type=:type")
    abstract fun getCategoriesByType(type: TransactionType): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM `Category`")
    abstract suspend fun getCategoryCount(): Int

    @Query("SELECT COUNT(*) FROM `Category` WHERE type = :type")
    abstract suspend fun getCategoryCountByType(type: TransactionType): Int
}
