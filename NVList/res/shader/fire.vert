#version 110

uniform float time;
uniform vec4 screen;

varying vec3 fc;

void main() {
    gl_TexCoord[0] = gl_MultiTexCoord0;        
    gl_Position = ftransform(); //gl_ProjectionMatrix * gl_ModelViewMatrix * gl_Vertex;
	gl_FrontColor = gl_Color;
	
    vec2 screenPos = (gl_Position.x, screen.a - gl_Position.y) - screen.xy;
    vec2 normScreenPos = screenPos.xy / screen.ba;
    
	vec2 tpos = normScreenPos.xy * gl_TexCoord[0].st;
	float s = (0.5 + 0.3 * tpos.y) + sin(time * 0.1) * 0.3;
	fc = s * vec3(0.6 + tpos.y * 0.3, tpos.y * 0.1, 0);
}
