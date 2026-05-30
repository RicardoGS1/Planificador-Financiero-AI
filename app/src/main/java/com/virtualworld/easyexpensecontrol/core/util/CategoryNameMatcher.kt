package com.virtualworld.easyexpensecontrol.core.util

import com.virtualworld.easyexpensecontrol.data.model.Category
import java.text.Normalizer

/**
 * Resuelve el nombre de categoría sugerido por la IA contra las categorías existentes.
 * Si no hay coincidencia, devuelve null para permitir crear una categoría nueva.
 */
object CategoryNameMatcher {

    private val otherLabels: Set<String> = setOf(
        "otros", "other", "sonstiges", "прочее", "अन्य", "misc", "miscellaneous"
    )

    fun resolve(
        suggestedName: String,
        categories: List<Category>,
        defaultOtherLabel: String
    ): Category? {
        if (categories.isEmpty()) return null

        val normalizedSuggested = normalize(suggestedName)
        if (normalizedSuggested.isBlank()) {
            return findOther(categories, defaultOtherLabel)
        }

        categories.firstOrNull { normalize(it.name) == normalizedSuggested }?.let { return it }

        categories.firstOrNull { category ->
            val normalizedCategory = normalize(category.name)
            normalizedCategory.contains(normalizedSuggested) ||
                normalizedSuggested.contains(normalizedCategory)
        }?.let { return it }

        if (normalizedSuggested in otherLabels || normalize(defaultOtherLabel) == normalizedSuggested) {
            return findOther(categories, defaultOtherLabel)
        }

        return null
    }

    private fun findOther(categories: List<Category>, defaultOtherLabel: String): Category? {
        val normalizedDefault = normalize(defaultOtherLabel)
        return categories.firstOrNull { normalize(it.name) == normalizedDefault }
            ?: categories.firstOrNull { normalize(it.name) in otherLabels }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
}
