package com.example.prog7313poepart1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.prog7313poepart1.data.DatabaseProvider
import com.example.prog7313poepart1.data.firebase.FirebaseRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        firebaseRepository = FirebaseRepository(this)
        sessionManager = SessionManager(this)

        loadUserProfile()
        loadRewardSummary()

        findViewById<SwitchMaterial>(R.id.switchNightMode).setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(
                this,
                if (isChecked) "Night mode demo enabled" else "Night mode demo disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<MaterialButton>(R.id.btnViewRewards).setOnClickListener {
            startActivity(Intent(this, RewardsActivity::class.java))
        }

        findViewById<Button>(R.id.btnSignOut).setOnClickListener {
            // Sign out from Firebase
            firebaseRepository.logout()
            
            // Clear local session data
            sessionManager.clearSession()
            
            // Redirect to MainActivity (Login) and clear activity stack
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.nav_profile
    }

    private fun loadUserProfile() {
        val db = DatabaseProvider.getDatabase(this)
        val userId = SessionManager(this).getUserId()

        lifecycleScope.launch {
            val user = db.userDao().getUserById(userId)
            val firebaseEmail = firebaseRepository.getCurrentUserEmail()

            runOnUiThread {
                val email = firebaseEmail ?: user?.email
                if (email != null) {
                    replaceText("Nicholas Kelly", email)
                    replaceText("nicholas@example.com", email)
                }
            }
        }
    }

    private fun loadRewardSummary() {
        lifecycleScope.launch {
            try {
                val rewards = firebaseRepository.getRewards()
                findViewById<TextView>(R.id.txtRewardsProfileSummary).text =
                    "${rewards.count { it.unlocked }} of ${rewards.size} rewards unlocked"
            } catch (e: Exception) {
                findViewById<TextView>(R.id.txtRewardsProfileSummary).text =
                    "Open rewards to start tracking badges"
            }
        }
    }

    private fun replaceText(oldText: String, newText: String) {
        replaceTextRecursive(window.decorView.rootView, oldText, newText)
    }

    private fun replaceTextRecursive(view: View, oldText: String, newText: String) {
        if (view is TextView && view.text.toString() == oldText) {
            view.text = newText
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                replaceTextRecursive(view.getChildAt(i), oldText, newText)
            }
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    true
                }
                R.id.nav_budget -> {
                    startActivity(Intent(this, SetBudgetActivity::class.java))
                    true
                }
                R.id.nav_reports -> {
                    startActivity(Intent(this, GraphActivity::class.java))
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }
}
