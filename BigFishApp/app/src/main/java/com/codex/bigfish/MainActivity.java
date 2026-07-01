package com.codex.bigfish;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new BigFishView(this));
    }

    static class BigFishView extends View {
        private static final String PREFS = "big_fish_save_v1";
        private static final int STATE_READY = 0;
        private static final int STATE_PLAYING = 1;
        private static final int STATE_OVER = 2;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();
        private final List<Fish> fish = new ArrayList<>();
        private final SharedPreferences prefs;
        private final RectF restartButton = new RectF();
        private final RectF resetButton = new RectF();

        private int width;
        private int height;
        private int state = STATE_READY;
        private long lastFrame;
        private long lastSpawn;
        private float playerX;
        private float playerY;
        private float targetX;
        private float targetY;
        private float playerSize;
        private int score;
        private int bestScore;
        private int bestSize;
        private int totalEaten;
        private int level;
        private float wave;

        BigFishView(Context context) {
            super(context);
            setFocusable(true);
            prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            bestScore = prefs.getInt("bestScore", 0);
            bestSize = prefs.getInt("bestSize", 34);
            totalEaten = prefs.getInt("totalEaten", 0);
            level = Math.max(1, prefs.getInt("level", 1));
            playerSize = Math.max(34, bestSize * 0.45f);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            width = w;
            height = h;
            playerX = targetX = width * 0.35f;
            playerY = targetY = height * 0.55f;
            layoutButtons();
        }

        private void layoutButtons() {
            float bw = Math.min(width * 0.56f, 420f);
            float bh = Math.max(58f, height * 0.06f);
            float cx = width * 0.5f;
            restartButton.set(cx - bw / 2f, height * 0.62f, cx + bw / 2f, height * 0.62f + bh);
            resetButton.set(cx - bw / 2f, restartButton.bottom + 22f, cx + bw / 2f, restartButton.bottom + 22f + bh);
        }

        private void startGame() {
            state = STATE_PLAYING;
            fish.clear();
            score = 0;
            playerSize = Math.max(34f, 30f + level * 1.5f);
            playerX = targetX = width * 0.35f;
            playerY = targetY = height * 0.55f;
            lastFrame = 0L;
            lastSpawn = 0L;
            spawnFish(true);
            invalidate();
        }

        private void resetSave() {
            bestScore = 0;
            bestSize = 34;
            totalEaten = 0;
            level = 1;
            prefs.edit().clear().apply();
            startGame();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            long now = System.currentTimeMillis();
            if (lastFrame == 0L) lastFrame = now;
            float dt = Math.min(0.033f, (now - lastFrame) / 1000f);
            lastFrame = now;
            if (state == STATE_PLAYING) updateGame(now, dt);
            drawSea(canvas);
            drawFishSchool(canvas);
            drawPlayer(canvas);
            drawHud(canvas);
            if (state == STATE_READY) drawOverlay(canvas, "大鱼吃小鱼", "点一下开始");
            if (state == STATE_OVER) drawOverlay(canvas, "被大鱼吃掉了", "再来一局");
            if (state == STATE_PLAYING) postInvalidateOnAnimation();
        }

        private void updateGame(long now, float dt) {
            wave += dt;
            playerX += (targetX - playerX) * Math.min(1f, dt * 5.2f);
            playerY += (targetY - playerY) * Math.min(1f, dt * 5.2f);
            playerX = clamp(playerX, playerSize, width - playerSize);
            playerY = clamp(playerY, playerSize + 80f, height - playerSize);

            if (now - lastSpawn > Math.max(430, 1100 - level * 35)) {
                spawnFish(false);
                lastSpawn = now;
            }

            Iterator<Fish> it = fish.iterator();
            while (it.hasNext()) {
                Fish f = it.next();
                f.x += f.speed * dt * f.direction;
                f.y += Math.sin((wave + f.phase) * 2.1f) * f.wobble * dt;
                if ((f.direction > 0 && f.x - f.size > width + 80) || (f.direction < 0 && f.x + f.size < -80)) {
                    it.remove();
                    continue;
                }
                float dx = f.x - playerX;
                float dy = f.y - playerY;
                float distance = (float)Math.sqrt(dx * dx + dy * dy);
                if (distance < playerSize * 0.72f + f.size * 0.68f) {
                    if (f.size <= playerSize * 0.92f) {
                        it.remove();
                        eat(f);
                    } else {
                        gameOver();
                        return;
                    }
                }
            }
        }

        private void eat(Fish f) {
            score += Math.max(1, Math.round(f.size));
            totalEaten += 1;
            playerSize = Math.min(Math.min(width, height) * 0.18f, playerSize + Math.max(1.2f, f.size * 0.05f));
            level = Math.max(level, 1 + totalEaten / 18);
            bestScore = Math.max(bestScore, score);
            bestSize = Math.max(bestSize, Math.round(playerSize));
            saveProgress();
        }

        private void gameOver() {
            state = STATE_OVER;
            saveProgress();
            invalidate();
        }

        private void saveProgress() {
            prefs.edit()
                .putInt("bestScore", bestScore)
                .putInt("bestSize", bestSize)
                .putInt("totalEaten", totalEaten)
                .putInt("level", level)
                .apply();
        }

        private void spawnFish(boolean initial) {
            int count = initial ? 9 : 1;
            for (int i = 0; i < count; i++) {
                Fish f = new Fish();
                f.direction = random.nextBoolean() ? 1 : -1;
                f.size = randomFishSize();
                f.x = f.direction > 0 ? -f.size - random.nextInt(180) : width + f.size + random.nextInt(180);
                if (initial) f.x = random.nextInt(Math.max(1, width));
                f.y = 120f + random.nextFloat() * Math.max(1f, height - 220f);
                f.speed = 95f + random.nextFloat() * (80f + level * 5f);
                f.phase = random.nextFloat() * 6.28f;
                f.wobble = 12f + random.nextFloat() * 20f;
                f.color = palette(random.nextInt(6), f.size > playerSize ? 1 : 0);
                fish.add(f);
            }
        }

        private float randomFishSize() {
            float base = 18f + random.nextFloat() * 36f + level * 1.2f;
            if (random.nextFloat() < 0.28f) base = playerSize * (1.08f + random.nextFloat() * 0.55f);
            if (random.nextFloat() < 0.38f) base = playerSize * (0.42f + random.nextFloat() * 0.38f);
            return clamp(base, 14f, Math.min(width, height) * 0.15f);
        }

        private int palette(int index, int danger) {
            if (danger == 1) {
                int[] dangerColors = {0xFFE84855, 0xFFD62828, 0xFFFF7B00};
                return dangerColors[index % dangerColors.length];
            }
            int[] colors = {0xFF00A6A6, 0xFF118AB2, 0xFF06D6A0, 0xFFFFC857, 0xFF7BDFF2, 0xFFB8F2E6};
            return colors[index % colors.length];
        }

        private void drawSea(Canvas canvas) {
            paint.setShader(new LinearGradient(0, 0, 0, height, 0xFF01497C, 0xFF012A4A, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, width, height, paint);
            paint.setShader(null);
            paint.setColor(0x33FFFFFF);
            for (int i = 0; i < 9; i++) {
                float y = 80 + i * 92f + (float)Math.sin(wave * 1.6f + i) * 12f;
                canvas.drawCircle((i * 83 + wave * 24f) % Math.max(1, width), y, 2.5f + i % 3, paint);
            }
            paint.setColor(0x2200D4FF);
            for (int i = 0; i < 4; i++) {
                canvas.drawOval(-80 + i * width / 3f, height - 130 - i * 18f, width * 0.55f + i * width / 3f, height + 120, paint);
            }
        }

        private void drawFishSchool(Canvas canvas) {
            for (Fish f : fish) drawFish(canvas, f.x, f.y, f.size, f.direction, f.color, f.size > playerSize);
        }

        private void drawPlayer(Canvas canvas) {
            drawFish(canvas, playerX, playerY, playerSize, 1, 0xFF4CC9F0, false);
            paint.setColor(0x55FFFFFF);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            canvas.drawCircle(playerX, playerY, playerSize * 0.92f, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawFish(Canvas canvas, float x, float y, float size, int dir, int color, boolean danger) {
            canvas.save();
            canvas.translate(x, y);
            canvas.scale(dir, 1f);
            paint.setShader(new RadialGradient(-size * 0.15f, -size * 0.2f, size * 1.2f, lighten(color), color, Shader.TileMode.CLAMP));
            Path body = new Path();
            body.moveTo(-size * 0.95f, 0);
            body.cubicTo(-size * 0.35f, -size * 0.75f, size * 0.88f, -size * 0.55f, size * 1.05f, 0);
            body.cubicTo(size * 0.88f, size * 0.55f, -size * 0.35f, size * 0.75f, -size * 0.95f, 0);
            canvas.drawPath(body, paint);
            paint.setShader(null);
            paint.setColor(color);
            Path tail = new Path();
            tail.moveTo(-size * 0.83f, 0);
            tail.lineTo(-size * 1.42f, -size * 0.52f);
            tail.lineTo(-size * 1.28f, 0);
            tail.lineTo(-size * 1.42f, size * 0.52f);
            tail.close();
            canvas.drawPath(tail, paint);
            paint.setColor(0xFFFFFFFF);
            canvas.drawCircle(size * 0.58f, -size * 0.18f, Math.max(2.4f, size * 0.11f), paint);
            paint.setColor(0xFF06283D);
            canvas.drawCircle(size * 0.61f, -size * 0.18f, Math.max(1.2f, size * 0.052f), paint);
            if (danger) {
                paint.setColor(0xCCFFFFFF);
                paint.setStrokeWidth(Math.max(2f, size * 0.035f));
                canvas.drawLine(size * 0.28f, size * 0.25f, size * 0.66f, size * 0.22f, paint);
            }
            canvas.restore();
        }

        private int lighten(int color) {
            int r = Math.min(255, Color.red(color) + 70);
            int g = Math.min(255, Color.green(color) + 70);
            int b = Math.min(255, Color.blue(color) + 70);
            return Color.rgb(r, g, b);
        }

        private void drawHud(Canvas canvas) {
            paint.setShader(null);
            paint.setColor(0xAA001B2E);
            canvas.drawRoundRect(18, 22, width - 18, 118, 18, 18, paint);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTextSize(32f);
            canvas.drawText("分数 " + score, 38, 62, paint);
            paint.setTextSize(26f);
            canvas.drawText("最高 " + bestScore + "   等级 " + level + "   已吃 " + totalEaten, 38, 100, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.format(Locale.CHINA, "体型 %.0f", playerSize), width - 38, 62, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        private void drawOverlay(Canvas canvas, String title, String action) {
            paint.setColor(0xBB00111F);
            canvas.drawRect(0, 0, width, height, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(Color.WHITE);
            paint.setTextSize(Math.min(56f, width * 0.105f));
            canvas.drawText(title, width / 2f, height * 0.36f, paint);
            paint.setTextSize(Math.min(28f, width * 0.052f));
            canvas.drawText("吃掉比你小的鱼，避开红色大鱼", width / 2f, height * 0.43f, paint);
            canvas.drawText("升级安装会保留存档", width / 2f, height * 0.48f, paint);
            drawButton(canvas, restartButton, action);
            drawButton(canvas, resetButton, "清空存档");
            paint.setTextAlign(Paint.Align.LEFT);
        }

        private void drawButton(Canvas canvas, RectF r, String label) {
            paint.setColor(0xFF00A6A6);
            canvas.drawRoundRect(r, 16f, 16f, paint);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(30f);
            Paint.FontMetrics fm = paint.getFontMetrics();
            canvas.drawText(label, r.centerX(), r.centerY() - (fm.ascent + fm.descent) / 2f, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (state != STATE_PLAYING) {
                    if (resetButton.contains(event.getX(), event.getY())) resetSave();
                    else startGame();
                    return true;
                }
            }
            if (state == STATE_PLAYING && (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE)) {
                targetX = event.getX();
                targetY = event.getY();
                return true;
            }
            return true;
        }

        private float clamp(float v, float min, float max) {
            return Math.max(min, Math.min(max, v));
        }

        static class Fish {
            float x;
            float y;
            float size;
            float speed;
            float phase;
            float wobble;
            int direction;
            int color;
        }
    }
}
