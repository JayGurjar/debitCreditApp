package com.example.debitcreditapp

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.IntentFilter
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val creditedList : MutableList<SmsItem> = mutableListOf<SmsItem>()
    private val debitedList : MutableList<SmsItem> = mutableListOf<SmsItem>()
    private val debitedAdapter: TransactionAdapter = TransactionAdapter(debitedList)
    private val creditedAdapter: TransactionAdapter = TransactionAdapter(creditedList)
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
        }else
            receiveMsg();
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 101 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }){
            receiveMsg();
        }
    }

    private val onUpdateTotal : (String,Double) -> Unit = { it: String, total: Double ->
        if(it == "credited"){
            val totalCreditedView = findViewById<TextView>(R.id.tvTotalCredited)
            val totalCreditedText = "Total Credited: $${String.format("%.2f", total)}"
            totalCreditedView.text = totalCreditedText
        }else{
            val totalDebitedView = findViewById<TextView>(R.id.tvTotalDebited)
            val totalDebitedText = "Total Debited: $${String.format("%.2f", total)}"
            totalDebitedView.text = totalDebitedText
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