package com.feresr.asteroids

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.BufferUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class Graphics(private val width: Int, private val height: Int) {

    private val shader: ShaderProgram by lazy { ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER) }
    fun init() {
        if (!shader.isCompiled) throw IllegalStateException(shader.log)

        Gdx.gl.glEnable(GL20.GL_BLEND)

        val tb = BufferUtils.newIntBuffer(1)
        Gdx.gl.glGenTextures(1, tb)
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, tb[0])

        Gdx.gl.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA, width, height, 0,
                GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, null)

        val atexcoor = shader.getAttributeLocation("a_texcoord")
        Gdx.gl.glVertexAttribPointer(
                atexcoor,
                2, GL30.GL_FLOAT, false, 0, createVerticesBuffer(TEX_VERTICES))
        Gdx.gl.glEnableVertexAttribArray(atexcoor)

        val aposition = shader.getAttributeLocation("a_position")
        Gdx.gl.glVertexAttribPointer(
                aposition,
                2, GL30.GL_FLOAT, false, 0, createVerticesBuffer(POS_VERTICES))
        Gdx.gl.glEnableVertexAttribArray(aposition)

        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST)
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST)
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE)
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE)

        shader.begin()
        print(Gdx.gl.glGetError())
    }

    private val buffer = BufferUtils.newIntBuffer(width * height)
    private val clear = IntArray(width * height)

    private fun createVerticesBuffer(vertices: FloatArray): FloatBuffer {
        if (vertices.size != 8) {
            throw RuntimeException("Number of vertices should be four.")
        }
        val buffer = ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        buffer.put(vertices).position(0)
        return buffer
    }

    fun drawPixel(x: Int, y: Int, r: Int, g: Int, b: Int) {
        try {
            var ny = y
            while (ny < 0) ny += height

            var nx = x
            while (nx < 0) nx += width

            buffer.put((ny % height) * width + (nx % width), r + (g shl 8) + (b shl 16))
        } catch (e: IndexOutOfBoundsException) {
            throw IndexOutOfBoundsException("Trying to draw to invalid position $x , $y")
        }

    }

    fun drawLine(x1: Float, y1: Float,
                 x2: Float, y2: Float,
                 r: Int, g: Int, b: Int) {

        val directionX = x2 - x1
        val directionY = y2 - y1
        val length = sqrt(directionX.pow(2) + directionY.pow(2))

        val unitX = directionX / length
        val unitY = directionY / length

        var currentX: Float = x1
        var currentY: Float = y1

        while (sqrt((x2 - currentX).pow(2) + (y2 - currentY).pow(2)) >= .9f) {
            drawPixel(currentX.roundToInt(), currentY.roundToInt(), r, g, b)
            currentX += unitX / 2
            currentY += unitY / 2
        }
    }

    fun render() {
        Gdx.gl.glClearColor(0f, 0.0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        buffer.rewind()
        Gdx.gl.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, width, height,
                GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, buffer)
        Gdx.gl.glDrawArrays(GL20.GL_TRIANGLE_STRIP, 0, 4)
        buffer.put(clear)
    }

    fun dispose() {
        shader.dispose()
    }

    companion object {
        private const val VERTEX_SHADER =
                "attribute vec4 a_position;\n" +
                        "attribute vec2 a_texcoord;\n" +
                        "varying vec2 v_texcoord;\n" +
                        "void main() {\n" +
                        "  gl_Position = a_position;\n" +
                        "  v_texcoord = a_texcoord;\n" +
                        "}\n"

        private const val FRAGMENT_SHADER =
                "uniform sampler2D tex_sampler;\n" +
                        "varying vec2 v_texcoord;\n" +
                        "void main() {\n" +
                        "  gl_FragColor = texture2D(tex_sampler, v_texcoord);\n" +
                        "}\n"

        private val TEX_VERTICES = floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        private val POS_VERTICES = floatArrayOf(-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f)
    }

}