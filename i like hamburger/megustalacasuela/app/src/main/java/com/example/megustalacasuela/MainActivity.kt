package com.example.megustalacasuela

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
//import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // aqui comienza el infierno en la tierra//
        /* variables */

        val etUsuario = findViewById<EditText>(R.id.username)
        val etPassword = findViewById<EditText>(R.id.password)
        val etBoton = findViewById<Button>(R.id.subir)

        etBoton.setOnClickListener {
            val Usuario = etUsuario.text.toString()
            val Password = etPassword.text.toString()

            if (Usuario.isEmpty() || Password.isEmpty()) {
                Toast.makeText(this, "ingrese usuario y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            when {

                Usuario == "admin" && Password == "1234" -> {
                    startActivity(Intent(this, adminactivity::class.java))
                    Toast.makeText(this, "Welcome Adminitrator", Toast.LENGTH_SHORT).show()
                }

                Usuario == "user" && Password == "abcd" -> {
                    startActivity(Intent(this, useractivity::class.java))
                    Toast.makeText(this, "Welcome User", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Usuario o contraseña incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}