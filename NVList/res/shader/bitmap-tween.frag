//Tweens from one texture to another using a grayscale bitmap to control the
//relative fade time of each pixel

#version 110

uniform vec4 crop0; //u0, u1, v0, v1
uniform vec4 crop1; //u0, u1, v0, v1

uniform sampler2D src0;
uniform sampler2D src1;
uniform sampler2D fade;
uniform sampler2D remap;

vec4 croppedTexture2D(sampler2D tex, vec2 pos, vec4 crop) {
    if (pos.x >= crop.s && pos.x <= crop.t && pos.y >= crop.p && pos.y <= crop.q) {
        return texture2D(tex, pos);
    } else {
        return vec4(0, 0, 0, 0);
    }
}

void main() {
    vec4 c0 = croppedTexture2D(src0, gl_TexCoord[0].st, crop0);
    vec4 c1 = croppedTexture2D(src1, gl_TexCoord[1].st, crop1);

    vec2 aa = texture2D(remap, texture2D(fade, gl_TexCoord[2].st).bg).bg;
    float alpha = (aa.y * 256.0 + aa.x) / 256.0;
	alpha = clamp(alpha, 0.0, 1.0);
    
    vec4 c;

	//Premultiply with alpha and blend colors
    c.rgb = mix(c0.rgb * c0.a, c1.rgb * c1.a, alpha); 
	//Blend alphas
	c.a = mix(c0.a, c1.a, alpha);
	//Reverse premultiply alpha
    c.rgb = c.rgb / c.a;
    
	//Multiply with glColor and clamp (otherwise the result might overflow)
    gl_FragColor = clamp(gl_Color * c, 0.0, 1.0);
}
