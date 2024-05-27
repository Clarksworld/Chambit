package com.exchange.chambit.onboarding

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.exchange.chambit.MainActivity
import com.exchange.chambit.R


class OnboardingFragment2 : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.second_splash_layout, container, false)

        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        view.findViewById<TextView>(R.id.second_next_button).setOnClickListener {
            // Move to the next fragment
            (requireActivity() as? OnboardingActivity)?.moveToNextFragment()
        }

        val skip = view.findViewById<TextView>(R.id.skip_text)
        skip.setOnClickListener {

            val editor = sharedPreferences.edit()
            editor.putString("key", "login") // Replace "key" and "value" with your data
            editor.apply()

            startActivity(Intent(requireActivity(), MainActivity::class.java))
            activity?.finish()

        }

        return view

    }
}