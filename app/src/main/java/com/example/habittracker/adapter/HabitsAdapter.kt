package com.example.habittracker.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.habittracker.R
import com.example.habittracker.databinding.CardViewLayoutBinding
import com.example.habittracker.model.Habit
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HabitsAdapter(
    var listOfHabits: MutableList<Habit>,
    private var onMarkDone: (Habit) -> Unit,
    private var onDeleteBtn: (Habit) -> Unit,
    private var onEditBtn: (Habit) -> Unit
) : RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

    inner class HabitViewHolder(val binding: CardViewLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val binding = CardViewLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HabitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = listOfHabits.getOrNull(position) ?: return
        val context = holder.binding.root.context

        try {
            holder.binding.habitText.text = habit.habitName
            holder.binding.startDates.text = "Start Date : ${habit.startDate}"
            holder.binding.endDates.text = "End Date : ${habit.endDate}"
            holder.binding.progressBar.max = habit.totalDays
            holder.binding.progressBar.progress = habit.currentProgress
            holder.binding.tvProgressCount.text = "${habit.currentProgress}/${habit.totalDays}"
            holder.binding.description.text = habit.description

            val resId = context.resources.getIdentifier(habit.iconName, "drawable", context.packageName)
            holder.binding.habitImage.setImageResource(if (resId != 0) resId else R.drawable._3d_target)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error loading habit: ${habit.habitName}", Toast.LENGTH_SHORT).show()
        }

        // Firebase realtime progress updates
        try {
            val dbRef = FirebaseDatabase.getInstance().getReference("habit_progress").child(habit.habitID)
            dbRef.keepSynced(true)
            dbRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val current = snapshot.child("currentProgress").getValue(Int::class.java) ?: 0
                        val total = snapshot.child("totalDays").getValue(Int::class.java) ?: 0
                        holder.binding.progressBar.max = total
                        holder.binding.progressBar.progress = current
                        holder.binding.tvProgressCount.text = "$current/$total"
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("HabitsAdapter", "Error updating progress bar for ${habit.habitName}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HabitsAdapter", "Database error: ${error.message}")
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Button click listeners
        holder.binding.doneBtn.setOnClickListener {  onMarkDone(habit)  }
        holder.binding.deleteBtn.setOnClickListener { onDeleteBtn(habit)  }
        holder.binding.edit.setOnClickListener {  onEditBtn(habit) }
    }

    override fun getItemCount(): Int = listOfHabits.size


}
