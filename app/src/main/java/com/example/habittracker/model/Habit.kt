package com.example.habittracker.model

import java.io.Serializable

data class Habit(
    var habitID : String ="",
    var iconName : String = " ",
    var habitName : String = "",
    var startDate : String = "",
    var endDate : String = "",
    var totalDays : Int = 0,
    var currentProgress : Int =0,
    var completed : Boolean = false,
    var description : String =""
) : Serializable