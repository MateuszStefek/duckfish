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
import duckchess.Piece.Companion.BLACK_BISHOP
import duckchess.Piece.Companion.BLACK_KING
import duckchess.Piece.Companion.BLACK_KNIGHT
import duckchess.Piece.Companion.BLACK_QUEEN
import duckchess.Piece.Companion.BLACK_ROOK
import duckchess.Piece.Companion.DUCK
import duckchess.Piece.Companion.EMPTY
import duckchess.Piece.Companion.WHITE_BISHOP
import duckchess.Piece.Companion.WHITE_KING
import duckchess.Piece.Companion.WHITE_KNIGHT
import duckchess.Piece.Companion.WHITE_QUEEN
import duckchess.Piece.Companion.WHITE_ROOK
import java.lang.IllegalStateException

private const val WHITE_SHORT_CASTLING_MASK: Int = 1
private const val WHITE_LONG_CASTLING_MASK: Int = 2
private const val BLACK_SHORT_CASTLING_MASK: Int = 4
private const val BLACK_LONG_CASTLING_MASK: Int = 8


class Board private constructor(
    val b: ByteArray,
    private var phaseVal: Phase,
    var result: GameResult = GameResult.UNDECIDED,
    private var castlingBits: Int,
    var duckPosition: Coord?,
    private var enPassantColValue: Byte = -1,
    val zobristHash: MutableZobristHash,
    var elementsLeft: Int
) {
    var phase: Phase
        get() = this.phaseVal
        set(value) {
            val from = this.phaseVal
            this.phaseVal = value
            zobristHash.updatePhase(from, value)
        }

    var enPassantColumn: Byte
        get() = this.enPassantColValue
        set(value) {
            val from = this.enPassantColValue
            this.enPassantColValue = value
            zobristHash.updateEnPassant(from, value)
        }

    var whiteShortCastlingAllowed: Boolean
        get() = (castlingBits and WHITE_SHORT_CASTLING_MASK) != 0
        set(value) {
            if (this.whiteShortCastlingAllowed == value) return
            val from = this.castlingBits
            this.castlingBits = this.castlingBits xor WHITE_SHORT_CASTLING_MASK
            zobristHash.updateCastlingBits(from, this.castlingBits)
        }

    var whiteLongCastlingAllowed: Boolean
        get() = (castlingBits and WHITE_LONG_CASTLING_MASK) != 0
        set(value) {
            if (this.whiteLongCastlingAllowed == value) return
            val from = this.castlingBits
            this.castlingBits = this.castlingBits xor WHITE_LONG_CASTLING_MASK
            zobristHash.updateCastlingBits(from, this.castlingBits)
        }

    var blackShortCastlingAllowed: Boolean
        get() = (castlingBits and BLACK_SHORT_CASTLING_MASK) != 0
        set(value) {
            if (this.blackShortCastlingAllowed == value) return
            val from = this.castlingBits
            this.castlingBits = this.castlingBits xor BLACK_SHORT_CASTLING_MASK
            zobristHash.updateCastlingBits(from, this.castlingBits)
        }

    var blackLongCastlingAllowed: Boolean
        get() = (castlingBits and BLACK_LONG_CASTLING_MASK) != 0
        set(value) {
            if (this.blackLongCastlingAllowed == value) return
            val from = this.castlingBits
            this.castlingBits = this.castlingBits xor BLACK_LONG_CASTLING_MASK
            zobristHash.updateCastlingBits(from, this.castlingBits)
        }

    var castlingBitsPublic: Int
        get() = castlingBits
        set(value) {
            if (this.castlingBits == value) return
            val from = this.castlingBits
            this.castlingBits = value
            zobristHash.updateCastlingBits(from, value)
        }

    fun copyBoard(): Board {
        val result = Board(
            b = b.clone(),
            phaseVal = phaseVal,
            result = result,
            castlingBits = castlingBits,
            duckPosition = duckPosition,
            zobristHash = zobristHash.clone(),
            enPassantColValue = enPassantColValue,
            elementsLeft = elementsLeft,
        )
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (!(other is Board)) {
            return false
        }
        return other.b contentEquals this.b &&
                other.phase == this.phase &&
                other.castlingBits == this.castlingBits &&
                other.enPassantColumn == this.enPassantColumn &&
                other.duckPosition == this.duckPosition
    }

    companion object {
        fun apOf(pieces: Map<Coord, Piece>, phase: Phase = Phase.WHITE_PIECE_MOVE): Board {
            val b = ByteArray(64) { EMPTY.code }
            val board = Board(
                b = b,
                phaseVal = Phase.WHITE_PIECE_MOVE,
                castlingBits = 0,
                duckPosition = null,
                zobristHash = MutableZobristHash(0/*, 0*/),
                elementsLeft = 0
            )

            pieces.forEach { (position, piece) ->
                board[position] = piece
                if (piece != DUCK && piece != EMPTY) {
                    board.elementsLeft++
                }
            }

            board.whiteShortCastlingAllowed = board[E1] == WHITE_KING && board[H1] == WHITE_ROOK
            board.blackShortCastlingAllowed = board[E8] == BLACK_KING && board[H8] == BLACK_ROOK
            board.whiteLongCastlingAllowed = board[E1] == WHITE_KING && board[A1] == WHITE_ROOK
            board.blackLongCastlingAllowed = board[E8] == BLACK_KING && board[A8] == BLACK_ROOK
            board.duckPosition = Coord.firstOrNull { board[it] == DUCK }
            return board
        }
    }

    /*** MOVE GEN *************/

    fun generateMoves(consumer: (Move) -> Unit) {
        when (phase) {
            Phase.WHITE_PIECE_MOVE -> whitePieceMoves(consumer)
            Phase.WHITE_DUCK_MOVE -> duckMoves(consumer)
            Phase.BLACK_PIECE_MOVE -> blackPieceMoves(consumer)
            Phase.BLACK_DUCK_MOVE -> duckMoves(consumer)
        }
    }

    private fun duckMoves(consumer: (Move) -> Unit) {
        Coord.forEach { to ->
            if (to != duckPosition) {
                if (get(to) == EMPTY) {
                    consumer(DuckMove.of(to))
                }
            }
        }
    }

    private inline fun moveThrough(
        from: Coord,
        to: Coord,
        simpleMove: (Coord, Coord) -> Move,
        captureMove: (Coord, Coord) -> Move,
        consumer: (Move) -> Unit,
        areWeWhite: Boolean
    ): Boolean {
        val destinationPiece = get(to)

        if (destinationPiece == EMPTY) {
            consumer(simpleMove(from, to))
            return true
        } else {
            if (areWeWhite) {
                if (destinationPiece.isBlack()) {
                    consumer(captureMove(from, to))
                }
            } else {
                if (destinationPiece.isWhite()) {
                    consumer(captureMove(from, to))
                }
            }
            return false
        }
    }

    private fun bishopMoves(from: Coord, consumer: (Move) -> Unit, areWeWhite: Boolean) {
        from.diagonalUpRight { to ->
            moveThrough(
                from,
                to,
                DiagonalSimpleMove::of,
                DiagonalCaptureMove::of,
                consumer,
                areWeWhite
            )
        }
        from.diagonalUpLeft { to ->
            moveThrough(
                from,
                to,
                DiagonalSimpleMove::of,
                DiagonalCaptureMove::of,
                consumer,
                areWeWhite
            )
        }
        from.diagonalDownLeft { to ->
            moveThrough(
                from,
                to,
                DiagonalSimpleMove::of,
                DiagonalCaptureMove::of,
                consumer,
                areWeWhite
            )
        }
        from.diagonalDownRight { to ->
            moveThrough(
                from,
                to,
                DiagonalSimpleMove::of,
                DiagonalCaptureMove::of,
                consumer,
                areWeWhite
            )
        }
    }

    private fun rookMoves(from: Coord, consumer: (Move) -> Unit, areWeWhite: Boolean) {
        from.right { to -> moveThrough(from, to, RookSimpleMove::of, RookCaptureMove::of, consumer, areWeWhite) }
        from.left { to -> moveThrough(from, to, RookSimpleMove::of, RookCaptureMove::of, consumer, areWeWhite) }
        from.up { to -> moveThrough(from, to, RookSimpleMove::of, RookCaptureMove::of, consumer, areWeWhite) }
        from.down { to -> moveThrough(from, to, RookSimpleMove::of, RookCaptureMove::of, consumer, areWeWhite) }
    }

    private inline fun queenMoves(from: Coord, noinline consumer: (Move) -> Unit, areWeWhite: Boolean) {
        bishopMoves(from, consumer, areWeWhite)
        rookMoves(from, consumer, areWeWhite)
    }

    private fun kingMoves(from: Coord, consumer: (Move) -> Unit, areWeWhite: Boolean) {
        from.iNeighbours { to ->
            val destinationPiece = get(to)
            if (destinationPiece == EMPTY) {
                consumer(KingSimpleMove.of(from, to))
            } else {
                if (areWeWhite) {
                    if (destinationPiece.isBlack()) {
                        consumer(KingCaptureMove.of(from, to))
                    }
                } else {
                    if (destinationPiece.isWhite()) {
                        consumer(KingCaptureMove.of(from, to))
                    }
                }
            }
        }
    }

    private fun knightMoves(from: Coord, consumer: (Move) -> Unit, areWeWhite: Boolean) {
        KnightMove.generate(from) { to ->
            val destinationPiece = get(to)
            if (destinationPiece == EMPTY) {
                consumer(KnightSimpleMove.of(from, to))
            } else {
                if (destinationPiece != DUCK) {
                    if (destinationPiece.isWhite() xor areWeWhite) {
                        consumer(KnightCaptureMove.of(from, to))
                    }
                }
            }
        }
    }

    private fun whitePawnMoves(from: Coord, consumer: (Move) -> Unit) {
        val oneUp: Coord = from.oneUp()
        val twoUp = from.twoUp()
        if (from.isSecondChessRow()) {
            if (get(oneUp) == EMPTY) {
                consumer(PawnOneStepMove.of(from, oneUp))
                if (get(twoUp) == EMPTY) {
                    consumer(PawnTwoStepMove.of(from, twoUp))
                }
            }
        } else if (!from.isSeventhChessRow()) {
            if (get(oneUp) == EMPTY) {
                consumer(PawnOneStepMove.of(from, oneUp))
            }
        } else {
            // seventh row
            if (get(oneUp) == EMPTY) {
                consumer(PawnPromotionMove.of(from, oneUp, WHITE_QUEEN))
                // In Duck Chess, there's never a reason to promote to a Rook or to a Bishop
                consumer(PawnPromotionMove.of(from, oneUp, WHITE_KNIGHT))
            }
            from.iWhitePawnCaptures { captureSquare ->
                if (get(captureSquare).isBlack()) {
                    consumer(PawnCapturePromotionMove.of(from, captureSquare, WHITE_QUEEN))
                    consumer(PawnCapturePromotionMove.of(from, captureSquare, WHITE_KNIGHT))
                }
            }
        }

        if (!from.isSeventhChessRow()) {
            from.iWhitePawnCaptures { captureSquare ->
                if (get(captureSquare).isBlack()) consumer(PawnCaptureMove.of(from, captureSquare))
            }
        }
    }

    private fun blackPawnMoves(from: Coord, consumer: (Move) -> Unit) {
        val oneDown: Coord = from.oneDown()
        val twoDown = from.twoDown()
        if (from.isSeventhChessRow()) {
            if (get(oneDown) == EMPTY) {
                consumer(PawnOneStepMove.of(from, oneDown))
                if (get(twoDown) == EMPTY) {
                    consumer(PawnTwoStepMove.of(from, twoDown))
                }
            }
        } else if (!from.isSecondChessRow()) {
            if (get(oneDown) == EMPTY) {
                consumer(PawnOneStepMove.of(from, oneDown))
            }
        } else {
            if (get(oneDown) == EMPTY) {
                consumer(PawnPromotionMove.of(from, oneDown, BLACK_QUEEN))
                // In Duck Chess, there's never a reason to promote to a Rook or to a Bishop
                consumer(PawnPromotionMove.of(from, oneDown, BLACK_KNIGHT))
            }
            from.iBlackPawnCaptures { captureSquare ->
                if (get(captureSquare).isWhite()) {
                    consumer(PawnCapturePromotionMove.of(from, captureSquare, BLACK_QUEEN))
                    consumer(PawnCapturePromotionMove.of(from, captureSquare, BLACK_KNIGHT))
                }
            }
        }

        if (!from.isSecondChessRow()) {
            from.iBlackPawnCaptures { captureSquare ->
                if (get(captureSquare).isWhite()) consumer(PawnCaptureMove.of(from, captureSquare))
            }
        }
    }

    private fun whitePieceMoves(consumer: (Move) -> Unit) {
        Coord.forEach { from ->
            when (get(from)) {
                WHITE_BISHOP -> bishopMoves(from, consumer, true)
                WHITE_ROOK -> rookMoves(from, consumer, true)
                WHITE_QUEEN -> queenMoves(from, consumer, true)
                WHITE_KING -> kingMoves(from, consumer, true)
                WHITE_KNIGHT -> knightMoves(from, consumer, true)
                Piece.WHITE_PAWN -> whitePawnMoves(from, consumer)
            }
        }
        if (enPassantColumn >= 0) {
            val to = Coord(Coord.A4.index + enPassantColumn)
            if (enPassantColumn > 0) {
                val from = Coord(to.index - 1)
                if (get(from) == Piece.WHITE_PAWN) {
                    consumer(EnPassantMove.of(from, to))
                }
            }
            if (enPassantColumn < 7) {
                val from = Coord(to.index + 1)
                if (get(from) == Piece.WHITE_PAWN) {
                    consumer(EnPassantMove.of(from, to))
                }
            }
        }

        if (whiteShortCastlingAllowed) {
            if (this[E1] != WHITE_KING || this[H1] != WHITE_ROOK) {
                throw IllegalStateException("AAA")
            }
            if (this[F1] == EMPTY && this[G1] == EMPTY) {
                consumer(WhiteShortCastleMove.instance)
            }
        }
        if (whiteLongCastlingAllowed) {
            if (this[E1] != WHITE_KING || this[A1] != WHITE_ROOK) {
                throw IllegalStateException("AAA")
            }
            if (this[B1] == EMPTY && this[C1] == EMPTY && this[D1] == EMPTY) {
                consumer(WhiteLongCastleMove.instance)
            }
        }
    }

    private fun blackPieceMoves(consumer: (Move) -> Unit) {
        Coord.forEach { from ->
            when (get(from)) {
                BLACK_BISHOP -> bishopMoves(from, consumer, false)
                BLACK_ROOK -> rookMoves(from, consumer, false)
                BLACK_QUEEN -> queenMoves(from, consumer, false)
                BLACK_KING -> kingMoves(from, consumer, false)
                BLACK_KNIGHT -> knightMoves(from, consumer, false)
                Piece.BLACK_PAWN -> blackPawnMoves(from, consumer)
            }
        }
        if (enPassantColumn >= 0) {
            val to = Coord(Coord.A5.index + enPassantColumn)
            if (enPassantColumn > 0) {
                val from = Coord(to.index - 1)
                if (get(from) == Piece.BLACK_PAWN) {
                    consumer(EnPassantMove.of(from, to))
                }
            }
            if (enPassantColumn < 7) {
                val from = Coord(to.index + 1)
                if (get(from) == Piece.BLACK_PAWN) {
                    consumer(EnPassantMove.of(from, to))
                }
            }
        }
        if (blackShortCastlingAllowed) {
            if (this[E8] != BLACK_KING || this[H8] != BLACK_ROOK) {
                throw IllegalStateException("AAA")
            }
            if (this[F8] == EMPTY && this[G8] == EMPTY) {
                consumer(BlackShortCastleMove.instance)
            }
        }
        if (blackLongCastlingAllowed) {
            if (this[E8] != BLACK_KING || this[A8] != BLACK_ROOK) {
                throw IllegalStateException("AAA")
            }
            if (this[B8] == EMPTY && this[C8] == EMPTY && this[D8] == EMPTY) {
                consumer(BlackLongCastleMove.instance)
            }

        }
    }

    /********* END OF MOVE GEN ********************/

    fun nextPhase(): Phase = when (phase) {
        Phase.BLACK_PIECE_MOVE -> Phase.BLACK_DUCK_MOVE
        Phase.BLACK_DUCK_MOVE -> Phase.WHITE_PIECE_MOVE
        Phase.WHITE_PIECE_MOVE -> Phase.WHITE_DUCK_MOVE
        Phase.WHITE_DUCK_MOVE -> Phase.BLACK_PIECE_MOVE
    }

    private fun index(row: Row, col: Column): Int = row.row * 8 + col.column
    private operator fun get(row: Row, col: Column): Piece = Piece(b[index(row, col)])
    operator fun get(position: Coord): Piece = Piece(b[position.index])
    operator fun set(position: Coord, piece: Piece) {
        val originalPiece = Piece(b[position.index])
        b[position.index] = piece.code
        zobristHash.update(position, originalPiece, piece)
    }

    fun withoutDuck(): Board {
        val duckPosition = this.duckPosition
        if (duckPosition != null) {
            val copy = copyBoard()
            copy.duckPosition = null
            copy[duckPosition] = EMPTY
            return copy
        } else {
            return this
        }
    }

    fun text(): String {
        return with(StringBuilder()) {
            (7 downTo 0).forEach { row ->
                append("--".repeat(8))
                append("-")
                if (row == 7 && phase == Phase.BLACK_PIECE_MOVE) {
                    append(" *")
                }
                append("\n")
                (0..7).forEach { col ->
                    append("|")
                    val piece = get(Row(row), Column(col))
                    if (piece != EMPTY) {
                        append(piece.text())
                    } else {
                        append(" ")
                    }
                }
                append("|\n")
            }
            append("--".repeat(8))
            append("-")
            if (phase == Phase.WHITE_PIECE_MOVE) {
                append(" *")
            }
            append(" ep: ${enPassantColumn}")
            append("\n")

            toString()
        }
    }
}
