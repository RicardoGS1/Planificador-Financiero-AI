package com.virtualworld.easyexpensecontrol.core.util

import com.virtualworld.easyexpensecontrol.data.model.Account
import java.text.Normalizer

/**
 * Resuelve el nombre de cuenta sugerido por la IA contra las cuentas visibles del usuario.
 */
object AccountNameMatcher {

    fun resolve(suggestedName: String, accounts: List<Account>): Account? {
        if (accounts.isEmpty()) return null

        val normalizedSuggested = normalize(suggestedName)
        if (normalizedSuggested.isBlank()) return null

        accounts.firstOrNull { normalize(it.name) == normalizedSuggested }?.let { return it }

        return accounts.firstOrNull { account ->
            val normalizedAccount = normalize(account.name)
            normalizedAccount.contains(normalizedSuggested) ||
                normalizedSuggested.contains(normalizedAccount)
        }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
}
