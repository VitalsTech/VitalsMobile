# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep DTOs for kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclasseswithmembers class com.vitals.mobile.**.*Dto {
    *;
}
-keep,includedescriptorclasses class com.vitals.mobile.**$$serializer { *; }
-keepclassmembers class com.vitals.mobile.** {
    *** Companion;
}
-keepclasseswithmembers class com.vitals.mobile.** {
    kotlinx.serialization.KSerializer serializer(...);
}
