# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 1. Protect Chaquopy's native C/Python binding infrastructure
-keep class com.chaquo.python.** { *; }
-keep class com.chaquo.python.android.** { *; }

# 2. Protect your Firebase Queue data model from being renamed or stripped
-keepclassmembers class com.example.applicaion.QueueItem {
    *** getId();
    *** getTitle();
    *** getArtist();
    *** getStreamUrl();
    void setId(***);
    void setTitle(***);
    void setArtist(***);
    void setStreamUrl(***);
}
-keep class com.example.applicaion.QueueItem { *; }