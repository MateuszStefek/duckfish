package duckchess

class Evaluator {
    var evaluatedPositionsCounter = 0

    fun evaluation(board: Board): BoardEval {
        evaluatedPositionsCounter++

        var score: Int = 0
        var coordA: Coord? = null
        var coordB: Coord? = null

        Coord.forEach { pos ->
            val piece = board[pos]

            if (piece == Piece.EMPTY) {
                if (coordA == null) {
                    coordA = pos
                } else if (coordB == null) {
                    coordB = pos
                }
            } else {
                score += pieceScore(piece, pos, board)
            }
        }

        val tempoBonus = 12

        score = when (board.phase) {
            Phase.WHITE_PIECE_MOVE -> score + tempoBonus
            Phase.BLACK_PIECE_MOVE -> -score + tempoBonus
            else -> TODO()
        }

        return BoardEval(score, coordA!!, score, coordB!!)
    }

    private fun pieceScore(piece: Piece, pos: Coord, board: Board) = when (piece) {
        Piece.WHITE_PAWN -> 100 + whitePawnPositionScore(pos, board)
        Piece.WHITE_BISHOP -> 300 + bishopPositionScore(pos)
        Piece.WHITE_KNIGHT -> 400 + knightPositionScore(pos)
        Piece.WHITE_ROOK -> 500
        Piece.WHITE_QUEEN -> 900
        Piece.BLACK_PAWN -> -100 - blackPawnPositionScore(pos, board)
        Piece.BLACK_BISHOP -> -300 - bishopPositionScore(pos)
        Piece.BLACK_KNIGHT -> -400 - knightPositionScore(pos)
        Piece.BLACK_ROOK -> -500
        Piece.BLACK_QUEEN -> -900
        else -> 0
    }

    private fun knightPositionScore(pos: Coord): Int {
        val col = pos.index % 8
        val row = pos.index / 8
        var penalty = 0
        if (col == 0 || col == 7) penalty += 10 else if (col == 1 || col == 6) penalty += 5
        if (row == 0 || row == 7) penalty += 10 else if (row == 1 || row == 6) penalty += 5
        return -penalty
    }

    private fun bishopPositionScore(pos: Coord): Int {
        val col = pos.index % 8
        val row = pos.index / 8
        var penalty = 0
        if (col == 0 || col == 7) penalty += 10
        if (row == 0 || row == 7) penalty += 10
        return -penalty
    }

    private fun whitePawnPositionScore(pos: Coord, board: Board): Int {
        val row = pos.index / 8
        if (row >= 6) return 30
        var score = row

        if (board[pos.oneUp()] == Piece.WHITE_PAWN) {
            score -= 17
        } else if (row < 5) {
            if (board[pos.twoUp()] == Piece.WHITE_PAWN) {
                score -= 16
            }
        }

        return score
    }

    private fun blackPawnPositionScore(pos: Coord, board: Board): Int {
        val row = pos.index / 8
        if (row <= 1) return 30
        var score = 7 - row

        if (board[pos.oneDown()] == Piece.BLACK_PAWN) {
            score -= 17
        } else if (row > 2) {
            if (board[pos.twoDown()] == Piece.BLACK_PAWN) {
                score -= 16
            }
        }

        return score
    }
}
