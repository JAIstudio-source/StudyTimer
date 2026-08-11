# Keep all classes and members in app package to prevent JSON serialization/deserialization issues or enum valueOf failures in obfuscated release builds
-keep class com.madeby.JAI.** { *; }
-keepclassmembers class com.madeby.JAI.** { *; }
-keepclassmembers enum com.madeby.JAI.** { *; }

