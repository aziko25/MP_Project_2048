package com.example.myapplication

// CameraX + ML Kit
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@Composable
fun Game2048Screen() {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var score by remember { mutableIntStateOf(0) }
    var bestScore by remember { mutableIntStateOf(0) }
    var board by remember { mutableStateOf(generateInitialBoard()) }
    var isGameOver by remember { mutableStateOf(false) }
    var showCongrats by remember { mutableStateOf(false) }
    var hasShownCongrats by remember { mutableStateOf(false) }

    val autoPlayHintMove = true
    val autoMovesQuantity = 10

    fun handleMove(direction: Direction) {
        val (newBoard, gained) = moveBoard(board, direction)
        if (newBoard != board) {
            board = newBoard
            score += gained
            if (score > bestScore) bestScore = score
            if (isGameOver(board)) isGameOver = true

            // Check max tile from current board
            val maxTile = board.flatten().maxOrNull() ?: 0
            if (maxTile >= 2048 && !hasShownCongrats) {
                showCongrats = true
                hasShownCongrats = true
            }
        }
    }

    fun restartGame() {
        if (score > bestScore) bestScore = score
        score = 0
        board = generateInitialBoard()
        isGameOver = false
        showCongrats = false
        hasShownCongrats = false
    }

    // Face direction to move
    val faceDetector = remember {
        FaceDetector(context, lifecycleOwner) { direction ->
            if (!isGameOver) handleMove(direction)
        }
    }

    DisposableEffect(Unit) {
        onDispose { faceDetector.stop() }
    }

    // Swipe gesture modifier
    val gestureModifier = Modifier.pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {
                val down = awaitFirstDown()
                awaitTouchSlopOrCancellation(down.id) { change, over ->
                    val direction = getDirectionFromOffset(over.x, over.y)
                    if (!isGameOver && direction != null) {
                        handleMove(direction)
                        change.consume()
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFAF8EF))) {

        // Face camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    faceDetector.start(this)
                }
            },
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .border(2.dp, Color.White.copy(alpha = 0.6f))
        )

        // Header + Hint Button
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 160.dp, start = 16.dp, end = 16.dp)
        ) {
            Header(
                score = score,
                bestScore = bestScore,
                onRestart = { restartGame() },
                onHint = {
                    repeat(autoMovesQuantity) {
                        val bestMove = getBestMove(board)
                        if (bestMove != null) {
                            if (autoPlayHintMove) {
                                handleMove(bestMove)
                            } else {
                                Toast.makeText(context, "Try moving ${bestMove.name}", Toast.LENGTH_SHORT).show()
                                return@Header
                            }
                        } else {
                            Toast.makeText(context, "No valid moves left", Toast.LENGTH_SHORT).show()
                            return@Header
                        }
                    }
                }
            )
        }

        // Board with gesture input
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .then(gestureModifier)
        ) {
            Board(board)
        }

        // Game over overlay
        if (isGameOver) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xAA000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Game Over", fontSize = 36.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    ActionButton("RESTART", onClick = { restartGame() })
                }
            }
        }

        if (showCongrats) {
            AlertDialog(
                onDismissRequest = { showCongrats = false },
                title = { Text("🎉 Congratulations!") },
                text = { Text("You reached 2048!") },
                confirmButton = {
                    TextButton(onClick = { showCongrats = false }) {
                        Text("Keep Playing")
                    }
                }
            )
        }
    }
}