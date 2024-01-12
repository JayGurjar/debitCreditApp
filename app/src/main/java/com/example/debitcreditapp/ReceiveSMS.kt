package com.example.debitcreditapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioTrack.Builder
import android.provider.Telephony
import android.widget.TextView
import android.widget.Toast
import java.util.Objects

class ReceiveSMS (
    private val applicationContext: Context,
    private val creditedList : MutableList<SmsItem>,
    private val debitedList : MutableList<SmsItem>,
    private val debitedAdapter: TransactionAdapter,
    private val creditedAdapter: TransactionAdapter,
    private var onUpdateTotal : (String, Double) -> Unit,
): BroadcastReceiver() {
    private var totalDebit : Double = 0.0
    private var totalCredit : Double = 0.0
    private fun extractTransactionInfo(text: String): Map<String, String>? {
        val regexPattern = "(?i)(credited|debited)\\s*\\$?\\s*([0-9,.]+)".toRegex()

        val matchResult = regexPattern.find(text)

        val transactionType = matchResult?.groupValues?.get(1)
        val amount = matchResult?.groupValues?.get(2)


        if (!transactionType.isNullOrBlank() && !amount.isNullOrBlank()) {
            return mapOf("transaction_type" to transactionType, "amount" to amount)
        }

        return null
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        if(Telephony.Sms.Intents.SMS_RECEIVED_ACTION == p1?.action){
            for (sms in Telephony.Sms.Intents.getMessagesFromIntent(p1)){
                val transactionInfo = extractTransactionInfo(sms.displayMessageBody)
                if(transactionInfo != null) {
                    val number : String = transactionInfo["amount"]?.takeIf { it.isNotBlank() } ?: ""
                    val transactionType : String? = transactionInfo["transaction_type"]

                    val smsItem = SmsItem(number)

                    if (transactionType == "credited") {
                        totalCredit += number.toInt()
                        creditedList.add(smsItem)
                        onUpdateTotal(transactionType, totalCredit)
                        creditedAdapter.notifyDataSetChanged()
                    } else {
                        totalDebit += number.toInt()
                        debitedList.add(smsItem)
                        onUpdateTotal("debited", totalDebit)
                        debitedAdapter.notifyDataSetChanged()
                    }
                }

            }
        }
    }

}