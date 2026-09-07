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

// GGX + Schlick Fresnel + Smith visibility, evaluated in linear light.
vec3 surfaceLight(vec3 base, float rough, float metal, vec3 n, vec3 v, vec3 l, vec3 radiance) {
    float nl = max(dot(n, l), 0.0);
    if (uQuality == 0) return base * radiance * nl;
    float nv = max(dot(n, v), 0.001);
    vec3 h = (v + l) / max(length(v + l), 0.0001);
    float nh = max(dot(n, h), 0.0);
    float vh = max(dot(v, h), 0.0);
    float a = max(rough * rough, 0.045);
    float a2 = a * a;
    float d = nh * nh * (a2 - 1.0) + 1.0;
    float distribution = a2 / max(3.141593 * d * d, 0.00001);
    float k = (rough + 1.0) * (rough + 1.0) * 0.125;
    float visibility = 1.0 / max(4.0 * (nv * (1.0-k) + k) * (nl * (1.0-k) + k), 0.001);
    vec3 f0 = mix(vec3(0.04), base, metal);
    vec3 fresnel = f0 + (1.0 - f0) * pow(1.0 - vh, 5.0);
    vec3 diffuse = (1.0 - fresnel) * (1.0 - metal) * base;
    return (diffuse + fresnel * distribution * visibility * 3.141593) * radiance * nl;
}

vec3 lucesDeLaCueva(vec3 p, vec3 n, vec3 v, vec3 base, float rough, float metal) {
    vec3 sum = vec3(0.0);
    for (int i = 0; i < MAX_LUCES; i++) {
        if (i >= uNumLuces) break;
        vec3 delta = uLuzPos[i].xyz - p;
        float dist = length(delta);
        float radius = max(uLuzPos[i].w, 0.001);
        float att = max(1.0 - dist / radius, 0.0);
        sum += surfaceLight(base, rough, metal, n, v, delta / max(dist, 0.0001), uLuzColor[i] * att * att);
    }
    return sum;
}

vec3 displayColor(vec3 color) {
    color = max(color * uBrightness, vec3(0.0));
    // Filmic shoulder preserves mineral color around bright lights.
    color = clamp((color * (2.51 * color + 0.03)) / (color * (2.43 * color + 0.59) + 0.14), 0.0, 1.0);
    return pow(color, vec3(1.0 / 2.2));
}

out vec4 fragColor;

mat3 tangentFrame(vec3 n) {
    // Derive from the actual UVs; an axis guess flips floor/ceiling relief.
    vec3 px = dFdx(vWorld), py = dFdy(vWorld);
    vec2 tx = dFdx(vUv), ty = dFdy(vUv);
    vec3 a = cross(py, n), b = cross(n, px);
    vec3 t = a * tx.x + b * ty.x;
    vec3 bitangent = a * tx.y + b * ty.y;
    float inv = inversesqrt(max(max(dot(t,t), dot(bitangent,bitangent)), 0.00000001));
    return mat3(t * inv, bitangent * inv, n);
}

