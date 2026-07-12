package com.virtualworld.easyexpensecontrol.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.virtualworld.easyexpensecontrol.data.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecurringTransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(recurringTransaction: RecurringTransaction): Long

    @Update
    abstract suspend fun update(recurringTransaction: RecurringTransaction)

    @Query("SELECT * FROM `RecurringTransaction` WHERE id = :id")
    abstract fun getById(id: Long): Flow<RecurringTransaction?>

    @Query("SELECT * FROM `RecurringTransaction` WHERE `recurring-is-active` = 1")
    abstract suspend fun getAllActive(): List<RecurringTransaction>
}
