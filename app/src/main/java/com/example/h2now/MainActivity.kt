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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.h2now.ui.theme.H2nowTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val waterViewModel: WaterViewModel by viewModels {
        WaterViewModelFactory(
            WaterRepository(database.waterIntakeDao()),
            UserPreferencesRepository(applicationContext)
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity created")
        enableEdgeToEdge()
        setContent {
            val darkMode by waterViewModel.darkModeEnabled.collectAsState()

            H2nowTheme(darkTheme = darkMode) {
                val records by waterViewModel.records.collectAsState()
                val dailyGoal by waterViewModel.dailyGoal.collectAsState()
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                var selectedRecordForEditing by remember { mutableStateOf<WaterIntakeRecord?>(null) }

                if (selectedRecordForEditing != null) {
                    EditIntakeDialog(
                        record = selectedRecordForEditing!!,
                        onDismiss = { selectedRecordForEditing = null },
                        onSave = {
                            waterViewModel.updateWaterIntake(it)
                            selectedRecordForEditing = null
                        },
                        onDelete = {
                            waterViewModel.deleteWaterIntake(it)
                            selectedRecordForEditing = null
                        }
                    )
                }

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
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        when (currentRoute) {
                                            "settings" -> "Settings"
                                            "tips" -> "Hydration Tips"
                                            else -> "H2Now"
                                        }, fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            actions = {
                                if (currentRoute == "main") {
                                    IconButton(onClick = { navController.navigate("settings") }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                }
                            },
                            navigationIcon = {
                                if (currentRoute != "main") {
                                    IconButton(onClick = { navController.navigateUp() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        BottomNavigationBar(navController = navController)
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("main") {
                            Column {
                                DailySummaryCard(records = records, dailyGoal = dailyGoal.toDouble()) { newGoal ->
                                    waterViewModel.setDailyGoal(newGoal)
                                }
                                AddIntakeSection { amount ->
                                    waterViewModel.addWaterIntake(amount)
                                }
                                WaterIntakeList(
                                    modifier = Modifier.weight(1f),
                                    records = records,
                                    onRecordClick = { selectedRecordForEditing = it }
                                )
                            }
                        }
                        composable("settings") {
                            SettingsScreen(waterViewModel = waterViewModel)
                        }
                        composable("tips") {
                            TipsScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: androidx.navigation.NavController) {
    NavigationBar {
        val items = listOf(
            NavigationItem("main", Icons.Default.Home, "Home"),
            NavigationItem("tips", Icons.Default.Info, "Tips")
        )
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationRoute!!) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class NavigationItem(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String)

val mockWaterIntakeRecords = listOf(
    WaterIntakeRecord(amount = 250.0, date = Date()),
    WaterIntakeRecord(amount = 500.0, date = Date()),
    WaterIntakeRecord(amount = 125.0, date = Date()),
    WaterIntakeRecord(amount = 300.0, date = Date()),
    WaterIntakeRecord(amount = 250.0, date = Date()),
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

    val targetProgress = if (dailyGoal > 0) (totalIntake / dailyGoal).toFloat().coerceIn(0f, 1f) else 0f
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
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
fun WaterIntakeCard(
    record: WaterIntakeRecord,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
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
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "${record.amount.toInt()} ml",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    Text(
                        text = sdf.format(record.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = when {
                            record.amount >= 500 -> "Great!"
                            record.amount >= 250 -> "Good"
                            else -> "Keep Going"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Record",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(18.dp)
                        .clickable { onClick() }
                )
            }
        }
    }
}

@Composable
fun WaterIntakeList(
    modifier: Modifier = Modifier,
    records: List<WaterIntakeRecord>,
    onRecordClick: (WaterIntakeRecord) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            Text(
                text = if (records.isEmpty()) "No intake recorded yet" else "Recent Intake",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        items(records) { record ->
            WaterIntakeCard(record = record) { onRecordClick(record) }
        }
    }
}

@Composable
fun EditIntakeDialog(
    record: WaterIntakeRecord,
    onDismiss: () -> Unit,
    onSave: (WaterIntakeRecord) -> Unit,
    onDelete: (WaterIntakeRecord) -> Unit
) {
    var text by remember { mutableStateOf(record.amount.toInt().toString()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Delete Intake?") },
            text = { Text("Are you sure you want to delete this ${record.amount.toInt()} ml intake record?") },
            confirmButton = {
                Button(
                    onClick = { onDelete(record) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Edit Water Intake",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Amount (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.WaterDrop, contentDescription = null)
                    }
                )

                val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                Text(
                    text = "Recorded: ${sdf.format(record.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newAmount = text.toDoubleOrNull()
                    if (newAmount != null && newAmount > 0) {
                        onSave(record.copy(amount = newAmount))
                    }
                },
                enabled = text.toDoubleOrNull()?.let { it > 0 } ?: false
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun WaterIntakeListPreview() {
    H2nowTheme {
        Column {
            DailySummaryCard(records = mockWaterIntakeRecords, dailyGoal = 2000.0) {}
            AddIntakeSection { }
            WaterIntakeList(records = mockWaterIntakeRecords) {}
        }
    }
}
