# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep application classes
-keep class cn.youth996.ui.** { *; }

# Keep model classes
-keep class cn.youth996.ui.model.** { *; }

# Keep adapter classes
-keep class cn.youth996.ui.adapter.** { *; }

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
