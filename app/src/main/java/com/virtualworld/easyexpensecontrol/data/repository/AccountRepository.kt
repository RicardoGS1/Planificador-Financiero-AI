package com.virtualworld.easyexpensecontrol.data.repository

import com.virtualworld.easyexpensecontrol.data.local.AccountDao
import com.virtualworld.easyexpensecontrol.data.model.Account
import com.virtualworld.easyexpensecontrol.domain.repository.AccountRepository as AccountRepositoryDomain
import kotlinx.coroutines.flow.Flow

class AccountRepository(private val accountDao: AccountDao) : AccountRepositoryDomain {

    override suspend fun addAccount(account: Account): Long = accountDao.addAccount(account)

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account)
    }

    override fun getAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()

    override fun getVisibleAccounts(): Flow<List<Account>> = accountDao.getVisibleAccounts()

    override fun getAccountById(id: Long): Flow<Account?> = accountDao.getAccountById(id)

    override suspend fun getAccountCount(): Int = accountDao.getAccountCount()

    override suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account)
    }
}
