# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve debugging line numbers and file names in stack traces
-keepattributes SourceFile,LineNumberTable

# Preserve generic type signatures and annotations for reflection
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# ==============================================================================
# Retrofit 2
# ==============================================================================
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep interface online.thensoji.smsforwarder.network.api.** { *; }

# ==============================================================================
# Gson & Network DTO Models
# ==============================================================================
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep fields annotated with @SerializedName
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep all project network DTO models
-keep class online.thensoji.smsforwarder.network.model.** { *; }

# ==============================================================================
# OkHttp 3 & Okio
# ==============================================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ==============================================================================
# Room Database
# ==============================================================================
-keep class online.thensoji.smsforwarder.data.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ==============================================================================
# WorkManager & Hilt Workers
# ==============================================================================
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class online.thensoji.smsforwarder.SendWorker { *; }

# ==============================================================================
# Hilt / Dagger
# ==============================================================================
-dontwarn dagger.hilt.**
-keep class * extends dagger.hilt.android.HiltAndroidApp