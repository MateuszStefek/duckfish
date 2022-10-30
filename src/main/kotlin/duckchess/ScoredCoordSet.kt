package duckchess

class ScoredCoordSet {
    class Entry(var coord: Coord, var score: Int, var selectedMove: Move?, var selectedDuckMove: Coord)

    val heap: Array<Entry> = Array(64) {
        Entry(Coord.A1, 0, null, Coord.A1)
    }
    var size: Int = 0;

    inline fun initialize(includePredicate: (Coord) -> Boolean, initialValue: Int) {
        var i = 0
        Coord.forEach { coord ->
            if (includePredicate(coord)) {
                val entry = heap[i++]
                entry.coord = coord
                entry.score = initialValue
                entry.selectedMove = null
            }
        }
        size = i
        // every element has the same score - heapification is unnecessary
        // initializeHeap()
    }

    private inline fun initializeHeap() {
        val size = this.size
        var i = size - 1
        while (i > 0) {
            bubbleDown(i)
            i--
        }
    }

    private fun bubbleUp(position: Int) {
        if (position == 0) return
        var i = position
        val entry = heap[i]
        do {
            val parentI = ((i + 1) / 2) - 1
            val parent = heap[parentI]
            if (parent.score > entry.score) {
                heap[i] = parent
            } else {
                break
            }
            i = parentI
        } while (i > 0)
        heap[i] = entry
    }

    private var exceptCoord: Coord = Coord.A1
    private var exceptBlockingMove: Move = DuckMove.of(Coord.A1)
    private var newSelectedMove: Move = DuckMove.of(Coord.A1)
    private var newDuckMove: Coord = Coord.A1

    /** Non-thread safe! */
    fun update(atLeast: Int, newSelectedMove: Move, newDuckMove: Coord, exceptCoord: Coord, exceptBlockingMove: Move) {
        this.newSelectedMove = newSelectedMove
        this.newDuckMove = newDuckMove
        this.exceptCoord = exceptCoord
        this.exceptBlockingMove = exceptBlockingMove
        update(atLeast)
    }

    private fun update(atLeast: Int) {
        updateRec(0, atLeast)
    }

    private fun updateRec(
        i: Int,
        atLeast: Int
    ) {
        if (i >= this.size) return
        val entry = heap[i]
        if (entry.score >= atLeast) return

        updateRec(i * 2 + 1, atLeast)
        updateRec(i * 2 + 2, atLeast)


        if (entry.coord != exceptCoord && !exceptBlockingMove.blockedByDuckAt(entry.coord)) {
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

        if (leftChildI >= this.size) return
        if (rightChildI == this.size) {
            nextI = leftChildI
            next = heap[nextI]
        } else {
            val leftChild = heap[leftChildI]
            val rightChild = heap[rightChildI]

            if (leftChild.score < rightChild.score) {
                nextI = leftChildI
                next = leftChild
            } else {
                nextI = rightChildI
                next = rightChild
            }
        }

        val current = heap[i]

        if (next.score < current.score) {
            heap[i] = next
            heap[nextI] = current
            bubbleDown(nextI)
        }
    }

    private val cachedMBE = MutableBoardEval()
    fun getBottomTwo(): MutableBoardEval {
        val size = this.size
        if (size == 0) {
            cachedMBE.scoreA = Integer.MAX_VALUE
            cachedMBE.duckPosA = Coord(999)
            cachedMBE.scoreB = Integer.MAX_VALUE
            cachedMBE.duckPosB = Coord(999)
            cachedMBE.moveA = null
            cachedMBE.duckMoveA = Coord.A1
            cachedMBE.moveB = null
        } else if (size == 1) {
            val entry = heap[0]
            cachedMBE.scoreA = entry.score
            cachedMBE.duckPosA = entry.coord
            cachedMBE.scoreB = entry.score
            cachedMBE.duckPosB = entry.coord
            cachedMBE.moveA = entry.selectedMove
            cachedMBE.duckMoveA = entry.selectedDuckMove
            cachedMBE.moveB = null
        }

        val best: Entry = heap[0]
        val secondBest: Entry = if (heap[1].score < heap[2].score) heap[1] else heap[2]
        cachedMBE.scoreA = best.score
        cachedMBE.duckPosA = best.coord
        cachedMBE.scoreB = secondBest.score
        cachedMBE.duckPosB = secondBest.coord
        cachedMBE.moveA = best.selectedMove
        cachedMBE.duckMoveA = best.selectedDuckMove
        cachedMBE.moveB = secondBest.selectedMove
        return cachedMBE
    }

    fun text(board: Board): String = with(StringBuilder()) {
        heap
            .toList()
            .take(size)
            .sortedBy { it.score }.forEachIndexed { index, it ->
                append(it.score)
                append("@")
                append(it.coord.text())
                append(" ")
                if (index == 0) {
                    val selectedMove = it.selectedMove
                    if (selectedMove != null) {
                        append(selectedMove.text(board))
                    }
                    val duM = it.selectedDuckMove
                    if (duM != null) {
                        append("X")
                        append(duM.text())
                    }
                }
            }
        toString()
    }
}

class ScoredCordSetPool {
    var pool: Array<ScoredCoordSet> = Array(100) { ScoredCoordSet() }
    var nextFree: Int = 0
    inline fun <R> withScoredCordSetFromPool(block: (ScoredCoordSet) -> R): R {
        val s = pool[nextFree++]
        try {
            return block(s)
        } finally {
            nextFree--
        }
    }
}
