#version 110

uniform float time;
uniform vec4 screen;

varying vec3 fc;

void main() {
    gl_TexCoord[0] = gl_MultiTexCoord0;        
    gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix * gl_Vertex;
	
    vec2 screenPos = (gl_Position.x, screen.a - gl_Position.y) - screen.xy;
    vec2 normScreenPos = screenPos.xy / screen.ba;
	        
    float s = clamp(normScreenPos.y * 0.8, 0.0, 1.0);	
	gl_FrontColor = gl_Color - vec4(s, s, s, 0.0);
	
    s = clamp(1.0 + sin(fract(time) * 7.0) * cos(time * 3.0), 0.25, 3.5);
    fc = s * vec3(normScreenPos.y * 0.3 + 0.5, normScreenPos.y * 0.3 + 0.1, 0.0);	
}
