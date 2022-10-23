package duckchess

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MoveKtTest {
    @Test
    fun testBlockedByDuckAt() {
        assertTrue(DiagonalCaptureMove.of(from = Coord.B5, to = Coord.E8).blockedByDuckAt(Coord.D7))
    }
}
