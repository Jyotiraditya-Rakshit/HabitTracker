package com.example.habittracker.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.habittracker.adapter.HabitsAdapter
import com.example.habittracker.R
import com.example.habittracker.databinding.ActivityMainBinding
import com.example.habittracker.model.Habit
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

import java.time.LocalDate


class MainActivity : AppCompatActivity() {

    private lateinit var habitsAdapter: HabitsAdapter
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: FirebaseFirestore

    private val habitIntent by lazy { Intent(this, HabitActivity::class.java) }
    private val completedIntent by lazy { Intent(this, CompletedStreakActivity::class.java) }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()

        binding.fab.setOnClickListener {
            try {
                startActivity(habitIntent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error launching HabitActivity", e)
                Toast.makeText(this, "Failed to open Habit screen", Toast.LENGTH_SHORT).show()
            }
        }

        fetchData()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchData() {
        try {
            val collection = db.collection("habits")

            collection.get(Source.CACHE)
                .addOnSuccessListener { snapshot ->
                    try {
                        if (!snapshot.isEmpty) {
                            val cachedList = snapshot.documents.mapNotNull { it.toObject(Habit::class.java) }
                            updateRecycler(cachedList)
                            Log.d("FirestoreCache", "Loaded from cache: ${cachedList.size}")
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error parsing cached habits", e)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error fetching cached habits", e)
                }

            collection.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirestoreError", "Error loading habits", e)
                    return@addSnapshotListener
                }

                try {
                    val habitList = snapshot?.documents?.mapNotNull { it.toObject(Habit::class.java) } ?: emptyList()
                    if (habitList.isNotEmpty()) {
                        updateRecycler(habitList)
                        binding.recyclerView.visibility = View.VISIBLE
                        binding.addHabitText.visibility = View.GONE
                    } else {
                        binding.recyclerView.visibility = View.GONE
                        binding.addHabitText.visibility = View.VISIBLE
                    }
                } catch (ex: Exception) {
                    Log.e("MainActivity", "Error updating habit list", ex)
                }
            }
        } catch (ex: Exception) {
            Log.e("MainActivity", "Failed to fetch habits", ex)
            Toast.makeText(this, "Unable to load habits", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateRecycler(habits: List<Habit>) {
        try {
            habitsAdapter = HabitsAdapter(
                habits.toMutableList(),
                onMarkDone = { habit -> markDone(habit) },
                onDeleteBtn = { habit -> deleteHabit(habit) },
                onEditBtn = { habit -> editHabit(habit) }
            )
            binding.recyclerView.adapter = habitsAdapter
        } catch (ex: Exception) {
            Log.e("MainActivity", "Error setting up RecyclerView adapter", ex)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun markDone(habit: Habit) {
        try {
            val realTimeRef = FirebaseDatabase.getInstance()
                .getReference("habit_progress")
                .child(habit.habitID)

            realTimeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val today = LocalDate.now().toString()
                        val lastMarkedDate = snapshot.child("lastMarkedDate").getValue(String::class.java)
                        val currentProgress = snapshot.child("currentProgress").getValue(Int::class.java) ?: 0
                        val totalDays = snapshot.child("totalDays").getValue(Int::class.java) ?: 0
                        var isCompleted = snapshot.child("completed").getValue(Boolean::class.java) ?: false

                        if (lastMarkedDate == today) {
                            Toast.makeText(this@MainActivity, "Already marked for today!", Toast.LENGTH_SHORT).show()
                            return
                        }

                        val newProgress = currentProgress + 1
                        realTimeRef.child("currentProgress").setValue(newProgress)
                        realTimeRef.child("lastMarkedDate").setValue(today)

                        if (newProgress >= totalDays) {
                            isCompleted = true
                            realTimeRef.child("completed").setValue(true)
                            startActivity(completedIntent)
                            deleteHabitFromList(habit)
                        }

                        db.collection("habits")
                            .document(habit.habitID)
                            .update(
                                mapOf(
                                    "completed" to isCompleted,
                                    "currentProgress" to newProgress
                                )
                            )
                    } catch (ex: Exception) {
                        Log.e("MainActivity", "Error updating progress for habit", ex)
                        Toast.makeText(this@MainActivity, "Failed to update habit", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainActivity", "Error writing to database: ${error.message}")
                    Toast.makeText(this@MainActivity, "Database error occurred", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (ex: Exception) {
            Log.e("MainActivity", "Failed to mark habit done", ex)
            Toast.makeText(this, "Unable to mark habit done", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteHabit(habit: Habit) {
        try {
            AlertDialog.Builder(this)
                .setTitle("Delete Habit")
                .setMessage("Are you sure you want to delete this habit?")
                .setPositiveButton("Delete") { _, _ ->
                    deleteHabitFromList(habit)
                }.setNegativeButton("Cancel", null)
                .show()
        } catch (ex: Exception) {
            Log.e("MainActivity", "Failed to show delete dialog", ex)
        }
    }

    private fun deleteHabitFromList(habit: Habit) {
        try {
            val realtime = FirebaseDatabase.getInstance().getReference("habit_progress").child(habit.habitID)
            db.collection("habits").document(habit.habitID)
                .delete()
                .addOnSuccessListener {
                    try {
                        realtime.removeValue().addOnCompleteListener {
                            Toast.makeText(this, "Habit deleted!", Toast.LENGTH_SHORT).show()
                        }

                        val position = habitsAdapter.listOfHabits.indexOf(habit)
                        if (position != -1) {
                            habitsAdapter.listOfHabits.removeAt(position)
                            habitsAdapter.notifyItemRemoved(position)
                        }
                    } catch (ex: Exception) {
                        Log.e("MainActivity", "Error updating UI after deletion", ex)
                    }
                }
                .addOnFailureListener {
                    Log.e("MainActivity", "Failed to delete habit", it)
                    Toast.makeText(this, "Failed to delete: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (ex: Exception) {
            Log.e("MainActivity", "Error deleting habit", ex)
            Toast.makeText(this, "Failed to delete habit", Toast.LENGTH_SHORT).show()
        }
    }

    private fun editHabit(habit: Habit) {
        try {
            val intent = Intent(this, HabitActivity::class.java)
            intent.putExtra("isEditMode", true)
            intent.putExtra("habitData", habit)
            startActivity(intent)
        } catch (ex: Exception) {
            Log.e("MainActivity", "Failed to open edit habit", ex)
            Toast.makeText(this, "Unable to edit habit", Toast.LENGTH_SHORT).show()
        }
    }
}
