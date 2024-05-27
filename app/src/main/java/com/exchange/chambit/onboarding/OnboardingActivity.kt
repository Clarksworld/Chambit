package com.exchange.chambit.onboarding


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager2.widget.ViewPager2
import com.exchange.chambit.MainActivity
import com.exchange.chambit.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)


        setContentView(R.layout.activity_onboarding)

        supportActionBar?.hide();


        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        val adapter = OnboardingPagerAdapter(this)
        viewPager.adapter = adapter

        // Connect ViewPager with TabLayout
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        // Customize tab layout to show dots
        for (i in 0 until adapter.itemCount) {
            val tab = tabLayout.getTabAt(i)
            tab?.customView = LayoutInflater.from(this).inflate(R.layout.tab_layout_dot, null)
        }
    }

    fun moveToNextFragment() {
        val currentItem = viewPager.currentItem
        val nextItem = currentItem + 1
        if (nextItem < viewPager.adapter?.itemCount ?: 0) {
            viewPager.currentItem = nextItem
        } else {
            // Do something when reaching the last fragment
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}


