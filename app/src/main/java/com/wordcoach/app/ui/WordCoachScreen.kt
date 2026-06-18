package com.wordcoach.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wordcoach.app.speech.TtsManager

@Composable
fun WordCoachScreen(
    viewModel: MainViewModel,
    onMicPressed: () -> Unit
) {
    val scroll = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(index = viewModel.index, total = viewModel.totalWords)

            Spacer(Modifier.height(16.dp))

            WordCard(
                viewModel = viewModel,
                onHearWord = viewModel::replayWord,
                onHearSentence = viewModel::speakExample
            )

            Spacer(Modifier.height(20.dp))

            ScoreSection(viewModel)

            Spacer(Modifier.height(20.dp))

            MicButton(
                isListening = viewModel.isListening,
                micLevel = viewModel.micLevel,
                onPressed = onMicPressed,
                onStop = viewModel::stopListening
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = viewModel.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            if (viewModel.ttsStatus == TtsManager.Status.LANGUAGE_MISSING) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tip: install the English voice in Settings > " +
                        "Language & input > Text-to-speech to hear words offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(24.dp))

            NavigationRow(
                onPrevious = viewModel::previousWord,
                onShuffle = viewModel::shuffleWord,
                onNext = viewModel::nextWord
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Header(index: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Word Coach",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = "${index + 1} of $total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun WordCard(
    viewModel: MainViewModel,
    onHearWord: () -> Unit,
    onHearSentence: () -> Unit
) {
    val word = viewModel.currentWord
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = word.word,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            if (word.soundsLike.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "say it like:  ${word.soundsLike}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))
            LabeledBlock(label = "Meaning", body = word.meaning)

            if (word.example.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                LabeledBlock(label = "Example", body = "\u201C${word.example}\u201D")
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onHearWord,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Hear word", style = MaterialTheme.typography.labelLarge)
                }
                if (word.example.isNotBlank()) {
                    OutlinedButton(
                        onClick = onHearSentence,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.RecordVoiceOver, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sentence", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledBlock(label: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ScoreSection(viewModel: MainViewModel) {
    val result = viewModel.lastResult
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StarRow(stars = result?.stars ?: 0)
        AnimatedVisibility(visible = result != null) {
            if (result != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(8.dp))
                    if (result.heardWord.isNotBlank()) {
                        Text(
                            text = "I heard: \u201C${result.heardWord}\u201D",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StarRow(stars: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 1..5) {
            val filled = i <= stars
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = if (filled) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
private fun MicButton(
    isListening: Boolean,
    micLevel: Float,
    onPressed: () -> Unit,
    onStop: () -> Unit
) {
    val baseScale = if (isListening) 1.1f else 1f
    val scale by animateFloatAsState(
        targetValue = baseScale + (if (isListening) micLevel * 0.25f else 0f),
        label = "micScale"
    )
    val container =
        if (isListening) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .scale(scale)
                .background(color = container, shape = CircleShape)
                .clickableCircle { if (isListening) onStop() else onPressed() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (isListening) "Stop listening" else "Start speaking",
                tint = Color.White,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

@Composable
private fun NavigationRow(
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onPrevious,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous")
            Text("Back", style = MaterialTheme.typography.labelLarge)
        }
        OutlinedButton(
            onClick = onShuffle,
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Shuffle")
        }
        Button(
            onClick = onNext,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Next", style = MaterialTheme.typography.labelLarge)
            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next")
        }
    }
}

/** Small helper to make a circular surface clickable with ripple. */
@Composable
private fun Modifier.clickableCircle(onClick: () -> Unit): Modifier {
    return this.then(
        Modifier.clip(CircleShape).clickable(onClick = onClick)
    )
}
