package com.virtualworld.easyexpensecontrol.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.virtualworld.easyexpensecontrol.data.model.Account
import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.RecurringTransaction
import com.virtualworld.easyexpensecontrol.data.model.Transaction

@Database(
    entities = [
        Transaction::class,
        Category::class,
        Budget::class,
        Account::class,
        RecurringTransaction::class
    ],
    version = 6,
    exportSchema = false
)
abstract class FinancialDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun accountDao(): AccountDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Category ADD COLUMN icon_name TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAccountTable(db)
                recreateTransactionTableWithAccountForeignKey(db, accountColumnAlreadyExists = false)
            }
        }

        /**
         * Repara instalaciones que aplicaron la migración 2→3 anterior (ALTER TABLE sin FK).
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAccountTable(db)
                recreateTransactionTableWithAccountForeignKey(
                    db,
                    accountColumnAlreadyExists = transactionHasAccountColumn(db)
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!accountHasHiddenColumn(db)) {
                    db.execSQL(
                        """
                        ALTER TABLE `Account`
                        ADD COLUMN `account-hidden` INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )
                }
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `RecurringTransaction` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `recurring-type` TEXT NOT NULL,
                        `recurring-amount` REAL NOT NULL,
                        `recurring-category` INTEGER NOT NULL,
                        `recurring-description` TEXT NOT NULL,
                        `recurring-account` INTEGER NOT NULL,
                        `recurring-day-of-month` INTEGER NOT NULL,
                        `recurring-is-active` INTEGER NOT NULL DEFAULT 1,
                        `recurring-last-processed-year-month` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`recurring-category`) REFERENCES `Category`(`id`)
                            ON UPDATE CASCADE ON DELETE CASCADE,
                        FOREIGN KEY(`recurring-account`) REFERENCES `Account`(`id`)
                            ON UPDATE CASCADE ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_RecurringTransaction_recurring-category`
                    ON `RecurringTransaction` (`recurring-category`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_RecurringTransaction_recurring-account`
                    ON `RecurringTransaction` (`recurring-account`)
                    """.trimIndent()
                )
                recreateTransactionTableWithRecurringColumns(db)
            }
        }

        private fun recreateTransactionTableWithRecurringColumns(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `Transaction_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `transaction-type` TEXT NOT NULL,
                    `transaction-amount` REAL NOT NULL,
                    `transaction-category` INTEGER NOT NULL,
                    `transaction-date` INTEGER NOT NULL,
                    `transaction-description` TEXT NOT NULL,
                    `transaction-account` INTEGER NOT NULL,
                    `transaction-recurring-id` INTEGER,
                    `transaction-is-auto-generated` INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(`transaction-category`) REFERENCES `Category`(`id`)
                        ON UPDATE CASCADE ON DELETE CASCADE,
                    FOREIGN KEY(`transaction-account`) REFERENCES `Account`(`id`)
                        ON UPDATE CASCADE ON DELETE RESTRICT,
                    FOREIGN KEY(`transaction-recurring-id`) REFERENCES `RecurringTransaction`(`id`)
                        ON UPDATE CASCADE ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `Transaction_new` (
                    `id`, `transaction-type`, `transaction-amount`, `transaction-category`,
                    `transaction-date`, `transaction-description`, `transaction-account`,
                    `transaction-recurring-id`, `transaction-is-auto-generated`
                )
                SELECT
                    `id`, `transaction-type`, `transaction-amount`, `transaction-category`,
                    `transaction-date`, `transaction-description`, `transaction-account`,
                    NULL, 0
                FROM `Transaction`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `Transaction`")
            db.execSQL("ALTER TABLE `Transaction_new` RENAME TO `Transaction`")
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_Transaction_transaction-category`
                ON `Transaction` (`transaction-category`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_Transaction_transaction-account`
                ON `Transaction` (`transaction-account`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_Transaction_transaction-recurring-id`
                ON `Transaction` (`transaction-recurring-id`)
                """.trimIndent()
            )
        }

        private fun ensureAccountTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `Account` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `account-name` TEXT NOT NULL,
                    `account-color` INTEGER,
                    `account-hidden` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO `Account` (`id`, `account-name`, `account-color`)
                VALUES (1, 'General', NULL)
                """.trimIndent()
            )
        }

        private fun accountHasHiddenColumn(db: SupportSQLiteDatabase): Boolean {
            db.query("PRAGMA table_info(`Account`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex < 0) return false
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == "account-hidden") {
                        return true
                    }
                }
            }
            return false
        }

        private fun transactionHasAccountColumn(db: SupportSQLiteDatabase): Boolean {
            db.query("PRAGMA table_info(`Transaction`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex < 0) return false
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == "transaction-account") {
                        return true
                    }
                }
            }
            return false
        }

        private fun recreateTransactionTableWithAccountForeignKey(
            db: SupportSQLiteDatabase,
            accountColumnAlreadyExists: Boolean
        ) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `Transaction_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `transaction-type` TEXT NOT NULL,
                    `transaction-amount` REAL NOT NULL,
                    `transaction-category` INTEGER NOT NULL,
                    `transaction-date` INTEGER NOT NULL,
                    `transaction-description` TEXT NOT NULL,
                    `transaction-account` INTEGER NOT NULL,
                    FOREIGN KEY(`transaction-category`) REFERENCES `Category`(`id`)
                        ON UPDATE CASCADE ON DELETE CASCADE,
                    FOREIGN KEY(`transaction-account`) REFERENCES `Account`(`id`)
                        ON UPDATE CASCADE ON DELETE RESTRICT
                )
                """.trimIndent()
            )

            if (accountColumnAlreadyExists) {
                db.execSQL(
                    """
                    INSERT INTO `Transaction_new` (
                        `id`, `transaction-type`, `transaction-amount`, `transaction-category`,
                        `transaction-date`, `transaction-description`, `transaction-account`
                    )
                    SELECT
                        `id`, `transaction-type`, `transaction-amount`, `transaction-category`,
                        `transaction-date`, `transaction-description`, `transaction-account`
                    FROM `Transaction`
                    """.trimIndent()
                )
            } else {
                db.execSQL(
                    """
                    INSERT INTO `Transaction_new` (
                        `id`, `transaction-type`, `transaction-amount`, `transaction-category`,
                        `transaction-date`, `transaction-description`, `transaction-account`
                    )
                    SELECT
                        `id`, `transaction-type`, `transaction-amount`, `transaction-category`,
                        `transaction-date`, `transaction-description`, 1
                    FROM `Transaction`
                    """.trimIndent()
                )
            }

            db.execSQL("DROP TABLE `Transaction`")
            db.execSQL("ALTER TABLE `Transaction_new` RENAME TO `Transaction`")
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_Transaction_transaction-category`
                ON `Transaction` (`transaction-category`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_Transaction_transaction-account`
                ON `Transaction` (`transaction-account`)
                """.trimIndent()
            )
        }
    }
}
