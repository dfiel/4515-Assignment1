package edu.temple.convoy

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {

    companion object {
        const val RESULT_STRING = "register_success"
    }

    private lateinit var etxtFirstName: EditText
    private lateinit var etxtLastName: EditText
    private lateinit var etxtUsername: EditText
    private lateinit var etxtPassword: EditText
    private lateinit var prefs: SharedPreferences
    private val client = OkHttpClient()
    private val returnIntent = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etxtFirstName = findViewById(R.id.etxtFirstName)
        etxtLastName = findViewById(R.id.etxtLastName)
        etxtUsername = findViewById(R.id.etxtUsername)
        etxtPassword = findViewById(R.id.etxtPassword)
        prefs = getSharedPreferences(MainActivity.PREFS_FILE, MODE_PRIVATE)
        findViewById<Button>(R.id.btnFormRegister).setOnClickListener {CoroutineScope(Dispatchers.IO).launch{ register() } }
    }

    @SuppressLint("ApplySharedPref")
    private suspend fun register() {
        withContext(Dispatchers.IO) {
            val form = FormBody.Builder()
                .add("action", "REGISTER")
                .add("username", etxtUsername.text.toString())
                .add("firstname", etxtFirstName.text.toString())
                .add("lastname", etxtLastName.text.toString())
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
                if (json.getString("status") != "SUCCESS") return@withContext
                prefs.edit()
                    .putString(MainActivity.DATA_USERNAME, etxtUsername.text.toString())
                    .putString(MainActivity.DATA_FIRSTNAME, etxtFirstName.text.toString())
                    .putString(MainActivity.DATA_LASTNAME, etxtLastName.text.toString())
                    .putString(MainActivity.DATA_SESSION, json.getString("session_key"))
                    .putBoolean(MainActivity.DATA_LOGGEDIN, true)
                    .commit()
                returnIntent.action = RESULT_STRING
                setResult(RESULT_OK, returnIntent)
                finish()
            }
        }
    }
}