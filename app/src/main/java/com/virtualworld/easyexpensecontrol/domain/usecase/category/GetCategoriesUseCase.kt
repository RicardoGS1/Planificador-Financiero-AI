package com.virtualworld.easyexpensecontrol.domain.usecase.category

import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class GetCategoriesUseCase(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> = categoryRepository.getCategories()
}
