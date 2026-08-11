#version 150

uniform float Time;
uniform vec2 Resolution;
uniform vec4 ColorModulator;
uniform float ToastAlpha;

in vec2 texCoord0;
out vec4 fragColor;

#define S(a, b, t) smoothstep(a, b, t)
#define sat(x) clamp(x, 0.0, 1.0)
#define HEARTCOL vec3(1.0, 0.01, 0.01)
#define NUM_HEARTS 28.0
#define LIGHT_DIR vec3(0.577, -0.577, -0.577)

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

float smax(float a, float b, float k) {
    float h = sat(0.5 + 0.5 * (b - a) / k);
    return mix(a, b, h) + k * h * (1.0 - h);
}

vec2 hash2(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)));
    return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
    mix(dot(hash2(i), f),
    dot(hash2(i + vec2(1.0, 0.0)), f - vec2(1.0, 0.0)), u.x),
    mix(dot(hash2(i + vec2(0.0, 1.0)), f - vec2(0.0, 1.0)),
    dot(hash2(i + vec2(1.0, 1.0)), f - vec2(1.0, 1.0)), u.x),
    u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p *= 2.0;
        a *= 0.5;
    }
    return v;
}

vec4 qmulq(vec4 q1, vec4 q2) {
    return vec4(q1.xyz * q2.w + q2.xyz * q1.w + cross(q1.xyz, q2.xyz),
    (q1.w * q2.w) - dot(q1.xyz, q2.xyz));
}

vec4 aa2q(vec3 axis, float angle) {
    return vec4(normalize(axis) * sin(angle * 0.5), cos(angle * 0.5));
}

vec4 qinv(vec4 q) {
    return vec4(-q.xyz, q.w) / dot(q, q);
}

vec3 qmulv(vec4 q, vec3 p) {
    return qmulq(q, qmulq(vec4(p, 0.0), qinv(q))).xyz;
}

vec2 RaySphere(vec3 rd, vec3 p) {
    float l = dot(rd, p);
    float det = l * l - dot(p, p) + 1.0;
    if (det < 0.0) {
        return vec2(-1.0);
    }
    float sd = sqrt(det);
    return vec2(l - sd, l + sd);
}

struct SphereInfo {
    vec3 p1;
    vec3 p2;
    vec3 n1;
    vec3 n2;
    vec2 uv1;
    vec2 uv2;
};

SphereInfo GetSphereUvs(vec3 rd, vec2 i, vec2 rot, vec3 s) {
    SphereInfo res;
    rot *= 6.2831;
    vec4 q = aa2q(vec3(cos(rot.x), sin(rot.x), 0.0), rot.y);
    vec3 o = qmulv(q, -s) + s;
    vec3 d = qmulv(q, rd);

    res.p1 = rd * i.x;
    vec3 p1 = o + d * i.x - s;
    res.uv1 = vec2(atan(p1.x, p1.z), p1.y);
    res.n1 = res.p1 - s;

    res.p2 = rd * i.y;
    vec3 p2 = o + d * i.y - s;
    res.uv2 = vec2(atan(p2.x, p2.z), p2.y);
    res.n2 = s - res.p2;

    return res;
}

float Heart(vec2 uv, float b) {
    uv.x *= 0.5;
    float shape = smax(sqrt(abs(uv.x)), b, 0.3 * b) * 0.5;
    uv.y -= shape * (1.0 - b);
    return S(b, -b, length(uv) - 0.5);
}

vec4 HeartBall(vec3 rd, vec3 p, vec2 rot, float t, float blur) {
    vec2 d = RaySphere(rd, p);
    vec4 col = vec4(0.0);
    if (d.x > 0.0) {
        SphereInfo info = GetSphereUvs(rd, d, rot, p);
        float sd = length(cross(p, rd));
        float edge = S(1.0, mix(1.0, 0.1, blur), sd);

        float backMask = Heart(info.uv2, blur) * edge;
        float frontMask = Heart(info.uv1, blur) * edge;
        float frontLight = sat(dot(LIGHT_DIR, info.n1) * 0.8 + 0.2);
        float backLight = sat(dot(LIGHT_DIR, info.n2) * 0.8 + 0.2) * 0.9;

        vec4 backCol = vec4(backLight * HEARTCOL, backMask);
        vec4 frontCol = vec4(frontLight * HEARTCOL, frontMask);
        col = mix(backCol, frontCol, frontCol.a);
    }
    return col;
}

float silkNoise(vec2 p, float t) {
    float v = 0.0;
    float a = 0.6;
    for (int i = 0; i < 3; i++) {
        p += sin(p.yx * 1.3 + t);
        v += a * abs(sin(p.x + p.y));
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = texCoord0;
    vec2 centered = uv * 2.0 - 1.0;
    float aspect = Resolution.x / max(1.0, Resolution.y);
    vec2 p = centered;
    p.x *= aspect;

    float t = Time * 0.4;

    float silk = silkNoise(uv * vec2(1.4, 2.6), t);
    vec3 bg = mix(vec3(0.18, 0.02, 0.04),
    vec3(0.6, 0.08, 0.15),
    silk);
    bg += vec3(1.0, 0.3, 0.4) * pow(silk, 4.0) * 0.25;

    vec3 rd = normalize(vec3(p, 1.0));

    vec4 hearts = vec4(0.0);
    for (float i = 0.0; i < 1.0; i += 1.0 / NUM_HEARTS) {
        float r1 = fract(sin(i * 123.4) * 4567.8);
        float r2 = fract(sin(i * 987.6) * 1357.9);
        float r3 = fract(sin(i * 246.8) * 9753.1);
        float r4 = fract(sin(i * 642.0) * 8642.3);

        vec2 anchor = vec2(
        mix(-1.15, 1.15, r1),
        mix(-0.75, 0.75, r2)
        );

        vec3 dirAnchor = normalize(vec3(anchor.x * aspect, anchor.y, 1.0));

        float depthFactor = pow(r3, 0.35);
        float dist = mix(4.5, 8.0, depthFactor);

        float phase = r4 * 6.2831;
        vec3 offset = vec3(
        sin(t * 0.7 + phase) * 0.6,
        cos(t * 0.6 + phase * 1.3) * 0.6,
        sin(t * 0.5 + phase * 0.7) * 0.5
        );

        vec3 center = dirAnchor * dist + offset;

        float blur = mix(0.08, 0.30, depthFactor);

        vec2 rot = t * vec2(0.12, 0.18) + vec2(r1, r2) * 6.2831;
        vec4 heart = HeartBall(rd, center, rot, t, blur);
        hearts = mix(hearts, heart, heart.a);
    }

    vec3 color = mix(bg, hearts.rgb, hearts.a);

    vec2 q = abs(centered) - vec2(1.0, 1.0) + 0.15;
    float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0);
    float softMask = 1.0 - smoothstep(0.0, 0.2, dist);

    float alpha = ToastAlpha * softMask * 0.9;
    fragColor = vec4(color * alpha, alpha) * ColorModulator;
}