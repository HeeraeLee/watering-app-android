# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.watering.app.**$$serializer { *; }
-keepclassmembers class com.watering.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.watering.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt
-keepclasseswithmembernames class * { @dagger.hilt.* <fields>; }

# Glance
-keep class androidx.glance.** { *; }
