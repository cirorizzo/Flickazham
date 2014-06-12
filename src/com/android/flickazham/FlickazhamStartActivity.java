package com.android.flickazham;

import com.android.flickazham.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;


public class FlickazhamStartActivity extends Activity {
	
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	private Handler mSplashHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_flickazham_start);

		final View contentView = findViewById(R.id.main_title_content);

		mSplashHandler = new Handler();
		mSplashHandler.postDelayed(runnableSplashHandler, AUTO_HIDE_DELAY_MILLIS);
		
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startKwazham();
			}
		});

	}

	private Runnable runnableSplashHandler = new Runnable() {

		@Override
		public void run() {			
			startKwazham();
		}		
	};

	private void startKwazham() {
		mSplashHandler.removeCallbacks(runnableSplashHandler);
		
		startActivity(new Intent(this, PhotoList.class));
		
		FlickazhamStartActivity.this.finish();
	}

}
