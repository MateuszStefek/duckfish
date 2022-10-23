package duckchess

class ScoredCoordSet {
    class Entry(val coord: Coord, var score: Int, var selectedMove: Move?, var selectedDuckMove: Coord?)

    private val t: Array<Entry>
    private var cachedSimplifiedBottomTwo: BoardEval? = null

    constructor(includePredicate: (Coord) -> Boolean, initialValue: Int) {
        val m: MutableList<Entry> = kotlin.collections.ArrayList(64)
        Coord.forEach { coord ->
            if (includePredicate(coord)) {
                m.add(Entry(coord, initialValue, null, null))
            }
        }
        t = m.toTypedArray()
        // every element has the same score - heapification is unnecessary
        // initializeHeap()
    }

    private inline fun initializeHeap() {
        val size = t.size
        var i = size - 1
        while (i > 0) {
            bubbleDown(i)
            i--
        }
    }

    private fun bubbleUp(position: Int) {
        if (position == 0) return
        var i = position
        val entry = t[i]
        do {
            val parentI = ((i + 1) / 2) - 1
            val parent = t[parentI]
            if (parent.score > entry.score) {
                t[i] = parent
            } else {
                break
            }
            i = parentI
        } while (i > 0)
        t[i] = entry
    }

    fun update(atLeast: Int, predicate: (Coord) -> Boolean, newSelectedMove: Move, newDuckMove: Coord) {
        updateRec(0, atLeast, predicate, newSelectedMove, newDuckMove)
    }

    private fun updateRec(
        i: Int,
        atLeast: Int,
        predicate: (Coord) -> Boolean,
        newSelectedMove: Move,
        newDuckMove: Coord
    ) {
        if (i >= t.size) return
        val entry = t[i]
        if (entry.score >= atLeast) return

        updateRec(i * 2 + 1, atLeast, predicate, newSelectedMove, newDuckMove)
        updateRec(i * 2 + 2, atLeast, predicate, newSelectedMove, newDuckMove)

        if (predicate(entry.coord)) {
            if (i < 2) cachedSimplifiedBottomTwo = null
            entry.selectedMove = newSelectedMove
            entry.selectedDuckMove = newDuckMove
            entry.score = atLeast

            bubbleDown(i)
        }
    }

    private tailrec fun bubbleDown(i: Int) {
        val nextI: Int
        val next: Entry

        val leftChildI = i * 2 + 1
        val rightChildI = leftChildI + 1

        if (leftChildI >= t.size) return
        if (rightChildI == t.size) {
            nextI = leftChildI
            next = t[nextI]
        } else {
            val leftChild = t[leftChildI]
            val rightChild = t[rightChildI]

            if (leftChild.score < rightChild.score) {
                nextI = leftChildI
                next = leftChild
            } else {
                nextI = rightChildI
                next = rightChild
            }
        }

        val current = t[i]

        if (next.score < current.score) {
            t[i] = next
            t[nextI] = current
            bubbleDown(nextI)
        }
    }

    fun simplifiedBottomTwo(): BoardEval {
        return cachedSimplifiedBottomTwo ?: bottomTwo()
    }

    fun bottomTwo(): BoardEval {
        val size = t.size
        if (size == 0) {
            return BoardEval(
                scoreA = Integer.MAX_VALUE,
                duckPosA = Coord(999),
                scoreB = Integer.MAX_VALUE,
                duckPosB = Coord(999)
            )
        } else if (size == 1) {
            val entry = t[0]
            return BoardEval(
                scoreA = entry.score,
                duckPosA = entry.coord,
                scoreB = entry.score,
                duckPosB = entry.coord,
                moveA = entry.selectedMove,
                duckMoveA = entry.selectedDuckMove
            )
        }

        val best: Entry = t[0]
        val secondBest: Entry = if (t[1].score < t[2].score) t[1] else t[2]
        val result = BoardEval(
            scoreA = best.score,
            duckPosA = best.coord,
            scoreB = secondBest.score,
            duckPosB = secondBest.coord,
            moveA = best.selectedMove,
            duckMoveA = best.selectedDuckMove
        )
        cachedSimplifiedBottomTwo = result
        return result
    }
}
