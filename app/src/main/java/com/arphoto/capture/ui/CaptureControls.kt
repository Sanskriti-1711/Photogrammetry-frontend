package com.arphoto.capture.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CaptureControls(
    isCapturing: Boolean,
    frameCount: Int,
    trackingState: String,
    onToggleCapture: () -> Unit,
    onProcess: () -> Unit,
    onClassify: () -> Unit,
    onReconstruct: () -> Unit,
    onPoseSanity: () -> Unit,
    showActionButtons: Boolean,
    isBusy: Boolean,
    busyOperation: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Frame count
            Text(
                text = "Frames Captured: $frameCount",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Tracking state
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (trackingState == "TRACKING")
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Tracking: $trackingState",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Capture button
            Button(
                onClick = onToggleCapture,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCapturing)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isCapturing) "Stop Capture" else "Start Capture",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (isBusy && !busyOperation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "$busyOperation in progress...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (showActionButtons) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onProcess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = "Process + Measure",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onClassify,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isBusy
                ) {
                    Text(text = "Classify")
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onReconstruct,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isBusy
                ) {
                    Text(text = "Reconstruct")
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onPoseSanity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isBusy
                ) {
                    Text(text = "Pose Sanity")
                }
            }
        }
    }
}
