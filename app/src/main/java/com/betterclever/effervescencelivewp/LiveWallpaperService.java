package com.betterclever.effervescencelivewp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Calendar;

/**
 * Created by better_clever on 14/9/16.
 */
public class LiveWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new betterCleverFueledLiveEngine();
    }

    private class betterCleverFueledLiveEngine extends Engine {
        private final Handler handler = new Handler();
        private BroadcastReceiver receiver = null;
        private float xOffset = 0.5f, yOffset = 0.5f;
        private Bitmap lastBackground = null, lastBackgroundScaled = null;
        private int lastHour = -1, lastWidth = -1, lastHeight = -1;
        private final Runnable drawRunner =
                new Runnable() {
                    @Override
                    public void run() {
                        draw();
                    }
                };
		//private int desiredHeight ;

        @Override
        public void onVisibilityChanged(boolean visible) {

            if (visible) {

                handler.post(drawRunner);
                IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
                filter.matchAction(Intent.ACTION_TIME_CHANGED);
                filter.matchAction(Intent.ACTION_TIMEZONE_CHANGED);

                receiver = new BroadcastReceiver() {

                    private int lastHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

                    @Override
                    public void onReceive(Context context, Intent intent) {
                        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

                        if (lastHour != currentHour) {
                            draw();
                        }

                        lastHour = currentHour;
                    }
                };

                registerReceiver(receiver, filter);
            } else {
                killResources();
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            killResources();
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            draw();
        }

        private void killResources() {
            handler.removeCallbacks(drawRunner);
            if (receiver != null) {
                unregisterReceiver(receiver);
                receiver = null;
            }
        }

        public void draw() {

            if (isPreview()) {
                xOffset = yOffset = 0.5f;
            }

            final SurfaceHolder holder = getSurfaceHolder();

            Canvas canvas = null;

            try {
                canvas = holder.lockCanvas();

                canvas.drawColor(Color.BLACK);

                Resources resources = getResources();
                DisplayMetrics metrics = resources.getDisplayMetrics();
                Bitmap background = getBackground(resources);

                float
                        x = (metrics.widthPixels - background.getWidth()) * xOffset,
                        y = (metrics.heightPixels - background.getHeight()) * yOffset;

                canvas.drawBitmap(background, x, y, null);

            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
            }

            handler.removeCallbacks(drawRunner);
        }

        private int getBackgroundForHour(int hour) {
            if (hour >= 21 || hour <= 4)
                return R.drawable.night2ef;
            else if (hour >= 16)
                return R.drawable.evening2ef;
            else if (hour >= 9)
                return R.drawable.noon2ef;
            else
                return R.drawable.morning2ef;
        }

        public Bitmap getBackground(Resources resources) {
            DisplayMetrics metrics = resources.getDisplayMetrics();

            int currentWidth = metrics.widthPixels;
			float currentHeightF = (float) (1.2*metrics.heightPixels);
			int currentHeight = (int) currentHeightF;
			int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

			//Log.d("curHeight", String.valueOf(currentHeight));

            if (lastHour != currentHour) {
                int id = getBackgroundForHour(currentHour);
                lastBackground = BitmapFactory.decodeResource(resources, id);
            }

            if (lastHour != currentHour
                    || lastWidth != currentWidth
                    || lastHeight != currentHeight) {

                lastBackgroundScaled = createBitmapFillDisplay(
                        lastBackground,
                        currentWidth,
                        currentHeight
                );

                lastHour = currentHour;
                lastWidth = currentWidth;
                lastHeight = currentHeight;
            }

            return lastBackgroundScaled;
        }

        private Bitmap createBitmapFillDisplay(Bitmap bitmap, float displayWidth, float displayHeight) {

            float
                    bitmapWidth = bitmap.getWidth(),
                    bitmapHeight = bitmap.getHeight(),
                    xScale = displayWidth / bitmapWidth,
                    yScale = displayHeight / bitmapHeight,
                    scale = Math.max(xScale, yScale),
                    scaledWidth = scale * bitmapWidth,
                    scaledHeight = scale * bitmapHeight;

            Bitmap scaledImage = Bitmap.createBitmap((int) scaledWidth, (int) scaledHeight, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(scaledImage);

            Matrix transformation = new Matrix();
            transformation.preScale(scale, scale);

            Paint paint = new Paint();
            paint.setFilterBitmap(true);

            canvas.drawBitmap(bitmap, transformation, paint);

            return scaledImage;
        }
    }


}