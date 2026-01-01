#version 150

uniform float Time;
uniform vec2 Resolution;
uniform vec4 ColorModulator;
uniform float ToastAlpha;

in vec2 texCoord0;
out vec4 fragColor;

float hash(float n) { return fract(sin(n) * 43758.5453123); }

float cloudNoise(vec2 p, float t) {
    float final = 0.0;
    float amp = 0.5;
    for (int i=0; i<3; i++) {
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
    vec2 p = centered;
    p.x *= aspect;

    vec3 skyColor = vec3(0.4, 0.7, 1.0);
    vec3 jadeColor = vec3(0.6, 0.9, 0.8);
    vec3 goldColor = vec3(1.0, 0.85, 0.5);
    vec3 dawnColor = vec3(1.0, 0.4, 0.4);

    float cloud = cloudNoise(uv * 2.0, Time * 0.3);
    vec3 color = mix(skyColor * 0.15, jadeColor * 0.4, uv.y + cloud * 0.2);
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