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
    float dx = 1.0 / resolution.x;
    for (int i = 1; i < 5; i++) {
        color += texture(Sampler0, texCoord0 + vec2(dx * float(i), 0.0)) * weight[i];
        color += texture(Sampler0, texCoord0 - vec2(dx * float(i), 0.0)) * weight[i];
    }
    if (color.a <= 0.0) {
        discard;
    }
    fragColor = color;
}
