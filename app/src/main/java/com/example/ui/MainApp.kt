package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainApp(viewModel: GameViewModel) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val unlockedThemes by viewModel.unlockedThemes.collectAsStateWithLifecycle()
    val palette = LocalThemePalette.current

    // Trigger theme update in the viewmodel or initialize
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.backgroundStart, palette.backgroundEnd)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                is Screen.Splash -> SplashScreen(
                    onTimeout = { viewModel.navigateTo(Screen.Home) }
                )
                is Screen.Home -> DashboardScreen(viewModel, profile)
                is Screen.MemoryGame -> MemoryGameScreen(viewModel, profile)
                is Screen.MathGame -> MathGameScreen(viewModel, profile)
                is Screen.PatternGame -> PatternGameScreen(viewModel, profile)
                is Screen.ReactionGame -> ReactionGameScreen(viewModel)
                is Screen.Profile -> ProfileScreen(viewModel, profile)
                is Screen.Achievements -> AchievementsScreen(viewModel, achievements)
                is Screen.Settings -> SettingsScreen(viewModel, profile, unlockedThemes)
            }
        }
    }
}

// ==========================================
// 🚀 SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val palette = LocalThemePalette.current

    // Infinite pulse animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        // Slowly increase progress
        while (progress < 1f) {
            delay(40)
            progress += 0.02f
        }
        delay(200)
        onTimeout()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("splash_screen"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing Brain Logo
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scale)
                .background(palette.primary.copy(alpha = 0.15f), shape = CircleShape)
                .border(2.dp, palette.primary, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "Brain Logo",
                tint = palette.primary,
                modifier = Modifier.size(70.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "COGNITIVE LABS",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Train Your Memory, Math & Speed",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(6.dp)
                .background(Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(palette.primary, shape = RoundedCornerShape(3.dp))
            )
        }
    }
}

