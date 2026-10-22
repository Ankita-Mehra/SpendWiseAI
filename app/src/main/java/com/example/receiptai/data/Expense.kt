package com.example.receiptai.data

data class Expense(
    val id: Long,
    val store: String,
    val date: String,      // yyyy-MM-dd
    val total: Double,
    val category: String
)

object Categories {
    val all = listOf(
        "Food", "Groceries", "Transport", "Shopping",
        "Health", "Bills", "Entertainment", "Other"
    )
}
