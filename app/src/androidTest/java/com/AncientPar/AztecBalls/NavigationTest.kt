package com.AncientPar.AztecBalls

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @Test fun backReturnsToHomeAndKeepsItVisible() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText(R.string.rules)).perform(click())
            onView(withText(R.string.rule_match)).check(matches(isDisplayed()))
            pressBackUnconditionally()
            pressBackUnconditionally()
            onView(withText(R.string.records)).perform(click())
            onView(withText(R.string.wins)).check(matches(isDisplayed()))
        }
    }
}
