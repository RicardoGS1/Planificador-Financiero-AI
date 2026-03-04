package com.virtualworld.easyexpensecontrol.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.virtualworld.easyexpensecontrol.data.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BudgetDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addBudget(budgetEntity: Budget)

    @Query("Select * from `Budget`")
    abstract fun getAllBudgets(): Flow<List<Budget>>

    @Update
    abstract suspend fun updateBudget(budgetEntity: Budget)

    @Delete
    abstract suspend fun deleteBudget(budgetEntity: Budget)

    @Query("Select * from `Budget` where id=:id")
    abstract fun getBudgetById(id: Long): Flow<Budget>

    @Query("SELECT * FROM `Budget` WHERE `budget-category` = :categoryId AND `budget-month` = :month AND `budget-year` = :year")
    abstract fun getBudgetForCategoryMonthAndYear(categoryId: Long, month: String, year: Int): Flow<Budget?>
}
