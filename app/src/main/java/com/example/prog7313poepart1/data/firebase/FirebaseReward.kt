package com.example.prog7313poepart1.data.firebase

data class FirebaseReward(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val unlocked: Boolean = false,
    val progress: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)