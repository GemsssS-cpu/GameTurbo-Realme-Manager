package com.gameturbo.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gameturbo.app.monitor.ThermalLevel
import com.gameturbo.app.ui.theme.*

@Composable
fun TemperatureGauge(
    temperature: Float,
    thermalLevel: ThermalLevel,
    modifier: Modifier = Modifier
) {
    val color = when (thermalLevel) {
        ThermalLevel.COOL -> ThermalCool
        ThermalLevel.NORMAL -> ThermalNormal
        ThermalLevel.WARM -> ThermalWarm
        ThermalLevel.HOT -> ThermalHot
    }

    val sweepFraction = (temperature / 60f).coerceIn(0f, 1f)

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 12.dp.toPx()
            val padding = strokeWidth / 2
            val size = Size(
                size.width - strokeWidth,
                size.height - strokeWidth
            )
            val topLeft = Offset(padding, padding)

            // Background arc
            drawArc(
                color = Color(0xFF30363D),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * sweepFraction,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${temperature.toInt()}°C",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = thermalLevel.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}
