package com.virtualworld.easyexpensecontrol.data.local

import android.content.Context
import androidx.annotation.StringRes
import androidx.sqlite.db.SupportSQLiteDatabase
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.core.util.LocaleHelper
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.TransactionType

private data class DefaultCategoryDef(
    @StringRes val nameRes: Int,
    val iconKey: String
)

object DefaultCategories {

    private val expenseDefinitions: List<DefaultCategoryDef> = listOf(
        DefaultCategoryDef(R.string.default_category_grocery, "grocery"),
        DefaultCategoryDef(R.string.default_category_restaurant, "restaurant"),
        DefaultCategoryDef(R.string.default_category_coffee, "coffee"),
        DefaultCategoryDef(R.string.default_category_food_delivery, "restaurant"),
        DefaultCategoryDef(R.string.default_category_rent, "home"),
        DefaultCategoryDef(R.string.default_category_hoa, "home"),
        DefaultCategoryDef(R.string.default_category_electricity, "electricity"),
        DefaultCategoryDef(R.string.default_category_water, "water"),
        DefaultCategoryDef(R.string.default_category_gas, "gas"),
        DefaultCategoryDef(R.string.default_category_internet_phone, "phone"),
        DefaultCategoryDef(R.string.default_category_home_maintenance, "home"),
        DefaultCategoryDef(R.string.default_category_furniture, "shopping_bag"),
        DefaultCategoryDef(R.string.default_category_fuel, "gas"),
        DefaultCategoryDef(R.string.default_category_public_transport, "bus"),
        DefaultCategoryDef(R.string.default_category_parking, "car"),
        DefaultCategoryDef(R.string.default_category_car_maintenance, "car"),
        DefaultCategoryDef(R.string.default_category_car_insurance, "car"),
        DefaultCategoryDef(R.string.default_category_taxi, "car"),
        DefaultCategoryDef(R.string.default_category_pharmacy, "pharmacy"),
        DefaultCategoryDef(R.string.default_category_medical, "hospital"),
        DefaultCategoryDef(R.string.default_category_dental, "hospital"),
        DefaultCategoryDef(R.string.default_category_health_insurance, "hospital"),
        DefaultCategoryDef(R.string.default_category_entertainment, "movie"),
        DefaultCategoryDef(R.string.default_category_music, "music"),
        DefaultCategoryDef(R.string.default_category_fitness, "fitness"),
        DefaultCategoryDef(R.string.default_category_travel, "flight"),
        DefaultCategoryDef(R.string.default_category_hobbies, "shopping_bag"),
        DefaultCategoryDef(R.string.default_category_clothing, "shopping_bag"),
        DefaultCategoryDef(R.string.default_category_technology, "shopping"),
        DefaultCategoryDef(R.string.default_category_gifts, "shopping_bag"),
        DefaultCategoryDef(R.string.default_category_beauty, "shopping_bag"),
        DefaultCategoryDef(R.string.default_category_education, "school"),
        DefaultCategoryDef(R.string.default_category_courses, "school"),
        DefaultCategoryDef(R.string.default_category_books, "school"),
        DefaultCategoryDef(R.string.default_category_subscriptions, "subscriptions"),
        DefaultCategoryDef(R.string.default_category_insurance, "wallet"),
        DefaultCategoryDef(R.string.default_category_bank_fees, "wallet"),
        DefaultCategoryDef(R.string.default_category_taxes, "wallet"),
        DefaultCategoryDef(R.string.default_category_childcare, "wallet"),
        DefaultCategoryDef(R.string.default_category_pets, "pets"),
        DefaultCategoryDef(R.string.default_category_donations, "wallet"),
        DefaultCategoryDef(R.string.default_category_fines, "wallet"),
        DefaultCategoryDef(R.string.category_default_other, "wallet"),
    )

    private val incomeDefinitions: List<DefaultCategoryDef> = listOf(
        DefaultCategoryDef(R.string.default_income_salary, "wallet"),
        DefaultCategoryDef(R.string.default_income_freelance, "wallet"),
        DefaultCategoryDef(R.string.default_income_investments, "wallet"),
        DefaultCategoryDef(R.string.default_income_interest, "wallet"),
        DefaultCategoryDef(R.string.default_income_rental, "home"),
        DefaultCategoryDef(R.string.default_income_sales, "shopping_bag"),
        DefaultCategoryDef(R.string.default_income_refunds, "wallet"),
        DefaultCategoryDef(R.string.default_income_bonuses, "wallet"),
        DefaultCategoryDef(R.string.default_income_pension, "wallet"),
        DefaultCategoryDef(R.string.default_income_grants, "wallet"),
        DefaultCategoryDef(R.string.default_income_gifts, "shopping_bag"),
        DefaultCategoryDef(R.string.category_default_other, "wallet"),
    )

    fun buildExpenseCategories(context: Context): List<Category> =
        buildCategories(context, expenseDefinitions, TransactionType.Gasto)

    fun buildIncomeCategories(context: Context): List<Category> =
        buildCategories(context, incomeDefinitions, TransactionType.Ingreso)

    fun seedIfEmpty(context: Context, db: SupportSQLiteDatabase) {
        seedTypeIfEmpty(context, db, expenseDefinitions, TransactionType.Gasto)
        seedTypeIfEmpty(context, db, incomeDefinitions, TransactionType.Ingreso)
    }

    private fun buildCategories(
        context: Context,
        definitions: List<DefaultCategoryDef>,
        type: TransactionType
    ): List<Category> {
        val localizedContext = LocaleHelper.applySavedLocale(context)
        return definitions.map { def ->
            Category(
                name = localizedContext.getString(def.nameRes),
                type = type,
                iconName = def.iconKey
            )
        }
    }

    private fun seedTypeIfEmpty(
        context: Context,
        db: SupportSQLiteDatabase,
        definitions: List<DefaultCategoryDef>,
        type: TransactionType
    ) {
        if (getCategoryCountByType(db, type) > 0) return
        val localizedContext = LocaleHelper.applySavedLocale(context)
        val typeValue = type.name
        for (def in definitions) {
            val name = escapeSql(localizedContext.getString(def.nameRes))
            db.execSQL(
                """
                INSERT OR IGNORE INTO `Category` (`name`, `type`, `icon_name`)
                VALUES ('$name', '$typeValue', '${def.iconKey}')
                """.trimIndent()
            )
        }
    }

    private fun getCategoryCountByType(db: SupportSQLiteDatabase, type: TransactionType): Int {
        db.query(
            "SELECT COUNT(*) FROM `Category` WHERE `type` = ?",
            arrayOf(type.name)
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun escapeSql(value: String): String = value.replace("'", "''")
}
