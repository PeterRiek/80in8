package me.riek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import me.riek.ui.theme.Amber500
import me.riek.ui.theme.Black
import me.riek.ui.theme.Blue500
import me.riek.ui.theme.Cyan
import me.riek.ui.theme.DimCyan
import me.riek.ui.theme.EightyIn8Theme
import me.riek.ui.theme.Emerald
import me.riek.ui.theme.EmeraldBg
import me.riek.ui.theme.EmeraldText
import me.riek.ui.theme.Faded
import me.riek.ui.theme.Lime
import me.riek.ui.theme.RedBg
import me.riek.ui.theme.RedC
import me.riek.ui.theme.RedText
import me.riek.ui.theme.Right
import me.riek.ui.theme.Sky500
import me.riek.ui.theme.SkippedBg
import me.riek.ui.theme.Surface
import me.riek.ui.theme.White
import me.riek.ui.theme.Wrong
import me.riek.ui.theme.Zinc200
import me.riek.ui.theme.Zinc400
import me.riek.ui.theme.Zinc500
import me.riek.ui.theme.Zinc800
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = AppDatabase.get(this)
        setContent {
            EightyIn8Theme {
                App(db.gameDao())
            }
        }
    }
}

/** Result of one finished game, carried from Play -> Result screen. */
class PlayedGame(val questions: List<Question>, val answers: List<String>, val timeUsed: Int) {
    // right = +1, wrong = -1, blank = 0 (netScore, exactly like the site)
    val right: Int = questions.indices.count { checkAnswer(answers[it], questions[it].answer) }
    val wrong: Int = questions.indices.count { answers[it].isNotBlank() && !checkAnswer(answers[it], questions[it].answer) }
    val blank: Int = QUESTION_COUNT - right - wrong
    val score: Int = right - wrong
}

/** Score bands from 80in8.com (range, label, colour, advice). */
class Band(val minScore: Int, val range: String, val label: String, val color: Color, val advice: String)

private val BANDS = listOf(
    Band(75, "75–80", "Cracked", Cyan, "Maintain and stress-test. You're performing at a top level — train with harder variations to stay sharp."),
    Band(70, "70–74", "Excellent", Emerald, "Refine precision under pressure. Aim to eliminate the last few mistakes."),
    Band(60, "60–69", "Strong", Lime, "Push your speed. Accuracy is solid; work on quicker recall and mental shortcuts."),
    Band(50, "50–59", "Solid", Amber500, "Improve consistency. You're close — reduce careless errors and sharpen basic operations."),
    Band(Int.MIN_VALUE, "< 50", "Keep Practicing", RedC, "Focus on fundamentals. Slow down slightly and prioritise accuracy before rebuilding speed."),
)

fun bandFor(net: Int): Band = BANDS.first { net >= it.minScore }

sealed interface Screen {
    data object Home : Screen
    data object Play : Screen
    data class Result(val game: PlayedGame) : Screen
    data object History : Screen
}

@Composable
fun App(dao: GameDao) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    Scaffold { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (val s = screen) {
                Screen.Home -> HomeScreen(
                    dao = dao,
                    onPlay = { screen = Screen.Play },
                    onHistory = { screen = Screen.History },
                )
                Screen.Play -> PlayScreen(
                    onFinish = { played -> screen = Screen.Result(played) },
                    onCancel = { screen = Screen.Home },
                )
                is Screen.Result -> ResultScreen(
                    game = s.game,
                    dao = dao,
                    onPlayAgain = { screen = Screen.Play },
                    onHome = { screen = Screen.Home },
                )
                Screen.History -> HistoryScreen(
                    dao = dao,
                    onBack = { screen = Screen.Home },
                )
            }
        }
    }
}

/* ----------------------------- Home ----------------------------- */

