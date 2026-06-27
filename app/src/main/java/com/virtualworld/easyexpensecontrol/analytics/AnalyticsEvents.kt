package com.virtualworld.easyexpensecontrol.analytics

object AnalyticsEvents {
    const val SCREEN_VIEW = "screen_view"
    const val TRANSACTION_SAVED = "transaction_saved"
    const val TRANSACTION_DELETED = "transaction_deleted"
    const val TRANSACTIONS_BATCH_SAVED = "transactions_batch_saved"
    const val AI_ANALYSIS_STARTED = "ai_analysis_started"
    const val AI_ANALYSIS_RESULT = "ai_analysis_result"
    const val BUDGET_SAVED = "budget_saved"
    const val BUDGET_DELETED = "budget_deleted"
    const val AD_INTERSTITIAL_SHOWN = "ad_interstitial_shown"
    const val AD_INTERSTITIAL_SKIPPED = "ad_interstitial_skipped"
    const val AD_REWARDED_COMPLETED = "ad_rewarded_completed"
    const val AD_REWARDED_DISMISSED = "ad_rewarded_dismissed"

    const val PARAM_SCREEN_NAME = "screen_name"
    const val PARAM_TYPE = "type"
    const val PARAM_IS_EDIT = "is_edit"
    const val PARAM_COUNT = "count"
    const val PARAM_SOURCE = "source"
    const val PARAM_SUCCESS = "success"
    const val PARAM_TRANSACTION_COUNT = "transaction_count"
    const val PARAM_ERROR_CATEGORY = "error_category"
    const val PARAM_PLACEMENT = "placement"
    const val PARAM_SKIP_REASON = "skip_reason"

    const val TYPE_INCOME = "income"
    const val TYPE_EXPENSE = "expense"

    const val SOURCE_RECEIPT = "receipt"
    const val SOURCE_AUDIO = "audio"
    const val SOURCE_SPREADSHEET = "spreadsheet"

    const val PLACEMENT_AI = "ai"
    const val PLACEMENT_CAMERA = "camera"

    const val SKIP_FREQUENCY_CAP = "frequency_cap"
    const val SKIP_NOT_READY = "not_ready"
    const val SKIP_TOO_SOON = "too_soon"
    const val SKIP_DISABLED = "disabled"

    const val ERROR_NETWORK = "network"
    const val ERROR_PARSE = "parse"
    const val ERROR_EMPTY = "empty"
    const val ERROR_UNKNOWN = "unknown"
}
