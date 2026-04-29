# Keep Polylex public API surface
-keep public class dev.polylex.Polylex { public *; }
-keep public class dev.polylex.PolylexConfig { public *; }
-keep public class dev.polylex.PolylexContextWrapper { public *; }
-keep public class dev.polylex.PolylexResources { public *; }
-keep public class dev.polylex.models.** { public *; }

# Keep kotlinx.serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class **$$serializer { *; }
