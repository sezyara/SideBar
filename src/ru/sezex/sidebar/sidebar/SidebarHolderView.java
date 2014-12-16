package ru.sezex.sidebar.sidebar;

import java.util.Map;
import java.util.Map.Entry;

import ru.sezex.sidebar.Common;
import ru.sezex.sidebar.R;
import ru.sezex.sidebar.Util;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

@SuppressLint("ViewConstructor")
public class SidebarHolderView extends LinearLayout {

	private final SidebarService mService;
	private final LayoutInflater mInflator;

	private final RelativeLayout mContentView;
	private final LinearLayout mHolderView;
	private final RelativeLayout mBarView;
	private final ImageView mTabView;
	private SidebarItemView[] mItemViews;

	private Rect mRect;

	private boolean mTransferTouchEventsToSidebarItemView;

	public SidebarHolderView(SidebarService service) {
		super(service);
		mInflator = LayoutInflater.from(service);
		mService = service;

		mInflator.inflate(R.layout.sidebar_main, this);
		mContentView = (RelativeLayout) findViewById(android.R.id.content);
		mBarView = (RelativeLayout) findViewById(android.R.id.background);
		mHolderView = (LinearLayout) findViewById(R.id.scroll_view_holder);

		if (mService.mBarOnRight) {
			mBarView.setBackgroundResource(R.drawable.bg_right);
			findViewById(android.R.id.button2).setVisibility(View.GONE);
			mTabView = (ImageView) findViewById(android.R.id.button1);
		} else {
			mBarView.setBackgroundResource(R.drawable.bg_left);
			findViewById(android.R.id.button1).setVisibility(View.GONE);
			mTabView = (ImageView) findViewById(android.R.id.button2);
		}
		mTabView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getActionMasked()) {
				case MotionEvent.ACTION_MOVE:
				case MotionEvent.ACTION_DOWN:
					mTabView.setImageState(
							new int[] { android.R.attr.state_pressed }, false);
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					mTabView.setImageState(
							new int[] { android.R.attr.state_empty }, false);
					break;
				}
				mService.tabTouchEvent(event);
				return true;
			}
		});
	}

	public void applySidebarWidth(int dp) {
		ViewGroup.LayoutParams bar_param = mBarView.getLayoutParams();
		bar_param.width = mService.mAppColumns * Util.dp(dp, mService);
	}

	public void setTabSize(int dp) {
		RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) mTabView
				.getLayoutParams();
		param.width = Util.dp(dp, getContext());
		mTabView.setLayoutParams(param);
	}

	public void setMarginFromTop(int top) {
		mTabView.setPadding(0, top, 0, 0);
	}

	public void addApps(final SharedPreferences pref, final PackageManager pm) {
		final Map<String, ?> map = pref.getAll();
		if (map.size() == 0) {
			Util.toast(mService, R.string.app_list_empty);
			return;
		}

		mHolderView.addView(new ProgressBar(mService));

		new Thread(new Runnable() {
			@Override
			public void run() {
				mItemViews = new SidebarItemView[map.size() * 2];

				for (Entry<String, ?> entry : map.entrySet()) {
					SidebarItemView item = new SidebarItemView(mService,
							mInflator) {
						@Override
						public void touchEventHelper(MotionEvent event,
								boolean long_press_verified) {
							itemViewTouchEventHelper(this, event,
									long_press_verified);
						}
					};
					try {
						ApplicationInfo info = pm.getApplicationInfo(
								entry.getKey(), 0);
						item.setIcon(info.loadIcon(pm));
						item.setLabel(info.loadLabel(pm));
						item.setPkg(entry.getKey());
					} catch (NameNotFoundException e) {
					}
					mItemViews[(Integer) entry.getValue()] = item;

				}

				insertViews();

			}
		}).start();
	}

	private void insertViews() {
		final int columns = mService.mAppColumns;
		Runnable r = new Runnable() {
			@Override
			public void run() {
				mHolderView.removeAllViews();
				if (columns == 1) {
					for (SidebarItemView view : mItemViews) {
						if (view != null)
							mHolderView.addView(view);
					}
				} else {
					for (int x = 0; x < mItemViews.length; x = x + columns) {
						LinearLayout layout = new LinearLayout(mService);
						layout.setOrientation(LinearLayout.HORIZONTAL);
						for (int y = x; y < x + columns
								&& y < mItemViews.length; y++) {
							SidebarItemView item = mItemViews[y];
							if (item != null) {
								item.setLayoutParams(new LinearLayout.LayoutParams(
										0,
										LinearLayout.LayoutParams.WRAP_CONTENT,
										0.5f));
								layout.addView(item);
							}
						}
						mHolderView.addView(layout);
					}
				}
			}
		};
		mService.mHandler.post(r);
	}

	private void itemViewTouchEventHelper(SidebarItemView item,
			MotionEvent event, boolean long_press_verified) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			setReceiveAllTouchEvents(true);
			break;
		case MotionEvent.ACTION_MOVE:
			if (long_press_verified) {
				mTransferTouchEventsToSidebarItemView = true;
				SidebarDraggedOutView view = SidebarDraggedOutView
						.getInstance(item);
				if (view.showView(item.getIcon())) {
					item.setToEmptyIcon(true);
					view.setPosition(event.getRawX(), event.getRawY(), false);
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			SidebarDraggedOutView view = SidebarDraggedOutView
					.getInstance(item);
			view.hideView();
			item.setToEmptyIcon(false);
			setReceiveAllTouchEvents(false);
		case MotionEvent.ACTION_CANCEL:
			break;
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_OUTSIDE:
			mService.mHandler.removeCallbacks(runnable);
			mService.hideBar();
			break;
		case MotionEvent.ACTION_MOVE:
			if (mTransferTouchEventsToSidebarItemView) {
				SidebarDraggedOutView view = SidebarDraggedOutView
						.getInstance(null);
				final boolean icon_inside_sidebar = mRect.contains(
						(int) event.getRawX(), (int) event.getRawY());
				view.setPosition(event.getRawX(), event.getRawY(),
						!icon_inside_sidebar);
				return true;
			}
		case MotionEvent.ACTION_DOWN:
			int[] pos = new int[2];
			mBarView.getLocationOnScreen(pos);
			mRect = new Rect(pos[0], 0, pos[0] + mBarView.getMeasuredWidth(),
					getResources().getDisplayMetrics().heightPixels);
			mService.mHandler.removeCallbacks(runnable);
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (mTransferTouchEventsToSidebarItemView) {
				mTransferTouchEventsToSidebarItemView = false;
				SidebarDraggedOutView view = SidebarDraggedOutView
						.getInstance(null);
				final boolean icon_inside_sidebar = mRect.contains(
						(int) event.getRawX(), (int) event.getRawY());
				if (!icon_inside_sidebar) {
					view.launch();
				}
				view.hideView();
			}
			mService.mHandler
					.postDelayed(runnable, Common.TIMEOUT_HIDE_SIDEBAR);
			break;
		}
		return super.dispatchTouchEvent(event);
	}

	final Runnable runnable = new Runnable() {
		@Override
		public void run() {
			try {
				mService.hideBar();
			} catch (NullPointerException e) {
			}
		}
	};

	public void animateView(boolean visible) {
		if (visible) {
			mService.addView(this);
			TranslateAnimation anim = new TranslateAnimation(
					Animation.RELATIVE_TO_PARENT, mService.mBarOnRight ? 1.0f
							: -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, 0, 0,
					0, 0);
			anim.setDuration(mService.mAnimationTime);
			mContentView.startAnimation(anim);
			mService.mHandler
					.postDelayed(runnable, Common.TIMEOUT_HIDE_SIDEBAR);
		} else {
			TranslateAnimation anim = new TranslateAnimation(
					Animation.RELATIVE_TO_PARENT, 0.0f,
					Animation.RELATIVE_TO_PARENT, mService.mBarOnRight ? 1.0f
							: -1.0f, 0, 0, 0, 0);
			anim.setDuration(mService.mAnimationTime);
			anim.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					mService.safelyRemoveView(SidebarHolderView.this);
				}
			});
			mContentView.startAnimation(anim);
		}
	}

	public boolean setReceiveAllTouchEvents(boolean yes) {
		WindowManager.LayoutParams param = (WindowManager.LayoutParams) getLayoutParams();
		if (yes) {
			param.flags = param.flags
					& ~WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
		} else {
			param.flags = param.flags
					| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
		}
		try {
			mService.mWindowManager.updateViewLayout(this, param);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}