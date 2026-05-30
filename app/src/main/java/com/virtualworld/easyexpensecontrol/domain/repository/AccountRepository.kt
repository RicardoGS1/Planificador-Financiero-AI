package com.virtualworld.easyexpensecontrol.domain.repository

import com.virtualworld.easyexpensecontrol.data.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {

    suspend fun addAccount(account: Account): Long

    suspend fun updateAccount(account: Account)

    fun getAccounts(): Flow<List<Account>>

    fun getVisibleAccounts(): Flow<List<Account>>

    fun getAccountById(id: Long): Flow<Account?>

    suspend fun getAccountCount(): Int

    suspend fun deleteAccount(account: Account)
}
