package com.mggx.laberinto.gl

/**
 * Shaders GLSL ES 3.0. La iluminacion es una antorcha puntual pegada a la camara
 * mas ambiental del tema, niebla exponencial y vetas de mineral emisivas.
 */
object Shaders {

    // ------------------------------------------------------------------ mundo
    const val WORLD_VS = """#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUv;
layout(location = 3) in float aAo;
layout(location = 4) in float aLayer;

uniform mat4 uViewProj;

out vec3 vWorld;
out vec3 vNormal;
out vec2 vUv;
out float vAo;
out float vLayer;

void main() {
    vWorld = aPos;
    vNormal = aNormal;
    vUv = aUv;
    vAo = aAo;
    vLayer = aLayer;
    gl_Position = uViewProj * vec4(aPos, 1.0);
}
"""

    const val WORLD_FS = """#version 300 es
precision highp float;
precision highp sampler2DArray;

in vec3 vWorld;
in vec3 vNormal;
in vec2 vUv;
in float vAo;
in float vLayer;

uniform sampler2DArray uAlbedo;
uniform sampler2DArray uNormalMap;

uniform vec3 uCamPos;
uniform vec3 uLightColor;
uniform float uLightRadius;
uniform float uLightIntensity;
uniform vec3 uAmbient;
uniform vec3 uFogColor;
uniform float uFogDensity;
uniform vec3 uVeinColor;
uniform float uVeinPulse;
uniform float uBrightness;
uniform float uTime;
uniform float uNormalStrength;
uniform int uQuality;

// Sonar: dibuja el contorno de las paredes cercanas
uniform float uSonarRange;
uniform vec3 uSonarColor;
// Linterna de carburo: un haz que sale del ojo hacia donde mira la camara.
// Con uSpotPower en 0 no cambia absolutamente nada.
uniform vec3 uSpotDir;
uniform float uSpotPower;
uniform float uSpotRange;
uniform float uSpotCos;

// Luces de la propia cueva: antorchas, cristales, hongos y la salida.
// Hasta ocho a la vez, las mas cercanas al jugador. Con uNumLuces en 0 el
// bucle no corre y la cueva queda iluminada solo por vos, como antes.
#define MAX_LUCES 8
uniform vec4 uLuzPos[MAX_LUCES];    // xyz posicion, w alcance
uniform vec3 uLuzColor[MAX_LUCES];
uniform int uNumLuces;

vec3 lucesDeLaCueva(vec3 p, vec3 n) {
    vec3 suma = vec3(0.0);
    for (int i = 0; i < MAX_LUCES; i++) {
        if (i >= uNumLuces) break;
        vec3 dl = uLuzPos[i].xyz - p;
        float dd = length(dl);
        float r = uLuzPos[i].w;
        if (dd >= r) continue;
        float att = 1.0 - dd / r;
        att *= att;
        // Un piso de luz ambiental propia para que la roca a contraluz de un
        // cristal no quede completamente negra.
        float ndl = max(dot(n, dl / max(dd, 0.0001)), 0.0) * 0.86 + 0.14;
        suma += uLuzColor[i] * ndl * att;
    }
    return suma;
}

out vec4 fragColor;

vec3 applyNormalMap(vec3 n, vec3 mapN) {
    // Las caras del laberinto son alineadas a ejes: la tangente sale del eje dominante.
    vec3 up = abs(n.y) > 0.9 ? vec3(0.0, 0.0, 1.0) : vec3(0.0, 1.0, 0.0);
    vec3 t = normalize(cross(up, n));
    vec3 b = cross(n, t);
    vec3 m = mapN * 2.0 - 1.0;
    m.xy *= uNormalStrength;
    return normalize(t * m.x + b * m.y + n * m.z);
}

void main() {
    vec3 toLight = uCamPos - vWorld;
    float dist = length(toLight);
    vec3 L = toLight / max(dist, 0.0001);

    vec4 alb = texture(uAlbedo, vec3(vUv, vLayer));
    vec3 baseColor = alb.rgb;
    float veinMask = alb.a;

    if (uQuality > 1) {
        // La misma textura, muy estirada, modula el brillo de a manchones
        // grandes. Es lo que rompe la repeticion cada 3 metros, que era lo
        // que mas cantaba a la vista.
        float macro = texture(uAlbedo, vec3(vUv * 0.143 + vec2(0.37, 0.71), vLayer)).g;
        baseColor *= (0.70 + 0.60 * macro);

        // Grano fino que solo se nota de cerca: a lo lejos se apaga para que
        // no titile con el mipmap.
        float cerca = 1.0 - clamp(dist / 7.0, 0.0, 1.0);
        float fino = texture(uAlbedo, vec3(vUv * 5.3, vLayer)).r;
        baseColor *= mix(1.0, 0.80 + 0.40 * fino, cerca * 0.55);
    }

    vec3 n = normalize(vNormal);
    float altura = 0.5;
    if (uQuality > 0) {
        vec4 nm = texture(uNormalMap, vec3(vUv, vLayer));
        n = applyNormalMap(n, nm.rgb);
        altura = nm.a;
    }

    // Atenuacion suave con corte en el radio de la antorcha
    float x = clamp(1.0 - dist / max(uLightRadius, 0.001), 0.0, 1.0);
    float atten = x * x * uLightIntensity;

    float ndl = max(dot(n, L), 0.0);
    vec3 diffuse = uLightColor * ndl * atten;

    // Haz de la linterna: mismo origen que la antorcha, pero con cono y mucho
    // mas alcance. El borde se suaviza para que no quede un circulo recortado.
    if (uSpotPower > 0.0) {
        float cd = dot(-L, uSpotDir);
        float cono = smoothstep(uSpotCos, mix(uSpotCos, 1.0, 0.42), cd);
        float ax = clamp(1.0 - dist / max(uSpotRange, 0.001), 0.0, 1.0);
        float spotAtt = ax * ax * uSpotPower * cono;
        diffuse += uLightColor * ndl * spotAtt * 1.55;
        atten += spotAtt;
    }

    // La antorcha va pegada al ojo, asi que el vector medio del especular es la
    // propia luz. Las hondonadas de la roca juntan humedad y brillan mas que
    // los salientes, que estan secos.
    // Ojo: smoothstep exige edge0 < edge1. Al reves es indefinido en GLSL ES y
    // en algunos drivers devuelve una constante, con lo que el brillo de
    // humedad desaparecia sin dar ningun error.
    float humedad = 1.0 - smoothstep(0.05, 0.62, altura);
    float spec = pow(max(dot(n, L), 0.0), 30.0) * atten * (0.05 + 0.34 * humedad);

    vec3 color = baseColor * (uAmbient + diffuse + lucesDeLaCueva(vWorld, n)) * vAo +
        uLightColor * spec;

    // Vetas de mineral: brillan solas y laten
    float pulse = 0.72 + 0.28 * sin(uTime * 1.6 + vWorld.x * 0.35 + vWorld.z * 0.27);
    color += uVeinColor * veinMask * uVeinPulse * pulse;

    // Sonar
    if (uSonarRange > 0.0 && dist < uSonarRange) {
        float edge = 1.0 - abs(dot(n, L));
        float wave = smoothstep(0.55, 1.0, edge);
        float falloff = 1.0 - dist / uSonarRange;
        color += uSonarColor * wave * falloff * 0.9;
    }

    // Niebla exponencial al cuadrado
    float f = 1.0 - exp(-uFogDensity * uFogDensity * dist * dist);
    color = mix(color, uFogColor, clamp(f, 0.0, 1.0));

    color *= uBrightness;
    // Tonemap Reinhard suave para que las luces no quemen
    color = color / (color + vec3(0.85));
    color = pow(color, vec3(1.0 / 2.2));

    fragColor = vec4(color, 1.0);
}
"""

