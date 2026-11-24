package com.example.prog7313poepart1

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7313poepart1.data.firebase.FirebaseRepository
import com.example.prog7313poepart1.data.firebase.FirebaseReward
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class RewardsActivity : AppCompatActivity() {

    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var rewardsAdapter: RewardsAdapter
    private lateinit var txtRewardSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewards)

        firebaseRepository = FirebaseRepository(this)
        txtRewardSummary = findViewById(R.id.txtRewardSummary)
        rewardsAdapter = RewardsAdapter()

        findViewById<RecyclerView>(R.id.recyclerRewards).apply {
            layoutManager = LinearLayoutManager(this@RewardsActivity)
            adapter = rewardsAdapter
        }

        setupBottomNav()
        createDefaultRewards()
        updateRewards()
    }

    override fun onResume() {
        super.onResume()
        updateRewards()
        findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.nav_profile
    }

    private fun createDefaultRewards() {
        lifecycleScope.launch {
            try {
                if (firebaseRepository.getRewards().isNotEmpty()) {
                    return@launch
                }

                val rewards = listOf(
                    FirebaseReward(
                        id = "budget_beginner",
                        name = "Budget Beginner",
                        description = "Add your first expense",
                        unlocked = false,
                        progress = 0
                    ),
                    FirebaseReward(
                        id = "smart_saver",
                        name = "Smart Saver",
                        description = "Stay under your maximum monthly budget",
                        unlocked = false,
                        progress = 0
                    ),
                    FirebaseReward(
                        id = "consistent_logger",
                        name = "Consistent Logger",
                        description = "Log expenses on 3 different days",
                        unlocked = false,
                        progress = 0
                    )
                )

                for (reward in rewards) {
                    firebaseRepository.saveReward(reward)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@RewardsActivity,
                    "Could not create rewards: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateRewards() {
        val month = getCurrentMonth()
        val startDate = "$month-01"
        val endDate = getMonthEndDate()

        lifecycleScope.launch {
            try {
                val expenses = firebaseRepository.getExpensesBetween(startDate, endDate)
                val goal = firebaseRepository.getGoal(month)
                val totalSpent = expenses.sumOf { it.amount }
                val differentDays = expenses.map { it.date }.distinct().size

                firebaseRepository.saveReward(
                    FirebaseReward(
                        id = "budget_beginner",
                        name = "Budget Beginner",
                        description = "Add your first expense",
                        unlocked = expenses.isNotEmpty(),
                        progress = if (expenses.isNotEmpty()) 100 else 0
                    )
                )

                val smartSaverUnlocked = goal != null &&
                        totalSpent <= goal.maximumGoal &&
                        expenses.isNotEmpty()
                val smartSaverProgress = when {
                    smartSaverUnlocked -> 100
                    expenses.isEmpty() -> 0
                    goal == null || goal.maximumGoal <= 0.0 -> 0
                    else -> ((1.0 - (totalSpent / goal.maximumGoal)) * 100).roundToInt()
                        .coerceIn(0, 99)
                }

                firebaseRepository.saveReward(
                    FirebaseReward(
                        id = "smart_saver",
                        name = "Smart Saver",
                        description = "Stay under your maximum monthly budget",
                        unlocked = smartSaverUnlocked,
                        progress = smartSaverProgress
                    )
                )

                firebaseRepository.saveReward(
                    FirebaseReward(
                        id = "consistent_logger",
                        name = "Consistent Logger",
                        description = "Log expenses on 3 different days",
                        unlocked = differentDays >= 3,
                        progress = ((differentDays / 3.0) * 100).toInt().coerceIn(0, 100)
                    )
                )

                loadRewards()
            } catch (e: Exception) {
                Toast.makeText(
                    this@RewardsActivity,
                    "Could not update rewards: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadRewards() {
        lifecycleScope.launch {
            try {
                val rewards = firebaseRepository.getRewards().sortedBy { it.name }
                rewardsAdapter.submitList(rewards)
                txtRewardSummary.text = "${rewards.count { it.unlocked }} of ${rewards.size} rewards unlocked"
            } catch (e: Exception) {
                Toast.makeText(
                    this@RewardsActivity,
                    "Could not load rewards: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getCurrentMonth(): String {
        return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Calendar.getInstance().time)
    }

    private fun getMonthEndDate(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile

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
                R.id.nav_reports -> {
                    startActivity(Intent(this, GraphActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    class RewardsAdapter : RecyclerView.Adapter<RewardsAdapter.ViewHolder>() {

        private val rewards = mutableListOf<FirebaseReward>()

        fun submitList(items: List<FirebaseReward>) {
            rewards.clear()
            rewards.addAll(items)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_reward, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = rewards.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(rewards[position])
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val txtRewardName: TextView = view.findViewById(R.id.txtRewardName)
            private val txtRewardDescription: TextView = view.findViewById(R.id.txtRewardDescription)
            private val txtRewardStatus: TextView = view.findViewById(R.id.txtRewardStatus)
            private val progressReward: ProgressBar = view.findViewById(R.id.progressReward)

            fun bind(reward: FirebaseReward) {
                txtRewardName.text = reward.name
                txtRewardDescription.text = reward.description
                txtRewardStatus.text = if (reward.unlocked) {
                    "Unlocked"
                } else {
                    "${reward.progress.coerceIn(0, 100)}% complete"
                }
                progressReward.progress = reward.progress.coerceIn(0, 100)
            }
        }
    }
}
