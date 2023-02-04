package edu.temple.convoy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        findViewById<Button>(R.id.btnLogIn).setOnClickListener {
            startForResult.launch(Intent(it.context, LogInActivity::class.java))
        }
        findViewById<Button>(R.id.btnCreateAccount).setOnClickListener {
            startForResult.launch(Intent(it.context, RegisterActivity::class.java))
        }
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            if (intent?.action == RegisterActivity.RESULT_STRING) {
                Log.d("Main", "Register Complete")
            }
            else if (intent?.action == LogInActivity.RESULT_STRING) {
                Log.d("Main", "Login Complete")
            }
            finish()
        }
    }
}