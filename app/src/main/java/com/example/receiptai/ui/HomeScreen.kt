package com.example.receiptai.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.receiptai.data.Expense
import java.io.File
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    expenses: List<Expense>,
    isScanning: Boolean,
    error: String?,
    onImagePicked: (Uri) -> Unit,
    onAddManually: () -> Unit,
    onDelete: (Long) -> Unit,
    onErrorShown: () -> Unit
) {
    val context = LocalContext.current
    val money = rememberMoneyFormatter()
    val snackbar = remember { SnackbarHostState() }

    // The URI the camera app will write the photo to (kept across rotation / process death)
    var pendingUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingUri
        pendingUri = null
        if (saved && uri != null) onImagePicked(uri)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImagePicked(uri)
    }

    LaunchedEffect(error) {
        if (error != null) {
            snackbar.showSnackbar(error)
            onErrorShown()
        }
    }

    fun launchCamera() {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingUri = uri
        cameraLauncher.launch(uri)
    }

    fun launchGallery() {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("SpendWiseAI") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { MonthSummary(expenses, money) }

            item {
                ScanActions(
                    enabled = !isScanning,
                    onCamera = ::launchCamera,
                    onGallery = ::launchGallery,
                    onManual = onAddManually
                )
            }

            if (isScanning) item { ScanningCard() }

            item { Text("Recent expenses", style = MaterialTheme.typography.titleMedium) }

            if (expenses.isEmpty()) {
                item {
                    Text(
                        "No expenses yet. Scan your first receipt!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(expenses, key = { it.id }) { expense ->
                ExpenseRow(expense, money, onDelete = { onDelete(expense.id) })
            }
        }
    }
}

@Composable
private fun ScanActions(
    enabled: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onManual: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCamera, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text("Take photo")
            }
            FilledTonalButton(onClick = onGallery, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text("From gallery")
            }
        }
        TextButton(onClick = onManual, enabled = enabled) { Text("Add manually") }
    }
}

@Composable
private fun ScanningCard() {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            Text("Reading receipt…")
        }
    }
}

@Composable
private fun MonthSummary(expenses: List<Expense>, money: (Double) -> String) {
    val month = remember { YearMonth.now().toString() } // e.g. "2026-10"
    val monthExpenses = remember(expenses) { expenses.filter { it.date.startsWith(month) } }
    val total = monthExpenses.sumOf { it.total }
    val byCategory = remember(monthExpenses) {
        monthExpenses
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.total } }
            .entries
            .sortedByDescending { it.value }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("This month", style = MaterialTheme.typography.labelLarge)
            Text(money(total), style = MaterialTheme.typography.headlineMedium)
            byCategory.forEach { (category, amount) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(category, style = MaterialTheme.typography.bodyMedium)
                        Text(money(amount), style = MaterialTheme.typography.bodyMedium)
                    }
                    LinearProgressIndicator(
                        progress = { if (total > 0) (amount / total).toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense, money: (Double) -> String, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    expense.store,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${expense.date} • ${expense.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(money(expense.total), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete expense")
            }
        }
    }
}
