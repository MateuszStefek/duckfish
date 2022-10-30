package duckchess

import kotlin.experimental.and

@JvmInline
value class Piece(val code: Byte) {
    companion object {
        const val NUM_VALUES = 14

        val EMPTY = Piece(0)
        val DUCK = Piece(1)
        val WHITE_BISHOP = Piece(2)
        val BLACK_BISHOP = Piece(3)
        val WHITE_KNIGHT = Piece(4)
        val BLACK_KNIGHT = Piece(5)
        val WHITE_QUEEN = Piece(6)
        val BLACK_QUEEN = Piece(7)
        val WHITE_KING = Piece(8)
        val BLACK_KING = Piece(9)
        val WHITE_ROOK = Piece(10)
        val BLACK_ROOK = Piece(11)
        val WHITE_PAWN = Piece(12)
        val BLACK_PAWN = Piece(13)

        fun fromText(text: String): Piece = when (text) {
            " " -> Piece.EMPTY
            DUCK.text() -> DUCK
            WHITE_BISHOP.text() -> WHITE_BISHOP
            BLACK_BISHOP.text() -> BLACK_BISHOP
            WHITE_KNIGHT.text() -> WHITE_KNIGHT
            BLACK_KNIGHT.text() -> BLACK_KNIGHT
            WHITE_QUEEN.text() -> WHITE_QUEEN
            BLACK_QUEEN.text() -> BLACK_QUEEN
            WHITE_KING.text() -> WHITE_KING
            BLACK_KING.text() -> BLACK_KING
            WHITE_ROOK.text() -> WHITE_ROOK
            BLACK_ROOK.text() -> BLACK_ROOK
            WHITE_PAWN.text() -> WHITE_PAWN
            BLACK_PAWN.text() -> BLACK_PAWN
            else -> throw IllegalArgumentException(text)
        }
    }

    fun isWhite() = code > 1 && (code and 1) == 0.toByte()
    fun isBlack() = code > 1 && (code and 1) == 1.toByte()
    fun text(): String = when (this) {
        DUCK -> "X"
        WHITE_BISHOP -> "B"
        BLACK_BISHOP -> "b"
        WHITE_PAWN -> "P"
        BLACK_PAWN -> "p"
        WHITE_KNIGHT -> "N"
        BLACK_KNIGHT -> "n"
        WHITE_ROOK -> "R"
        BLACK_ROOK -> "r"
        WHITE_QUEEN -> "Q"
        BLACK_QUEEN -> "q"
        WHITE_KING -> "K"
        BLACK_KING -> "k"
        /*WHITE_BISHOP -> "♗"
        BLACK_BISHOP -> "♝"
        WHITE_PAWN -> "♙"
        BLACK_PAWN -> "♟"
        WHITE_KNIGHT -> "♘"
        BLACK_KNIGHT -> "♞"
        WHITE_ROOK -> "♖"
        BLACK_ROOK -> "♜"
        WHITE_QUEEN -> "♕"
        BLACK_QUEEN -> "♛"
        WHITE_KING -> "♔"
        BLACK_KING -> "♚"*/

        else -> ""
    }
}
