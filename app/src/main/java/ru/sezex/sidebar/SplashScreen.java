package ru.sezex.sidebar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashScreen extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.splashscreen);

		new Handler().postDelayed(new Runnable() {

			public void run() {
				Intent intent = new Intent(SplashScreen.this,
						MainActivity.class);
				SplashScreen.this.startActivity(intent);
				SplashScreen.this.finish();
			}

		}, 3000); // 3 Seconds
	}
}