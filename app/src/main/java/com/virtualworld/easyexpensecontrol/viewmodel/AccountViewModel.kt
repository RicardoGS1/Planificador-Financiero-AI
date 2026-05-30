package com.virtualworld.easyexpensecontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualworld.easyexpensecontrol.data.model.Account
import com.virtualworld.easyexpensecontrol.domain.repository.AccountRepository
import com.virtualworld.easyexpensecontrol.domain.usecase.account.AddAccountUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.account.GetAccountsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.account.GetVisibleAccountsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.account.UpdateAccountUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getVisibleAccountsUseCase: GetVisibleAccountsUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val accountRepository: AccountRepository
) : ViewModel() {

    val accounts: Flow<List<Account>> = getAccountsUseCase()
    val visibleAccounts: Flow<List<Account>> = getVisibleAccountsUseCase()

    fun getAccountById(id: Long): Flow<Account?> = accountRepository.getAccountById(id)

    fun addAccount(
        name: String,
        onError: suspend (String) -> Unit,
        onSuccess: suspend (Long) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = addAccountUseCase(name)
            result.fold(
                onSuccess = { id -> withContext(Dispatchers.Main) { onSuccess(id) } },
                onFailure = { e ->
                    withContext(Dispatchers.Main) {
                        onError(e.message.orEmpty())
                    }
                }
            )
        }
    }

    fun updateAccount(
        account: Account,
        name: String,
        isHidden: Boolean,
        onError: suspend (String) -> Unit,
        onSuccess: suspend () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = updateAccountUseCase(account, name, isHidden)
            result.fold(
                onSuccess = { withContext(Dispatchers.Main) { onSuccess() } },
                onFailure = { e ->
                    withContext(Dispatchers.Main) {
                        onError(e.message.orEmpty())
                    }
                }
            )
        }
    }
}
