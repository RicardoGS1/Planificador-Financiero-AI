package com.virtualworld.easyexpensecontrol.domain.usecase.account

import com.virtualworld.easyexpensecontrol.data.model.Account
import com.virtualworld.easyexpensecontrol.domain.repository.AccountRepository

class UpdateAccountUseCase(private val accountRepository: AccountRepository) {

    suspend operator fun invoke(account: Account, name: String, isHidden: Boolean): Result<Unit> {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Account name cannot be empty"))
        }
        return try {
            accountRepository.updateAccount(account.copy(name = trimmed, isHidden = isHidden))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
