package com.example.prog7313poepart1.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseSeeder {

    fun seedDefaults(context: Context) {
        val db = DatabaseProvider.getDatabase(context)

        CoroutineScope(Dispatchers.IO).launch {
            val existingCategories = db.categoryDao().getAllCategories()

            if (existingCategories.isEmpty()) {
                db.categoryDao().insertCategory(CategoryEntity(name = "Food"))
                db.categoryDao().insertCategory(CategoryEntity(name = "Transport"))
                db.categoryDao().insertCategory(CategoryEntity(name = "Book/Study"))
            }
        }
    }
}