// ==========================================
// 🎮 MAIN DASHBOARD
// ==========================================
@Composable
fun DashboardScreen(viewModel: GameViewModel, profile: UserProfile) {
    val palette = LocalThemePalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("dashboard_screen")
    ) {
        // Top Header Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome Back,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
                Text(
                    text = profile.username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            }

            // Coin indicator
            Row(
                modifier = Modifier
                    .background(palette.surface, shape = RoundedCornerShape(20.dp))
                    .border(1.dp, palette.border, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = "Coins",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${profile.coins}",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Level & Streak Bar
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(palette.primary.copy(alpha = 0.2f), shape = CircleShape)
                                .border(1.dp, palette.primary, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${profile.level}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Cognitive Level",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                            val xpNeeded = profile.level * 100
                            Text(
                                text = "XP: ${profile.totalXp} / $xpNeeded",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Streak Display
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 ${profile.dailyStreak} Day Streak",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF5722)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // XP linear progress
                val xpNeeded = profile.level * 100
                val progressFactor = (profile.totalXp.toFloat() / xpNeeded).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFactor)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(palette.primary, palette.secondary)
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Challenge Box
        val todayStr = viewModel.getTodayString()
        val challengeCompleted = profile.dailyChallengeCompletedDate == todayStr

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowingColor = if (challengeCompleted) palette.secondary else palette.primary
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚡ DAILY CHALLENGE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.primary
                    )
                    Text(
                        text = if (challengeCompleted) {
                            "Completed! Claimed +100 XP & +50 Coins."
                        } else {
                            "Complete all daily puzzle variants to secure bonus resources!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (!challengeCompleted) {
                            viewModel.completeDailyChallenge()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (challengeCompleted) palette.secondary.copy(alpha = 0.3f) else palette.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !challengeCompleted,
                    modifier = Modifier.testTag("daily_challenge_button")
                ) {
                    Text(
                        text = if (challengeCompleted) "Done" else "Complete",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (challengeCompleted) palette.textSecondary else Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "MINI GAMES",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Games Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                GameDashboardCard(
                    title = "Memory Match",
                    description = "Pair tiles",
                    icon = Icons.Default.FilterFrames,
                    highscore = profile.memoryHighScore,
                    level = profile.memoryLevel,
                    tag = "game_memory_card",
                    onClick = {
                        viewModel.startMemoryGame(profile.memoryLevel)
                        viewModel.navigateTo(Screen.MemoryGame)
                    }
                )
            }
            item {
                GameDashboardCard(
                    title = "Math Puzzle",
                    description = "Rapid equations",
                    icon = Icons.Default.Calculate,
                    highscore = profile.mathHighScore,
                    level = profile.mathLevel,
                    tag = "game_math_card",
                    onClick = {
                        viewModel.startMathGame(profile.mathLevel)
                        viewModel.navigateTo(Screen.MathGame)
                    }
                )
            }
            item {
                GameDashboardCard(
                    title = "Pattern Crack",
                    description = "Solve sequences",
                    icon = Icons.Default.Category,
                    highscore = profile.patternHighScore,
                    level = profile.patternLevel,
                    tag = "game_pattern_card",
                    onClick = {
                        viewModel.startPatternGame(profile.patternLevel)
                        viewModel.navigateTo(Screen.PatternGame)
                    }
                )
            }
            item {
                GameDashboardCard(
                    title = "Reflex Speed",
                    description = "Reaction time",
                    icon = Icons.Default.Bolt,
                    highscore = profile.reactionHighScore,
                    level = profile.reactionLevel,
                    tag = "game_reaction_card",
                    onClick = {
                        viewModel.startReactionGame()
                        viewModel.navigateTo(Screen.ReactionGame)
                    }
                )
            }
        }

        // Navigation Footer
        BottomNavigationBar(viewModel, activeScreen = Screen.Home)
    }
}

@Composable
fun GameDashboardCard(
    title: String,
    description: String,
    icon: ImageVector,
    highscore: Int,
    level: Int,
    tag: String,
    onClick: () -> Unit
) {
    val palette = LocalThemePalette.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(palette.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = palette.primary, modifier = Modifier.size(20.dp))
                }

                // Level Capsule
                Box(
                    modifier = Modifier
                        .background(palette.secondary.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Lvl $level",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 1
                )
            }

            Text(
                text = "Best: $highscore",
                style = MaterialTheme.typography.labelSmall,
                color = palette.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==========================================
// 🎨 TRANSLUCENT GLASS CARD UTILITY
// ==========================================
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glowingColor: Color? = null,
    content: @Composable () -> Unit
) {
    val palette = LocalThemePalette.current

    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                clip = false
            )
            .background(
                color = palette.surface,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = glowingColor?.copy(alpha = 0.5f) ?: palette.border,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        content()
    }
}

// ==========================================
// 🧭 TRANSLUCENT NAVIGATION FOOTER
// ==========================================
@Composable
fun BottomNavigationBar(viewModel: GameViewModel, activeScreen: Screen) {
    val palette = LocalThemePalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(palette.surface, shape = RoundedCornerShape(20.dp))
            .border(1.dp, palette.border, shape = RoundedCornerShape(20.dp))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavBarItem(
            icon = Icons.Default.GridView,
            label = "Games",
            isSelected = activeScreen is Screen.Home,
            onClick = { viewModel.navigateTo(Screen.Home) }
        )
        NavBarItem(
            icon = Icons.Default.Person,
            label = "Stats",
            isSelected = activeScreen is Screen.Profile,
            onClick = { viewModel.navigateTo(Screen.Profile) }
        )
        NavBarItem(
            icon = Icons.Default.EmojiEvents,
            label = "Badges",
            isSelected = activeScreen is Screen.Achievements,
            onClick = { viewModel.navigateTo(Screen.Achievements) }
        )
        NavBarItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            isSelected = activeScreen is Screen.Settings,
            onClick = { viewModel.navigateTo(Screen.Settings) }
        )
    }
}

