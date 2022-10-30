package duckchess

class Evaluator {
    var evaluatedPositionsCounter = 0

    fun evaluation(board: Board): BoardEval {
        evaluatedPositionsCounter++

        var score: Int = 0
        var coordA: Coord? = null
        var coordB: Coord? = null

        var whitePawns = 0
        var blackPawns = 0
        var whiteFigures = 0
        var blackFigures = 0

        var whiteKing: Coord = Coord(0)
        var blackKing: Coord = Coord(0)

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
                when {
                    piece == Piece.WHITE_PAWN -> whitePawns++
                    piece == Piece.BLACK_PAWN -> blackPawns++
                    piece == Piece.WHITE_KING -> whiteKing = pos
                    piece == Piece.BLACK_KING -> blackKing = pos
                    piece.isBlack() -> blackFigures++
                    piece.isWhite() -> whiteFigures++
                }
            }
        }

        score += materialPotentialScore(whiteFigures, whitePawns, blackFigures, blackPawns, whiteKing, blackKing)

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

    private fun materialPotentialScore(whiteFigures: Int, whitePawns: Int, blackFigures: Int, blackPawns: Int,
        whiteKing: Coord, blackKing: Coord): Int {
        if (blackFigures == 0 && whiteFigures == 0) {
            if (whitePawns > blackPawns + 1) {
                return 200
            }
            if (blackPawns > whitePawns + 1) {
                return -200
            }
        }

        var score = 0
        if (whiteFigures == 0 && whitePawns == 0) {
            score -= 666
            score += whiteKing.distanceTo(blackKing)
            score -= 3 * whiteKing.distanceToCenter()
        }
        if (blackFigures == 0 && blackPawns == 0) {
            score += 666
            score -= whiteKing.distanceTo(blackKing)
            score += 3 * blackKing.distanceToCenter()
        }
        if (blackPawns == 0) {
            score += 50
        }
        if (whitePawns == 0) {
            score -= 50
        }

        if (whiteFigures >= 2 && blackFigures == 0) {
            score += 200
        }
        if (blackFigures >= 2 && whiteFigures == 0) {
            score -= 200
        }

        return score
    }
}
