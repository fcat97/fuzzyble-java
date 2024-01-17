package media.uqab.fuzzyble.dbCreator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.onClick
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp

private data class Item(val id: Int, val text: String, val list: Int)

@Composable
fun DragAndDropExample() {
    val list1 = remember { mutableStateListOf<Item>() }
    val list2 = remember { mutableStateListOf<Item>() }

    LaunchedEffect(Unit) {
        list1.addAll(generateItems(5, 1))
        list2.addAll(generateItems(3, 2))
    }

    var beingDragged by remember { mutableStateOf<Item?>(null) }
    var draggedOffsetX by remember { mutableStateOf(0f) }
    var draggedOffsetY by remember { mutableStateOf(0f) }

    fun onDragStart(item: Item, x: Float, y: Float) {
        draggedOffsetX = x; draggedOffsetY = y
        beingDragged = item
    }

    fun onDrag(x: Float, y: Float) {
        draggedOffsetX = x; draggedOffsetY = y
    }

    fun onDrop(item: Item, fromList: Int) {
        draggedOffsetX = 0f; draggedOffsetY = 0f; beingDragged = null

        when(fromList) {
            1 -> {
                list1.remove(item)
                list2.add(item)
            }
            2 -> {
                list2.remove(item)
                list1.add(item)
            }
        }
    }

    Box {
        Column {
            LazyHorizontalGrid(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                rows = GridCells.Adaptive(200.dp)
            ) {
                items(items = list1) { item ->
                    DraggableItem(
                        item = item,
                        onDragStart = { it, x, y -> onDragStart(it, x, y) },
                        onDrag = { x, y -> onDrag(x, y) },
                        onDrop = { onDrop(it, 1) }
                    )
                }
            }

            LazyHorizontalGrid(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                rows = GridCells.Fixed(2)
            ) {
                items(list2) { item ->
                    DraggableItem(
                        item = item,
                        onDragStart = { it, x, y -> onDragStart(it, x, y) },
                        onDrag = { x, y -> onDrag(x, y) },
                        onDrop = { onDrop(it, 2) }
                    )
                }
            }
        }

        if (beingDragged != null) {
            DraggingItem(
                beingDragged!!,
                draggedOffsetX,
                draggedOffsetY,
            )
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DraggableItem(
    item: Item,
    modifier: Modifier = Modifier,
    onDragStart: (Item, Float, Float) -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDrop: (Item) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var globalX by remember { mutableStateOf(0f) }
    var globalY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.padding(4.dp)
            .onGloballyPositioned { layoutCoordinates ->
                globalX = layoutCoordinates.positionInRoot().x
                globalY = layoutCoordinates.positionInRoot().y
            }
            .onClick {
                println(item)
            }
            .pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = {
                    println("onDragEnd")
                    isDragging = false
                    offsetX = globalX
                    offsetY = globalY
                    onDrop(item)
                },
                onDragStart = {
                    offsetX = globalX + it.x
                    offsetY = globalY + it.y
                    isDragging = true
                    onDragStart(item, offsetX, offsetY)
                },
                onDrag = { _, dragAmount ->
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                    onDrag(offsetX, offsetY)
                }
            )
        },
        elevation = 4.dp
    ) {
        Text(
            text = item.text,
            modifier = Modifier
                .background(MaterialTheme.colors.primary)
                .padding(16.dp)
        )
    }
}

@Composable
private fun DraggingItem(
    item: Item,
    offsetX: Float,
    offsetY: Float,
) {
    DraggableItem(
        item,
        modifier = Modifier.graphicsLayer(
            translationX = offsetX,
            translationY = offsetY,
            alpha = 0.5f
        ),
        onDrop = {},
        onDragStart = {_, _, _ -> },
        onDrag = { _, _ -> },
    )
}

private fun generateItems(count: Int, forList: Int): List<Item> {
    return List(count) { index ->
        Item(id = index, text = "Item $index", list = forList)
    }
}