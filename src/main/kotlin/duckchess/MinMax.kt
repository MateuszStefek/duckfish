package duckchess

import java.time.Duration
import java.time.Instant
import kotlin.math.absoluteValue

private fun winsScore(): Int = 100_000

private fun isDecisiveScore(score: Int) = score.absoluteValue > 95_000

private inline fun negScore(score: Int): Int {
    return when {
        score > 0 -> 1 - score
        score < 0 -> -1 - score
        else -> score
    }
}

private inline fun negForward(score: Int): Int {
    return when {
        score > 0 -> -(score + 1)
        score < 0 -> (-score) + 1
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

    fun negated() = copy(scoreA = -scoreA, scoreB = -scoreB)
}

data class SelectedMove(val move: Move, val duckMove: DuckMove, val score: Int, val depth: Int) {
    fun text(board: Board): String {
        return "${move.text(board)}${duckMove.text(board)} score: $score, depth: $depth"
    }
}

private data class MoveWithScore(var move: Move, var score: Int) : Comparable<MoveWithScore> {
    override fun compareTo(other: MoveWithScore): Int {
        return other.score - score
    }

}

private class Stop : RuntimeException()

class MinMax(val evaluator: Evaluator = Evaluator()) {
    var staticEvalCount: Long = 0
    var visitedNodes: Long = 0
    var cutOffTime: Instant? = null
    val transpositionCache = TranspositionCache<BoardEval>()
    val finalHorizontDepth = 3
    private val scoredCordSetPool: ScoredCordSetPool = ScoredCordSetPool()
    private val moveWitScoreArrayPool = MoveScoreArrayPool()

    fun bestMove(boardArg: Board, duration: Duration): SelectedMove {
        staticEvalCount = 0
        visitedNodes = 0

        val board = boardArg.copyBoard()
        val start = Instant.now()
        cutOffTime = null
        var selectedMove: SelectedMove? = null
        var maxDepth = 3
        while (true) {
            try {
                val startThisDepth = Instant.now()
                selectedMove = bestMove(board, maxDepth, selectedMove?.move)
                println("Best move at depth ${maxDepth}: ${selectedMove.text(board)}")

                if (isDecisiveScore(selectedMove.score)) {
                    println("Game solved at depth ${maxDepth}")
                    return selectedMove!!
                }

                val endThisDepth = Instant.now()
                val durationThisDepth = Duration.between(startThisDepth, endThisDepth)
                cutOffTime = start.plus(duration)
                maxDepth++
                if (Instant.now().plus(durationThisDepth.dividedBy(4)).isAfter(cutOffTime)) {
                    println("Giving up on evaluating at depth ${maxDepth}")
                    return selectedMove!!
                }
            } catch (e: Stop) {
                println("Time has run out at depth ${maxDepth}")
                return selectedMove!!
            }
        }
    }

    fun bestMove(board: Board, maxDepth: Int, previousBestMove: Move? = null): SelectedMove {
        val boardEval = NodeEvaluation(
            board = board,
            remainingDepth = maxDepth * finalHorizontDepth,
            bestMovePreviousDepth = previousBestMove
        )
            .negMax()!!

        return SelectedMove(boardEval.moveA!!, DuckMove.of(boardEval.duckMoveA!!), boardEval.scoreA, maxDepth)
    }

    private inner class NodeEvaluation(
        val remainingDepth: Int,
        val board: Board,
        private val alphaArg: Int = -99_999,
        private val beta: Int = 99_999,
        private val immediateBeta: BoardEval = BoardEval(
            scoreA = Integer.MIN_VALUE + 10,
            Coord.A1,
            scoreB = Integer.MIN_VALUE + 10,
            Coord.A1
        ),
        private val areWeWhite: Boolean = when (board.phase) {
            Phase.WHITE_PIECE_MOVE -> true
            Phase.BLACK_PIECE_MOVE -> false
            else -> TODO()
        },
        val bestMovePreviousDepth: Move? = null,
        var bestMovePreviousDepthAnalysed: Boolean = false,
    ) {

        private var bestMoveFromCache: Move? = null
        private var alpha: Int = alphaArg

        init {
            visitedNodes++
        }

        private inline fun debug(textGen: () -> String) {
            /*            if (remainingDepth > 0) textGen.invoke().let { text ->
                            println(" ".repeat(maxOf(0, 15 - remainingDepth)) + "depth $remainingDepth $text")
                        }*/
        }

        /**
         * @return null, when pruned by alpha-beta.
         */
        fun negMax(): BoardEval? {
            debug { "Entering with alpha=${alphaArg} beta=${beta} whiteSCA=${board.whiteShortCastlingAllowed}" }

            if (board.result != GameResult.UNDECIDED) {
                return finalResult(board)
            }

            if (remainingDepth <= 0) {
                staticEvalCount++
                val staticEval = evaluator.evaluation(board)
                return staticEval
            }

            if (remainingDepth > finalHorizontDepth) checkTimeout()

            var cacheEntry = transpositionCache.get(board)
            if (cacheEntry != null && cacheEntry.remainingDepth >= remainingDepth) {
                debug { "Cached entry: ${cacheEntry}" }

                if (cacheEntry.alpha <= alpha && cacheEntry.beta >= beta) {
                    debug { "Returing cached ${cacheEntry}" }
                    return cacheEntry.score
                }
                val cacheEntryScore = cacheEntry.score!!
                if (cacheEntryScore.scoreA > cacheEntry.alpha && cacheEntryScore.scoreB < cacheEntry.beta) {
                    debug { "Returning cached ${cacheEntry}" }
                    return cacheEntry.score
                }

                if (cacheEntryScore.scoreA > beta && cacheEntryScore.scoreB > beta && cacheEntryScore.scoreB > cacheEntry.beta) {
                    debug { "Returning cached alpha-beta prune ${cacheEntry}" }
                    return cacheEntry.score
                }
            }
            this.bestMoveFromCache = cacheEntry?.score?.moveA



            return scoredCordSetPool.withScoredCordSetFromPool { s ->
                if (board.duckPosition != null) {
                    s.initialize({ coord -> board[coord] == Piece.DUCK }, Integer.MIN_VALUE + 10)
                } else {
                    s.initialize({ coord -> board[coord] == Piece.EMPTY }, Integer.MIN_VALUE + 10)
                }

                evaluateRecursively(areWeWhite = areWeWhite, scoredCoordSet = s)
            }
        }
        private fun evaluateRecursively(areWeWhite: Boolean, scoredCoordSet: ScoredCoordSet): BoardEval? {
            return moveWitScoreArrayPool.withMoveScoreArray { allMovesArray ->
                val moveCount = generateMoves(allMovesArray)
                evaluateMovesRecursively(areWeWhite, scoredCoordSet, allMovesArray, moveCount)
            }
        }

        private fun evaluateMovesRecursively(
            areWeWhite: Boolean,
            scoredCoordSet: ScoredCoordSet,
            allMovesArray: Array<MoveWithScore>,
            moveCount: Int
        ): BoardEval? {

            var currentEval = scoredCoordSet.bottomTwo()

            val nextPhase = if (areWeWhite) Phase.BLACK_PIECE_MOVE else Phase.WHITE_PIECE_MOVE

            val boardWithoutDuck = board.withoutDuck()

            try {
                var moveI = 0
                while (moveI < moveCount) {
                    val move = allMovesArray[moveI].move
                    moveI++

                    var shouldReturnNull = false

                    debug { "Evaluating ${move.text(board)} alpha=${alpha} beta=${beta} wscA =${boardWithoutDuck.whiteShortCastlingAllowed}" }

                    move.moveAndRevert(boardWithoutDuck) {
                        debug { "after move wsca=${boardWithoutDuck.whiteShortCastlingAllowed}" }

                        boardWithoutDuck.phase = nextPhase

                        if (currentEval.scoreA >= -immediateBeta.scoreA) {
                            debug { "${move.text(board)} cut-off A" }
                            shouldReturnNull = true
                            return@moveAndRevert
                        }
                        if (currentEval.duckPosA == immediateBeta.duckPosA) {
                            if (currentEval.scoreB > -immediateBeta.scoreA) {
                                debug { "${move.text(board)} cut-off B" }
                                shouldReturnNull = true
                                return@moveAndRevert
                            }
                        }

                        val nextRemainingDepth = when {
                            remainingDepth == 1 -> 0
                            remainingDepth <= finalHorizontDepth && move is CaptureMove -> remainingDepth - 1
                            else -> maxOf(0, remainingDepth - finalHorizontDepth)
                        }

                        val moveEval = NodeEvaluation(
                            board = boardWithoutDuck,
                            remainingDepth = nextRemainingDepth,
                            alphaArg = negForward(beta),
                            beta = negForward(alpha),
                            immediateBeta = currentEval,
                            areWeWhite = !areWeWhite
                        ).negMax()

                        debug { "${move.text(board)} eval returned from child: ${moveEval}" }
                        if (moveEval != null) {
                            scoredCoordSet.update(
                                minOf(beta + 1, negScore(moveEval.scoreA)),
                                predicate = { coord -> coord != moveEval.duckPosA && !move.blockedByDuckAt(coord) },
                                move, moveEval.duckPosA
                            )
                            scoredCoordSet.update(
                                minOf(beta + 1, negScore(moveEval.scoreB)),
                                predicate = { coord -> coord != moveEval.duckPosB && !move.blockedByDuckAt(coord) },
                                move, moveEval.duckPosB
                            )
                            currentEval = scoredCoordSet.bottomTwo()

                            alpha = maxOf(alpha, currentEval.scoreA)
                        }
                    }

                    debug { " new gA: ${alpha}" }
                    debug { " new s: ${scoredCoordSet.text(board)}" }

                    if (shouldReturnNull) {
                        return@evaluateMovesRecursively null
                    }

                    if (alpha > beta) {
                        var prunedScore = scoredCoordSet.bottomTwo().copy(scoreA = alpha, scoreB = alpha)
                        debug { "${move.text(board)} alpha-beta cut off. Returning $prunedScore" }
                        transpositionCache.set(
                            board,
                            remainingDepth = remainingDepth,
                            score = prunedScore,
                            alphaArg,
                            beta
                        )
                        return prunedScore
                    }

                    if (bestMovePreviousDepth == move) {
                        bestMovePreviousDepthAnalysed = true
                    }
                }
            } catch (e: Stop) {
                if (bestMovePreviousDepth != null && bestMovePreviousDepthAnalysed && board.duckPosition != null) {
                    println(" Time has run out, but the previous best move (${bestMovePreviousDepth.text(board)}) has already been analysed")
                    return currentEval
                } else {
                    throw e;
                }
            }

            val result = scoredCoordSet.bottomTwo()
            transpositionCache.set(board, remainingDepth = remainingDepth, score = result, alphaArg, beta)

            debug { "Full return ${result}" }

            return result
        }

        private fun generateMoves(allMoves: Array<MoveWithScore>): Int {
            var moveCount = 0

            board.generateMoves { move ->
                var strengthEst = moveStrengthEstimation(move, board, areWeWhite)
                if (move === bestMoveFromCache) strengthEst += 300
                val mvs = allMoves[moveCount++]
                mvs.move = move
                mvs.score = strengthEst
            }

            allMoves.sort(0, moveCount)
            return moveCount
        }
    }


    private fun checkTimeout() {
        cutOffTime?.let { if (Instant.now().isAfter(it)) throw Stop() }
    }


    // Stronger moves should have larger strength
    // This is heuristic for better move ordering
    private fun moveStrengthEstimation(move: Move, board: Board, areWeWhite: Boolean): Int {
        return when (move) {
            is PawnCaptureMove -> {
                val capturedPiece = board[move.to]
                when (capturedPiece) {
                    Piece.WHITE_PAWN -> 100
                    Piece.BLACK_PAWN -> 100
                    Piece.WHITE_KING -> 1000
                    Piece.BLACK_KING -> 1000
                    else -> 200
                }
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

    private fun finalResult(board: Board): BoardEval {
        return when (val result = board.result) {
            GameResult.WHITE_WON -> BoardEval(
                -winsScore(),
                Coord.A1,
                -winsScore(),
                Coord.B1
            )

            GameResult.BLACK_WON -> BoardEval(
                -winsScore(),
                Coord.A1,
                -winsScore(),
                Coord.B1
            )

            else -> throw IllegalArgumentException("Non-final $result")
        }
    }
}

private class MoveScoreArrayPool {
    private val t: Array<Array<MoveWithScore>> = Array(1000) {
        Array(100) {
            MoveWithScore(DuckMove.of(Coord.A1), 0)
        }
    }
    var nextFree: Int = 0

    inline fun <R> withMoveScoreArray(consumer: (Array<MoveWithScore>) -> R): R {
        var array: Array<MoveWithScore> = t[nextFree++]
        try {
            return consumer(array)
        } finally {
            nextFree--
        }
    }
}
