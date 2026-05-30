package com.virtualworld.easyexpensecontrol.domain.usecase.account

import com.virtualworld.easyexpensecontrol.data.model.Account
import com.virtualworld.easyexpensecontrol.domain.repository.AccountRepository

class AddAccountUseCase(private val accountRepository: AccountRepository) {

    suspend operator fun invoke(name: String): Result<Long> {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Account name cannot be empty"))
        }
        return try {
            val id = accountRepository.addAccount(Account(name = trimmed))
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
