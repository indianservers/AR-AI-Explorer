package com.indianservers.aiexplorer.macrobenchmark

import androidx.benchmark.macro.*
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class PlatformMacrobenchmarks {
 @get:Rule val rule=MacrobenchmarkRule()
 @Test fun coldStartup()=rule.measureRepeated("com.indianservers.aiexplorer",listOf(StartupTimingMetric(),FrameTimingMetric(),MemoryUsageMetric(MemoryUsageMetric.Mode.Last)),CompilationMode.Partial(BaselineProfileMode.Require),StartupMode.COLD,5,setupBlock={pressHome()}){startActivityAndWait()}
 @Test fun examNavigationAndSubmission()=rule.measureRepeated("com.indianservers.aiexplorer",listOf(FrameTimingMetric()),CompilationMode.Partial(),StartupMode.WARM,5,setupBlock={startActivityAndWait()}){device.wait(Until.hasObject(By.textContains("Learning Intelligence")),5_000);device.findObject(By.textContains("Learning Intelligence"))?.click();device.findObject(By.textContains("Exam, Reports"))?.click();device.findObject(By.text("Exam"))?.click();device.waitForIdle();device.findObject(By.text("Start"))?.click();device.findObject(By.textContains("Final review"))?.click()}
 @Test fun scrollLargePlatformSurface()=rule.measureRepeated("com.indianservers.aiexplorer",listOf(FrameTimingMetric()),CompilationMode.Partial(),StartupMode.WARM,5,setupBlock={startActivityAndWait()}){device.swipe(device.displayWidth/2,device.displayHeight*3/4,device.displayWidth/2,device.displayHeight/4,20)}
 @Test fun graphPanAndZoomFrames()=rule.measureRepeated("com.indianservers.aiexplorer",listOf(FrameTimingMetric(),MemoryUsageMetric(MemoryUsageMetric.Mode.Last)),CompilationMode.Partial(BaselineProfileMode.Require),StartupMode.WARM,5,setupBlock={startActivityAndWait();device.findObject(By.desc("Open Maths laboratory"))?.click();device.waitForIdle()}){repeat(4){device.swipe(device.displayWidth/4,device.displayHeight/2,device.displayWidth*3/4,device.displayHeight/2,18);device.swipe(device.displayWidth*3/4,device.displayHeight/2,device.displayWidth/4,device.displayHeight/2,18)}}
}
