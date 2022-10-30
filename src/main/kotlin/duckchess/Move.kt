package duckchess

import duckchess.Coord.Companion.A1
import duckchess.Coord.Companion.A8
import duckchess.Coord.Companion.B1
import duckchess.Coord.Companion.B8
import duckchess.Coord.Companion.C1
import duckchess.Coord.Companion.C8
import duckchess.Coord.Companion.D1
import duckchess.Coord.Companion.D8
import duckchess.Coord.Companion.E1
import duckchess.Coord.Companion.E8
import duckchess.Coord.Companion.F1
import duckchess.Coord.Companion.F8
import duckchess.Coord.Companion.G1
import duckchess.Coord.Companion.G8
import duckchess.Coord.Companion.H1
import duckchess.Coord.Companion.H8
import duckchess.Move.Companion.updateCastlingRights
import duckchess.Piece.Companion.BLACK_KING
import duckchess.Piece.Companion.BLACK_ROOK
import duckchess.Piece.Companion.EMPTY
import duckchess.Piece.Companion.WHITE_KING
import duckchess.Piece.Companion.WHITE_ROOK
import java.lang.IllegalStateException

sealed interface Move {
    fun text(board: Board): String

    companion object {
        fun newResultAfterCapture(capturedPiece: Piece): GameResult = when (capturedPiece) {
            BLACK_KING -> GameResult.WHITE_WON
            WHITE_KING -> GameResult.BLACK_WON
            else -> GameResult.UNDECIDED
        }

        fun isSlidingMoveBlockedByDuck(from: Coord, to: Coord, duck: Coord): Boolean {
            return movesBlockedBy[from.index * 64 * 64 + to.index * 64 + duck.index]
        }

        @JvmStatic
        val movesBlockedBy = BooleanArray(64 * 64 * 64).apply {
            Coord.forEach { from ->

                fun g(generator: (Coord, (Coord) -> Boolean) -> Unit) {
                    generator(from) { duck ->
                        generator(duck) { to ->
                            set(from.index * 64 * 64 + to.index * 64 + duck.index, true)
                            true
                        }
                        set(from.index * 64 * 64 + duck.index * 64 + duck.index, true)
                        true
                    }
                }

                g(Coord::diagonalUpRight)
                g(Coord::diagonalUpLeft)
                g(Coord::diagonalDownLeft)
                g(Coord::diagonalDownRight)
                g(Coord::up)
                g(Coord::down)
                g(Coord::left)
                g(Coord::right)
            }
        }

        inline fun updateCastlingRights(board: Board, pos: Coord) = with(board) {
            when (pos) {
                A1 -> { whiteLongCastlingAllowed = false }
                E1 -> { whiteLongCastlingAllowed = false; whiteShortCastlingAllowed = false }
                H1 -> { whiteShortCastlingAllowed = false }
                A8 -> { blackLongCastlingAllowed = false }
                E8 -> { blackLongCastlingAllowed = false; blackShortCastlingAllowed = false }
                H8 -> { blackShortCastlingAllowed = false }
            }
        }
    }

    fun blockedByDuckAt(duckPos: Coord): Boolean = false

    fun moveAt(board: Board): Board {
        var boardCopy: Board? = null
        var originalEp = board.enPassantColumn
        moveAndRevert(board) {
            boardCopy = board.copyBoard()
        }
        val result = boardCopy!!
        if (this is DuckMove) {
            result.enPassantColumn = originalEp
        }
        //result.phase = result.nextPhase()
        return result
    }

    fun internalMoveImpl(board: Board, block: () -> Unit)

    fun moveAndRevert(board: Board, block: () -> Unit) {
        val castlingBitsOriginal = board.castlingBitsPublic
        val enPassantColumn = board.enPassantColumn
        val phase = board.phase
        val result = board.result

        board.enPassantColumn = -1

        board.phase = board.nextPhase()

        try {
            internalMoveImpl(board, block)
        } finally {

            board.castlingBitsPublic = castlingBitsOriginal
            board.enPassantColumn = enPassantColumn
            board.phase = phase
            board.result = result
        }
    }
}

