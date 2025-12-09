package com.example.h2now

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.h2now.ui.theme.H2nowTheme

@Composable
fun TipsScreen() {
    val tipsViewModel: TipsViewModel = viewModel(
        factory = TipsViewModelFactory(TipsRepository())
    )
    val uiState by tipsViewModel.uiState.collectAsState()

    TipsContent(uiState = uiState)
}

@Composable
fun TipsContent(uiState: TipsUiState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (uiState) {
            is TipsUiState.Loading -> CircularProgressIndicator()
            is TipsUiState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(uiState.tips) { tip ->
                        TipCard(tip = tip, modifier = Modifier.padding(8.dp))
                    }
                }
            }
            is TipsUiState.Error -> Text(text = uiState.message, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
fun TipCard(tip: Tip, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
    ) {
        Column {
            AsyncImage(
                model = tip.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = tip.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = tip.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun SuccessStatePreviewContent() {
    val mockTips = listOf(
        Tip(1, "Start Your Day With Water", "Drinking a glass of water first thing in the morning helps kickstart your metabolism.", "https://raw.githubusercontent.com/cheryl7114/mobile_CA3/main/data/tips1.png"),
        Tip(2, "Carry a Water Bottle", "Keep a reusable water bottle with you to make hydration convenient.", "https://raw.githubusercontent.com/cheryl7114/mobile_CA3/main/data/tips2.png"),
        Tip(3, "Set Hydration Reminders", "Gentle reminders help you stay consistent with your hydration goals.", "https://raw.githubusercontent.com/cheryl7114/mobile_CA3/main/data/tips3.png"),
        Tip(4, "Eat Water‑Rich Foods", "Fruits like watermelon, oranges, and strawberries naturally boost hydration.", "https://raw.githubusercontent.com/cheryl7114/mobile_CA3/main/data/tips4.png")
    )
    H2nowTheme {
        TipsContent(uiState = TipsUiState.Success(mockTips))
    }
}

@Preview(name = "Phone", group = "Screen Sizes", showBackground = true, device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun TipsScreenPhonePreview() {
    SuccessStatePreviewContent()
}

@Preview(name = "Foldable (Unfolded)", group = "Screen Sizes", showBackground = true, device = "spec:width=673dp,height=841dp,dpi=480")
@Composable
fun TipsScreenFoldablePreview() {
    SuccessStatePreviewContent()
}

@Preview(name = "Tablet", group = "Screen Sizes", showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=480")
@Composable
fun TipsScreenTabletPreview() {
    SuccessStatePreviewContent()
}
