package com.virtualworld.easyexpensecontrol.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.budgetListVisibilityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "budget_list_visibility"
)

class BudgetListVisibilityRepository(context: Context) {

    private val dataStore = context.applicationContext.budgetListVisibilityDataStore

    val hiddenCategoryIds: Flow<Set<Long>> = dataStore.data.map { prefs ->
        prefs[HIDDEN_IDS_KEY].orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
    }

    suspend fun setHiddenCategoryIds(ids: Set<Long>) {
        dataStore.edit { prefs ->
            prefs[HIDDEN_IDS_KEY] = ids.map { it.toString() }.toSet()
        }
    }

    companion object {
        private val HIDDEN_IDS_KEY = stringSetPreferencesKey("budget_list_hidden_category_ids")
    }
}