    // ------------------------------------------------------------------ props
    const val PROP_VS = """#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
// Instancia: xyz posicion, w escala
layout(location = 2) in vec4 iPosScale;
// Instancia: rgb color, a intensidad de emision
layout(location = 3) in vec4 iColor;
// Instancia: x rotacion Y, y desfase de animacion, z tipo, w alpha
layout(location = 4) in vec4 iParams;

uniform mat4 uViewProj;
uniform float uTime;

out vec3 vWorld;
out vec3 vNormal;
out vec4 vColor;
out float vEmissive;
out float vAlpha;

void main() {
    // iParams.z = tipo: 0 quieto, 1 gema que gira y flota, 2 ala de murcielago,
    // 3 bicho que repta. Solo el tipo 1 gira solo.
    float tipo = iParams.z;
    bool gema = tipo > 0.5 && tipo < 1.5;
    float ang = iParams.x + uTime * (gema ? 1.15 : 0.0);
    float s = sin(ang), c = cos(ang);
    vec3 p = aPos * iPosScale.w;

    if (tipo > 1.5 && tipo < 2.5) {
        // Aleteo: la punta del ala sube y baja, la raiz casi no se mueve.
        p.y += sin(uTime * 9.0 + iParams.y) * abs(p.x) * 1.25;
    } else if (tipo > 2.5) {
        // Reptar: ondula de costado a lo largo del cuerpo.
        p.x += sin(uTime * 6.0 + iParams.y + p.z * 2.4) * 0.055;
    }

    vec3 rp = vec3(p.x * c + p.z * s, p.y, -p.x * s + p.z * c);
    vec3 rn = vec3(aNormal.x * c + aNormal.z * s, aNormal.y, -aNormal.x * s + aNormal.z * c);

    float bob = gema ? sin(uTime * 2.1 + iParams.y) * 0.11 : 0.0;
    vec3 world = iPosScale.xyz + rp + vec3(0.0, bob, 0.0);

    vWorld = world;
    vNormal = rn;
    vColor = iColor;
    vEmissive = iColor.a;
    vAlpha = iParams.w;
    gl_Position = uViewProj * vec4(world, 1.0);
}
"""

