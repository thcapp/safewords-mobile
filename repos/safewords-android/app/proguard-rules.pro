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
