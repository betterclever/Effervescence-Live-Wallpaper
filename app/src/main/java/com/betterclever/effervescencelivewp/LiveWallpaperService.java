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
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by better_clever on 14/9/16.
 */
public class LiveWallpaperService extends WallpaperService {
	@Override
	public Engine onCreateEngine() {
		return new betterCleverFueledLiveEngine();
	}

	private class betterCleverFueledLiveEngine extends Engine {

		private final int NIGHT_STATE = 0;
		private final int MORNING_STATE = 1;
		private final int NOON_STATE = 2;
		private final int EVENING_STATE = 3;


		private final Handler handler = new Handler();
		private BroadcastReceiver receiver = null;
		private float xOffset = 0.5f, yOffset = 0.5f;
		private Bitmap lastBackground = null, lastBackgroundScaled = null;
		private int lastHour = -1, lastWidth = -1, lastHeight = -1;
		private long daysLeft = 0;
		int timeState = 0;
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
				filter.matchAction(Intent.ACTION_DATE_CHANGED);

				receiver = new BroadcastReceiver() {

					private int lastHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
					private Date curdate = Calendar.getInstance().getTime();
					private Date effeDate = new Date(116, 9, 15);
					long diff = curdate.getTime() - effeDate.getTime();

					private long daysLeft = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

					@Override
					public void onReceive(Context context, Intent intent) {
						int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
						curdate = Calendar.getInstance().getTime();
						effeDate = new Date(116, 9, 15);
						long diff = curdate.getTime() - effeDate.getTime();
						long newDaysLeft = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

						if (lastHour != currentHour || daysLeft != newDaysLeft) {
							draw();
						}

						lastHour = currentHour;
						daysLeft = newDaysLeft;
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
			if (hour >= 21 || hour <= 4) {
				timeState = NIGHT_STATE;
				return R.drawable.night2ef;
			} else if (hour >= 16) {
				timeState = EVENING_STATE;
				return R.drawable.evening2ef;
			} else if (hour >= 9) {
				timeState = NOON_STATE;
				return R.drawable.noon2ef;
			} else {
				timeState = MORNING_STATE;
				return R.drawable.morning2ef;
			}
		}

		public Bitmap getBackground(Resources resources) {
			DisplayMetrics metrics = resources.getDisplayMetrics();

			int currentWidth = metrics.widthPixels;
			float currentHeightF = (float) (1.2 * metrics.heightPixels);
			int currentHeight = (int) currentHeightF;
			int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

			Date curdate = Calendar.getInstance().getTime();
			Date effeDate = new Date(116, 9, 15);

			long diff = effeDate.getTime() - curdate.getTime();
			long newDaysLeft = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

			//Log.d("curHeight", String.valueOf(currentHeight));

			if (lastHour != currentHour || daysLeft != newDaysLeft) {
				int id = getBackgroundForHour(currentHour);
				BitmapFactory.Options options = new BitmapFactory.Options();
				//options.inJustDecodeBounds = true;
				//options.inSampleSize = 2;
				options.inTempStorage = new byte[16*1024];
				lastBackground = BitmapFactory.decodeResource(resources, id,options);
			}

			if (lastHour != currentHour
				|| lastWidth != currentWidth
				|| lastHeight != currentHeight
				|| daysLeft != newDaysLeft
				) {

				lastBackgroundScaled = createBitmapFillDisplay(
					lastBackground,
					currentWidth,
					currentHeight
				);

				lastHour = currentHour;
				lastWidth = currentWidth;
				lastHeight = currentHeight;
				daysLeft = newDaysLeft;
			}

			return lastBackgroundScaled;
		}

		private Bitmap createBitmapFillDisplay(Bitmap bitmap, float displayWidth, float displayHeight) {

			float
				bitmapWidth = bitmap.getWidth(),
				bitmapHeight = bitmap.getHeight(),
				xScale = displayWidth / bitmapWidth ,
				yScale = displayHeight / bitmapHeight,
				scale = Math.max(xScale, yScale),
				scaledWidth = scale * bitmapWidth,
				scaledHeight = scale * bitmapHeight;

			Bitmap scaledImage = Bitmap.createBitmap((int) scaledWidth, (int) scaledHeight, Bitmap.Config.RGB_565);

			Canvas canvas = new Canvas(scaledImage);

			Matrix transformation = new Matrix();
			transformation.preScale(scale, scale);

			Paint paint = new Paint();
			paint.setFilterBitmap(true);

			Date curdate = Calendar.getInstance().getTime();
			Date effeDate = new Date(116, 9, 15);

			long diff = effeDate.getTime() - curdate.getTime();
			long newDaysLeft = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

			String bannerMessage = newDaysLeft + " days to go";

			if(newDaysLeft < 0 && newDaysLeft>-3){
				bannerMessage = "Enjoy the Carnival";
			}
			else if(newDaysLeft <= -3){
				bannerMessage = "Stay tuned for next year";
			}


			canvas.drawBitmap(bitmap, transformation, paint);

			if (timeState == NIGHT_STATE) {
				drawTextToBitmap(getApplicationContext(), scaledImage, canvas,
					bannerMessage, getColor(R.color.md_purple_900),
					0.31f, 0.36f, -11, 80);
			}
			else if(timeState == EVENING_STATE){
				drawTextToBitmap(getApplicationContext(), scaledImage, canvas,
					bannerMessage, getColor(R.color.md_brown_800),
					0.31f, 0.35f, -11, 80);
			}
			else {
				drawTextToBitmap(getApplicationContext(), scaledImage, canvas,
					bannerMessage, getColor(R.color.md_yellow_50),
					0.5f, 0.25f, 0, 60);
			}

			return scaledImage;
		}

		public void drawTextToBitmap(Context gContext, Bitmap bitmap, Canvas canvas, String gText, int color, float corX, float corY, int angle, int textSizeScale) {

			Resources resources = gContext.getResources();
			float scale = resources.getDisplayMetrics().density;

			Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setColor(color);
			paint.setTextSize((int) (textSizeScale * scale));
			Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/quigleywiggly.ttf");
			paint.setTypeface(tf);

			// draw text to the Canvas center
			Rect bounds = new Rect();
			paint.getTextBounds(gText, 0, gText.length(), bounds);

			int x = (int) ((bitmap.getWidth() - bounds.width()) * corX);
			int y = (int) ((bitmap.getHeight() + bounds.height()) * corY);

			Log.d("Angle", String.valueOf(angle));

			canvas.save();
			canvas.rotate(angle);
			canvas.drawText(gText, x, y, paint);
			canvas.restore();

		}
	}
}