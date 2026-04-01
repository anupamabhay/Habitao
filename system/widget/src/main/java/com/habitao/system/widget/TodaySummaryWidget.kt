package com.habitao.system.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

private const val MainActivityPackage = "com.habitao.app"
private const val MainActivityClass = "com.habitao.app.MainActivity"

class TodaySummaryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: androidx.glance.appwidget.GlanceId,
    ) {
        provideContent {
            TodaySummaryWidgetContent()
        }
    }
}

@Composable
private fun TodaySummaryWidgetContent() {
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
                .clickable(
                    actionStartActivity(Intent().setClassName(MainActivityPackage, MainActivityClass)),
                ),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Text(
            text = "Today",
            style = TextStyle(fontWeight = FontWeight.Bold),
        )
        Text(text = "Open Habitao to check habits and tasks")
    }
}
