package com.example.debitcreditapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter (
    private val transactions : MutableList<SmsItem>,
) : RecyclerView.Adapter<TransactionAdapter.AccountViewHolder>() {
    class AccountViewHolder(accountView : View) : RecyclerView.ViewHolder(accountView) {
        val numberTextView: TextView = accountView.findViewById(R.id.tvTransaction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        return AccountViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.transaction,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val item = transactions[position]
        holder.numberTextView.text = "$${String.format("%.2f", item.amount.toDoubleOrNull())}"
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    fun calculateTotal(): Double {
        var total = 0.0
        for (item in transactions) {
            total += item.amount.toDoubleOrNull() ?: 0.0
        }
        return total
    }
}