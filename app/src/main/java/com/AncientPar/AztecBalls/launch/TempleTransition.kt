package com.AncientPar.AztecBalls.launch

import android.app.Activity
import android.content.Intent
import android.os.Build
import com.AncientPar.AztecBalls.R

internal fun Activity.showTemplePage(pageUrl: String) {
    val browserIntent = Intent(this, TempleWebActivity::class.java).apply {
        putExtra(TempleWebActivity.START_ADDRESS, pageUrl)
    }
    startActivity(browserIntent)
    animatePageChange(this, closing = false)
    finish()
    animatePageChange(this, closing = true)
}

@Suppress("DEPRECATION")
private fun animatePageChange(activity: Activity, closing: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val type = if (closing) {
            Activity.OVERRIDE_TRANSITION_CLOSE
        } else {
            Activity.OVERRIDE_TRANSITION_OPEN
        }
        activity.overrideActivityTransition(
            type,
            R.anim.temple_appear,
            R.anim.temple_disappear
        )
    } else {
        activity.overridePendingTransition(R.anim.temple_appear, R.anim.temple_disappear)
    }
}