interface SimpleMove : Move {
    val from: Coord
    val to: Coord

    override fun text(board: Board) = board[from].let { piece ->
        "${piece.text()}${from.text()}-${to.text()}"
    }

    override fun blockedByDuckAt(duckPos: Coord): Boolean {
        return Move.isSlidingMoveBlockedByDuck(from, to, duckPos)
    }

    override fun internalMoveImpl(board: Board, block: () -> Unit) {
        val piece = board[from]
        board[from] = EMPTY
        board[to] = piece
        updateCastlingRights(board, from)

        try {
            block()
        } finally {
            board[from] = piece
            board[to] = EMPTY
        }
    }
}

interface CaptureMove : Move {
    val from: Coord
    val to: Coord

    override fun text(board: Board) = board[from].let { piece ->
        "${piece.text()}${from.text()}X${to.text()}"
    }

    override fun internalMoveImpl(board: Board, block: () -> Unit) {
        val piece = board[from]
        val capturedPiece = board[to]
        board[from] = EMPTY
        board[to] = piece

        board.result = Move.newResultAfterCapture(capturedPiece)

        updateCastlingRights(board, from)
        updateCastlingRights(board, to)

        board.elementsLeft--

        try {
            block()

        } finally {

            board.elementsLeft++
            board[from] = piece
            board[to] = capturedPiece
        }
    }
}

class DiagonalSimpleMove private constructor(override val from: Coord, override val to: Coord) : SimpleMove {
    companion object {
        @JvmStatic
        private val instances = Array<DiagonalSimpleMove>(64 * 64) { idx ->
            val fromIdx = idx / 64
            val toIdx = idx % 64
            DiagonalSimpleMove(Coord(fromIdx), Coord(toIdx))
        }

        fun of(from: Coord, to: Coord): DiagonalSimpleMove {
            val idx = from.index * 64 + to.index
            return instances[idx]
        }
    }
}

class DiagonalCaptureMove private constructor(override val from: Coord, override val to: Coord) : CaptureMove {
    companion object {
        @JvmStatic
        private val instances = Array<DiagonalCaptureMove>(64 * 64) { idx ->
            val fromIdx = idx / 64
            val toIdx = idx % 64
            DiagonalCaptureMove(Coord(fromIdx), Coord(toIdx))
        }

        fun of(from: Coord, to: Coord): DiagonalCaptureMove {
            val idx = from.index * 64 + to.index
            return instances[idx]
        }
    }

    override fun blockedByDuckAt(duckPos: Coord): Boolean {
        return Move.isSlidingMoveBlockedByDuck(from, to, duckPos)
    }
}

class RookSimpleMove private constructor(override val from: Coord, override val to: Coord) : SimpleMove {
    companion object {
        @JvmStatic
        private val instances = Array<RookSimpleMove>(64 * 64) { idx ->
            val fromIdx = idx / 64
            val toIdx = idx % 64
            RookSimpleMove(Coord(fromIdx), Coord(toIdx))
        }

        fun of(from: Coord, to: Coord): RookSimpleMove {
            val idx = from.index * 64 + to.index
            return instances[idx]
        }
    }
}

class RookCaptureMove private constructor(override val from: Coord, override val to: Coord) : CaptureMove {
    companion object {
        @JvmStatic
        private val instances = Array<RookCaptureMove>(64 * 64) { idx ->
            val fromIdx = idx / 64
            val toIdx = idx % 64
            RookCaptureMove(Coord(fromIdx), Coord(toIdx))
        }

        fun of(from: Coord, to: Coord): RookCaptureMove {
            val idx = from.index * 64 + to.index
            return instances[idx]
        }
    }

    override fun blockedByDuckAt(duckPos: Coord): Boolean {
        return Move.isSlidingMoveBlockedByDuck(from, to, duckPos)
    }
}

interface KnightMove : Move {
    val from: Coord
    val to: Coord

