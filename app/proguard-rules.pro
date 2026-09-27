# Keep all classes and members in app package to prevent JSON serialization/deserialization issues or enum valueOf failures in obfuscated release builds
-keep class com.madeby.JAI.** { *; }
-keepclassmembers class com.madeby.JAI.** { *; }
-keepclassmembers enum com.madeby.JAI.** { *; }

# Strip debug and verbose log calls in release builds to avoid information disclosure
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}


