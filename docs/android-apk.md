# Installing CI-built Android APKs

Every push, pull request, and manual workflow dispatch runs the **Android APK** GitHub Actions workflow.

## Download the APK

1. Open the repository on GitHub.
2. Go to **Actions**.
3. Open the latest **Android APK** workflow run for the commit you want.
4. In **Artifacts**, download `kheyr-debug-apk-<commit-sha>`.
5. Unzip the artifact and install `app-debug.apk` on your Android phone.

## Phone install notes

- This is a debug APK, so Android may ask you to allow installing apps from your browser or file manager.
- SMS features require granting SMS permissions and making Kheyr the default SMS app.
- Debug APKs are intended for testing only and should not be uploaded to app stores.
