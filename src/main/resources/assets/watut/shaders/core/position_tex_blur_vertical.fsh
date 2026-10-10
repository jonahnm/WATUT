#version 330

layout(std140) uniform WatutBlurInfo {
    vec2 resolution;
    float radius;
    float blurLevel;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;

out vec4 fragColor;

const float weight[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main() {
    vec4 color = texture(Sampler0, texCoord0) * weight[0];
    float dy = 1.0 / resolution.y;
    for (int i = 1; i < 5; i++) {
        color += texture(Sampler0, texCoord0 + vec2(0.0, dy * float(i))) * weight[i];
        color += texture(Sampler0, texCoord0 - vec2(0.0, dy * float(i))) * weight[i];
    }
    //visual cutoff radius from the center
    float xs = resolution.x;
    float ys = resolution.y;
    float xmid = xs / 2;
    float ymid = ys / 2;
    int cutoff = 128;
    int cutoff2 = 64;
    float dist = distance(gl_FragCoord.xy, vec2(xmid, ymid));
    if (dist > cutoff) {
        color.a = min(color.a, 1 - ((dist - cutoff) / cutoff2));
    }
    if (color.a <= 0.0) {
        discard;
    }
    fragColor = color;
}
