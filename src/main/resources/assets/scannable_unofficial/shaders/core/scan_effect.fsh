#version 330
#extension GL_ARB_separate_shader_objects : require

// Fullscreen scan-reveal effect. Reconstructs world position from the main depth buffer and
// paints an additive expanding spherical shell ("ping wave") onto whatever surface it crosses.
// Ported from the 1.21.1 ShaderInstance shader to the 26.1.2 RenderPipeline/UBO convention.

uniform sampler2D Sampler0;

// Per-frame scan parameters (see ScannerRenderer).
layout(std140) uniform ScanInfo {
    mat4 InvProjMat;  // inverse of the camera projection matrix
    mat4 InvViewMat;  // inverse of the camera view-rotation matrix
    vec4 Center;      // xyz = scan centre (world)
    vec4 CameraPos;   // xyz = camera position (world)
    vec4 Params;      // x = current wave radius
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

const float width = 10.0;
const float sharpness = 10.0;
const vec4 outerColor = vec4(0.8, 1.0, 0.9, 1.0);
const vec4 midColor = vec4(0.4, 0.5, 0.7, 1.0);
const vec4 innerColor = vec4(0.1, 0.4, 0.9, 1.0);
const vec4 scanlineColor = vec4(0.6, 1.0, 0.2, 1.0);

float scanlines() {
    return sin(gl_FragCoord.y) * 0.5 + 0.5;
}

vec3 worldpos(float depth) {
    float z = depth;
    vec4 clipSpacePosition = vec4(texCoord * 2.0 - 1.0, z, 1.0);
    vec4 viewSpacePosition = InvProjMat * clipSpacePosition;
    viewSpacePosition /= viewSpacePosition.w;
    vec4 worldSpacePosition = InvViewMat * viewSpacePosition;
    return CameraPos.xyz + worldSpacePosition.xyz;
}

void main() {
    vec4 color = vec4(0.0);

    float depth = texture(Sampler0, texCoord).r;
    // In Reverse-Z (Minecraft 26.3), 0.0 represents the sky/far plane.
    if (depth > 0.0001 && depth <= 1.0) {
        vec3 pos = worldpos(depth);
        float dist = distance(pos, Center.xyz);
        float radius = Params.x;

        if (dist < radius && dist > radius - width) {
            float diff = 1.0 - (radius - dist) / width;
            vec4 edge = mix(midColor, outerColor, pow(diff, sharpness));
            color = mix(innerColor, edge, diff) + scanlines() * scanlineColor;
            color *= diff;
        }
    }

    fragColor = color;
}
