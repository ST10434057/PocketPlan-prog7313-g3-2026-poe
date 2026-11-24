package com.example.prog7313poepart1.data.firebase

data class FirebaseGoal(
    val id: String = "",
    val userId: String = "",
    val month: String = "",
    val minimumGoal: Double = 0.0,
    val maximumGoal: Double = 0.0,
    val totalBudgetGoal: Double = 0.0,
    val categoryLimits: Map<String, Double> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
)