#version 150

uniform float Time;
uniform vec2 Resolution;
uniform vec4 ColorModulator;
uniform float ToastAlpha;
uniform vec2 MouseUV;

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

void main() {
    vec2 uv = texCoord0;
    float t = Time;

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

    float r = length(uv - vec2(0.5, 0.5));
    float vignette = smoothstep(0.4, 0.9, r);
    vignette = 1.0 - vignette;

    vec3 col = base;
    col += goldCol;
    col += cloudCol;
    col += lanternCol;
    col += textGlow;

    col *= vignette * 0.6 + 0.4;

    vec2 q = abs(centered) - vec2(1.0, 1.0) + 0.15;
    float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0);
    float softMask = 1.0 - smoothstep(0.0, 0.2, dist);

    float alpha = ToastAlpha * softMask * 0.9;
    fragColor = vec4(col * alpha, alpha) * ColorModulator;
}
