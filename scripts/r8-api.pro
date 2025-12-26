# R8 Configuration for Baritone - API Variant
# Keeps baritone.api package completely untouched, obfuscates main code

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

# For API variant: obfuscate everything except API classes
# Main code gets obfuscated, API stays untouched
-flattenpackagehierarchy
-repackageclasses 'baritone'

# External library warnings (safely ignored)
-dontwarn org.lwjgl.**         # LWJGL native bindings
-dontwarn module-info           # Java 9+ module system
-dontwarn baritone.launch.BaritoneForgeModXD  # Forge module loaded separately
-dontwarn java.lang.invoke.MethodHandle  # R8 signature polymorphism

# Keep API package completely untouched - no obfuscation, no repackaging
# Using -keep without allowobfuscation to preserve names
-keep class baritone.api.** { *; }
-keeppackagenames baritone.api.**

# Service provider needs these exact class names (but can be in repackaged location)
# Let main code be obfuscated but keep the provider findable
-keep class baritone.BaritoneProvider
-keep class baritone.api.IBaritoneProvider

-keep class baritone.api.utils.MyChunkPos { *; } # even in standalone we need to keep this for gson reflect
-keepnames class baritone.api.utils.BlockOptionalMeta # this name is exposed to the user, so we need to keep it in all builds

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