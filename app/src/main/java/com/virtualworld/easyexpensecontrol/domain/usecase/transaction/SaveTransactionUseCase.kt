package com.virtualworld.easyexpensecontrol.domain.usecase.transaction

import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.domain.repository.BudgetRepository
import com.virtualworld.easyexpensecontrol.domain.repository.CategoryRepository
import com.virtualworld.easyexpensecontrol.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
class SaveTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository
) {

    suspend operator fun invoke(
        id: Long,
        type: TransactionType,
        amount: Double,
        description: String,
        categoryName: String,
        category: Category?,
        date: Long,
        iconName: String?,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        try {
            val categoryId = resolveCategoryId(categoryName.trim(), category, type, iconName)
            val transaction = Transaction(
                id = id,
                type = type,
                amount = amount,
                description = description,
                category = categoryId,
                date = date
            )

            if (id != 0L) {
                val oldTransaction = transactionRepository.getTransactionById(id).first()
                updateTransactionAndBudget(oldTransaction, transaction)
            } else {
                addTransactionAndUpdateBudget(transaction)
            }
            onSuccess()
        } catch (e: Exception) {
            onError(e.message.orEmpty())
        }
    }

    private suspend fun resolveCategoryId(
        categoryName: String,
        existingCategory: Category?,
        type: TransactionType,
        iconName: String?
    ): Long =
        when {
            existingCategory == null -> {
                categoryRepository.addCategory(Category(name = categoryName, type = type, iconName = iconName))
            }
            existingCategory.type != type -> {
                categoryRepository.addCategory(Category(name = categoryName, type = type, iconName = iconName))
            }
            else -> {
                if (iconName != existingCategory.iconName) {
                    categoryRepository.updateCategory(existingCategory.copy(iconName = iconName))
                }
                existingCategory.id
            }
        }

    private suspend fun updateTransactionAndBudget(oldTransaction: Transaction, newTransaction: Transaction) {
        if (oldTransaction.type == TransactionType.Gasto) {
            findBudget(oldTransaction.category, oldTransaction.date)?.let { budget ->
                budgetRepository.updateBudget(budget.copy(currentExpenditure = budget.currentExpenditure - oldTransaction.amount))
            }
        }

        if (newTransaction.type == TransactionType.Gasto) {
            val budget = if (oldTransaction.category != newTransaction.category || oldTransaction.date != newTransaction.date) {
                findBudget(newTransaction.category, newTransaction.date)
            } else {
                findBudget(oldTransaction.category, oldTransaction.date)
            }
            budget?.let {
                budgetRepository.updateBudget(it.copy(currentExpenditure = it.currentExpenditure + newTransaction.amount))
            }
        }

        transactionRepository.updateTransaction(newTransaction)
    }

    private suspend fun addTransactionAndUpdateBudget(transaction: Transaction) {
        transactionRepository.addTransaction(transaction)
        if (transaction.type == TransactionType.Gasto) {
            findBudget(transaction.category, transaction.date)?.let { budget ->
                budgetRepository.updateBudget(budget.copy(currentExpenditure = budget.currentExpenditure + transaction.amount))
            }
        }
    }

    private suspend fun findBudget(categoryId: Long, date: Long) =
        budgetRepository.getBudgetForCategoryMonthAndYear(
            categoryId = categoryId,
            year = Instant.fromEpochMilliseconds(date).toLocalDateTime(TimeZone.currentSystemDefault()).date.year,
            month = Instant.fromEpochMilliseconds(date).toLocalDateTime(TimeZone.currentSystemDefault()).date.monthNumber.toString().padStart(2, '0')
        ).first()
}
