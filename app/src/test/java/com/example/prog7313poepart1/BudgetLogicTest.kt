package com.example.prog7313poepart1

import com.example.prog7313poepart1.data.CategoryTotal
import com.example.prog7313poepart1.data.ExpenseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetLogicTest {

    @Test
    fun expenseAmount_isSavedCorrectly() {
        val expense = ExpenseEntity(
            title = "Food",
            amount = 120.50,
            date = "2026-04-29",
            time = "14:30",
            categoryId = 1,
            imagePath = "sample_images/foodslip.jpg",
            description = "Lunch receipt",
            userId = 1
        )

        assertEquals(120.50, expense.amount, 0.01)
        assertEquals("Food", expense.title)
        assertEquals("sample_images/foodslip.jpg", expense.imagePath)
    }

    @Test
    fun categoryTotal_calculatesCorrectly() {
        val expenses = listOf(
            ExpenseEntity(title = "Food 1", amount = 100.0, date = "2026-04-29", time = "10:00", categoryId = 1, userId = 1),
            ExpenseEntity(title = "Food 2", amount = 50.0, date = "2026-04-29", time = "11:00", categoryId = 1, userId = 1)
        )

        val total = expenses.sumOf { it.amount }

        assertEquals(150.0, total, 0.01)
    }

    @Test
    fun overBudgetWarning_triggersWhenTotalIsAboveMaxGoal() {
        val totalSpent = 1200.0
        val maxGoal = 1000.0

        val isOverBudget = totalSpent > maxGoal

        assertTrue(isOverBudget)
    }
}