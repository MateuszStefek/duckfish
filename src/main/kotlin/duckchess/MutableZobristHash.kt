package duckchess

import kotlin.random.Random

class MutableZobristHash(
    var hashA: Long/*,
    var hashB: Long*/
) {
    companion object {
        const val DIFFERENT_PIECES: Int = Piece.NUM_VALUES
        val t: LongArray = LongArray(DIFFERENT_PIECES * 64/* * 2*/) {
            Random.nextLong()
        }
    }

    fun update(coord: Coord, from: Piece, to: Piece) {
        val fromIndex = from.code/* * 2*/
        val toIndex = to.code/* * 2*/
        val baseIndex = /*2 **/ DIFFERENT_PIECES * coord.index
        hashA = hashA xor t[baseIndex + fromIndex] xor t[baseIndex + toIndex]
        //hashB = hashB xor t[baseIndex + fromIndex + 1] xor t[baseIndex + toIndex + 1]
    }

    fun updateEnPassant(from: Byte, to: Byte) {
        if (from != to) {
            hashA = hashA xor maskForEnPassant(from) xor maskForEnPassant(to)
        }
    }

    private fun maskForEnPassant(enPassantColumn: Byte): Long {
        return when {
            enPassantColumn < 0 -> 0
            else -> (enPassantColumn + 1).toLong() * 999
        }
    }

    fun updateCastlingBits(from: Int, to: Int) {
        if (from == to) return
        hashA = hashA xor maskForCastlingBits(from) xor maskForCastlingBits(to)
    }

    private fun maskForCastlingBits(bits: Int): Long = bits.toLong() * 2096


    fun updatePhase(from: Phase, to: Phase) {
        if (from == to) return
        hashA = hashA xor maskForPhase(from) xor maskForPhase(to)
    }

    private fun maskForPhase(phase: Phase) : Long = phase.ordinal.toLong() * 9

    fun clone(): MutableZobristHash {
        return MutableZobristHash(hashA/*, hashB*/)
    }
}
