# --- MGGX Laberinto ---
-keepattributes SourceFile,LineNumberTable,*Annotation*
-renamesourcefileattribute SourceFile

# El renderer se instancia desde GLSurfaceView por reflexion interna en algunos OEM
-keep class com.mggx.laberinto.gl.** { *; }

# Catalogo de objetos: se recorre por nombre en tests/consistencia
-keep class com.mggx.laberinto.game.ItemCatalog { *; }

-dontwarn kotlinx.**
