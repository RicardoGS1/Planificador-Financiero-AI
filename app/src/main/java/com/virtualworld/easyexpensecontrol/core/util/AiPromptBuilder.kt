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
        categoryNames: List<String>,
        outputLanguageTag: String,
        defaultOtherCategory: String
    ): String {
        val appLanguageName = languageDisplayName(outputLanguageTag)
        val typeLabel = when (transactionType) {
            TransactionType.Gasto -> "expense"
            TransactionType.Ingreso -> "income"
            null -> "expense"
        }
        val descriptionRule = when (mode) {
            InputMode.AUDIO -> audioDescriptionRule(appLanguageName)
            InputMode.RECEIPT -> receiptDescriptionRule(appLanguageName)
        }
        val taskLine = when (mode) {
            InputMode.AUDIO -> "Listen to the attached voice note and extract financial $typeLabel transaction(s)."
            InputMode.RECEIPT -> "Analyze the attached receipt or invoice image and extract financial expense transaction(s)."
        }
        val categorySection = buildCategorySection(categoryNames, defaultOtherCategory, appLanguageName)
        val modeRules = when (mode) {
            InputMode.RECEIPT -> receiptRules()
            InputMode.AUDIO -> audioRules(typeLabel)
        }

        return """
            $descriptionRule

            $taskLine
            Return ONLY valid JSON (no markdown, no extra text):
            {"transactions":[{"amount":0.0,"description":"","categoryName":""}]}

            $categorySection

            Field rules:
            - amount: positive number, dot as decimal separator; use 0.0 if unknown.
            - description: brief summary, max 50 characters. Language rule above is mandatory.
            - categoryName: max 30 characters, ONLY field that must be in $appLanguageName.

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
        CORRECT:   {"amount":50.0,"description":"स्टारबक्स में कॉफी","categoryName":"Restaurante"}
        WRONG:     {"amount":50.0,"description":"Café en Starbucks","categoryName":"Restaurante"}
    """.trimIndent()

    private fun receiptDescriptionRule(appLanguageName: String): String = """
        CRITICAL — DESCRIPTION LANGUAGE (follow strictly, highest priority):
        1. Detect the language of the text printed on the receipt or invoice.
        2. Write "description" ONLY in that detected language (use the dominant language if mixed).
        3. Do NOT translate "description" to $appLanguageName or any other language.
        4. Only "categoryName" must be in $appLanguageName (app language).

        Example — app=$appLanguageName, receipt text in Hindi:
        CORRECT:   {"amount":120.0,"description":"सब्जियां और दूध","categoryName":"Supermercado"}
        WRONG:     {"amount":120.0,"description":"Verduras y leche","categoryName":"Supermercado"}
    """.trimIndent()

    private fun buildCategorySection(
        categoryNames: List<String>,
        defaultOtherCategory: String,
        appLanguageName: String
    ): String = if (categoryNames.isEmpty()) {
        """
            Categories: no predefined list. Suggest a short sensible name in $appLanguageName for each transaction.
            Use "$defaultOtherCategory" only when nothing more specific applies.
        """.trimIndent()
    } else {
        val list = categoryNames.joinToString(", ")
        """
            Categories: prefer an EXACT name from this list when it fits: $list.
            All category names must be in $appLanguageName.
            If none fits well, create a new short name in $appLanguageName (do not force a weak match).
            Use "$defaultOtherCategory" only when genuinely miscellaneous.
        """.trimIndent()
    }

    private fun receiptRules(): String = """
        Receipt rules:
        - One transaction per distinct category or relevant line when the receipt spans multiple categories.
        - Single-category receipt: return one transaction.
        - Amounts should sum to the receipt total when possible.
    """.trimIndent()

    private fun audioRules(typeLabel: String): String = """
        Audio rules:
        - Transaction type: $typeLabel.
        - One transaction per distinct $typeLabel mentioned.
        - Single $typeLabel: return one transaction.
        - Empty description if nothing meaningful can be inferred.
    """.trimIndent()

    fun resolveOutputLanguageTag(contextTag: String, fallbackLocale: Locale = Locale.getDefault()): String =
        contextTag.trim().ifBlank { fallbackLocale.toLanguageTag() }

    private fun languageDisplayName(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        return locale.getDisplayLanguage(Locale.ENGLISH).replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.ENGLISH) else ch.toString()
        }.ifBlank { "English" }
    }
}
