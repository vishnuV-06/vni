# Preserve Annotations & Generics
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep all data models and their members (JSON serialization/deserialization)
-keep class com.vni.app.data.model.** { *; }
-keepclassmembers class com.vni.app.data.model.** { *; }

# Keep Focus models and states
-keep class com.vni.app.ui.focus.** { *; }
-keepclassmembers class com.vni.app.ui.focus.** { *; }

# Keep Storage, Crypto, and Security classes
-keep class com.vni.app.data.PersistentStorageManager { *; }
-keepclassmembers class com.vni.app.data.PersistentStorageManager { *; }
-keep class com.vni.app.ui.security.CryptoUtils { *; }
-keepclassmembers class com.vni.app.ui.security.CryptoUtils { *; }

# Keep Notification Receivers and Helpers
-keep class com.vni.app.notifications.** { *; }
-keepclassmembers class com.vni.app.notifications.** { *; }

# Keep ViewModel state and public functions
-keep class com.vni.app.SimpleHomeViewModel { *; }
-keepclassmembers class com.vni.app.SimpleHomeViewModel { *; }

# Firebase & Google Play Services
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Compose Runtime rules
-dontwarn androidx.compose.**

# Keep Update Manager and Models
-keep class com.vni.app.ui.updates.** { *; }
-keepclassmembers class com.vni.app.ui.updates.** { *; }