@Composable
fun HomeScreen(dao: GameDao, onPlay: () -> Unit, onHistory: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ---- header (80in8.com style) ----
        Text("80in8", fontFamily = FontFamily.Monospace, fontSize = 56.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp, color = Cyan)
        Text(
            "PREP FOR CRACKING THE QUANT ASSESSMENT",
            color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
        )
        Text("80 math questions · 8 minutes", color = Zinc400, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))

        // ---- scoring line ----
        Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("+1 correct", color = Emerald, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            Text("−1 wrong", color = RedC, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            Text("0 skipped", color = Zinc400, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }

        // ---- scoring benchmarks ----
        Column(
            Modifier.fillMaxWidth().padding(top = 20.dp)
                .border(1.dp, Zinc800, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)),
        ) {
            Text(
                "SCORING BENCHMARKS", color = Zinc500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            BANDS.forEach { b ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(b.range, color = Zinc400, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Text(b.label, color = b.color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ---- start button (below benchmarks) ----
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth().widthIn(max = 320.dp).height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Black),
            shape = RoundedCornerShape(18.dp),
        ) { Text("Start Quiz", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
        Text("Press Enter to advance · no going back", color = Zinc500, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onHistory,
            modifier = Modifier.fillMaxWidth().widthIn(max = 320.dp).height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Surface, contentColor = Zinc400),
            shape = RoundedCornerShape(16.dp),
        ) { Text("PROGRESS", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 34.sp, fontWeight = FontWeight.Black, color = White)
            Text(label, color = Faded, fontSize = 13.sp)
        }
    }
}

@Composable
fun GameRow(game: Game) {
    val fmt = remember { SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(fmt.format(Date(game.playedAt)), color = Zinc400, fontSize = 13.sp, modifier = Modifier.weight(1f))
        // fixed-width columns so passed/failed/skipped line up across every row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Stat(Icons.Filled.CheckCircle, game.right, EmeraldText)
            Stat(Icons.Filled.Cancel, game.wrong, RedText)
            Stat(Icons.Filled.Remove, game.skipped, Zinc400)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(64.dp).padding(start = 16.dp),
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = bandFor(game.score).color, modifier = Modifier.size(15.dp))
            Text(
                "${game.score}", color = bandFor(game.score).color, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun Stat(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int, color: Color) {
    // icon at a fixed leading position (so icons line up across rows), number right beside it
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        Text(
            "$count", color = color, fontFamily = FontFamily.Monospace, fontSize = 13.sp,
            modifier = Modifier.width(18.dp),
        )
    }
}

/* ----------------------------- Play ----------------------------- */

@Composable
fun PlayScreen(onFinish: (PlayedGame) -> Unit, onCancel: () -> Unit) {
    val questions = remember { generateQuestions() }
    val answers = remember { Array(QUESTION_COUNT) { "" } }
    var index by remember { mutableStateOf(0) }
    var input by remember { mutableStateOf("") }
    var secondsLeft by remember { mutableStateOf(GAME_SECONDS) }
    var done by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    // Android back opens the confirmation (unless it's already open → let it dismiss)
    BackHandler(enabled = !showConfirm) { showConfirm = true }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = Surface,
            title = { Text("Cancel this run?", color = White) },
            text = { Text("This game won't be saved to your history.", color = Zinc400) },
            confirmButton = { TextButton(onClick = onCancel) { Text("Quit", color = RedC, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Keep playing", color = Zinc400) } },
        )
    }

    fun finish() {
        if (!done) {
            done = true
            onFinish(PlayedGame(questions, answers.toList(), GAME_SECONDS - secondsLeft))
        }
    }

    // countdown
    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !done) {
            delay(1000)
            secondsLeft--
        }
        if (secondsLeft <= 0) finish()
    }

    LaunchedEffect(index) { focus.requestFocus() }

    fun submit() {
        answers[index] = input.trim()
        input = ""
        if (index + 1 >= QUESTION_COUNT) finish() else index++
    }

    Column(Modifier.fillMaxSize().padding(vertical = 24.dp)) {
        // timer
        val mm = secondsLeft / 60
        val ss = secondsLeft % 60
        Text(
            "%d:%02d".format(mm, ss),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (secondsLeft <= 30) Wrong else Cyan,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { secondsLeft.toFloat() / GAME_SECONDS },
            modifier = Modifier.fillMaxWidth().height(4.dp),  // edge-to-edge
            color = if (secondsLeft <= 30) Wrong.copy(alpha = 0.6f) else DimCyan,
            trackColor = Surface,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )

      Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)) {
        Spacer(Modifier.weight(1f))

        Text(
            "${index + 1}/$QUESTION_COUNT Questions",
            color = Faded,
            fontSize = 15.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            questions[index].text,
            fontSize = 52.sp,
            fontWeight = FontWeight.Black,
            color = White,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { input = it.filter { c -> c.isDigit() || c == '.' || c == '-' || c == '/' } },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center, color = White),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Cyan,
                unfocusedBorderColor = Faded,
                cursorColor = Cyan,
            ),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "press enter to submit",
            color = Zinc500,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))
      }

        // cancel button, bottom centre
        Box(
            Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp).size(48.dp)
                .clip(CircleShape).border(1.dp, Zinc800, CircleShape)
                .clickable { showConfirm = true },
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = Zinc400, fontSize = 20.sp)
        }
    }
}

/* ----------------------------- Result ----------------------------- */

