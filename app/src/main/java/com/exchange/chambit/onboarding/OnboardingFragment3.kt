package com.exchange.chambit.onboarding

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.exchange.chambit.MainActivity
import com.exchange.chambit.R

class OnboardingFragment3 : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.third_splash_layout, container, false)

        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        view.findViewById<Button>(R.id.third_next_button).setOnClickListener {

            val editor = sharedPreferences.edit()
            editor.putString("key", "login") // Replace "key" and "value" with your data
            editor.apply()
            // Move to the next fragment
            startActivity(Intent(requireActivity(), MainActivity::class.java))
            activity?.finish()


        }

        return view
    }
}
