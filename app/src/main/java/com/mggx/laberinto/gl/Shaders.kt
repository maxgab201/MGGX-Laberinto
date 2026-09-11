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
    // Relieve por parallax, solo cerca de la camara y en calidad alta.
    //
    // La profundidad subio de 0.018 a 0.032: con el valor viejo el relieve
    // estaba, pero tan sutil que la pared seguia leyendose plana salvo pegado.
    // Es el ajuste que mas hace por que la roca se vea en 3D y no como un
    // dibujo pintado encima de un plano.
    if (uQuality >= 2) {
        float height = texture(uNormalMap, vec3(uv, vLayer)).a - 0.5;
        vec3 tangentView = vec3(dot(v,frame[0]), dot(v,frame[1]), dot(v,geometric));
        float nearFade = 1.0 - smoothstep(4.0, 10.0, dist);
        uv -= tangentView.xy / max(abs(tangentView.z), 0.4) * height * 0.032 * nearFade;
    }
    // ------------------------------------------------- variantes de la roca
    //
    // La textura se proyecta en coordenadas del mundo, asi que la MISMA
    // baldosa de 3 m se repite por toda la cueva. De lejos eso se lee como una
    // lamina fotocopiada y es lo que mas delata que la pared es procedural.
    //
    // El arreglo no es hacer la textura mas grande (mas pixeles = mas tiempo
    // de carga y mas memoria, y la repeticion sigue igual): es tomar DOS
    // muestras de la misma textura, una de ellas girada y corrida, y mezclarlas
    // con una mascara de frecuencia muy baja. Donde la mascara vale 0 se ve una
    // version, donde vale 1 la otra, y en el medio una mezcla. El ojo deja de
    // encontrar el patron aunque la textura sea la misma.
    //
    // El giro es de 0.6 radianes, que no es multiplo de un cuarto de vuelta a
    // proposito: con 90 grados la veta seguiria alineada con la grilla y la
    // repeticion se notaria igual.
    vec4 alb;
    vec4 nm;
    if (uQuality > 1) {
        const mat2 giro = mat2(0.8253, -0.5646, 0.5646, 0.8253);    // 0.6 rad
        const mat2 desgiro = mat2(0.8253, 0.5646, -0.5646, 0.8253); // la inversa
        vec2 uvB = giro * uv * 1.37 + vec2(4.21, 7.63);
        // La mascara sale de la propia textura, muy estirada: no cuesta un
        // muestreo aparte con su propio ruido, y ya viene sin costuras.
        float mezcla = smoothstep(0.35, 0.65, texture(uAlbedo, vec3(uv * 0.083 + vec2(0.19, 0.57), vLayer)).g);
        alb = mix(texture(uAlbedo, vec3(uv, vLayer)), texture(uAlbedo, vec3(uvB, vLayer)), mezcla);

        vec4 nA = texture(uNormalMap, vec3(uv, vLayer));
        vec4 nB = texture(uNormalMap, vec3(uvB, vLayer));
        // La segunda muestra se leyo con las coordenadas GIRADAS, asi que su
        // normal tambien viene girada: apunta 0.6 radianes al costado de donde
        // deberia. Hay que devolverla al marco de la primera antes de
        // mezclarlas, o el relieve queda iluminado desde una direccion que no
        // existe justo en las zonas de mezcla. Se desgira solo XY, que es la
        // parte que vive en el plano de la textura; Z es la altura y no rota.
        nB.xy = desgiro * (nB.xy - 0.5) + 0.5;
        nm = mix(nA, nB, mezcla);
    } else {
        alb = texture(uAlbedo, vec3(uv, vLayer));
        nm  = texture(uNormalMap, vec3(uv, vLayer));
    }
    vec3 base = alb.rgb;
    if (uQuality > 1) {
        float macro = texture(uAlbedo, vec3(uv * 0.143 + vec2(0.37,0.71), vLayer)).g;
        base *= 0.78 + 0.44 * macro;
    }
    vec3 n = geometric;
    float height = 0.5;
    if (uQuality > 0) {
        vec3 mapped = nm.rgb * 2.0 - 1.0;
        mapped.xy *= uNormalStrength;

        // --------------------------------------------- detalle de cerca
        //
        // Pegado a la pared, la baldosa de 3 m se estira tanto que la roca
        // queda lisa: el grano fino simplemente no existe a esa escala. Esta
        // segunda muestra, ocho veces mas chica, devuelve ese grano SOLO en el
        // primer metro y medio, donde el ojo lo busca. Es detalle que no cuesta
        // ni un pixel de textura ni un milisegundo de carga: sale de la misma
        // imagen leida mas de cerca.
        if (uQuality >= 2) {
            float cerca = 1.0 - smoothstep(0.6, 2.6, dist);
            if (cerca > 0.004) {
                vec3 fino = texture(uNormalMap, vec3(uv * 8.0, vLayer)).rgb * 2.0 - 1.0;
                mapped.xy += fino.xy * 0.55 * cerca;
                base *= 1.0 + (texture(uAlbedo, vec3(uv * 8.0, vLayer)).g - 0.5) * 0.28 * cerca;
            }
        }
        n = normalize(frame * mapped);
        height = nm.a;
    }
    // ------------------------------------------------ sombra propia del relieve
    //
    // Lo que termina de convencer de que una superficie tiene bultos no es el
    // mapa de normales: es que los bultos SE TAPEN ENTRE SI. Se camina el mapa
    // de altura unos pasos en la direccion de la luz (en espacio tangente): si
    // en el camino hay algo mas alto que la recta hacia la luz, este pixel esta
    // a la sombra de ese bulto.
    //
    // La luz principal de este juego es la antorcha que lleva el jugador, o
    // sea que esta EN EL OJO: por eso se usa `v` como direccion de la luz y no
    // hay un vector aparte. Es la misma razon por la que la sombra propia se
    // mueve con vos al girar, como pasa con una linterna de verdad.
    //
    // Cuatro pasos alcanzan y solo en calidad ultra: son cuatro muestreos mas
    // por pixel, y es lo primero que hay que sacar en un telefono modesto.
    float sombraPropia = 1.0;
    if (uQuality >= 3) {
        vec3 lTan = vec3(dot(v, frame[0]), dot(v, frame[1]), dot(v, geometric));
        if (lTan.z > 0.05) {
            vec2 paso = lTan.xy / lTan.z * 0.030 * 0.25;
            float h0 = height;
            float tapado = 0.0;
            for (int i = 1; i <= 4; i++) {
                float hi = texture(uNormalMap, vec3(uv + paso * float(i), vLayer)).a;
                tapado = max(tapado, hi - (h0 + 0.030 * 0.25 * float(i) * 1.6));
            }
            sombraPropia = 1.0 - clamp(tapado * 6.0, 0.0, 0.72);
        }
    }

    float wet = 1.0 - smoothstep(0.08, 0.60, height);
    float rough = clamp(0.90 - wet * 0.46 - alb.a * 0.16, 0.30, 0.95);
    float cavity = mix(0.68, 1.0, smoothstep(0.05, 0.65, height));
    // Caida de la antorcha.
    //
    // Antes era `att^2` a secas, que se apaga muy despacio cerca y de golpe
    // lejos: daba una cueva pareja, iluminada de punta a punta, sin oscuridad
    // de verdad en ningun lado. La cuarta potencia concentra la luz en un
    // circulo alrededor tuyo y deja el resto en penumbra, que es lo que hace
    // que una antorcha de la pared o un cristal a lo lejos signifiquen algo.
    float att = max(1.0 - dist / max(uLightRadius, 0.001), 0.0);
    att = att * att;
    att = att * att * uLightIntensity;
    if (uSpotPower > 0.0) {
        float cone = smoothstep(uSpotCos, mix(uSpotCos,1.0,0.42), dot(-v,uSpotDir));
        float x = max(1.0 - dist / max(uSpotRange,0.001), 0.0);
        att += x*x*uSpotPower*cone*1.55;
    }
    vec3 color = base * uAmbient * vAo * cavity;
    color += surfaceLight(base,rough,0.0,n,v,v,uLightColor*att) * mix(0.72,1.0,vAo) * sombraPropia;
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
    // 3 bicho que repta, 4 minero caminando. Solo el tipo 1 gira solo.
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
    } else if (tipo > 3.5) {
        // Minero caminando. El reloj es iParams.y, que son los METROS que
        // lleva caminados y no el tiempo: asi las piernas se mueven cuando
        // avanza y se quedan quietas cuando esta parado, sin mandar nada
        // extra por la red.
        //
        // El modelo es una sola malla, asi que las partes se separan por
        // donde estan: lo de abajo de la cintura son las piernas (cada una
        // hacia un lado, por el signo de x) y lo de los costados a la altura
        // del pecho son los brazos, que van al reves que la pierna del mismo
        // lado. La amplitud crece con la altura del modelo, que mide 1: por
        // eso se usa iPosScale.w para pasarla a metros.
        float ciclo = iParams.y * 3.4 + iParams.x;
        float lado = sign(aPos.x);
        float esc = iPosScale.w;
        float cintura = 0.46 * esc;
        float pierna = max(0.0, cintura - p.y) / max(cintura, 0.001);
        p.z += sin(ciclo) * lado * pierna * 0.26 * esc;
        p.y += max(0.0, sin(ciclo) * lado) * pierna * 0.05 * esc;
        // Brazos: los costados, por fuera del ancho del torso (0.175) y por
        // encima de las manos. Debajo de ese ancho todavia es cuerpo.
        float brazo = step(0.175, abs(aPos.x)) * step(0.40 * esc, p.y);
        p.z -= sin(ciclo) * lado * brazo * 0.20 * esc;
        // El cuerpo entero acompana con un balanceo chico.
        p.y += abs(sin(ciclo)) * 0.018 * esc;
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
// Color del arma que se lleva en la mano (madera, hierro, cristal, piedra).
uniform vec3 uArma;

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

    // El arma viaja en la misma malla que los brazos, marcada con aSide = 2:
    // sigue a la mano derecha (envion del golpe incluido) pero no es ni piel
    // ni guante, asi que sale por su propio camino antes de todo lo demas.
    if (vSide > 1.5) {
        float veta = hash(floor(vLocal.zy * 120.0)) * 0.22 - 0.11;
        vec3 mat = uArma * (1.0 + veta);
        vec3 c = mat * (uAmbient * 1.8 + vec3(0.05) + uLightColor * ndl * atten);
        // Un reflejo duro en el canto: es lo que hace que el hierro se lea
        // como hierro y no como un palo pintado de gris.
        float spec = pow(max(dot(reflect(-L, n), vec3(0.0, 0.0, 1.0)), 0.0), 26.0);
        c += uLightColor * spec * 0.30 * atten;
        c *= uBrightness;
        c = c / (c + vec3(0.85));
        fragColor = vec4(pow(c, vec3(1.0 / 2.2)), 1.0);
        return;
    }

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
    // ------------------------------------------------------------------- agua
    //
    // El agua de la cueva: un plano horizontal que refleja lo que tiene
    // encima. No hay una segunda pasada de camara —en un telefono eso es
    // duplicar el costo de dibujar la cueva entera— sino el reflejo que de
    // verdad se ve en un charco a oscuras: las LUCES. En una cueva sin cielo
    // ni paisaje, lo que aparece en el agua es la antorcha del jugador, las
    // antorchas de la pared y los cristales, estirados en una columna
    // temblorosa. Eso es lo que hace que se lea como agua.
    const val WATER_VS = """#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in float aHondura;

uniform mat4 uViewProj;

out vec3 vWorld;
out float vHondura;

void main() {
    vWorld = aPos;
    vHondura = aHondura;
    gl_Position = uViewProj * vec4(aPos, 1.0);
}
"""

    const val WATER_FS = """#version 300 es
precision highp float;

in vec3 vWorld;
in float vHondura;

uniform vec3 uCamPos;
uniform float uTime;
uniform vec3 uLightColor;
uniform float uLightRadius;
uniform float uLightIntensity;
uniform vec3 uAmbient;
uniform vec3 uFogColor;
uniform float uFogDensity;
uniform vec3 uAgua;          // color propio del agua del bioma
uniform float uBrightness;
uniform int uQuality;

#define MAX_LUCES 8
uniform vec4 uLuzPos[MAX_LUCES];
uniform vec3 uLuzColor[MAX_LUCES];
uniform int uNumLuces;

out vec4 fragColor;

// Ruido de valor barato, para las ondas. Dos capas que se mueven en
// direcciones distintas: una sola se lee como una tela arrastrandose.
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float ruido(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

/** Normal de la superficie con las ondas encima. */
vec3 normalDelAgua(vec2 p) {
    if (uQuality <= 0) return vec3(0.0, 1.0, 0.0);
    float e = 0.12;
    // Dos trenes de onda cruzados, a distinta velocidad y escala.
    float t = uTime;
    vec2 a = p * 1.7 + vec2(t * 0.13, t * 0.09);
    vec2 b = p * 3.3 - vec2(t * 0.07, t * 0.17);
    float h  = ruido(a) * 0.6 + ruido(b) * 0.4;
    float hx = ruido(a + vec2(e, 0.0)) * 0.6 + ruido(b + vec2(e, 0.0)) * 0.4;
    float hz = ruido(a + vec2(0.0, e)) * 0.6 + ruido(b + vec2(0.0, e)) * 0.4;
    float fuerza = (uQuality >= 2) ? 0.42 : 0.24;
    return normalize(vec3(-(hx - h) / e * fuerza, 1.0, -(hz - h) / e * fuerza));
}

/**
 * El brillo de una luz reflejado en el agua.
 *
 * Es un especular GGX comun, pero con la rugosidad muy baja: eso es lo que
 * estira el reflejo de la antorcha en una columna larga y tembleque sobre el
 * agua, en vez de dejar un puntito.
 */
vec3 reflejoDe(vec3 n, vec3 v, vec3 l, vec3 radiancia) {
    vec3 h = normalize(v + l);
    float nh = max(dot(n, h), 0.0);
    float nl = max(dot(n, l), 0.0);
    float a = 0.055;
    float a2 = a * a;
    float d = nh * nh * (a2 - 1.0) + 1.0;
    float dist = a2 / max(3.141593 * d * d, 0.00001);
    return radiancia * dist * nl;
}

void main() {
    vec3 haciaOjo = uCamPos - vWorld;
    float dist = length(haciaOjo);
    vec3 v = haciaOjo / max(dist, 0.0001);
    vec3 n = normalDelAgua(vWorld.xz);

    // Fresnel: de frente el agua es casi transparente y se ve el fondo; de
    // costado es un espejo. Es lo que mas hace por que parezca agua, mas que
    // cualquier reflejo.
    float fresnel = 0.03 + 0.97 * pow(1.0 - max(dot(n, v), 0.0), 5.0);

    // --- lo que se ve a traves: el agua tiñe mas cuanto mas honda.
    float turbio = clamp(vHondura / 0.30, 0.0, 1.0);
    vec3 fondo = uAgua * mix(0.55, 1.0, turbio) * (uAmbient * 3.0 + 0.05);

    // --- lo que se refleja: las luces de la cueva y la antorcha del jugador.
    float att = max(1.0 - dist / max(uLightRadius, 0.001), 0.0);
    vec3 reflejo = reflejoDe(n, v, v, uLightColor * att * att * uLightIntensity);
    for (int i = 0; i < MAX_LUCES; i++) {
        if (i >= uNumLuces) break;
        vec3 delta = uLuzPos[i].xyz - vWorld;
        float d = length(delta);
        float radio = max(uLuzPos[i].w, 0.001);
        float k = max(1.0 - d / radio, 0.0);
        reflejo += reflejoDe(n, v, delta / max(d, 0.0001), uLuzColor[i] * k * k);
    }

    vec3 color = mix(fondo, fondo * 0.25 + reflejo, fresnel) + reflejo * 0.35;

    // La niebla tiene que ser la MISMA que la de la cueva: si el agua no se
    // apagara con la distancia igual que la roca, un charco lejano se veria
    // flotando encima de la niebla como un recorte.
    float fog = 1.0 - exp(-uFogDensity * uFogDensity * dist * dist);
    color = mix(color, uFogColor, clamp(fog, 0.0, 1.0));

    color = max(color * uBrightness, vec3(0.0));
    color = clamp((color * (2.51 * color + 0.03)) / (color * (2.43 * color + 0.59) + 0.14), 0.0, 1.0);
    // El borde del charco se desvanece: un canto duro delata el cuadrado de
    // la casilla y arruina la ilusion de que el agua siguio el terreno.
    float alfa = mix(0.35, 0.93, turbio);
    fragColor = vec4(pow(color, vec3(1.0 / 2.2)), alfa);
}
"""

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

