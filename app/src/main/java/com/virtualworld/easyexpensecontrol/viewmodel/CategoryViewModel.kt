package com.virtualworld.easyexpensecontrol.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoriesByTypeUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoriesUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoryByIdUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoryByNameUseCase
import kotlinx.coroutines.flow.Flow

class CategoryViewModel(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getCategoryByIdUseCase: GetCategoryByIdUseCase,
    private val getCategoryByNameUseCase: GetCategoryByNameUseCase,
    private val getCategoriesByTypeUseCase: GetCategoriesByTypeUseCase
) : ViewModel() {
    var categoryNameState by mutableStateOf("")
    var categoryTypeState by mutableStateOf(TransactionType.Ingreso)

    val getAllCategories: Flow<List<Category>> = getCategoriesUseCase()

    fun getCategoryById(id: Long): Flow<Category> = getCategoryByIdUseCase(id)

    fun getCategoryByName(name: String): Flow<Category> = getCategoryByNameUseCase(name)

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        getCategoriesByTypeUseCase(type)
}
