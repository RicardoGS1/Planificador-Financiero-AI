package com.virtualworld.easyexpensecontrol.domain.usecase.category

import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class GetCategoriesByTypeUseCase(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(type: TransactionType): Flow<List<Category>> =
        categoryRepository.getCategoriesByType(type)
}
