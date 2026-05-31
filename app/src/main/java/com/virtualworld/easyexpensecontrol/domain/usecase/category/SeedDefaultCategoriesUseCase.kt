package com.virtualworld.easyexpensecontrol.domain.usecase.category

import android.content.Context
import com.virtualworld.easyexpensecontrol.data.local.DefaultCategories
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.domain.repository.CategoryRepository

class SeedDefaultCategoriesUseCase(
    private val categoryRepository: CategoryRepository,
    private val context: Context
) {

    suspend operator fun invoke() {
        if (categoryRepository.getCategoryCountByType(TransactionType.Gasto) == 0) {
            for (category in DefaultCategories.buildExpenseCategories(context)) {
                categoryRepository.addCategory(category)
            }
        }
        if (categoryRepository.getCategoryCountByType(TransactionType.Ingreso) == 0) {
            for (category in DefaultCategories.buildIncomeCategories(context)) {
                categoryRepository.addCategory(category)
            }
        }
    }
}
