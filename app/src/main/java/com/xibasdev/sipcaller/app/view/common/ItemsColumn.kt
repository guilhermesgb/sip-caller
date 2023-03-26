package com.xibasdev.sipcaller.app.view.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun <T : Any> ItemsColumn(
    columnName: String,
    sizeProvider: () -> Int,
    itemKeyProvider: (itemIndex: Int) -> T,
    modifier: Modifier,
    content: @Composable (
        itemIndex: Int,
        visibleProvider: () -> Boolean,
        animateDeleteItem: (onDeleteItem: () -> Unit) -> Unit,
        itemModifier: Modifier
    ) -> Unit
) {

    val lazyColumnState = rememberLazyListState()
    val visibleItemsState = remember { mutableStateMapOf<T, Boolean>() }
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        state = lazyColumnState,
        contentPadding = PaddingValues(4.dp),
        modifier = modifier
    ) {

        stickyHeader {
            val size = sizeProvider()

            Text(
                text = columnName + (if (size == 0) " (empty)" else ""),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color = MaterialTheme.colorScheme.secondary)
            )

            Spacer(modifier = Modifier.padding(bottom = 4.dp))

            for (itemIndex in 0 until size) {
                val itemKey = itemKeyProvider(itemIndex)

                if (!visibleItemsState.contains(itemKey)) {
                    visibleItemsState[itemKey] = false
                }
            }
            LaunchedEffect(size, coroutineScope) {
                for (itemIndex in 0 until size) {
                    val itemKey = itemKeyProvider(itemIndex)

                    if (visibleItemsState[itemKey] == false) {
                        visibleItemsState[itemKey] = true
                    }
                }
            }
        }

        items(
            count = sizeProvider(),
            key = itemKeyProvider
        ) { itemIndex ->

            content(
                itemIndex = itemIndex,
                visibleProvider = {
                    visibleItemsState.getOrDefault(itemKeyProvider(itemIndex), false)
                },
                animateDeleteItem = { onDeleteItem ->

                    coroutineScope.launch {
                        visibleItemsState[itemKeyProvider(itemIndex)] = false

                        delay(300)

                        onDeleteItem()
                    }
                },
                itemModifier = Modifier.animateItemPlacement()
            )

            Spacer(modifier =  Modifier.padding(bottom = 4.dp))
        }
    }
}
