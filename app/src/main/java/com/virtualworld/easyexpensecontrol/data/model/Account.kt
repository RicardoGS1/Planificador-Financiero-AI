package com.virtualworld.easyexpensecontrol.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Account")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "account-name")
    val name: String,
    @ColumnInfo(name = "account-color")
    val colorArgb: Int? = null,
    @ColumnInfo(name = "account-hidden", defaultValue = "0")
    val isHidden: Boolean = false
)
