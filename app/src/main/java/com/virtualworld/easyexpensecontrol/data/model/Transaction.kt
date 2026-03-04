package com.virtualworld.easyexpensecontrol.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Transaction",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["transaction-category"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "transaction-type")
    val type: TransactionType = TransactionType.Ingreso,
    @ColumnInfo(name = "transaction-amount")
    val amount: Double = 0.0,
    @ColumnInfo(name = "transaction-category", index = true)
    val category: Long = 0L,
    @ColumnInfo(name = "transaction-date")
    val date: Long = 0L,
    @ColumnInfo(name = "transaction-description")
    val description: String = ""
)
