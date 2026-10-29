# kotlinx.serialization: keep generated serializers for seed DTOs.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class dev.toufikforyou.questionbank.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.toufikforyou.questionbank.**$$serializer { *; }
