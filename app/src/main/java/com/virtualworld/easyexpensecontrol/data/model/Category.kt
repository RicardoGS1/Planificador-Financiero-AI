package com.virtualworld.easyexpensecontrol.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Category",
    indices = [
        Index(
            value = ["name", "type"],
            unique = true
        )
    ]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "type")
    val type: TransactionType
)
