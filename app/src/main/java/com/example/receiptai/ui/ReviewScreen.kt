package com.example.receiptai.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.receiptai.ai.ScannedReceipt
import com.example.receiptai.data.Categories
import java.time.LocalDate
import java.util.Locale

/** The AI result is a suggestion: the user reviews and corrects it before saving. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    initial: ScannedReceipt,
    onSave: (store: String, date: String, total: Double, category: String) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler(onBack = onCancel)

    var store by rememberSaveable { mutableStateOf(initial.store) }
    var date by rememberSaveable { mutableStateOf(initial.date) }
    var totalText by rememberSaveable {
        mutableStateOf(if (initial.total > 0) String.format(Locale.US, "%.2f", initial.total) else "")
    }
    var category by rememberSaveable { mutableStateOf(initial.category) }

    val total = totalText.toDoubleOrNull()
    val dateValid = runCatching { LocalDate.parse(date) }.isSuccess
    val canSave = store.isNotBlank() && total != null && total >= 0 && dateValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review receipt") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Check what the AI found and fix anything that's wrong.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = store,
                onValueChange = { store = it },
                label = { Text("Store") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date (yyyy-MM-dd)") },
                singleLine = true,
                isError = !dateValid,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = totalText,
                onValueChange = { totalText = it },
                label = { Text("Total") },
                singleLine = true,
                isError = totalText.isNotEmpty() && total == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Category", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Categories.all.forEach { option ->
                    FilterChip(
                        selected = option == category,
                        onClick = { category = option },
                        label = { Text(option) }
                    )
                }
            }

            Button(
                onClick = { onSave(store, date, total ?: 0.0, category) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save expense") }

            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Discard")
            }
        }
    }
}
