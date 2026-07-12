package com.virtualworld.easyexpensecontrol.data.repository

import com.virtualworld.easyexpensecontrol.data.local.RecurringTransactionDao
import com.virtualworld.easyexpensecontrol.data.model.RecurringTransaction
import com.virtualworld.easyexpensecontrol.domain.repository.RecurringTransactionRepository as RecurringTransactionRepositoryDomain
import kotlinx.coroutines.flow.Flow

class RecurringTransactionRepository(
    private val recurringTransactionDao: RecurringTransactionDao
) : RecurringTransactionRepositoryDomain {

    override suspend fun insert(recurringTransaction: RecurringTransaction): Long =
        recurringTransactionDao.insert(recurringTransaction)

    override suspend fun update(recurringTransaction: RecurringTransaction) {
        recurringTransactionDao.update(recurringTransaction)
    }

    override fun getById(id: Long): Flow<RecurringTransaction?> =
        recurringTransactionDao.getById(id)

    override suspend fun getAllActive(): List<RecurringTransaction> =
        recurringTransactionDao.getAllActive()
}
