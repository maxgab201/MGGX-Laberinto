# --- MGGX Laberinto ---
-keepattributes SourceFile,LineNumberTable,*Annotation*
-renamesourcefileattribute SourceFile

# El renderer se instancia desde GLSurfaceView por reflexion interna en algunos OEM
-keep class com.mggx.laberinto.gl.** { *; }

# Catalogo de objetos: se recorre por nombre en tests/consistencia
-keep class com.mggx.laberinto.game.ItemCatalog { *; }

# Firebase se arma solo al arrancar: un ContentProvider que Android instancia
# por nombre desde el manifest, y de ahi una lista de "registrars" que el SDK
# busca por reflexion. Nada de eso son llamadas que R8 pueda ver, asi que si
# no se le avisa se los puede llevar puestos y el multijugador queda diciendo
# que falta la configuracion.
-keep class com.google.firebase.provider.FirebaseInitProvider { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-keepclassmembers class com.google.firebase.FirebaseApp {
    public static ** initializeApp(...);
    public static ** getInstance(...);
}
-dontwarn com.google.firebase.**

-dontwarn kotlinx.**
