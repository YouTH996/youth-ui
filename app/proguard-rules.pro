# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep application classes
-keep class com.youth.desktop.** { *; }

# Keep model classes
-keep class com.youth.desktop.model.** { *; }

# Keep adapter classes
-keep class com.youth.desktop.adapter.** { *; }

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
