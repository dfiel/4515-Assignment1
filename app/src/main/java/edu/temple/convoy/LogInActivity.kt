package edu.temple.convoy

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class LogInActivity : AppCompatActivity() {

    companion object {
        const val RESULT_STRING = "login_success"
    }

    private lateinit var etxtUsername: EditText
    private lateinit var etxtPassword: EditText
    private lateinit var prefs: SharedPreferences
    private val client = OkHttpClient()
    private val returnIntent = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etxtUsername = findViewById(R.id.etxtUsername)
        etxtPassword = findViewById(R.id.etxtPassword)
        prefs = getSharedPreferences(MainActivity.PREFS_FILE, MODE_PRIVATE)
        findViewById<Button>(R.id.btnFormLogIn).setOnClickListener { CoroutineScope(Dispatchers.IO).launch{ login() } }
    }

    @SuppressLint("ApplySharedPref")
    private fun login() {
        val form = FormBody.Builder()
            .add("action", "LOGIN")
            .add("username", etxtUsername.text.toString())
            .add("password", etxtPassword.text.toString())
            .build()
        val request = Request.Builder()
            .url(MainActivity.ACCOUNT_URL)
            .post(form)
            .build()
        val response = client.newCall(request).execute()
        if (response.code == 200) {
            val body = response.body!!.string()
            Log.d("Request Response", body)
            val json = JSONObject(body)
            if (json.getString("status") != "SUCCESS") return
            prefs.edit()
                .putString(MainActivity.DATA_USERNAME, etxtUsername.text.toString())
                .putString(MainActivity.DATA_SESSION, json.getString("session_key"))
                .putBoolean(MainActivity.DATA_LOGGEDIN, true)
                .commit()
            returnIntent.action = RESULT_STRING
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }
}