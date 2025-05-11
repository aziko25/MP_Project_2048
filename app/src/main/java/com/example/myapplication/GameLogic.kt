package com.example.myapplication

import kotlin.math.abs
import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

fun generateInitialBoard(): List<List<Int>> {
    val empty = List(4) { MutableList(4) { 0 } }
    return spawnRandomTile(spawnRandomTile(empty))
}

fun getDirectionFromOffset(x: Float, y: Float): Direction? {
    return when {
        abs(x) > abs(y) && x > 0 -> Direction.RIGHT
        abs(x) > abs(y) && x < 0 -> Direction.LEFT
        abs(y) > abs(x) && y > 0 -> Direction.DOWN
        abs(y) > abs(x) && y < 0 -> Direction.UP
        else -> null
    }
}

fun isGameOver(board: List<List<Int>>): Boolean {
    if (board.any { row -> row.contains(0) }) return false
    for (i in 0..3) for (j in 0..3) {
        val v = board[i][j]
        if (i < 3 && board[i + 1][j] == v) return false
        if (j < 3 && board[i][j + 1] == v) return false
    }
    return true
}

fun moveLine(line: List<Int>): Pair<List<Int>, Int> {
    val filtered = line.filter { it != 0 }.toMutableList()
    var score = 0
    var i = 0
    while (i < filtered.size - 1) {
        if (filtered[i] == filtered[i + 1]) {
            filtered[i] *= 2
            score += filtered[i]
            filtered.removeAt(i + 1)
        } else i++
    }
    while (filtered.size < 4) filtered.add(0)
    return filtered to score
}

fun moveBoard(board: List<List<Int>>, direction: Direction): Pair<List<List<Int>>, Int> {
    var working = board
    when (direction) {
        Direction.UP -> working = transpose(working)
        Direction.DOWN -> working = reverseRows(transpose(working))
        Direction.RIGHT -> working = reverseRows(working)
        Direction.LEFT -> {}
    }

    var gained = 0
    val moved = working.map {
        val (line, score) = moveLine(it)
        gained += score
        line
    }

    var final = moved
    when (direction) {
        Direction.UP -> final = transpose(final)
        Direction.DOWN -> final = transpose(reverseRows(final))
        Direction.RIGHT -> final = reverseRows(final)
        Direction.LEFT -> {}
    }

    return if (final != board) spawnRandomTile(final) to gained else board to 0
}

fun transpose(board: List<List<Int>>) = board[0].indices.map { col -> board.map { it[col] } }
fun reverseRows(board: List<List<Int>>) = board.map { it.reversed() }

fun spawnRandomTile(board: List<List<Int>>): List<List<Int>> {
    val empty = board.flatMapIndexed { r, row ->
        row.mapIndexedNotNull { c, v -> if (v == 0) r to c else null }
    }
    if (empty.isEmpty()) return board
    val (r, c) = empty.random()
    val newValue = if (Random.nextFloat() < 0.9f) 2 else 4
    return board.mapIndexed { i, row ->
        row.mapIndexed { j, v -> if (i == r && j == c) newValue else v }
    }
}


fun getPossibleMoves(board: List<List<Int>>): List<Direction> {
    return Direction.entries.filter { direction ->
        moveBoard(board, direction).first != board
    }
}

fun getBestMove(board: List<List<Int>>): Direction? {
    val depth = getAdaptiveDepth(board)
    return getPossibleMoves(board)
        .map { move ->
            val (newBoard, _) = moveBoard(board, move)
            val eval = expectimax(newBoard, depth - 1, isPlayer = false)
            move to eval
        }
        .maxByOrNull { it.second }
        ?.first
}

fun getAdaptiveDepth(board: List<List<Int>>): Int {
    val empty = board.flatten().count { it == 0 }
    return when {
        empty >= 6 -> 4
        empty >= 3 -> 5
        else -> 6
    }
}

fun expectimax(board: List<List<Int>>, depth: Int, isPlayer: Boolean): Double {
    if (depth == 0 || isGameOver(board)) {
        return evaluateBoard(board, 0).toDouble()
    }

    return if (isPlayer) {
        getPossibleMoves(board)
            .map { moveBoard(board, it) }
            .maxOfOrNull { (newBoard, _) ->
                expectimax(newBoard, depth - 1, isPlayer = false)
            } ?: evaluateBoard(board, 0).toDouble()
    } else {
        val emptyTiles = mutableListOf<Pair<Int, Int>>()
        for (i in board.indices) {
            for (j in board[i].indices) {
                if (board[i][j] == 0) emptyTiles.add(i to j)
            }
        }

        if (emptyTiles.isEmpty()) return evaluateBoard(board, 0).toDouble()

        var total = 0.0
        for ((i, j) in emptyTiles) {
            val with2 = board.deepCopy().apply { this[i][j] = 2 }
            val with4 = board.deepCopy().apply { this[i][j] = 4 }

            total += 0.9 * expectimax(with2, depth - 1, isPlayer = true)
            total += 0.1 * expectimax(with4, depth - 1, isPlayer = true)
        }

        return total / emptyTiles.size
    }
}

fun evaluateBoard(board: List<List<Int>>, score: Int): Int {
    val emptyTiles = board.flatten().count { it == 0 }
    val maxTile = board.flatten().maxOrNull() ?: 0
    val smoothness = calculateSmoothness(board)
    val monotonicity = calculateMonotonicity(board)
    val cornerBonus = if (isMaxTileInCorner(board)) 10000 else 0

    return score +
            (emptyTiles * 270) +
            (smoothness * 0.5).toInt() +
            (monotonicity * 100) +
            (maxTile * 10) +
            cornerBonus
}

fun calculateSmoothness(board: List<List<Int>>): Int {
    var smoothness = 0
    for (i in board.indices) {
        for (j in board[i].indices) {
            val value = board[i][j]
            if (value == 0) continue
            if (j + 1 < board[i].size && board[i][j + 1] != 0) {
                smoothness -= abs(value - board[i][j + 1])
            }
            if (i + 1 < board.size && board[i + 1][j] != 0) {
                smoothness -= abs(value - board[i + 1][j])
            }
        }
    }
    return smoothness
}

fun calculateMonotonicity(board: List<List<Int>>): Int {
    var score = 0

    for (row in board) {
        for (i in 1 until row.size) {
            score += if (row[i - 1] >= row[i]) 1 else -1
        }
    }

    for (j in board[0].indices) {
        for (i in 1 until board.size) {
            score += if (board[i - 1][j] >= board[i][j]) 1 else -1
        }
    }

    return score
}

fun isMaxTileInCorner(board: List<List<Int>>): Boolean {
    val max = board.flatten().maxOrNull() ?: return false
    val corners = listOf(
        board[0][0], board[0][board.lastIndex],
        board[board.lastIndex][0], board[board.lastIndex][board.lastIndex]
    )
    return max in corners
}

fun List<List<Int>>.deepCopy(): List<MutableList<Int>> =
    map { it.toMutableList() }