package com.virtualworld.easyexpensecontrol.domain.repository

import com.virtualworld.easyexpensecontrol.data.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

interface RecurringTransactionRepository {

    suspend fun insert(recurringTransaction: RecurringTransaction): Long

    suspend fun update(recurringTransaction: RecurringTransaction)

    fun getById(id: Long): Flow<RecurringTransaction?>

    suspend fun getAllActive(): List<RecurringTransaction>
}
