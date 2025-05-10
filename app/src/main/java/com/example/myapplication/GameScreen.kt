package com.example.myapplication

// CameraX + ML Kit
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

@Composable
fun Game2048Screen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Game state management
    var score by remember { mutableIntStateOf(0) }
    var bestScore by remember { mutableIntStateOf(0) }
    var board by remember { mutableStateOf(generateInitialBoard()) }
    var isGameOver by remember { mutableStateOf(false) }

    // Face detector instance
    val faceDetector = remember {
        FaceDetector(context, lifecycleOwner) { direction ->
            if (!isGameOver) {
                val gameDirection = when (direction) {
                    Direction.LEFT -> Direction.LEFT
                    Direction.RIGHT -> Direction.RIGHT
                    Direction.UP -> Direction.UP
                    Direction.DOWN -> Direction.DOWN
                }

                val (newBoard, gained) = moveBoard(board, gameDirection)
                if (newBoard != board) {
                    board = newBoard
                    score += gained
                    if (score > bestScore) bestScore = score
                    if (isGameOver(board)) isGameOver = true
                }
            }
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            faceDetector.stop()
        }
    }

    // Restart function
    fun restartGame() {
        if (score > bestScore) bestScore = score
        score = 0
        board = generateInitialBoard()
        isGameOver = false
    }

    // Gesture-based swiping
    val gestureModifier = Modifier.pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {
                val down = awaitFirstDown()
                var handled = false

                awaitTouchSlopOrCancellation(down.id) { change, over ->
                    if (!handled && !isGameOver) {
                        val (x, y) = over
                        val dir = getDirectionFromOffset(x, y)
                        dir?.let {
                            val (newBoard, gained) = moveBoard(board, it)
                            if (newBoard != board) {
                                board = newBoard
                                score += gained
                                if (score > bestScore) bestScore = score
                                if (isGameOver(board)) isGameOver = true
                            }
                            handled = true
                        }
                        change.consume()
                    }
                }
            }
        }
    }

    // Main layout
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFAF8EF))) {

        // PreviewView for Face Detection
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

        Column(
            modifier = Modifier.align(Alignment.TopCenter)
                .padding(top = 160.dp, start = 16.dp, end = 16.dp)
        ) {
            Header(score, bestScore, onRestart = { restartGame() })
        }

        Column(
            modifier = Modifier.align(Alignment.Center)
                .then(gestureModifier)
        ) {
            Board(board)
        }

        if (isGameOver) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Color(0xAA000000))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Game Over", fontSize = 36.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    ActionButton("RESTART", onClick = { restartGame() })
                }
            }
        }
    }
}