package com.example.prog7313poepart1

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.prog7313poepart1.data.firebase.FirebaseRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GraphActivity : AppCompatActivity() {

    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var categoryChart: SpendingBarChartView
    private lateinit var dailyChart: SpendingBarChartView
    private lateinit var edtStartDate: TextInputEditText
    private lateinit var edtEndDate: TextInputEditText
    private lateinit var txtPeriod: TextView
    private lateinit var txtMinGoal: TextView
    private lateinit var txtMaxGoal: TextView

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val pickerCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        firebaseRepository = FirebaseRepository(this)

        categoryChart = findViewById(R.id.categoryChart)
        dailyChart = findViewById(R.id.dailyChart)
        edtStartDate = findViewById(R.id.edtGraphStartDate)
        edtEndDate = findViewById(R.id.edtGraphEndDate)
        txtPeriod = findViewById(R.id.txtGraphPeriod)
        txtMinGoal = findViewById(R.id.txtMinGoal)
        txtMaxGoal = findViewById(R.id.txtMaxGoal)

        setupPeriodControls()
        setupBottomNav()
        loadCurrentMonth()
    }

    override fun onResume() {
        super.onResume()
        val start = edtStartDate.text.toString()
        val end = edtEndDate.text.toString()
        if (start.isNotBlank() && end.isNotBlank()) {
            loadGraphs(start, end)
        }
        findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.nav_reports
    }

    private fun setupPeriodControls() {
        edtStartDate.setOnClickListener { showDatePicker(edtStartDate) }
        edtEndDate.setOnClickListener { showDatePicker(edtEndDate) }

        findViewById<MaterialButton>(R.id.btnThisMonth).setOnClickListener { loadCurrentMonth() }
        findViewById<MaterialButton>(R.id.btnLast7Days).setOnClickListener { loadRollingDays(7) }
        findViewById<MaterialButton>(R.id.btnLast30Days).setOnClickListener { loadRollingDays(30) }

        findViewById<MaterialButton>(R.id.btnApplyGraphDates).setOnClickListener {
            val start = edtStartDate.text.toString().trim()
            val end = edtEndDate.text.toString().trim()

            if (start.isEmpty() || end.isEmpty()) {
                Toast.makeText(this, "Select both dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadGraphs(start, end)
        }

        findViewById<MaterialButton>(R.id.btnViewTransactions).setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnOpenRewards).setOnClickListener {
            startActivity(Intent(this, RewardsActivity::class.java))
        }
    }

    private fun loadCurrentMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val start = dateFormat.format(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val end = dateFormat.format(calendar.time)

        loadGraphs(start, end)
    }

    private fun loadRollingDays(days: Int) {
        val end = Calendar.getInstance()
        val start = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }
        loadGraphs(dateFormat.format(start.time), dateFormat.format(end.time))
    }

    private fun showDatePicker(field: TextInputEditText) {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                pickerCalendar.set(year, month, dayOfMonth)
                field.setText(dateFormat.format(pickerCalendar.time))
            },
            pickerCalendar.get(Calendar.YEAR),
            pickerCalendar.get(Calendar.MONTH),
            pickerCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadGraphs(startDate: String, endDate: String) {
        edtStartDate.setText(startDate)
        edtEndDate.setText(endDate)
        txtPeriod.text = "$startDate to $endDate"

        lifecycleScope.launch {
            try {
                val month = startDate.take(7)

                val categoryTotals = firebaseRepository.getCategoryTotals(startDate, endDate)
                val expenses = firebaseRepository.getExpensesBetween(startDate, endDate)
                val goal = firebaseRepository.getGoal(month)

                // CATEGORY CHART
                val categoryItems = categoryTotals.map {
                    SpendingBarChartView.BarItem(it.categoryName, it.total)
                }

                // DAILY CHART
                val dailyItems = expenses
                    .groupBy { it.date }
                    .mapValues { entry -> entry.value.sumOf { expense -> expense.amount } }
                    .toSortedMap()
                    .map { (date, total) ->
                        SpendingBarChartView.BarItem(date.substringAfterLast("-"), total)
                    }

                val minimumGoal = goal?.minimumGoal
                val maximumGoal = goal?.maximumGoal

                categoryChart.setData(categoryItems, minimumGoal, maximumGoal)
                dailyChart.setData(dailyItems, minimumGoal, maximumGoal)

                if (goal != null) {
                    txtMinGoal.text = "Minimum goal: R %.2f".format(goal.minimumGoal)
                    txtMaxGoal.text = "Maximum goal: R %.2f".format(goal.maximumGoal)
                } else {
                    txtMinGoal.text = "Minimum goal: Not set"
                    txtMaxGoal.text = "Maximum goal: Not set"
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@GraphActivity,
                    "Could not load graph data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_reports

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java)
                        .apply { addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) })
                    true
                }

                R.id.nav_budget -> {
                    startActivity(Intent(this, SetBudgetActivity::class.java)
                        .apply { addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) })
                    true
                }

                R.id.nav_reports -> true

                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java)
                        .apply { addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) })
                    true
                }

                else -> false
            }
        }
    }
}
