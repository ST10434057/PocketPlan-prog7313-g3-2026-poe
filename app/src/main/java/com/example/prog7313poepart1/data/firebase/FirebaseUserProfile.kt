package com.example.prog7313poepart1.data.firebase

data class FirebaseUserProfile(
    val uid: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis()
)