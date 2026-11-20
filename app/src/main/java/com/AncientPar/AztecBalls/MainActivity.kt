package com.AncientPar.AztecBalls

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.*
import java.util.Locale
import kotlin.math.*

private enum class Screen { HOME, RULES, RECORDS, GAME, RESULTS }

class MainActivity : ComponentActivity() {
    private lateinit var art: Artwork
    private lateinit var root: FrameLayout
    private lateinit var content: FrameLayout
    private val prefs by lazy { getSharedPreferences("temple", MODE_PRIVATE) }
    private var screen = Screen.HOME
    private var round: Round? = null
    private var field: GameView? = null
    private var overlay: FrameLayout? = null
    private lateinit var progress: TextView
    private lateinit var points: TextView
    private lateinit var preview: View
    private val fill = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
    private fun dp(n: Int) = (n*resources.displayMetrics.density).roundToInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.statusBars())
        art = Artwork(this)
        root = FrameLayout(this)
        content = FrameLayout(this)
        root.addView(Backdrop(this,art))
        root.addView(content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            content.setPadding(bars.left,bars.top,bars.right,bars.bottom)
            insets
        }
        setContentView(root)
        onBackPressedDispatcher.addCallback(this,object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (screen) {
                    Screen.GAME -> if (overlay == null) pauseGame() else resumeGame()
                    Screen.HOME -> Unit
                    else -> home()
                }
            }
        })
        when (savedInstanceState?.getString("screen")) {
            Screen.GAME.name -> startGame(true)
            Screen.RESULTS.name -> results()
            Screen.RECORDS.name -> records()
            Screen.RULES.name -> rules()
            else -> home()
        }
    }

    private fun clear(to: Screen) {
        field?.paused = true
        field = null
        overlay = null
        content.removeAllViews()
        screen = to
    }

    private fun shape(color: Int, border: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1),border)
    }

    private fun text(label: String, size: Float, color: Int = cream, bold: Boolean = true) = TextView(this).apply {
        text = label
        setTextSize(TypedValue.COMPLEX_UNIT_SP,size)
        setTextColor(color)
        typeface = Typeface.create(if (bold) "sans-serif-condensed" else "sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        gravity = Gravity.CENTER
        includeFontPadding = false
    }

    private fun button(id: Int, primary: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = getString(id)
        setOnClickListener { action() }
        isAllCaps = false
        setTextColor(if (primary) ink else cream)
        typeface = Typeface.create("sans-serif-medium",Typeface.BOLD)
        textSize = 17f
        letterSpacing = .025f
        setPadding(dp(8),0,dp(8),0)
        backgroundTintList = null
        background = RippleDrawable(ColorStateList.valueOf(0x447b98ef),shape(if(primary) gold else ink,if(primary) cream else 0xff9baacc.toInt(),16),null)
    }

    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
    private fun LinearLayout.row(view: View, height: Int = wrap, top: Int = 0) {
        addView(view,LinearLayout.LayoutParams(fill,if (height < 0) height else dp(height)).apply { topMargin = dp(top) })
    }

    private fun page(): LinearLayout {
        val scroll = ScrollView(this).apply { isFillViewport = true; isVerticalScrollBarEnabled = false }
        content.addView(scroll,FrameLayout.LayoutParams(fill,fill))
        val holder = column()
        scroll.addView(holder)
        val panel = column()
        val w = min(resources.displayMetrics.widthPixels-dp(40),dp(370)).coerceAtLeast(dp(160))
        holder.addView(panel,LinearLayout.LayoutParams(w,wrap).apply { topMargin=dp(20); bottomMargin=dp(20) })
        panel.background = shape(0xf2152c70.toInt(),0xffffcf78.toInt(),26)
        panel.setPadding(dp(22),dp(22),dp(22),dp(22))
        return panel
    }

    private fun home() {
        clear(Screen.HOME)
        round = null
        val panel = page()
        panel.row(Emblem(this,art),158)
        val title = text(getString(R.string.home_title),55f)
        title.typeface = Typeface.create("sans-serif-black",Typeface.BOLD)
        title.letterSpacing = .07f
        title.setLineSpacing(0f,.84f)
        title.setShadowLayer(dp(1).toFloat(),0f,dp(3).toFloat(),ink)
        panel.row(title,wrap,4)
        panel.row(button(if (prefs.contains("round")) R.string.continue_round else R.string.play,true) { startGame(prefs.contains("round")) },54,24)
        val nav = LinearLayout(this)
        listOf(button(R.string.rules) { rules() },button(R.string.records) { records() }).forEachIndexed { index, b ->
            nav.addView(b,LinearLayout.LayoutParams(0,fill,1f).apply { if(index==1) leftMargin=dp(8) })
        }
        panel.row(nav,50,10)
    }

    private fun title(panel: LinearLayout, id: Int) {
        panel.row(text(getString(id),36f),wrap,6)
    }

    private fun rules() {
        clear(Screen.RULES)
        val panel = page()
        title(panel,R.string.rules)
        listOf(R.string.rule_aim,R.string.rule_match,R.string.rule_goal).forEachIndexed { i,id ->
            panel.row(object : View(this) {
                override fun onDraw(c: Canvas) {
                    val s = min(width/170f,height/54f)
                    c.save(); c.translate(width/2f,height/2f); c.scale(s,s)
                    when (i) {
                        0 -> {
                            art.idol(c,-30f,0f,23f,0f)
                            for (j in 0..2) art.disc(c,j*8f,0f,1.6f,gold)
                            art.ball(c,0,36f,0f,10f)
                        }
                        1 -> {
                            for (j in -2..2) art.ball(c,if(abs(j)==2) 1 else 2,j*21f,0f,10f)
                            art.ring(c,0f,0f,17f,gold,1.5f)
                        }
                        else -> {
                            art.ball(c,1,-32f,0f,10f); art.ball(c,0,-10f,0f,10f)
                            art.bitmap(c,art.field,android.graphics.Rect(53,365,159,464),11f,-21f,45f,43f)
                        }
                    }
                    c.restore()
                }
            },48,14)
            panel.row(text(getString(id),17f,cream,false))
        }
        panel.row(button(R.string.home) { home() },50,24)
    }

    private fun stat(panel: LinearLayout, id: Int, value: Int) {
        val row = LinearLayout(this).apply {
            gravity=Gravity.CENTER_VERTICAL
            background=shape(ink,0xff455d95.toInt(),12)
            setPadding(dp(16),dp(10),dp(16),dp(10))
        }
        row.addView(text(getString(id),17f,cream,false).apply { gravity=Gravity.START },LinearLayout.LayoutParams(0,wrap,1f))
        row.addView(text(String.format(Locale.US,"%,d",value),24f,gold))
        panel.row(row,wrap,8)
    }

    private fun records() {
        clear(Screen.RECORDS)
        val panel = page()
        panel.row(Emblem(this,art),92)
        title(panel,R.string.records)
        stat(panel,R.string.best,prefs.getInt("best",0))
        stat(panel,R.string.most,prefs.getInt("most",0))
        stat(panel,R.string.wins,prefs.getInt("wins",0))
        if (!prefs.contains("lastScore")) panel.row(text(getString(R.string.empty_records),15f,cream,false),wrap,14)
        panel.row(button(R.string.home) { home() },50,20)
    }

    private fun restore(): Round? = prefs.getString("round",null)?.let {
        runCatching { DataInputStream(ByteArrayInputStream(Base64.decode(it,Base64.NO_WRAP))).use(Round::read) }.getOrNull()
    }

    private fun save() {
        val game = round ?: return
        if (game.outcome != Outcome.PLAYING) return
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use(game::write)
        prefs.edit().putString("round",Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP)).commit()
    }

    private fun startGame(continuing: Boolean) {
        clear(Screen.GAME)
        val saved = if (continuing) restore() else null
        val game = saved ?: Round()
        round = game
        val layout = column()
        content.addView(layout)
        val hud = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = shape(ink,0xff887e71.toInt(),16)
            setPadding(dp(5),0,dp(12),0)
        }
        layout.addView(hud,LinearLayout.LayoutParams(fill,dp(56)).apply { setMargins(dp(12),dp(8),dp(12),0) })
        val pause = button(R.string.pause) { pauseGame() }
        pause.text = "Ⅱ"
        pause.contentDescription = getString(R.string.pause)
        hud.addView(pause,LinearLayout.LayoutParams(dp(48),dp(48)))
        points = text("",16f)
        hud.addView(points,LinearLayout.LayoutParams(0,wrap,1f))
        progress = text("",24f,gold)
        hud.addView(progress)
        field = GameView(this,art,game,::updateHud) { finishRound(game) }.also {
            it.contentDescription = getString(R.string.board_desc)
            layout.addView(it,LinearLayout.LayoutParams(fill,0,1f))
        }
        val nextBox = LinearLayout(this).apply {
            gravity=Gravity.CENTER
            background=shape(ink,gold,22)
        }
        nextBox.addView(text(getString(R.string.next),17f))
        preview = object : View(this) {
            override fun onDraw(c: Canvas) { art.ball(c,game.next,width*.55f,height*.5f,dp(13).toFloat()) }
        }
        nextBox.addView(preview,LinearLayout.LayoutParams(dp(38),fill))
        layout.addView(nextBox,LinearLayout.LayoutParams(dp(112),dp(40)).apply { bottomMargin=dp(8) })
        updateHud()
        if (saved != null) pauseGame()
    }

    private fun updateHud() {
        val game = round ?: return
        progress.text = String.format(Locale.US, getString(R.string.progress_count), game.cleared)
        points.text = getString(R.string.score)+"  "+game.score
        preview.contentDescription = getString(R.string.next_desc,resources.getStringArray(R.array.ball_names)[game.next])
        preview.invalidate()
    }

    private fun pauseGame() {
        if (overlay != null || screen != Screen.GAME) return
        field?.paused = true
        save()
        val cover = FrameLayout(this).apply { setBackgroundColor(0xab071437.toInt()); isClickable=true }
        overlay = cover
        content.addView(cover)
        val panel = column().apply {
            background=shape(blue,gold,22)
            setPadding(dp(22),dp(20),dp(22),dp(22))
        }
        cover.addView(panel,FrameLayout.LayoutParams(min(dp(290),resources.displayMetrics.widthPixels-dp(32)),wrap,Gravity.CENTER))
        title(panel,R.string.paused)
        panel.row(button(R.string.resume,true) { resumeGame() },50,20)
        panel.row(button(R.string.save_home) { home() },50,8)
    }

    private fun resumeGame() {
        overlay?.let(content::removeView)
        overlay = null
        field?.paused = false
    }

    private fun finishRound(game: Round) {
        if (screen != Screen.GAME || round !== game) return
        val won = game.outcome == Outcome.WON
        prefs.edit().remove("round").putInt("lastScore",game.score).putInt("lastClear",game.cleared)
            .putBoolean("lastWin",won).putInt("best",max(game.score,prefs.getInt("best",0)))
            .putInt("most",max(game.cleared,prefs.getInt("most",0)))
            .putInt("wins",prefs.getInt("wins",0)+if(won) 1 else 0).commit()
        results()
    }

    private fun results() {
        clear(Screen.RESULTS)
        round = null
        val panel = page()
        panel.row(Emblem(this,art),112)
        title(panel,if(prefs.getBoolean("lastWin",false)) R.string.won else R.string.lost)
        stat(panel,R.string.cleared,prefs.getInt("lastClear",0))
        stat(panel,R.string.score,prefs.getInt("lastScore",0))
        stat(panel,R.string.best,prefs.getInt("best",0))
        panel.row(button(R.string.again,true) { startGame(false) },52,18)
        panel.row(button(R.string.home) { home() },50,8)
    }

    override fun onPause() {
        if (screen == Screen.GAME) pauseGame()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("screen",screen.name)
        super.onSaveInstanceState(outState)
    }
}
