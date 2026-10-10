#version 330

layout(std140) uniform WatutBlurInfo {
    vec2 resolution;
    float radius;
    float blurLevel;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);

    float r = radius;
    float x,y,xx,yy,rr=r*r,dx,dy,w,w0;
    float xs = resolution[0];
    float ys = resolution[1];
    vec2 pixelCoord = gl_FragCoord.xy;
    float xmid = xs / 2;
    float ymid = ys / 2;
    w0=0.3780/pow(r,1.975);
    vec2 p;
    vec2 pos = texCoord0;
    vec4 col=vec4(0.0,0.0,0.0,0.0);
    for (dx=1.0/xs, x=-r, p.x=(pos.x)+(x*dx); x<=r; x++, p.x+=dx) {
        xx=x*x;
        for (dy=1.0/ys, y=-r, p.y=(pos.y)+(y*dy); y<=r; y++, p.y+=dy) {
            yy=y*y;
            if (xx+yy<=rr) {
                w=w0*exp((-xx-yy)/(2.0*rr));
                col+=texture(Sampler0,p)*w;
            }
        }
    }
    if (r == 0) {
        col=texture(Sampler0, texCoord0);
    }
    int cutoff = 128;
    int cutoff2 = 64;
    float dist = distance(pixelCoord, vec2(xmid, ymid));
    if (dist > cutoff) {
        col.a = min(col.a, 1 - ((dist - cutoff) / cutoff2));
    }
    if (col.a <= 0.0) {
        discard;
    }
    fragColor = col;
}
