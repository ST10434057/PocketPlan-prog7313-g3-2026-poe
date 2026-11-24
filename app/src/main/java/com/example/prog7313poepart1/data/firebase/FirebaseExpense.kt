package com.example.prog7313poepart1.data.firebase

data class FirebaseExpense(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val categoryId: String = "",
    val categoryName: String = "",
    val date: String = "",
    val time: String = "",
    val description: String = "",
    val receiptImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)