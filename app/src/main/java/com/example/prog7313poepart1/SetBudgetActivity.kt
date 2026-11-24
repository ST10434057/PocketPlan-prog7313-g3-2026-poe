package com.example.prog7313poepart1

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.prog7313poepart1.data.CategoryEntity
import com.example.prog7313poepart1.data.DatabaseProvider
import com.example.prog7313poepart1.data.firebase.FirebaseCategory
import com.example.prog7313poepart1.data.firebase.FirebaseGoal
import com.example.prog7313poepart1.data.firebase.FirebaseRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SetBudgetActivity : AppCompatActivity() {

    private lateinit var edtBudgetAmount: TextInputEditText
    private lateinit var edtSavingsGoal: TextInputEditText
    private lateinit var btnSaveBudget: MaterialButton
    private lateinit var btnCancel: MaterialButton

    private lateinit var edtCustomCategoryName: TextInputEditText
    private lateinit var edtCustomCategoryBudget: TextInputEditText
    private lateinit var btnAddCustomCategory: MaterialButton
    private lateinit var layoutCustomCategoriesContainer: LinearLayout

    private lateinit var firebaseRepository: FirebaseRepository

    private var firebaseCategories: List<FirebaseCategory> = emptyList()
    private val categoryLimitInputs = mutableMapOf<String, EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_budget)

        firebaseRepository = FirebaseRepository(this)
        val db = DatabaseProvider.getDatabase(this)

        edtBudgetAmount = findViewById(R.id.edtBudgetAmount)
        edtSavingsGoal = findViewById(R.id.edtSavingsGoal)
        btnSaveBudget = findViewById(R.id.btnSaveBudget)
        btnCancel = findViewById(R.id.btnCancel)

        edtCustomCategoryName = findViewById(R.id.edtCustomCategoryName)
        edtCustomCategoryBudget = findViewById(R.id.edtCustomCategoryBudget)
        btnAddCustomCategory = findViewById(R.id.btnAddCustomCategory)
        layoutCustomCategoriesContainer = findViewById(R.id.layoutCustomCategoriesContainer)

        loadExistingGoalAndCategories()

        btnAddCustomCategory.setOnClickListener {
            val categoryName = edtCustomCategoryName.text.toString().trim()
            val categoryBudget = edtCustomCategoryBudget.text.toString().trim().toDoubleOrNull() ?: 0.0

            if (categoryName.isEmpty()) {
                Toast.makeText(this, "Enter category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // Collect currently typed values before refreshing
                    val currentLimits = collectCategoryLimits().toMutableMap()

                    db.categoryDao().insertCategory(CategoryEntity(name = categoryName))
                    firebaseRepository.addCategory(categoryName)

                    Toast.makeText(
                        this@SetBudgetActivity,
                        "Category saved",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Add the new category's budget to our map so it shows up in the refreshed list
                    currentLimits[categoryName] = categoryBudget

                    edtCustomCategoryName.text?.clear()
                    edtCustomCategoryBudget.text?.clear()
                    
                    // Refresh with preserved values + new category value
                    loadCategoriesForLimits(currentLimits)

                } catch (e: Exception) {
                    Toast.makeText(
                        this@SetBudgetActivity,
                        "Could not save category: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        btnSaveBudget.setOnClickListener {
            saveBudgetGoal()
        }

        btnCancel.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_budget

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java)
                        .apply { addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) })
                    true
                }
                R.id.nav_budget -> true
                R.id.nav_reports -> {
                    startActivity(Intent(this, GraphActivity::class.java)
                        .apply { addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) })
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java)
                        .apply { addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) })
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.nav_budget
    }

    private fun getCurrentMonth(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        return LocalDate.now().format(formatter)
    }

    private fun loadExistingGoalAndCategories() {
        lifecycleScope.launch {
            try {
                val month = getCurrentMonth()
                val goal = firebaseRepository.getGoal(month)

                if (goal != null) {
                    edtBudgetAmount.setText(goal.maximumGoal.toString())
                    edtSavingsGoal.setText(goal.minimumGoal.toString())
                    loadCategoriesForLimits(goal.categoryLimits)
                } else {
                    loadCategoriesForLimits(emptyMap())
                }
            } catch (e: Exception) {
                loadCategoriesForLimits(emptyMap())
            }
        }
    }

    private fun loadCategoriesForLimits(initialValues: Map<String, Double>) {
        lifecycleScope.launch {
            try {
                firebaseCategories = firebaseRepository.getCategories()

                layoutCustomCategoriesContainer.removeAllViews()
                categoryLimitInputs.clear()

                for (category in firebaseCategories) {
                    val row = layoutInflater.inflate(
                        R.layout.item_category_limit,
                        layoutCustomCategoriesContainer,
                        false
                    )

                    val txtName = row.findViewById<TextView>(R.id.txtCategoryName)
                    val edtLimit = row.findViewById<EditText>(R.id.edtCategoryLimit)

                    txtName.text = category.name
                    categoryLimitInputs[category.id] = edtLimit

                    // Populate with initial value if available
                    val value = initialValues[category.name]
                    if (value != null && value > 0) {
                        edtLimit.setText("%.2f".format(value))
                    }

                    layoutCustomCategoriesContainer.addView(row)
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@SetBudgetActivity,
                    "Could not load categories: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun collectCategoryLimits(): Map<String, Double> {
        val limits = mutableMapOf<String, Double>()

        for (category in firebaseCategories) {
            val editText = categoryLimitInputs[category.id] ?: continue
            val text = editText.text.toString().trim()
            val value = text.toDoubleOrNull()

            if (value != null && value >= 0) {
                limits[category.name] = value
            }
        }

        return limits
    }

    private fun saveBudgetGoal() {
        val minGoalText = edtSavingsGoal.text.toString().trim()
        val maxGoalText = edtBudgetAmount.text.toString().trim()

        val minGoal = minGoalText.toDoubleOrNull()
        val maxGoal = maxGoalText.toDoubleOrNull()

        if (minGoal == null || minGoal < 0) {
            edtSavingsGoal.error = "Enter a valid minimum goal"
            Toast.makeText(this, "Please enter a valid savings goal", Toast.LENGTH_SHORT).show()
            return
        }

        if (maxGoal == null || maxGoal <= 0) {
            edtBudgetAmount.error = "Enter a valid maximum goal"
            Toast.makeText(this, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (minGoal > maxGoal) {
            Toast.makeText(this, "Minimum goal cannot be more than maximum goal", Toast.LENGTH_SHORT).show()
            return
        }

        val categoryLimits = collectCategoryLimits()

        val goal = FirebaseGoal(
            id = getCurrentMonth(),
            userId = firebaseRepository.getCurrentUserId() ?: "",
            month = getCurrentMonth(),
            minimumGoal = minGoal,
            maximumGoal = maxGoal,
            totalBudgetGoal = maxGoal,
            categoryLimits = categoryLimits,
            createdAt = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            try {
                btnSaveBudget.isEnabled = false
                btnSaveBudget.text = "Saving..."
                
                firebaseRepository.saveGoal(goal)

                Toast.makeText(
                    this@SetBudgetActivity,
                    "Budget goal saved successfully",
                    Toast.LENGTH_SHORT
                ).show()

                // Go back to Dashboard
                val intent = Intent(this@SetBudgetActivity, DashboardActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                btnSaveBudget.isEnabled = true
                btnSaveBudget.text = "Save Budget"

                Toast.makeText(
                    this@SetBudgetActivity,
                    "Could not save goal: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
