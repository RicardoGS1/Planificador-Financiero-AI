package com.virtualworld.easyexpensecontrol.data.repository

import com.virtualworld.easyexpensecontrol.data.local.CategoryDao
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.domain.repository.CategoryRepository as CategoryRepositoryDomain
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) : CategoryRepositoryDomain {

    override suspend fun addCategory(category: Category): Long {
        return categoryDao.addCategory(category)
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    override fun getCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    override fun getCategoryById(id: Long): Flow<Category> {
        return categoryDao.getCategoryById(id)
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    override fun getCategoryByName(name: String): Flow<Category> {
        return categoryDao.getCategoryByName(name)
    }

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> = categoryDao.getCategoriesByType(type)

    override suspend fun getCategoryCount(): Int = categoryDao.getCategoryCount()

    override suspend fun getCategoryCountByType(type: TransactionType): Int =
        categoryDao.getCategoryCountByType(type)
}
