package duckchess

import kotlin.system.measureTimeMillis

fun play(initialBoard: Board) {

    println("Starting position:\n${initialBoard.text()}")

    var currentBoard = initialBoard
    var moveNumber = 2

    val minMax = MinMax(4)

    while (currentBoard.result == GameResult.UNDECIDED) {
        minMax.maxDepth = if (currentBoard.phase == Phase.WHITE_PIECE_MOVE) 7 else 4

        val selected: SelectedMove
        val millis = measureTimeMillis {
            selected = minMax.bestMove(currentBoard)
        }

        System.gc()
        var memUsage = with(Runtime.getRuntime()) {
            totalMemory() - freeMemory()
        }

        println(
            "Moving: $moveNumber. ${selected.text(currentBoard)}," +
                " depth: ${minMax.maxDepth}" +
                " evaluated positions: ${minMax.counter}" +
                " evaluated in ${millis}ms " +
                " memUsage:$memUsage" +
                " cache: ${minMax.transpositionCache.cache.size()} ${minMax.transpositionCache.cache.stats()}"
        )

        currentBoard = selected.move.moveAt(currentBoard)
        currentBoard = selected.duckMove.moveAt(currentBoard)
        if (currentBoard.phase == Phase.WHITE_PIECE_MOVE) {
            moveNumber++
        }

        val text = currentBoard.text()

        println(text)
    }

    println("Result: ${currentBoard.result}")
}

fun main() {
    var board = initialPosition

    board = PawnTwoStepMove.of(Coord.E2, Coord.E4).moveAt(board)
    board = DuckMove.of(Coord.E6).moveAt(board)
    board = KnightSimpleMove.of(Coord.B8, Coord.C6).moveAt(board)
    board = DuckMove.of(Coord.E2).moveAt(board)

    play(board)
}
