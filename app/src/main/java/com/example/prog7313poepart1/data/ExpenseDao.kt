package com.example.prog7313poepart1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses ORDER BY id DESC")
    suspend fun getExpenses(): List<ExpenseEntity>

    @Query("""
        SELECT categories.name AS categoryName, SUM(expenses.amount) AS totalAmount
        FROM expenses
        INNER JOIN categories ON expenses.categoryId = categories.id
        GROUP BY categories.name
        ORDER BY categories.name ASC
    """)
    suspend fun getCategoryTotals(): List<CategoryTotal>
}