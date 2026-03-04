package com.virtualworld.easyexpensecontrol.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.Transaction

@Database(
    entities = [
        Transaction::class,
        Category::class,
        Budget::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FinancialDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
}
