package com.virtualworld.easyexpensecontrol.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingTutorialDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onboarding_tutorial"
)

class OnboardingTutorialRepository(context: Context) {

    private val dataStore = context.applicationContext.onboardingTutorialDataStore

    val defaultAccountTipSeen: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DEFAULT_ACCOUNT_TIP_SEEN_KEY] == true
    }

    val addTransactionTipSeen: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ADD_TRANSACTION_TIP_SEEN_KEY] == true
    }

    val currencyTipSeen: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[CURRENCY_TIP_SEEN_KEY] == true
    }

    suspend fun markDefaultAccountTipSeen() {
        dataStore.edit { prefs ->
            prefs[DEFAULT_ACCOUNT_TIP_SEEN_KEY] = true
        }
    }

    suspend fun markAddTransactionTipSeen() {
        dataStore.edit { prefs ->
            prefs[ADD_TRANSACTION_TIP_SEEN_KEY] = true
        }
    }

    suspend fun markCurrencyTipSeen() {
        dataStore.edit { prefs ->
            prefs[CURRENCY_TIP_SEEN_KEY] = true
        }
    }

    companion object {
        const val DEFAULT_ACCOUNT_ID = 1L

        private val DEFAULT_ACCOUNT_TIP_SEEN_KEY = booleanPreferencesKey("default_account_tip_seen")
        private val ADD_TRANSACTION_TIP_SEEN_KEY = booleanPreferencesKey("add_transaction_tip_seen")
        private val CURRENCY_TIP_SEEN_KEY = booleanPreferencesKey("currency_tip_seen")
    }
}
