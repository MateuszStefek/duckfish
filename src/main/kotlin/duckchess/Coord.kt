package duckchess

@JvmInline
value class Row(val row: Int) {
    operator fun plus(offset: Int) = Row(row + offset)
    operator fun compareTo(row: Int) = this.row.compareTo(row)
    fun text() = "${row + 1}"
}

@JvmInline
value class Column(val column: Int) {
    operator fun plus(offset: Int) = Column(column + offset)
    operator fun compareTo(column: Int) = this.column.compareTo(column)
    fun text(): String = "" + ('A' + column)
}

@JvmInline
value class Coord(val index: Int) {
    companion object {
        val A1 = Coord(0 * 8 + 0)
        val B1 = Coord(0 * 8 + 1)
        val C1 = Coord(0 * 8 + 2)
        val D1 = Coord(0 * 8 + 3)
        val E1 = Coord(0 * 8 + 4)
        val F1 = Coord(0 * 8 + 5)
        val G1 = Coord(0 * 8 + 6)
        val H1 = Coord(0 * 8 + 7)

        val A2 = Coord(1 * 8 + 0)
        val B2 = Coord(1 * 8 + 1)
        val C2 = Coord(1 * 8 + 2)
        val D2 = Coord(1 * 8 + 3)
        val E2 = Coord(1 * 8 + 4)
        val F2 = Coord(1 * 8 + 5)
        val G2 = Coord(1 * 8 + 6)
        val H2 = Coord(1 * 8 + 7)

        val A3 = Coord(2 * 8 + 0)
        val B3 = Coord(2 * 8 + 1)
        val C3 = Coord(2 * 8 + 2)
        val D3 = Coord(2 * 8 + 3)
        val E3 = Coord(2 * 8 + 4)
        val F3 = Coord(2 * 8 + 5)
        val G3 = Coord(2 * 8 + 6)
        val H3 = Coord(2 * 8 + 7)

        val A4 = Coord(3 * 8 + 0)
        val B4 = Coord(3 * 8 + 1)
        val C4 = Coord(3 * 8 + 2)
        val D4 = Coord(3 * 8 + 3)
        val E4 = Coord(3 * 8 + 4)
        val F4 = Coord(3 * 8 + 5)
        val G4 = Coord(3 * 8 + 6)
        val H4 = Coord(3 * 8 + 7)

        val A5 = Coord(4 * 8 + 0)
        val B5 = Coord(4 * 8 + 1)
        val C5 = Coord(4 * 8 + 2)
        val D5 = Coord(4 * 8 + 3)
        val E5 = Coord(4 * 8 + 4)
        val F5 = Coord(4 * 8 + 5)
        val G5 = Coord(4 * 8 + 6)
        val H5 = Coord(4 * 8 + 7)

        val A6 = Coord(5 * 8 + 0)
        val B6 = Coord(5 * 8 + 1)
        val C6 = Coord(5 * 8 + 2)
        val D6 = Coord(5 * 8 + 3)
        val E6 = Coord(5 * 8 + 4)
        val F6 = Coord(5 * 8 + 5)
        val G6 = Coord(5 * 8 + 6)
        val H6 = Coord(5 * 8 + 7)

        val A7 = Coord(6 * 8 + 0)
        val B7 = Coord(6 * 8 + 1)
        val C7 = Coord(6 * 8 + 2)
        val D7 = Coord(6 * 8 + 3)
        val E7 = Coord(6 * 8 + 4)
        val F7 = Coord(6 * 8 + 5)
        val G7 = Coord(6 * 8 + 6)
        val H7 = Coord(6 * 8 + 7)

        val A8 = Coord(7 * 8 + 0)
        val B8 = Coord(7 * 8 + 1)
        val C8 = Coord(7 * 8 + 2)
        val D8 = Coord(7 * 8 + 3)
        val E8 = Coord(7 * 8 + 4)
        val F8 = Coord(7 * 8 + 5)
        val G8 = Coord(7 * 8 + 6)
        val H8 = Coord(7 * 8 + 7)

        inline fun forEach(block: (Coord) -> Unit) {
            var i = 0
            do {
                block(Coord(i))
                i++
            } while (i < 64)
        }

        inline fun firstOrNull(predicate: (Coord) -> Boolean): Coord? {
            var i = 0
            do {
                val coord = Coord(i)
                if (predicate(coord)) {
                    return coord
                }
                i++
            } while (i < 64)
            return null
        }
    }

    val row: Row
        get() = Row(index / 8)
    val column: Column
        get() = Column(index % 8)

    fun text() = "${column.text()}${row.text()}"

    inline fun iNeighbours(block: (Coord) -> Unit) {
        val col = index % 8
        if (index < A8.index) {
            if (col > 0) {
                block(Coord(index + 7))
            }
            block(Coord(index + 8))
            if (col < 7) {
                block(Coord(index + 9))
            }
        }
        if (col > 0) {
            block(Coord(index - 1))
        }
        if (col < 7) {
            block(Coord(index + 1))
        }
        if (index > H1.index) {
            if (col > 0) {
                block(Coord(index - 9))
            }
            block(Coord(index - 8))
            if (col < 7) {
                block(Coord(index - 7))
            }
        }
    }

    inline fun iKnight(block: (Coord) -> Unit) {
        val col = index % 8
        val row = index / 8

        if (col > 0) {
            if (col > 1) {
                if (row < 7) block(Coord(index + 6))
                if (row > 0) block(Coord(index - 10))
            }
            if (row < 6) block(Coord(index + 15))
            if (row > 1) block(Coord(index - 17))
        }
        if (col < 7) {
            if (col < 6) {
                if (row < 7) block(Coord(index + 10))
                if (row > 0) block(Coord(index - 6))
            }
            if (row < 6) block(Coord(index + 17))
            if (row > 1) block(Coord(index - 15))
        }
    }

    inline fun diagonalUpRight(block: (Coord) -> Boolean) {
        var col = index % 8
        var row = index / 8
        while (row < 7 && col < 7) {
            col++
            row++
            if (!block(Coord(row * 8 + col))) break
        }
    }

    inline fun diagonalUpLeft(block: (Coord) -> Boolean) {
        var col = index % 8
        var row = index / 8
        while (row < 7 && col > 0) {
            col--
            row++
            if (!block(Coord(row * 8 + col))) break
        }
    }

    inline fun diagonalDownRight(block: (Coord) -> Boolean) {
        var col = index % 8
        var row = index / 8
        while (row > 0 && col < 7) {
            col++
            row--
            if (!block(Coord(row * 8 + col))) break
        }
    }

    inline fun diagonalDownLeft(block: (Coord) -> Boolean) {
        var col = index % 8
        var row = index / 8
        while (row > 0 && col > 0) {
            col--
            row--
            if (!block(Coord(row * 8 + col))) break
        }
    }

    inline fun right(block: (Coord) -> Boolean) {
        var col = index % 8
        val row = index / 8
        while (col < 7) {
            col++
            if (!block(Coord(row * 8 + col))) break
        }
    }

    inline fun left(block: (Coord) -> Boolean) {
        var col = index % 8
        val row = index / 8
        while (col > 0) {
            col--
            if (!block(Coord(row * 8 + col))) break
        }
    }

    inline fun up(block: (Coord) -> Boolean) {
        var col = index % 8
        var row = index / 8
        while (row < 7) {
            row++
            if (!block(Coord(row * 8 + col))) break
        }
    }

    inline fun down(block: (Coord) -> Boolean) {
        var col = index % 8
        var row = index / 8
        while (row > 0) {
            row--
            if (!block(Coord(row * 8 + col))) break
        }
    }

    fun oneUp(): Coord = Coord(index + 8)
    fun twoUp(): Coord = Coord(index + 16)
    fun oneDown(): Coord = Coord(index - 8)
    fun twoDown(): Coord = Coord(index - 16)

    fun oneLeftUp(): Coord? {
        var col = index % 8
        if (col > 0) {
            return Coord(index + 7)
        }
        return null
    }

    fun oneRightUp(): Coord? {
        var col = index % 8
        if (col < 7) {
            return Coord(index + 9)
        }
        return null
    }
    fun oneLeftDown(): Coord? {
        var col = index % 8
        if (col > 0) {
            return Coord(index - 9)
        }
        return null
    }

    fun oneRightDown(): Coord? {
        var col = index % 8
        if (col < 7) {
            return Coord(index - 7)
        }
        return null
    }

    inline fun iWhitePawnCaptures(block: (Coord) -> Unit) {
        val col = index % 8
        if (col < 7) {
            block(Coord(index + 9))
        }
        if (col > 0) {
            block(Coord(index + 7))
        }
    }

    inline fun iBlackPawnCaptures(block: (Coord) -> Unit) {
        val col = index % 8
        if (col < 7) {
            block(Coord(index - 7))
        }
        if (col > 0) {
            block(Coord(index - 9))
        }
    }

    fun isSecondChessRow(): Boolean = index >= A2.index && index <= H2.index
    fun isSeventhChessRow(): Boolean = index >= A7.index && index <= H7.index
}
