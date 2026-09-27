package com.commitpulse.app.ui.components

import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.WidgetSettings
import com.commitpulse.app.ui.theme.widgetTheme
import com.commitpulse.app.widget.CommitPulseWidgetContent
import java.time.LocalDate

/**
 * Podgląd renderujący dokładnie ten sam kod Glance co widget na ekranie głównym (RemoteViews
 * w zadanym rozmiarze) — dzięki temu podgląd nie rozjeżdża się z prawdziwym widgetem.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
fun LiveWidgetPreview(
    settings: WidgetSettings,
    history: List<DayCommit>,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var remoteViews by remember { mutableStateOf<RemoteViews?>(null) }

    LaunchedEffect(settings, history, width, height) {
        remoteViews = GlanceRemoteViews().compose(context, DpSize(width, height)) {
            CommitPulseWidgetContent(settings, history, LocalDate.now(), widgetTheme(context, settings), interactive = false)
        }.remoteViews
    }

    AndroidView(
        factory = { FrameLayout(it) },
        update = { frame ->
            frame.removeAllViews()
            remoteViews?.let { views ->
                frame.addView(
                    views.apply(frame.context, frame),
                    FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
                )
            }
        },
        modifier = modifier.size(width, height),
    )
}
