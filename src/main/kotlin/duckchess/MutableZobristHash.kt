package duckchess

import kotlin.random.Random

class MutableZobristHash(
    var hashA: Long,
    var hashB: Long
) {
    companion object {
        const val DIFFERENT_PIECES: Int = Piece.NUM_VALUES
        val t: LongArray = LongArray(DIFFERENT_PIECES * 64 * 2) {
            Random.nextLong()
        }
    }

    fun update(coord: Coord, from: Piece, to: Piece) {
        val fromIndex = from.code * 2
        val toIndex = to.code * 2
        val baseIndex = 2 * DIFFERENT_PIECES * coord.index
        hashA = hashA xor t[baseIndex + fromIndex] xor t[baseIndex + toIndex]
        hashB = hashB xor t[baseIndex + fromIndex + 1] xor t[baseIndex + toIndex + 1]
    }

    fun clone(): MutableZobristHash {
        return MutableZobristHash(hashA, hashB)
    }
}
