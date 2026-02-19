package com.arphoto.capture.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arphoto.capture.data.AssetCategory

@Composable
fun CategorySelector(
    selectedCategory: AssetCategory,
    onCategorySelected: (AssetCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Asset Category",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            AssetCategory.values().forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = category == selectedCategory,
                            onClick = { onCategorySelected(category) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = category == selectedCategory,
                        onClick = { onCategorySelected(category) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = category.getDisplayName(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
