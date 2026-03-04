package com.virtualworld.easyexpensecontrol.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Budget",
    indices = [Index(
        value = ["budget-month", "budget-year", "budget-category"],
        unique = true
    )],
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["budget-category"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "budget-category")
    val category: Long = 0L,
    @ColumnInfo(name = "budget-monthlyLimit")
    val monthlyLimit: Double = 0.0,
    @ColumnInfo(name = "budget-currentExpenditure")
    val currentExpenditure: Double = 0.0,
    @ColumnInfo(name = "budget-month")
    val month: String = "0",
    @ColumnInfo(name = "budget-year")
    val year: Int = 0
)
