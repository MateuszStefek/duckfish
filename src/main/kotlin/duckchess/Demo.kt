package duckchess

import java.time.Duration
import kotlin.random.Random
import kotlin.system.measureTimeMillis

fun play(initialBoard: Board) {

    println("Starting position:\n${initialBoard.text()}")

    var currentBoard = initialBoard
    var moveNumber = 2

    val minMax = MinMax()

    while (currentBoard.result == GameResult.UNDECIDED) {
        //minMax.maxDepth = if (currentBoard.phase == Phase.WHITE_PIECE_MOVE) 7 else 4

        val selected: SelectedMove
        val millis = measureTimeMillis {
            selected = minMax.bestMove(currentBoard, Duration.ofSeconds(5 + Random.nextInt(10).toLong()))
        }

        //System.gc()
        var memUsage = with(Runtime.getRuntime()) {
            totalMemory() - freeMemory()
        }

        println(
            "Moving: $moveNumber. ${selected.text(currentBoard)}," +
                " evaluated positions: ${minMax.staticEvalCount}" +
                " visited nodes: ${minMax.visitedNodes}" +
                " evaluated in ${millis}ms " +
                " memUsage:$memUsage" +
                " cache: ${minMax.transpositionCache.maxSize}"
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
    board = PawnOneStepMove.of(Coord.D2, Coord.D3).moveAt(board)
    board = DuckMove.of(Coord.F6).moveAt(board)
    board = PawnTwoStepMove.of(Coord.E7, Coord.E5).moveAt(board)
    board = DuckMove.of(Coord.E7).moveAt(board)
    board = KnightSimpleMove.of(Coord.G1, Coord.F3).moveAt(board)
    board = DuckMove.of(Coord.D6).moveAt(board)

/*    board = parseBoard("""
        ----------------- *
        |r| |b|q|k|b|X|r|
        -----------------
        |p|n|p|p|p|p|p|p|
        -----------------
        | | | | | | | | |
        -----------------
        | |N| | | | | |Q|
        -----------------
        | |P| | |P| | | |
        -----------------
        | | | |P|B| | | |
        -----------------
        |P| |P| | |P|P|P|
        -----------------
        |R| | | |K|B|N|R|
        ----------------- ep: -1
    """.trimIndent())*/

/*    board = parseBoard("""
-----------------
| | |n|k|b| | | |
-----------------
| | | | | | | | |
-----------------
| | | | | | | | |
-----------------
| | | | | | | | |
-----------------
| | |K| | | | | |
-----------------
| | | | | | | | |
-----------------
| | | | | | | |B|
-----------------
| | | |r| | | | |
----------------- * ep: -1
    """.trimIndent())*/

/*
    board = parseBoard("""
        ----------------- *
        | | | | | | | | |
        -----------------
        | | | | |b| |k|X|
        -----------------
        | | |q| | | | |p|
        -----------------
        | |R|p| |p|N|p|P|
        -----------------
        |P| | |r| | | |R|
        -----------------
        | | | |P| | | | |
        -----------------
        | | |P| |K|P|B| |
        -----------------
        | | | | | | | | |
        ----------------- ep: -1
    """.trimIndent())
*/
    //board = positionUndecidedBug

    play(board)
}
