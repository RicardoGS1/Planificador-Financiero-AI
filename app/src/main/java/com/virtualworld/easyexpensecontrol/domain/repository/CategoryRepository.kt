package com.virtualworld.easyexpensecontrol.domain.repository

import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    suspend fun addCategory(category: Category): Long

    suspend fun updateCategory(category: Category)

    fun getCategories(): Flow<List<Category>>

    fun getCategoryById(id: Long): Flow<Category>

    suspend fun deleteCategory(category: Category)

    fun getCategoryByName(name: String): Flow<Category>

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>

    suspend fun getCategoryCount(): Int

    suspend fun getCategoryCountByType(type: TransactionType): Int
}
