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
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
    private val MONTH_IN_MILLIS = 30 * DAY_IN_MILLIS
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
            return mapOf("transaction_type" to transactionType.lowercase(), "amount" to amount)
        }

        return null
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerCreditView : RecyclerView = findViewById(R.id.rvCreditList)
        val recyclerDebitView : RecyclerView = findViewById(R.id.rvDebitList);
        val spinnerView : Spinner = findViewById(R.id.spinFilterTime);

        ArrayAdapter.createFromResource(
            this,
            R.array.filter_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerView.adapter = adapter
            spinnerView.setSelection(adapter.getPosition("Last Month"));
        }

        recyclerCreditView.layoutManager = LinearLayoutManager(this)
        recyclerCreditView.adapter = creditedAdapter;

        recyclerDebitView.layoutManager = LinearLayoutManager(this)
        recyclerDebitView.adapter = debitedAdapter;

        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),101);
        }else{

            spinnerView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    val selectedFilter = p0?.getItemAtPosition(p2).toString();
                    creditedList.clear()
                    debitedList.clear()
                    fetchMessages(selectedFilter);
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                }

            }
            receiveMsg();
        }

    }

    @SuppressLint("Range")
    private fun fetchMessages(timeFrame : String) {
        val contentResolver: ContentResolver = this.contentResolver
        val uri: Uri = Telephony.Sms.CONTENT_URI

        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)

        cursor?.use { cursor1 ->
            while (cursor1.moveToNext()) {
                val messageBody = cursor1.getString(cursor1.getColumnIndex(Telephony.Sms.BODY))
                val dateSentMilli = cursor1.getLong(cursor1.getColumnIndex(Telephony.Sms.DATE))
                val transactionInfo = extractTransactionInfo(messageBody)
                val number : String = transactionInfo?.get("amount")?.takeIf { it.isNotBlank() } ?: ""
                val transactionType : String? = transactionInfo?.get("transaction_type")
                if (isMessageInTimeFrame(dateSentMilli, timeFrame)) {
                    if (transactionInfo != null) {
                        if (transactionType == "credited") {
                            creditedList.add(SmsItem(number))
                        } else {
                            debitedList.add(SmsItem(number))
                        }
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

    private fun isMessageInTimeFrame(dateSentMillis: Long, timeFrame: String): Boolean {

        val currentTimeMillis = System.currentTimeMillis()
        val startTimeMillis = when (timeFrame) {
            "Last Month" -> currentTimeMillis - MONTH_IN_MILLIS
            "Last 3 Months" -> currentTimeMillis - 3 * MONTH_IN_MILLIS
            else -> 0L // "All" - start from the beginning
        }

        return dateSentMillis >= startTimeMillis
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 101 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }){
            fetchMessages("Last Month");
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