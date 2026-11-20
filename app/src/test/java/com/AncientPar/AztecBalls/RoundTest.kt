package com.AncientPar.AztecBalls

import org.junit.Assert.*
import org.junit.Test
import java.io.*
import kotlin.random.Random

class RoundTest {
    private fun setup(vararg colors: Int) = Round(Random(42)).apply {
        chain.clear()
        colors.forEachIndexed { index, color -> chain += Orb(color,index*Round.GAP) }
    }

    private fun hit(game: Round, index: Int, color: Int) {
        val p = Trail.at(game.chain[index].distance)
        game.shot = Shot(color,p.x,p.y,1f,0f)
        game.step(1f/120)
    }

    @Test fun singlePairAndMiddleGroup() {
        for (size in 1..3) {
            val game = setup(1,*IntArray(size) { 0 },2)
            hit(game,1+size/2,0)
            assertEquals(size,game.cleared)
            assertEquals(10*size*size,game.score)
            assertEquals(listOf(1,2),game.chain.map { it.color })
        }
    }

    @Test fun gapAndWrongColor() {
        val game = setup(0,0,1)
        game.chain[1].distance += 30
        game.chain[2].distance += 30
        hit(game,1,2)
        assertEquals(0,game.score)
        hit(game,1,0)
        assertEquals(1,game.cleared)
    }

    @Test fun crossesTargetAndBeatsGateOnSameTick() {
        val game = setup(1,1,2)
        game.chain.last().distance=Trail.length
        game.cleared=99
        hit(game,0,1)
        assertEquals(100,game.cleared)
        assertEquals(40,game.score)
        assertEquals(Outcome.WON,game.outcome)
    }

    @Test fun emptyChainRefillsAndNewChargeCanMatch() {
        val game=setup(1)
        hit(game,0,1)
        assertTrue(game.chain.isNotEmpty())
        assertEquals(game.chain.single().color,game.next)
        assertEquals(game.chain.single().color,game.loaded)
    }

    @Test fun missLeavesFieldAndDoesNotScore() {
        val game=setup(0,2)
        game.shot=Shot(0,387f,40f,590f,0f)
        game.step(1f/120)
        assertNull(game.shot)
        assertEquals(0,game.cleared)
    }

    @Test fun gatedEndAndFrozenOutcome() {
        val game=setup(2)
        game.chain[0].distance=Trail.length-.02f
        game.step(1f/120)
        assertEquals(Outcome.LOST,game.outcome)
        val end=game.chain.last().distance
        game.step(1f/120)
        assertEquals(end,game.chain.last().distance,0f)
    }

    @Test fun shotKeepsDirectionAndRoundTripsMidFlight() {
        val game=setup(2,0,1)
        game.fire(Round.X-40,Round.Y+40)
        assertTrue(game.shot!!.dx < 0 && game.shot!!.dy > 0)
        val initial=game.shot
        game.fire(Round.X+10,Round.Y)
        assertSame(initial,game.shot)
        val data=ByteArrayOutputStream()
        DataOutputStream(data).use(game::write)
        val restored=DataInputStream(data.toByteArray().inputStream()).use(Round::read)
        assertEquals(game.shot,restored.shot)
        assertEquals(game.chain,restored.chain)
        assertEquals(game.aim,restored.aim,0f)
        assertEquals(game.next,restored.next)
    }
}
