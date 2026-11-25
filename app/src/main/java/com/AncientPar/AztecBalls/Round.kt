package com.AncientPar.AztecBalls

import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.*
import kotlin.random.Random

data class Point(val x: Float, val y: Float)
data class Orb(val color: Int, var distance: Float)
data class Shot(val color: Int, var x: Float, var y: Float, val dx: Float, val dy: Float)
data class Burst(val point: Point, val color: Int, var age: Float)
enum class Outcome { PLAYING, WON, LOST }

object Trail {
    private val points = mutableListOf(Point(1f, 22f))
    private val lengths = mutableListOf(0f)
    val length get() = lengths.last()

    init {
        curve(108, -9, 96, 30, 145, 36)
        curve(208, 45, 248, 6, 294, 61)
        curve(329, 102, 374, 194, 342, 220)
        curve(298, 262, 214, 153, 161, 127)
        curve(104, 92, 52, 141, 40, 205)
        curve(19, 304, 107, 341, 215, 352)
        curve(290, 361, 344, 345, 354, 406)
        curve(368, 475, 277, 573, 197, 561)
        curve(147, 556, 122, 500, 107, 440)
    }

    private fun curve(ax: Int, ay: Int, bx: Int, by: Int, cx: Int, cy: Int) {
        val start = points.last()
        for (i in 1..80) {
            val t = i / 80f
            val u = 1 - t
            fun axis(s: Float, a: Int, b: Int, c: Int) =
                u*u*u*s + 3*u*u*t*a + 3*u*t*t*b + t*t*t*c
            val p = Point(axis(start.x, ax, bx, cx), axis(start.y, ay, by, cy))
            lengths += length + hypot(p.x - points.last().x, p.y - points.last().y)
            points += p
        }
    }

    fun at(distance: Float): Point {
        val d = distance.coerceIn(0f, length)
        val index = lengths.binarySearch(d)
        if (index >= 0) return points[index]
        val right = -index - 1
        val a = points[right - 1]
        val b = points[right]
        val t = (d - lengths[right - 1]) / (lengths[right] - lengths[right - 1])
        return Point(a.x + (b.x-a.x)*t, a.y + (b.y-a.y)*t)
    }
}

class Round(private val random: Random = Random.Default) {
    companion object {
        const val X = 179f
        const val Y = 266f
        const val GAP = 22f
        fun read(input: DataInputStream): Round = with(input) {
            require(readInt() == 1)
            Round().apply {
                chain.clear()
                repeat(readInt().also { require(it in 0..100) }) { chain += Orb(readInt(), readFloat()) }
                loaded = readInt()
                next = readInt()
                aim = readFloat()
                cleared = readInt()
                score = readInt()
                if (readBoolean()) shot = Shot(readInt(), readFloat(), readFloat(), readFloat(), readFloat())
            }
        }
    }

    val chain = mutableListOf<Orb>()
    val bursts = mutableListOf<Burst>()
    var shot: Shot? = null
    var aim = (PI / 2).toFloat()
    var loaded = 0
    var next = 0
    var score = 0
    var cleared = 0
    var outcome = Outcome.PLAYING
    var flash = 0f

    init {
        repeat(12) { chain += Orb(random.nextInt(3), it * GAP) }
        loaded = chain.random(random).color
        next = chain.random(random).color
    }

    fun write(output: DataOutputStream) = with(output) {
        writeInt(1)
        writeInt(chain.size)
        chain.forEach { writeInt(it.color); writeFloat(it.distance) }
        writeInt(loaded); writeInt(next); writeFloat(aim); writeInt(cleared); writeInt(score)
        writeBoolean(shot != null)
        shot?.let { writeInt(it.color); writeFloat(it.x); writeFloat(it.y); writeFloat(it.dx); writeFloat(it.dy) }
        Unit
    }

    fun fire(x: Float, y: Float) {
        if (shot != null || outcome != Outcome.PLAYING || hypot(x-X, y-Y) < 1f) return
        aim = atan2(y-Y, x-X)
        val dx = cos(aim)
        val dy = sin(aim)
        shot = Shot(loaded, X+dx*23, Y+dy*23, dx*590, dy*590)
        loaded = next
        next = chain.random(random).color
        flash = 1f
    }

    fun step(dt: Float) {
        if (outcome != Outcome.PLAYING) return
        flash = max(0f, flash-dt*5)
        bursts.forEach { it.age += dt }
        bursts.removeAll { it.age > .36f }
        val speed = Trail.length / 45f * (1 + .08f * (cleared / 25))
        val gap = (1 until chain.size).firstOrNull { chain[it].distance-chain[it-1].distance > GAP+.01f }
        val travel = if (gap == null) speed*dt else min(speed*4*dt, chain[gap].distance-chain[gap-1].distance-GAP)
        for (i in 0 until (gap ?: chain.size)) chain[i].distance += travel
        if (chain.isEmpty() || chain.first().distance >= GAP) chain.add(0, Orb(random.nextInt(3), 0f))
        shot?.let { moveShot(it, dt) }
        if (cleared >= 100) outcome = Outcome.WON
        else if (chain.isNotEmpty() && chain.last().distance >= Trail.length) outcome = Outcome.LOST
    }

    private fun moveShot(projectile: Shot, dt: Float) {
        val sx = projectile.dx*dt
        val sy = projectile.dy*dt
        val a = sx*sx+sy*sy
        var first = Float.POSITIVE_INFINITY
        var hit = -1
        chain.forEachIndexed { index, orb ->
            val p = Trail.at(orb.distance)
            val x = projectile.x-p.x
            val y = projectile.y-p.y
            val b = x*sx+y*sy
            val c = x*x+y*y-20f*20f
            val disc = b*b-a*c
            if (disc >= 0 && a > 0) {
                val t = if (c <= 0) 0f else (-b-sqrt(disc))/a
                if (t in 0f..1f && t < first) { first = t; hit = index }
            }
        }
        if (hit >= 0) {
            if (chain[hit].color == projectile.color) clearGroup(hit)
            else bursts += Burst(Point(projectile.x+first*sx, projectile.y+first*sy), projectile.color, .12f)
            shot = null
        } else {
            projectile.x += sx
            projectile.y += sy
            if (projectile.x !in -20f..388f || projectile.y !in -65f..644f) shot = null
        }
    }

    private fun clearGroup(hit: Int) {
        val color = chain[hit].color
        var left = hit
        var right = hit
        while (left > 0 && chain[left-1].color == color && chain[left].distance-chain[left-1].distance <= GAP+.1f) left--
        while (right < chain.lastIndex && chain[right+1].color == color && chain[right+1].distance-chain[right].distance <= GAP+.1f) right++
        for (i in left..right) bursts += Burst(Trail.at(chain[i].distance), color, -abs(i-hit)*.016f)
        val count = right-left+1
        chain.subList(left, right+1).clear()
        cleared = min(100, cleared+count)
        score += 10*count*count
        if (chain.isEmpty() && cleared < 100) chain += Orb(random.nextInt(3), 0f)
        if (chain.isNotEmpty()) {
            if (chain.none { it.color == loaded }) loaded = chain.random(random).color
            if (chain.none { it.color == next }) next = chain.random(random).color
        }
    }
}
