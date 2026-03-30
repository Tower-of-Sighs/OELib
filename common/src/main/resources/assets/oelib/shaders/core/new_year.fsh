#version 330

#moj_import <dynamictransforms.glsl>

layout(std140) uniform Time {
    vec4 UboTime;
};
layout(std140) uniform Resolution {
    vec4 UboResolution;
};
layout(std140) uniform ToastAlpha {
    vec4 UboToastAlpha;
};
layout(std140) uniform MouseUV {
    vec4 UboMouseUV;
};

in vec2 texCoord0;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.545);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

#define PI 3.141592653589793238
#define TWOPI 6.283185307179586
#define S(x,y,z) smoothstep(x,y,z)
#define B(x,y,z,w) S(x - z, x + z, w) * S(y + z, y - z, w)
#define saturate(x) clamp(x, 0.0, 1.0)

#define NUM_EXPLOSIONS 8.0
#define NUM_PARTICLES 70.0

#define MOD3 vec3(0.1031, 0.11369, 0.13787)
vec3 hash31(float p) {
    vec3 p3 = fract(vec3(p) * MOD3);
    p3 += dot(p3, p3.yzx + 19.19);
    return fract(vec3((p3.x + p3.y) * p3.z, (p3.x + p3.z) * p3.y, (p3.y + p3.z) * p3.x));
}
float hash12(vec2 p){
    vec3 p3  = fract(vec3(p.xyx) * MOD3);
    p3 += dot(p3, p3.yzx + 19.19);
    return fract((p3.x + p3.y) * p3.z);
}
float circ(vec2 uv, vec2 pos, float size) {
    uv -= pos;
    size *= size;
    return S(size * 1.1, size, dot(uv, uv));
}
float light(vec2 uv, vec2 pos, float size) {
    uv -= pos;
    size *= size;
    return size / dot(uv, uv);
}
vec3 explosion(vec2 uv, vec2 p, float seed, float t) {
    vec3 col = vec3(0.0);
    vec3 en = hash31(seed);
    vec3 baseCol = en;
    for (float i = 0.0; i < NUM_PARTICLES; i += 1.0) {
        vec3 n = hash31(i) - 0.5;
        vec2 startP = p - vec2(0.0, t * t * 0.1);
        float FW_SCALE = 1.12;
        vec2 endP = startP + normalize(n.xy) * n.z * FW_SCALE;
        float pt = 1.0 - pow(t - 1.0, 2.0);
        vec2 pos = mix(p, endP, pt);
        float size = mix(0.01, 0.005, S(0.0, 0.1, pt));
        size *= S(1.0, 0.1, pt);
        float sparkle = (sin((pt + n.z) * 100.0) * 0.5 + 0.5);
        sparkle = pow(sparkle, pow(en.x, 3.0) * 50.0) * 0.01;
        size += sparkle * B(en.x, en.y, en.z, t);
        col += baseCol * light(uv, pos, size * 1.08);
    }
    return col;
}

void main() {
    vec2 uv = texCoord0;
    float t = UboTime.x;

    vec2 centered = uv * 2.0 - 1.0;

    float paper = noise(uv * 6.0) * 0.12;
    vec3 base = vec3(0.45, 0.05, 0.05) + paper;

    float flow = noise(vec2(uv.x * 3.0 - t * 0.4, uv.y * 6.0));
    float gold = smoothstep(0.55, 0.85, flow);
    vec3 goldCol = vec3(1.0, 0.78, 0.25) * gold * 0.9;

    float cloud = smoothstep(0.02, 0.0, abs(noise(uv * 10.0 + vec2(0.0, t * 0.3)) - 0.5));
    vec3 cloudCol = vec3(1.0, 0.85, 0.5) * cloud * 0.4;

    float lx = uv.x * 2.0 - 0.2;
    float ly = uv.y * 2.0 - 1.0;
    float lantern = smoothstep(0.25, 0.23, length(vec2(lx, ly)));
    vec3 lanternCol = vec3(1.0, 0.4, 0.2) * lantern * 0.5;

    vec2 m = abs(uv - vec2(0.5, 0.5));
    float textMask = smoothstep(0.22, 0.18, m.x) * smoothstep(0.20, 0.16, m.y);
    vec3 textGlow = goldCol * (1.0 - textMask) * 1.2;

    float aspectRatio = UboResolution.x / UboResolution.y;
    vec2 uvBW = uv;
    uvBW.x -= 0.5;
    uvBW.x *= aspectRatio;
    vec3 fwCol = vec3(0.0);
    float tt = UboTime.x * 0.5;
    for (float i = 0.0; i < NUM_EXPLOSIONS; i += 1.0) {
        float et = tt + i * 1234.45235;
        float id = floor(et);
        float etr = et - id;
        vec2 p = hash31(id).xy;
        p.x -= 0.5;
        p.x *= 1.6;
        fwCol += explosion(uvBW, p, id, etr);
    }

    float r = length(uv - vec2(0.5, 0.5));
    float vignette = smoothstep(0.4, 0.9, r);
    vignette = 1.0 - vignette;

    vec3 col = base;
    col += goldCol;
    col += cloudCol;
    col += lanternCol;
    col += textGlow;
    col += fwCol;

    col *= vignette * 0.6 + 0.4;

    vec2 q = abs(centered) - vec2(1.0, 1.0) + 0.15;
    float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0);
    float softMask = 1.0 - smoothstep(0.0, 0.2, dist);

    float alpha = UboToastAlpha.x * softMask * 0.9;
    fragColor = vec4(col * alpha, alpha) * ColorModulator;
}
