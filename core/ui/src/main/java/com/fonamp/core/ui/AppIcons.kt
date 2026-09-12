/*
 * Copyright 2026 The Fonamp Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Icon vectors below are verbatim copies of the Material filled icons from
 * androidx.compose.material:material-icons-extended 1.7.6 (Apache 2.0, AOSP),
 * re-homed here so the app no longer depends on the ~13.5 MB extended
 * artifact. Builder defaults replicate AOSP's internal materialIcon /
 * materialPath helpers exactly (24dp, 24x24 viewport, black fill,
 * NonZero winding), so each glyph renders pixel-identical to the
 * Icons.Filled.* original. No core-icon substitution was made: none of the
 * six has a visually identical counterpart in material-icons-core.
 */

package com.fonamp.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Project-owned copies of the six `Icons.Filled.*` glyphs that only exist in
 * `material-icons-extended` (verified absent from the `material-icons-core`
 * 1.7.6 API: no `AlbumKt`, `MusicNoteKt`, `PauseKt`, `PublicKt`, `RadioKt`
 * or `TagKt`). Path commands are copied verbatim from the AOSP 1.7.6
 * extended sources; only the internal `materialIcon`/`materialPath`
 * builders are re-implemented below with identical parameters.
 *
 * Everything else in the app keeps importing `Icons.Filled.*` from
 * `material-icons-core` (ArrowBack, Close, Delete, Favorite, FavoriteBorder,
 * Person, Place, PlayArrow, Refresh, Search, Settings).
 */
object AppIcons {

