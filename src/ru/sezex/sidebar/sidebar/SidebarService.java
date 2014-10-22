package ru.sezex.sidebar.sidebar;

import ru.sezex.sidebar.Common;
import ru.sezex.sidebar.IntentUtil;
import ru.sezex.sidebar.MainActivity;
import ru.sezex.sidebar.R;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class SidebarService extends Service {
	public static boolean isRunning;
	public static boolean isStoppable;
	public static boolean isSidebarShown;

	public WindowManager mWindowManager;
	public Handler mHandler;

	public boolean mBarOnRight = true;
	public int mAnimationTime;
	public int mLabelSize;
	public int mAppColumns;

	private static SidebarHolderView mShownSidebar;
	private static SidebarHiddenView mHiddenSidebar;

	@Override
	public void onCreate() {
		super.onCreate();
		isRunning = true;
		isStoppable = false;

		mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		mHandler = new Handler();

		if (!isSidebarShown && mHiddenSidebar == null && mShownSidebar == null) {
			SharedPreferences main_prefs = getSharedPreferences(
					Common.KEY_PREFERENCE_MAIN, Context.MODE_PRIVATE);
			mBarOnRight = Integer.parseInt(main_prefs.getString(
					Common.PREF_KEY_SIDEBAR_POSITION,
					Common.PREF_DEF_SIDEBAR_POSITION)) == 1;

			mHiddenSidebar = new SidebarHiddenView(this);
			mShownSidebar = new SidebarHolderView(this);
			isSidebarShown = true;

			refreshSettings();

			hideBar();
		}
	}

	@SuppressWarnings("deprecation")
	private void refreshSettings() {
		SharedPreferences app_prefs = getSharedPreferences(
				Common.KEY_PREFERENCE_APPS, Context.MODE_PRIVATE);
		SharedPreferences main_prefs = getSharedPreferences(
				Common.KEY_PREFERENCE_MAIN, Context.MODE_PRIVATE);

		final int tab_size = main_prefs.getInt(Common.PREF_KEY_TAB_SIZE,
				Common.PREF_DEF_TAB_SIZE);
		mShownSidebar.setTabSize(tab_size);
		mHiddenSidebar.setTabSize(tab_size);

		final int hidden_tab_alpha_percentage = main_prefs.getInt(
				Common.PREF_KEY_TAB_ALPHA_HIDDEN,
				Common.PREF_DEF_TAB_ALPHA_HIDDEN);
		mHiddenSidebar.setTabAlpha(hidden_tab_alpha_percentage * 0.01f);

		final int speed = main_prefs.getInt(Common.PREF_KEY_ANIM_TIME,
				Common.PREF_DEF_ANIM_TIME);
		mAnimationTime = speed;

		final int mode = Integer.parseInt(main_prefs.getString(
				Common.PREF_KEY_LAUNCH_MODE, Common.PREF_DEF_LAUNCH_MODE));
		IntentUtil.setLaunchMode(mode);

		final int label_size = main_prefs.getInt(Common.PREF_KEY_LABEL_SIZE,
				Common.PREF_DEF_LABEL_SIZE);
		mLabelSize = label_size;

		if (main_prefs.getBoolean(Common.PREF_KEY_KEEP_IN_BG,
				Common.PREF_DEF_KEEP_IN_BG)) {
			PendingIntent intent = PendingIntent.getActivity(this, 0,
					new Intent(this, MainActivity.class), 0);
			Notification.Builder n = new Notification.Builder(this)
					.setContentTitle(getResources().getText(R.string.service))
					.setSmallIcon(R.drawable.notification)
					.setContentText(
							getResources().getText(R.string.service_running))
					.setContentIntent(intent);

			if (Build.VERSION.SDK_INT >= 16) {
				n.setPriority(Notification.PRIORITY_MIN);
				startForeground(99999, n.build());
			} else {
				startForeground(99999, n.getNotification());
			}

			if (Build.VERSION.SDK_INT >= 17) {
				n.setPriority(Notification.PRIORITY_MIN);
				n.setShowWhen(false);
				startForeground(99999, n.build());
			} else {
				startForeground(99999, n.getNotification());
			}

		} else {
			stopForeground(true);
		}

		mAppColumns = main_prefs.getInt(Common.PREF_KEY_COLUMN_NUMBER,
				Common.PREF_DEF_COLUMN_NUMBER);
		mShownSidebar.applySidebarWidth(80);

		mShownSidebar.addApps(app_prefs, getPackageManager());
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if (intent != null
				&& intent.getBooleanExtra(Common.EXTRA_REFRESH_SERVICE, false)) {
			refreshSettings();
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		isRunning = false;

		if (isStoppable) {
			isSidebarShown = false;
			isStoppable = false;
			mHiddenSidebar.animateView(false);
			mShownSidebar.animateView(false);
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					safelyRemoveView(mHiddenSidebar);
					safelyRemoveView(mShownSidebar);
					mHiddenSidebar = null;
					mShownSidebar = null;
				}
			}, 500);
		} else {
			Intent i = new Intent(Common.PKG_THIS + ".START");
			sendBroadcast(i);
		}
	}

	public static Intent stopSidebar(Context ctx) {
		isStoppable = true;
		Intent i = new Intent(ctx, SidebarService.class);
		ctx.stopService(i);
		return i;
	}

	public void safelyRemoveView(View v) {
		try {
			mWindowManager.removeView(v);
		} catch (Exception e) {
		}
	}

	public void addView(View v) {
		final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				v instanceof SidebarHolderView ? WindowManager.LayoutParams.MATCH_PARENT
						: WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
				WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
						| WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
						| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
						| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
						| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
				PixelFormat.TRANSLUCENT);
		if (mBarOnRight) {
			params.gravity = Gravity.TOP | Gravity.RIGHT;
		} else {
			params.gravity = Gravity.TOP | Gravity.LEFT;
		}
		try {
			mWindowManager.addView(v, params);
		} catch (Exception e) {
		}
	}

	public void showBar() {
		mHiddenSidebar.animateView(false);
		mShownSidebar.animateView(true);
	}

	public void hideBar() {
		mHiddenSidebar.animateView(true);
		mShownSidebar.animateView(false);
		try {
			mShownSidebar.setMarginFromTop(mMargin);
			mHiddenSidebar.setMarginFromTop(mMargin);
		} catch (Exception e) {
		}
	}

	private int mOldMargin = 20;
	private int mMargin = 20;
	private float mInitialPosition;

	public boolean tabTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mInitialPosition = event.getRawY();
			mOldMargin = mMargin;
			break;
		case MotionEvent.ACTION_MOVE:
			float final_position = event.getRawY();
			if (final_position > getResources().getDisplayMetrics().heightPixels - 20) {
				final_position = getResources().getDisplayMetrics().heightPixels - 20;
			}
			mMargin = (int) (mOldMargin + final_position - mInitialPosition);
			if (mMargin < 0) {
				mMargin = 0;
			}
			try {
				mShownSidebar.setMarginFromTop(mMargin);
				mHiddenSidebar.setMarginFromTop(mMargin);
			} catch (Exception e) {
			}
			if (Math.abs(mOldMargin - mMargin) > 25) {
				return true;
			}
			break;
		}
		return false;
	}
}