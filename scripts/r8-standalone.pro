# R8 Configuration for Baritone - Standalone Variant
# Obfuscates everything including API for maximum size reduction

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Ignore warnings about missing Java runtime classes
-ignorewarnings

-verbose

-allowaccessmodification # anything not kept can be changed from public to private and inlined etc
-overloadaggressively
# R8 handles case sensitivity automatically
# -dontusemixedcaseclassnames

# R8-specific flags
# Note: R8 handles member optimization automatically

# For standalone: rename to baritone.a, baritone.b, etc to avoid conflicts
# For API: this will be modified to exclude API packages
-flattenpackagehierarchy
-repackageclasses 'baritone'

# lwjgl is weird
-dontwarn org.lwjgl.**
# also lwjgl lol
-dontwarn module-info
# we dont have forge
-dontwarn baritone.launch.BaritoneForgeModXD
# R8 doesn't like signature polymorphism
-dontwarn java.lang.invoke.MethodHandle

# For standalone, we obfuscate everything except what's absolutely necessary

# Only keep what's needed for reflection/gson
-keep class baritone.api.utils.MyChunkPos { *; } # needed for gson reflect

# Keep any class or member annotated with @KeepName so we dont have to put everything in the script
-keep,allowobfuscation @interface baritone.KeepName
-keep @baritone.KeepName class *
-keepclassmembers class * {
    @baritone.KeepName *;
}

# setting names are reflected from field names, so keep field names
-keepclassmembers class baritone.api.Settings {
    public <fields>;
}

# need to keep mixin names
-keep class baritone.launch.** { *; }

#try to keep usage of schematica in separate classes
-keep class baritone.utils.schematic.schematica.**
-keep class baritone.utils.schematic.litematica.**
# R8 doesn't like it when it can't find our fake schematica classes
-dontwarn baritone.utils.schematic.schematica.**
-dontwarn baritone.utils.schematic.litematica.**

# nether-pathfinder uses JNI to access its own classes
# and some of our builds include it before running R8
# conservatively keep all of it, even though only PathSegment.<init> is needed
-keep,allowoptimization class dev.babbaj.pathfinder.** { *; }
# Prevent warnings for shadowed nether-pathfinder classes
-dontwarn dev.babbaj.pathfinder.**

# Also keep - Enumerations. Keep the special static methods that are required in
# enumeration classes.
-keepclassmembers enum  * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep names - Native method names. Keep all native class/method names.
-keepclasseswithmembers,includedescriptorclasses,allowshrinking class * {
    native <methods>;
}

# These -assumenosideeffects rules have been removed as they are likely unnecessary
# and cause "does not match anything" warnings in R8