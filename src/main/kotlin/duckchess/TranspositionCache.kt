package duckchess

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder

class TranspositionCache<SCORE>(maxSize: Int = 1_000_000) {
    val cache: Cache<CacheEntryKey, CacheEntry<SCORE>> = CacheBuilder.newBuilder()
        .initialCapacity(maxSize)
        .maximumSize(maxSize.toLong())
        .recordStats()
        .build()

    class CacheEntry<SCORE>(
        var remainingDepth: Int,
        var score: SCORE
    )

    data class CacheEntryKey(val hashA: Long, val hashB: Long)

    fun get(board: Board, remainingDepth: Int): CacheEntry<SCORE>? {
        val key = cacheEntryKey(board)
        return cache.getIfPresent(key)
            ?.takeIf { cacheEntry -> cacheEntry.remainingDepth >= remainingDepth }
    }

    fun set(board: Board, remainingDepth: Int, score: SCORE) {
        val key = cacheEntryKey(board)
        var cacheEntry = cache.getIfPresent(key)
        if (cacheEntry == null) {
            cacheEntry = CacheEntry(remainingDepth, score)
            cache.put(key, cacheEntry)
        } else {
            if (cacheEntry.remainingDepth < remainingDepth) {
                cacheEntry.remainingDepth = remainingDepth
                cacheEntry.score = score
            }
        }
    }

    private fun cacheEntryKey(board: Board): CacheEntryKey {
        var hashA = board.zobristHash.hashA
        var hashB = board.zobristHash.hashB
        // TODO include these inside the zobrist hash
        if (board.whiteShortCastlingAllowed) hashA = hashA xor 1
        if (board.whiteLongCastlingAllowed) hashA = hashA xor 2
        if (board.blackShortCastlingAllowed) hashA = hashA xor 4
        if (board.blackLongCastlingAllowed) hashA = hashA xor 8
        if (board.enPassantColumn > 0) hashA = hashA xor board.enPassantColumn.toLong() * 16
        hashB = hashB xor board.phase.ordinal.toLong() * 999
        return CacheEntryKey(hashA, hashB)
    }
}
