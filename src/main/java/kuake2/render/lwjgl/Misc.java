/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package kuake2.render.lwjgl;

import kuake2.Defines;
import kuake2.client.VID;
import kuake2.qcommon.FS;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Misc
 *
 * @author cwei
 */
public abstract class Misc extends Mesh {

    private final static int TGA_HEADER_SIZE = 18;
    /*
    ==================
    R_InitParticleTexture
    ==================
    */
    byte[][] dottexture =
            {
                    {0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 0, 1, 1, 0, 0, 0, 0},
                    {0, 1, 1, 1, 1, 0, 0, 0},
                    {0, 1, 1, 1, 1, 0, 0, 0},
                    {0, 0, 1, 1, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0},
            };

//	/* 
//	============================================================================== 
// 
//							SCREEN SHOTS 
// 
//	============================================================================== 
//	*/ 
//
//	typedef struct _TargaHeader {
//		unsigned char 	id_length, colormap_type, image_type;
//		unsigned short	colormap_index, colormap_length;
//		unsigned char	colormap_size;
//		unsigned short	x_origin, y_origin, width, height;
//		unsigned char	pixel_size, attributes;
//	} TargaHeader;

    void R_InitParticleTexture() {
        int x, y;
        byte[] data = new byte[8 * 8 * 4];

        //
        // particle texture
        //
        for (x = 0; x < 8; x++) {
            for (y = 0; y < 8; y++) {
                data[y * 32 + x * 4 + 0] = (byte) 255;
                data[y * 32 + x * 4 + 1] = (byte) 255;
                data[y * 32 + x * 4 + 2] = (byte) 255;
                data[y * 32 + x * 4 + 3] = (byte) (dottexture[x][y] * 255);

            }
        }
        r_particletexture = GL_LoadPic("***particle***", data, 8, 8, it_sprite, 32);

        //
        // also use this for bad textures, but without alpha
        //
        for (x = 0; x < 8; x++) {
            for (y = 0; y < 8; y++) {
                data[y * 32 + x * 4 + 0] = (byte) (dottexture[x & 3][y & 3] * 255);
                data[y * 32 + x * 4 + 1] = 0; // dottexture[x&3][y&3]*255;
                data[y * 32 + x * 4 + 2] = 0; //dottexture[x&3][y&3]*255;
                data[y * 32 + x * 4 + 3] = (byte) 255;
            }
        }
        r_notexture = GL_LoadPic("***r_notexture***", data, 8, 8, it_wall, 32);
    }

    /*
    ==================
    GL_ScreenShot_f
    ==================
    */
    void GL_ScreenShot_f() {
        StringBuffer sb = new StringBuffer(FS.Gamedir() + "/scrshot/jake00.tga");
        FS.CreatePath(sb.toString());
        File file = new File(sb.toString());
        // find a valid file name
        int i = 0;
        int offset = sb.length() - 6;
        while (file.exists() && i++ < 100) {
            sb.setCharAt(offset, (char) ((i / 10) + '0'));
            sb.setCharAt(offset + 1, (char) ((i % 10) + '0'));
            file = new File(sb.toString());
        }
        if (i == 100) {
            VID.Printf(Defines.PRINT_ALL, "Clean up your screenshots\n");
            return;
        }

        try {
            RandomAccessFile out = new RandomAccessFile(file, "rw");
            FileChannel ch = out.getChannel();
            int fileLength = TGA_HEADER_SIZE + vid.width * vid.height * 3;
            out.setLength(fileLength);
            MappedByteBuffer image = ch.map(FileChannel.MapMode.READ_WRITE, 0,
                    fileLength);

            // write the TGA header
            image.put(0, (byte) 0).put(1, (byte) 0);
            image.put(2, (byte) 2); // uncompressed type
            image.put(12, (byte) (vid.width & 0xFF)); // vid.width
            image.put(13, (byte) (vid.width >> 8)); // vid.width
            image.put(14, (byte) (vid.height & 0xFF)); // vid.height
            image.put(15, (byte) (vid.height >> 8)); // vid.height
            image.put(16, (byte) 24); // pixel size

            // go to image data position
            image.position(TGA_HEADER_SIZE);


            // change pixel alignment for reading
            if (vid.width % 4 != 0) {
                GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            }

            // OpenGL 1.2+ supports the GL_BGR color format
            // check the GL_VERSION to use the TARGA BGR order if possible
            // e.g.: 1.5.2 NVIDIA 66.29
            if (gl_config.getOpenGLVersion() >= 1.2f) {
                // read the BGR values into the image buffer
                GL11.glReadPixels(0, 0, vid.width, vid.height, GL12.GL_BGR, GL11.GL_UNSIGNED_BYTE, image);
            } else {
                // read the RGB values into the image buffer
                GL11.glReadPixels(0, 0, vid.width, vid.height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, image);
                // flip RGB to BGR
                byte tmp;
                for (i = TGA_HEADER_SIZE; i < fileLength; i += 3) {
                    tmp = image.get(i);
                    image.put(i, image.get(i + 2));
                    image.put(i + 2, tmp);
                }
            }
            // reset to default alignment
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 4);
            // close the file channel
            ch.close();
        } catch (IOException e) {
            VID.Printf(Defines.PRINT_ALL, e.getMessage() + '\n');
        }

