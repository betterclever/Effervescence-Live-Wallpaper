package com.betterclever.effervescencelivewp;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Pranjal Paliwal on 9/18/2016.
 */
public class ClockWidgetProvider extends AppWidgetProvider {
	public static class ClockUpdateService extends Service {

		private static final String ACTION_UPDATE = "com.betterclever.effervescencelwp.clockwidget.UPDATE";
		private IntentFilter intentFilter;

		// BroadcastReceiver receiving the updates.
		private final BroadcastReceiver clockChangedReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				updateTime(context);
			}
		};

		public ClockUpdateService() {
			super();
		}

		public void onCreate() {
			super.onCreate();
			intentFilter = new IntentFilter();
			intentFilter.addAction(Intent.ACTION_TIME_TICK);
			intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
			intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
			intentFilter.addAction(ACTION_UPDATE);
			registerReceiver(clockChangedReceiver, intentFilter);
		}

		public void onDestroy() {
			super.onDestroy();
			unregisterReceiver(clockChangedReceiver);
		}

		@Override
		public IBinder onBind(Intent intent) {
			return null;
		}

		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			if (intent != null && intent.getAction() != null) {
				if (intent.getAction().equals(ACTION_UPDATE)) {
					updateTime(this);
				}
			}
			return super.onStartCommand(intent, flags, startId);
		}
	}

	private static RemoteViews updateViews(Context context) {
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.clock_widget_layout);

		Date date = new Date();
		DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
		//remoteViews.setTextViewText(R.id.textViewDate, dateFormatter.format(date));
		remoteViews.setImageViewBitmap(R.id.imageViewDate, getFontBitmap(
			context,
			dateFormatter.format(date),
			Color.WHITE,
			36));
		DateFormat timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
		//remoteViews.setTextViewText(R.id.textViewTime, timeFormatter.format(date));
		remoteViews.setImageViewBitmap(R.id.imageViewTime, getFontBitmap(
			context,
			timeFormatter.format(date),
			Color.WHITE,
			48));

		return remoteViews;
	}

	private static Bitmap getFontBitmap(Context context, String text, int color, float fontSizeSP) {


		int fontSizePX = convertDiptoPix(context, fontSizeSP);
		int pad = (fontSizePX / 9);
		Paint paint = new Paint();
		Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/painterz.ttf");
		paint.setAntiAlias(true);
		paint.setTypeface(typeface);
		paint.setColor(color);
		paint.setTextSize(fontSizePX);
		paint.setShadowLayer(2, 1, 1, Color.DKGRAY);

		Paint paint1 = new Paint();
		paint1.setColor(Color.parseColor("#F4511E"));
		paint1.setAntiAlias(true);
		paint1.setAlpha(128);

		int textWidth = (int) (paint.measureText(text) + pad * 2);
		int height = (int) (fontSizePX / 0.75);
		Bitmap bitmap = Bitmap.createBitmap(textWidth, height, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);

		//canvas.drawPaint(paint1);
		canvas.drawText(text, pad, fontSizePX, paint);
		return bitmap;
	}
	private static int convertDiptoPix(Context context, float dip) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, context.getResources().getDisplayMetrics());
	}

	private static void updateTime(Context context) {
		RemoteViews remoteViews = updateViews(context);
		ComponentName clockWidget = new ComponentName(context, ClockWidgetProvider.class);
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		appWidgetManager.updateAppWidget(clockWidget, remoteViews);
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		context.stopService(new Intent(context, ClockUpdateService.class));
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		updateTime(context);
		context.startService(new Intent(context, ClockUpdateService.class));
	}

	@Override
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions){
		updateTime(context);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		updateTime(context);
		context.startService(new Intent(context, ClockUpdateService.class));
	}
}
