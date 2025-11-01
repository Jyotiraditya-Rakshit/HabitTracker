package com.example.habittracker.activities


import android.R
import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast

import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.habittracker.databinding.ActivityHabitBinding
import com.example.habittracker.model.Habit
import com.google.firebase.Firebase
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.collections.iterator
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid





class HabitActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null
    private var selectedIconName: String? = null
    private lateinit var binding: ActivityHabitBinding

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHabitBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        db = FirebaseFirestore.getInstance()

        try {
            setupCategorySelection()
            binding.startDateEditText.setOnClickListener { showDatePicker(true) }
            binding.endDateEditText.setOnClickListener { showDatePicker(false) }
            binding.fabAdd.setOnClickListener { saveHabit() }

            val isEditMode = intent.getBooleanExtra("isEditMode", false)
            val existingHabit = intent.getSerializableExtra("habitData") as? Habit
            if (isEditMode && existingHabit != null) {
                populateFields(existingHabit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error initializing Habit screen", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun populateFields(habit: Habit) {
        try {
            binding.habitNameEditText.setText(habit.habitName)
            binding.descriptionEditText.setText(habit.description)
            binding.startDateEditText.setText(habit.startDate)
            binding.endDateEditText.setText(habit.endDate)
            binding.duration.text = "Duration: ${habit.totalDays} days"
            binding.fabAdd.text = "Save"
            selectedIconName = habit.iconName
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupCategorySelection() {
        try {
            val imageViews = mapOf(
                binding.drinking to "champagne",
                binding.meditation to "harmony",
                binding.noSmoking to "no_smoking",
                binding.readingBooks to "reading_book",
                binding.defaultIcon to "_3d_target"
            )

            var selectedImageView: ImageView? = null

            for ((img, iconName) in imageViews) {
                img.setOnClickListener {
                    try {
                        selectedImageView?.setBackgroundResource(com.example.habittracker.R.drawable.image_not_selected)
                        img.setBackgroundResource(com.example.habittracker.R.drawable.image_selected)
                        selectedIconName = iconName
                        selectedImageView = img
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePicker(isStart: Boolean) {
        try {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    try {
                        val date = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                        if (isStart) {
                            startDate = date
                            binding.startDateEditText.setText(date.toString())
                        } else {
                            endDate = date
                            binding.endDateEditText.setText(date.toString())
                        }

                        if (startDate != null && endDate != null) {
                            val days = ChronoUnit.DAYS.between(startDate, endDate).toInt()
                            binding.duration.text = if (days >= 0) "Duration: $days days" else "Invalid range"
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "Invalid date selected", Toast.LENGTH_SHORT).show()
                    }
                },
                year,
                month,
                day
            )
            datePicker.show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to open date picker", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveHabit() {
        try {
            val habitName = binding.habitNameEditText.text.toString()
            val habitDescription = binding.descriptionEditText.text.toString()

            if (habitName.isEmpty()) {
                binding.habitNameInputLayout.error = "Enter Habit Name"
                return
            } else binding.habitNameInputLayout.error = null

            if (startDate == null || endDate == null) {
                binding.duration.text = "Please Select both Dates"
                return
            }

            if (habitDescription.isEmpty()) {
                binding.descriptionLayout.error = "Enter Habit Description"
                return
            }

            val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt()
            if (totalDays < 0) {
                binding.duration.text = "End date must be after start date"
                return
            }

            if (selectedIconName == null) {
                Toast.makeText(this, "Please select a category icon", Toast.LENGTH_SHORT).show()
                return
            }

            storeFireBase(habitName, totalDays, habitDescription, selectedIconName!!)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save habit", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalUuidApi::class)
    private fun storeFireBase(habitName: String, totalDays: Int, description: String, iconName: String) {
        try {
            val isEditMode = intent.getBooleanExtra("isEditMode", false)
            val existingHabit = intent.getSerializableExtra("habitData") as? Habit
            val userID = if (isEditMode && existingHabit != null) existingHabit.habitID else Uuid.random().toString()
            val habit = Habit(
                habitID = userID,
                iconName = iconName,
                habitName = habitName,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                totalDays = totalDays,
                description = description
            )

            db.collection("habits").document(userID)
                .set(habit)
                .addOnCompleteListener {
                    try {
                        val realtimeRef = FirebaseDatabase.getInstance().getReference("habit_progress")
                        if (!isEditMode) {
                            realtimeRef.child(userID).setValue(
                                mapOf(
                                    "habitName" to habit.habitName,
                                    "currentProgress" to habit.currentProgress,
                                    "totalDays" to totalDays,
                                    "completed" to false,
                                    "lastMarkedDate" to ""
                                )
                            )
                        } else {
                            realtimeRef.child(userID).child("totalDays").setValue(totalDays)
                        }

                        Toast.makeText(this, "Habit Saved Successfully", Toast.LENGTH_LONG).show()
                        finish()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "Error saving progress data", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save habit: ${it.message}", Toast.LENGTH_LONG).show()
                    it.printStackTrace()
                }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save habit", Toast.LENGTH_SHORT).show()
        }
    }
}
