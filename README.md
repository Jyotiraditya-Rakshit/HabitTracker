# 🧠 Habit Tracker App

A clean and modern Android application that helps you build positive habits, track daily progress, and stay consistent.  
This app allows you to create multiple habits with custom icons, select date ranges, mark daily progress, and receive a completion screen once a habit is fully achieved.  
Data is synced using **Firestore** for habits and **Realtime Database** for progress tracking.

---

# 🛠️ How It Works (Step-by-Step)

This section shows how to use the Habit Tracker App with short visual GIF previews.

---

## 1️⃣ Add a New Habit

- Tap **Add Habit**
- Enter habit name & description  
- Select start & end date  
- Choose an icon  
- Press **Add Habit**

<p align="center">
  <img src="assets/add_habit.gif" width="300"/>
</p>

---

## 2️⃣ Mark a Habit as Done

- Open your habit list  
- Tap the **Done** button  
- Progress increases automatically  
- App prevents marking twice on the same day  

<p align="center">
  <img src="assets/mark_habit.gif" width="300"/>
</p>

---

## 3️⃣ Completing a Habit

- When progress reaches the total days  
- A celebration screen is shown  
- The habit is moved out of the active list  

<p align="center">
  <img src="assets/finish_habit.gif" width="300"/>
</p>

---


## 🎯 Features

- ✨ Create your own habits with name, description & category icon  
- 📅 Choose start and end dates (auto-calculates total duration)  
- ✔ Mark daily progress once per day  
- 🔁 Prevents duplicate marking (“Already marked for today”)  
- 📊 Animated progress indicator  
- 🔥 Completion screen when habit finishes  
- 📝 Edit or delete any habit  
- ⚡ Real-time syncing with Firebase  
- 🧩 Separate Firestore (habits) & Realtime Database (progress) storage  
- 🎨 Beautiful Material UI with smooth transitions  

---

## 🗄 Database Structure

### **1️⃣ Firestore — stores habit details**

