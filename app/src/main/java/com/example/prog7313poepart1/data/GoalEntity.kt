package com.example.prog7313poepart1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val minGoal: Double,
    val maxGoal: Double,
    val month: String
)