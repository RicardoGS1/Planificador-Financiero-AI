package com.virtualworld.easyexpensecontrol.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.virtualworld.easyexpensecontrol.data.model.Account
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addAccount(account: Account): Long

    @Query("SELECT * FROM Account ORDER BY id ASC")
    abstract fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM Account WHERE `account-hidden` = 0 ORDER BY id ASC")
    abstract fun getVisibleAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM Account WHERE id = :id")
    abstract fun getAccountById(id: Long): Flow<Account?>

    @Query("SELECT COUNT(*) FROM Account")
    abstract suspend fun getAccountCount(): Int

    @Update
    abstract suspend fun updateAccount(account: Account)

    @Delete
    abstract suspend fun deleteAccount(account: Account)
}
