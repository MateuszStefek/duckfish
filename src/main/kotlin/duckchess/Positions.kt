package duckchess

val initialPosition = Board.apOf(
    mapOf(
        Coord.A1 to Piece.WHITE_ROOK,
        Coord.B1 to Piece.WHITE_KNIGHT,
        Coord.C1 to Piece.WHITE_BISHOP,
        Coord.D1 to Piece.WHITE_QUEEN,
        Coord.E1 to Piece.WHITE_KING,
        Coord.F1 to Piece.WHITE_BISHOP,
        Coord.G1 to Piece.WHITE_KNIGHT,
        Coord.H1 to Piece.WHITE_ROOK,

        Coord.A2 to Piece.WHITE_PAWN,
        Coord.B2 to Piece.WHITE_PAWN,
        Coord.C2 to Piece.WHITE_PAWN,
        Coord.D2 to Piece.WHITE_PAWN,
        Coord.E2 to Piece.WHITE_PAWN,
        Coord.F2 to Piece.WHITE_PAWN,
        Coord.G2 to Piece.WHITE_PAWN,
        Coord.H2 to Piece.WHITE_PAWN,

        Coord.A7 to Piece.BLACK_PAWN,
        Coord.B7 to Piece.BLACK_PAWN,
        Coord.C7 to Piece.BLACK_PAWN,
        Coord.D7 to Piece.BLACK_PAWN,
        Coord.E7 to Piece.BLACK_PAWN,
        Coord.F7 to Piece.BLACK_PAWN,
        Coord.G7 to Piece.BLACK_PAWN,
        Coord.H7 to Piece.BLACK_PAWN,

        Coord.A8 to Piece.BLACK_ROOK,
        Coord.B8 to Piece.BLACK_KNIGHT,
        Coord.C8 to Piece.BLACK_BISHOP,
        Coord.D8 to Piece.BLACK_QUEEN,
        Coord.E8 to Piece.BLACK_KING,
        Coord.F8 to Piece.BLACK_BISHOP,
        Coord.G8 to Piece.BLACK_KNIGHT,
        Coord.H8 to Piece.BLACK_ROOK

        // Coord.C3 to duckchess.Piece.BLACK_KNIGHT,
    )
)

val bug1Position = Board.apOf(
    phase = Phase.BLACK_PIECE_MOVE,
    pieces = mapOf(
        Coord.C6 to Piece.DUCK,

        Coord.A1 to Piece.WHITE_ROOK,
        Coord.F3 to Piece.WHITE_KNIGHT,
        Coord.C1 to Piece.WHITE_BISHOP,
        Coord.G5 to Piece.WHITE_QUEEN,
        Coord.E1 to Piece.WHITE_KING,
        Coord.B5 to Piece.WHITE_BISHOP,
        Coord.F3 to Piece.WHITE_KNIGHT,
        Coord.H1 to Piece.WHITE_ROOK,

        Coord.A2 to Piece.WHITE_PAWN,
        Coord.B2 to Piece.WHITE_PAWN,
        Coord.C2 to Piece.WHITE_PAWN,
        Coord.D2 to Piece.WHITE_PAWN,
        Coord.E4 to Piece.WHITE_PAWN,
        Coord.F2 to Piece.WHITE_PAWN,
        Coord.G2 to Piece.WHITE_PAWN,
        Coord.H2 to Piece.WHITE_PAWN,

        Coord.A4 to Piece.BLACK_PAWN,
        Coord.B7 to Piece.BLACK_PAWN,
        Coord.C7 to Piece.BLACK_PAWN,
        Coord.D6 to Piece.BLACK_PAWN,
        Coord.E5 to Piece.BLACK_PAWN,
        Coord.F7 to Piece.BLACK_PAWN,
        // Coord.G7 to duckchess.Piece.BLACK_PAWN,
        Coord.H6 to Piece.BLACK_PAWN,
        // Coord.G5 to duckchess.Piece.BLACK_PAWN,

        Coord.A8 to Piece.BLACK_ROOK,
        Coord.B8 to Piece.BLACK_KNIGHT,
        Coord.C8 to Piece.BLACK_BISHOP,
        Coord.D8 to Piece.BLACK_QUEEN,
        Coord.E8 to Piece.BLACK_KING,
        Coord.F8 to Piece.BLACK_BISHOP,
        Coord.F6 to Piece.BLACK_KNIGHT,
        Coord.H8 to Piece.BLACK_ROOK,

        // Coord.C3 to duckchess.Piece.BLACK_KNIGHT,
    )
)

/**
 * Bug White seen the pawn as a guaranteed capture,
 * so it never captured it, actually.
 */
val positionUndecidedBug = parseBoard(
    """
    ----------------- *
    | | | | | | | | |
    -----------------
    | | | | | | | | |
    -----------------
    | | | | | | |k| |
    -----------------
    | | | | | | |p|R|
    -----------------
    | | | | | | |X| |
    -----------------
    | | | | |K|B| |P|
    -----------------
    | | | | | | | | |
    -----------------
    | | | | | | | | |
    ----------------- ep: -1
""".trimIndent()
)

// Drawish in general
val positionKRBPvsKR = parseBoard(
    """
    ----------------- *
    | | | | | | |k| |
    -----------------
    | | | | | |r| | |
    -----------------
    | | | | | |b| | |
    -----------------
    | | | | | | | |K|
    -----------------
    | | | | | | | | |
    -----------------
    | | |p| | | | | |
    -----------------
    | | |X| | |R| | |
    -----------------
    | | | | | | | | |
    ----------------- ep: -1
""".trimIndent()
)
