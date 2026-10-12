# Sadhana Card Proguard / R8 Optimization Rules

# Keep annotations & line numbers for crash reporting
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-dontwarn java.time.**

# Room Database & TypeConverters
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.sadhana.card.data.local.converter.** { *; }

# Domain Models
-keep class com.sadhana.card.domain.model.** { *; }

# Hilt / Dagger DI
-dontwarn com.google.dagger.**
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class dagger.hilt.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
