package com.virtualworld.easyexpensecontrol.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "RecurringTransaction",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["recurring-category"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["recurring-account"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "recurring-type")
    val type: TransactionType = TransactionType.Ingreso,
    @ColumnInfo(name = "recurring-amount")
    val amount: Double = 0.0,
    @ColumnInfo(name = "recurring-category", index = true)
    val categoryId: Long = 0L,
    @ColumnInfo(name = "recurring-description")
    val description: String = "",
    @ColumnInfo(name = "recurring-account", index = true)
    val accountId: Long = 1L,
    @ColumnInfo(name = "recurring-day-of-month")
    val dayOfMonth: Int = 1,
    @ColumnInfo(name = "recurring-is-active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "recurring-last-processed-year-month")
    val lastProcessedYearMonth: Int = 0
)
