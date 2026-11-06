package com.example.lab2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etAge: EditText
    private lateinit var cbAgree: CheckBox
    private lateinit var btnSubmit: Button
    private lateinit var etMessage: EditText
    private lateinit var btnGo: Button

    // результат со второго экрана
    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Вернулись с экрана 2", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etName = findViewById(R.id.etName)
        etAge = findViewById(R.id.etAge)
        cbAgree = findViewById(R.id.cbAgree)
        btnSubmit = findViewById(R.id.btnSubmit)
        etMessage = findViewById(R.id.etMessage)
        btnGo = findViewById(R.id.btnGo)

        // задание 1
        btnSubmit.setOnClickListener {
            val name = etName.text.toString().trim()
            val ageText = etAge.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Введите имя", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ageText.isEmpty()) {
                Toast.makeText(this, "Введите возраст", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val age = ageText.toIntOrNull()
            if (age == null || age < 0 || age > 150) {
                Toast.makeText(this, "Некорректный возраст", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!cbAgree.isChecked) {
                Toast.makeText(this, "Примите правила", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Имя: $name, Возраст: $age", Toast.LENGTH_LONG).show()
        }

        // задание 2
        btnGo.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Введите текст", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("EXTRA_MESSAGE", text)
            detailLauncher.launch(intent)
        }
    }
}