        VID.Printf(Defines.PRINT_ALL, "Wrote " + file + '\n');
    }

    /*
    ** GL_Strings_f
    */
    void GL_Strings_f() {
        VID.Printf(Defines.PRINT_ALL, "GL_VENDOR: " + gl_config.vendor_string + '\n');
        VID.Printf(Defines.PRINT_ALL, "GL_RENDERER: " + gl_config.renderer_string + '\n');
        VID.Printf(Defines.PRINT_ALL, "GL_VERSION: " + gl_config.version_string + '\n');
        VID.Printf(Defines.PRINT_ALL, "GL_EXTENSIONS: " + gl_config.extensions_string + '\n');
    }

    /*
    ** GL_SetDefaultState
    */
    void GL_SetDefaultState() {
        GL11.glClearColor(1f, 0f, 0.5f, 0.5f); // original quake2
        //GL11.glClearColor(0, 0, 0, 0); // replaced with black
        GL11.glCullFace(GL11.GL_FRONT);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.666f);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glColor4f(1, 1, 1, 1);

        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        GL11.glShadeModel(GL11.GL_FLAT);

        GL_TextureMode(gl_texturemode.string);
        GL_TextureAlphaMode(gl_texturealphamode.string);
        GL_TextureSolidMode(gl_texturesolidmode.string);

        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, gl_filter_min);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, gl_filter_max);

        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL_TexEnv(GL11.GL_REPLACE);

        if (qglPointParameterfEXT) {
            // float[] attenuations = { gl_particle_att_a.value, gl_particle_att_b.value, gl_particle_att_c.value };
            FloatBuffer att_buffer = BufferUtils.createFloatBuffer(4);
            att_buffer.put(0, gl_particle_att_a.value);
            att_buffer.put(1, gl_particle_att_b.value);
            att_buffer.put(2, gl_particle_att_c.value);

            GL11.glEnable(GL11.GL_POINT_SMOOTH);
            EXTPointParameters.glPointParameterfEXT(EXTPointParameters.GL_POINT_SIZE_MIN_EXT, gl_particle_min_size.value);
            EXTPointParameters.glPointParameterfEXT(EXTPointParameters.GL_POINT_SIZE_MAX_EXT, gl_particle_max_size.value);
            EXTPointParameters.glPointParameterEXT(EXTPointParameters.GL_DISTANCE_ATTENUATION_EXT, att_buffer);
        }

        if (qglColorTableEXT && gl_ext_palettedtexture.value != 0.0f) {
            GL11.glEnable(EXTSharedTexturePalette.GL_SHARED_TEXTURE_PALETTE_EXT);

            GL_SetTexturePalette(d_8to24table);
        }

        GL_UpdateSwapInterval();

		/*
		 * vertex array extension
		 */
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        ARBMultitexture.glClientActiveTextureARB(GL_TEXTURE0);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
    }

    void GL_UpdateSwapInterval() {
        if (gl_swapinterval.modified) {
            gl_swapinterval.modified = false;
            if (!gl_state.stereo_enabled) {
                if (qwglSwapIntervalEXT) {
                    // ((WGL)gl).wglSwapIntervalEXT((int)gl_swapinterval.value);
                }
            }
        }
    }
}
