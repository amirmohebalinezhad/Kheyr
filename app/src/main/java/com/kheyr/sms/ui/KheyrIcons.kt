package com.kheyr.sms.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Small, hand-tuned icon set so the app has one consistent visual voice instead of mixing the
 * default Material glyphs with the brand mark. Sizes follow the Material 24dp grid.
 */
object KheyrIcons {
    val Chat: ImageVector by lazy {
        icon("Kheyr.Chat") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2.4f)
                curveTo(6.48f, 2.4f, 2f, 6.34f, 2f, 11.2f)
                curveTo(2f, 13.73f, 3.22f, 16f, 5.16f, 17.56f)
                lineTo(4.56f, 20.9f)
                curveTo(4.46f, 21.45f, 5.02f, 21.88f, 5.52f, 21.65f)
                lineTo(9.68f, 19.7f)
                curveTo(10.42f, 19.9f, 11.2f, 20f, 12f, 20f)
                curveTo(17.52f, 20f, 22f, 16.06f, 22f, 11.2f)
                curveTo(22f, 6.34f, 17.52f, 2.4f, 12f, 2.4f)
                close()
                moveTo(8f, 9.1f)
                lineTo(16f, 9.1f)
                lineTo(16f, 10.9f)
                lineTo(8f, 10.9f)
                close()
                moveTo(8f, 12.7f)
                lineTo(13.4f, 12.7f)
                lineTo(13.4f, 14.5f)
                lineTo(8f, 14.5f)
                close()
            }
        }
    }

    val Contacts: ImageVector by lazy {
        icon("Kheyr.Contacts") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8.6f, 11.3f)
                curveTo(10.5f, 11.3f, 12f, 9.8f, 12f, 7.9f)
                curveTo(12f, 6f, 10.5f, 4.5f, 8.6f, 4.5f)
                curveTo(6.7f, 4.5f, 5.2f, 6f, 5.2f, 7.9f)
                curveTo(5.2f, 9.8f, 6.7f, 11.3f, 8.6f, 11.3f)
                close()
                moveTo(8.6f, 13.1f)
                curveTo(5.2f, 13.1f, 2.4f, 15.2f, 2.4f, 17.8f)
                lineTo(2.4f, 19.4f)
                lineTo(14.8f, 19.4f)
                lineTo(14.8f, 17.8f)
                curveTo(14.8f, 15.2f, 12f, 13.1f, 8.6f, 13.1f)
                close()
                moveTo(16.4f, 11f)
                curveTo(18f, 11f, 19.3f, 9.7f, 19.3f, 8.1f)
                curveTo(19.3f, 6.5f, 18f, 5.2f, 16.4f, 5.2f)
                curveTo(16f, 5.2f, 15.6f, 5.3f, 15.3f, 5.4f)
                curveTo(15.9f, 6.2f, 16.2f, 7.1f, 16.2f, 8.1f)
                curveTo(16.2f, 9.1f, 15.9f, 10f, 15.3f, 10.8f)
                curveTo(15.6f, 10.9f, 16f, 11f, 16.4f, 11f)
                close()
                moveTo(17.2f, 13.3f)
                curveTo(18.9f, 14.1f, 20.1f, 15.6f, 20.1f, 17.5f)
                lineTo(20.1f, 19.4f)
                lineTo(22.6f, 19.4f)
                lineTo(22.6f, 17.8f)
                curveTo(22.6f, 15.6f, 20.2f, 13.9f, 17.2f, 13.3f)
                close()
            }
        }
    }

    val Profile: ImageVector by lazy {
        icon("Kheyr.Profile") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 2.6f)
                arcToRelative(9.4f, 9.4f, 0f, false, true, 0f, 18.8f)
                arcToRelative(9.4f, 9.4f, 0f, false, true, 0f, -18.8f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 6f)
                curveTo(10.23f, 6f, 8.8f, 7.43f, 8.8f, 9.2f)
                curveTo(8.8f, 10.97f, 10.23f, 12.4f, 12f, 12.4f)
                curveTo(13.77f, 12.4f, 15.2f, 10.97f, 15.2f, 9.2f)
                curveTo(15.2f, 7.43f, 13.77f, 6f, 12f, 6f)
                close()
                moveTo(12f, 14.1f)
                curveTo(9.46f, 14.1f, 7.2f, 15.5f, 7.2f, 17.4f)
                lineTo(7.2f, 18.4f)
                curveTo(8.5f, 19.4f, 10.17f, 20f, 12f, 20f)
                curveTo(13.83f, 20f, 15.5f, 19.4f, 16.8f, 18.4f)
                lineTo(16.8f, 17.4f)
                curveTo(16.8f, 15.5f, 14.54f, 14.1f, 12f, 14.1f)
                close()
            }
        }
    }

    val Compose: ImageVector by lazy {
        icon("Kheyr.Compose") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 17.25f)
                lineTo(3f, 21f)
                lineTo(6.75f, 21f)
                lineTo(17.81f, 9.94f)
                lineTo(14.06f, 6.19f)
                lineTo(3f, 17.25f)
                close()
                moveTo(20.71f, 7.04f)
                curveTo(21.1f, 6.65f, 21.1f, 6.02f, 20.71f, 5.63f)
                lineTo(18.37f, 3.29f)
                curveTo(17.98f, 2.9f, 17.35f, 2.9f, 16.96f, 3.29f)
                lineTo(15.13f, 5.12f)
                lineTo(18.88f, 8.87f)
                lineTo(20.71f, 7.04f)
                close()
            }
        }
    }

    val Copy: ImageVector by lazy {
        icon("Kheyr.Copy") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8.5f, 8.5f)
                lineTo(16.5f, 8.5f)
                curveTo(18.4f, 8.5f, 19.5f, 9.6f, 19.5f, 11.5f)
                lineTo(19.5f, 19.5f)
                curveTo(19.5f, 21.4f, 18.4f, 22.5f, 16.5f, 22.5f)
                lineTo(8.5f, 22.5f)
                curveTo(6.6f, 22.5f, 5.5f, 21.4f, 5.5f, 19.5f)
                lineTo(5.5f, 11.5f)
                curveTo(5.5f, 9.6f, 6.6f, 8.5f, 8.5f, 8.5f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8.2f, 5.2f)
                lineTo(15.6f, 5.2f)
                curveTo(17.3f, 5.2f, 18.2f, 6.1f, 18.2f, 7.8f)
                lineTo(18.2f, 9.2f)
            }
        }
    }

    val Archive: ImageVector by lazy {
        icon("Kheyr.Archive") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 3.5f)
                lineTo(20f, 3.5f)
                lineTo(20f, 7f)
                lineTo(4f, 7f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4.9f, 9f)
                lineTo(4.9f, 19f)
                curveTo(4.9f, 20.1f, 5.8f, 21f, 6.9f, 21f)
                lineTo(17.1f, 21f)
                curveTo(18.2f, 21f, 19.1f, 20.1f, 19.1f, 19f)
                lineTo(19.1f, 9f)
                moveTo(4.9f, 11.7f)
                lineTo(19.1f, 11.7f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 13.8f)
                lineTo(13f, 13.8f)
                lineTo(13f, 16.9f)
                lineTo(15.1f, 16.9f)
                lineTo(12f, 20f)
                lineTo(8.9f, 16.9f)
                lineTo(11f, 16.9f)
                close()
            }
        }
    }

    val Mute: ImageVector by lazy {
        icon("Kheyr.Mute") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6.6f, 10.2f)
                curveTo(6.6f, 6.9f, 9f, 4.5f, 12f, 4.5f)
                curveTo(15f, 4.5f, 17.4f, 6.9f, 17.4f, 10.2f)
                lineTo(17.4f, 13.2f)
                lineTo(19.2f, 16.6f)
                lineTo(4.8f, 16.6f)
                lineTo(6.6f, 13.2f)
                close()
                moveTo(10.2f, 19.6f)
                curveTo(10.5f, 20.4f, 11.2f, 20.9f, 12f, 20.9f)
                curveTo(12.8f, 20.9f, 13.5f, 20.4f, 13.8f, 19.6f)
                moveTo(4.2f, 3.8f)
                lineTo(19.8f, 20.2f)
            }
        }
    }

    val Pin: ImageVector by lazy {
        icon("Kheyr.Pin") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(15.1f, 2.6f)
                lineTo(21.4f, 8.9f)
                lineTo(19.6f, 10.7f)
                lineTo(18f, 9.9f)
                lineTo(14.1f, 13.8f)
                lineTo(13.2f, 17.8f)
                lineTo(11.5f, 19.5f)
                lineTo(7.7f, 15.7f)
                lineTo(3.4f, 20f)
                lineTo(4.1f, 19f)
                lineTo(8.3f, 14.3f)
                lineTo(4.5f, 10.5f)
                lineTo(6.2f, 8.8f)
                lineTo(10.2f, 7.9f)
                lineTo(14.1f, 4f)
                lineTo(13.3f, 2.4f)
                close()
            }
        }
    }

    val Sim: ImageVector by lazy {
        icon("Kheyr.Sim") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7.4f, 2.8f)
                lineTo(14.2f, 2.8f)
                lineTo(20.2f, 8.8f)
                lineTo(20.2f, 19f)
                curveTo(20.2f, 20.1f, 19.3f, 21f, 18.2f, 21f)
                lineTo(5.8f, 21f)
                curveTo(4.7f, 21f, 3.8f, 20.1f, 3.8f, 19f)
                lineTo(3.8f, 4.8f)
                curveTo(3.8f, 3.7f, 4.7f, 2.8f, 5.8f, 2.8f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(7.4f, 12f)
                lineTo(9.2f, 12f)
                lineTo(9.2f, 16.8f)
                lineTo(7.4f, 16.8f)
                close()
                moveTo(11.1f, 12f)
                lineTo(12.9f, 12f)
                lineTo(12.9f, 16.8f)
                lineTo(11.1f, 16.8f)
                close()
                moveTo(14.8f, 12f)
                lineTo(16.6f, 12f)
                lineTo(16.6f, 16.8f)
                lineTo(14.8f, 16.8f)
                close()
            }
        }
    }

    val Cloud: ImageVector by lazy {
        icon("Kheyr.Cloud") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.35f, 10.04f)
                curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
                curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
                curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
                curveTo(0f, 17.31f, 2.69f, 20f, 6f, 20f)
                lineTo(19f, 20f)
                curveTo(21.76f, 20f, 24f, 17.76f, 24f, 15f)
                curveTo(24f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
                close()
            }
        }
    }

    val Monitor: ImageVector by lazy {
        icon("Kheyr.Monitor") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4.8f, 3.4f)
                lineTo(19.2f, 3.4f)
                curveTo(20.2f, 3.4f, 21f, 4.2f, 21f, 5.2f)
                lineTo(21f, 15.2f)
                curveTo(21f, 16.2f, 20.2f, 17f, 19.2f, 17f)
                lineTo(4.8f, 17f)
                curveTo(3.8f, 17f, 3f, 16.2f, 3f, 15.2f)
                lineTo(3f, 5.2f)
                curveTo(3f, 4.2f, 3.8f, 3.4f, 4.8f, 3.4f)
                close()
                moveTo(8f, 21f)
                lineTo(16f, 21f)
                moveTo(12f, 17.2f)
                lineTo(12f, 20.8f)
            }
        }
    }

    val Sun: ImageVector by lazy {
        icon("Kheyr.Sun") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(12f, 7.4f)
                curveTo(9.46f, 7.4f, 7.4f, 9.46f, 7.4f, 12f)
                curveTo(7.4f, 14.54f, 9.46f, 16.6f, 12f, 16.6f)
                curveTo(14.54f, 16.6f, 16.6f, 14.54f, 16.6f, 12f)
                curveTo(16.6f, 9.46f, 14.54f, 7.4f, 12f, 7.4f)
                close()
                moveTo(12f, 2.6f)
                lineTo(12f, 4.8f)
                moveTo(12f, 19.2f)
                lineTo(12f, 21.4f)
                moveTo(2.6f, 12f)
                lineTo(4.8f, 12f)
                moveTo(19.2f, 12f)
                lineTo(21.4f, 12f)
                moveTo(5.35f, 5.35f)
                lineTo(6.9f, 6.9f)
                moveTo(17.1f, 17.1f)
                lineTo(18.65f, 18.65f)
                moveTo(18.65f, 5.35f)
                lineTo(17.1f, 6.9f)
                moveTo(6.9f, 17.1f)
                lineTo(5.35f, 18.65f)
            }
        }
    }

    val Moon: ImageVector by lazy {
        icon("Kheyr.Moon") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.6f, 2.4f)
                curveTo(7.1f, 2.9f, 3f, 7.5f, 3f, 13.05f)
                curveTo(3f, 18.9f, 7.75f, 23.6f, 13.6f, 23.6f)
                curveTo(17.45f, 23.6f, 20.8f, 21.55f, 22.55f, 18.5f)
                curveTo(17.2f, 19.7f, 11.55f, 15.55f, 11.55f, 9.95f)
                curveTo(11.55f, 7.3f, 12.8f, 4.9f, 14.75f, 3.35f)
                curveTo(14.05f, 2.9f, 13.35f, 2.55f, 12.6f, 2.4f)
                close()
            }
        }
    }

    val Shield: ImageVector by lazy {
        icon("Kheyr.Shield") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 2.4f)
                lineTo(20f, 5.5f)
                lineTo(20f, 11.5f)
                curveTo(20f, 16.1f, 16.6f, 20.25f, 12f, 21.6f)
                curveTo(7.4f, 20.25f, 4f, 16.1f, 4f, 11.5f)
                lineTo(4f, 5.5f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(10.95f, 15.4f)
                lineTo(7.6f, 12.05f)
                lineTo(9.02f, 10.63f)
                lineTo(10.95f, 12.56f)
                lineTo(15.02f, 8.49f)
                lineTo(16.44f, 9.91f)
                close()
            }
        }
    }

    val Bell: ImageVector by lazy {
        icon("Kheyr.Bell") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2.4f)
                curveTo(9.9f, 2.4f, 8.2f, 4.1f, 8.2f, 6.2f)
                lineTo(8.2f, 9.1f)
                curveTo(8.2f, 10.4f, 7.6f, 11.6f, 6.7f, 12.5f)
                lineTo(5.2f, 14f)
                lineTo(5.2f, 15.8f)
                lineTo(18.8f, 15.8f)
                lineTo(18.8f, 14f)
                lineTo(17.3f, 12.5f)
                curveTo(16.4f, 11.6f, 15.8f, 10.4f, 15.8f, 9.1f)
                lineTo(15.8f, 6.2f)
                curveTo(15.8f, 4.1f, 14.1f, 2.4f, 12f, 2.4f)
                close()
                moveTo(9.9f, 17.6f)
                curveTo(9.9f, 18.8f, 10.8f, 19.7f, 12f, 19.7f)
                curveTo(13.2f, 19.7f, 14.1f, 18.8f, 14.1f, 17.6f)
                close()
            }
        }
    }

    val Help: ImageVector by lazy {
        icon("Kheyr.Help") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
            ) {
                moveTo(12f, 2.6f)
                arcToRelative(9.4f, 9.4f, 0f, false, true, 0f, 18.8f)
                arcToRelative(9.4f, 9.4f, 0f, false, true, 0f, -18.8f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(9.4f, 9.3f)
                curveTo(9.5f, 7.85f, 10.55f, 7f, 12f, 7f)
                curveTo(13.55f, 7f, 14.6f, 8f, 14.6f, 9.35f)
                curveTo(14.6f, 10.4f, 14.05f, 11.05f, 12.9f, 11.75f)
                curveTo(12.25f, 12.15f, 12f, 12.6f, 12f, 13.4f)
                lineTo(12f, 14.2f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 16.4f)
                lineTo(13f, 16.4f)
                lineTo(13f, 18.4f)
                lineTo(11f, 18.4f)
                close()
            }
        }
    }

    val ArrowDown: ImageVector by lazy {
        icon("Kheyr.ArrowDown") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 4.5f)
                lineTo(12f, 19.5f)
                moveTo(5.5f, 13f)
                lineTo(12f, 19.5f)
                lineTo(18.5f, 13f)
            }
        }
    }

    val Send: ImageVector by lazy {
        icon("Kheyr.Send") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(2.7f, 20.7f)
                lineTo(22f, 12f)
                lineTo(2.7f, 3.3f)
                lineTo(2.7f, 10.1f)
                lineTo(16.2f, 12f)
                lineTo(2.7f, 13.9f)
                close()
            }
        }
    }

    val Block: ImageVector by lazy {
        icon("Kheyr.Block") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(12f, 2.8f)
                arcToRelative(9.2f, 9.2f, 0f, false, true, 0f, 18.4f)
                arcToRelative(9.2f, 9.2f, 0f, false, true, 0f, -18.4f)
                close()
                moveTo(5.5f, 5.5f)
                lineTo(18.5f, 18.5f)
            }
        }
    }
}

private fun icon(name: String, builder: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(builder).build()
