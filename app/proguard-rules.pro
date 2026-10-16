# Kotlinx serialization: keep serializers for route and DTO classes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.androbito.tilfaz.**$$serializer { *; }
-keepclassmembers class com.androbito.tilfaz.** { *** Companion; }
-keepclasseswithmembers class com.androbito.tilfaz.** { kotlinx.serialization.KSerializer serializer(...); }

# Retrofit interfaces are used reflectively.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>
