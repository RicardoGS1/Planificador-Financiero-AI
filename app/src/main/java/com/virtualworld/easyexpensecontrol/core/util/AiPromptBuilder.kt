package com.virtualworld.easyexpensecontrol.core.util

import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import java.util.Locale

/**
 * Construye prompts para Gemini (comprobante y audio).
 * Instrucciones en inglés para no sesgar el idioma de "description".
 * Solo "categoryName" usa el idioma de la app.
 */
object AiPromptBuilder {

    enum class InputMode { RECEIPT, AUDIO }

    fun buildPrompt(
        mode: InputMode,
        transactionType: TransactionType?,
        expenseCategoryNames: List<String>,
        incomeCategoryNames: List<String>,
        accountNames: List<String>,
        outputLanguageTag: String,
        defaultOtherCategory: String
    ): String {
        val appLanguageName = languageDisplayName(outputLanguageTag)
        val typeLabel = when (transactionType) {
            TransactionType.Gasto -> "expense"
            TransactionType.Ingreso -> "income"
            null -> "expense or income"
        }
        val descriptionRule = when (mode) {
            InputMode.AUDIO -> audioDescriptionRule(appLanguageName)
            InputMode.RECEIPT -> receiptDescriptionRule(appLanguageName)
        }
        val taskLine = when (mode) {
            InputMode.AUDIO -> "Listen to the attached voice note and extract financial transaction(s)."
            InputMode.RECEIPT -> "Analyze the attached receipt or invoice image and extract financial expense transaction(s)."
        }
        val categorySection = buildCategorySection(
            transactionType = transactionType,
            expenseCategoryNames = expenseCategoryNames,
            incomeCategoryNames = incomeCategoryNames,
            defaultOtherCategory = defaultOtherCategory,
            appLanguageName = appLanguageName
        )
        val accountSection = buildAccountSection(accountNames)
        val modeRules = when (mode) {
            InputMode.RECEIPT -> receiptRules()
            InputMode.AUDIO -> audioRules(typeLabel, transactionType == null)
        }
        val defaultTypeRule = when (transactionType) {
            TransactionType.Gasto -> "Default transactionType to \"expense\" when not specified."
            TransactionType.Ingreso -> "Default transactionType to \"income\" when not specified."
            null -> "Infer transactionType from context; use \"expense\" or \"income\" only."
        }

        return """
            $descriptionRule

            $taskLine
            Return ONLY valid JSON (no markdown, no extra text):
            {"transactions":[{"amount":0.0,"description":"","categoryName":"","date":"","transactionType":"","accountName":""}]}

            $categorySection

            $accountSection

            Field rules:
            - amount: positive number, dot as decimal separator; use 0.0 if unknown.
            - description: brief summary, max 50 characters. Language rule above is mandatory.
            - categoryName: max 30 characters, ONLY field that must be in $appLanguageName. Pick categories matching the transaction type.
            - date: ISO yyyy-MM-dd when the user or receipt mentions a date; empty string "" if not mentioned or unknown.
            - transactionType: "expense" or "income" when specified or clearly inferable; empty string "" otherwise. $defaultTypeRule
            - accountName: EXACT name from the account list when the user mentions an account or payment method; empty string "" if not mentioned.

            $modeRules
        """.trimIndent()
    }

    private fun audioDescriptionRule(appLanguageName: String): String = """
        CRITICAL — DESCRIPTION LANGUAGE (follow strictly, highest priority):
        1. Detect the language the user speaks in the audio.
        2. Write "description" ONLY in that detected spoken language.
        3. Do NOT translate "description" to $appLanguageName or any other language.
        4. Do NOT use the app language for "description" even if these instructions are shown in another language.
        5. Only "categoryName" must be in $appLanguageName (app language).

        Example — app=$appLanguageName, user speaks Hindi:
        CORRECT:   {"amount":50.0,"description":"स्टारबक्स में कॉफी","categoryName":"Restaurante","date":"","transactionType":"expense","accountName":""}
        WRONG:     {"amount":50.0,"description":"Café en Starbucks","categoryName":"Restaurante","date":"","transactionType":"expense","accountName":""}
    """.trimIndent()

