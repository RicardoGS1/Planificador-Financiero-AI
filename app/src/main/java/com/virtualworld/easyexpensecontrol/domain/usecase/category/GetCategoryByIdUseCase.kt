package com.virtualworld.easyexpensecontrol.domain.usecase.category

import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class GetCategoryByIdUseCase(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(id: Long): Flow<Category> = categoryRepository.getCategoryById(id)
}
