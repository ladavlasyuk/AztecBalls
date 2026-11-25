package com.AncientPar.AztecBalls

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class GameView(context: Context, private val art: Artwork, private val round: Round,
               private val updateHud: () -> Unit, private val finish: () -> Unit) : View(context) {
    private var clock = 0L
    private var accumulator = 0.0
    var paused = false
        set(value) { field = value; clock = 0; invalidate() }
    private val scale get() = min(width/386f,height/602f)
    private val ox get() = (width-368*scale)/2
    private val oy get() = (height-579*scale)/2

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        if (!paused && clock != 0L) {
            accumulator += min((now-clock)*1e-9,.08)
            val before = round.cleared
            while (accumulator >= 1.0/120) {
                round.step(1f/120)
                accumulator -= 1.0/120
            }
            if (before != round.cleared) updateHud()
        }
        clock = now
        c.save()
        c.translate(ox,oy)
        c.scale(scale,scale)
        art.bitmap(c,art.field,null,0f,0f,368f,579f)
        round.chain.forEach {
            val p = Trail.at(it.distance)
            art.drawGem(c,it.color,p.x,p.y,11f)
        }
        if ((round.chain.lastOrNull()?.distance ?: 0f) > Trail.length*.86f) {
            val a = (95 + 75*cos(now*1e-8)).toInt()
            for (x in listOf(90f,115f)) art.disc(c,x,415f,6f,(a shl 24) or 0xff793c)
        }
        val dx = cos(round.aim)
        val dy = sin(round.aim)
        val x = Round.X-dx*round.flash*3
        val y = Round.Y-dy*round.flash*3
        art.disc(c,Round.X,Round.Y+2,34f,ink)
        art.ring(c,Round.X,Round.Y,33f,gold,1.4f)
        art.idol(c,x,y,31f,Math.toDegrees(round.aim.toDouble()).toFloat())
        art.drawGem(c,round.loaded,x+dx*21,y+dy*21,8f)
        if (round.flash>0) {
            for (side in listOf(-1,1)) art.disc(c,x-dx*5-dy*side*10,y-dy*5+dx*side*10,3f,
                ((round.flash*210).toInt() shl 24) or 0x9bfffa)
        }
        round.shot?.let { art.drawGem(c,it.color,it.x,it.y,10f) }
        round.bursts.forEach {
            val t = max(0f,it.age)/.36f
            val color = ((255*(1-t)).toInt().coerceIn(0,255) shl 24) or (orbColors[it.color] and 0xffffff)
            art.ring(c,it.point.x,it.point.y,9+17*t,color,2*(1-t))
            for (i in 0..7) {
                val angle = i*.7854f
                art.disc(c,it.point.x+cos(angle)*(8+22*t),it.point.y+sin(angle)*(8+22*t),2.4f*(1-t),color)
            }
        }
        c.restore()
        if (!paused && round.outcome != Outcome.PLAYING) {
            paused = true
            post(finish)
        } else if (!paused) postInvalidateOnAnimation()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (paused) return false
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            round.fire((event.x-ox)/scale,(event.y-oy)/scale)
            updateHud()
        }
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return true
    }

    override fun performClick(): Boolean { super.performClick(); return true }
}
