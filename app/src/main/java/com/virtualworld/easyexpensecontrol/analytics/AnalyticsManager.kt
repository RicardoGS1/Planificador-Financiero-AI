package com.virtualworld.easyexpensecontrol.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.virtualworld.easyexpensecontrol.BuildConfig
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.viewmodel.AiAnalysisSource
import java.io.IOException

class AnalyticsManager(context: Context) {

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)

    fun logScreenView(screenName: String) {
        if (!AnalyticsConsent.isAnalyticsStorageGranted()) return
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logTransactionSaved(type: TransactionType, isEdit: Boolean) {
        logEvent(
            AnalyticsEvents.TRANSACTION_SAVED,
            bundleOf(
                AnalyticsEvents.PARAM_TYPE to type.toAnalyticsValue(),
                AnalyticsEvents.PARAM_IS_EDIT to isEdit
            )
        )
    }

    fun logTransactionDeleted(type: TransactionType) {
        logEvent(
            AnalyticsEvents.TRANSACTION_DELETED,
            bundleOf(AnalyticsEvents.PARAM_TYPE to type.toAnalyticsValue())
        )
    }

    fun logTransactionsBatchSaved(count: Int) {
        logEvent(
            AnalyticsEvents.TRANSACTIONS_BATCH_SAVED,
            bundleOf(AnalyticsEvents.PARAM_COUNT to count.toLong())
        )
    }

    fun logAiAnalysisStarted(source: AiAnalysisSource) {
        logEvent(
            AnalyticsEvents.AI_ANALYSIS_STARTED,
            bundleOf(AnalyticsEvents.PARAM_SOURCE to source.toAnalyticsValue())
        )
    }

    fun logAiAnalysisResult(
        source: AiAnalysisSource,
        success: Boolean,
        transactionCount: Int = 0,
        errorCategory: String? = null
    ) {
        val bundle = bundleOf(
            AnalyticsEvents.PARAM_SOURCE to source.toAnalyticsValue(),
            AnalyticsEvents.PARAM_SUCCESS to success
        )
        if (success) {
            bundle.putLong(AnalyticsEvents.PARAM_TRANSACTION_COUNT, transactionCount.toLong())
        } else if (errorCategory != null) {
            bundle.putString(AnalyticsEvents.PARAM_ERROR_CATEGORY, errorCategory)
        }
        logEvent(AnalyticsEvents.AI_ANALYSIS_RESULT, bundle)
    }

    fun logBudgetSaved(isEdit: Boolean) {
        logEvent(
            AnalyticsEvents.BUDGET_SAVED,
            bundleOf(AnalyticsEvents.PARAM_IS_EDIT to isEdit)
        )
    }

    fun logBudgetDeleted() {
        logEvent(AnalyticsEvents.BUDGET_DELETED)
    }

    fun logAdInterstitialShown() {
        logEvent(AnalyticsEvents.AD_INTERSTITIAL_SHOWN)
    }

    fun logAdInterstitialSkipped(reason: String) {
        logEvent(
            AnalyticsEvents.AD_INTERSTITIAL_SKIPPED,
            bundleOf(AnalyticsEvents.PARAM_SKIP_REASON to reason)
        )
    }

    fun logAdRewardedCompleted(placement: String) {
        logEvent(
            AnalyticsEvents.AD_REWARDED_COMPLETED,
            bundleOf(AnalyticsEvents.PARAM_PLACEMENT to placement)
        )
    }

    fun logAdRewardedDismissed(placement: String) {
        logEvent(
            AnalyticsEvents.AD_REWARDED_DISMISSED,
            bundleOf(AnalyticsEvents.PARAM_PLACEMENT to placement)
        )
    }

    fun categorizeAiError(error: Throwable): String {
        return when {
            error is IOException -> AnalyticsEvents.ERROR_NETWORK
            error is IllegalArgumentException -> AnalyticsEvents.ERROR_PARSE
            error.message == "empty_file" || error.message == "empty_spreadsheet" ->
                AnalyticsEvents.ERROR_EMPTY
            else -> AnalyticsEvents.ERROR_UNKNOWN
        }
    }

    private fun logEvent(name: String, params: Bundle = Bundle.EMPTY) {
        if (!AnalyticsConsent.isAnalyticsStorageGranted()) return
        firebaseAnalytics.logEvent(name, params)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Event: $name params=${params.keySet().associateWith { params.get(it) }}")
        }
    }

    private fun bundleOf(vararg pairs: Pair<String, Any>): Bundle {
        val bundle = Bundle()
        for ((key, value) in pairs) {
            when (value) {
                is String -> bundle.putString(key, value)
                is Long -> bundle.putLong(key, value)
                is Int -> bundle.putLong(key, value.toLong())
                is Boolean -> bundle.putLong(key, if (value) 1L else 0L)
            }
        }
        return bundle
    }

    companion object {
        private const val TAG = "AnalyticsManager"

        @Volatile
        private var instance: AnalyticsManager? = null

        fun bind(manager: AnalyticsManager) {
            instance = manager
        }

        fun current(): AnalyticsManager? = instance
    }
}

private fun TransactionType.toAnalyticsValue(): String = when (this) {
    TransactionType.Ingreso -> AnalyticsEvents.TYPE_INCOME
    TransactionType.Gasto -> AnalyticsEvents.TYPE_EXPENSE
}

private fun AiAnalysisSource.toAnalyticsValue(): String = when (this) {
    AiAnalysisSource.RECEIPT -> AnalyticsEvents.SOURCE_RECEIPT
    AiAnalysisSource.AUDIO -> AnalyticsEvents.SOURCE_AUDIO
    AiAnalysisSource.SPREADSHEET -> AnalyticsEvents.SOURCE_SPREADSHEET
}