    companion object {
        @JvmStatic
        public val internalKnightMovesCoords = Array<ByteArray>(64) { fromB: Int ->
            val from = Coord(fromB)
            val toMoves: MutableList<Coord> = mutableListOf()
            from.iKnight { toMoves.add(it) }
            ByteArray(toMoves.size) { idx -> toMoves[idx].index.toByte() }
        }

        public inline fun generate(from: Coord, consumer: (Coord) -> Unit) {
            internalKnightMovesCoords[from.index].forEach { toB ->
                consumer(Coord(toB.toInt()))
            }
        }
    }
}

class KnightSimpleMove private constructor(override val from: Coord, override val to: Coord) : SimpleMove, KnightMove {
    companion object {
        @JvmStatic
        val instances = Array<KnightSimpleMove>(64 * 64) { idx ->
            val fromIdx = idx / 64
            val toIdx = idx % 64
            KnightSimpleMove(Coord(fromIdx), Coord(toIdx))
        }

        fun of(from: Coord, to: Coord): KnightSimpleMove {
            val idx = from.index * 64 + to.index
            return instances[idx]
        }
    }
    override fun blockedByDuckAt(duckPos: Coord): Boolean = duckPos == to
}

class KnightCaptureMove private constructor(override val from: Coord, override val to: Coord) : CaptureMove, KnightMove {
    companion object {
        @JvmStatic
        val instances = Array<KnightCaptureMove>(64 * 64) { idx ->
            val fromIdx = idx / 64
            val toIdx = idx % 64
            KnightCaptureMove(Coord(fromIdx), Coord(toIdx))
        }

        fun of(from: Coord, to: Coord): KnightCaptureMove {
            val idx = from.index * 64 + to.index
            return instances[idx]
        }
    }
}

class KingSimpleMove private constructor(override val from: Coord, override val to: Coord) : SimpleMove {
    companion object {
        private val instances = Array<KingSimpleMove>(64 * 64) { idx ->
            val fromIdx = idx / 64
            val toIdx = idx % 64
            KingSimpleMove(Coord(fromIdx), Coord(toIdx))
        }

        fun of(from: Coord, to: Coord): KingSimpleMove {
            val idx = from.index * 64 + to.index
            return instances[idx]
        }
    }

    override fun internalMoveImpl(board: Board, block: () -> Unit) {
        val piece = board[from]
        board[from] = EMPTY
        board[to] = piece
        updateCastlingRights(board, from)

        try {
            block()
        } finally {
            board[from] = piece
            board[to] = EMPTY
        }
    }

    override fun blockedByDuckAt(duckPos: Coord): Boolean = duckPos == to
}

class KingCaptureMove private constructor(override val from: Coord, override val to: Coord) : CaptureMove {
    companion object {
        private val instances = Array<KingCaptureMove>(64 * 64) { idx ->
            val fromIdx = idx / 64
            val toIdx = idx % 64
            KingCaptureMove(Coord(fromIdx), Coord(toIdx))
        }

        fun of(from: Coord, to: Coord): KingCaptureMove {
            val idx = from.index * 64 + to.index
            return instances[idx]
        }
    }
}

class PawnOneStepMove private constructor(override val from: Coord, override val to: Coord) : SimpleMove {
    companion object {
        private val instances = Array<PawnOneStepMove>(64 * 64) { idx ->
            val fromIdx = idx / 64
            val toIdx = idx % 64
            PawnOneStepMove(Coord(fromIdx), Coord(toIdx))
        }

        fun of(from: Coord, to: Coord): PawnOneStepMove {
            val idx = from.index * 64 + to.index
            return instances[idx]
        }
    }

    override fun blockedByDuckAt(duckPos: Coord): Boolean = duckPos == to
}

class PawnTwoStepMove private constructor(val from: Coord, val to: Coord) : Move {
    companion object {
        private val instances = Array<PawnTwoStepMove>(64 * 64) { idx ->
            val fromIdx = idx / 64
            val toIdx = idx % 64
            PawnTwoStepMove(Coord(fromIdx), Coord(toIdx))
        }

        fun of(from: Coord, to: Coord): PawnTwoStepMove {
            val idx = from.index * 64 + to.index
            return instances[idx]
        }
    }

