package com.example.prog7313poepart1.data.firebase

data class FirebaseCategory(
    val id: String = "",
    val name: String = "",
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)