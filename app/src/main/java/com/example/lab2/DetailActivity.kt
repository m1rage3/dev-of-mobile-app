package com.example.lab2

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DetailActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        tvResult = findViewById(R.id.tvResult)
        btnBack = findViewById(R.id.btnBack)

        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Нет данных"
        tvResult.text = "Получено: $message"

        btnBack.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }
}
