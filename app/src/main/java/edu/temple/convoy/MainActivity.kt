package edu.temple.convoy

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    companion object {
        private const val BASE_URL = "https://kamorris.com/lab/convoy/"
        const val ACCOUNT_URL = BASE_URL + "account.php"
        const val CONVOY_URL = BASE_URL + "convoy.php"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnLogIn).setOnClickListener {
            it.context.startActivity(Intent(it.context, LogInActivity::class.java))
        }
        findViewById<Button>(R.id.btnCreateAccount).setOnClickListener {
            it.context.startActivity(Intent(it.context, RegisterActivity::class.java))
        }
    }
}