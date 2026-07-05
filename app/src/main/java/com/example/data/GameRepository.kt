package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val profileDao: ProfileDao) {
    val userProfileFlow: Flow<UserProfile?> = profileDao.getUserProfileFlow()
    val achievementsFlow: Flow<List<Achievement>> = profileDao.getAchievementsFlow()

    suspend fun getProfile(): UserProfile? = profileDao.getUserProfile()

    suspend fun updateProfile(profile: UserProfile) {
        profileDao.updateProfile(profile)
    }

    suspend fun insertProfile(profile: UserProfile) {
        profileDao.insertProfile(profile)
    }

    suspend fun getAchievements(): List<Achievement> = profileDao.getAchievements()

    suspend fun insertAchievements(achievements: List<Achievement>) {
        profileDao.insertAchievements(achievements)
    }

    suspend fun updateAchievement(achievement: Achievement) {
        profileDao.updateAchievement(achievement)
    }
}
