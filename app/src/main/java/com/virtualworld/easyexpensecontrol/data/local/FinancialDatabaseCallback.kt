package com.virtualworld.easyexpensecontrol.data.local

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

class FinancialDatabaseCallback(
    private val context: Context
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.execSQL(
            """
            INSERT OR IGNORE INTO `Account` (`id`, `account-name`, `account-color`, `account-hidden`)
            VALUES (1, 'General', NULL, 0)
            """.trimIndent()
        )
        DefaultCategories.seedIfEmpty(context, db)
    }
}
