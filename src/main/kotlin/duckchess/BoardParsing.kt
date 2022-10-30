package duckchess

fun parseBoard(text: String): Board {
    val lines = text.lines()
    val pieces: MutableMap<Coord, Piece> = mutableMapOf()
    Coord.forEach { coord ->
        val col = coord.column
        val row = coord.row

        val pieceText = lines[1 + 2 * (7 - row.row)][1 + col.column * 2].toString()
        val piece = Piece.fromText(pieceText)

        pieces[coord] = piece
    }

    val board = Board.apOf(pieces = pieces)

    if (lines[0].contains("-- *")) {
        board.phase = Phase.BLACK_PIECE_MOVE
    } else if (lines[16].contains("-- *")) {
        board.phase = Phase.WHITE_PIECE_MOVE
    }

    return board
}