    override fun text(board: Board) = board[from].let { piece ->
        "${piece.text()}${from.text()}-${to.text()}"
    }

    override fun blockedByDuckAt(duckPos: Coord): Boolean {
        if (duckPos == to) return true
        if (duckPos.index == (from.index + to.index) / 2) return true
        return false
    }

    override fun internalMoveImpl(board: Board, block: () -> Unit) {
        val piece = board[from]
        board[from] = EMPTY
        board[to] = piece
        board.enPassantColumn = (from.index % 8).toByte()

        try {
            block()
        } finally {
            board[from] = piece
            board[to] = EMPTY
        }
    }
}

class PawnCaptureMove private constructor(override val from: Coord, override val to: Coord) : CaptureMove {
    companion object {
        private val instances = Array<PawnCaptureMove>(64 * 64) { idx ->
            val fromIdx = idx / 64
            val toIdx = idx % 64
            PawnCaptureMove(Coord(fromIdx), Coord(toIdx))
        }

        fun of(from: Coord, to: Coord): PawnCaptureMove {
            val idx = from.index * 64 + to.index
            return instances[idx]
        }
    }
}

class EnPassantMove private constructor(override val from: Coord, override val to: Coord) : Move, CaptureMove {
    companion object {
        private val instances = Array<EnPassantMove>(64 * 64) { idx ->
            val fromIdx = idx / 64
            val toIdx = idx % 64
            EnPassantMove(Coord(fromIdx), Coord(toIdx))
        }

        fun of(from: Coord, to: Coord): EnPassantMove {
            val idx = from.index * 64 + to.index
            return instances[idx]
        }
    }

    override fun text(board: Board) = board[from].let { piece ->
        "${piece.text()}${from.text()}x${to.text()}ep"
    }

    override fun blockedByDuckAt(duckPos: Coord): Boolean {
        return duckPos == to
    }

    override fun internalMoveImpl(board: Board, block: () -> Unit) {
        val piece = board[from]
        val capturedPiece = board[to]
        board[from] = EMPTY
        board[to] = EMPTY
        val dest = if (piece == Piece.WHITE_PAWN) to.oneUp() else to.oneDown()
        board[dest] = piece

        board.elementsLeft--

        try {
            block()
        } finally {
            board.elementsLeft--

            board[from] = piece
            board[to] = capturedPiece
            board[dest] = EMPTY
        }
    }
}

class PawnPromotionMove(val from: Coord, val to: Coord, val newPiece: Piece) : Move {
    override fun text(board: Board) = board[from].let { piece ->
        "${piece.text()}${from.text()}-${to.text()}${newPiece.text()}"
    }

    override fun blockedByDuckAt(duckPos: Coord): Boolean = duckPos == to

    override fun internalMoveImpl(board: Board, block: () -> Unit) {
        val piece = board[from]
        board[from] = EMPTY
        board[to] = newPiece

        block()

        board[from] = piece
        board[to] = EMPTY
    }
}

class PawnCapturePromotionMove(val from: Coord, val to: Coord, val newPiece: Piece) : Move {
    override fun text(board: Board) = board[from].let { piece ->
        "${piece.text()}${from.text()}X${to.text()}${newPiece.text()}"
    }

    override fun internalMoveImpl(board: Board, block: () -> Unit) {
        val piece = board[from]
        val capturedPiece = board[to]
        board[from] = EMPTY
        board[to] = newPiece
        updateCastlingRights(board, to)
        board.result = Move.newResultAfterCapture(capturedPiece)

        board.elementsLeft--

        try {
            block()
        } finally {
            board.elementsLeft++

            board[from] = piece
            board[to] = capturedPiece
        }
    }
}

class WhiteShortCastleMove private constructor() : Move {
    companion object {
        val instance = WhiteShortCastleMove()
    }

    override fun text(board: Board) = "0-0"

    override fun blockedByDuckAt(duckPos: Coord): Boolean = duckPos == G1 || duckPos == F1

