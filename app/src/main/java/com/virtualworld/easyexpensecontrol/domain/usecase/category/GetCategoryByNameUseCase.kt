package com.virtualworld.easyexpensecontrol.domain.usecase.category

import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class GetCategoryByNameUseCase(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(name: String): Flow<Category> = categoryRepository.getCategoryByName(name)
}
