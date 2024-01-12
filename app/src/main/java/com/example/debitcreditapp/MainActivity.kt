package com.example.debitcreditapp

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val creditedList : MutableList<SmsItem> = mutableListOf<SmsItem>()
    private val debitedList : MutableList<SmsItem> = mutableListOf<SmsItem>()
    private val debitedAdapter: TransactionAdapter = TransactionAdapter(debitedList)
    private val creditedAdapter: TransactionAdapter = TransactionAdapter(creditedList)

    private val onUpdateTotal : (String) -> Unit = { it: String, ->
        if(it == "credited"){
            val totalCreditedView = findViewById<TextView>(R.id.tvTotalCredited)
            val total = creditedAdapter.calculateTotal();
            val totalCreditedText = "Total: $${String.format("%.2f", total)}"
            totalCreditedView.text = totalCreditedText
        }else{
            val totalDebitedView = findViewById<TextView>(R.id.tvTotalDebited)
            val total = debitedAdapter.calculateTotal();
            val totalDebitedText = "Total: $${String.format("%.2f", total)}"
            totalDebitedView.text = totalDebitedText
        }
    }

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerCreditView : RecyclerView = findViewById(R.id.rvCreditList)
        val recyclerDebitView : RecyclerView = findViewById(R.id.rvDebitList);

        recyclerCreditView.layoutManager = LinearLayoutManager(this)
        recyclerCreditView.adapter = creditedAdapter;

        recyclerDebitView.layoutManager = LinearLayoutManager(this)
        recyclerDebitView.adapter = debitedAdapter;

        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),101);
        }else{
            fetchMessages();
            receiveMsg();
        }
    }

    @SuppressLint("Range")
    private fun fetchMessages() {
        val contentResolver: ContentResolver = this.contentResolver
        val uri: Uri = Telephony.Sms.CONTENT_URI

        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)

        cursor?.use { cursor1 ->
            while (cursor1.moveToNext()) {
                val messageBody = cursor1.getString(cursor1.getColumnIndex(Telephony.Sms.BODY))
                val transactionInfo = extractTransactionInfo(messageBody)
                val number : String = transactionInfo?.get("amount")?.takeIf { it.isNotBlank() } ?: ""
                val transactionType : String? = transactionInfo?.get("transaction_type")
                if(transactionInfo != null) {
                    if (transactionType == "credited") {
                        creditedList.add(SmsItem(number))
                    } else {
                        debitedList.add(SmsItem(number))
                    }
                }
            }

            onUpdateTotal("credited");
            onUpdateTotal("debited");
            // Notify adapters that data has changed
            creditedAdapter.notifyDataSetChanged()
            debitedAdapter.notifyDataSetChanged()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 101 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }){
            fetchMessages();
            receiveMsg();
        }
    }

    private fun receiveMsg() {
        val receiveSMSObject : ReceiveSMS = ReceiveSMS(applicationContext,
            creditedList,
            debitedList,
            debitedAdapter,
            creditedAdapter,
            onUpdateTotal
        );
        registerReceiver(receiveSMSObject, IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
    }

}