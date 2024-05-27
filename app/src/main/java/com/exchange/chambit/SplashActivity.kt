package com.exchange.chambit

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import com.exchange.chambit.onboarding.OnboardingActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)


        setContentView(R.layout.activity_splash)


        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // Retrieve data from SharedPreferences
        val retrievedData = sharedPreferences.getString("key", "default_value") // Provide a default value if the key is not found




        supportActionBar?.hide();


        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        Handler().postDelayed({

            if (retrievedData == "login"){


                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()

            }else{

                val intent = Intent(this, OnboardingActivity::class.java)
                startActivity(intent)
                finish()

            }



//            Toast.makeText(this, "Main Splsh", Toast.LENGTH_SHORT).show()


        },2000

        )
    }
}