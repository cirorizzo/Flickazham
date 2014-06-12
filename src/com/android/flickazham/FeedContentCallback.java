package com.android.flickazham;

import android.app.ProgressDialog;

public interface FeedContentCallback {
	
	void gotResults();
	
	void gotErrors(String aError);
	
	void updateBar(String aFirstValue, String aSecondValue);

}
