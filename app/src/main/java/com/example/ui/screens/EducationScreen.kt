package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.EducationRepository
import com.example.model.Lesson
import com.example.model.QuizQuestion
import com.example.ui.theme.*
import kotlinx.coroutines.launch

import com.example.model.UserSubscriptionState

@Composable
fun EducationScreen(
    educationRepository: EducationRepository,
    subscriptionState: UserSubscriptionState = UserSubscriptionState(),
    onNavigateToPremium: () -> Unit = {},
    onBack: () -> Unit
) {
    val progressMap by educationRepository.progressFlow.collectAsState(initial = emptyMap())
    val lessons = remember { educationRepository.getAllLessons() }
    var activeLesson by remember { mutableStateOf<Lesson?>(null) }
    var showVipLockedDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    if (activeLesson != null) {
        LessonDetailView(
            lesson = activeLesson!!,
            isCompleted = progressMap[activeLesson!!.id] == true,
            onBack = { activeLesson = null },
            onQuizCompleted = { score ->
                coroutineScope.launch {
                    educationRepository.markLessonCompleted(activeLesson!!.id, score)
                }
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                }
                Column {
                    Text(
                        text = "TRADING ACADEMY",
                        color = PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Forex • Indian Markets • Crypto • SMC Mastery",
                        color = MintGreen,
                        fontSize = 11.sp
                    )
                }
            }

            // Progress Banner
            val completedCount = lessons.count { progressMap[it.id] == true }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CURRICULUM PROGRESS", color = SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$completedCount / ${lessons.size} Modules Completed", color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    val pct = if (lessons.isNotEmpty()) (completedCount.toFloat() / lessons.size.toFloat()) else 0f
                    CircularProgressIndicator(
                        progress = { pct },
                        color = MintGreen,
                        trackColor = DarkBlueGray,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Lessons List
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(lessons) { lesson ->
                    val isDone = progressMap[lesson.id] == true
                    val isLocked = lesson.isVipOnly && !subscriptionState.isPremium

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isLocked) {
                                    showVipLockedDialog = true
                                } else {
                                    activeLesson = lesson
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (lesson.isVipOnly) WarningGold.copy(alpha = 0.2f) else DarkBlueGray
                                    ) {
                                        Text(
                                            lesson.category.uppercase(),
                                            color = if (lesson.isVipOnly) WarningGold else MintGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (lesson.isVipOnly) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MintGreen.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                "VIP MASTERCLASS",
                                                color = MintGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(lesson.title, color = PrimaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(lesson.subtitle, color = SecondaryText, fontSize = 11.sp)
                            }

                            if (isLocked) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(DarkBlueGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "VIP Locked", tint = WarningGold, modifier = Modifier.size(16.dp))
                                }
                            } else if (isDone) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(BullishGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Completed", tint = DeepBackground, modifier = Modifier.size(18.dp))
                                }
                            } else {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = SecondaryText)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVipLockedDialog) {
        AlertDialog(
            onDismissRequest = { showVipLockedDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Stars, contentDescription = null, tint = MintGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VIP Masterclass Facility", color = PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This advanced institutional module (CBDR, Options Greeks & $100k+ Prop Firm sizing) is part of Amend Trend VIP facilities.",
                        color = PrimaryText,
                        fontSize = 13.sp
                    )
                    Text(
                        "Join VIP now:\n• Monthly: $10\n• 6 Months: $60 (Popular)\n• 1 Year: $120 (All Facilities Unlocked)",
                        color = MintGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVipLockedDialog = false
                        onNavigateToPremium()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen)
                ) {
                    Text("View Plans & Facilities", color = DeepBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVipLockedDialog = false }) {
                    Text("Later", color = SecondaryText)
                }
            },
            containerColor = CardBackground
        )
    }
}

@Composable
fun LessonDetailView(
    lesson: Lesson,
    isCompleted: Boolean,
    onBack: () -> Unit,
    onQuizCompleted: (Int) -> Unit
) {
    var quizStep by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isAnswerSubmitted by remember { mutableStateOf(false) }
    var userScore by remember { mutableStateOf(0) }
    var quizFinished by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryText)
            }
            Column {
                Text(lesson.title, color = PrimaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("${lesson.category} • ${lesson.readTimeMinutes} min read", color = MintGreen, fontSize = 11.sp)
            }
        }

        // Lesson Content
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = lesson.contentMarkdown,
                    color = PrimaryText,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                Divider(color = BorderStroke)

                Text("KEY TAKEAWAYS", color = MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                lesson.keyTakeaways.forEach { item ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("✓ ", color = MintGreen, fontWeight = FontWeight.Bold)
                        Text(item, color = SecondaryText, fontSize = 12.sp)
                    }
                }
            }
        }

        // Interactive Quiz Section
        if (lesson.quiz.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBlueGray)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "KNOWLEDGE CHECK QUIZ",
                        color = MintGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    if (!quizFinished) {
                        val currentQ = lesson.quiz[quizStep]
                        Text("Question ${quizStep + 1} of ${lesson.quiz.size}", color = SecondaryText, fontSize = 11.sp)
                        Text(currentQ.question, color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                        currentQ.options.forEachIndexed { idx, optionText ->
                            val isSel = selectedOption == idx
                            val optionBg = when {
                                isAnswerSubmitted && idx == currentQ.correctIndex -> BullishGreen
                                isAnswerSubmitted && isSel && idx != currentQ.correctIndex -> BearishRed
                                isSel -> MintGreen
                                else -> CardBackground
                            }
                            val textColor = if (isSel || (isAnswerSubmitted && idx == currentQ.correctIndex)) DeepBackground else PrimaryText

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = optionBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAnswerSubmitted) { selectedOption = idx }
                            ) {
                                Text(
                                    text = optionText,
                                    color = textColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (isAnswerSubmitted) {
                            Text(
                                text = currentQ.explanation,
                                color = if (selectedOption == currentQ.correctIndex) MintGreen else PrimaryText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            Button(
                                onClick = {
                                    if (quizStep + 1 < lesson.quiz.size) {
                                        quizStep += 1
                                        selectedOption = null
                                        isAnswerSubmitted = false
                                    } else {
                                        quizFinished = true
                                        onQuizCompleted(userScore)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MintGreen)
                            ) {
                                Text(
                                    if (quizStep + 1 < lesson.quiz.size) "Next Question" else "Finish & Complete Module",
                                    color = DeepBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (selectedOption != null) {
                                        isAnswerSubmitted = true
                                        if (selectedOption == currentQ.correctIndex) {
                                            userScore += 100 / lesson.quiz.size
                                        }
                                    }
                                },
                                enabled = selectedOption != null,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MintGreen)
                            ) {
                                Text("Check Answer", color = DeepBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = WarningGold, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Module Mastered!", color = PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Progress saved to your trading profile.", color = SecondaryText, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = MintGreen)) {
                                Text("Return to Curriculum", color = DeepBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
