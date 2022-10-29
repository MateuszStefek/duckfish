package duckchess

import duckchess.Coord.Companion.B1
import duckchess.Coord.Companion.B8
import duckchess.Coord.Companion.C1
import duckchess.Coord.Companion.C8
import duckchess.Coord.Companion.D1
import duckchess.Coord.Companion.D8
import duckchess.Coord.Companion.F1
import duckchess.Coord.Companion.F8
import duckchess.Coord.Companion.G1
import duckchess.Coord.Companion.G8
import duckchess.Piece.Companion.BLACK_BISHOP
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

class Board(
    val b: ByteArray,
    var phase: Phase,
    var result: GameResult = GameResult.UNDECIDED,
    var whiteShortCastlingAllowed: Boolean,
    var whiteLongCastlingAllowed: Boolean,
    var blackShortCastlingAllowed: Boolean,
    var blackLongCastlingAllowed: Boolean,
    var enPassantColumn: Byte = -1,
    var duckPosition: Coord?,
    val zobristHash: MutableZobristHash
) {

    fun copyBoard(): Board {
        return Board(
            b.clone(),
            phase,
            result,
            whiteShortCastlingAllowed,
            whiteLongCastlingAllowed,
            blackShortCastlingAllowed,
            blackLongCastlingAllowed,
            enPassantColumn,
            duckPosition,
            zobristHash.clone()
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (!(other is Board)) {
            return false
        }
        return other.b contentEquals this.b &&
            other.phase == this.phase &&
            other.whiteShortCastlingAllowed == this.whiteShortCastlingAllowed &&
            other.whiteLongCastlingAllowed == this.whiteLongCastlingAllowed &&
            other.blackShortCastlingAllowed == this.blackShortCastlingAllowed &&
            other.blackLongCastlingAllowed == this.blackLongCastlingAllowed &&
            other.enPassantColumn == this.enPassantColumn &&
            other.duckPosition == this.duckPosition
    }

    companion object {
        fun apOf(pieces: Map<Coord, Piece>, phase: Phase = Phase.WHITE_PIECE_MOVE): Board {
            val b = ByteArray(64) { EMPTY.code }
            val board = Board(
                b = b,
                phase = phase,
                whiteLongCastlingAllowed = true,
                whiteShortCastlingAllowed = true,
                blackLongCastlingAllowed = true,
                blackShortCastlingAllowed = true,
                enPassantColumn = -1,
                duckPosition = Coord.firstOrNull { b[it.index] == DUCK.code },
                zobristHash = MutableZobristHash(0, 0)
            )
            pieces.forEach { (position, piece) -> board[position] = piece }
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
        from.diagonalUpRight { to -> moveThrough(from, to, DiagonalSimpleMove::of, DiagonalCaptureMove::of, consumer, areWeWhite) }
        from.diagonalUpLeft { to -> moveThrough(from, to, DiagonalSimpleMove::of, DiagonalCaptureMove::of, consumer, areWeWhite) }
        from.diagonalDownLeft { to -> moveThrough(from, to, DiagonalSimpleMove::of, DiagonalCaptureMove::of, consumer, areWeWhite) }
        from.diagonalDownRight { to -> moveThrough(from, to, DiagonalSimpleMove::of, DiagonalCaptureMove::of, consumer, areWeWhite) }
    }

    private fun rookMoves(from: Coord, consumer: (Move) -> Unit, areWeWhite: Boolean) {
        from.right { to -> moveThrough(from, to, RookSimpleMove::of, RookCaptureMove::of, consumer, areWeWhite) }
        from.left { to -> moveThrough(from, to, RookSimpleMove::of, RookCaptureMove::of, consumer, areWeWhite) }
        from.up { to -> moveThrough(from, to, RookSimpleMove::of, RookCaptureMove::of, consumer, areWeWhite) }
        from.down { to -> moveThrough(from, to, RookSimpleMove::of, RookCaptureMove::of, consumer, areWeWhite) }
    }

    private fun queenMoves(from: Coord, consumer: (Move) -> Unit, areWeWhite: Boolean) {
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
                consumer(PawnPromotionMove(from, oneUp, WHITE_KNIGHT))
                consumer(PawnPromotionMove(from, oneUp, WHITE_BISHOP))
                consumer(PawnPromotionMove(from, oneUp, WHITE_ROOK))
                consumer(PawnPromotionMove(from, oneUp, WHITE_QUEEN))
            }
            from.iWhitePawnCaptures { captureSquare ->
                if (get(captureSquare).isBlack()) {
                    consumer(PawnCapturePromotionMove(from, captureSquare, WHITE_KNIGHT))
                    consumer(PawnCapturePromotionMove(from, captureSquare, WHITE_BISHOP))
                    consumer(PawnCapturePromotionMove(from, captureSquare, WHITE_ROOK))
                    consumer(PawnCapturePromotionMove(from, captureSquare, WHITE_QUEEN))
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
                consumer(PawnPromotionMove(from, oneDown, BLACK_KNIGHT))
                consumer(PawnPromotionMove(from, oneDown, BLACK_BISHOP))
                consumer(PawnPromotionMove(from, oneDown, BLACK_ROOK))
                consumer(PawnPromotionMove(from, oneDown, BLACK_QUEEN))
            }
            from.iBlackPawnCaptures { captureSquare ->
                if (get(captureSquare).isWhite()) {
                    consumer(PawnCapturePromotionMove(from, captureSquare, BLACK_KNIGHT))
                    consumer(PawnCapturePromotionMove(from, captureSquare, BLACK_BISHOP))
                    consumer(PawnCapturePromotionMove(from, captureSquare, BLACK_ROOK))
                    consumer(PawnCapturePromotionMove(from, captureSquare, BLACK_QUEEN))
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
            if (this[F1] == EMPTY && this[G1] == EMPTY) {
                consumer(WhiteShortCastleMove.instance)
            }
        }
        if (whiteLongCastlingAllowed) {
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
                Piece.BLACK_KING -> kingMoves(from, consumer, false)
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
            if (this[F8] == EMPTY && this[G8] == EMPTY) {
                consumer(BlackShortCastleMove.instance)
            }
        }
        if (blackLongCastlingAllowed) {
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
                append("-\n")
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
            append("- ep: ${enPassantColumn}")
            append("\n")

            toString()
        }
    }
}
