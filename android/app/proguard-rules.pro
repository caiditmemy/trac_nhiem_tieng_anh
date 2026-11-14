# NamBeDe release build (R8 shrinks and obfuscates the code).
# The app reads the gateway's JSON with kotlinx.serialization and calls it through Retrofit; Retrofit, OkHttp and
# kotlinx.serialization ship their own rules, the lines below cover the app's own classes.

# JSON models, their generated serializers and the Retrofit API interface: looked up at runtime from the API's types
-keep class vn.edu.qnu.st.nambede.data.** { *; }

# Retrofit reads the generic return types of the API methods (suspend functions included)
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Readable line numbers in crash reports
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
