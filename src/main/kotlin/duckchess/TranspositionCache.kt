package duckchess

import kotlin.math.absoluteValue

class TranspositionCache<SCORE>(val maxSize: Int = 7_000_000) {
    val cache: Array<CacheEntry<SCORE>> = Array(maxSize) {
        CacheEntry(0, 0, 0, null, 0, 0)
    }

    data class CacheEntry<SCORE>(
        var hashA: Long,
        var hashB: Long,
        var remainingDepth: Int,
        var score: SCORE?,
        var alpha: Int,
        var beta: Int
    )

    fun get(board: Board): CacheEntry<SCORE>? {
        acacheEntry(board,
            matching = { entry -> return entry },
            nonMatching = { _, _, _ -> return null })
        return null
    }

    private inline fun acacheEntry(
        board: Board,
        matching: (CacheEntry<SCORE>) -> Unit,
        nonMatching: (CacheEntry<SCORE>, Long, Long) -> Unit
    ) {
        val hashA = board.zobristHash.hashA
        val hashB = board.zobristHash.hashB

        val bucket: Int = (hashA.toInt() % maxSize).absoluteValue
        if (bucket < 0 || bucket >= maxSize) throw IllegalStateException("$hashA")


        val entry = cache[bucket]
        if (entry.hashA == board.zobristHash.hashA &&
            entry.hashB == board.zobristHash.hashB &&
            entry.score != null
        ) {
            matching(entry)
        } else {
            nonMatching(entry, hashA, hashB)
        }
    }

    fun set(board: Board, remainingDepth: Int, score: SCORE, alpha: Int, beta: Int) {
        acacheEntry(board,
            matching = { cacheEntry ->
                val canOverride = when {
                    cacheEntry.remainingDepth < remainingDepth -> true
                    cacheEntry.remainingDepth == remainingDepth -> (alpha <= cacheEntry.alpha && cacheEntry.beta <= beta)
                    else -> false
                }
                if (canOverride) {
                    cacheEntry.remainingDepth = remainingDepth
                    cacheEntry.score = score
                    cacheEntry.alpha = alpha
                    cacheEntry.beta = beta
                }
            },
            nonMatching = { cacheEntry, hashA, hashB ->
                /*if (cacheEntry.score == null || cacheEntry.remainingDepth < remainingDepth) {*/
                    cacheEntry.hashA = hashA
                    cacheEntry.hashB = hashB
                    cacheEntry.remainingDepth = remainingDepth
                    cacheEntry.score = score
                    cacheEntry.alpha = alpha
                    cacheEntry.beta = beta
                /*}*/
            })
    }

}
