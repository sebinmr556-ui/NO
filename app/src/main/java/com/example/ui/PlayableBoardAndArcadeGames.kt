package com.example.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GamePlayerSlot
import com.example.data.GameRoomDoc
import com.example.data.UserProfile
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Procedural Game Sound Effects & Music Manager that works across all 11 games.
 * Safely wraps Android ToneGenerator so unit/Robolectric tests never fail.
 */
object GameSoundEffectsEngine {
    var sfxEnabled: Boolean = true
    var bgmEnabled: Boolean = true
    var musicEnabled: Boolean
        get() = bgmEnabled
        set(value) { bgmEnabled = value }

    private fun playToneSafe(toneType: Int, durationMs: Int) {
        if (!sfxEnabled) return
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
            tg.startTone(toneType, durationMs)
        } catch (_: Throwable) {
            // Ignored in JVM unit tests
        }
    }

    fun playTap() = playToneSafe(ToneGenerator.TONE_PROP_BEEP, 45)
    fun playDiceRoll() = playToneSafe(ToneGenerator.TONE_PROP_ACK, 90)
    fun playMove() = playToneSafe(ToneGenerator.TONE_DTMF_5, 55)
    fun playPieceMove() = playMove()
    fun playCueStrike() = playToneSafe(ToneGenerator.TONE_DTMF_7, 75)
    fun playCapture() = playToneSafe(ToneGenerator.TONE_DTMF_9, 110)
    fun playPocket() = playToneSafe(ToneGenerator.TONE_PROP_BEEP2, 120)
    fun playCheckAlert() = playToneSafe(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 160)
    fun playVictory() = playToneSafe(ToneGenerator.TONE_CDMA_MED_L, 260)
}

// ============================================================================
// 6. COMPLETE PLAYABLE CHESS (8x8 Board, 32 Pieces, Check, Checkmate, Castling,
//    En Passant, Pawn Promotion, Draw Detection, Move History, 1v1 Multiplayer)
// ============================================================================

enum class ChessColor { WHITE, BLACK }
enum class ChessPieceType(val symbolWhite: String, val symbolBlack: String, val value: Int) {
    KING("♔", "♚", 1000),
    QUEEN("♕", "♛", 9),
    ROOK("♖", "♜", 5),
    BISHOP("♗", "♝", 3),
    KNIGHT("♘", "♞", 3),
    PAWN("♙", "♟", 1)
}

data class ChessPiece(
    val type: ChessPieceType,
    val color: ChessColor,
    val hasMoved: Boolean = false
) {
    val glyph: String get() = if (color == ChessColor.WHITE) type.symbolWhite else type.symbolBlack
}

data class ChessMove(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val isEnPassant: Boolean = false,
    val isCastleKingside: Boolean = false,
    val isCastleQueenside: Boolean = false
)

