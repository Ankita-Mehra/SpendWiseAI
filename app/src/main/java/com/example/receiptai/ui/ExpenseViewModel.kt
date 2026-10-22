package com.example.receiptai.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.receiptai.ai.ReceiptScanException
import com.example.receiptai.ai.ReceiptScanner
import com.example.receiptai.ai.ScannedReceipt
import com.example.receiptai.data.Expense
import com.example.receiptai.data.ExpenseStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Ready(val receipt: ScannedReceipt) : ScanState   // show the review form
    data class Failed(val message: String) : ScanState
}

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = ExpenseStore(app)
    private val scanner = ReceiptScanner(app)

    val expenses: StateFlow<List<Expense>> = storage.expenses

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    init {
        viewModelScope.launch { storage.load() }
    }

    fun scan(uri: Uri) {
        if (_scanState.value is ScanState.Scanning) return
        _scanState.value = ScanState.Scanning
        viewModelScope.launch {
            _scanState.value = try {
                ScanState.Ready(scanner.scan(uri))
            } catch (e: CancellationException) {
                throw e
            } catch (e: ReceiptScanException) {
                ScanState.Failed(e.message ?: "Couldn't read the receipt.")
            } catch (e: Exception) {
                ScanState.Failed("Something went wrong: ${e.localizedMessage ?: e.javaClass.simpleName}")
            }
        }
    }

    fun startManualEntry() {
        _scanState.value = ScanState.Ready(
            ScannedReceipt(store = "", date = LocalDate.now().toString(), total = 0.0, category = "Other")
        )
    }

    fun save(store: String, date: String, total: Double, category: String) {
        viewModelScope.launch { storage.add(store.trim(), date, total, category) }
        _scanState.value = ScanState.Idle
    }

    fun delete(id: Long) {
        viewModelScope.launch { storage.delete(id) }
    }

    /** Goes back to the home screen (used for cancel, and after an error has been shown). */
    fun dismissScan() {
        _scanState.value = ScanState.Idle
    }
}
