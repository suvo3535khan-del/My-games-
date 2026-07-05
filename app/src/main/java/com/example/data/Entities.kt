package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val username: String = "Cognitive Explorer",
    val totalXp: Int = 0,
    val level: Int = 1,
    val coins: Int = 100, // Starts with 100 coins
    val dailyStreak: Int = 0,
    val lastActiveTimestamp: Long = 0L,
    val activeTheme: String = "Sophisticated Dark",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val memoryLevel: Int = 1,
    val mathLevel: Int = 1,
    val patternLevel: Int = 1,
    val reactionLevel: Int = 1,
    val memoryHighScore: Int = 0,
    val mathHighScore: Int = 0,
    val patternHighScore: Int = 0,
    val reactionHighScore: Int = 0,
    val hintsCount: Int = 3,
    val dailyChallengeCompletedDate: String = "" // Formatted "YYYY-MM-DD"
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val unlockedTime: Long = 0L,
    val targetValue: Int = 1,
    val currentValue: Int = 0,
    val iconName: String
)
