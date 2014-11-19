package ru.sezex.sidebar;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashScreen extends Activity {
	public ProgressDialog myDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.splashscreen);

		new Handler().postDelayed(new Runnable() {

			public void run() {
				myDialog = ProgressDialog.show(SplashScreen.this, "",
						"Loading", true);

				Intent intent = new Intent(SplashScreen.this,
						MainActivity.class);
				SplashScreen.this.startActivity(intent);
				myDialog.dismiss();
				SplashScreen.this.finish();
			}

		}, 3000); // 3 Seconds
	}
};