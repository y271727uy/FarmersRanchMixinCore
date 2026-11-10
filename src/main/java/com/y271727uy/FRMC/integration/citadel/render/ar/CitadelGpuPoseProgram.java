package com.y271727uy.FRMC.integration.citadel.render.ar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL45C.*;

public final class CitadelGpuPoseProgram {
    private CitadelGpuPoseProgram() {
    }

    public static int compileProgram() throws IOException {
        return compileProgram("citadel_pose.comp");
    }

    static int compileProgram(String shaderName) throws IOException {
        return compileProgram(shaderName, "");
    }

    public static int compileProgram(String shaderName, String defines) throws IOException {
        String source;
        try (InputStream resource = CitadelGpuPoseProgram.class
                .getResourceAsStream("/assets/frmc/shaders/compute/" + shaderName)) {
            if (resource == null) {
                throw new IOException("Missing Citadel compute shader " + shaderName);
            }
            source = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }
        int versionEnd = source.indexOf('\n') + 1;
        int shader = glCreateShader(GL_COMPUTE_SHADER);
        try {
            glShaderSource(shader, source.substring(0, versionEnd), defines, source.substring(versionEnd));
            glCompileShader(shader);
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                throw new IllegalStateException(glGetShaderInfoLog(shader));
            }
            int program = glCreateProgram();
            glAttachShader(program, shader);
            glLinkProgram(program);
            if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                String error = glGetProgramInfoLog(program);
                glDeleteProgram(program);
                throw new IllegalStateException(error);
            }
            return program;
        } finally {
            glDeleteShader(shader);
        }
    }

    public record BufferBinding(int buffer, long offset, long size) {
        public static BufferBinding capture(int index) {
            return new BufferBinding(glGetIntegeri(GL_SHADER_STORAGE_BUFFER_BINDING, index),
                    glGetInteger64i(GL_SHADER_STORAGE_BUFFER_START, index),
                    glGetInteger64i(GL_SHADER_STORAGE_BUFFER_SIZE, index));
        }

        public void restore(int index) {
            if (buffer != 0 && size > 0) {
                glBindBufferRange(GL_SHADER_STORAGE_BUFFER, index, buffer, offset, size);
            } else {
                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, index, buffer);
            }
        }
    }
}
