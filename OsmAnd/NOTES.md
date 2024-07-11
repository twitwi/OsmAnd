

~~~
  582  ../gradlew --continue --no-parallel -Dorg.gradle.jvmargs=-Xmx8192m assembleNightlyFreeLegacyArm64Debug 
  567  ../gradlew --continue --no-parallel -Dorg.gradle.jvmargs=-Xmx8192m :OsmAnd:mergeNightlyFreeLegacyArm64DebugResources
  568  ../gradlew --continue --no-parallel -Dorg.gradle.jvmargs=-Xmx8192m assembleNightlyFreeLegacyArm64Debug 
  588  lla ./build/outputs/apk/nightlyFreeLegacyArm64/debug/OsmAnd-nightlyFree-legacy-arm64-debug.apk
  570  adb install ./build/outputs/apk/nightlyFreeLegacyArm64/debug/OsmAnd-nightlyFree-legacy-arm64-debug.apk

~~~
