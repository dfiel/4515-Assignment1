package edu.temple.convoy

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody


class RegisterActivity : AppCompatActivity() {

    private lateinit var etxtFirstName: EditText
    private lateinit var etxtLastName: EditText
    private lateinit var etxtUsername: EditText
    private lateinit var etxtPassword: EditText
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etxtFirstName = findViewById(R.id.etxtFirstName);
        etxtLastName = findViewById(R.id.etxtLastName);
        etxtUsername = findViewById(R.id.etxtUsername);
        etxtPassword = findViewById(R.id.etxtPassword);
        findViewById<Button>(R.id.btnFormRegister).setOnClickListener {
            val form = FormBody.Builder()
                .add()
        }
    }
}