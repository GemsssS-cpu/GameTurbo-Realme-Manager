package com.gameturbo.app.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.gameturbo.app.data.GameInfo
import com.gameturbo.app.data.GameRepository
import com.gameturbo.app.ui.theme.AccentBlue
import com.gameturbo.app.ui.theme.DarkCard

@Composable
fun GameCard(
    game: GameInfo,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onLaunch: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gameRepository = GameRepository(context)
    val icon = gameRepository.getGameIcon(game.packageName)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .clickable { onLaunch?.invoke() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let { drawable ->
            val bitmap = drawable.toBitmap(48, 48).asImageBitmap()
            Image(
                bitmap = bitmap,
                contentDescription = game.name,
                modifier = Modifier.size(48.dp)
            )
        } ?: Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF30363D), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("🎮", fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = game.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = game.packageName,
                fontSize = 11.sp,
                color = Color(0xFF8B949E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = AccentBlue,
                uncheckedColor = Color(0xFF8B949E)
            )
        )
    }
}
