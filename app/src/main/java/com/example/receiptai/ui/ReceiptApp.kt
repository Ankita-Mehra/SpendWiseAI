package com.example.receiptai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat

/** Root composable: shows the review form when a scan is ready, otherwise the home screen. */
@Composable
fun ReceiptApp(vm: ExpenseViewModel = viewModel()) {
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val scanState by vm.scanState.collectAsStateWithLifecycle()

    when (val state = scanState) {
        is ScanState.Ready -> ReviewScreen(
            initial = state.receipt,
            onSave = vm::save,
            onCancel = vm::dismissScan
        )
        else -> HomeScreen(
            expenses = expenses,
            isScanning = state is ScanState.Scanning,
            error = (state as? ScanState.Failed)?.message,
            onImagePicked = vm::scan,
            onAddManually = vm::startManualEntry,
            onDelete = vm::delete,
            onErrorShown = vm::dismissScan
        )
    }
}

/** Formats numbers using the device's own currency/locale. */
@Composable
fun rememberMoneyFormatter(): (Double) -> String {
    val format = remember { NumberFormat.getCurrencyInstance() }
    return { amount -> format.format(amount) }
}