    const val PROP_FS = """#version 300 es
precision highp float;

in vec3 vWorld;
in vec3 vNormal;
in vec4 vColor;
in float vEmissive;
in float vAlpha;

uniform vec3 uCamPos;
uniform vec3 uLightColor;
uniform float uLightRadius;
uniform float uLightIntensity;
uniform vec3 uAmbient;
uniform vec3 uFogColor;
uniform float uFogDensity;
uniform float uBrightness;
uniform vec3 uSpotDir;
uniform float uSpotPower;
uniform float uSpotRange;
uniform float uSpotCos;

// Luces de la propia cueva: antorchas, cristales, hongos y la salida.
// Hasta ocho a la vez, las mas cercanas al jugador. Con uNumLuces en 0 el
// bucle no corre y la cueva queda iluminada solo por vos, como antes.
#define MAX_LUCES 8
uniform vec4 uLuzPos[MAX_LUCES];    // xyz posicion, w alcance
uniform vec3 uLuzColor[MAX_LUCES];
uniform int uNumLuces;

vec3 lucesDeLaCueva(vec3 p, vec3 n) {
    vec3 suma = vec3(0.0);
    for (int i = 0; i < MAX_LUCES; i++) {
        if (i >= uNumLuces) break;
        vec3 dl = uLuzPos[i].xyz - p;
        float dd = length(dl);
        float r = uLuzPos[i].w;
        if (dd >= r) continue;
        float att = 1.0 - dd / r;
        att *= att;
        // Un piso de luz ambiental propia para que la roca a contraluz de un
        // cristal no quede completamente negra.
        float ndl = max(dot(n, dl / max(dd, 0.0001)), 0.0) * 0.86 + 0.14;
        suma += uLuzColor[i] * ndl * att;
    }
    return suma;
}

out vec4 fragColor;

void main() {
    vec3 n = normalize(vNormal);
    vec3 toLight = uCamPos - vWorld;
    float dist = length(toLight);
    vec3 L = toLight / max(dist, 0.0001);

    float x = clamp(1.0 - dist / max(uLightRadius, 0.001), 0.0, 1.0);
    float atten = x * x * uLightIntensity;
    if (uSpotPower > 0.0) {
        float cd = dot(-L, uSpotDir);
        float cono = smoothstep(uSpotCos, mix(uSpotCos, 1.0, 0.42), cd);
        float ax = clamp(1.0 - dist / max(uSpotRange, 0.001), 0.0, 1.0);
        atten += ax * ax * uSpotPower * cono * 1.55;
    }
    float ndl = max(dot(n, L), 0.0);

    vec3 color = vColor.rgb *
        (uAmbient + uLightColor * ndl * atten + lucesDeLaCueva(vWorld, n));
    color += vColor.rgb * vEmissive;

    float f = 1.0 - exp(-uFogDensity * uFogDensity * dist * dist);
    color = mix(color, uFogColor, clamp(f, 0.0, 1.0) * (1.0 - vEmissive * 0.65));

    color *= uBrightness;
    color = color / (color + vec3(0.85));
    color = pow(color, vec3(1.0 / 2.2));

    fragColor = vec4(color, vAlpha);
}
"""

