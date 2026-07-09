# Verification

Pending for reopened scope.

Planned commands:
- `rtk env ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk ./gradlew :app:testDebugUnitTest --tests com.opus.readerparser.ui.browse.BrowseViewModelTest --tests com.opus.readerparser.sources.freewebnovel.FreeWebNovelTest --tests com.opus.readerparser.sources.asurascans.AsuraScansTest --console=plain`
- `rtk env ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk ./gradlew :app:testDebugUnitTest --console=plain`
- `rtk env ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk ./gradlew :app:assembleDebug --console=plain` if a device APK is needed.
