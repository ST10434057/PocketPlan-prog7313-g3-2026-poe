package com.example.prog7313poepart1

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.prog7313poepart1.data.firebase.FirebaseCategoryTotal
import com.example.prog7313poepart1.data.firebase.FirebaseExpense
import com.example.prog7313poepart1.data.firebase.FirebaseRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DashboardActivity : AppCompatActivity() {

    private lateinit var firebaseRepository: FirebaseRepository

    private lateinit var txtTotalSpent: TextView
    private lateinit var txtBudget: TextView
    private lateinit var txtRemaining: TextView
    private lateinit var txtGoalStatus: TextView
    private lateinit var txtWarnings: TextView
    private lateinit var progressBudget: ProgressBar
    private lateinit var layoutRecentExpenses: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        firebaseRepository = FirebaseRepository(this)

        txtTotalSpent = findViewById(R.id.txtTotalSpent)
        txtBudget = findViewById(R.id.txtBudget)
        txtRemaining = findViewById(R.id.txtRemaining)
        txtGoalStatus = findViewById(R.id.txtGoalStatus)
        txtWarnings = findViewById(R.id.txtWarnings)
        progressBudget = findViewById(R.id.progressBudget)
        layoutRecentExpenses = findViewById(R.id.layoutRecentExpenses)

        findViewById<MaterialButton>(R.id.btnAddExpense).setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        findViewById<TextView>(R.id.txtSeeAll).setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        loadDashboard()
        setupBottomNav()
    }

    // ⭐ FIX: Refresh dashboard when returning from AddExpenseActivity
    override fun onResume() {
        super.onResume()
        loadDashboard()
        findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.nav_home
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true

                R.id.nav_budget -> {
                    startActivity(Intent(this, SetBudgetActivity::class.java)
                        .apply { addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) })
                    true
                }

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

    private fun getCurrentMonth(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        return LocalDate.now().format(formatter)
    }

    private fun loadDashboard() {
        val month = getCurrentMonth()
        val startDate = "$month-01"
        val endDate = "$month-31"

        lifecycleScope.launch {
            try {
                val goal = firebaseRepository.getGoal(month)
                val expenses = firebaseRepository.getExpensesBetween(startDate, endDate)
                val categoryTotals = firebaseRepository.getCategoryTotals(startDate, endDate)

                val totalSpent = expenses.sumOf { it.amount }
                val maxGoal = goal?.maximumGoal ?: 0.0
                val minGoal = goal?.minimumGoal ?: 0.0
                val totalBudget = goal?.totalBudgetGoal ?: maxGoal

                txtTotalSpent.text = "R %.2f".format(totalSpent)
                txtBudget.text = "R %.2f".format(totalBudget)
                txtRemaining.text = "R %.2f".format(totalBudget - totalSpent)

                val progress = if (totalBudget > 0) {
                    ((totalSpent / totalBudget) * 100).toInt()
                } else 0

                progressBudget.progress = progress.coerceIn(0, 100)

                txtGoalStatus.text = when {
                    goal == null -> "No goal set for this month"
                    totalSpent < minGoal -> "You are below your minimum spending goal"
                    totalSpent <= maxGoal -> "You are within your spending goal"
                    else -> "You are over your maximum spending goal"
                }

                showCategoryWarnings(goal?.categoryLimits ?: emptyMap(), categoryTotals)
                showRecentExpenses(expenses)

            } catch (e: Exception) {
                val msg = if (e.message?.contains("offline") == true || e.message?.contains("UNAVAILABLE") == true)
                    "You're offline. Connect to the internet to load your dashboard."
                else
                    "Could not load dashboard. Please try again."
                Toast.makeText(this@DashboardActivity, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showRecentExpenses(expenses: List<FirebaseExpense>) {
        layoutRecentExpenses.removeAllViews()

        val recent = expenses.take(5)

        if (recent.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No expenses this month yet"
                setTextColor(Color.parseColor("#888888"))
                textSize = 14f
            }
            layoutRecentExpenses.addView(empty)
            return
        }

        recent.forEachIndexed { index, expense ->
            // Divider between rows (not before the first)
            if (index > 0) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    ).also { it.topMargin = dpToPx(10); it.bottomMargin = dpToPx(10) }
                    setBackgroundColor(Color.parseColor("#E0E0E0"))
                }
                layoutRecentExpenses.addView(divider)
            }

            // Row: title + amount side by side
            val rowTop = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val txtTitle = TextView(this).apply {
                text = expense.title
                setTextColor(Color.BLACK)
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val txtAmount = TextView(this).apply {
                text = "-R %.2f".format(expense.amount)
                setTextColor(Color.parseColor("#C62828"))
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            rowTop.addView(txtTitle)
            rowTop.addView(txtAmount)
            layoutRecentExpenses.addView(rowTop)

            // Category • date subtitle
            val txtMeta = TextView(this).apply {
                text = "${expense.categoryName}  •  ${expense.date}"
                setTextColor(Color.parseColor("#666666"))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = dpToPx(2) }
            }
            layoutRecentExpenses.addView(txtMeta)
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun showCategoryWarnings(
        categoryLimits: Map<String, Double>,
        categoryTotals: List<FirebaseCategoryTotal>
    ) {
        val warnings = StringBuilder()

        for (categoryTotal in categoryTotals) {
            val limit = categoryLimits[categoryTotal.categoryName]

            if (limit != null && categoryTotal.total > limit) {
                val overAmount = categoryTotal.total - limit
                warnings.append(
                    "${categoryTotal.categoryName} is over budget by R %.2f\n"
                        .format(overAmount)
                )
            }
        }

        txtWarnings.text = if (warnings.isEmpty()) {
            "No overspending detected"
        } else warnings.toString()
    }
}
