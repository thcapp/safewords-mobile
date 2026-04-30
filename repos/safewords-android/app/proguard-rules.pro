# Safewords ProGuard Rules

# Keep Gson serialized classes
-keepclassmembers class com.thc.safewords.model.** { *; }
-keepclassmembers class com.thc.safewords.service.QRCodeService$QRPayload { *; }

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ML Kit
-keep class com.google.mlkit.** { *; }

# ZXing
-keep class com.google.zxing.** { *; }

# Firebase telemetry transport — excluded at the configurations level because
# we never use it. R8 sees dangling references from MLKit's optional telemetry
# path; tell R8 to ignore them. At runtime MLKit's bundled-model barcode
# scanner never touches these classes.
-dontwarn com.google.android.datatransport.**
-dontwarn com.google.firebase.encoders.**
-dontwarn com.google.firebase.components.**
