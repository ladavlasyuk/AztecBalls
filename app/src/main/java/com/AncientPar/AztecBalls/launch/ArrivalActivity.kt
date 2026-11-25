package com.AncientPar.AztecBalls.launch

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.AncientPar.AztecBalls.MainActivity
import com.AncientPar.AztecBalls.R
import kotlinx.coroutines.launch

class ArrivalActivity : ComponentActivity() {
    private lateinit var panel: LinearLayout
    private lateinit var router: ArrivalRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
        }
        setContentView(panel)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
        router = ArrivalRouter(applicationContext, TempleGateway(applicationContext)::requestRoute)
        resolveArrival()
    }

    private fun resolveArrival() {
        panel.removeAllViews()
        panel.addView(ProgressBar(this))
        lifecycleScope.launch {
            when (val arrival = router.resolve()) {
                is Arrival.Website -> showTemplePage(arrival.address)
                Arrival.Game -> {
                    startActivity(Intent(this@ArrivalActivity, MainActivity::class.java))
                    finish()
                }
                Arrival.Unavailable -> showRetry()
            }
        }
    }

    private fun showRetry() {
        panel.removeAllViews()
        for (message in listOf(R.string.temple_unavailable, R.string.temple_connection_hint)) {
            panel.addView(TextView(this).apply {
                setText(message)
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
            })
        }
        panel.addView(Button(this).apply {
            setText(R.string.temple_retry)
            setOnClickListener { resolveArrival() }
        })
    }
}
