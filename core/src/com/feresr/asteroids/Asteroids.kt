package com.feresr.asteroids

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import kotlin.math.*
import kotlin.random.Random

class Asteroids : ApplicationAdapter() {

    private val graphics: Graphics by lazy { Graphics(SCREEN_SIZE, SCREEN_SIZE) }

    private val spaceShip = WireFrame(
            SCREEN_SIZE / 2f, SCREEN_SIZE / 2f, 0f, 0f, 0f,
            floatArrayOf(0.0f, 2.5f, -2.5f),
            floatArrayOf(-5.0f, 2.5f, 2.5f),
            2.5f
    )
    private var bullets = mutableSetOf<Bullet>()
    private var asteroids = mutableSetOf<WireFrame>()

    private val random = Random(0)
    private var t1 = System.currentTimeMillis()
    private var t2 = System.currentTimeMillis()
    private var elapsed = 0L

    override fun create() {
        graphics.init()
        startNewGame()
    }

    private fun startNewGame() {
        gameOver = false
        asteroids.clear()
        bullets.clear()
        asteroids.add(generateAsteroid(SCREEN_SIZE * .3f, SCREEN_SIZE * .3f, 16f))
        asteroids.add(generateAsteroid(SCREEN_SIZE * .9f, SCREEN_SIZE * .3f, 16f))
        asteroids.add(generateAsteroid(SCREEN_SIZE * .3f, SCREEN_SIZE * .9f, 16f))

        spaceShip.dx = 0f
        spaceShip.dy = 0f

        spaceShip.x = SCREEN_SIZE / 2f
        spaceShip.y = SCREEN_SIZE / 2f
    }

    private fun generateAsteroid(x: Float, y: Float, radius: Float): WireFrame {
        val side = 8
        val xs = FloatArray(side)
        val ys = FloatArray(side)
        var angle = 0f

        for (i in 0 until side) {
            val r = (random.nextFloat() * ASTEROID_DEFORMATION)
            xs[i] = -sin(angle) * radius + r
            ys[i] = cos(angle) * radius + r

            angle += 2 * PI.toFloat() / side
        }
        val aangle = random.nextFloat() * 2 * PI.toFloat()
        return WireFrame(x, y, -sin(aangle) / radius, cos(aangle) / radius, aangle, xs, ys, radius)
    }

