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
    val duckMoveA: Coord = Coord.A1,
    val moveB: Move? = null

) {
    companion object {
        val weHaveJustLostBoardEval: BoardEval = BoardEval(
            -winsScore(),
            Coord.A1,
            -winsScore(),
            Coord.B1
        )
    }
    override fun toString(): String {
        return "BoardEval(A=$scoreA@${duckPosA.text()},B=$scoreB@${duckPosB.text()})"
    }
}

data class MutableBoardEval(
    var scoreA: Int = 0,
    var duckPosA: Coord = Coord.A1,
    var scoreB: Int = 0,
    var duckPosB: Coord = Coord.A1,

    var moveA: Move? = null,
    var duckMoveA: Coord = Coord.A1,
    var moveB: Move? = null
) {
    fun toImmutable() = BoardEval(
        scoreA = scoreA,
        duckPosA = duckPosA,
        scoreB = scoreB,
        duckPosB = duckPosB,
        moveA = moveA,
        duckMoveA = duckMoveA,
        moveB = moveB
    )
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
    private val moveWithScoreArrayPool = MoveScoreArrayPool()

    fun bestMove(boardArg: Board, duration: Duration, maximumMaxDepth: Int = 100): SelectedMove {
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
                cutOffTime = start.plus(duration)

                selectedMove = selectMoveAtDepth(boardArg.copyBoard(), maxDepth, selectedMove)
                val totalTimeFromStart = Duration.between(start, Instant.now())
                println("Best move at depth ${maxDepth}: ${selectedMove.text(board)} after ${totalTimeFromStart} from start")

                if (isDecisiveScore(selectedMove.score)) {
                    // If at Ply7 we evaluate to K8, we don't need to evaluate at Ply8
                    if (winsScore() - selectedMove.score.absoluteValue <= maxDepth + 1) {
                        println("Game solved at depth ${maxDepth}")
                        return selectedMove
                    }
                }

                if (maxDepth >= maximumMaxDepth) {
                    println("maximum allowed depth reached (${maximumMaxDepth}")
                    return selectedMove
                }


                val endThisDepth = Instant.now()
                val durationThisDepth = Duration.between(startThisDepth, endThisDepth)

                maxDepth++
                if (Instant.now().plus(durationThisDepth.dividedBy(4)).isAfter(cutOffTime)) {
                    println("Giving up on evaluating at depth ${maxDepth}")
                    return selectedMove
                }
            } catch (e: Stop) {
                println("Time has run out at depth ${maxDepth}")
                return selectedMove!!
            }
        }
    }

    private fun selectMoveAtDepth(board: Board, maxDepth: Int, previousDepthSelectedMove: SelectedMove?): SelectedMove {
        if (previousDepthSelectedMove != null) {
            if (!isDecisiveScore(previousDepthSelectedMove.score)) {
                var aspAlpha = previousDepthSelectedMove.score - 75
                var aspBeta = previousDepthSelectedMove.score + 75
                println("Aspirational search at depth ${maxDepth} with alpha=${aspAlpha}, beta=${aspBeta}")
                val aspirationResult = bestMove(board, maxDepth, null, aspAlpha, aspBeta)
                if (aspirationResult.score in (aspAlpha + 1) until aspBeta) {
                    println("Aspirational search at depth ${maxDepth} with alpha=${aspAlpha}, beta=${aspBeta} succeeded with score=${aspirationResult.score}}")
                    return aspirationResult
                }
                println("Aspirational search at depth ${maxDepth} with alpha=${aspAlpha}, beta=${aspBeta} failed with score=${aspirationResult.score}")
            }
        }
        var alpha = -99_999
        var beta = 99_999
        println("Full search at depth ${maxDepth}")
        return bestMove(board, maxDepth, previousDepthSelectedMove?.move, alpha, beta)
    }

    fun bestMove(
        board: Board,
        maxDepth: Int,
        previousBestMove: Move? = null,
        alpha: Int = -99_999,
        beta: Int = 99_999,
    ): SelectedMove {
        val boardEval = NodeEvaluation(
            board = board,
            remainingDepth = maxDepth * finalHorizontDepth,
            bestMovePreviousDepth = previousBestMove,
            alphaArg = alpha,
            beta = beta
        )
            .negMax()!!

        return SelectedMove(boardEval.moveA!!, DuckMove.of(boardEval.duckMoveA!!), boardEval.scoreA, maxDepth)
    }

    private inner class NodeEvaluation(
        val parentNode: NodeEvaluation? = null,
        val enteredWithReversibleMove: Boolean = true,
        val remainingDepth: Int,
        val board: Board,
        private val alphaArg: Int = -99_999,
        private val beta: Int = 99_999,
        private val immediateBeta: MutableBoardEval = MutableBoardEval(
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
        private var secondBestMoveFromCache: Move? = null
        private var alpha: Int = alphaArg

        private val hashOfBoard: Long = board.zobristHash.hashA

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

            if (currentBoardAlreadySeenInCallStack()) {
                return drawEval(board)
            }

            val cacheEntry = transpositionCache.get(board)
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
            this.secondBestMoveFromCache = cacheEntry?.score?.moveB



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
            return moveWithScoreArrayPool.withMoveScoreArray { allMovesArray ->
                val moveCount = generateMoves(allMovesArray)
                evaluateMovesRecursively( areWeWhite, scoredCoordSet, allMovesArray, moveCount)
            }
        }

        private fun evaluateMovesRecursively(
            areWeWhite: Boolean,
            scoredCoordSet: ScoredCoordSet,
            allMovesArray: Array<MoveWithScore>,
            moveCount: Int
        ): BoardEval? {

            var currentEval: MutableBoardEval = scoredCoordSet.getBottomTwo()

            val nextPhase = if (areWeWhite) Phase.BLACK_PIECE_MOVE else Phase.WHITE_PIECE_MOVE

            val boardWithoutDuck = board.withoutDuck()

            /*
             * Trick to avoid multiple allocations of the lambda passed to move.moveAndRevert
             */
            val moveAnalyser = object : MoveVisitor {
                private lateinit var move: Move

                fun analyze(move: Move) {
                    this.move = move
                    move.moveAndRevert(boardWithoutDuck, this)
                }

                override fun invoke(isReversible: Boolean) {
                    boardWithoutDuck.phase = nextPhase

                    val nextRemainingDepth = when {
                        remainingDepth == 1 -> 0
                        remainingDepth <= finalHorizontDepth && (move is CaptureMove || move is PawnPromotionMove) -> remainingDepth - 1
                        else -> maxOf(0, remainingDepth - finalHorizontDepth)
                    }

                    val moveEval = NodeEvaluation(
                        parentNode = this@NodeEvaluation,
                        enteredWithReversibleMove = isReversible,
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
                            atLeast = minOf(beta + 1, negScore(moveEval.scoreA)),
                            exceptBlockingMove = move,
                            exceptCoord = moveEval.duckPosA,
                            newSelectedMove = move,
                            newDuckMove = moveEval.duckPosA
                        )
                        scoredCoordSet.update(
                            atLeast = minOf(beta + 1, negScore(moveEval.scoreB)),
                            exceptBlockingMove = move,
                            exceptCoord = moveEval.duckPosB,
                            newSelectedMove = move,
                            newDuckMove = moveEval.duckPosB
                        )
                        currentEval = scoredCoordSet.getBottomTwo()

                        alpha = maxOf(alpha, currentEval.scoreA)
                    }

                    if (bestMovePreviousDepth == move) {
                        bestMovePreviousDepthAnalysed = true
                    }

                }
            }

            try {
                var moveI = 0
                while (moveI < moveCount) {
                    val move = allMovesArray[moveI++].move

                    if (currentEval.scoreA >= -immediateBeta.scoreA) {
                        debug { "${move.text(board)} cut-off A" }
                        return@evaluateMovesRecursively null
                    }
                    if (currentEval.duckPosA == immediateBeta.duckPosA) {
                        if (currentEval.scoreB > -immediateBeta.scoreA) {
                            debug { "${move.text(board)} cut-off B" }
                            return@evaluateMovesRecursively null
                        }
                    }


                    debug { "Evaluating ${move.text(board)} alpha=${alpha} beta=${beta} wscA =${boardWithoutDuck.whiteShortCastlingAllowed}" }

                    moveAnalyser.analyze(move)

                    debug { " new gA: ${alpha}" }
                    debug { " new s: ${scoredCoordSet.text(board)}" }

                    if (alpha > beta) {
                        currentEval = scoredCoordSet.getBottomTwo()
                        currentEval.scoreA = alpha
                        currentEval.scoreB = alpha

                        val prunedScore = currentEval.toImmutable()
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
                }
            } catch (e: Stop) {
                if (bestMovePreviousDepth != null && bestMovePreviousDepthAnalysed && board.duckPosition != null) {
                    println(" Time has run out, but the previous best move (${bestMovePreviousDepth.text(board)}) has already been analysed")
                    return currentEval.toImmutable()
                } else {
                    throw e
                }
            }

            val result = scoredCoordSet.getBottomTwo().toImmutable()
            transpositionCache.set(board, remainingDepth = remainingDepth, score = result, alphaArg, beta)

            debug { "Full return ${result}" }

            return result
        }

        private fun drawEval(board: Board): BoardEval {
            var coordA: Coord = Coord(-1)
            var coordB: Coord = Coord(-1)
            Coord.forEach { coord ->
                val piece = board[coord]
                if (piece == Piece.EMPTY) {
                    if (coordA.index == -1) {
                        coordA = coord
                    } else if (coordB.index == -1) {
                        coordB = coord
                        return@forEach
                    }
                }
            }

            return BoardEval(scoreA = 0, duckPosA = coordA, scoreB = 0, duckPosB = coordB)
        }

        private fun generateMoves(allMoves: Array<MoveWithScore>): Int {
            val moveCount = singletonMoveGeneratorConsumer.generateMoves(board, allMoves)

            var i = 0
            while (i < moveCount) {
                val moveWithScore = allMoves[i]
                val move = moveWithScore.move
                var score = moveStrengthEstimation(move, board, areWeWhite)
                if (move === bestMoveFromCache) {
                    score += 1000
                } else if (move === secondBestMoveFromCache) {
                    score += 300
                }
                moveWithScore.score = score
                i++
            }

            allMoves.sort(0, moveCount)
            return moveCount
        }

        private fun currentBoardAlreadySeenInCallStack(): Boolean {
            var node: NodeEvaluation = this
            val currentHash = this.hashOfBoard
            do {
                if (!node.enteredWithReversibleMove) return false
                val parentNode: NodeEvaluation = node.parentNode ?: return false
                val parentParent: NodeEvaluation = parentNode.parentNode ?: return false
                if (!parentNode.enteredWithReversibleMove) return false
                node = parentParent
                if (node.hashOfBoard == currentHash) return true
            } while (true)
        }
    }


    private fun checkTimeout() {
        cutOffTime?.let { if (System.currentTimeMillis() > it.toEpochMilli()) throw Stop() }
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
            GameResult.WHITE_WON -> BoardEval.weHaveJustLostBoardEval
            GameResult.BLACK_WON -> BoardEval.weHaveJustLostBoardEval
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
        val array: Array<MoveWithScore> = t[nextFree++]
        try {
            return consumer(array)
        } finally {
            nextFree--
        }
    }
}


private val singletonMoveGeneratorConsumer = object : (Move) -> Unit {
    var counter = 0
    var arrayToFill: Array<MoveWithScore> = Array(0) { TODO() }
    override fun invoke(move: Move) {
        arrayToFill[counter++].move = move
    }

    /*
     * Non thread-safe
     * Non reentrant-safe
     */
    fun generateMoves(board: Board, arrayToFill: Array<MoveWithScore>): Int {
        counter = 0
        this.arrayToFill = arrayToFill
        board.generateMoves(this)
        return counter
    }
}

