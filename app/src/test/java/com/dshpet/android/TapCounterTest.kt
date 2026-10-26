package com.dshpet.android

import android.app.Application
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class TapCounterTest {
    private lateinit var context: Context
    private val pets = mutableListOf<Pet>()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("dsh_pet", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        pets.forEach { it.release() }
    }

    private fun newPet(): Pet {
        val spec = PetSpec("test", "测试鱼", 150, "top-left", 0, 100)
        // 只测试触摸分流；不依赖大体积动画素材的解码结果。
        val config = PetConfig(
            pets = listOf(spec), names = emptyMap(), durations = emptyMap(),
            idle = listOf("test-idle"), turn = emptyList(), drag = emptyList(),
            clicks = emptyList(), moves = emptyList(),
            moveMinDist = 60.0, moveMaxDist = 240.0, moveMargin = 20.0,
            categories = emptyList(), weightIdle = 1, weightTurn = 0, weightMove = 0,
            physics = PhysicsParams(),
        )
        return Pet(context, config, spec) { _, _, _, _ -> }.also { pet ->
            pets.add(pet)
            pet.onTap = { pet.updateTapCount(Prefs.incrementTapCount(context), true) }
        }
    }

    private fun event(view: View, action: Int, x: Float = 20f, y: Float = 20f) {
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, action, x, y, 0)
        try {
            assertTrue(view.onTouchEvent(event))
        } finally {
            event.recycle()
        }
    }

    private fun tap(pet: Pet) {
        event(pet.view, MotionEvent.ACTION_DOWN)
        event(pet.view, MotionEvent.ACTION_UP)
        // 让待完成的动画解码失效，避免测试触摸时启动随机动画链。
        pet.release()
    }

    private fun drawnText(pet: Pet): List<String> {
        pet.view.layout(0, 0, pet.sizePx, pet.heightPx)
        val canvas = Canvas(Bitmap.createBitmap(pet.sizePx, pet.heightPx, Bitmap.Config.ARGB_8888))
        pet.view.draw(canvas)
        val shadow = shadowOf(canvas)
        return (0 until shadow.textHistoryCount).map { shadow.getDrawnTextEvent(it).text }
    }

    @Test
    fun counterAppearsOnTapHidesAfterThreeSecondsAndReappearsOnNextTap() {
        val pet = newPet()
        val controller = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup()
        controller.get().setContentView(pet.view)
        repeat(8) { Prefs.incrementTapCount(context) }
        pet.updateTapCount(Prefs.tapCount(context))
        assertTrue(drawnText(pet).isEmpty())

        tap(pet)
        val firstTapMs = SystemClock.uptimeMillis()
        assertEquals(listOf(
            context.getString(R.string.tap_counter_badge, 9L),
            context.getString(R.string.tap_counter_feedback),
        ), drawnText(pet))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2900))
        assertEquals(listOf(context.getString(R.string.tap_counter_badge, 9L)), drawnText(pet))
        shadowOf(pet.view).clearWasInvalidated()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(firstTapMs + 3000 - SystemClock.uptimeMillis()))
        assertTrue(shadowOf(pet.view).wasInvalidated())
        assertTrue(drawnText(pet).isEmpty())
        assertEquals(9L, Prefs.tapCount(context))

        tap(pet)
        assertEquals(listOf(
            context.getString(R.string.tap_counter_badge, 10L),
            context.getString(R.string.tap_counter_feedback),
        ), drawnText(pet))
        controller.pause().stop().destroy()
    }

    @Test
    fun everyTapRestartsHideTimerButPassiveCountUpdatesDoNot() {
        val pet = newPet()
        val controller = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup()
        controller.get().setContentView(pet.view)
        tap(pet)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2500))
        tap(pet)
        val lastTapMs = SystemClock.uptimeMillis()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        assertTrue(drawnText(pet).contains(context.getString(R.string.tap_counter_badge, 2L)))

        pet.updateTapCount(77L)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2400))
        assertEquals(listOf(context.getString(R.string.tap_counter_badge, 77L)), drawnText(pet))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(lastTapMs + 3000 - SystemClock.uptimeMillis()))
        assertTrue(drawnText(pet).isEmpty())
        pet.updateTapCount(78L)
        assertTrue(drawnText(pet).isEmpty())
        controller.pause().stop().destroy()
    }

    @Test
    fun rapidTapsAreNeverThrottledBySoundOrSpeechSettings() {
        Prefs.setSoundOn(context, false)
        Prefs.setBubbleOn(context, false)
        val pet = newPet()
        repeat(100) { tap(pet) }
        assertEquals(100L, Prefs.tapCount(context))
        assertEquals(context.getString(R.string.tap_counter_description, 100L), pet.view.contentDescription)
    }

    @Test
    fun dragDoesNotCountEvenWhenFingerReturnsToStartingPoint() {
        val pet = newPet()
        event(pet.view, MotionEvent.ACTION_DOWN)
        event(pet.view, MotionEvent.ACTION_MOVE, 100f, 100f)
        event(pet.view, MotionEvent.ACTION_MOVE)
        event(pet.view, MotionEvent.ACTION_UP)
        pet.release()
        assertEquals(0L, Prefs.tapCount(context))
        tap(pet)
        assertEquals(1L, Prefs.tapCount(context))
    }

    @Test
    fun longPressOpensMenuWithoutCountingAndNextTapStillCounts() {
        val pet = newPet()
        var menus = 0
        pet.onLongPress = { menus++ }
        // 挂到真实的 Activity 视图树上，确保 View.postDelayed 的长按任务正常执行。
        val activity = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        activity.setContentView(pet.view)
        event(pet.view, MotionEvent.ACTION_DOWN)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        event(pet.view, MotionEvent.ACTION_UP)
        assertEquals(1, menus)
        assertEquals(0L, Prefs.tapCount(context))
        tap(pet)
        assertEquals(1L, Prefs.tapCount(context))
        activity.finish()
    }

    @Test
    fun cancelledTouchDoesNotCountAndNextTapStillCounts() {
        val pet = newPet()
        event(pet.view, MotionEvent.ACTION_DOWN)
        event(pet.view, MotionEvent.ACTION_CANCEL)
        assertEquals(0L, Prefs.tapCount(context))
        tap(pet)
        assertEquals(1L, Prefs.tapCount(context))
    }

    @Test
    fun multiplePetsShareSavedTotalAndNewPetRestoresIt() {
        val first = newPet()
        val second = newPet()
        repeat(7) { tap(first) }
        repeat(5) { tap(second) }
        assertEquals(12L, Prefs.tapCount(context))
        assertEquals(12L, context.getSharedPreferences("dsh_pet", Context.MODE_PRIVATE).getLong("tap_count", -1L))
        val recreated = newPet()
        recreated.updateTapCount(Prefs.tapCount(context))
        assertEquals(context.getString(R.string.tap_counter_description, 12L), recreated.view.contentDescription)
        tap(recreated)
        assertEquals(13L, Prefs.tapCount(context))
    }

    @Test
    fun resetPreservesOtherPreferencesAndNextTapStartsAtOne() {
        Prefs.setSizeDp(context, 200)
        Prefs.setPetCount(context, 3)
        repeat(10) { Prefs.incrementTapCount(context) }
        Prefs.resetTapCount(context)
        assertEquals(0L, Prefs.tapCount(context))
        assertEquals(200, Prefs.sizeDp(context))
        assertEquals(3, Prefs.petCount(context))
        assertEquals(1L, Prefs.incrementTapCount(context))
    }

    @Test
    fun settingsUpdateLiveAndResetRequiresConfirmation() {
        Prefs.setKeepAlive(context, false)
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val label = activity.findViewById<TextView>(R.id.tapCountLabel)
        val reset = activity.findViewById<TextView>(R.id.btnResetTapCount)
        repeat(5) { Prefs.incrementTapCount(context) }
        assertEquals(context.getString(R.string.tap_counter_total, 5L), label.text.toString())

        reset.performClick()
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(5L, Prefs.tapCount(context))

        reset.performClick()
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0L, Prefs.tapCount(context))
        assertEquals(context.getString(R.string.tap_counter_total, 0L), label.text.toString())
        controller.pause().stop().destroy()
    }

    @Test
    fun countUsesLongAndDoesNotOverflowToNegative() {
        val prefs = context.getSharedPreferences("dsh_pet", Context.MODE_PRIVATE)
        prefs.edit().putLong("tap_count", Int.MAX_VALUE.toLong()).commit()
        assertEquals(Int.MAX_VALUE.toLong() + 1L, Prefs.incrementTapCount(context))
        prefs.edit().putLong("tap_count", Long.MAX_VALUE).commit()
        assertEquals(Long.MAX_VALUE, Prefs.incrementTapCount(context))
    }
}