    // ------------------------------------------------------------------ brazos
    const val ARMS_VS = """#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in float aSide;   // -1 izquierda, +1 derecha

uniform mat4 uProj;
uniform mat4 uArmL;
uniform mat4 uArmR;

out vec3 vViewPos;
out vec3 vNormal;
out float vSide;

void main() {
    mat4 m = aSide < 0.0 ? uArmL : uArmR;
    vec4 vp = m * vec4(aPos, 1.0);
    vViewPos = vp.xyz;
    vNormal = normalize(mat3(m) * aNormal);
    vSide = aSide;
    gl_Position = uProj * vp;
}
"""

    const val ARMS_FS = """#version 300 es
precision highp float;

in vec3 vViewPos;
in vec3 vNormal;
in float vSide;

uniform vec3 uLightColor;
uniform float uLightIntensity;
uniform vec3 uAmbient;
uniform vec3 uSkin;
uniform vec3 uCloth;
uniform float uBrightness;
uniform float uTime;
uniform int uStyle;
// Skin del personaje: agrega su propio detalle sobre el traje.
uniform int uSkinStyle;

out vec4 fragColor;

// Ruido barato para la textura del guante
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec3 n = normalize(vNormal);
    // La antorcha esta practicamente en el ojo: la luz sale del origen de la vista
    vec3 L = normalize(-vViewPos);
    float dist = length(vViewPos);
    float atten = uLightIntensity * clamp(1.0 - dist / 1.9, 0.12, 1.0);
    float ndl = max(dot(n, L), 0.0);

    // La mano se separa en piel (punta) y guante (base) segun la profundidad local
    // Cuanto mas lejos de la camara, mas mano y menos guante: el antebrazo
    // (que esta cerca) va de cuero y la mano (mas adelante) va de piel.
    float t = clamp((-vViewPos.z - 0.52) / 0.16, 0.0, 1.0);
    vec3 base = mix(uCloth, uSkin, t);
    // Grano fino: la piel y el cuero no son superficies planas de un solo color.
    float grano = hash(floor(vViewPos.xy * 190.0)) * 0.16 - 0.08;
    base *= (1.0 + grano);
    // Sombra en los pliegues entre los dedos
    float pliegue = smoothstep(0.0, 0.35, abs(fract(vViewPos.x * 26.0) - 0.5));
    base *= mix(0.86, 1.0, pliegue);

    if (uStyle == 1) {
        // Malla de hierro: retícula regular
        float g = step(0.55, hash(floor(vViewPos.xy * 46.0)));
        base = mix(base, base * 1.75, g * (1.0 - t));
    } else if (uStyle == 2) {
        // Manos de ceniza: grietas incandescentes
        float cr = hash(floor(vViewPos.xy * 26.0));
        float glow = smoothstep(0.86, 1.0, cr);
        base = mix(base * 0.55, vec3(1.0, 0.42, 0.12), glow * 0.85);
    } else if (uStyle == 3) {
        // Cazador: gema en el dorso
        float d = length(vViewPos.xy - vec2(vSide * 0.16, -0.10));
        base = mix(base, vec3(0.62, 0.90, 1.0), smoothstep(0.055, 0.0, d));
    } else if (uStyle == 4) {
        // Cristal vivo: brillo que late
        float p = 0.5 + 0.5 * sin(uTime * 2.4 + vViewPos.y * 12.0);
        base = mix(base, vec3(0.60, 0.92, 1.0), 0.35 + 0.25 * p);
    }

    // Detalle propio de la skin, encima del guante.
    if (uSkinStyle == 1) {
        // Veterano: la casaca esta remendada con parches mas oscuros.
        float parche = step(0.72, hash(floor(vViewPos.xy * 17.0)));
        base = mix(base, base * 0.68, parche * (1.0 - t));
    } else if (uSkinStyle == 2) {
        // Tecnico: bandas reflectantes en la manga.
        float banda = step(0.80, fract(vViewPos.y * 9.0 + 0.5));
        base = mix(base, vec3(0.94, 0.94, 0.86), banda * (1.0 - t) * 0.85);
    } else if (uSkinStyle == 3) {
        // Ceniza: polvo gris que apaga los brillos.
        base *= 0.86;
    } else if (uSkinStyle == 4) {
        // Vetagris: la piel tiene vetas que brillan apenas.
        float veta = smoothstep(0.88, 1.0, hash(floor(vViewPos.xy * 33.0)));
        base = mix(base, vec3(0.78, 0.90, 1.0), veta * t * 0.9);
    } else if (uSkinStyle == 5) {
        // Esporas: puntitos bioluminiscentes que laten.
        float pl = 0.5 + 0.5 * sin(uTime * 1.8 + vViewPos.x * 21.0);
        float pt = smoothstep(0.90, 1.0, hash(floor(vViewPos.xy * 29.0)));
        base = mix(base, vec3(0.55, 1.0, 0.70), pt * (0.35 + 0.45 * pl));
    }

    // Las manos siempre llevan algo de luz propia: son lo mas cercano a la
    // antorcha y si quedan negras el juego se siente vacio.
    vec3 color = base * (uAmbient * 1.8 + vec3(0.045) + uLightColor * ndl * atten);
    float rim = pow(1.0 - max(dot(n, vec3(0.0, 0.0, 1.0)), 0.0), 2.5);
    color += uLightColor * rim * 0.07 * atten;

    color *= uBrightness;
    color = color / (color + vec3(0.85));
    color = pow(color, vec3(1.0 / 2.2));
    fragColor = vec4(color, 1.0);
}
"""