    override fun internalMoveImpl(board: Board, block: () -> Unit) {
        board[E1] = EMPTY
        board[F1] = WHITE_ROOK
        board[G1] = WHITE_KING
        board[H1] = EMPTY

        board.whiteShortCastlingAllowed = false
        board.whiteLongCastlingAllowed = false

        try {
            block()
        } finally {

            board[E1] = WHITE_KING
            board[F1] = EMPTY
            board[G1] = EMPTY
            board[H1] = WHITE_ROOK
        }
    }
}

class WhiteLongCastleMove private constructor() : Move {
    companion object {
        val instance = WhiteLongCastleMove()
    }

    override fun text(board: Board) = "0-0-0"

    override fun blockedByDuckAt(duckPos: Coord): Boolean = duckPos == B1 || duckPos == C1 || duckPos == D1

    override fun internalMoveImpl(board: Board, block: () -> Unit) {
        board[A1] = EMPTY
        board[B1] = EMPTY
        board[C1] = WHITE_KING
        board[D1] = WHITE_ROOK
        board[E1] = EMPTY

        board.whiteShortCastlingAllowed = false
        board.whiteLongCastlingAllowed = false

        try {
            block()
        } finally {
            board[A1] = WHITE_ROOK
            board[B1] = EMPTY
            board[C1] = EMPTY
            board[D1] = EMPTY
            board[E1] = WHITE_KING
        }

    }
}

class BlackShortCastleMove private constructor() : Move {
    companion object {
        val instance = BlackShortCastleMove()
    }

    override fun text(board: Board) = "0-0"

    override fun blockedByDuckAt(duckPos: Coord): Boolean = duckPos == G8 || duckPos == F8

    override fun internalMoveImpl(board: Board, block: () -> Unit) {
        if (board[E8] != BLACK_KING) {
            throw IllegalStateException()
        }

        board[E8] = EMPTY
        board[F8] = BLACK_ROOK
        board[G8] = BLACK_KING
        board[H8] = EMPTY

        board.blackShortCastlingAllowed = false
        board.blackLongCastlingAllowed = false

        try {
            block()
        } finally {
            board[E8] = BLACK_KING
            board[F8] = EMPTY
            board[G8] = EMPTY
            board[H8] = BLACK_ROOK
        }
    }
}

class BlackLongCastleMove private constructor() : Move {
    companion object {
        val instance = BlackLongCastleMove()
    }

    override fun text(board: Board) = "0-0-0"

    override fun blockedByDuckAt(duckPos: Coord): Boolean = duckPos == B8 || duckPos == C8 || duckPos == D8

    override fun internalMoveImpl(board: Board, block: () -> Unit) {
        if (board[E8] != BLACK_KING) {
            throw IllegalStateException()
        }

        board[A8] = EMPTY
        board[B8] = EMPTY
        board[C8] = BLACK_KING
        board[D8] = BLACK_ROOK
        board[E8] = EMPTY

        board.blackShortCastlingAllowed = false
        board.blackLongCastlingAllowed = false

        try {
            block()
        } finally {
            board[A8] = BLACK_ROOK
            board[B8] = EMPTY
            board[C8] = EMPTY
            board[D8] = EMPTY
            board[E8] = BLACK_KING
        }
    }
}

class DuckMove private constructor(val to: Coord) : Move {
    companion object {
        private val instances = Array<DuckMove>(64) { idx ->
            DuckMove(Coord(idx))
        }

        fun of(to: Coord): DuckMove {
            return instances[to.index]
        }
    }

    override fun text(board: Board) = "${Piece.DUCK.text()}${to.text()}"

    override fun internalMoveImpl(board: Board, block: () -> Unit) {
        val currentDuckPos = board.duckPosition
        if (currentDuckPos != null) {
            board[currentDuckPos] = EMPTY
        }
        board[to] = Piece.DUCK
        board.duckPosition = to

        try {
            block()
        } finally {
            board[to] = EMPTY
            if (currentDuckPos != null) {
                board[currentDuckPos] = Piece.DUCK
            }
            board.duckPosition = currentDuckPos
        }
    }
}