@Composable
fun ResultScreen(game: PlayedGame, dao: GameDao, onPlayAgain: () -> Unit, onHome: () -> Unit) {
    // save exactly once
    LaunchedEffect(game) {
        dao.insert(Game(
            playedAt = System.currentTimeMillis(),
            score = game.score,
            right = game.right,
            wrong = game.wrong,
            skipped = game.blank,
        ))
    }

    val band = bandFor(game.score)
    val mm = game.timeUsed / 60
    val ss = game.timeUsed % 60
    val timeText = when {
        game.timeUsed >= GAME_SECONDS -> "Time expired"
        game.blank == 0 -> "Finished in ${mm}m ${ss}s"
        else -> "${mm}m ${ss}s elapsed"
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ---- header ----
        Text(band.label, color = band.color, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(
            "${game.score}",
            fontFamily = FontFamily.Monospace, fontSize = 64.sp, fontWeight = FontWeight.Bold, color = White,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("+${game.right}", color = Emerald, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Text(" · ", color = Zinc500, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Text("−${game.wrong}", color = RedC, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            if (game.blank > 0) {
                Text(" · ", color = Zinc500, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Text("${game.blank} skipped", color = Zinc400, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            }
        }
        Text(timeText, color = Zinc500, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        Text(
            band.advice, color = Zinc400, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 340.dp).padding(top = 10.dp),
        )

        // ---- buttons ----
        Row(
            Modifier.padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onHome,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Zinc800),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Zinc400),
            ) { Text("Home", fontWeight = FontWeight.SemiBold) }
            Button(
                onClick = onPlayAgain,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Black),
            ) { Text("Try Again", fontWeight = FontWeight.SemiBold) }
        }

        Spacer(Modifier.height(16.dp))

        // ---- answers container ----
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .border(1.dp, Zinc800, RoundedCornerShape(16.dp))
                .padding(12.dp),
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(game.questions) { i, q ->
                    AnswerRow(i + 1, q.text, game.answers[i], q.answer)
                }
            }
        }
    }
}

@Composable
private fun AnswerRow(number: Int, question: String, given: String, answer: String) {
    val skipped = given.isBlank()
    val correct = checkAnswer(given, answer)
    val bg = when { correct -> EmeraldBg; skipped -> SkippedBg; else -> RedBg }
    val ansColor = when { correct -> EmeraldText; skipped -> Zinc400; else -> RedText }
    val icon = when { correct -> Icons.Filled.CheckCircle; skipped -> Icons.Filled.Remove; else -> Icons.Filled.Cancel }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$number", color = Zinc500, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
            textAlign = TextAlign.End, modifier = Modifier.width(22.dp),
        )
        Icon(icon, contentDescription = null, tint = ansColor, modifier = Modifier.padding(start = 10.dp).size(18.dp))
        Text(
            question, color = Zinc200, fontFamily = FontFamily.Monospace, fontSize = 15.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
        )
        if (!correct && !skipped) {
            Text(
                given, color = Zinc500, fontFamily = FontFamily.Monospace, fontSize = 14.sp,
                textDecoration = TextDecoration.LineThrough, modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            answer, color = ansColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp),
        )
    }
}

/* ----------------------------- History ----------------------------- */

@Composable
fun HistoryScreen(dao: GameDao, onBack: () -> Unit) {
    val games by dao.all().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Surface,
            title = { Text("Reset progress?", color = White) },
            text = { Text("This permanently deletes all ${games.size} saved games.", color = Zinc400) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    scope.launch { dao.deleteAll() }
                }) { Text("Reset", color = RedC, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel", color = Zinc400) } },
        )
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Progress", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Cyan)
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = Zinc400)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Reset progress", color = RedC) },
                        onClick = { menuOpen = false; confirmReset = true },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (games.size < 2) {
            Text("Play at least 2 games to see your progress.", color = Faded)
        } else {
            ScoreChart(
                scores = games.map { it.score },
                modifier = Modifier.fillMaxWidth().height(240.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("All games", color = White, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(games.reversed()) { GameRow(it) }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Surface, contentColor = Zinc400),
            shape = RoundedCornerShape(16.dp),
        ) { Text("BACK", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun ScoreChart(scores: List<Int>, modifier: Modifier = Modifier) {
    val hi = (scores.max()).coerceAtLeast(1)
    val lo = (scores.min()).coerceAtMost(0)      // always include zero
    val range = (hi - lo).coerceAtLeast(1)
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(16.dp)) {
        Canvas(Modifier.fillMaxSize().padding(16.dp)) {
            val w = size.width
            val h = size.height
            val n = scores.size
            if (n < 2) return@Canvas
            val stepX = w / (n - 1)
            fun y(v: Int) = h - ((v - lo).toFloat() / range) * h

            // zero baseline
            val zeroY = y(0)
            drawLine(Faded, Offset(0f, zeroY), Offset(w, zeroY), strokeWidth = 2f)

            val points = scores.mapIndexed { i, v -> Offset(i * stepX, y(v)) }
            val colors = scores.map { bandFor(it).color }
            // each segment fades smoothly between its endpoints' band colours
            for (i in 0 until points.size - 1) {
                drawLine(
                    brush = Brush.linearGradient(
                        listOf(colors[i], colors[i + 1]),
                        start = points[i], end = points[i + 1],
                    ),
                    start = points[i], end = points[i + 1], strokeWidth = 5f,
                )
            }
            points.forEachIndexed { i, p ->
                drawCircle(colors[i], radius = 7f, center = p)
                drawCircle(White, radius = 3f, center = p)
            }
        }
    }
}
