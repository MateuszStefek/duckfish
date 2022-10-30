package duckchess

import duckchess.Coord.Companion.A8
import duckchess.Coord.Companion.C8
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class BoardParsingKtTest {
    @Test
    fun `testParse 1 white to move`() {
        val text = """
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
        """.trimIndent()
        val pieces: Map<Coord, Piece> = mutableMapOf(
            A8 to Piece.BLACK_ROOK,
            C8 to Piece.BLACK_BISHOP,
            Coord.E8 to Piece.BLACK_KING,
            Coord.F8 to Piece.BLACK_BISHOP,
            Coord.H8 to Piece.BLACK_ROOK,
            Coord.A7 to Piece.BLACK_PAWN,
            Coord.B7 to Piece.BLACK_PAWN,
            Coord.D7 to Piece.BLACK_PAWN,
            Coord.H7 to Piece.BLACK_PAWN,
            Coord.F6 to Piece.BLACK_PAWN,
            Coord.G6 to Piece.BLACK_PAWN,
            Coord.B4 to Piece.BLACK_PAWN,
            Coord.D4 to Piece.BLACK_PAWN,
            Coord.H4 to Piece.BLACK_QUEEN,
            Coord.F3 to Piece.BLACK_KNIGHT,
            Coord.A2 to Piece.WHITE_PAWN,
            Coord.C2 to Piece.WHITE_PAWN,
            Coord.D2 to Piece.WHITE_QUEEN,
            Coord.A1 to Piece.WHITE_ROOK,
            Coord.B1 to Piece.WHITE_KNIGHT,
            Coord.C1 to Piece.WHITE_BISHOP,
            Coord.D1 to Piece.DUCK,
            Coord.E1 to Piece.WHITE_KING,
            Coord.F1 to Piece.WHITE_BISHOP
        )
        val expectedBoard = Board.apOf(pieces)

        val actualBoard = parseBoard(text)

        println(actualBoard.text())

        assertEquals(actualBoard, expectedBoard)
        assertEquals(actualBoard.duckPosition!!, Coord.D1)
    }

    @Test
    fun `testParse 1 black to move`() {
        val text = """
            ----------------- *
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
            ----------------- ep: -1
        """.trimIndent()
        val pieces: Map<Coord, Piece> = mutableMapOf(
            A8 to Piece.BLACK_ROOK,
            C8 to Piece.BLACK_BISHOP,
            Coord.E8 to Piece.BLACK_KING,
            Coord.F8 to Piece.BLACK_BISHOP,
            Coord.H8 to Piece.BLACK_ROOK,
            Coord.A7 to Piece.BLACK_PAWN,
            Coord.B7 to Piece.BLACK_PAWN,
            Coord.D7 to Piece.BLACK_PAWN,
            Coord.H7 to Piece.BLACK_PAWN,
            Coord.F6 to Piece.BLACK_PAWN,
            Coord.G6 to Piece.BLACK_PAWN,
            Coord.B4 to Piece.BLACK_PAWN,
            Coord.D4 to Piece.BLACK_PAWN,
            Coord.H4 to Piece.BLACK_QUEEN,
            Coord.F3 to Piece.BLACK_KNIGHT,
            Coord.A2 to Piece.WHITE_PAWN,
            Coord.C2 to Piece.WHITE_PAWN,
            Coord.D2 to Piece.WHITE_QUEEN,
            Coord.A1 to Piece.WHITE_ROOK,
            Coord.B1 to Piece.WHITE_KNIGHT,
            Coord.C1 to Piece.WHITE_BISHOP,
            Coord.D1 to Piece.DUCK,
            Coord.E1 to Piece.WHITE_KING,
            Coord.F1 to Piece.WHITE_BISHOP
        )
        val expectedBoard = Board.apOf(pieces, phase = Phase.BLACK_PIECE_MOVE)

        val actualBoard = parseBoard(text)

        println(actualBoard.text())

        assertEquals(actualBoard, expectedBoard)
        assertEquals(Phase.BLACK_PIECE_MOVE, actualBoard.phase)
    }

}
