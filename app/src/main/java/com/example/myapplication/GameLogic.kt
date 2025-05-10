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