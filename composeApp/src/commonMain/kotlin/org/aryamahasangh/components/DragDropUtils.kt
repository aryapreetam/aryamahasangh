package org.aryamahasangh.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Enhanced Drag and Drop State with smooth animations and visual feedback
 */
@Stable
class DragDropState(
  private val onMove: (fromIndex: Int, toIndex: Int) -> Unit
) {
  var draggedIndex by mutableStateOf<Int?>(null)
    private set

  var dragOffset by mutableStateOf(Offset.Zero)
    private set

  var isDragging by mutableStateOf(false)
    private set

  // Track the item being dragged over for visual feedback
  var hoveredIndex by mutableStateOf<Int?>(null)
    private set

  // Store item positions and heights for better hover detection
  private val itemPositions = mutableMapOf<Int, Rect>()
  private val itemHeights = mutableMapOf<Int, Float>()

  // Simplified offset tracking for smooth animations
  private val itemOffsets = mutableMapOf<Int, Float>()

  fun updateItemPosition(index: Int, position: Rect) {
    itemPositions[index] = position
    itemHeights[index] = position.height
  }

  fun onDragStart(index: Int) {
    draggedIndex = index
    isDragging = true
    dragOffset = Offset.Zero
    hoveredIndex = null

    // Initialize offsets for all items
    itemPositions.keys.forEach { itemIndex ->
      itemOffsets[itemIndex] = 0f
    }
  }

  fun onDragEnd() {
    // Perform the move operation if hovering over a different item
    val draggedIdx = draggedIndex
    val hoveredIdx = hoveredIndex

    if (draggedIdx != null && hoveredIdx != null && draggedIdx != hoveredIdx) {
      onMove(draggedIdx, hoveredIdx)
    }

    // Reset all state
    draggedIndex = null
    hoveredIndex = null
    dragOffset = Offset.Zero
    isDragging = false
    itemOffsets.clear()
  }

  fun onDrag(offset: Offset) {
    dragOffset += offset

    // Update hover based on current drag position
    val draggedIdx = draggedIndex
    if (draggedIdx != null && isDragging) {
      val draggedItemPosition = itemPositions[draggedIdx]
      if (draggedItemPosition != null) {
        val currentDragPosition = Offset(
          draggedItemPosition.center.x + dragOffset.x,
          draggedItemPosition.center.y + dragOffset.y
        )

        // Find which item we're hovering over
        var newHoveredIndex: Int? = null
        for ((index, rect) in itemPositions) {
          if (index != draggedIdx && rect.contains(currentDragPosition)) {
            newHoveredIndex = index
            break
          }
        }

        // Update hover and animate items if changed
        if (newHoveredIndex != hoveredIndex) {
          updateItemSliding(draggedIdx, hoveredIndex, newHoveredIndex)
          hoveredIndex = newHoveredIndex
        }
      }
    }
  }

  private fun updateItemSliding(
    draggedIndex: Int,
    previousHovered: Int?,
    newHovered: Int?
  ) {
    val draggedHeight = itemHeights[draggedIndex] ?: 0f
    val spacing = 8f // 8dp spacing

    // Reset all offsets first
    itemOffsets.replaceAll { _, _ -> 0f }

    // Apply new offset to hovered item
    newHovered?.let { newIndex ->
      if (draggedIndex < newIndex) {
        // Dragging down: move hovered item up
        itemOffsets[newIndex] = -(draggedHeight + spacing)
      } else {
        // Dragging up: move hovered item down
        itemOffsets[newIndex] = draggedHeight + spacing
      }
    }
  }

  fun getItemOffset(index: Int): Float {
    return itemOffsets[index] ?: 0f
  }

  fun onHover(index: Int) {
    if (isDragging && draggedIndex != index) {
      hoveredIndex = index
    }
  }

  fun clearHover() {
    hoveredIndex = null
  }
}

/**
 * Create a rememberable DragDropState
 */
@Composable
fun rememberDragDropState(
  onMove: (fromIndex: Int, toIndex: Int) -> Unit
): DragDropState {
  return remember { DragDropState(onMove) }
}

/**
 * Enhanced draggable item modifier with beautiful visual feedback
 */
@Composable
fun Modifier.draggableItem(
  state: DragDropState,
  index: Int,
  enabled: Boolean = true
): Modifier {
  val haptic = LocalHapticFeedback.current

  // Animate the offset smoothly
  val animatedOffset by animateFloatAsState(
    targetValue = state.getItemOffset(index),
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "item_offset_$index"
  )

  return this.then(
    if (enabled) {
      Modifier
        .onGloballyPositioned { coordinates ->
          // Update item position for hover detection
          val position = Rect(
            coordinates.positionInRoot(),
            coordinates.size.toSize()
          )
          state.updateItemPosition(index, position)
        }
        .offset { IntOffset(0, animatedOffset.roundToInt()) }
        .zIndex(if (state.draggedIndex == index) 1f else 0f)
        .graphicsLayer {
          if (state.draggedIndex == index) {
            // Dragged item styling
            translationX = state.dragOffset.x
            translationY = state.dragOffset.y
            scaleX = 1.03f
            scaleY = 1.03f
            alpha = 0.95f

            // Add subtle rotation for natural feel
            rotationZ = (state.dragOffset.x * 0.005f).coerceIn(-1f, 1f)
          } else if (state.hoveredIndex == index && state.isDragging) {
            // Hovered item subtle feedback
            scaleX = 0.98f
            scaleY = 0.98f
            alpha = 0.8f
          }
        }
        .shadow(
          elevation = if (state.draggedIndex == index) 8.dp else 0.dp,
          shape = RoundedCornerShape(8.dp),
          clip = false
        )
        .pointerInput(index) {
          detectDragGestures(
            onDragStart = { _ ->
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              state.onDragStart(index)
            },
            onDragEnd = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              state.onDragEnd()
            },
            onDrag = { _, offset ->
              state.onDrag(offset)
            }
          )
        }
    } else {
      Modifier
    }
  )
}

/**
 * Drop target modifier for detecting when items are dropped
 * Enhanced for smooth Column layout animations
 */
@Composable
fun Modifier.dropTarget(
  state: DragDropState,
  index: Int,
  itemHeight: Dp = 120.dp
): Modifier {
  // This is now handled in draggableItem modifier
  return this
}

/**
 * Hover target modifier optimized for Column layout
 */
@Composable
fun Modifier.hoverTarget(
  state: DragDropState,
  index: Int
): Modifier {
  return this.then(
    Modifier.onGloballyPositioned { coordinates ->
      // Update item position for hover detection
      if (!state.isDragging) {
        val position = Rect(
          coordinates.positionInRoot(),
          coordinates.size.toSize()
        )
        state.updateItemPosition(index, position)
      }
    }
  )
}
