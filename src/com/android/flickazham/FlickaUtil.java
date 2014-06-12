package com.android.flickazham;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class FlickaUtil {
	public final static String KILLING_COMMAND = "KILLING_COMMAND";
	
    public boolean isConnectivityOn(Context ctx) {
    	boolean resCode = false;

    	try {
    		ConnectivityManager cm =
    				(ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

    		resCode = cm.getActiveNetworkInfo().isConnectedOrConnecting();

    	} catch (Exception e) {
    		// TODO: handle exception
    	}

    	return resCode;
    }
}
