# AR Photogrammetry Android App

## Backend Configuration

Edit `network/ApiConfig.kt` and set `BASE_URL`:

- **Android Emulator**: `http://10.0.2.2:5000`
- **Physical Device (same network)**: `http://YOUR_LOCAL_IP:5000`
  - Find your local IP: `ipconfig` (Windows) or `ifconfig` (Mac/Linux)
- **Production**: `https://api.yourapp.com`

## Testing End-to-End

1. Start backend server on your machine: `python api/app.py`
2. Set `BASE_URL` in `ApiConfig.kt` appropriately
3. Run Android app on device/emulator
4. Select asset category
5. Start capture and move slowly around the asset
6. Stop capture
7. Tap "Upload to Backend"
8. View measurement results

## Device Requirements
- ARCore-compatible Android device
- ARCore Depth API support (check: https://developers.google.com/ar/devices)
- Android 7.0 (API 24) or higher
- Recommended devices: Google Pixel 4+, Samsung Galaxy S20+, OnePlus 8 Pro

## Setup
1. Install Android Studio
2. Open project
3. Sync Gradle
4. Connect ARCore-compatible device
5. Run app
