package com.indianservers.aiexplorer.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()
    @Test fun criticalUserJourneys() = rule.collect("com.indianservers.aiexplorer", includeInStartupProfile = true) {
        pressHome(); startActivityAndWait()
        device.wait(Until.hasObject(By.textContains("Learning Intelligence")), 5_000)
        device.findObject(By.textContains("Learning Intelligence"))?.click()
        device.waitForIdle()
        device.findObject(By.textContains("Tutor & Experiment"))?.click()
        device.waitForIdle()
        device.pressBack()
        device.findObject(By.textContains("Exam, Reports"))?.click()
        device.waitForIdle()
    }
}
