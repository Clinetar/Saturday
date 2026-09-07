# Keep kotlinx.serialization generated serializers.
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.AI.clinetar.Saturday.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
