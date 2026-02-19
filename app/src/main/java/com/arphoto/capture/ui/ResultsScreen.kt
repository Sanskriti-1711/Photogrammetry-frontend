package com.arphoto.capture.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arphoto.capture.data.MeasurementResult

@Composable
fun ResultsScreen(
    result: MeasurementResult,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Measurement Results",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Category
        ResultCard(
            title = "Asset Category",
            value = result.category.uppercase()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Measurements based on category
        when (result.category.lowercase()) {
            "trench" -> {
                ResultCard(
                    title = "Depth",
                    value = "${result.depthM?.let { "%.2f m".format(it) } ?: "N/A"}"
                )
                Spacer(modifier = Modifier.height(8.dp))
                ResultCard(
                    title = "Width",
                    value = "${result.widthM?.let { "%.2f m".format(it) } ?: "N/A"}"
                )
            }
            "manhole" -> {
                ResultCard(
                    title = "Diameter",
                    value = "${result.diameterM?.let { "%.2f m".format(it) } ?: "N/A"}"
                )
                Spacer(modifier = Modifier.height(8.dp))
                ResultCard(
                    title = "Depth",
                    value = "${result.depthM?.let { "%.2f m".format(it) } ?: "N/A"}"
                )
            }
            "duct" -> {
                ResultCard(
                    title = "Diameter",
                    value = "${result.diameterM?.let { "%.2f m".format(it) } ?: "N/A"}"
                )
                Spacer(modifier = Modifier.height(8.dp))
                ResultCard(
                    title = "Length",
                    value = "${result.lengthM?.let { "%.2f m".format(it) } ?: "N/A"}"
                )
            }
            "handhole" -> {
                ResultCard(
                    title = "Length",
                    value = "${result.lM?.let { "%.2f m".format(it) } ?: "N/A"}"
                )
                Spacer(modifier = Modifier.height(8.dp))
                ResultCard(
                    title = "Width",
                    value = "${result.wM?.let { "%.2f m".format(it) } ?: "N/A"}"
                )
                Spacer(modifier = Modifier.height(8.dp))
                ResultCard(
                    title = "Height",
                    value = "${result.hM?.let { "%.2f m".format(it) } ?: "N/A"}"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Confidence
        ResultCard(
            title = "Confidence",
            value = result.confidence.uppercase(),
            isHighlight = result.confidence == "high"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Point count
        ResultCard(
            title = "Point Count",
            value = result.pointCount.toString()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Back button
        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Capture Another",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun ResultCard(
    title: String,
    value: String,
    isHighlight: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