@Composable
fun PlayableChessArena(
    gameRoom: GameRoomDoc,
    players: List<GamePlayerSlot>,
    currentUserProfile: UserProfile?,
    onSyncState: (String, String, List<GamePlayerSlot>?, String?, String?, String?) -> Unit,
    onFinishMatch: (UserProfile, String, Long, Long) -> Unit
) {
    val myUid = currentUserProfile?.userId?.ifBlank { currentUserProfile.id } ?: ""
    val whitePlayer = players.getOrNull(0)
    val blackPlayer = players.getOrNull(1)

    var board by remember { mutableStateOf(createInitialChessBoard()) }
    var turnColor by remember { mutableStateOf(ChessColor.WHITE) }
    var selectedSquare by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var legalMovesForSelected by remember { mutableStateOf<List<ChessMove>>(emptyList()) }
    var enPassantTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var pendingPromotionMove by remember { mutableStateOf<ChessMove?>(null) }
    var moveHistory by remember { mutableStateOf<List<String>>(emptyList()) }
    var whiteCapturedScore by remember { mutableIntStateOf(0) }
    var blackCapturedScore by remember { mutableIntStateOf(0) }
    var statusBanner by remember { mutableStateOf("White's Turn • Select any White piece") }
    var gameEnded by remember { mutableStateOf(false) }
    var turnTimerSec by remember { mutableIntStateOf(45) }

    LaunchedEffect(turnColor, gameEnded) {
        turnTimerSec = 45
        while (!gameEnded && turnTimerSec > 0) {
            delay(1000L)
            turnTimerSec--
        }
    }

    fun executeChessMove(move: ChessMove, promotionPiece: ChessPieceType = ChessPieceType.QUEEN) {
        val moving = board[move.fromRow][move.fromCol] ?: return
        val target = board[move.toRow][move.toCol]
        val nextBoard = Array(8) { r -> Array(8) { c -> board[r][c] } }

        var capturedValue = target?.type?.value ?: 0
        nextBoard[move.fromRow][move.fromCol] = null

        if (move.isEnPassant) {
            val capturedPawnRow = move.fromRow
            val capturedPawn = nextBoard[capturedPawnRow][move.toCol]
            capturedValue += capturedPawn?.type?.value ?: 1
            nextBoard[capturedPawnRow][move.toCol] = null
        }

        if (move.isCastleKingside) {
            val rook = nextBoard[move.fromRow][7]
            nextBoard[move.fromRow][7] = null
            nextBoard[move.fromRow][5] = rook?.copy(hasMoved = true)
        } else if (move.isCastleQueenside) {
            val rook = nextBoard[move.fromRow][0]
            nextBoard[move.fromRow][0] = null
            nextBoard[move.fromRow][3] = rook?.copy(hasMoved = true)
        }

        val promoted = if (moving.type == ChessPieceType.PAWN && (move.toRow == 0 || move.toRow == 7)) {
            ChessPiece(promotionPiece, moving.color, hasMoved = true)
        } else {
            moving.copy(hasMoved = true)
        }
        nextBoard[move.toRow][move.toCol] = promoted

        if (capturedValue > 0) {
            if (moving.color == ChessColor.WHITE) whiteCapturedScore += capturedValue * 10
            else blackCapturedScore += capturedValue * 10
            GameSoundEffectsEngine.playCapture()
        } else {
            GameSoundEffectsEngine.playMove()
        }

        // Update En Passant square
        enPassantTarget = if (moving.type == ChessPieceType.PAWN && abs(move.toRow - move.fromRow) == 2) {
            Pair((move.fromRow + move.toRow) / 2, move.fromCol)
        } else {
            null
        }

        val files = "abcdefgh"
        val moveNotation = "${moving.glyph}${files[move.fromCol]}${8 - move.fromRow}→${files[move.toCol]}${8 - move.toRow}"
        moveHistory = (moveHistory + moveNotation).takeLast(24)

        val nextTurn = if (turnColor == ChessColor.WHITE) ChessColor.BLACK else ChessColor.WHITE
        board = nextBoard
        selectedSquare = null
        legalMovesForSelected = emptyList()
        turnColor = nextTurn

        val inCheck = isChessKingInCheck(nextBoard, nextTurn)
        val allResponses = generateAllLegalChessMoves(nextBoard, nextTurn, enPassantTarget)

        val updatedSlots = players.mapIndexed { idx, slot ->
            when (idx) {
                0 -> slot.copy(score = whiteCapturedScore.toLong())
                1 -> slot.copy(score = blackCapturedScore.toLong())
                else -> slot
            }
        }
        val nextTurnUid = if (nextTurn == ChessColor.WHITE) {
            whitePlayer?.userId ?: myUid
        } else {
            blackPlayer?.userId ?: myUid
        }

        if (allResponses.isEmpty()) {
            gameEnded = true
            if (inCheck) {
                GameSoundEffectsEngine.playVictory()
                val winnerColorName = if (moving.color == ChessColor.WHITE) "White" else "Black"
                statusBanner = "CHECKMATE! $winnerColorName Wins!"
                val winnerProfile = currentUserProfile ?: UserProfile(userId = myUid, username = whitePlayer?.username ?: "Champion")
                val runnerUp = if (moving.color == ChessColor.WHITE) (blackPlayer?.username ?: "Challenger") else (whitePlayer?.username ?: "Challenger")
                onFinishMatch(winnerProfile, runnerUp, (whiteCapturedScore + 200).toLong(), blackCapturedScore.toLong())
            } else {
                statusBanner = "STALEMATE DRAW! No legal moves remain."
            }
        } else if (inCheck) {
            GameSoundEffectsEngine.playCheckAlert()
            statusBanner = "CHECK! ${if (nextTurn == ChessColor.WHITE) "White" else "Black"} King is under attack!"
        } else {
            statusBanner = "${if (nextTurn == ChessColor.WHITE) "White" else "Black"}'s Turn • $moveNotation"
        }

        onSyncState(
            "{\"turn\":\"$nextTurn\",\"lastMove\":\"$moveNotation\",\"wScore\":$whiteCapturedScore,\"bScore\":$blackCapturedScore}",
            nextTurnUid,
            updatedSlots,
            null,
            null,
            null
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("playable_chess_arena"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Turn & Score Bar
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF141026),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "♔ ${whitePlayer?.username ?: "White"} (${whiteCapturedScore} pts)",
                        color = if (turnColor == ChessColor.WHITE) Color(0xFFFFD700) else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "♚ ${blackPlayer?.username ?: "Black"} (${blackCapturedScore} pts)",
                        color = if (turnColor == ChessColor.BLACK) Color(0xFF00E5FF) else Color(0xFFB8B2D1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "⏱ ${turnTimerSec}s",
                    color = if (turnTimerSec <= 10) Color(0xFFFF5252) else Color(0xFF00E676),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = statusBanner,
            color = Color(0xFFFFD54F),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        // 8x8 Interactive Royal 3D Chessboard
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF23193E),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(2.dp, Color(0xFFFFD54F).copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .padding(6.dp)
                .testTag("chess_board_grid")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until 8) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        for (c in 0 until 8) {
                            val isLight = (r + c) % 2 == 0
                            val piece = board[r][c]
                            val isSelected = selectedSquare?.first == r && selectedSquare?.second == c
                            val matchingMove = legalMovesForSelected.firstOrNull { it.toRow == r && it.toCol == c }

                            val squareBg = when {
                                isSelected -> Color(0xFF7C4DFF)
                                matchingMove != null -> Color(0xFF00E676).copy(alpha = 0.52f)
                                isLight -> Color(0xFFE8DFF5)
                                else -> Color(0xFF4A3B78)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(squareBg)
                                    .clickable(enabled = !gameEnded) {
                                        if (matchingMove != null) {
                                            val movingPiece = board[matchingMove.fromRow][matchingMove.fromCol]
                                            if (movingPiece?.type == ChessPieceType.PAWN && (matchingMove.toRow == 0 || matchingMove.toRow == 7)) {
                                                pendingPromotionMove = matchingMove
                                            } else {
                                                executeChessMove(matchingMove)
                                            }
                                        } else if (piece != null && piece.color == turnColor) {
                                            GameSoundEffectsEngine.playTap()
                                            selectedSquare = Pair(r, c)
                                            legalMovesForSelected = generateLegalMovesForSquare(board, r, c, enPassantTarget)
                                        } else {
                                            selectedSquare = null
                                            legalMovesForSelected = emptyList()
                                        }
                                    }
                                    .testTag("chess_sq_${r}_${c}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (piece != null) {
                                    Text(
                                        text = piece.glyph,
                                        fontSize = 24.sp,
                                        color = if (piece.color == ChessColor.WHITE) Color(0xFFFFF8E1) else Color(0xFF120C24),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                } else if (matchingMove != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E676))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pawn Promotion Selector
        pendingPromotionMove?.let { promoMove ->
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E1638),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Promote Pawn:", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    listOf(
                        ChessPieceType.QUEEN to "Queen ♕",
                        ChessPieceType.ROOK to "Rook ♖",
                        ChessPieceType.BISHOP to "Bishop ♗",
                        ChessPieceType.KNIGHT to "Knight ♘"
                    ).forEach { (type, label) ->
                        Button(
                            onClick = {
                                pendingPromotionMove = null
                                executeChessMove(promoMove, type)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Move History & Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (moveHistory.isEmpty()) "Moves: None yet" else "History: ${moveHistory.takeLast(5).joinToString(" • ")}",
                color = Color(0xFFB8B2D1),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    board = createInitialChessBoard()
                    turnColor = ChessColor.WHITE
                    selectedSquare = null
                    legalMovesForSelected = emptyList()
                    enPassantTarget = null
                    moveHistory = emptyList()
                    whiteCapturedScore = 0
                    blackCapturedScore = 0
                    gameEnded = false
                    statusBanner = "New Chess Match Started • White's Turn"
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B224B)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("New Board", fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

private fun createInitialChessBoard(): Array<Array<ChessPiece?>> {
    val grid = Array(8) { arrayOfNulls<ChessPiece>(8) }
    val backRank = listOf(
        ChessPieceType.ROOK,
        ChessPieceType.KNIGHT,
        ChessPieceType.BISHOP,
        ChessPieceType.QUEEN,
        ChessPieceType.KING,
        ChessPieceType.BISHOP,
        ChessPieceType.KNIGHT,
        ChessPieceType.ROOK
    )
    for (c in 0 until 8) {
        grid[0][c] = ChessPiece(backRank[c], ChessColor.BLACK)
        grid[1][c] = ChessPiece(ChessPieceType.PAWN, ChessColor.BLACK)
        grid[6][c] = ChessPiece(ChessPieceType.PAWN, ChessColor.WHITE)
        grid[7][c] = ChessPiece(backRank[c], ChessColor.WHITE)
    }
    return grid
}

private fun generateAllLegalChessMoves(
    board: Array<Array<ChessPiece?>>,
    color: ChessColor,
    enPassantTarget: Pair<Int, Int>?
): List<ChessMove> {
    val result = mutableListOf<ChessMove>()
    for (r in 0 until 8) {
        for (c in 0 until 8) {
            if (board[r][c]?.color == color) {
                result.addAll(generateLegalMovesForSquare(board, r, c, enPassantTarget))
            }
        }
    }
    return result
}

private fun generateLegalMovesForSquare(
    board: Array<Array<ChessPiece?>>,
    row: Int,
    col: Int,
    enPassantTarget: Pair<Int, Int>?
): List<ChessMove> {
    val piece = board[row][col] ?: return emptyList()
    val pseudo = generatePseudoChessMoves(board, row, col, piece, enPassantTarget)
    return pseudo.filter { move ->
        val copy = Array(8) { r -> Array(8) { c -> board[r][c] } }
        copy[move.toRow][move.toCol] = copy[move.fromRow][move.fromCol]
        copy[move.fromRow][move.fromCol] = null
        if (move.isEnPassant) {
            copy[move.fromRow][move.toCol] = null
        }
        !isChessKingInCheck(copy, piece.color)
    }
}

private fun generatePseudoChessMoves(
    board: Array<Array<ChessPiece?>>,
    r: Int,
    c: Int,
    piece: ChessPiece,
    enPassantTarget: Pair<Int, Int>?
): List<ChessMove> {
    val moves = mutableListOf<ChessMove>()
    fun inBounds(nr: Int, nc: Int) = nr in 0..7 && nc in 0..7

    when (piece.type) {
        ChessPieceType.PAWN -> {
            val dir = if (piece.color == ChessColor.WHITE) -1 else 1
            val startRow = if (piece.color == ChessColor.WHITE) 6 else 1
            if (inBounds(r + dir, c) && board[r + dir][c] == null) {
                moves.add(ChessMove(r, c, r + dir, c))
                if (r == startRow && inBounds(r + dir * 2, c) && board[r + dir * 2][c] == null) {
                    moves.add(ChessMove(r, c, r + dir * 2, c))
                }
            }
            for (dc in listOf(-1, 1)) {
                val nr = r + dir
                val nc = c + dc
                if (inBounds(nr, nc)) {
                    val occupant = board[nr][nc]
                    if (occupant != null && occupant.color != piece.color) {
                        moves.add(ChessMove(r, c, nr, nc))
                    } else if (enPassantTarget?.first == nr && enPassantTarget.second == nc) {
                        moves.add(ChessMove(r, c, nr, nc, isEnPassant = true))
                    }
                }
            }
        }
        ChessPieceType.KNIGHT -> {
            val jumps = listOf(
                -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                1 to -2, 1 to 2, 2 to -1, 2 to 1
            )
            for ((dr, dc) in jumps) {
                val nr = r + dr
                val nc = c + dc
                if (inBounds(nr, nc) && board[nr][nc]?.color != piece.color) {
                    moves.add(ChessMove(r, c, nr, nc))
                }
            }
        }
        ChessPieceType.BISHOP, ChessPieceType.ROOK, ChessPieceType.QUEEN -> {
            val dirs = mutableListOf<Pair<Int, Int>>()
            if (piece.type != ChessPieceType.BISHOP) {
                dirs.addAll(listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1))
            }
            if (piece.type != ChessPieceType.ROOK) {
                dirs.addAll(listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1))
            }
            for ((dr, dc) in dirs) {
                var nr = r + dr
                var nc = c + dc
                while (inBounds(nr, nc)) {
                    val occ = board[nr][nc]
                    if (occ == null) {
                        moves.add(ChessMove(r, c, nr, nc))
                    } else {
                        if (occ.color != piece.color) moves.add(ChessMove(r, c, nr, nc))
                        break
                    }
                    nr += dr
                    nc += dc
                }
            }
        }
        ChessPieceType.KING -> {
            for (dr in -1..1) {
                for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val nr = r + dr
                    val nc = c + dc
                    if (inBounds(nr, nc) && board[nr][nc]?.color != piece.color) {
                        moves.add(ChessMove(r, c, nr, nc))
                    }
                }
            }
            // Castling
            if (!piece.hasMoved && !isChessKingInCheck(board, piece.color)) {
                val kingSideRook = board[r][7]
                if (kingSideRook?.type == ChessPieceType.ROOK && !kingSideRook.hasMoved &&
                    board[r][5] == null && board[r][6] == null
                ) {
                    moves.add(ChessMove(r, c, r, 6, isCastleKingside = true))
                }
                val queenSideRook = board[r][0]
                if (queenSideRook?.type == ChessPieceType.ROOK && !queenSideRook.hasMoved &&
                    board[r][1] == null && board[r][2] == null && board[r][3] == null
                ) {
                    moves.add(ChessMove(r, c, r, 2, isCastleQueenside = true))
                }
            }
        }
    }
    return moves
}

private fun isChessKingInCheck(board: Array<Array<ChessPiece?>>, kingColor: ChessColor): Boolean {
    var kingR = -1
    var kingC = -1
    for (r in 0 until 8) {
        for (c in 0 until 8) {
            val p = board[r][c]
            if (p?.type == ChessPieceType.KING && p.color == kingColor) {
                kingR = r
                kingC = c
                break
            }
        }
    }
    if (kingR < 0) return true
    val enemyColor = if (kingColor == ChessColor.WHITE) ChessColor.BLACK else ChessColor.WHITE
    for (r in 0 until 8) {
        for (c in 0 until 8) {
            val p = board[r][c] ?: continue
            if (p.color == enemyColor) {
                val enemyMoves = if (p.type == ChessPieceType.KING) {
                    val list = mutableListOf<ChessMove>()
                    for (dr in -1..1) for (dc in -1..1) {
                        if (dr != 0 || dc != 0) list.add(ChessMove(r, c, r + dr, c + dc))
                    }
                    list
                } else {
                    generatePseudoChessMoves(board, r, c, p, null)
                }
                if (enemyMoves.any { it.toRow == kingR && it.toCol == kingC }) return true
            }
        }
    }
    return false
}

// ============================================================================
// 7. COMPLETE PLAYABLE CHECKERS (8x8 Board, 24 Pieces, Diagonal Moves,
//    Capturing, Multi-Jump, King Promotion, Winner Detection)
// ============================================================================

data class CheckersPiece(
    val isRed: Boolean,
    val isKing: Boolean = false
)

data class CheckersMove(
    val fromR: Int,
    val fromC: Int,
    val toR: Int,
    val toC: Int,
    val capturedR: Int? = null,
    val capturedC: Int? = null
)

@Composable
fun PlayableCheckersArena(
    gameRoom: GameRoomDoc,
    players: List<GamePlayerSlot>,
    currentUserProfile: UserProfile?,
    onSyncState: (String, String, List<GamePlayerSlot>?, String?, String?, String?) -> Unit,
    onFinishMatch: (UserProfile, String, Long, Long) -> Unit
) {
    val myUid = currentUserProfile?.userId?.ifBlank { currentUserProfile.id } ?: ""
    val redPlayer = players.getOrNull(0)
    val goldPlayer = players.getOrNull(1)

    var board by remember { mutableStateOf(createInitialCheckersBoard()) }
    var redTurn by remember { mutableStateOf(true) }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var legalMoves by remember { mutableStateOf<List<CheckersMove>>(emptyList()) }
    var redCaptured by remember { mutableIntStateOf(0) }
    var goldCaptured by remember { mutableIntStateOf(0) }
    var statusText by remember { mutableStateOf("Red's Turn • Tap a Red piece to move diagonally") }

    fun getMovesForPiece(grid: Array<Array<CheckersPiece?>>, r: Int, c: Int): List<CheckersMove> {
        val p = grid[r][c] ?: return emptyList()
        val rowDirs = if (p.isKing) listOf(-1, 1) else if (p.isRed) listOf(-1) else listOf(1)
        val moves = mutableListOf<CheckersMove>()
        for (dr in rowDirs) {
            for (dc in listOf(-1, 1)) {
                val nr = r + dr
                val nc = c + dc
                if (nr in 0..7 && nc in 0..7 && grid[nr][nc] == null) {
                    moves.add(CheckersMove(r, c, nr, nc))
                }
                val jr = r + dr * 2
                val jc = c + dc * 2
                if (jr in 0..7 && jc in 0..7 && grid[jr][jc] == null) {
                    val mid = grid[nr][nc]
                    if (mid != null && mid.isRed != p.isRed) {
                        moves.add(CheckersMove(r, c, jr, jc, nr, nc))
                    }
                }
            }
        }
        return moves
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
            .testTag("playable_checkers_arena"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF141026),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔴 ${redPlayer?.username ?: "Red"}: $redCaptured/12",
                    color = if (redTurn) Color(0xFFFF5252) else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "🟡 ${goldPlayer?.username ?: "Gold"}: $goldCaptured/12",
                    color = if (!redTurn) Color(0xFFFFD700) else Color(0xFFB8B2D1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(statusText, color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        // 8x8 Checkers Board
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1B1333),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(2.dp, Color(0xFFFFB800).copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .padding(6.dp)
                .testTag("checkers_board_grid")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until 8) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (c in 0 until 8) {
                            val isDark = (r + c) % 2 == 1
                            val piece = board[r][c]
                            val isSelected = selectedCell?.first == r && selectedCell?.second == c
                            val moveHere = legalMoves.firstOrNull { it.toR == r && it.toC == c }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        when {
                                            isSelected -> Color(0xFF7C4DFF)
                                            moveHere != null -> Color(0xFF00E676).copy(alpha = 0.45f)
                                            isDark -> Color(0xFF281E46)
                                            else -> Color(0xFFE6DCF5)
                                        }
                                    )
                                    .clickable {
                                        if (moveHere != null) {
                                            val next = Array(8) { rr -> Array(8) { cc -> board[rr][cc] } }
                                            val moving = next[moveHere.fromR][moveHere.fromC] ?: return@clickable
                                            next[moveHere.fromR][moveHere.fromC] = null
                                            val crowned = moving.isKing ||
                                                (moving.isRed && moveHere.toR == 0) ||
                                                (!moving.isRed && moveHere.toR == 7)
                                            next[moveHere.toR][moveHere.toC] = moving.copy(isKing = crowned)

                                            var didCapture = false
                                            if (moveHere.capturedR != null && moveHere.capturedC != null) {
                                                next[moveHere.capturedR][moveHere.capturedC] = null
                                                didCapture = true
                                                if (moving.isRed) redCaptured++ else goldCaptured++
                                                GameSoundEffectsEngine.playCapture()
                                            } else {
                                                GameSoundEffectsEngine.playMove()
                                            }

                                            board = next
                                            // Check multi-jump continuation
                                            val followUpJumps = if (didCapture && !crowned) {
                                                getMovesForPiece(next, moveHere.toR, moveHere.toC)
                                                    .filter { it.capturedR != null }
                                            } else {
                                                emptyList()
                                            }

                                            if (redCaptured >= 12 || goldCaptured >= 12) {
                                                GameSoundEffectsEngine.playVictory()
                                                val winnerName = if (redCaptured >= 12) (redPlayer?.username ?: "Red") else (goldPlayer?.username ?: "Gold")
                                                statusText = "VICTORY! $winnerName captured all 12 pieces!"
                                                val winnerProfile = currentUserProfile ?: UserProfile(userId = myUid, username = winnerName)
                                                onFinishMatch(winnerProfile, "Challenger", (redCaptured * 25).toLong(), (goldCaptured * 25).toLong())
                                            } else if (followUpJumps.isNotEmpty()) {
                                                selectedCell = Pair(moveHere.toR, moveHere.toC)
                                                legalMoves = followUpJumps
                                                statusText = "MULTI-JUMP AVAILABLE! Jump again!"
                                            } else {
                                                selectedCell = null
                                                legalMoves = emptyList()
                                                redTurn = !redTurn
                                                statusText = if (redTurn) "Red's Turn" else "Gold's Turn"
                                            }
                                            onSyncState(
                                                "{\"redTurn\":$redTurn,\"redCap\":$redCaptured,\"goldCap\":$goldCaptured}",
                                                myUid,
                                                null,
                                                null,
                                                null,
                                                null
                                            )
                                        } else if (piece != null && piece.isRed == redTurn) {
                                            GameSoundEffectsEngine.playTap()
                                            selectedCell = Pair(r, c)
                                            legalMoves = getMovesForPiece(board, r, c)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (piece != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    if (piece.isRed) {
                                                        listOf(Color(0xFFFF8A80), Color(0xFFD50000))
                                                    } else {
                                                        listOf(Color(0xFFFFF59D), Color(0xFFFF8F00))
                                                    }
                                                )
                                            )
                                            .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (piece.isKing) {
                                            Text("👑", fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun createInitialCheckersBoard(): Array<Array<CheckersPiece?>> {
    val grid = Array(8) { arrayOfNulls<CheckersPiece>(8) }
    for (r in 0..2) {
        for (c in 0..7) {
            if ((r + c) % 2 == 1) grid[r][c] = CheckersPiece(isRed = false, isKing = false)
        }
    }
    for (r in 5..7) {
        for (c in 0..7) {
            if ((r + c) % 2 == 1) grid[r][c] = CheckersPiece(isRed = true, isKing = false)
        }
    }
    return grid
}

// ============================================================================
// 8. COMPLETE PLAYABLE TIC TAC TOE (3x3 Neon Board, X/O, Win Line, Draw, Replay)
// ============================================================================

@Composable
fun PlayableTicTacToeArena(
    gameRoom: GameRoomDoc,
    players: List<GamePlayerSlot>,
    currentUserProfile: UserProfile?,
    onSyncState: (String, String, List<GamePlayerSlot>?, String?, String?, String?) -> Unit,
    onFinishMatch: (UserProfile, String, Long, Long) -> Unit
) {
    val myUid = currentUserProfile?.userId?.ifBlank { currentUserProfile.id } ?: ""
    val playerX = players.getOrNull(0)?.username ?: currentUserProfile?.username ?: "Player X"
    val playerO = players.getOrNull(1)?.username ?: "Challenger O"

    var cells by remember { mutableStateOf(List(9) { "" }) }
    var xTurn by remember { mutableStateOf(true) }
    var winningLine by remember { mutableStateOf<List<Int>?>(null) }
    var xWins by remember { mutableIntStateOf(0) }
    var oWins by remember { mutableIntStateOf(0) }
    var draws by remember { mutableIntStateOf(0) }
    var statusMsg by remember { mutableStateOf("$playerX's Turn (X)") }

    val winPatterns = remember {
        listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("playable_tic_tac_toe_arena"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF17122B)) {
                Text(
                    text = "❌ $playerX: $xWins",
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF17122B)) {
                Text(
                    text = "Draws: $draws",
                    color = Color(0xFFB8B2D1),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF17122B)) {
                Text(
                    text = "⭕ $playerO: $oWins",
                    color = Color(0xFFFF2E93),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(statusMsg, color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))

        // 3x3 Neon Board
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF120E24),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .aspectRatio(1f)
                .border(
                    2.dp,
                    Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF2E93))),
                    RoundedCornerShape(24.dp)
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (r in 0..2) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (c in 0..2) {
                            val idx = r * 3 + c
                            val symbol = cells[idx]
                            val isWinningCell = winningLine?.contains(idx) == true

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isWinningCell) Color(0xFF00C853).copy(alpha = 0.35f) else Color(0xFF1D1736),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(
                                        1.5.dp,
                                        when {
                                            isWinningCell -> Color(0xFF00E676)
                                            symbol == "X" -> Color(0xFF00E5FF).copy(alpha = 0.7f)
                                            symbol == "O" -> Color(0xFFFF2E93).copy(alpha = 0.7f)
                                            else -> Color.White.copy(alpha = 0.14f)
                                        },
                                        RoundedCornerShape(18.dp)
                                    )
                                    .clickable(enabled = symbol.isEmpty() && winningLine == null) {
                                        val mark = if (xTurn) "X" else "O"
                                        val nextCells = cells.toMutableList().also { it[idx] = mark }
                                        cells = nextCells
                                        val matched = winPatterns.firstOrNull { line ->
                                            line.all { nextCells[it] == mark }
                                        }
                                        if (matched != null) {
                                            winningLine = matched
                                            GameSoundEffectsEngine.playVictory()
                                            if (mark == "X") xWins++ else oWins++
                                            val winnerName = if (mark == "X") playerX else playerO
                                            statusMsg = "🏆 $winnerName ($mark) Wins Round!"
                                            if (xWins >= 2 || oWins >= 2) {
                                                val wp = currentUserProfile ?: UserProfile(userId = myUid, username = winnerName)
                                                onFinishMatch(wp, playerO, (xWins * 100).toLong(), (oWins * 100).toLong())
                                            }
                                        } else if (nextCells.none { it.isEmpty() }) {
                                            draws++
                                            GameSoundEffectsEngine.playCapture()
                                            statusMsg = "🤝 Round Draw! Tap Play Again."
                                        } else {
                                            GameSoundEffectsEngine.playMove()
                                            xTurn = !xTurn
                                            statusMsg = if (xTurn) "$playerX's Turn (X)" else "$playerO's Turn (O)"
                                        }
                                        onSyncState(
                                            "{\"cells\":\"${nextCells.joinToString(",")}\",\"xTurn\":$xTurn}",
                                            myUid,
                                            null,
                                            null,
                                            null,
                                            null
                                        )
                                    }
                                    .testTag("ttt_cell_$idx")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = symbol,
                                        color = if (symbol == "X") Color(0xFF00E5FF) else Color(0xFFFF2E93),
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = {
                cells = List(9) { "" }
                winningLine = null
                xTurn = true
                statusMsg = "New Round • $playerX's Turn (X)"
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text("Replay Round", color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ============================================================================
// 9. COMPLETE PLAYABLE DOMINOES (28 Double-Six Tiles, Player Hands, Boneyard,
//    Left/Right Chain Ends, Pip Scoring, Multiplayer Sync)
// ============================================================================

data class DominoTile(val left: Int, val right: Int) {
    val pipSum: Int get() = left + right
    fun matches(endVal: Int): Boolean = left == endVal || right == endVal
}

@Composable
fun PlayableDominoesArena(
    gameRoom: GameRoomDoc,
    players: List<GamePlayerSlot>,
    currentUserProfile: UserProfile?,
    onSyncState: (String, String, List<GamePlayerSlot>?, String?, String?, String?) -> Unit,
    onFinishMatch: (UserProfile, String, Long, Long) -> Unit
) {
    val myUid = currentUserProfile?.userId?.ifBlank { currentUserProfile.id } ?: ""

    fun buildDoubleSixDeck(): List<DominoTile> {
        val deck = mutableListOf<DominoTile>()
        for (i in 0..6) {
            for (j in i..6) {
                deck.add(DominoTile(i, j))
            }
        }
        return deck.shuffled()
    }

    val initialDeck = remember { buildDoubleSixDeck() }
    var myHand by remember { mutableStateOf(initialDeck.take(7)) }
    var opponentHand by remember { mutableStateOf(initialDeck.drop(7).take(7)) }
    var boneyard by remember { mutableStateOf(initialDeck.drop(14)) }
    var boardChain by remember { mutableStateOf<List<DominoTile>>(emptyList()) }
    var leftEnd by remember { mutableIntStateOf(-1) }
    var rightEnd by remember { mutableIntStateOf(-1) }
    var selectedTileIndex by remember { mutableIntStateOf(-1) }
    var myScore by remember { mutableIntStateOf(0) }
    var opponentScore by remember { mutableIntStateOf(0) }
    var statusText by remember { mutableStateOf("Your Turn • Tap any Domino tile from your hand to play") }

    fun triggerBotOrOpponentReply() {
        if (opponentHand.isEmpty()) return
        val playableIdx = opponentHand.indexOfFirst { tile ->
            boardChain.isEmpty() || tile.matches(leftEnd) || tile.matches(rightEnd)
        }
        if (playableIdx >= 0) {
            val t = opponentHand[playableIdx]
            opponentHand = opponentHand.filterIndexed { i, _ -> i != playableIdx }
            if (boardChain.isEmpty()) {
                boardChain = listOf(t)
                leftEnd = t.left
                rightEnd = t.right
            } else if (t.matches(rightEnd)) {
                val oriented = if (t.left == rightEnd) t else DominoTile(t.right, t.left)
                boardChain = boardChain + oriented
                rightEnd = oriented.right
            } else {
                val oriented = if (t.right == leftEnd) t else DominoTile(t.right, t.left)
                boardChain = listOf(oriented) + boardChain
                leftEnd = oriented.left
            }
            val endsSum = leftEnd + rightEnd
            if (endsSum % 5 == 0) opponentScore += endsSum
            statusText = "Opponent played [${t.left}|${t.right}] • Your Turn!"
            if (opponentHand.isEmpty()) {
                val bonus = myHand.sumOf { it.pipSum }
                opponentScore += bonus
                statusText = "Opponent Dominoed! (+$bonus pip bonus)"
            }
        } else if (boneyard.isNotEmpty()) {
            opponentHand = opponentHand + boneyard.first()
            boneyard = boneyard.drop(1)
            statusText = "Opponent drew from Boneyard • Your Turn!"
        }
    }

    fun placeSelectedTile(playOnLeft: Boolean) {
        val tile = myHand.getOrNull(selectedTileIndex) ?: return
        if (boardChain.isEmpty()) {
            boardChain = listOf(tile)
            leftEnd = tile.left
            rightEnd = tile.right
        } else if (playOnLeft) {
            if (!tile.matches(leftEnd)) {
                statusText = "Tile [${tile.left}|${tile.right}] does not match Left End ($leftEnd)!"
                return
            }
            val oriented = if (tile.right == leftEnd) tile else DominoTile(tile.right, tile.left)
            boardChain = listOf(oriented) + boardChain
            leftEnd = oriented.left
        } else {
            if (!tile.matches(rightEnd)) {
                statusText = "Tile [${tile.left}|${tile.right}] does not match Right End ($rightEnd)!"
                return
            }
            val oriented = if (tile.left == rightEnd) tile else DominoTile(tile.right, tile.left)
            boardChain = boardChain + oriented
            rightEnd = oriented.right
        }

        GameSoundEffectsEngine.playMove()
        myHand = myHand.filterIndexed { idx, _ -> idx != selectedTileIndex }
        selectedTileIndex = -1

        val endsSum = leftEnd + rightEnd
        if (endsSum > 0 && endsSum % 5 == 0) {
            myScore += endsSum
        }

        if (myHand.isEmpty()) {
            val pipBonus = opponentHand.sumOf { it.pipSum }
            myScore += pipBonus + 50
            GameSoundEffectsEngine.playVictory()
            statusText = "DOMINO VICTORY! You cleared your hand (+$pipBonus pip bonus)!"
            val wp = currentUserProfile ?: UserProfile(userId = myUid, username = "Domino Master")
            onFinishMatch(wp, "Challenger", myScore.toLong(), opponentScore.toLong())
            return
        }

        triggerBotOrOpponentReply()
        onSyncState(
            "{\"chainSize\":${boardChain.size},\"left\":$leftEnd,\"right\":$rightEnd,\"score\":$myScore}",
            myUid,
            null,
            null,
            null,
            null
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
            .testTag("playable_dominoes_arena"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF141026),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("🁣 Your Score: $myScore", color = Color(0xFF00E676), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text("Boneyard: ${boneyard.size} tiles", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Opponent: ${opponentHand.size} tiles ($opponentScore pts)", color = Color(0xFFFF80AB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(statusText, color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))

        // Domino Chain Table
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0D2824),
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .border(2.dp, Color(0xFF2EC4B6), RoundedCornerShape(20.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (boardChain.isEmpty()) "Chain Empty • Play any tile to start" else "Left End: [$leftEnd]   •   Chain (${boardChain.size} tiles)   •   Right End: [$rightEnd]",
                    color = Color(0xFF80CBC4),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    boardChain.forEach { t ->
                        DominoTileCard(tile = t, isSelected = false, onClick = {})
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { placeSelectedTile(playOnLeft = true) },
                        enabled = selectedTileIndex >= 0,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EC4B6)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Play on Left End", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = { placeSelectedTile(playOnLeft = false) },
                        enabled = selectedTileIndex >= 0,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F1C)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Play on Right End", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Player's Domino Hand
        Text("Your Domino Hand (${myHand.size} tiles) • Tap a tile to select:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            myHand.forEachIndexed { idx, tile ->
                val playable = boardChain.isEmpty() || tile.matches(leftEnd) || tile.matches(rightEnd)
                DominoTileCard(
                    tile = tile,
                    isSelected = selectedTileIndex == idx,
                    isPlayable = playable,
                    onClick = {
                        GameSoundEffectsEngine.playTap()
                        selectedTileIndex = idx
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (boneyard.isNotEmpty()) {
                        GameSoundEffectsEngine.playTap()
                        val drawn = boneyard.first()
                        myHand = myHand + drawn
                        boneyard = boneyard.drop(1)
                        statusText = "Drew [${drawn.left}|${drawn.right}] from Boneyard"
                    } else {
                        triggerBotOrOpponentReply()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (boneyard.isNotEmpty()) "Draw from Boneyard (${boneyard.size})" else "Pass Turn", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DominoTileCard(
    tile: DominoTile,
    isSelected: Boolean,
    isPlayable: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFFFFF59D) else Color(0xFFF5F5F5),
        modifier = Modifier
            .width(64.dp)
            .height(36.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = when {
                    isSelected -> Color(0xFFFF2E93)
                    isPlayable -> Color(0xFF00E676)
                    else -> Color.Gray
                },
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(text = "${tile.left}", color = Color(0xFF141026), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Box(modifier = Modifier.width(1.5.dp).fillMaxHeight(0.75f).background(Color(0xFF141026)))
            Text(text = "${tile.right}", color = Color(0xFF141026), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ============================================================================
// 10. COMPLETE PLAYABLE SNAKES AND LADDERS (100-Square 10x10 Board,
//     Visual Snakes & Ladders, Animated Dice, Exact Movement, Multiplayer)
// ============================================================================

@Composable
fun PlayableSnakesAndLaddersArena(
    gameRoom: GameRoomDoc,
    players: List<GamePlayerSlot>,
    currentUserProfile: UserProfile?,
    onSyncState: (String, String, List<GamePlayerSlot>?, String?, String?, String?) -> Unit,
    onFinishMatch: (UserProfile, String, Long, Long) -> Unit
) {
    val myUid = currentUserProfile?.userId?.ifBlank { currentUserProfile.id } ?: ""
    val ladders = remember {
        mapOf(4 to 25, 13 to 46, 33 to 49, 42 to 63, 50 to 69, 62 to 81, 74 to 92)
    }
    val snakes = remember {
        mapOf(27 to 5, 40 to 3, 43 to 18, 54 to 31, 66 to 45, 89 to 53, 95 to 77, 99 to 41)
    }

    var p1Square by remember { mutableIntStateOf(1) }
    var p2Square by remember { mutableIntStateOf(1) }
    var lastDice by remember { mutableIntStateOf(6) }
    var myTurn by remember { mutableStateOf(true) }
    var eventLog by remember { mutableStateOf("Roll the 3D Dice to climb from Square 1 to 100!") }

    fun squareCenterOffset(square: Int, boardW: Float, boardH: Float): Offset {
        val idx = (square - 1).coerceIn(0, 99)
        val rowFromBottom = idx / 10
        val colInRow = idx % 10
        val col = if (rowFromBottom % 2 == 0) colInRow else (9 - colInRow)
        val rowFromTop = 9 - rowFromBottom
        val cellW = boardW / 10f
        val cellH = boardH / 10f
        return Offset((col + 0.5f) * cellW, (rowFromTop + 0.5f) * cellH)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
            .testTag("playable_snakes_ladders_arena"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF141026),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🟢 You: Sq $p1Square/100", color = Color(0xFF00E676), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text("🎲 Dice: $lastDice", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text("🟣 Rival: Sq $p2Square/100", color = Color(0xFFFF80AB), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(eventLog, color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))

        // 100-Square 10x10 Board with Visual Ladders & Snakes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFFFFD54F), RoundedCornerShape(16.dp))
                .testTag("snakes_ladders_board_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellW = size.width / 10f
                val cellH = size.height / 10f

                for (r in 0..9) {
                    for (c in 0..9) {
                        val isAlt = (r + c) % 2 == 0
                        drawRect(
                            color = if (isAlt) Color(0xFF1B1435) else Color(0xFF281E4B),
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW, cellH)
                        )
                    }
                }

                // Draw Golden Ladders
                ladders.forEach { (startSq, endSq) ->
                    val pStart = squareCenterOffset(startSq, size.width, size.height)
                    val pEnd = squareCenterOffset(endSq, size.width, size.height)
                    drawLine(
                        color = Color(0xFFFFD700),
                        start = pStart,
                        end = pEnd,
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }

                // Draw Neon Snakes
                snakes.forEach { (headSq, tailSq) ->
                    val pHead = squareCenterOffset(headSq, size.width, size.height)
                    val pTail = squareCenterOffset(tailSq, size.width, size.height)
                    drawLine(
                        color = Color(0xFFFF1744),
                        start = pHead,
                        end = pTail,
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }

                // Draw Player 1 & Player 2 Tokens
                val p1Pos = squareCenterOffset(p1Square, size.width, size.height)
                val p2Pos = squareCenterOffset(p2Square, size.width, size.height)
                drawCircle(color = Color(0xFF00E676), radius = cellW * 0.30f, center = Offset(p1Pos.x - 4f, p1Pos.y))
                drawCircle(color = Color.White, radius = cellW * 0.30f, center = Offset(p1Pos.x - 4f, p1Pos.y), style = Stroke(2.5f))
                drawCircle(color = Color(0xFFFF2E93), radius = cellW * 0.28f, center = Offset(p2Pos.x + 4f, p2Pos.y))
                drawCircle(color = Color.White, radius = cellW * 0.28f, center = Offset(p2Pos.x + 4f, p2Pos.y), style = Stroke(2.5f))
            }

            // Overlay square numbers 1..100
            Column(modifier = Modifier.fillMaxSize()) {
                for (rowTop in 0..9) {
                    val rowBottom = 9 - rowTop
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0..9) {
                            val sq = if (rowBottom % 2 == 0) {
                                rowBottom * 10 + col + 1
                            } else {
                                rowBottom * 10 + (9 - col) + 1
                            }
                            val isLadder = sq in ladders
                            val isSnake = sq in snakes
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp),
                                contentAlignment = Alignment.TopStart
                            ) {
                                Text(
                                    text = when {
                                        sq == 100 -> "100👑"
                                        isLadder -> "$sq🪜"
                                        isSnake -> "$sq🐍"
                                        else -> "$sq"
                                    },
                                    color = when {
                                        sq == 100 -> Color(0xFFFFD700)
                                        isLadder -> Color(0xFF69F0AE)
                                        isSnake -> Color(0xFFFF8A80)
                                        else -> Color.White.copy(alpha = 0.72f)
                                    },
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                GameSoundEffectsEngine.playDiceRoll()
                val roll = (1..6).random()
                lastDice = roll
                val tentative = if (p1Square + roll <= 100) p1Square + roll else p1Square
                val afterJump = ladders[tentative] ?: snakes[tentative] ?: tentative
                p1Square = afterJump

                eventLog = when {
                    tentative in ladders -> "🎲 Rolled $roll! Climbed Ladder $tentative 🪜 $afterJump!"
                    tentative in snakes -> "🎲 Rolled $roll! Bit by Snake at $tentative 🐍 slid to $afterJump!"
                    else -> "🎲 Rolled $roll • Moved to Square $p1Square"
                }

                if (p1Square == 100) {
                    GameSoundEffectsEngine.playVictory()
                    eventLog = "🏆 Square 100 Reached! You Win Snakes & Ladders!"
                    val wp = currentUserProfile ?: UserProfile(userId = myUid, username = "Champion")
                    onFinishMatch(wp, "Challenger", 100L, p2Square.toLong())
                    return@Button
                }

                // Opponent turn
                val oppRoll = (1..6).random()
                val oppTentative = if (p2Square + oppRoll <= 100) p2Square + oppRoll else p2Square
                p2Square = ladders[oppTentative] ?: snakes[oppTentative] ?: oppTentative
                myTurn = true

                onSyncState(
                    "{\"p1\":$p1Square,\"p2\":$p2Square,\"dice\":$roll}",
                    myUid,
                    null,
                    null,
                    null,
                    null
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2E93)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("snakes_ladders_roll_btn")
        ) {
            Icon(Icons.Default.Casino, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Roll 3D Dice (Current: $lastDice)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ============================================================================
// 11. COMPLETE PLAYABLE MINI ARCADE GAMES HUB (4 Playable Arcade Classics:
//     Bubble Shooter, Memory Match, Snake, Block Puzzle)
// ============================================================================

@Composable
fun PlayableMiniArcadeArena(
    gameRoom: GameRoomDoc,
    players: List<GamePlayerSlot>,
    currentUserProfile: UserProfile?,
    onSyncState: (String, String, List<GamePlayerSlot>?, String?, String?, String?) -> Unit,
    onFinishMatch: (UserProfile, String, Long, Long) -> Unit
) {
    val myUid = currentUserProfile?.userId?.ifBlank { currentUserProfile.id } ?: ""
    var selectedArcadeTab by remember { mutableIntStateOf(0) }
    val arcadeTitles = listOf("Bubble Shooter", "Memory Match", "Snake", "Block Puzzle")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
            .testTag("playable_mini_arcade_arena"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Arcade Game Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            arcadeTitles.forEachIndexed { idx, title ->
                val selected = selectedArcadeTab == idx
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) Color(0xFFFF2E93) else Color(0xFF1A1433),
                    modifier = Modifier
                        .clickable {
                            GameSoundEffectsEngine.playTap()
                            selectedArcadeTab = idx
                        }
                        .testTag("mini_arcade_tab_$idx")
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        when (selectedArcadeTab) {
            0 -> MiniBubbleShooterGame(currentUserProfile, myUid, onFinishMatch)
            1 -> MiniMemoryMatchGame(currentUserProfile, myUid, onFinishMatch)
            2 -> MiniSnakeArcadeGame(currentUserProfile, myUid, onFinishMatch)
            else -> MiniBlockPuzzleGame(currentUserProfile, myUid, onFinishMatch)
        }
    }
}

@Composable
private fun MiniBubbleShooterGame(
    currentUserProfile: UserProfile?,
    myUid: String,
    onFinishMatch: (UserProfile, String, Long, Long) -> Unit
) {
    val bubbleColors = listOf(Color(0xFFFF2E93), Color(0xFF00E5FF), Color(0xFFFFEA00), Color(0xFF00E676))
    var grid by remember {
        mutableStateOf(List(5) { List(7) { (0..3).random() } })
    }
    var cannonColorIdx by remember { mutableIntStateOf(0) }
    var aimColumn by remember { mutableFloatStateOf(3f) }
    var score by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("Aim Cannon & Tap FIRE BUBBLE to pop matching clusters!") }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("🎯 Bubble Shooter Score: $score", color = Color(0xFFFFD700), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text(status, color = Color(0xFFB8B2D1), fontSize = 11.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF120D24),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grid.forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEachIndexed { c, colorIdx ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (colorIdx >= 0) bubbleColors[colorIdx] else Color.Transparent
                                    )
                                    .border(
                                        width = if (c == aimColumn.roundToInt() && colorIdx >= 0) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Cannon Preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Cannon Bubble:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(bubbleColors[cannonColorIdx])
                            .border(2.dp, Color.White, CircleShape)
                    )
                    Text("Target Col: ${aimColumn.roundToInt() + 1}", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = aimColumn,
                    onValueChange = { aimColumn = it },
                    valueRange = 0f..6f,
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF2E93), activeTrackColor = Color(0xFF00E5FF))
                )

                Button(
                    onClick = {
                        val col = aimColumn.roundToInt().coerceIn(0, 6)
                        val nextGrid = grid.map { it.toMutableList() }.toMutableList()
                        var popped = 0
                        for (r in 4 downTo 0) {
                            val cellColor = nextGrid[r][col]
                            if (cellColor >= 0) {
                                if (cellColor == cannonColorIdx) {
                                    // Pop matching bubbles in column & neighbors
                                    nextGrid[r][col] = -1
                                    popped++
                                    if (col > 0 && nextGrid[r][col - 1] == cannonColorIdx) {
                                        nextGrid[r][col - 1] = -1
                                        popped++
                                    }
                                    if (col < 6 && nextGrid[r][col + 1] == cannonColorIdx) {
                                        nextGrid[r][col + 1] = -1
                                        popped++
                                    }
                                    if (r > 0 && nextGrid[r - 1][col] == cannonColorIdx) {
                                        nextGrid[r - 1][col] = -1
                                        popped++
                                    }
                                } else {
                                    nextGrid[r][col] = cannonColorIdx
                                }
                                break
                            }
                        }
                        if (popped > 0) {
                            GameSoundEffectsEngine.playPocket()
                            val pts = popped * 35
                            score += pts
                            status = "💥 Popped $popped matching bubbles (+$pts pts)!"
                        } else {
                            GameSoundEffectsEngine.playMove()
                            score += 10
                            status = "Bubble attached! Match colors to pop clusters."
                        }
                        grid = nextGrid
                        cannonColorIdx = (0..3).random()
                        if (score >= 250 || nextGrid.flatten().all { it < 0 }) {
                            GameSoundEffectsEngine.playVictory()
                            val wp = currentUserProfile ?: UserProfile(userId = myUid, username = "Arcade Pro")
                            onFinishMatch(wp, "Challenger", score.toLong(), 120L)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2E93)),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("FIRE BUBBLE", color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun MiniMemoryMatchGame(
    currentUserProfile: UserProfile?,
    myUid: String,
    onFinishMatch: (UserProfile, String, Long, Long) -> Unit
) {
    val symbols = remember {
        listOf("💎", "👑", "🚀", "🔥", "⚡", "🌟", "🎯", "🏆")
            .let { it + it }
            .shuffled()
    }
    var revealed by remember { mutableStateOf(List(16) { false }) }
    var matched by remember { mutableStateOf(List(16) { false }) }
    var firstIdx by remember { mutableIntStateOf(-1) }
    var moves by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("🧠 Memory Match • Score: $score • Moves: $moves", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))

        for (r in 0..3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (c in 0..3) {
                    val idx = r * 4 + c
                    val isFaceUp = revealed[idx] || matched[idx]
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            matched[idx] -> Color(0xFF00C853).copy(alpha = 0.35f)
                            isFaceUp -> Color(0xFF7C4DFF)
                            else -> Color(0xFF1D1636)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .padding(vertical = 4.dp)
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable(enabled = !isFaceUp) {
                                GameSoundEffectsEngine.playTap()
                                val nextRev = revealed.toMutableList().also { it[idx] = true }
                                revealed = nextRev
                                if (firstIdx < 0) {
                                    firstIdx = idx
                                } else {
                                    moves++
                                    val prev = firstIdx
                                    firstIdx = -1
                                    if (symbols[prev] == symbols[idx]) {
                                        GameSoundEffectsEngine.playPocket()
                                        val nextMat = matched.toMutableList().also {
                                            it[prev] = true
                                            it[idx] = true
                                        }
                                        matched = nextMat
                                        score += 50
                                        if (nextMat.all { it }) {
                                            GameSoundEffectsEngine.playVictory()
                                            val wp = currentUserProfile ?: UserProfile(userId = myUid, username = "Memory King")
                                            onFinishMatch(wp, "Challenger", score.toLong(), 150L)
                                        }
                                    } else {
                                        revealed = List(16) { false }
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isFaceUp) symbols[idx] else "❓",
                                fontSize = 24.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniSnakeArcadeGame(
    currentUserProfile: UserProfile?,
    myUid: String,
    onFinishMatch: (UserProfile, String, Long, Long) -> Unit
) {
    val gridSize = 12
    var snake by remember { mutableStateOf(listOf(Pair(6, 5), Pair(6, 4), Pair(6, 3))) }
    var dir by remember { mutableStateOf(Pair(0, 1)) }
    var apple by remember { mutableStateOf(Pair(3, 8)) }
    var score by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(true) }

    LaunchedEffect(running, dir, snake) {
        if (!running) return@LaunchedEffect
        delay(260L)
        val head = snake.first()
        val nextR = (head.first + dir.first + gridSize) % gridSize
        val nextC = (head.second + dir.second + gridSize) % gridSize
        val nextHead = Pair(nextR, nextC)
        if (nextHead == apple) {
            GameSoundEffectsEngine.playPocket()
            score += 30
            snake = listOf(nextHead) + snake
            apple = Pair((0 until gridSize).random(), (0 until gridSize).random())
            if (score >= 180) {
                running = false
                GameSoundEffectsEngine.playVictory()
                val wp = currentUserProfile ?: UserProfile(userId = myUid, username = "Snake Champion")
                onFinishMatch(wp, "Challenger", score.toLong(), 90L)
            }
        } else {
            snake = listOf(nextHead) + snake.dropLast(1)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("🐍 Neon Snake • Score: $score / 180", color = Color(0xFF00E676), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0E0A1C))
                .border(2.dp, Color(0xFF00E676), RoundedCornerShape(16.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cw = size.width / gridSize
                val ch = size.height / gridSize
                // Apple
                drawCircle(
                    color = Color(0xFFFF2E93),
                    radius = cw * 0.42f,
                    center = Offset((apple.second + 0.5f) * cw, (apple.first + 0.5f) * ch)
                )
                // Snake body
                snake.forEachIndexed { idx, seg ->
                    drawRoundRect(
                        color = if (idx == 0) Color(0xFF00E5FF) else Color(0xFF00E676),
                        topLeft = Offset(seg.second * cw + 2f, seg.first * ch + 2f),
                        size = Size(cw - 4f, ch - 4f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // D-Pad Controls
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { dir = Pair(0, -1) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281F47))) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = Color.White)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { dir = Pair(-1, 0) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281F47))) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = Color.White)
                }
                Button(onClick = { dir = Pair(1, 0) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281F47))) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.White)
                }
            }
            Button(onClick = { dir = Pair(0, 1) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281F47))) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = Color.White)
            }
        }
    }
}

@Composable
private fun MiniBlockPuzzleGame(
    currentUserProfile: UserProfile?,
    myUid: String,
    onFinishMatch: (UserProfile, String, Long, Long) -> Unit
) {
    var grid by remember { mutableStateOf(List(6) { List(6) { false } }) }
    var score by remember { mutableIntStateOf(0) }
    var linesCleared by remember { mutableIntStateOf(0) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("🧱 Block Puzzle • Score: $score • Lines Cleared: $linesCleared", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        Text("Tap grid cells to place jewel blocks & complete full rows or columns!", color = Color(0xFFB8B2D1), fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF130E26),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .border(2.dp, Color(0xFF7C4DFF), RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                for (r in 0..5) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (c in 0..5) {
                            val filled = grid[r][c]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (filled) Color(0xFFFF2E93) else Color(0xFF221A3F))
                                    .clickable {
                                        val next = grid.map { it.toMutableList() }.toMutableList()
                                        next[r][c] = true
                                        if (c + 1 <= 5) next[r][c + 1] = true
                                        var clearedNow = 0
                                        for (rr in 0..5) {
                                            if (next[rr].all { it }) {
                                                for (cc in 0..5) next[rr][cc] = false
                                                clearedNow++
                                            }
                                        }
                                        for (cc in 0..5) {
                                            if ((0..5).all { rr -> next[rr][cc] }) {
                                                for (rr in 0..5) next[rr][cc] = false
                                                clearedNow++
                                            }
                                        }
                                        grid = next
                                        if (clearedNow > 0) {
                                            GameSoundEffectsEngine.playPocket()
                                            linesCleared += clearedNow
                                            score += clearedNow * 60
                                        } else {
                                            GameSoundEffectsEngine.playMove()
                                            score += 10
                                        }
                                        if (score >= 220) {
                                            GameSoundEffectsEngine.playVictory()
                                            val wp = currentUserProfile ?: UserProfile(userId = myUid, username = "Block Master")
                                            onFinishMatch(wp, "Challenger", score.toLong(), 100L)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
