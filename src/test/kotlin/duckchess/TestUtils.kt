package duckchess

internal fun expectMove(move: Move, board: Board, vararg expectedMoves: String) {
    val text = move.text(board)
    if (expectedMoves.none { it == text }) {
        throw AssertionError("Expected one of (${expectedMoves.toList()}), but was $text")
    }
}
