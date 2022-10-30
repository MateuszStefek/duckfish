# Duckfish

An Engine that plays a chess variant called [Duck Chess](https://duckchess.com/).

Written in Kotlin as an exercise.

JVM is not a good platform for computation-heavy programs.
A lot of code ugliness is a result of performance hacking to avoid allocations.

# Running

Run [`Demo.kt`](./src/main/kotlin/duckchess/Demo.kt) with at least 1G of heap.


# Algorithm
If you try to apply a Minimax algorithm from a chess engine to the Duck Chess variant,
in a naive way,
you will quickly experience an explosion of tree size, caused by the large number of possible duck moves.


## Duck Chess Minimax

In this algorithm, we explore the tree of normal chess positions,
like in a classical chess Minimax,
but we modify the propagation method and the alpha-beta pruning method.

In the minimax, we visit classical chess positions (no Duck on the board)
and in each position we recursively consider all normal chess moves
(with the twist that checks and stalemates don't exist). 

The minimax recursive function
no longer returns a single number (maximum evaluation for white, minimum evaluation for black),
but rather a structure - `BoardEval`.

### BoardEval propagation method

TODO

### BoardEval alpha-beta pruning

TODO

### Best move selection

TODO
