package com.harmonixia.android.ui.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems

private val AlphabetItemSize = 24.dp
private val AlphabetScrollerWidth = 16.dp
private val AlphabetCharList = ('A'..'Z').toList()
private val AlphabetCharSet = AlphabetCharList.toSet()

@Composable
fun AlphabetFastScroller(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onLetterChange: (Char) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val alphabetHeightInPixels = with(LocalDensity.current) { AlphabetItemSize.toPx() }
    var alphabetRelativeDragYOffset by remember { mutableStateOf<Float?>(null) }
    var alphabetDistanceFromTopOfScreen by remember { mutableStateOf(0F) }
    var lastLetter by remember { mutableStateOf<Char?>(null) }

    BoxWithConstraints(modifier = modifier) {
        content()

        if (!isEnabled) return@BoxWithConstraints

        AlphabetScroller(
            modifier = Modifier.align(Alignment.CenterEnd),
            onAlphabetListDrag = { relativeDragYOffset, containerDistance ->
                alphabetRelativeDragYOffset = relativeDragYOffset
                alphabetDistanceFromTopOfScreen = containerDistance
                if (relativeDragYOffset == null) {
                    lastLetter = null
                    return@AlphabetScroller
                }
                val letter = relativeDragYOffset.getIndexOfCharBasedOnYPosition(
                    alphabetHeightInPixels = alphabetHeightInPixels
                )
                if (letter != lastLetter) {
                    lastLetter = letter
                    onLetterChange(letter)
                }
            }
        )

        val yOffset = alphabetRelativeDragYOffset
        if (yOffset != null) {
            AlphabetBubble(
                boxConstraintMaxWidth = this.maxWidth,
                bubbleOffsetYFloat = yOffset + alphabetDistanceFromTopOfScreen,
                currAlphabetScrolledOn = yOffset.getIndexOfCharBasedOnYPosition(
                    alphabetHeightInPixels = alphabetHeightInPixels
                )
            )
        }
    }
}

@Composable
private fun AlphabetScroller(
    modifier: Modifier = Modifier,
    onAlphabetListDrag: (relativeDragYOffset: Float?, distanceFromTopOfScreen: Float) -> Unit
) {
    var distanceFromTopOfScreen by remember { mutableStateOf(0F) }

    Column(
        modifier = modifier
            .width(AlphabetScrollerWidth)
            .onGloballyPositioned {
                distanceFromTopOfScreen = it.positionInRoot().y
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        onAlphabetListDrag(offset.y, distanceFromTopOfScreen)
                    },
                    onDragEnd = {
                        onAlphabetListDrag(null, distanceFromTopOfScreen)
                    },
                    onDragCancel = {
                        onAlphabetListDrag(null, distanceFromTopOfScreen)
                    }
                ) { change, _ ->
                    onAlphabetListDrag(change.position.y, distanceFromTopOfScreen)
                }
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (letter in AlphabetCharList) {
            Text(
                modifier = Modifier.height(AlphabetItemSize),
                text = letter.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlphabetBubble(
    boxConstraintMaxWidth: Dp,
    bubbleOffsetYFloat: Float,
    currAlphabetScrolledOn: Char,
    modifier: Modifier = Modifier
) {
    val bubbleSize = 96.dp
    Surface(
        shape = CircleShape,
        modifier = modifier
            .size(bubbleSize)
            .offset(
                x = boxConstraintMaxWidth - (bubbleSize + AlphabetItemSize),
                y = with(LocalDensity.current) {
                    bubbleOffsetYFloat.toDp() - (bubbleSize / 2)
                }
            ),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(
            modifier = Modifier.size(bubbleSize),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currAlphabetScrolledOn.toString(),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

internal fun Float.getIndexOfCharBasedOnYPosition(
    alphabetHeightInPixels: Float
): Char {
    var index = (this / alphabetHeightInPixels).toInt()
    index = index.coerceIn(0, AlphabetCharList.lastIndex)
    return AlphabetCharList[index]
}

internal fun <T : Any> LazyPagingItems<T>.alphabetIndexMap(
    labelSelector: (T) -> String
): Map<Char, Int> {
    val snapshot = itemSnapshotList
    if (snapshot.items.isEmpty()) return emptyMap()
    val firstLetterIndexes = mutableMapOf<Char, Int>()
    val offset = snapshot.placeholdersBefore
    snapshot.items.forEachIndexed { index, item ->
        val label = labelSelector(item).trim()
        if (label.isEmpty()) return@forEachIndexed
        val firstChar = label.first().uppercaseChar()
        if (firstChar !in AlphabetCharSet) return@forEachIndexed
        if (!firstLetterIndexes.containsKey(firstChar)) {
            firstLetterIndexes[firstChar] = offset + index
        }
    }
    return firstLetterIndexes
}
