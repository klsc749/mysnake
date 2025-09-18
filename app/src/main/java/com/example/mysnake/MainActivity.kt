package com.example.mysnake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.hypot


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SnakeGameScreen(useDPad = true) }
    }
}

private enum class Dir { UP, DOWN, LEFT, RIGHT }

@Composable
fun SnakeGameScreen(useDPad: Boolean) {
    val cols = 20
    val rows = 28
    val cell = 16f
    val cellDp = 16.dp

    val wall = 1
    val playableX = (wall .. cols - 1 - wall)
    val playableY = (wall .. rows - 1 - wall)

    var dir by remember { mutableStateOf(Dir.RIGHT) }
    var snake by remember {
        mutableStateOf(listOf(Offset(5f, 10f), Offset(4f, 10f), Offset(3f, 10f)))
    }
    var food by remember { mutableStateOf(randomFoodIn(playableX, playableY, snake)) }
    var alive by remember { mutableStateOf(true) }
    var score by remember { mutableStateOf(0) }

    fun tryChangeDir(newDir: Dir) {
        val opposite = when (dir) {
            Dir.UP -> Dir.DOWN
            Dir.DOWN -> Dir.UP
            Dir.LEFT -> Dir.RIGHT
            Dir.RIGHT -> Dir.LEFT
        }
        if (newDir != opposite) dir = newDir
    }

    LaunchedEffect(alive, dir, snake) {
        while (alive) {
            delay(120)
            val head = snake.first()
            val next = when (dir) {
                Dir.UP -> head.copy(y = head.y - 1)
                Dir.DOWN -> head.copy(y = head.y + 1)
                Dir.LEFT -> head.copy(x = head.x - 1)
                Dir.RIGHT -> head.copy(x = head.x + 1)
            }
            val hitWall =
                next.x < playableX.first.toFloat() || next.x > playableX.last.toFloat() ||
                        next.y < playableY.first.toFloat() || next.y > playableY.last.toFloat()

            if (hitWall || next in snake) {
                alive = false
            } else {
                val newBody = listOf(next) + snake
                snake = if (next == food) {
                    score += 1
                    food = randomFoodIn(playableX, playableY, newBody)
                    newBody
                } else newBody.dropLast(1)
            }
        }
    }

    fun onKey(event: KeyEvent) {
        if (event.type != KeyEventType.KeyDown) return
        when (event.key) {
            Key.DirectionUp    -> tryChangeDir(Dir.UP)
            Key.DirectionDown  -> tryChangeDir(Dir.DOWN)
            Key.DirectionLeft  -> tryChangeDir(Dir.LEFT)
            Key.DirectionRight -> tryChangeDir(Dir.RIGHT)
            else -> {}
        }
    }

    val snakeColor = MaterialTheme.colorScheme.primary
    val foodColor  = MaterialTheme.colorScheme.tertiary
    val wallColor  = MaterialTheme.colorScheme.secondary

    MaterialTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onPreviewKeyEvent { onKey(it); true }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Score: $score",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .size(width = cellDp * cols, height = cellDp * rows)  // ✅ dp
                    .align(Alignment.CenterHorizontally)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                val cell = size.width / cols
                // up wall
                drawRect(
                    color = wallColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, cell)
                )
                // down wall
                drawRect(
                    color = wallColor,
                    topLeft = Offset(0f, size.height - cell),
                    size = Size(size.width, cell)
                )
                // left wall
                drawRect(
                    color = wallColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(cell, size.height)
                )
                // right wall
                drawRect(
                    color = wallColor,
                    topLeft = Offset(size.width - cell, 0f),
                    size = Size(cell, size.height)
                )

                snake.forEach {
                    drawRect(
                        color = snakeColor,
                        topLeft = Offset(it.x * cell, it.y * cell),
                        size = Size(cell, cell)
                    )
                }

                drawCircle(
                    color = foodColor,
                    radius = cell / 2f,
                    center = Offset(food.x * cell + cell / 2f, food.y * cell + cell / 2f)
                )

            }

            Spacer(Modifier.height(12.dp))

            if (useDPad) {
                DPad(
                    onUp    = { tryChangeDir(Dir.UP) },
                    onDown  = { tryChangeDir(Dir.DOWN) },
                    onLeft  = { tryChangeDir(Dir.LEFT) },
                    onRight = { tryChangeDir(Dir.RIGHT) },
                )
            } else {
                TouchJoystick(
                    onDirection = { tryChangeDir(it) },
                    diameter = 160.dp
                )
            }

            if (!alive) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Game Over. Hit Start to play again.",
                    color = MaterialTheme.colorScheme.error
                )
                LaunchedEffect(Unit) {
                    dir = Dir.RIGHT
                    snake = listOf(Offset(5f, 10f), Offset(4f, 10f), Offset(3f, 10f))
                    food = randomFoodIn(playableX, playableY, snake)
                    alive = true
                    score = 0
                }
            }
        }
    }
}

@Composable
private fun DPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    val btnSize = 64.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = onUp, modifier = Modifier.size(btnSize)) { Text("↑") }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onLeft, modifier = Modifier.size(btnSize)) { Text("←") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onDown, modifier = Modifier.size(btnSize)) { Text("↓") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onRight, modifier = Modifier.size(btnSize)) { Text("→") }
        }
    }
}

private fun randomFoodIn(
    xRange: IntRange,
    yRange: IntRange,
    snake: List<Offset>
): Offset {
    while (true) {
        val fx = Random.nextInt(xRange.first, xRange.last + 1).toFloat()
        val fy = Random.nextInt(yRange.first, yRange.last + 1).toFloat()
        val f = Offset(fx, fy)
        if (f !in snake) return f
    }
}

@Composable
private fun TouchJoystick(
    modifier: Modifier = Modifier,
    diameter: Dp = 160.dp,
    onDirection: (Dir) -> Unit
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { (diameter / 2).toPx() }

    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { knobOffset = Offset.Zero },
                    onDrag = { change, drag ->
                        knobOffset += drag
                        val len = hypot(knobOffset.x, knobOffset.y)
                        val maxR = radiusPx * 0.7f
                        if (len > maxR) {
                            val s = maxR / len
                            knobOffset = Offset(knobOffset.x * s, knobOffset.y * s)
                        }

                        val threshold = radiusPx * 0.25f
                        if (len > threshold) {
                            val dir = if (abs(knobOffset.x) > abs(knobOffset.y)) {
                                if (knobOffset.x > 0) Dir.RIGHT else Dir.LEFT
                            } else {
                                if (knobOffset.y > 0) Dir.DOWN else Dir.UP
                            }
                            onDirection(dir)
                        }
                        change.consume()
                    },
                    onDragEnd = { knobOffset = Offset.Zero },
                    onDragCancel = { knobOffset = Offset.Zero }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(4.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
        )

        Box(
            Modifier
                .offset {
                    IntOffset(
                        x = with(density) { knobOffset.x.toInt() },
                        y = with(density) { knobOffset.y.toInt() }
                    )
                }
                .size(diameter * 0.35f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
