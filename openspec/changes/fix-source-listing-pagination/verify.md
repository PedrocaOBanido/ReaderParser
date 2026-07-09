# Verification

| Command | Result |
| --- | --- |
| `rtk gradlew :app:compileDebugAndroidTestKotlin` | Passed |
| `rtk gradlew :app:testDebugUnitTest --tests "*BrowseViewModelTest"` | Passed |
| Connected adb `BrowseContentTest` | Passed (6 tests OK) |
| `rtk gradlew :app:testDebugUnitTest --tests "*FreeWebNovelTest"` | Passed |
| `rtk gradlew :app:testDebugUnitTest --tests "*AsuraScansTest"` | Passed |
| `ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk rtk gradlew :app:testDebugUnitTest --console=plain` | BUILD SUCCESSFUL in 15s |
| `ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk rtk gradlew :app:assembleDebug --console=plain` | BUILD SUCCESSFUL in 33s |
