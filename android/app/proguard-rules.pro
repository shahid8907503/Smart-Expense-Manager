# Proguard rules for Voice-Enabled Smart Expense Manager
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class com.smart.expense.data.remote.dto.** { *; }

