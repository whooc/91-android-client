# kotlinx.serialization keeps its serializers as generated code, but the plugin
# still registers them through a lookup table that R8 must not strip.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.whooc.nineone.**$$serializer { *; }
-keepclassmembers class com.whooc.nineone.** {
    *** Companion;
}
-keepclasseswithmembers class com.whooc.nineone.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Media3 resolves renderers and extractors by name.
-dontwarn androidx.media3.**
