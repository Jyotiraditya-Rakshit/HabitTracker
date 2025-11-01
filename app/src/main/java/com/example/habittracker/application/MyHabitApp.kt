package com.example.habittracker.application

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class MyHabitApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Firestore offline cache
        val firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings

        // Realtime Database offline cache
        val realtimeDB = FirebaseDatabase.getInstance()
        realtimeDB.setPersistenceEnabled(true)
        realtimeDB.getReference("habit_progress").keepSynced(true)
    }
}