    /**
     * Copy of `Icons.Filled.MusicNote` (extended-only). Generic-note fallback
     * for artwork slots; no core note glyph exists, so copied, not replaced.
     */
    val MusicNote: ImageVector by lazy {
        appMaterialIcon(name = "Filled.MusicNote") {
            appMaterialPath {
                moveTo(12.0f, 3.0f)
                verticalLineToRelative(10.55f)
                curveToRelative(-0.59f, -0.34f, -1.27f, -0.55f, -2.0f, -0.55f)
                curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f)
                reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
                reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f)
                verticalLineTo(7.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(3.0f)
                horizontalLineToRelative(-6.0f)
                close()
            }
        }
    }

    /**
     * Copy of `Icons.Filled.Pause` (extended-only). Transport control that
     * must pair glyph-for-glyph with core `PlayArrow`; copied, not replaced.
     */
    val Pause: ImageVector by lazy {
        appMaterialIcon(name = "Filled.Pause") {
            appMaterialPath {
                moveTo(6.0f, 19.0f)
                horizontalLineToRelative(4.0f)
                lineTo(10.0f, 5.0f)
                lineTo(6.0f, 5.0f)
                verticalLineToRelative(14.0f)
                close()
                moveTo(14.0f, 5.0f)
                verticalLineToRelative(14.0f)
                horizontalLineToRelative(4.0f)
                lineTo(18.0f, 5.0f)
                horizontalLineToRelative(-4.0f)
                close()
            }
        }
    }

    /**
     * Copy of `Icons.Filled.Album` (extended-only). Album-segment fallback;
     * core has no album glyph, so copied, not replaced.
     */
    val Album: ImageVector by lazy {
        appMaterialIcon(name = "Filled.Album") {
            appMaterialPath {
                moveTo(12.0f, 2.0f)
                curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
                reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
                close()
                moveTo(12.0f, 16.5f)
                curveToRelative(-2.49f, 0.0f, -4.5f, -2.01f, -4.5f, -4.5f)
                reflectiveCurveTo(9.51f, 7.5f, 12.0f, 7.5f)
                reflectiveCurveToRelative(4.5f, 2.01f, 4.5f, 4.5f)
                reflectiveCurveToRelative(-2.01f, 4.5f, -4.5f, 4.5f)
                close()
                moveTo(12.0f, 11.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                close()
            }
        }
    }

    /**
     * Copy of `Icons.Filled.Radio` (extended-only, receiver glyph). Bottom-tab
     * and station-row affordance; core has no radio glyph, so copied.
     */
    val Radio: ImageVector by lazy {
        appMaterialIcon(name = "Filled.Radio") {
            appMaterialPath {
                moveTo(3.24f, 6.15f)
                curveTo(2.51f, 6.43f, 2.0f, 7.17f, 2.0f, 8.0f)
                verticalLineToRelative(12.0f)
                curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(16.0f)
                curveToRelative(1.11f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                lineTo(22.0f, 8.0f)
                curveToRelative(0.0f, -1.11f, -0.89f, -2.0f, -2.0f, -2.0f)
                lineTo(8.3f, 6.0f)
                lineToRelative(8.26f, -3.34f)
                lineTo(15.88f, 1.0f)
                lineTo(3.24f, 6.15f)
                close()
                moveTo(7.0f, 20.0f)
                curveToRelative(-1.66f, 0.0f, -3.0f, -1.34f, -3.0f, -3.0f)
                reflectiveCurveToRelative(1.34f, -3.0f, 3.0f, -3.0f)
                reflectiveCurveToRelative(3.0f, 1.34f, 3.0f, 3.0f)
                reflectiveCurveToRelative(-1.34f, 3.0f, -3.0f, 3.0f)
                close()
                moveTo(20.0f, 12.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(2.0f)
                lineTo(4.0f, 12.0f)
                lineTo(4.0f, 8.0f)
                horizontalLineToRelative(16.0f)
                verticalLineToRelative(4.0f)
                close()
            }
        }
    }

    /**
     * Copy of `Icons.Filled.Public` (extended-only, 1.7.6 globe-with-person
     * glyph). Countries-tab affordance; no core equivalent, so copied.
     */
    val Public: ImageVector by lazy {
        appMaterialIcon(name = "Filled.Public") {
            appMaterialPath {
                moveTo(12.0f, 2.0f)
                curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
                reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
                close()
                moveTo(11.0f, 19.93f)
                curveToRelative(-3.95f, -0.49f, -7.0f, -3.85f, -7.0f, -7.93f)
                curveToRelative(0.0f, -0.62f, 0.08f, -1.21f, 0.21f, -1.79f)
                lineTo(9.0f, 15.0f)
                verticalLineToRelative(1.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                verticalLineToRelative(1.93f)
                close()
                moveTo(17.9f, 17.39f)
                curveToRelative(-0.26f, -0.81f, -1.0f, -1.39f, -1.9f, -1.39f)
                horizontalLineToRelative(-1.0f)
                verticalLineToRelative(-3.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                lineTo(8.0f, 12.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                lineTo(11.0f, 7.0f)
                horizontalLineToRelative(2.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineToRelative(-0.41f)
                curveToRelative(2.93f, 1.19f, 5.0f, 4.06f, 5.0f, 7.41f)
                curveToRelative(0.0f, 2.08f, -0.8f, 3.97f, -2.1f, 5.39f)
                close()
            }
        }
    }

    /**
     * Copy of `Icons.Filled.Tag` (extended-only, hashtag-grid glyph — not the
     * price-label). Genres/tags-tab affordance; no core equivalent, so copied.
     */
    val Tag: ImageVector by lazy {
        appMaterialIcon(name = "Filled.Tag") {
            appMaterialPath {
                moveTo(20.0f, 10.0f)
                lineTo(20.0f, 8.0f)
                horizontalLineToRelative(-4.0f)
                lineTo(16.0f, 4.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(4.0f)
                horizontalLineToRelative(-4.0f)
                lineTo(10.0f, 4.0f)
                lineTo(8.0f, 4.0f)
                verticalLineToRelative(4.0f)
                lineTo(4.0f, 8.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(4.0f)
                lineTo(4.0f, 14.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(4.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(4.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(4.0f)
                close()
                moveTo(14.0f, 14.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(4.0f)
                close()
            }
        }
    }
}

/**
 * Local replica of AOSP's internal `materialIcon` helper (same 24dp
 * dimensions, 24x24 viewport, no auto-mirror).
 */
private inline fun appMaterialIcon(
    name: String,
    block: ImageVector.Builder.() -> ImageVector.Builder,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
    autoMirror = false,
).block().build()

/**
 * Local replica of AOSP's internal `ImageVector.Builder.materialPath`
 * (black fill, full alpha, butt/bevel stroke params, NonZero winding).
 */
private inline fun ImageVector.Builder.appMaterialPath(
    pathBuilder: PathBuilder.() -> Unit,
) = path(
    fill = SolidColor(Color.Black),
    fillAlpha = 1f,
    stroke = null,
    strokeAlpha = 1f,
    strokeLineWidth = 1f,
    strokeLineCap = StrokeCap.Butt,
    strokeLineJoin = StrokeJoin.Bevel,
    strokeLineMiter = 1f,
    pathFillType = DefaultFillType,
    pathBuilder = pathBuilder,
)
