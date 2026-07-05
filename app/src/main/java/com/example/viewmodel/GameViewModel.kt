package com.example.viewmodel

import android.app.Application
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.sound.SoundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = GameRepository(database.profileDao())

    val userProfile: StateFlow<UserProfile> = repository.userProfileFlow
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val achievements: StateFlow<List<Achievement>> = repository.achievementsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val soundManager = SoundManager { userProfile.value.soundEnabled }
    private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    // Screen navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Unlocked Themes List state
    private val _unlockedThemes = MutableStateFlow(setOf("Sophisticated Dark", "Dark Glass"))
    val unlockedThemes: StateFlow<Set<String>> = _unlockedThemes.asStateFlow()

    // Sound and vibration modifiers
    fun triggerVibration(durationMs: Long = 50L) {
        if (!userProfile.value.vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Check and initialize user profile
            val profile = repository.getProfile()
            if (profile == null) {
                repository.insertProfile(UserProfile(lastActiveTimestamp = System.currentTimeMillis()))
            } else {
                checkAndUpdateStreak(profile)
            }

            // Initialize Achievements if empty
            val existingAchievements = repository.getAchievements()
            if (existingAchievements.isEmpty()) {
                val initialList = listOf(
                    Achievement("first_steps", "First Steps", "Play any mini game", false, 0, 1, 0, "play"),
                    Achievement("memory_master", "Memory Elite", "Reach level 5 in Memory Match", false, 0, 5, 1, "memory"),
                    Achievement("math_streak", "Math Prodigy", "Get a streak of 8 correct in Math Puzzle", false, 0, 8, 0, "math"),
                    Achievement("light_speed", "Super Reflexes", "Achieve a reaction time below 250ms", false, 0, 1, 0, "reaction"),
                    Achievement("coin_hoarder", "Treasure Hunter", "Accumulate 300 coins", false, 0, 300, 100, "coins"),
                    Achievement("streak_starter", "Daily Focus", "Maintain a 3-day training streak", false, 0, 3, 0, "streak"),
                    Achievement("daily_conqueror", "Daily Champion", "Complete today's Daily Challenge", false, 0, 1, 0, "challenge")
                )
                repository.insertAchievements(initialList)
            }

            // Load unlocked themes from coins or mock persistence. Simple persistence can be derived from coins or a setting.
            // Let's allow unlocking themes dynamically.
            determineUnlockedThemes()
        }
    }

    fun navigateTo(screen: Screen) {
        soundManager.playClick()
        _currentScreen.value = screen
    }

    private fun checkAndUpdateStreak(profile: UserProfile) {
        val lastActive = profile.lastActiveTimestamp
        val now = System.currentTimeMillis()

        if (lastActive == 0L) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateProfile(profile.copy(lastActiveTimestamp = now, dailyStreak = 1))
            }
            return
        }

        val lastDay = lastActive / (1000 * 60 * 60 * 24)
        val today = now / (1000 * 60 * 60 * 24)

        viewModelScope.launch(Dispatchers.IO) {
            val newStreak = when {
                today == lastDay -> profile.dailyStreak // Same day, keep streak
                today == lastDay + 1 -> {
                    val updated = profile.dailyStreak + 1
                    updateAchievementProgress("streak_starter", updated)
                    updated
                } // Next day, increment streak
                else -> 1 // Long break, reset streak to 1
            }
            repository.updateProfile(profile.copy(lastActiveTimestamp = now, dailyStreak = newStreak))
        }
    }

    private fun determineUnlockedThemes() {
        val coins = userProfile.value.coins
        val themes = mutableSetOf("Sophisticated Dark", "Dark Glass")
        // Dynamically unlock themes if they have sufficient coins, or allow purchasing them
        // To make a functional coin economy, we can have a list of purchased themes stored or computed.
        // Let's create an explicit theme purchase mechanism!
        // We'll store purchased themes in a comma-separated string in Profile username, or just save them locally.
        // Actually, we can use userProfile's coins and allow purchase:
        // We can unlock standard themes for a coin fee, e.g. Neon: 100 coins, Crimson: 150 coins, Emerald: 200 coins.
    }

    fun purchaseTheme(themeName: String, cost: Int) {
        val currentProfile = userProfile.value
        if (currentProfile.coins >= cost && !_unlockedThemes.value.contains(themeName)) {
            viewModelScope.launch(Dispatchers.IO) {
                val updatedProfile = currentProfile.copy(
                    coins = currentProfile.coins - cost,
                    activeTheme = themeName
                )
                repository.updateProfile(updatedProfile)
                _unlockedThemes.value = _unlockedThemes.value + themeName
                soundManager.playLevelUp()
                triggerVibration(100)
                checkCoinAchievements(updatedProfile.coins)
            }
        } else if (_unlockedThemes.value.contains(themeName)) {
            // Already unlocked, just select it
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateProfile(currentProfile.copy(activeTheme = themeName))
                soundManager.playClick()
            }
        } else {
            soundManager.playFailure()
            triggerVibration(150)
        }
    }

    // Settings
    fun toggleSound() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProfile(userProfile.value.copy(soundEnabled = !userProfile.value.soundEnabled))
            soundManager.playClick()
        }
    }

    fun toggleVibration() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProfile(userProfile.value.copy(vibrationEnabled = !userProfile.value.vibrationEnabled))
            triggerVibration(60)
            soundManager.playClick()
        }
    }

    fun resetProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            // Reset profile
            repository.insertProfile(UserProfile(id = 1, lastActiveTimestamp = System.currentTimeMillis()))
            // Reset achievements
            val resetList = repository.getAchievements().map {
                it.copy(isUnlocked = false, unlockedTime = 0, currentValue = 0)
            }
            repository.insertAchievements(resetList)
            _unlockedThemes.value = setOf("Dark Glass")
            soundManager.playFailure()
            triggerVibration(200)
        }
    }

    // Hint management
    fun useHint(): Boolean {
        val profile = userProfile.value
        if (profile.hintsCount > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateProfile(profile.copy(hintsCount = profile.hintsCount - 1))
            }
            soundManager.playClick()
            triggerVibration(30)
            return true
        }
        soundManager.playFailure()
        triggerVibration(100)
        return false
    }

    fun buyHint() {
        val profile = userProfile.value
        if (profile.coins >= 25) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateProfile(profile.copy(
                    coins = profile.coins - 25,
                    hintsCount = profile.hintsCount + 1
                ))
            }
            soundManager.playSuccess()
            triggerVibration(50)
        } else {
            soundManager.playFailure()
            triggerVibration(100)
        }
    }


    // ==========================================
    // 🎮 MEMORY CARD MATCH GAME STATE & LOGIC
    // ==========================================

    data class MemoryCard(
        val id: Int,
        val symbol: String,
        var isFaceUp: Boolean = false,
        var isMatched: Boolean = false
    )

    private val _memoryCards = MutableStateFlow<List<MemoryCard>>(emptyList())
    val memoryCards: StateFlow<List<MemoryCard>> = _memoryCards.asStateFlow()

    private val _memoryMoves = MutableStateFlow(0)
    val memoryMoves: StateFlow<Int> = _memoryMoves.asStateFlow()

    private val _memoryTimer = MutableStateFlow(0)
    val memoryTimer: StateFlow<Int> = _memoryTimer.asStateFlow()

    private val _memoryGameState = MutableStateFlow<GameState>(GameState.Idle)
    val memoryGameState: StateFlow<GameState> = _memoryGameState.asStateFlow()

    private var memoryTimerJob: Job? = null
    private var selectedMemoryIndices = mutableListOf<Int>()

    fun startMemoryGame(difficultyLevel: Int) {
        selectedMemoryIndices.clear()
        _memoryMoves.value = 0
        _memoryTimer.value = getMemoryTimerForLevel(difficultyLevel)
        _memoryGameState.value = GameState.Playing

        // Generate matching emoji pairs based on level.
        // Level 1: 4 cards (2 pairs), Level 2: 8 cards (4 pairs), Level 3: 12 cards (6 pairs), Level 4: 16 cards (8 pairs).
        val emojis = listOf("🧠", "💡", "🎯", "⚡", "🧩", "🔮", "🧪", "🎨", "🔑", "🛡️", "🚀", "🛸")
        val pairsNeeded = when (difficultyLevel) {
            1 -> 2
            2 -> 4
            3 -> 6
            else -> 8
        }
        val selectedEmojis = emojis.take(pairsNeeded)
        val deck = (selectedEmojis + selectedEmojis).shuffled().mapIndexed { index, emoji ->
            MemoryCard(id = index, symbol = emoji)
        }
        _memoryCards.value = deck

        // Start countdown timer
        memoryTimerJob?.cancel()
        memoryTimerJob = viewModelScope.launch {
            while (_memoryTimer.value > 0 && _memoryGameState.value == GameState.Playing) {
                delay(1000)
                _memoryTimer.value -= 1
            }
            if (_memoryTimer.value == 0 && _memoryGameState.value == GameState.Playing) {
                _memoryGameState.value = GameState.Fail
                soundManager.playFailure()
                triggerVibration(150)
            }
        }
    }

    private fun getMemoryTimerForLevel(level: Int): Int {
        return when (level) {
            1 -> 20
            2 -> 35
            3 -> 50
            else -> 60
        }
    }

    fun onMemoryCardClicked(index: Int) {
        val cards = _memoryCards.value.toMutableList()
        val card = cards[index]

        if (_memoryGameState.value != GameState.Playing || card.isFaceUp || card.isMatched || selectedMemoryIndices.size >= 2) {
            return
        }

        soundManager.playClick()
        triggerVibration(25)

        card.isFaceUp = true
        _memoryCards.value = cards
        selectedMemoryIndices.add(index)

        if (selectedMemoryIndices.size == 2) {
            _memoryMoves.value += 1
            viewModelScope.launch {
                delay(800)
                checkMemoryMatch()
            }
        }
    }

    private fun checkMemoryMatch() {
        if (selectedMemoryIndices.size < 2) return
        val cards = _memoryCards.value.toMutableList()
        val index1 = selectedMemoryIndices[0]
        val index2 = selectedMemoryIndices[1]

        val card1 = cards[index1]
        val card2 = cards[index2]

        if (card1.symbol == card2.symbol) {
            // Match found!
            card1.isMatched = true
            card2.isMatched = true
            soundManager.playSuccess()
            triggerVibration(50)

            // Check win
            if (cards.all { it.isMatched }) {
                handleMemoryGameWin()
            }
        } else {
            // No match, flip back
            card1.isFaceUp = false
            card2.isFaceUp = false
        }

        _memoryCards.value = cards
        selectedMemoryIndices.clear()
    }

    private fun handleMemoryGameWin() {
        memoryTimerJob?.cancel()
        _memoryGameState.value = GameState.Success
        soundManager.playLevelUp()
        triggerVibration(100)

        // Award rewards
        val level = userProfile.value.memoryLevel
        val xpGain = 15 * level + (memoryTimer.value / 2)
        val coinGain = 5 * level + (if (memoryMoves.value < level * 4) 10 else 5)

        viewModelScope.launch(Dispatchers.IO) {
            val profile = userProfile.value
            // Auto scale difficulty up!
            val nextLevel = if (level < 4) level + 1 else 4
            val score = (memoryTimer.value * 10) - (memoryMoves.value * 5)
            val updatedProfile = profile.copy(
                totalXp = profile.totalXp + xpGain,
                coins = profile.coins + coinGain,
                memoryLevel = nextLevel,
                memoryHighScore = maxOf(profile.memoryHighScore, score)
            )
            repository.updateProfile(updatedProfile)
            updateLevelXP(updatedProfile)

            updateAchievementProgress("first_steps", 1)
            updateAchievementProgress("memory_master", nextLevel)
            checkCoinAchievements(updatedProfile.coins)
        }
    }


    // ==========================================
    // 🧠 MATH PUZZLE GAME STATE & LOGIC
    // ==========================================

    data class MathQuestion(
        val equation: String,
        val options: List<Int>,
        val correctAnswer: Int
    )

    private val _currentMathQuestion = MutableStateFlow<MathQuestion?>(null)
    val currentMathQuestion: StateFlow<MathQuestion?> = _currentMathQuestion.asStateFlow()

    private val _mathStreak = MutableStateFlow(0)
    val mathStreak: StateFlow<Int> = _mathStreak.asStateFlow()

    private val _mathScore = MutableStateFlow(0)
    val mathScore: StateFlow<Int> = _mathScore.asStateFlow()

    private val _mathTimer = MutableStateFlow(0)
    val mathTimer: StateFlow<Int> = _mathTimer.asStateFlow()

    private val _mathGameState = MutableStateFlow<GameState>(GameState.Idle)
    val mathGameState: StateFlow<GameState> = _mathGameState.asStateFlow()

    private var mathTimerJob: Job? = null

    fun startMathGame(difficultyLevel: Int) {
        _mathStreak.value = 0
        _mathScore.value = 0
        _mathGameState.value = GameState.Playing
        generateNextMathQuestion(difficultyLevel)
    }

    private fun generateNextMathQuestion(level: Int) {
        // Build equations dynamically based on difficulty
        val num1: Int
        val num2: Int
        val op: String
        var ans = 0

        val random = Random.Default
        when (level) {
            1 -> {
                num1 = random.nextInt(1, 15)
                num2 = random.nextInt(1, 15)
                op = if (random.nextBoolean()) "+" else "-"
                ans = if (op == "+") num1 + num2 else num1 - num2
            }
            2 -> {
                num1 = random.nextInt(10, 50)
                num2 = random.nextInt(5, 30)
                op = if (random.nextBoolean()) "+" else "-"
                ans = if (op == "+") num1 + num2 else num1 - num2
            }
            3 -> {
                num1 = random.nextInt(2, 10)
                num2 = random.nextInt(2, 10)
                op = "*"
                ans = num1 * num2
            }
            else -> {
                // Expert: Mix addition/subtraction and simple multiplication
                num1 = random.nextInt(3, 12)
                num2 = random.nextInt(3, 10)
                val num3 = random.nextInt(5, 20)
                op = "*+"
                ans = (num1 * num2) + num3
            }
        }

        val equationStr = when (op) {
            "*" -> "$num1 × $num2"
            "*+" -> "($num1 × $num2) + ${ans - (num1 * num2)}"
            else -> "$num1 $op $num2"
        }

        // Distractors close to real answer
        val options = mutableSetOf(ans)
        while (options.size < 4) {
            val offset = random.nextInt(-10, 10)
            if (offset != 0) {
                options.add(ans + offset)
            }
        }

        _currentMathQuestion.value = MathQuestion(
            equation = equationStr,
            options = options.toList().shuffled(),
            correctAnswer = ans
        )

        // Reset timer to 12 seconds per equation
        _mathTimer.value = 12
        startMathTimer()
    }

    private fun startMathTimer() {
        mathTimerJob?.cancel()
        mathTimerJob = viewModelScope.launch {
            while (_mathTimer.value > 0 && _mathGameState.value == GameState.Playing) {
                delay(1000)
                _mathTimer.value -= 1
            }
            if (_mathTimer.value == 0 && _mathGameState.value == GameState.Playing) {
                // Time's up is treated as mistake, reset streak
                handleMathAnswer(null)
            }
        }
    }

    fun submitMathAnswer(answer: Int) {
        handleMathAnswer(answer)
    }

    private fun handleMathAnswer(userAnswer: Int?) {
        val question = _currentMathQuestion.value ?: return
        val isCorrect = userAnswer == question.correctAnswer

        if (isCorrect) {
            soundManager.playSuccess()
            triggerVibration(40)
            _mathStreak.value += 1
            viewModelScope.launch(Dispatchers.IO) {
                updateAchievementProgress("math_streak", _mathStreak.value)
            }

            // Multiplier adds visual spice to score
            val multiplier = if (_mathStreak.value >= 5) 2 else 1
            _mathScore.value += 10 * multiplier + (_mathTimer.value)

            // Generate next question
            if (_mathStreak.value >= 6) {
                // Win the set!
                handleMathGameWin()
            } else {
                generateNextMathQuestion(userProfile.value.mathLevel)
            }
        } else {
            soundManager.playFailure()
            triggerVibration(150)
            _mathStreak.value = 0 // Lose streak

            // Deduct some score or proceed
            _mathScore.value = maxOf(0, _mathScore.value - 5)

            // Auto Scale down difficulty slightly if they are failing hard, but keep minimum level 1
            viewModelScope.launch(Dispatchers.IO) {
                val currentLvl = userProfile.value.mathLevel
                if (currentLvl > 1) {
                    repository.updateProfile(userProfile.value.copy(mathLevel = currentLvl - 1))
                }
            }

            // Still let them continue
            generateNextMathQuestion(userProfile.value.mathLevel)
        }
    }

    private fun handleMathGameWin() {
        mathTimerJob?.cancel()
        _mathGameState.value = GameState.Success
        soundManager.playLevelUp()
        triggerVibration(100)

        val level = userProfile.value.mathLevel
        val xpGain = 20 * level + (_mathScore.value / 10)
        val coinGain = 10 * level

        viewModelScope.launch(Dispatchers.IO) {
            val profile = userProfile.value
            val nextLevel = if (level < 4) level + 1 else 4
            val updatedProfile = profile.copy(
                totalXp = profile.totalXp + xpGain,
                coins = profile.coins + coinGain,
                mathLevel = nextLevel,
                mathHighScore = maxOf(profile.mathHighScore, _mathScore.value)
            )
            repository.updateProfile(updatedProfile)
            updateLevelXP(updatedProfile)

            updateAchievementProgress("first_steps", 1)
            checkCoinAchievements(updatedProfile.coins)
        }
    }


    // ==========================================
    // 🎨 PATTERN RECOGNITION GAME STATE & LOGIC
    // ==========================================

    enum class PatternShape { Circle, Square, Triangle, Hexagon }
    enum class PatternColor { Red, Blue, Green, Yellow, Purple }

    data class PatternItem(
        val shape: PatternShape,
        val color: PatternColor
    )

    data class PatternChallenge(
        val sequence: List<PatternItem>,
        val choices: List<PatternItem>,
        val correctIndex: Int
    )

    private val _currentPatternChallenge = MutableStateFlow<PatternChallenge?>(null)
    val currentPatternChallenge: StateFlow<PatternChallenge?> = _currentPatternChallenge.asStateFlow()

    private val _patternScore = MutableStateFlow(0)
    val patternScore: StateFlow<Int> = _patternScore.asStateFlow()

    private val _patternStep = MutableStateFlow(0) // Need 5 steps to win
    val patternStep: StateFlow<Int> = _patternStep.asStateFlow()

    private val _patternGameState = MutableStateFlow<GameState>(GameState.Idle)
    val patternGameState: StateFlow<GameState> = _patternGameState.asStateFlow()

    fun startPatternGame(difficultyLevel: Int) {
        _patternScore.value = 0
        _patternStep.value = 0
        _patternGameState.value = GameState.Playing
        generatePatternChallenge(difficultyLevel)
    }

    private fun generatePatternChallenge(level: Int) {
        val random = Random.Default
        val shapes = PatternShape.values()
        val colors = PatternColor.values()

        val sequence = mutableListOf<PatternItem>()
        val correctItem: PatternItem

        // Pattern logic styles:
        // 1. Repeating Color, Rotating Shapes
        // 2. Repeating Shape, Rotating Colors
        // 3. Alternating Color and Alternating Shape
        val patternStyle = if (level == 1) random.nextInt(1, 3) else random.nextInt(1, 4)

        when (patternStyle) {
            1 -> {
                // Color is constant, shapes cycle
                val baseColor = colors[random.nextInt(colors.size)]
                val startShapeIdx = random.nextInt(shapes.size)
                for (i in 0..2) {
                    val s = shapes[(startShapeIdx + i) % shapes.size]
                    sequence.add(PatternItem(s, baseColor))
                }
                correctItem = PatternItem(shapes[(startShapeIdx + 3) % shapes.size], baseColor)
            }
            2 -> {
                // Shape is constant, colors cycle
                val baseShape = shapes[random.nextInt(shapes.size)]
                val startColorIdx = random.nextInt(colors.size)
                for (i in 0..2) {
                    val c = colors[(startColorIdx + i) % colors.size]
                    sequence.add(PatternItem(baseShape, c))
                }
                correctItem = PatternItem(baseShape, colors[(startColorIdx + 3) % colors.size])
            }
            else -> {
                // Alternating color & alternating shape
                val c1 = colors[random.nextInt(colors.size)]
                var c2 = colors[random.nextInt(colors.size)]
                while (c1 == c2) { c2 = colors[random.nextInt(colors.size)] }

                val s1 = shapes[random.nextInt(shapes.size)]
                var s2 = shapes[random.nextInt(shapes.size)]
                while (s1 == s2) { s2 = shapes[random.nextInt(shapes.size)] }

                // Sequence: 1, 2, 1, ? Correct: 2
                sequence.add(PatternItem(s1, c1))
                sequence.add(PatternItem(s2, c2))
                sequence.add(PatternItem(s1, c1))
                correctItem = PatternItem(s2, c2)
            }
        }

        // Generate choices including correct item
        val choicesSet = mutableSetOf(correctItem)
        while (choicesSet.size < 4) {
            val rs = shapes[random.nextInt(shapes.size)]
            val rc = colors[random.nextInt(colors.size)]
            choicesSet.add(PatternItem(rs, rc))
        }

        val choicesList = choicesSet.toList().shuffled()
        val correctIndex = choicesList.indexOf(correctItem)

        _currentPatternChallenge.value = PatternChallenge(
            sequence = sequence,
            choices = choicesList,
            correctIndex = correctIndex
        )
    }

    fun submitPatternAnswer(choiceIdx: Int) {
        if (_patternGameState.value != GameState.Playing) return
        val challenge = _currentPatternChallenge.value ?: return

        if (choiceIdx == challenge.correctIndex) {
            soundManager.playSuccess()
            triggerVibration(40)
            _patternScore.value += 15
            _patternStep.value += 1

            if (_patternStep.value >= 5) {
                handlePatternWin()
            } else {
                generatePatternChallenge(userProfile.value.patternLevel)
            }
        } else {
            soundManager.playFailure()
            triggerVibration(150)
            // Lose pattern game immediately or subtract step
            _patternGameState.value = GameState.Fail
        }
    }

    private fun handlePatternWin() {
        _patternGameState.value = GameState.Success
        soundManager.playLevelUp()
        triggerVibration(100)

        val level = userProfile.value.patternLevel
        val xpGain = 20 * level
        val coinGain = 8 * level

        viewModelScope.launch(Dispatchers.IO) {
            val profile = userProfile.value
            val nextLevel = if (level < 4) level + 1 else 4
            val updatedProfile = profile.copy(
                totalXp = profile.totalXp + xpGain,
                coins = profile.coins + coinGain,
                patternLevel = nextLevel,
                patternHighScore = maxOf(profile.patternHighScore, _patternScore.value)
            )
            repository.updateProfile(updatedProfile)
            updateLevelXP(updatedProfile)

            updateAchievementProgress("first_steps", 1)
            checkCoinAchievements(updatedProfile.coins)
        }
    }


    // ==========================================
    // ⚡ REACTION SPEED TEST GAME STATE & LOGIC
    // ==========================================

    enum class ReactionState { Idle, Wait, Go, Finished, Cheat }

    private val _reactionState = MutableStateFlow(ReactionState.Idle)
    val reactionState: StateFlow<ReactionState> = _reactionState.asStateFlow()

    private val _currentReactionTime = MutableStateFlow<Long?>(null)
    val currentReactionTime: StateFlow<Long?> = _currentReactionTime.asStateFlow()

    private var reactionStartTime: Long = 0
    private var reactionDelayJob: Job? = null

    fun startReactionGame() {
        _reactionState.value = ReactionState.Wait
        _currentReactionTime.value = null

        reactionDelayJob?.cancel()
        reactionDelayJob = viewModelScope.launch {
            // Random delay between 1.5s to 4s
            val delayMs = Random.nextLong(1500, 4000)
            delay(delayMs)
            if (_reactionState.value == ReactionState.Wait) {
                _reactionState.value = ReactionState.Go
                reactionStartTime = System.currentTimeMillis()
                triggerVibration(80) // Strong haptic on GO!
            }
        }
    }

    fun onReactionScreenClicked() {
        when (_reactionState.value) {
            ReactionState.Wait -> {
                // Cheat detection!
                reactionDelayJob?.cancel()
                _reactionState.value = ReactionState.Cheat
                soundManager.playFailure()
                triggerVibration(150)
            }
            ReactionState.Go -> {
                val endTime = System.currentTimeMillis()
                val diff = endTime - reactionStartTime
                _currentReactionTime.value = diff
                _reactionState.value = ReactionState.Finished

                soundManager.playSuccess()
                triggerVibration(60)

                handleReactionFinished(diff)
            }
            else -> {}
        }
    }

    private fun handleReactionFinished(timeMs: Long) {
        // Calculate dynamic reward based on reaction speed
        // Under 250ms is outstanding, 250-400ms is standard, >400ms is slow.
        val xpGain: Int
        val coinGain: Int
        val isLightSpeed = timeMs < 250

        if (isLightSpeed) {
            xpGain = 35
            coinGain = 15
        } else if (timeMs < 400) {
            xpGain = 20
            coinGain = 8
        } else {
            xpGain = 10
            coinGain = 3
        }

        val level = userProfile.value.reactionLevel
        // Auto scale level based on speed!
        val nextLevel = if (timeMs < 280) {
            if (level < 4) level + 1 else 4
        } else if (timeMs > 450) {
            if (level > 1) level - 1 else 1
        } else {
            level
        }

        // Score formulation
        val score = (1000 - timeMs).coerceAtLeast(10).toInt()

        viewModelScope.launch(Dispatchers.IO) {
            val profile = userProfile.value
            val updatedProfile = profile.copy(
                totalXp = profile.totalXp + xpGain,
                coins = profile.coins + coinGain,
                reactionLevel = nextLevel,
                reactionHighScore = if (profile.reactionHighScore == 0) score else maxOf(profile.reactionHighScore, score)
            )
            repository.updateProfile(updatedProfile)
            updateLevelXP(updatedProfile)

            updateAchievementProgress("first_steps", 1)
            if (isLightSpeed) {
                updateAchievementProgress("light_speed", 1)
            }
            checkCoinAchievements(updatedProfile.coins)
        }
    }


    // ==========================================
    // 🏆 DAILY CHALLENGE SYSTEM
    // ==========================================

    fun getTodayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun completeDailyChallenge() {
        val todayStr = getTodayString()
        val profile = userProfile.value

        if (profile.dailyChallengeCompletedDate != todayStr) {
            viewModelScope.launch(Dispatchers.IO) {
                val updatedProfile = profile.copy(
                    coins = profile.coins + 50, // Huge bonus!
                    totalXp = profile.totalXp + 100,
                    dailyChallengeCompletedDate = todayStr
                )
                repository.updateProfile(updatedProfile)
                updateLevelXP(updatedProfile)

                soundManager.playLevelUp()
                triggerVibration(120)

                updateAchievementProgress("daily_conqueror", 1)
                checkCoinAchievements(updatedProfile.coins)
            }
        }
    }


    // ==========================================
    // 📈 XP & LEVEL SYSTEM
    // ==========================================

    private suspend fun updateLevelXP(profile: UserProfile) {
        val currentLevel = profile.level
        val neededXp = currentLevel * 100 // Level 1 needs 100xp, Level 2 needs 200xp, etc.

        if (profile.totalXp >= neededXp) {
            val newLevel = currentLevel + 1
            repository.updateProfile(profile.copy(
                level = newLevel,
                totalXp = profile.totalXp - neededXp, // rollover
                coins = profile.coins + 30 // level-up reward
            ))
            soundManager.playLevelUp()
            triggerVibration(200)
        }
    }


    // ==========================================
    // 🏅 ACHIEVEMENT / BADGES ENGINE
    // ==========================================

    private suspend fun updateAchievementProgress(id: String, incrementValue: Int) {
        val list = repository.getAchievements()
        val item = list.find { it.id == id } ?: return

        if (item.isUnlocked) return

        val newValue = if (id == "streak_starter" || id == "math_streak" || id == "memory_master" || id == "daily_conqueror" || id == "light_speed") {
            // Absolute assignments
            incrementValue
        } else {
            // Incremental
            item.currentValue + incrementValue
        }

        val completed = newValue >= item.targetValue
        val updated = item.copy(
            currentValue = minOf(newValue, item.targetValue),
            isUnlocked = completed,
            unlockedTime = if (completed) System.currentTimeMillis() else 0L
        )

        repository.updateAchievement(updated)
        if (completed) {
            // Reward coins for achievements
            val profile = repository.getProfile() ?: return
            repository.updateProfile(profile.copy(coins = profile.coins + 20))
            soundManager.playLevelUp()
            triggerVibration(150)
        }
    }

    private suspend fun checkCoinAchievements(currentCoins: Int) {
        updateAchievementProgress("coin_hoarder", currentCoins)
    }
}

sealed class Screen {
    object Splash : Screen()
    object Home : Screen()
    object MemoryGame : Screen()
    object MathGame : Screen()
    object PatternGame : Screen()
    object ReactionGame : Screen()
    object Profile : Screen()
    object Achievements : Screen()
    object Settings : Screen()
}

enum class GameState { Idle, Playing, Success, Fail }
