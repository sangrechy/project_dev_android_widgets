package com.sangrechy.devwidgets.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.sangrechy.devwidgets.ui.main.MainScreen]. */
class MainScreenTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {
        composeTestRule.setContent { MainScreen() }
    }

    @Test
    fun configurationSections_exist() {
        composeTestRule.onNodeWithText("Developer Accounts & Linking").assertExists()
        composeTestRule.onNodeWithText("Live Home Screen Widget Preview").assertExists()
        composeTestRule.onNodeWithText("LeetCode Profile").assertExists()
        composeTestRule.onNodeWithText("GitHub Profile").assertExists()
    }
}