@Composable
fun NavBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalThemePalette.current

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) palette.primary else palette.textSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) palette.primary else palette.textSecondary.copy(alpha = 0.6f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ==========================================
// 🎮 MEMORY MATCH GAME PLAY SCREEN
// ==========================================
@Composable
fun MemoryGameScreen(viewModel: GameViewModel, profile: UserProfile) {
    val palette = LocalThemePalette.current
    val cards by viewModel.memoryCards.collectAsStateWithLifecycle()
    val moves by viewModel.memoryMoves.collectAsStateWithLifecycle()
    val timer by viewModel.memoryTimer.collectAsStateWithLifecycle()
    val state by viewModel.memoryGameState.collectAsStateWithLifecycle()

    var showPauseDialog by remember { mutableStateOf(false) }
    var revealedHintSymbol by remember { mutableStateOf<String?>(null) }

    // Handlers for dynamic actions
    val useHintAction = {
        if (revealedHintSymbol == null && viewModel.useHint()) {
            val unmatched = cards.filter { !it.isMatched && !it.isFaceUp }
            if (unmatched.isNotEmpty()) {
                val matchTarget = unmatched.random()
                viewModel.viewModelScope.launch {
                    revealedHintSymbol = matchTarget.symbol
                    delay(1200)
                    revealedHintSymbol = null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("memory_game_screen")
    ) {
        // Stats Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showPauseDialog = true }
            ) {
                Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause", tint = palette.textPrimary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Timer", tint = palette.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${timer}s",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (timer < 8) Color.Red else palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Moves: $moves",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(cards) { index, card ->
                    val isRevealed = card.isFaceUp || card.isMatched || revealedHintSymbol == card.symbol
                    MemoryCardView(card, isRevealed) {
                        viewModel.onMemoryCardClicked(index)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = useHintAction,
                colors = ButtonDefaults.buttonColors(containerColor = palette.secondary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).padding(end = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.TipsAndUpdates, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Hint (${profile.hintsCount})")
            }

            Button(
                onClick = { viewModel.startMemoryGame(profile.memoryLevel) },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).padding(start = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Restart", color = Color.Black)
            }
        }

        // Overlay states
        if (state == GameState.Success) {
            GameSuccessDialog(
                title = "Brain Match Clean!",
                rewardXP = profile.memoryLevel * 15 + (timer / 2),
                rewardCoins = profile.memoryLevel * 5 + 5,
                onContinue = { viewModel.navigateTo(Screen.Home) }
            )
        } else if (state == GameState.Fail) {
            GameFailDialog(
                onRestart = { viewModel.startMemoryGame(profile.memoryLevel) },
                onExit = { viewModel.navigateTo(Screen.Home) }
            )
        }

        if (showPauseDialog) {
            GamePauseDialog(
                onResume = { showPauseDialog = false },
                onExit = {
                    showPauseDialog = false
                    viewModel.navigateTo(Screen.Home)
                }
            )
        }
    }
}

@Composable
fun MemoryCardView(card: GameViewModel.MemoryCard, isRevealed: Boolean, onClick: () -> Unit) {
    val palette = LocalThemePalette.current

    // Rotation effect
    val rotation by animateFloatAsState(
        targetValue = if (isRevealed) 180f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .background(
                if (isRevealed) palette.primary.copy(alpha = 0.15f) else palette.surface,
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                if (isRevealed) palette.primary else palette.border,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .scale(if (rotation > 90f) -1f else 1f),
        contentAlignment = Alignment.Center
    ) {
        if (isRevealed) {
            Text(
                text = card.symbol,
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )
        } else {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "?",
                tint = palette.textSecondary.copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// ==========================================
// 🧠 MATH PUZZLE PLAY SCREEN
// ==========================================
@Composable
fun MathGameScreen(viewModel: GameViewModel, profile: UserProfile) {
    val palette = LocalThemePalette.current
    val question by viewModel.currentMathQuestion.collectAsStateWithLifecycle()
    val streak by viewModel.mathStreak.collectAsStateWithLifecycle()
    val score by viewModel.mathScore.collectAsStateWithLifecycle()
    val timer by viewModel.mathTimer.collectAsStateWithLifecycle()
    val state by viewModel.mathGameState.collectAsStateWithLifecycle()

    var showPauseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("math_game_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showPauseDialog = true }) {
                Icon(imageVector = Icons.Default.Pause, contentDescription = null, tint = palette.textPrimary)
            }

            // Streak indicator
            Row(
                modifier = Modifier
                    .background(palette.secondary.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔥 Streak: $streak",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.secondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Score: $score",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Equation Card
        question?.let { mathQ ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Counting timer
                    CircularProgressIndicator(
                        progress = { timer / 12f },
                        modifier = Modifier.size(70.dp),
                        color = if (timer < 5) Color.Red else palette.primary,
                        strokeWidth = 6.dp,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Solve Equation:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textSecondary
                    )

                    Text(
                        text = mathQ.equation,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Option Choices
            Column(
                modifier = Modifier
                    .weight(1.8f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                mathQ.options.forEach { option ->
                    Button(
                        onClick = { viewModel.submitMathAnswer(option) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("math_option_$option"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.surface
                        ),
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Text(
                            text = "$option",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Handle states
        if (state == GameState.Success) {
            GameSuccessDialog(
                title = "Mathematical Genius!",
                rewardXP = profile.mathLevel * 20 + (score / 10),
                rewardCoins = profile.mathLevel * 10,
                onContinue = { viewModel.navigateTo(Screen.Home) }
            )
        }

        if (showPauseDialog) {
            GamePauseDialog(
                onResume = { showPauseDialog = false },
                onExit = {
                    showPauseDialog = false
                    viewModel.navigateTo(Screen.Home)
                }
            )
        }
    }
}

// ==========================================
// 🎨 PATTERN RECOGNITION PLAY SCREEN
// ==========================================
@Composable
fun PatternGameScreen(viewModel: GameViewModel, profile: UserProfile) {
    val palette = LocalThemePalette.current
    val challenge by viewModel.currentPatternChallenge.collectAsStateWithLifecycle()
    val score by viewModel.patternScore.collectAsStateWithLifecycle()
    val step by viewModel.patternStep.collectAsStateWithLifecycle()
    val state by viewModel.patternGameState.collectAsStateWithLifecycle()

    var showPauseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("pattern_game_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showPauseDialog = true }) {
                Icon(imageVector = Icons.Default.Pause, contentDescription = null, tint = palette.textPrimary)
            }

            Text(
                text = "Progress: $step / 5",
                style = MaterialTheme.typography.titleMedium,
                color = palette.primary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Score: $score",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        challenge?.let { chall ->
            // Sequence Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "What shape comes next in the sequence?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Row showing Sequence
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        chall.sequence.forEach { item ->
                            PatternShapeView(item = item, modifier = Modifier.size(64.dp))

                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = palette.textSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Unknown item
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(palette.secondary.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                                .border(1.dp, palette.secondary, shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "?",
                                style = MaterialTheme.typography.displayMedium,
                                color = palette.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Choices Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .weight(1.7f)
                    .fillMaxWidth()
            ) {
                itemsIndexed(chall.choices) { idx, choice ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1.2f)
                            .shadow(4.dp, RoundedCornerShape(18.dp))
                            .background(palette.surface, shape = RoundedCornerShape(18.dp))
                            .border(1.dp, palette.border, shape = RoundedCornerShape(18.dp))
                            .clickable { viewModel.submitPatternAnswer(idx) }
                            .testTag("pattern_choice_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        PatternShapeView(item = choice, modifier = Modifier.size(60.dp))
                    }
                }
            }
        }

        // Overlay dialogs
        if (state == GameState.Success) {
            GameSuccessDialog(
                title = "Sequence Solved!",
                rewardXP = profile.patternLevel * 20,
                rewardCoins = profile.patternLevel * 8,
                onContinue = { viewModel.navigateTo(Screen.Home) }
            )
        } else if (state == GameState.Fail) {
            GameFailDialog(
                onRestart = { viewModel.startPatternGame(profile.patternLevel) },
                onExit = { viewModel.navigateTo(Screen.Home) }
            )
        }

        if (showPauseDialog) {
            GamePauseDialog(
                onResume = { showPauseDialog = false },
                onExit = {
                    showPauseDialog = false
                    viewModel.navigateTo(Screen.Home)
                }
            )
        }
    }
}

@Composable
fun PatternShapeView(item: GameViewModel.PatternItem, modifier: Modifier = Modifier) {
    val color = when (item.color) {
        GameViewModel.PatternColor.Red -> Color(0xFFFF5252)
        GameViewModel.PatternColor.Blue -> Color(0xFF40C4FF)
        GameViewModel.PatternColor.Green -> Color(0xFF69F0AE)
        GameViewModel.PatternColor.Yellow -> Color(0xFFFFD740)
        GameViewModel.PatternColor.Purple -> Color(0xFFE040FB)
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (item.shape) {
            GameViewModel.PatternShape.Circle -> {
                drawCircle(color = color, radius = w / 2f)
            }
            GameViewModel.PatternShape.Square -> {
                drawRect(color = color, size = size)
            }
            GameViewModel.PatternShape.Triangle -> {
                val path = Path().apply {
                    moveTo(w / 2f, 0f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path = path, color = color)
            }
            GameViewModel.PatternShape.Hexagon -> {
                val path = Path().apply {
                    moveTo(w / 2f, 0f)
                    lineTo(w, h * 0.25f)
                    lineTo(w, h * 0.75f)
                    lineTo(w / 2f, h)
                    lineTo(0f, h * 0.75f)
                    lineTo(0f, h * 0.25f)
                    close()
                }
                drawPath(path = path, color = color)
            }
        }
    }
}

// ==========================================
// ⚡ REACTION SPEED TEST PLAY SCREEN
// ==========================================
@Composable
fun ReactionGameScreen(viewModel: GameViewModel) {
    val palette = LocalThemePalette.current
    val state by viewModel.reactionState.collectAsStateWithLifecycle()
    val lastTime by viewModel.currentReactionTime.collectAsStateWithLifecycle()

    val screenBg = when (state) {
        GameViewModel.ReactionState.Idle -> palette.backgroundStart
        GameViewModel.ReactionState.Wait -> Color(0xFFD50000) // Deep red
        GameViewModel.ReactionState.Go -> Color(0xFF00C853) // Neon green
        GameViewModel.ReactionState.Finished -> palette.backgroundEnd
        GameViewModel.ReactionState.Cheat -> Color(0xFFFFD600) // Yellow warning
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .clickable { viewModel.onReactionScreenClicked() }
            .testTag("reaction_game_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            when (state) {
                GameViewModel.ReactionState.Idle -> {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = palette.primary, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Reflex Speed Test",
                        style = MaterialTheme.typography.headlineLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "When red turns GREEN, tap the screen immediately. Simple!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.startReactionGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("start_reaction_button")
                    ) {
                        Text(text = "Start Test", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                GameViewModel.ReactionState.Wait -> {
                    Text(
                        text = "WAIT FOR GREEN...",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                GameViewModel.ReactionState.Go -> {
                    Text(
                        text = "TAP NOW!!!",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                GameViewModel.ReactionState.Cheat -> {
                    Text(
                        text = "Too Early!",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Wait for green before clicking.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.startReactionGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Text(text = "Try Again", color = Color.White)
                    }
                }
                GameViewModel.ReactionState.Finished -> {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = palette.primary, modifier = Modifier.size(70.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your Speed:",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textSecondary
                    )
                    Text(
                        text = "${lastTime ?: 0} ms",
                        style = MaterialTheme.typography.displayLarge,
                        color = palette.primary,
                        fontWeight = FontWeight.ExtraBold
                    )

                    val analysis = when {
                        lastTime == null -> ""
                        lastTime!! < 200 -> "Incredible! Superhuman reflexes."
                        lastTime!! < 280 -> "Fast! Outstanding mental agility."
                        lastTime!! < 400 -> "Standard. Great job."
                        else -> "Keep training to boost reflexes!"
                    }

                    Text(
                        text = analysis,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row {
                        Button(
                            onClick = { viewModel.navigateTo(Screen.Home) },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.surface),
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Text(text = "Main Menu")
                        }
                        Button(
                            onClick = { viewModel.startReactionGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        ) {
                            Text(text = "Try Again", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 📊 STATS / PROFILE SCREEN
// ==========================================
@Composable
fun ProfileScreen(viewModel: GameViewModel, profile: UserProfile) {
    val palette = LocalThemePalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("profile_screen")
    ) {
        Text(
            text = "COGNITIVE STATS",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Avatar & Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface, shape = RoundedCornerShape(18.dp))
                .border(1.dp, palette.border, shape = RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(palette.secondary.copy(alpha = 0.2f), shape = CircleShape)
                    .border(2.dp, palette.secondary, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = palette.secondary, modifier = Modifier.size(36.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = profile.username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "Lvl ${profile.level} - Brain Gym Member",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "High Scores",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                StatRow(title = "Memory Match", score = profile.memoryHighScore, maxScore = 500, color = palette.primary)
            }
            item {
                StatRow(title = "Math Puzzle", score = profile.mathHighScore, maxScore = 500, color = palette.secondary)
            }
            item {
                StatRow(title = "Pattern Recognition", score = profile.patternHighScore, maxScore = 500, color = palette.primary)
            }
            item {
                StatRow(title = "Reflex Speed (Formulated)", score = profile.reactionHighScore, maxScore = 1000, color = palette.secondary)
            }
        }

        BottomNavigationBar(viewModel, activeScreen = Screen.Profile)
    }
}

@Composable
fun StatRow(title: String, score: Int, maxScore: Int, color: Color) {
    val palette = LocalThemePalette.current

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text(text = "$score", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
            }

            Spacer(modifier = Modifier.height(6.dp))

            val fraction = (score.toFloat() / maxScore).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(color, shape = RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

// ==========================================
// 🏅 ACHIEVEMENTS SCREEN
// ==========================================
@Composable
fun AchievementsScreen(viewModel: GameViewModel, achievements: List<Achievement>) {
    val palette = LocalThemePalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("achievements_screen")
    ) {
        Text(
            text = "TRAINING BADGES",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(achievements) { badge ->
                AchievementRow(badge)
            }
        }

        BottomNavigationBar(viewModel, activeScreen = Screen.Achievements)
    }
}

@Composable
fun AchievementRow(badge: Achievement) {
    val palette = LocalThemePalette.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        glowingColor = if (badge.isUnlocked) palette.primary else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (badge.isUnlocked) palette.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                        shape = CircleShape
                    )
                    .border(
                        1.dp,
                        if (badge.isUnlocked) palette.primary else palette.border,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (badge.iconName) {
                        "memory" -> Icons.Default.FilterFrames
                        "math" -> Icons.Default.Calculate
                        "reaction" -> Icons.Default.Bolt
                        "streak" -> Icons.Default.LocalFireDepartment
                        "coins" -> Icons.Default.MonetizationOn
                        else -> Icons.Default.Star
                    },
                    contentDescription = null,
                    tint = if (badge.isUnlocked) palette.primary else palette.textSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = badge.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (badge.isUnlocked) palette.textPrimary else palette.textSecondary.copy(alpha = 0.6f)
                )
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Indicator
                val ratio = (badge.currentValue.toFloat() / badge.targetValue).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratio)
                                .background(
                                    if (badge.isUnlocked) palette.primary else palette.secondary.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${badge.currentValue}/${badge.targetValue}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}

// ==========================================
// ⚙️ SETTINGS PANEL & COIN SHOP
// ==========================================
@Composable
fun SettingsScreen(viewModel: GameViewModel, profile: UserProfile, unlockedThemes: Set<String>) {
    val palette = LocalThemePalette.current
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = "LAB PREFERENCES",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Preferences Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Sound FX
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = palette.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "Sound FX", color = palette.textPrimary)
                            }
                            Switch(
                                checked = profile.soundEnabled,
                                onCheckedChange = { viewModel.toggleSound() },
                                modifier = Modifier.testTag("sound_switch")
                            )
                        }

                        Divider(color = palette.border, modifier = Modifier.padding(vertical = 8.dp))

                        // Vibration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Vibration, contentDescription = null, tint = palette.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "Haptic Feedback", color = palette.textPrimary)
                            }
                            Switch(
                                checked = profile.vibrationEnabled,
                                onCheckedChange = { viewModel.toggleVibration() },
                                modifier = Modifier.testTag("vibration_switch")
                            )
                        }
                    }
                }
            }

            // Hint Shop Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Hint Marketplace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text(text = "Buy 1 hint for 25 coins", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                        Button(
                            onClick = { viewModel.buyHint() },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("buy_hint_button")
                        ) {
                            Text(text = "Buy (25c)", color = Color.Black)
                        }
                    }
                }
            }

            // Theme Unlock Shop
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Laboratory Themes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        val themeCosts = mapOf(
                            "Sophisticated Dark" to 0,
                            "Dark Glass" to 0,
                            "Electric Neon" to 100,
                            "Sunset Crimson" to 150,
                            "Emerald Forest" to 200
                        )

                        themeCosts.forEach { (name, cost) ->
                            val isUnlocked = name == "Sophisticated Dark" || name == "Dark Glass" || unlockedThemes.contains(name) || profile.coins >= cost // Dynamic simulation fallback
                            val isActive = profile.activeTheme == name

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(
                                                when (name) {
                                                    "Sophisticated Dark" -> SophisticatedPrimary
                                                    "Electric Neon" -> NeonPrimary
                                                    "Sunset Crimson" -> SunsetPrimary
                                                    "Emerald Forest" -> EmeraldPrimary
                                                    else -> GlassPrimary
                                                },
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = name, color = palette.textPrimary, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                                }

                                Button(
                                    onClick = { viewModel.purchaseTheme(name, cost) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isActive) palette.primary else palette.surface
                                    ),
                                    border = BorderStroke(1.dp, palette.border),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isActive) "Active" else if (unlockedThemes.contains(name) || cost == 0) "Select" else "Unlock (${cost}c)",
                                        color = if (isActive) Color.Black else palette.textPrimary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Reset Card
            item {
                Button(
                    onClick = { showResetConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("reset_progress_button")
                ) {
                    Text(text = "Reset All Training Progress", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showResetConfirm) {
            Dialog(onDismissRequest = { showResetConfirm = false }) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Reset Progress?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "This will delete all coins, XP, levels, streak records, highscores, and achievements. This action is irreversible.", color = palette.textSecondary, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(20.dp))
                        Row {
                            Button(
                                onClick = { showResetConfirm = false },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.surface),
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Text(text = "Cancel")
                            }
                            Button(
                                onClick = {
                                    viewModel.resetProgress()
                                    showResetConfirm = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            ) {
                                Text(text = "Reset", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        BottomNavigationBar(viewModel, activeScreen = Screen.Settings)
    }
}

// ==========================================
// 🛠 GENERAL POPUP DIALOGS (PAUSE, SUCCESS, FAIL)
// ==========================================
@Composable
fun GamePauseDialog(onResume: () -> Unit, onExit: () -> Unit) {
    val palette = LocalThemePalette.current

    Dialog(onDismissRequest = onResume) {
        GlassCard {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Game Paused", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Resume", color = Color.Black)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.surface),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, palette.border),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Quit to Dashboard")
                }
            }
        }
    }
}

@Composable
fun GameSuccessDialog(title: String, rewardXP: Int, rewardCoins: Int, onContinue: () -> Unit) {
    val palette = LocalThemePalette.current

    Dialog(onDismissRequest = onContinue) {
        GlassCard {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(palette.primary.copy(alpha = 0.15f), shape = CircleShape)
                        .border(1.dp, palette.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = palette.primary, modifier = Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "XP gained", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Text(text = "+$rewardXP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Coins earned", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Text(text = "+$rewardCoins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onContinue,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    modifier = Modifier.fillMaxWidth().testTag("success_continue_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Continue", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GameFailDialog(onRestart: () -> Unit, onExit: () -> Unit) {
    val palette = LocalThemePalette.current

    Dialog(onDismissRequest = onExit) {
        GlassCard {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.Red.copy(alpha = 0.15f), shape = CircleShape)
                        .border(1.dp, Color.Red, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.HighlightOff, contentDescription = null, tint = Color.Red, modifier = Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Timer Expired!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Don't discourage, train is a progression.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Try Again", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.surface),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, palette.border),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Exit")
                }
            }
        }
    }
}
