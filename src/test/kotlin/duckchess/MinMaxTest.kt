package duckchess

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

internal class MinMaxTest {
    @Test
    fun testBug1() {
        val board = parseBoard(
            """
                        -----------------
                        |r| |b| |k|b| |r|
                        -----------------
                        |p|p| |p| | | |p|
                        -----------------
                        | | | | | |p|p| |
                        -----------------
                        | | | | | | | | |
                        -----------------
                        | |p| |p| | | |q|
                        -----------------
                        | | | | | |n| | |
                        -----------------
                        |P| |P|Q| | | | |
                        -----------------
                        |R|N|B|X|K|B| | |
                        ----------------- * ep: -1
        """.trimIndent())

        var bestMove = MinMax().bestMove(board, 3)

        expectMove(bestMove.move, board, "KE1-E2", "KE1-F2")
    }
}