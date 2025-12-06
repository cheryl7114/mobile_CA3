package com.example.h2now

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.h2now.ui.theme.H2nowTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private val waterViewModel: WaterViewModel by viewModels {
        WaterViewModelFactory(
            WaterRepository(),
            UserPreferencesRepository(applicationContext)
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity created")
        enableEdgeToEdge()
        setContent {
            H2nowTheme {
                val records by waterViewModel.records.collectAsState()
                val dailyGoal by waterViewModel.dailyGoal.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        tint = Color(0xFF2196F3)
                                    )
                                    Text("H2Now", fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.White,
                                titleContentColor = Color(0xFF1976D2)
                            )
                        )
                    }
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        DailySummaryCard(records = records, dailyGoal = dailyGoal.toDouble()) { newGoal ->
                            waterViewModel.setDailyGoal(newGoal)
                        }
                        AddIntakeSection { amount ->
                            waterViewModel.addWaterIntake(amount)
                        }
                        WaterIntakeList(
                            modifier = Modifier.weight(1f),
                            records = records
                        )
                    }
                }
            }
        }
    }
}

data class WaterIntakeRecord(val amount: Double, val date: Date)

val mockWaterIntakeRecords = listOf(
    WaterIntakeRecord(250.0, Date()),
    WaterIntakeRecord(500.0, Date()),
    WaterIntakeRecord(125.0, Date()),
    WaterIntakeRecord(300.0, Date()),
    WaterIntakeRecord(250.0, Date()),
)

@Composable
fun AddIntakeSection(onAddRecord: (Double) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Intake amount (ml)") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Button(
            onClick = {
                val amount = text.toDoubleOrNull()
                if (amount != null) {
                    onAddRecord(amount)
                    text = ""
                }
            },
            enabled = text.isNotBlank(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Intake")
        }
    }
}

@Composable
fun DailyGoalInput(currentGoal: Int, onGoalSet: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(currentGoal.toString()) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Set Daily Goal") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Goal (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newGoal = text.toIntOrNull()
                        if (newGoal != null) {
                            onGoalSet(newGoal)
                        }
                        showDialog = false
                    }
                ) {
                    Text("Set")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Row(
        modifier = Modifier.clickable { showDialog = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "of $currentGoal ml goal",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit Goal",
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp)
        )
    }
}


@Composable
fun DailySummaryCard(
    records: List<WaterIntakeRecord>,
    dailyGoal: Double,
    modifier: Modifier = Modifier,
    onGoalSet: (Int) -> Unit
) {
    val totalIntake = records.sumOf { it.amount }

    var animationPlayed by remember { mutableStateOf(false) }

    val targetProgress = (totalIntake / dailyGoal).toFloat().coerceIn(0f, 1f)
    val targetIntake = totalIntake.toInt()

    val animatedIntake by animateIntAsState(
        targetValue = if (animationPlayed) targetIntake else 0,
        animationSpec = tween(durationMillis = 1000),
        label = "animatedIntake"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) targetProgress else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "animatedProgress"
    )

    LaunchedEffect(key1 = totalIntake) {
        animationPlayed = true
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF2196F3),
                            Color(0xFF03A9F4)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Today's Progress",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "$animatedIntake ml",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                DailyGoalInput(currentGoal = dailyGoal.toInt(), onGoalSet = onGoalSet)
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(animatedProgress * 100).toInt()}% complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun WaterIntakeCard(record: WaterIntakeRecord, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "${record.amount.toInt()} ml",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    Text(
                        text = sdf.format(record.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE8F5E9)
            ) {
                Text(
                    text = when {
                        record.amount >= 500 -> "Great!"
                        record.amount >= 250 -> "Good!"
                        else -> "Keep Up!"
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun WaterIntakeList(modifier: Modifier = Modifier, records: List<WaterIntakeRecord>) {
    LazyColumn(
        modifier = modifier
            .background(Color(0xFFF5F5F5))
            .fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            Text(
                text = "Recent Intake",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        items(records) { record ->
            WaterIntakeCard(record = record)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WaterIntakeListPreview() {
    H2nowTheme {
        Column {
            DailySummaryCard(records = mockWaterIntakeRecords, dailyGoal = 2000.0) {}
            AddIntakeSection { }
            WaterIntakeList(records = mockWaterIntakeRecords)
        }
    }
}