    // ---------------------------------------------------- decals de suelo
    const val DECAL_VS = """#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUv;
layout(location = 2) in vec4 aColor;

uniform mat4 uViewProj;

out vec2 vUv;
out vec4 vColor;
out vec3 vWorld;

void main() {
    vUv = aUv;
    vColor = aColor;
    vWorld = aPos;
    gl_Position = uViewProj * vec4(aPos, 1.0);
}
"""

    const val DECAL_FS = """#version 300 es
precision highp float;
in vec2 vUv;
in vec4 vColor;
in vec3 vWorld;

uniform vec3 uCamPos;
uniform float uFogDensity;
uniform float uBrightness;

out vec4 fragColor;

void main() {
    float d = length(vUv - vec2(0.5)) * 2.0;
    float a = smoothstep(1.0, 0.15, d) * vColor.a;
    float dist = length(uCamPos - vWorld);
    float f = 1.0 - exp(-uFogDensity * uFogDensity * dist * dist);
    a *= (1.0 - clamp(f, 0.0, 1.0));
    if (a < 0.01) discard;
    vec3 c = vColor.rgb * uBrightness;
    c = c / (c + vec3(0.85));
    fragColor = vec4(pow(c, vec3(1.0 / 2.2)), a);
}
"""

    // ------------------------------------------------------------- vineta
    const val OVERLAY_VS = """#version 300 es
layout(location = 0) in vec2 aPos;
out vec2 vUv;
void main() {
    vUv = aPos * 0.5 + 0.5;
    gl_Position = vec4(aPos, 0.0, 1.0);
}
"""

    const val OVERLAY_FS = """#version 300 es
precision mediump float;
in vec2 vUv;
uniform float uStrength;
uniform vec3 uTintColor;
uniform float uTintAmount;
uniform float uHurt;
out vec4 fragColor;

void main() {
    vec2 p = vUv - 0.5;
    float r = length(p) * 1.42;
    float v = smoothstep(0.42, 1.05, r) * uStrength;
    vec3 col = mix(vec3(0.0), uTintColor, uTintAmount);
    float a = v;
    if (uHurt > 0.0) {
        float edge = smoothstep(0.25, 1.0, r);
        col = mix(col, vec3(0.72, 0.06, 0.05), uHurt);
        a = max(a, edge * uHurt);
    }
    fragColor = vec4(col, clamp(a, 0.0, 1.0));
}
"""
}
