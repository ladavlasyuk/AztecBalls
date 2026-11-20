package com.AncientPar.AztecBalls

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.*

val cream = Color.rgb(255, 242, 207)
val gold = Color.rgb(255, 202, 98)
val ink = Color.rgb(13, 29, 77)
val blue = Color.rgb(30, 59, 148)
val orbColors = intArrayOf(0xff57e7d4.toInt(), 0xffff9346.toInt(), 0xffcee94f.toInt())

class Artwork(context: Context) {
    private fun load(context: Context, id: Int) = BitmapFactory.decodeResource(context.resources, id)
    val bg = load(context, R.drawable.bg)
    val field = load(context, R.drawable.field)
    private val head = load(context, R.drawable.head)
    val balls = load(context, R.drawable.balls)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val sprites = listOf(Rect(4, 3, 51, 51), Rect(52, 2, 101, 50), Rect(102, 4, 148, 50))
    private val mask = Rect(67, 75, 188, 182)
    private val dst = RectF()

    fun bitmap(c: Canvas, bitmap: Bitmap, src: Rect?, x: Float, y: Float, w: Float, h: Float) {
        paint.color = Color.WHITE
        dst.set(x, y, x+w, y+h)
        c.drawBitmap(bitmap, src, dst, paint)
    }

    fun disc(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        paint.color = color
        c.drawCircle(x, y, r, paint)
    }

    fun ring(c: Canvas, x: Float, y: Float, r: Float, color: Int, width: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width
        disc(c,x,y,r,color)
        paint.style = Paint.Style.FILL
    }

    fun ball(c: Canvas, color: Int, x: Float, y: Float, r: Float) {
        disc(c,x,y+1.4f,r+1,0x770b1236)
        disc(c,x,y,r,orbColors[color])
        bitmap(c,balls,sprites[color],x-r+1,y-r+1,2*r-2,2*r-2)
    }

    fun idol(c: Canvas, x: Float, y: Float, radius: Float, degrees: Float) {
        c.save()
        c.rotate(degrees+180, x, y)
        bitmap(c,head,mask,x-radius,y-radius*.884f,radius*2,radius*1.768f)
        c.restore()
    }

    fun seal(c: Canvas, x: Float, y: Float, r: Float) {
        c.save()
        c.translate(x,y)
        c.scale(r/100,r/100)
        disc(c,0f,4f,99f,0x66362419)
        disc(c,0f,0f,99f,ink)
        ring(c,0f,0f,97f,gold,2f)
        ring(c,0f,0f,90f,0xff916f47.toInt(),1f)
        ring(c,0f,0f,78f,gold,1.6f)
        disc(c,0f,0f,75f,blue)
        val glyph = Path().apply { moveTo(-3.3f,-82f);lineTo(0f,-89f);lineTo(3.3f,-82f);close() }
        repeat(40) {
            paint.color = if (it%5==0) cream else 0xffc59650.toInt()
            c.drawPath(glyph,paint)
            c.rotate(9f)
        }
        ring(c,0f,0f,68f,0x5551bbeb,1f)
        idol(c,0f,0f,66f,90f)
        c.restore()
    }
}

class Backdrop(context: Context, private val art: Artwork) : View(context) {
    override fun onDraw(c: Canvas) {
        val scale = max(width/393f,height/852f)
        art.bitmap(c,art.bg,null,(width-393*scale)/2,(height-852*scale)/2,393*scale,852*scale)
        val p = art.paint
        p.color = 0x180b1547
        c.drawRect(0f,0f,width.toFloat(),height.toFloat(),p)
        c.save()
        c.translate(width*.94f,height*.055f)
        val unit = width*.22f
        for (i in 1..4) art.ring(c,0f,0f,unit*(.55f+i*.27f),0x42ffecb3,1.5f)
        repeat(32) {
            p.color=0x60ffeac2
            p.strokeWidth=2f
            c.drawLine(0f,unit*.83f,0f,unit*1.07f,p)
            c.rotate(11.25f)
        }
        c.restore()
    }
}

class Emblem(context: Context, private val art: Artwork) : View(context) {
    override fun onDraw(c: Canvas) {
        val r = min(width*.32f,height*.455f)
        art.seal(c,width/2f,height/2f,r)
        for (sign in listOf(-1,1)) {
            val x = width/2f+sign*(r+12)
            art.paint.color=cream
            art.paint.strokeWidth=2f
            c.drawLine(x,height/2f,x+sign*r*.28f,height/2f,art.paint)
            art.disc(c,x+sign*r*.33f,height/2f,2.6f,gold)
        }
    }
}
