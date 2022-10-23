package duckchess

private fun whiteWinsScore(depth: Int): Int = 100_000 - depth
private fun blackWinsScore(depth: Int): Int = -100_000 + depth

data class BoardEval(
    val scoreA: Int,
    val duckPosA: Coord,
    val scoreB: Int,
    val duckPosB: Coord,

    val moveA: Move? = null,
    val duckMoveA: Coord? = null

) {
    override fun toString(): String {
        return "BoardEval(A=$scoreA@${duckPosA.text()},B=$scoreB@${duckPosB.text()})"
    }
}

data class SelectedMove(val move: Move, val duckMove: DuckMove, val score: Int) {
    fun text(board: Board): String {
        return "${move.text(board)}${duckMove.text(board)} score: $score"
    }
}

private data class MoveWithScore(val move: Move, val score: Int)

class MinMax(val maxDepth: Int, val evaluator: Evaluator = Evaluator()) {
    var counter: Long = 0
    val transpositionCache = TranspositionCache<BoardEval>()

    fun bestMove(board: Board): SelectedMove {
        val areWeWhite: Boolean = when (board.phase) {
            Phase.WHITE_PIECE_MOVE -> true
            Phase.BLACK_PIECE_MOVE -> false
            else -> TODO()
        }

        val worstAlpha = BoardEval(scoreA = Integer.MIN_VALUE + 10, Coord.A1, scoreB = Integer.MIN_VALUE + 10, Coord.A1)

        val boardEval = negMax(board, 0, areWeWhite, worstAlpha)!!

        return SelectedMove(boardEval.moveA!!, DuckMove.of(boardEval.duckMoveA!!), boardEval.scoreA)
    }

    /**
     * @return null, when pruned by alpha-beta.
     */
    private fun negMax(board: Board, depth: Int, areWeWhite: Boolean, alpha: BoardEval): BoardEval? {

        if (board.result != GameResult.UNDECIDED) {
            return finalResult(board, depth, areWeWhite)
        }

        if (depth >= maxDepth) {
            return evaluator.evaluation(board)
        }

        val cacheEntry = transpositionCache.get(board, remainingDepth = maxDepth - depth)
        if (cacheEntry != null) {
            return cacheEntry.score
        }

        val boardWithoutDuck = board.withoutDuck()

        val nextPhase = if (areWeWhite) Phase.BLACK_PIECE_MOVE else Phase.WHITE_PIECE_MOVE

        val s: ScoredCoordSet =
            if (board.duckPosition != null) {
                ScoredCoordSet({ coord -> board[coord] == Piece.DUCK }, Integer.MIN_VALUE + 10)
            } else {
                ScoredCoordSet({ coord -> board[coord] == Piece.EMPTY }, Integer.MIN_VALUE + 10)
            }

        val allMoves: MutableList<MoveWithScore> = kotlin.collections.ArrayList(64)
        board.generateMoves { move ->
            allMoves.add(MoveWithScore(move, moveStrengthEstimation(move, board, areWeWhite)))
        }

        allMoves.sortByDescending { move -> move.score }

        var beta = s.bottomTwo()

        allMoves.forEach { (move, _) ->
            var shouldStop = false

            move.moveAndRevert(boardWithoutDuck) {
                boardWithoutDuck.phase = nextPhase

                if (beta.scoreA >= -alpha.scoreA) {
                    shouldStop = true
                    return@moveAndRevert
                }
                if (beta.duckPosA == alpha.duckPosA) {
                    if (beta.scoreB > -alpha.scoreA) {
                        shouldStop = true
                        return@moveAndRevert
                    }
                }

                val nextDepth = when {
                    // (move is CaptureMove && depth == maxDepth - 1) -> depth
                    else -> depth + 1
                }

                val eval: BoardEval? = negMax(boardWithoutDuck, nextDepth, !areWeWhite, beta)
                if (eval != null) {
                    s.update(
                        -eval.scoreA,
                        predicate = { coord -> coord != eval.duckPosA && !move.blockedByDuckAt(coord) },
                        move, eval.duckPosA
                    )
                    s.update(
                        -eval.scoreB,
                        predicate = { coord -> coord != eval.duckPosB && !move.blockedByDuckAt(coord) },
                        move, eval.duckPosB
                    )
                    beta = s.simplifiedBottomTwo()
                }
            }

            if (shouldStop) {
                return@negMax null
            }
        }

        val result = s.bottomTwo()

        transpositionCache.set(board, remainingDepth = maxDepth - depth, score = result)

        return result
    }

    // Stronger moves should have larger strength
    // This is heuristic for better move ordering
    private fun moveStrengthEstimation(move: Move, board: Board, areWeWhite: Boolean): Int {
        return when (move) {
            is PawnCaptureMove -> {
                val capturedPiece = board[move.to]
                if (capturedPiece != Piece.WHITE_PAWN && capturedPiece != Piece.BLACK_PAWN) {
                    return 200
                }
                return 100
            }
            is DiagonalCaptureMove -> 50
            is RookCaptureMove -> 50
            is PawnPromotionMove -> 200
            is PawnCapturePromotionMove -> 300
            is KnightCaptureMove -> {
                20
            }
            is PawnTwoStepMove -> 7
            is KnightSimpleMove -> {
                val fromRow = move.from.row
                val toRow: Row = move.to.row
                if (areWeWhite xor (toRow.row < fromRow.row)) {
                    15
                } else {
                    -2
                }
            }
            is WhiteShortCastleMove -> 10
            is BlackShortCastleMove -> 10
            else -> 0
        }
    }

    private fun finalResult(board: Board, depth: Int, areWeWhite: Boolean): BoardEval {
        val multiplier: Int = if (areWeWhite) 1 else -1
        return when (val result = board.result) {
            GameResult.WHITE_WON -> BoardEval(
                multiplier * whiteWinsScore(depth),
                Coord.A1,
                multiplier * whiteWinsScore(depth),
                Coord.B1
            )
            GameResult.BLACK_WON -> BoardEval(
                multiplier * blackWinsScore(depth),
                Coord.A1,
                multiplier * blackWinsScore(depth),
                Coord.B1
            )
            else -> throw IllegalArgumentException("Non-final $result")
        }
    }
}