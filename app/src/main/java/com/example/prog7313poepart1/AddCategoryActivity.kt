package com.example.prog7313poepart1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.prog7313poepart1.data.firebase.FirebaseRepository
import kotlinx.coroutines.launch

class AddCategoryActivity : AppCompatActivity() {

    private lateinit var firebaseRepository: FirebaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_category)

        firebaseRepository = FirebaseRepository(this)

        val etCategoryName = findViewById<EditText>(R.id.etCategoryName)
        val btnSaveCategory = findViewById<Button>(R.id.btnSaveCategory)

        btnSaveCategory.setOnClickListener {
            val name = etCategoryName.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    firebaseRepository.addCategory(name)
                    Toast.makeText(
                        this@AddCategoryActivity,
                        "Category saved online",
                        Toast.LENGTH_SHORT
                    ).show()
                    etCategoryName.text.clear()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@AddCategoryActivity,
                        "Could not save category: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
