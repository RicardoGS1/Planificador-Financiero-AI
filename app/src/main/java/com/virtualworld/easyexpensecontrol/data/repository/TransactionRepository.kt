package com.virtualworld.easyexpensecontrol.data.repository

import com.virtualworld.easyexpensecontrol.data.local.TransactionDao
import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.domain.repository.TransactionRepository as TransactionRepositoryDomain
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) : TransactionRepositoryDomain {

    override suspend fun addTransaction(transaction: Transaction) {
        transactionDao.addTransaction(transaction)
    }

    override fun getTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    override fun getTransactionById(id: Long): Flow<Transaction> {
        return transactionDao.getTransactionById(id)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    override suspend fun getTransactionCountForCategory(categoryId: Long): Int {
        return transactionDao.getTransactionCountForCategory(categoryId)
    }

    override fun getTransactionsByCategoryAndDate(categoryId: Long, year: Int, month: String): Flow<List<Transaction>> {
        val yearString = year.toString()
        return transactionDao.getTransactionsByCategoryAndDate(categoryId, yearString, month)
    }
}
