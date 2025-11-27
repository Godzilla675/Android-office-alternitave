# Add project specific ProGuard rules here.
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.**
-dontwarn javax.**
-dontwarn org.w3c.**
-dontwarn org.xml.**

# ML Kit
-keep class com.google.mlkit.** { *; }

# iText
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Markwon
-keep class io.noties.markwon.** { *; }
