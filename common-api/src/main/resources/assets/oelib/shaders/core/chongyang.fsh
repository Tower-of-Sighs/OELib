#version 150

uniform float Time;
uniform vec2 Resolution;
uniform vec4 ColorModulator;
uniform float ToastAlpha;

in vec2 texCoord0;
out vec4 fragColor;

float hash(float n) { return fract(sin(n) * 43758.5453123); }

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec2 hash21(float p) {
    vec3 p3 = fract(vec3(p) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

float noise(vec2 p) {
    vec2 ip = floor(p);
    vec2 fp = fract(p);
    fp = smoothstep(0.0, 1.0, fp);
    float n00 = hash12(ip + vec2(0.0, 0.0));
    float n10 = hash12(ip + vec2(1.0, 0.0));
    float n01 = hash12(ip + vec2(0.0, 1.0));
    float n11 = hash12(ip + vec2(1.0, 1.0));
    float nx0 = mix(n00, n10, fp.x);
    float nx1 = mix(n01, n11, fp.x);
    return mix(nx0, nx1, fp.y);
}

float fbm(vec2 p, int lv) {
    float a = 1.0;
    float t = 0.0;
    for (int i = 0; i < lv; i++) {
        p += vec2(13.102, 1.535);
        t += a * noise(p);
        p *= mat2(3.0, 4.0, -4.0, 3.0) * 0.4;
        a *= 0.5;
    }
    return 0.5 * t;
}

vec3 mountainScene(vec2 fragCoord, vec2 resolution, float time) {
    vec2 uv = (2.0 * fragCoord - resolution.xy) / resolution.y;

    float mtHeight = fbm(uv.xx * 1.5 + 0.6, 8) * 1.5;
    float mtHeightSm = fbm(uv.xx * 1.5 + 0.6, 3);

    mtHeight = pow(mtHeight, 1.2);
    mtHeightSm = pow(mtHeightSm, 1.2);

    vec2 sunPos = vec2(0.8, -0.8);
    vec3 skyCol = vec3(0.075, 0.310, 0.518);

    float q = uv.y - sunPos.y;
    float q2 = uv.x - sunPos.x;

    skyCol = mix(skyCol, vec3(0.482, 0.580, 0.902), exp(-0.5 * q * q - 0.2 * q2 * q2));
    skyCol = mix(skyCol, vec3(0.706, 0.851, 0.953), exp(-3.0 * q * q - 0.5 * q2 * q2));
    skyCol = mix(skyCol, vec3(0.980, 0.5, 0.3), exp(-10.0 * q * q - 0.5 * q2 * q2));
    skyCol = mix(skyCol, vec3(1.0, 1.0, 0.7), exp(-3.0 * length(uv - sunPos)));

    vec3 mountainBaseColor = vec3(0.15, 0.28, 0.32);
    vec3 mountainTopColor  = vec3(0.22, 0.38, 0.45);
    vec3 mountainShadow    = vec3(0.08, 0.15, 0.20);

    float mountainY = uv.y + 0.3 * uv.x + 1.0 - mtHeight;
    float isMountain = 1.0 - smoothstep(-0.1, 0.1, mountainY);

    vec3 mountainCol = mix(mountainBaseColor, mountainTopColor, clamp(1.0 - uv.y, 0.0, 1.0));
    mountainCol = mix(mountainCol, mountainShadow, 0.4 * (1.0 - mtHeight));

    vec3 lightDir = normalize(vec3(1.0, 1.0, 1.0));
    vec3 normal = normalize(vec3(dFdx(mountainY), dFdy(mountainY), 1.0));
    float diffuse = max(dot(normal, lightDir), 0.3);
    mountainCol *= diffuse;

    float fog = smoothstep(0.0, 1.0, mountainY + 0.2);
    mountainCol = mix(mountainCol, skyCol * 0.75, fog * 0.3);

    vec3 col = mix(mountainCol, skyCol, 1.0 - isMountain);

    float w = 1.5 * length(fwidth(uv));
    float mountainEdge = uv.y + 0.3 * uv.x + 0.2 * max(uv.x, 0.0) + 1.0 - mtHeightSm;
    float isSky = smoothstep(0.0, w, mountainEdge);
    col = mix(col, skyCol, isSky);

    vec2 fuv = fract(0.1 * uv);
    vec2 uvv = 20.0 * fuv * (1.0 - fuv) * (0.5 - fuv);
    uvv = vec2(1.0, -1.0) * uvv.yx;
    vec2 uv2 = uv + uvv * cos(0.1 * time);

    float silver = fbm(30.0 * uv - 0.06 * time, 8) + 30.0 * (uv.y + 0.8);
    silver = smoothstep(0.0, 1.0, silver) * smoothstep(2.0, 1.0, silver) * 1.0 / (1.0 + 500.0 * q2 * q2) * isSky;
    col += silver * vec3(0.9, 0.6, 0.3) * 100.0;

    col = mix(col, vec3(0.9, 0.6, 0.3) * 100.0, 1.0 / (1.0 + 2000.0 * (q * q + q2 * q2)));
    col += vec3(0.9, 0.6, 0.3) * 2.0 / (1.0 + 10.0 * sqrt(q * q + 0.3 * q2 * q2));

    col = mix(col, col * pow(col / (col.r + col.g + col.b + 1e-6), vec3(dot(uv, uv) * 0.3)), 0.2);

    col = pow(col, vec3(2.2));
    col = (col * (2.51 * col + 0.03)) / (col * (2.43 * col + 0.59) + 0.14);
    col = pow(col, vec3(1.0 / 2.2));

    col += 0.03 * (hash12(fragCoord) - 0.5) * sqrt(resolution.y / 400.0);

    return col;
}

float cloudNoise(vec2 p, float t) {
    float final = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 3; i++) {
        p += sin(p.yx * 1.5 + t);
        final += amp * abs(sin(p.x + p.y));
        amp *= 0.5;
    }
    return final;
}

void main() {
    vec2 uv = texCoord0;
    vec2 centered = uv * 2.0 - 1.0;
    float aspect = Resolution.x / max(1.0, Resolution.y);
    vec2 fragCoord = uv * Resolution;
    vec2 p = centered;
    p.x *= aspect;

    vec3 baseColor = mountainScene(fragCoord, Resolution, Time);

    vec3 skyColor = vec3(0.4, 0.7, 1.0);
    vec3 jadeColor = vec3(0.6, 0.9, 0.8);
    vec3 goldColor = vec3(1.0, 0.85, 0.5);
    vec3 dawnColor = vec3(1.0, 0.4, 0.4);

    float cloud = cloudNoise(uv * 2.0, Time * 0.3);
    vec3 overlayGradient = mix(skyColor * 0.15, jadeColor * 0.4, uv.y + cloud * 0.2);
    vec3 color = mix(baseColor, overlayGradient, 0.35);
    color += dawnColor * pow(cloud, 3.0) * 0.1;

    float particles = 0.0;
    for (float i = 0.0; i < 22.0; i++) {
        float seed = hash(i * 157.1);
        float t = Time * (0.4 + seed * 0.6) + seed * 6.28;

        float radiusX = 1.8 + 0.5 * sin(t * 0.5);
        float radiusZ = 1.2 + seed;
        float height = 0.6 * cos(t * 0.8 + seed * 10.0);

        vec3 p3d = vec3(cos(t) * radiusX, height, sin(t) * radiusZ);

        float perspective = 1.0 / (2.5 + p3d.z);
        vec2 p2d = p3d.xy * perspective * 1.5;

        float d = length(p - p2d);
        float size = 0.006 * perspective;
        float glow = size / (d * d + 0.0005);
        float fade = smoothstep(-1.5, 1.0, p3d.z);
        particles += glow * fade * (0.6 + 0.4 * sin(Time * 4.0 + seed));
    }
    color += goldColor * particles * 0.7;

    float border = smoothstep(0.98, 0.8, length(centered * vec2(0.5, 0.9)));
    float borderSparkle = pow(cloudNoise(uv * 5.0, Time), 2.0);
    color += jadeColor * borderSparkle * border * 0.4;

    vec2 q = abs(centered) - vec2(1.0, 1.0) + 0.15;
    float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0);
    float softMask = 1.0 - smoothstep(0.0, 0.2, dist);

    float finalAlpha = ToastAlpha * softMask * 0.9;

    fragColor = vec4(color * finalAlpha, finalAlpha) * ColorModulator;
}