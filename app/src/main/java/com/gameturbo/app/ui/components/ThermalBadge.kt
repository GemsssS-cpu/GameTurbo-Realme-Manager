package com.gameturbo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gameturbo.app.monitor.ThermalLevel
import com.gameturbo.app.ui.theme.*

@Composable
fun ThermalBadge(
    thermalLevel: ThermalLevel,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, label) = when (thermalLevel) {
        ThermalLevel.COOL -> Triple(ThermalCool.copy(alpha = 0.15f), ThermalCool, "COOL")
        ThermalLevel.NORMAL -> Triple(ThermalNormal.copy(alpha = 0.15f), ThermalNormal, "NORMAL")
        ThermalLevel.WARM -> Triple(ThermalWarm.copy(alpha = 0.15f), ThermalWarm, "WARM")
        ThermalLevel.HOT -> Triple(ThermalHot.copy(alpha = 0.15f), ThermalHot, "HOT")
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
