package com.example.prog7313poepart1

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7313poepart1.data.firebase.FirebaseExpense
import com.example.prog7313poepart1.data.firebase.FirebaseRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var firebaseRepository: FirebaseRepository
    private var allExpenses: List<FirebaseExpense> = emptyList()

    private lateinit var edtStartDate: TextInputEditText
    private lateinit var edtEndDate: TextInputEditText
    private val filterCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        recycler = findViewById(R.id.recyclerTransactionHistory)
        recycler.layoutManager = LinearLayoutManager(this)

        edtStartDate = findViewById(R.id.edtStartDate)
        edtEndDate = findViewById(R.id.edtEndDate)

        firebaseRepository = FirebaseRepository(this)

        setupBottomNav()
        setupFilters()
        loadTransactions()
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
        findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.nav_reports
    }

    private fun loadTransactions() {
        lifecycleScope.launch {
            try {
                allExpenses = firebaseRepository.getExpenses()
                showExpenses(allExpenses)
                updateSummary(allExpenses)
            } catch (e: Exception) {
                Toast.makeText(
                    this@TransactionHistoryActivity,
                    "Could not load transactions: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showExpenses(expenses: List<FirebaseExpense>) {
        recycler.adapter = TransactionAdapter(expenses)
    }

    private fun updateSummary(expenses: List<FirebaseExpense>) {
        val total = expenses.sumOf { it.amount }
        val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().time)
        findViewById<TextView>(R.id.txtTransactionCount).text = expenses.size.toString()
        findViewById<TextView>(R.id.txtTotalSpent).text = "R %.2f".format(total)
        findViewById<TextView>(R.id.txtCurrentMonth).text = month
    }

    private fun setupFilters() {
        findViewById<MaterialButton>(R.id.btnAll).setOnClickListener {
            edtStartDate.text?.clear()
            edtEndDate.text?.clear()
            showExpenses(allExpenses)
            updateSummary(allExpenses)
        }

        findViewById<MaterialButton>(R.id.btnFood).setOnClickListener {
            filterByCategory("Food")
        }

        findViewById<MaterialButton>(R.id.btnTransport).setOnClickListener {
            filterByCategory("Transport")
        }

        findViewById<MaterialButton>(R.id.btnBooks).setOnClickListener {
            filterByCategory("Book/Study")
        }

        edtStartDate.setOnClickListener { showDatePicker(edtStartDate) }
        edtEndDate.setOnClickListener { showDatePicker(edtEndDate) }

        findViewById<MaterialButton>(R.id.btnApplyDateFilter).setOnClickListener {
            val start = edtStartDate.text.toString().trim()
            val end = edtEndDate.text.toString().trim()

            if (start.isEmpty() || end.isEmpty()) {
                Toast.makeText(this, "Select both dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val filtered = firebaseRepository.getExpensesBetween(start, end)
                    showExpenses(filtered)
                    updateSummary(filtered)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@TransactionHistoryActivity,
                        "Could not filter: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        findViewById<TextInputEditText>(R.id.edtSearchTransaction)
            .setOnEditorActionListener { v, _, _ ->
                val search = v.text.toString().lowercase()
                val filtered = allExpenses.filter {
                    it.title.lowercase().contains(search) ||
                            it.description.lowercase().contains(search)
                }
                showExpenses(filtered)
                updateSummary(filtered)
                true
            }
    }

    private fun filterByCategory(name: String) {
        val filtered = allExpenses.filter { it.categoryName.equals(name, ignoreCase = true) }
        showExpenses(filtered)
        updateSummary(filtered)
    }

    private fun showDatePicker(field: TextInputEditText) {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                filterCalendar.set(year, month, dayOfMonth)
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                field.setText(fmt.format(filterCalendar.time))
            },
            filterCalendar.get(Calendar.YEAR),
            filterCalendar.get(Calendar.MONTH),
            filterCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_reports

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    true
                }
                R.id.nav_budget -> {
                    startActivity(Intent(this, SetBudgetActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    true
                }
                R.id.nav_reports -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    true
                }
                else -> false
            }
        }
    }

    inner class TransactionAdapter(
        private val expenses: List<FirebaseExpense>
    ) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

        inner class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val imgIcon: android.widget.ImageView = view.findViewById(R.id.imgTransactionIcon)
            val txtTitle: TextView = view.findViewById(R.id.txtTransactionTitle)
            val txtCategory: TextView = view.findViewById(R.id.txtTransactionCategory)
            val txtDate: TextView = view.findViewById(R.id.txtTransactionDate)
            val txtAmount: TextView = view.findViewById(R.id.txtTransactionAmount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction_history, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = expenses.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val expense = expenses[position]
            holder.txtTitle.text = expense.title
            holder.txtCategory.text = expense.categoryName
            holder.txtDate.text = "${expense.date} ${expense.time}"
            holder.txtAmount.text = "-R %.2f".format(expense.amount)
            holder.imgIcon.setImageResource(
                if (expense.receiptImageUrl.isNotEmpty()) android.R.drawable.ic_menu_camera
                else android.R.drawable.ic_menu_info_details
            )
        }
    }
}