    private fun receiptDescriptionRule(appLanguageName: String): String = """
        CRITICAL — DESCRIPTION LANGUAGE (follow strictly, highest priority):
        1. Detect the language of the text printed on the receipt or invoice.
        2. Write "description" ONLY in that detected language (use the dominant language if mixed).
        3. Do NOT translate "description" to $appLanguageName or any other language.
        4. Only "categoryName" must be in $appLanguageName (app language).

        Example — app=$appLanguageName, receipt text in Hindi:
        CORRECT:   {"amount":120.0,"description":"सब्जियां और दूध","categoryName":"Supermercado","date":"2024-03-15","transactionType":"expense","accountName":""}
        WRONG:     {"amount":120.0,"description":"Verduras y leche","categoryName":"Supermercado","date":"","transactionType":"expense","accountName":""}
    """.trimIndent()

    private fun buildCategorySection(
        transactionType: TransactionType?,
        expenseCategoryNames: List<String>,
        incomeCategoryNames: List<String>,
        defaultOtherCategory: String,
        appLanguageName: String
    ): String {
        fun formatList(names: List<String>): String = when {
            names.isEmpty() -> "no predefined list"
            else -> names.joinToString(", ")
        }

        return when (transactionType) {
            TransactionType.Gasto -> categoryBlock(
                typeLabel = "expense",
                names = expenseCategoryNames,
                defaultOtherCategory = defaultOtherCategory,
                appLanguageName = appLanguageName
            )
            TransactionType.Ingreso -> categoryBlock(
                typeLabel = "income",
                names = incomeCategoryNames,
                defaultOtherCategory = defaultOtherCategory,
                appLanguageName = appLanguageName
            )
            null -> """
                Expense categories: prefer an EXACT name from this list when it fits: ${formatList(expenseCategoryNames)}.
                Income categories: prefer an EXACT name from this list when it fits: ${formatList(incomeCategoryNames)}.
                All category names must be in $appLanguageName.
                Match categoryName to the detected transactionType.
                Use "$defaultOtherCategory" only when genuinely miscellaneous.
            """.trimIndent()
        }
    }

    private fun categoryBlock(
        typeLabel: String,
        names: List<String>,
        defaultOtherCategory: String,
        appLanguageName: String
    ): String = if (names.isEmpty()) {
        """
            $typeLabel categories: no predefined list. Suggest a short sensible name in $appLanguageName.
            Use "$defaultOtherCategory" only when nothing more specific applies.
        """.trimIndent()
    } else {
        val list = names.joinToString(", ")
        """
            $typeLabel categories: prefer an EXACT name from this list when it fits: $list.
            All category names must be in $appLanguageName.
            If none fits well, create a new short name in $appLanguageName (do not force a weak match).
            Use "$defaultOtherCategory" only when genuinely miscellaneous.
        """.trimIndent()
    }

    private fun buildAccountSection(accountNames: List<String>): String = if (accountNames.isEmpty()) {
        "Accounts: no accounts configured. Always return empty string for accountName."
    } else {
        val list = accountNames.joinToString(", ")
        """
            Accounts: when the user mentions an account, wallet or payment source, use an EXACT name from: $list.
            If none matches, leave accountName as empty string.
        """.trimIndent()
    }

    private fun receiptRules(): String = """
        Receipt rules:
        - One transaction per distinct category or relevant line when the receipt spans multiple categories.
        - Single-category receipt: return one transaction.
        - Amounts should sum to the receipt total when possible.
        - Extract the receipt date into "date" when visible on the document.
        - transactionType is usually "expense"; leave empty only if truly unclear.
    """.trimIndent()

    private fun audioRules(typeLabel: String, typeUndetermined: Boolean): String {
        val typeRule = if (typeUndetermined) {
            "- Detect whether each item is expense or income and set transactionType accordingly."
        } else {
            "- Transaction type context: $typeLabel."
        }
        return """
            Audio rules:
            $typeRule
            - One transaction per distinct item mentioned.
            - Single item: return one transaction.
            - Empty description if nothing meaningful can be inferred.
            - If the user says a date (e.g. yesterday, last Monday, 15 March), convert it to yyyy-MM-dd in "date".
            - If the user names an account, set accountName to the matching name from the list.
        """.trimIndent()
    }

    fun resolveOutputLanguageTag(contextTag: String, fallbackLocale: Locale = Locale.getDefault()): String =
        contextTag.trim().ifBlank { fallbackLocale.toLanguageTag() }

    fun parseTransactionType(raw: String, fallback: TransactionType): TransactionType =
        when (raw.trim().lowercase()) {
            "income", "ingreso", "ingresos" -> TransactionType.Ingreso
            "expense", "gasto", "gastos", "expenses" -> TransactionType.Gasto
            else -> fallback
        }

    private fun languageDisplayName(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        return locale.getDisplayLanguage(Locale.ENGLISH).replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.ENGLISH) else ch.toString()
        }.ifBlank { "English" }
    }
}