    override fun render() {
        t2 = System.currentTimeMillis()
        elapsed = t2 - t1
        t1 = t2


        if (gameOver) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                startNewGame()
            }
            return
        }


        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            spaceShip.dx += sin(spaceShip.angle) * ACCELERATION * elapsed
            spaceShip.dy -= cos(spaceShip.angle) * ACCELERATION * elapsed
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) spaceShip.angle -= ROTATION_SPEED * elapsed
        if (Gdx.input.isKeyPressed(Input.Keys.D)) spaceShip.angle += ROTATION_SPEED * elapsed

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            bullets.add(Bullet(spaceShip.x,
                    spaceShip.y,
                    sin(spaceShip.angle) * SHOT_SPEED,
                    -cos(spaceShip.angle) * SHOT_SPEED,
                    100
            ))
        }

        bullets = bullets.filter {
            val x = (it.x + it.dx * elapsed).roundToInt()
            val y = (it.y + it.dy * elapsed).roundToInt()

            x in 0 until SCREEN_SIZE && y in 0 until SCREEN_SIZE && it.last > 0
        }.toMutableSet()

        for (bullet in bullets) {
            bullet.x += bullet.dx * elapsed
            bullet.y += bullet.dy * elapsed
            bullet.last--

            val hitAsteroids = mutableSetOf<WireFrame>()
            for (asteroid in asteroids) {
                if (isPointInsideCircle(bullet.x, bullet.y, asteroid.x, asteroid.y, asteroid.radius)) {
                    bullet.last = 0
                    hitAsteroids.add(asteroid)
                }
            }
            hitAsteroids.forEach {
                val radius = it.radius / 2
                if (radius >= 4) {
                    asteroids.add(generateAsteroid(it.x, it.y, radius))
                    asteroids.add(generateAsteroid(it.x, it.y, radius))
                }
            }
            asteroids = (asteroids - hitAsteroids).toMutableSet()
            graphics.drawPixel(bullet.x.roundToInt(), bullet.y.roundToInt(), 255, 255, 255)
        }


        spaceShip.x += spaceShip.dx
        spaceShip.y += spaceShip.dy
        if (spaceShip.y < 0) spaceShip.y += SCREEN_SIZE
        if (spaceShip.y > SCREEN_SIZE) spaceShip.y -= SCREEN_SIZE
        if (spaceShip.x < 0) spaceShip.x += SCREEN_SIZE
        if (spaceShip.x > SCREEN_SIZE) spaceShip.x -= SCREEN_SIZE

        for (asteroid in asteroids) {
            asteroid.angle += .001f * elapsed
            asteroid.x += asteroid.dx
            asteroid.y += asteroid.dy

            if (asteroid.y < 0) asteroid.y += SCREEN_SIZE
            if (asteroid.y > SCREEN_SIZE) asteroid.y -= SCREEN_SIZE
            if (asteroid.x < 0) asteroid.x += SCREEN_SIZE
            if (asteroid.x > SCREEN_SIZE) asteroid.x -= SCREEN_SIZE


            if (isPointInsideCircle(spaceShip.x, spaceShip.y, asteroid.x, asteroid.y, asteroid.radius)) {
                gameOver = true
            }
            drawWireFrame(asteroid, 255 + (207 shl 8))
        }

        var spaceshipColor = 0b000111111111111100011111
        if (asteroids.isEmpty()) {
            gameOver = true
            spaceshipColor = 0b111111111111111100000000
        }
        drawWireFrame(spaceShip, spaceshipColor)
        graphics.render()
    }

    var gameOver = false

    override fun dispose() {
        graphics.dispose()
    }

    private fun drawWireFrame(wireFrame: WireFrame, color: Int) {

        val transformedX = FloatArray(wireFrame.modelX.size) {
            val rotate = wireFrame.modelX[it] * cos(wireFrame.angle) - wireFrame.modelY[it] * sin(wireFrame.angle)
            //Translate
            rotate + wireFrame.x

        }

        val transformedY = FloatArray(wireFrame.modelY.size) {
            val rotate = wireFrame.modelX[it] * sin(wireFrame.angle) + wireFrame.modelY[it] * cos(wireFrame.angle)
            //Translate
            rotate + wireFrame.y
        }

        for (i in wireFrame.modelX.indices) {
            graphics.drawLine(
                    transformedX[i],
                    transformedY[i],
                    transformedX[(i + 1) % transformedX.size],
                    transformedY[(i + 1) % transformedY.size],
                    color, (color ushr 8), (color ushr 16)
            )
        }
    }

    private fun isPointInsideCircle(px: Float, py: Float, cx: Float, cy: Float, radius: Float): Boolean {
        // Toroidal space
        var xDiff = abs(px - cx)
        if (xDiff > (SCREEN_SIZE / 2)) xDiff = SCREEN_SIZE - xDiff

        var yDiff = abs(py - cy)
        if (yDiff > (SCREEN_SIZE / 2)) yDiff = SCREEN_SIZE - yDiff

        return sqrt((yDiff).pow(2) + (xDiff).pow(2)) < radius
    }

    abstract class SpaceObject(var x: Float,
                               var y: Float,
                               var dx: Float,
                               var dy: Float)

    class WireFrame(
            x: Float,
            y: Float,
            dx: Float,
            dy: Float,
            var angle: Float,
            val modelX: FloatArray,
            val modelY: FloatArray,
            val radius: Float
    ) : SpaceObject(x, y, dx, dy)


    class Bullet(x: Float,
                 y: Float,
                 dx: Float,
                 dy: Float,
                 var last: Int = 100) : SpaceObject(x, y, dx, dy)

    companion object {
        const val ROTATION_SPEED = 0.006f
        const val ACCELERATION = .001f
        const val SHOT_SPEED = .08f
        const val SCREEN_SIZE = 128
        const val ASTEROID_DEFORMATION = 4

    }
}
