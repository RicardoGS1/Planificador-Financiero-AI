package com.virtualworld.easyexpensecontrol.domain.usecase.account

import com.virtualworld.easyexpensecontrol.data.model.Account
import com.virtualworld.easyexpensecontrol.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class GetVisibleAccountsUseCase(private val accountRepository: AccountRepository) {

    operator fun invoke(): Flow<List<Account>> = accountRepository.getVisibleAccounts()
}