void main() {
    vec3 toEye = uCamPos - vWorld;
    float dist = length(toEye);
    vec3 v = toEye / max(dist, 0.0001);
    vec3 geometric = normalize(vNormal);
    mat3 frame = tangentFrame(geometric);
    vec2 uv = vUv;
    // Bounded parallax only near the camera and on high quality.
    if (uQuality >= 2) {
        float height = texture(uNormalMap, vec3(uv, vLayer)).a - 0.5;
        vec3 tangentView = vec3(dot(v,frame[0]), dot(v,frame[1]), dot(v,geometric));
        float nearFade = 1.0 - smoothstep(4.0, 10.0, dist);
        uv -= tangentView.xy / max(abs(tangentView.z), 0.4) * height * 0.018 * nearFade;
    }
    vec4 alb = texture(uAlbedo, vec3(uv, vLayer));
    vec3 base = alb.rgb;
    if (uQuality > 1) {
        float macro = texture(uAlbedo, vec3(uv * 0.143 + vec2(0.37,0.71), vLayer)).g;
        base *= 0.78 + 0.44 * macro;
    }
    vec3 n = geometric;
    float height = 0.5;
    if (uQuality > 0) {
        vec4 nm = texture(uNormalMap, vec3(uv, vLayer));
        vec3 mapped = nm.rgb * 2.0 - 1.0;
        mapped.xy *= uNormalStrength;
        n = normalize(frame * mapped);
        height = nm.a;
    }
    float wet = 1.0 - smoothstep(0.08, 0.60, height);
    float rough = clamp(0.90 - wet * 0.46 - alb.a * 0.16, 0.30, 0.95);
    float cavity = mix(0.68, 1.0, smoothstep(0.05, 0.65, height));
    float att = max(1.0 - dist / max(uLightRadius, 0.001), 0.0);
    att = att * att * uLightIntensity;
    if (uSpotPower > 0.0) {
        float cone = smoothstep(uSpotCos, mix(uSpotCos,1.0,0.42), dot(-v,uSpotDir));
        float x = max(1.0 - dist / max(uSpotRange,0.001), 0.0);
        att += x*x*uSpotPower*cone*1.55;
    }
    vec3 color = base * uAmbient * vAo * cavity;
    color += surfaceLight(base,rough,0.0,n,v,v,uLightColor*att) * mix(0.72,1.0,vAo);
    color += lucesDeLaCueva(vWorld,n,v,base,rough,0.0) * mix(0.72,1.0,vAo);
    float pulse = 0.72 + 0.28 * sin(uTime*1.6 + vWorld.x*0.35 + vWorld.z*0.27);
    color += uVeinColor * alb.a * uVeinPulse * pulse;
    if (uSonarRange > 0.0 && dist < uSonarRange) {
        float edge = 1.0 - abs(dot(n,v));
        color += uSonarColor * smoothstep(0.55,1.0,edge) * (1.0-dist/uSonarRange) * 0.9;
    }
    float fog = 1.0-exp(-uFogDensity*uFogDensity*dist*dist);
    color = mix(color,uFogColor,clamp(fog,0.0,1.0));
    fragColor = vec4(displayColor(color),1.0);
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
out vec3 vLocal;
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
    vec3 normal = aNormal;
    vLocal = aPos;

    if (tipo > 1.5 && tipo < 2.5) {
        // Aleteo: la punta del ala sube y baja, la raiz casi no se mueve.
        normal.x -= sin(uTime * 9.0 + iParams.y) * sign(p.x) * 1.25 * normal.y;
        p.y += sin(uTime * 9.0 + iParams.y) * abs(p.x) * 1.25;
    } else if (tipo > 2.5) {
        // Reptar: ondula de costado a lo largo del cuerpo.
        normal.z -= cos(uTime * 6.0 + iParams.y + p.z * 2.4) * 0.132 * normal.x;
        p.x += sin(uTime * 6.0 + iParams.y + p.z * 2.4) * 0.055;
    }

    vec3 rp = vec3(p.x * c + p.z * s, p.y, -p.x * s + p.z * c);
    vec3 rn = vec3(normal.x * c + normal.z * s, normal.y, -normal.x * s + normal.z * c);

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
in vec3 vLocal;
in vec3 vNormal;
in vec4 vColor;
in float vEmissive;
in float vAlpha;

uniform int uMaterial;
uniform int uQuality;
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

// GGX + Schlick Fresnel + Smith visibility, evaluated in linear light.
vec3 surfaceLight(vec3 base, float rough, float metal, vec3 n, vec3 v, vec3 l, vec3 radiance) {
    float nl = max(dot(n, l), 0.0);
    if (uQuality == 0) return base * radiance * nl;
    float nv = max(dot(n, v), 0.001);
    vec3 h = (v + l) / max(length(v + l), 0.0001);
    float nh = max(dot(n, h), 0.0);
    float vh = max(dot(v, h), 0.0);
    float a = max(rough * rough, 0.045);
    float a2 = a * a;
    float d = nh * nh * (a2 - 1.0) + 1.0;
    float distribution = a2 / max(3.141593 * d * d, 0.00001);
    float k = (rough + 1.0) * (rough + 1.0) * 0.125;
    float visibility = 1.0 / max(4.0 * (nv * (1.0-k) + k) * (nl * (1.0-k) + k), 0.001);
    vec3 f0 = mix(vec3(0.04), base, metal);
    vec3 fresnel = f0 + (1.0 - f0) * pow(1.0 - vh, 5.0);
    vec3 diffuse = (1.0 - fresnel) * (1.0 - metal) * base;
    return (diffuse + fresnel * distribution * visibility * 3.141593) * radiance * nl;
}

vec3 lucesDeLaCueva(vec3 p, vec3 n, vec3 v, vec3 base, float rough, float metal) {
    vec3 sum = vec3(0.0);
    for (int i = 0; i < MAX_LUCES; i++) {
        if (i >= uNumLuces) break;
        vec3 delta = uLuzPos[i].xyz - p;
        float dist = length(delta);
        float radius = max(uLuzPos[i].w, 0.001);
        float att = max(1.0 - dist / radius, 0.0);
        sum += surfaceLight(base, rough, metal, n, v, delta / max(dist, 0.0001), uLuzColor[i] * att * att);
    }
    return sum;
}

vec3 displayColor(vec3 color) {
    color = max(color * uBrightness, vec3(0.0));
    // Filmic shoulder preserves mineral color around bright lights.
    color = clamp((color * (2.51 * color + 0.03)) / (color * (2.43 * color + 0.59) + 0.14), 0.0, 1.0);
    return pow(color, vec3(1.0 / 2.2));
}

out vec4 fragColor;

float grain(vec3 p) {
    // Continuous object-space detail, fades with pixel footprint.
    return sin(p.x*71.0 + sin(p.z*19.0)) * sin(p.y*83.0 + p.z*13.0);
}
void main() {
    vec3 n = normalize(vNormal);
    vec3 delta = uCamPos-vWorld;
    float dist = length(delta);
    vec3 v = delta/max(dist,0.0001);
    // 0 stone, 1 wood, 2 iron, 3 crystal, 4 organic, 5 cloth, 6 flame.
    float rough = 0.83, metal = 0.0;
    vec3 base = vColor.rgb;
    float footprint = max(length(dFdx(vLocal)), length(dFdy(vLocal)));
    float detail = (1.0-smoothstep(0.008,0.055,footprint)) * (1.0-smoothstep(8.0,20.0,dist));
    if (uQuality == 0) detail = 0.0;
    float noise = grain(vLocal);
    base *= 1.0 + noise * 0.10 * detail;
    if (uMaterial == 1) {
        float rings = sin(vLocal.y*55.0 + sin(vLocal.x*12.0+vLocal.z*8.0)*2.0);
        base *= 1.0 + rings*0.15*detail;
        rough = 0.88;
    } else if (uMaterial == 2) {
        metal = 0.72; rough = 0.36 + noise*0.06*detail;
    } else if (uMaterial == 3) {
        rough = 0.20;
    } else if (uMaterial == 4) {
        rough = 0.62;
    } else if (uMaterial == 5) {
        float weave = sin(vLocal.x*160.0) * sin(vLocal.y*160.0);
        base *= 1.0 + weave*0.08*detail;
        rough = 0.94;
    }
    float att = max(1.0-dist/max(uLightRadius,0.001),0.0);
    att = att*att*uLightIntensity;
    if (uSpotPower > 0.0) {
        float cone = smoothstep(uSpotCos,mix(uSpotCos,1.0,0.42),dot(-v,uSpotDir));
        float x = max(1.0-dist/max(uSpotRange,0.001),0.0);
        att += x*x*uSpotPower*cone*1.55;
    }
    vec3 color = base*uAmbient;
    color += surfaceLight(base,rough,metal,n,v,v,uLightColor*att);
    color += lucesDeLaCueva(vWorld,n,v,base,rough,metal);
    color += base * max(vEmissive,0.0);
    float fog = 1.0-exp(-uFogDensity*uFogDensity*dist*dist);
    // Emission can exceed 1.0; never extrapolate fog with a negative weight.
    color = mix(color,uFogColor,clamp(fog,0.0,1.0)*(1.0-clamp(vEmissive*0.65,0.0,0.90)));
    fragColor = vec4(displayColor(color),vAlpha);
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
out vec3 vLocal;
out vec3 vNormal;
out float vSide;

void main() {
    mat4 m = aSide < 0.0 ? uArmL : uArmR;
    vec4 vp = m * vec4(aPos, 1.0);
    vViewPos = vp.xyz;
    vLocal = aPos;
    vNormal = normalize(mat3(m) * aNormal);
    vSide = aSide;
    gl_Position = uProj * vp;
}
"""

    const val ARMS_FS = """#version 300 es
precision highp float;

in vec3 vViewPos;
in vec3 vLocal;
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
    float t = 1.0 - smoothstep(-0.10, 0.045, vLocal.z);
    vec3 base = mix(uCloth, uSkin, t);
    // Grano fino: la piel y el cuero no son superficies planas de un solo color.
    float grano = hash(floor(vLocal.xy * 190.0)) * 0.16 - 0.08;
    base *= (1.0 + grano);
    // Sombra en los pliegues entre los dedos
    float pliegue = smoothstep(0.0, 0.35, abs(fract(vLocal.x * 26.0) - 0.5));
    base *= mix(0.86, 1.0, pliegue);

    if (uStyle == 1) {
        // Malla de hierro: retícula regular
        float g = step(0.55, hash(floor(vLocal.xy * 46.0)));
        base = mix(base, base * 1.75, g * (1.0 - t));
    } else if (uStyle == 2) {
        // Manos de ceniza: grietas incandescentes
        float cr = hash(floor(vLocal.xy * 26.0));
        float glow = smoothstep(0.86, 1.0, cr);
        base = mix(base * 0.55, vec3(1.0, 0.42, 0.12), glow * 0.85);
    } else if (uStyle == 3) {
        // Cazador: gema en el dorso
        float d = length(vLocal.xy - vec2(vSide * 0.16, -0.10));
        base = mix(base, vec3(0.62, 0.90, 1.0), (1.0 - smoothstep(0.0, 0.055, d)));
    } else if (uStyle == 4) {
        // Cristal vivo: brillo que late
        float p = 0.5 + 0.5 * sin(uTime * 2.4 + vLocal.y * 12.0);
        base = mix(base, vec3(0.60, 0.92, 1.0), 0.35 + 0.25 * p);
    }

    // Detalle propio de la skin, encima del guante.
    if (uSkinStyle == 1) {
        // Veterano: la casaca esta remendada con parches mas oscuros.
        float parche = step(0.72, hash(floor(vLocal.xy * 17.0)));
        base = mix(base, base * 0.68, parche * (1.0 - t));
    } else if (uSkinStyle == 2) {
        // Tecnico: bandas reflectantes en la manga.
        float banda = step(0.80, fract(vLocal.y * 9.0 + 0.5));
        base = mix(base, vec3(0.94, 0.94, 0.86), banda * (1.0 - t) * 0.85);
    } else if (uSkinStyle == 3) {
        // Ceniza: polvo gris que apaga los brillos.
        base *= 0.86;
    } else if (uSkinStyle == 4) {
        // Vetagris: la piel tiene vetas que brillan apenas.
        float veta = smoothstep(0.88, 1.0, hash(floor(vLocal.xy * 33.0)));
        base = mix(base, vec3(0.78, 0.90, 1.0), veta * t * 0.9);
    } else if (uSkinStyle == 5) {
        // Esporas: puntitos bioluminiscentes que laten.
        float pl = 0.5 + 0.5 * sin(uTime * 1.8 + vLocal.x * 21.0);
        float pt = smoothstep(0.90, 1.0, hash(floor(vLocal.xy * 29.0)));
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
    float a = (1.0 - smoothstep(0.15, 1.0, d)) * vColor.a;
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

