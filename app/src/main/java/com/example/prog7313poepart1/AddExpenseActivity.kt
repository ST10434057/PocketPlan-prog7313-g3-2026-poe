package com.example.prog7313poepart1

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.prog7313poepart1.data.firebase.FirebaseCategory
import com.example.prog7313poepart1.data.firebase.FirebaseExpense
import com.example.prog7313poepart1.data.firebase.FirebaseRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var spinnerCategory: Spinner
    private lateinit var edtExpenseTitle: TextInputEditText
    private lateinit var edtExpenseAmount: TextInputEditText
    private lateinit var edtExpenseDate: TextInputEditText
    private lateinit var edtExpenseTime: TextInputEditText
    private lateinit var edtExpenseNotes: TextInputEditText
    private lateinit var btnAddExpense: MaterialButton
    private lateinit var btnCancelExpense: MaterialButton
    private lateinit var imgExpensePhotoPreview: ImageView
    private lateinit var txtPhotoPath: TextView
    private lateinit var btnPickFromGallery: MaterialButton

    private lateinit var firebaseRepository: FirebaseRepository
    private var firebaseCategories: List<FirebaseCategory> = emptyList()
    private var selectedImageUri: Uri? = null
    private val calendar = Calendar.getInstance()

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                imgExpensePhotoPreview.visibility = View.VISIBLE
                imgExpensePhotoPreview.setImageURI(uri)
                txtPhotoPath.text = "Photo selected"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        spinnerCategory = findViewById(R.id.spinnerCategory)
        edtExpenseTitle = findViewById(R.id.edtExpenseTitle)
        edtExpenseAmount = findViewById(R.id.edtExpenseAmount)
        edtExpenseDate = findViewById(R.id.edtExpenseDate)
        edtExpenseTime = findViewById(R.id.edtExpenseTime)
        edtExpenseNotes = findViewById(R.id.edtExpenseNotes)
        btnAddExpense = findViewById(R.id.btnAddExpense)
        btnCancelExpense = findViewById(R.id.btnCancelExpense)
        imgExpensePhotoPreview = findViewById(R.id.imgExpensePhotoPreview)
        txtPhotoPath = findViewById(R.id.txtPhotoPath)
        btnPickFromGallery = findViewById(R.id.btnPickFromGallery)

        firebaseRepository = FirebaseRepository(this)

        loadCategoriesFromFirebase()

        edtExpenseDate.setOnClickListener { showDatePicker() }
        edtExpenseTime.setOnClickListener { showTimePicker() }
        btnPickFromGallery.setOnClickListener { pickImageLauncher.launch("image/*") }

        btnAddExpense.setOnClickListener {
            if (firebaseCategories.isEmpty()) {
                Toast.makeText(this, "Please create a category first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (spinnerCategory.selectedItemPosition < 0) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveExpense()
        }

        btnCancelExpense.setOnClickListener { finish() }
    }

    private fun loadCategoriesFromFirebase() {
        lifecycleScope.launch {
            try {
                firebaseCategories = firebaseRepository.getCategories()

                if (firebaseCategories.isEmpty()) {
                    Toast.makeText(
                        this@AddExpenseActivity,
                        "No categories found. Please add one first.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                val names = firebaseCategories.map { it.name }
                val adapter = ArrayAdapter(
                    this@AddExpenseActivity,
                    android.R.layout.simple_spinner_item,
                    names
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategory.adapter = adapter

            } catch (e: Exception) {
                Toast.makeText(
                    this@AddExpenseActivity,
                    "Could not load categories: ${e.localizedMessage ?: e.toString()}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                edtExpenseDate.setText(format.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                edtExpenseTime.setText(format.format(calendar.time))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun saveExpense() {
        val title = edtExpenseTitle.text.toString().trim()
        val amountStr = edtExpenseAmount.text.toString().trim()
        val date = edtExpenseDate.text.toString().trim()
        val time = edtExpenseTime.text.toString().trim()
        val notes = edtExpenseNotes.text.toString().trim()

        if (title.isEmpty()) {
            edtExpenseTitle.error = "Enter expense title"
            return
        }

        if (amountStr.isEmpty()) {
            edtExpenseAmount.error = "Enter amount"
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            edtExpenseAmount.error = "Enter a valid amount"
            return
        }

        if (date.isEmpty()) {
            edtExpenseDate.error = "Select a date"
            return
        }

        if (time.isEmpty()) {
            edtExpenseTime.error = "Select a time"
            return
        }

        val selectedCategory = firebaseCategories[spinnerCategory.selectedItemPosition]

        val expense = FirebaseExpense(
            id = "",
            userId = firebaseRepository.getCurrentUserId() ?: "",
            title = title,
            amount = amount,
            categoryId = selectedCategory.id,
            categoryName = selectedCategory.name,
            date = date,
            time = time,
            description = notes,
            receiptImageUrl = "",
            createdAt = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            try {
                firebaseRepository.addExpense(expense, selectedImageUri)

                Toast.makeText(
                    this@AddExpenseActivity,
                    "Expense saved online",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            } catch (e: Exception) {
                val detail = e.message ?: e.javaClass.simpleName
                android.util.Log.e("AddExpense", "Save failed", e)
                Toast.makeText(
                    this@AddExpenseActivity,
                    "Could not save expense: $detail",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
