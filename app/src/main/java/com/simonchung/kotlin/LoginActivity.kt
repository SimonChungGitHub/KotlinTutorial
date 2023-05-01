package com.simonchung.kotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.google.android.material.snackbar.Snackbar
import com.simonchung.kotlin.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    lateinit var username : EditText
    lateinit var password: EditText
    lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.loginButton.setOnClickListener {
            if (binding.username.text.toString() == "user" && binding.password.text.toString() == "1111") {
                Snackbar.make(binding.root, "Login Successful!", Snackbar.LENGTH_SHORT).show()
                finish()
            } else {
                Snackbar.make(binding.root, "Login Failed!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}