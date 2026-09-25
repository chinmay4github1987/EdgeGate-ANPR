# LiteRT / TFLite uses JNI and reflection for delegates.
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
# ML Kit ships its own consumer rules; nothing extra needed.
