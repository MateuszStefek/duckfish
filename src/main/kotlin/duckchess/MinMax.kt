package duckchess

private fun whiteWinsScore(): Int = 100_000
private fun blackWinsScore(): Int = -100_000

private inline fun negScore(score: Int): Int {
    return when {
        score > 0 -> 1 - score
        score < 0 -> -1 - score
        else -> score
    }
}

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

class MinMax(var maxDepth: Int, val evaluator: Evaluator = Evaluator()) {
    var counter: Long = 0
    val transpositionCache = TranspositionCache<BoardEval>()

    fun bestMove(board: Board): SelectedMove {
        counter = 0
        val areWeWhite: Boolean = when (board.phase) {
            Phase.WHITE_PIECE_MOVE -> true
            Phase.BLACK_PIECE_MOVE -> false
            else -> TODO()
        }

        val worstAlpha = BoardEval(scoreA = Integer.MIN_VALUE + 10, Coord.A1, scoreB = Integer.MIN_VALUE + 10, Coord.A1)

        val boardEval = negMax(
            board,
            maxDepth,
            areWeWhite,
            Integer.MIN_VALUE + 5,
            Integer.MAX_VALUE - 5,
            worstAlpha,
        )!!

        return SelectedMove(boardEval.moveA!!, DuckMove.of(boardEval.duckMoveA!!), boardEval.scoreA)
    }

    /**
     * @return null, when pruned by alpha-beta.
     */
    private fun negMax(
        board: Board,
        remainingDepth: Int,
        areWeWhite: Boolean,
        gAArg: Int,
        gB: Int,
        alpha: BoardEval
    ): BoardEval? {
        if (board.result != GameResult.UNDECIDED) {
            return finalResult(board, areWeWhite)
        }

        if (remainingDepth <= 0) {
            counter++
            return evaluator.evaluation(board)
        }

        var gA = gAArg

        val cacheEntry = transpositionCache.get(board)
        if (cacheEntry != null && cacheEntry.remainingDepth >= remainingDepth) {
            if (cacheEntry.alpha <= gA && cacheEntry.beta >= gB) {
                return cacheEntry.score
            }
            if (cacheEntry.score!!.scoreA > cacheEntry.alpha && cacheEntry.score!!.scoreB < cacheEntry.beta) {
                return cacheEntry.score
            }
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
            var strengthEst = moveStrengthEstimation(move, board, areWeWhite)
            if (cacheEntry != null) {
                if (move === cacheEntry.score?.moveA) {
                    strengthEst += 300
                }
            }
            allMoves.add(MoveWithScore(move, strengthEst))
        }

        allMoves.sortByDescending { move -> move.score }

        var beta = s.bottomTwo()

        allMoves.forEach { (move, _) ->
            var shouldReturnNull = false

            move.moveAndRevert(boardWithoutDuck) {
                boardWithoutDuck.phase = nextPhase

                if (beta.scoreA >= -alpha.scoreA) {
                    shouldReturnNull = true
                    return@moveAndRevert
                }
                if (beta.duckPosA == alpha.duckPosA) {
                    if (beta.scoreB > -alpha.scoreA) {
                        shouldReturnNull = true
                        return@moveAndRevert
                    }
                }

                val nextRemainingDepth = when {
                    // (move is CaptureMove && depth == maxDepth - 1) -> depth
                    else -> remainingDepth - 1
                }

                val eval: BoardEval? = negMax(boardWithoutDuck, nextRemainingDepth, !areWeWhite, -gB, -gA, beta)
                if (eval != null) {
                    s.update(
                        negScore(eval.scoreA),
                        predicate = { coord -> coord != eval.duckPosA && !move.blockedByDuckAt(coord) },
                        move, eval.duckPosA
                    )
                    s.update(
                        negScore(eval.scoreB),
                        predicate = { coord -> coord != eval.duckPosB && !move.blockedByDuckAt(coord) },
                        move, eval.duckPosB
                    )
                    beta = s.simplifiedBottomTwo()

                    gA = maxOf(gA, beta.scoreA)
                }
            }

            if (shouldReturnNull) {
                return@negMax null
            }

            if (gA >= gB) {
                var prunedScore = s.bottomTwo().copy(scoreA = gA, scoreB = gA)
                transpositionCache.set(
                    boardWithoutDuck,
                    remainingDepth = remainingDepth,
                    score = prunedScore,
                    gAArg,
                    gB
                )
                return prunedScore
            }
        }

        val result = s.bottomTwo()

/*
        if (result.scoreA < -1_000_000 || result.scoreB < -1_000_000) {
            // most moves are pruned, all remaining moves are blockaded by duck
            // TODO: any edge-case with a real stale mate?
            return null
        } else {
*/

        transpositionCache.set(boardWithoutDuck, remainingDepth = remainingDepth, score = result, gAArg, gB)
//        }

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

    private fun finalResult(board: Board, areWeWhite: Boolean): BoardEval {
        val multiplier: Int = if (areWeWhite) 1 else -1
        return when (val result = board.result) {
            GameResult.WHITE_WON -> BoardEval(
                multiplier * whiteWinsScore(),
                Coord.A1,
                multiplier * whiteWinsScore(),
                Coord.B1
            )
            GameResult.BLACK_WON -> BoardEval(
                multiplier * blackWinsScore(),
                Coord.A1,
                multiplier * blackWinsScore(),
                Coord.B1
            )
            else -> throw IllegalArgumentException("Non-final $result")
        }
    }
}
