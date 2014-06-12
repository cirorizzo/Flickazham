package com.android.flickazham;

import java.util.List;

import android.app.DialogFragment;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.flickazham.PhotoFeedContent.PhotoItem;
import com.android.flickazham.R;

public class PhotoList extends ListActivity implements FeedContentCallback {

	public AsyncHandler aHandler;
	
	private PhotoFeedContent photoFeedContent;
	
	private TextView txtVwSearchContent;
	private TextView txtVwPaging;

	private final String SEARCHING_TAG = "squirrel";
	
	private int countPage = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_photo_list);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		txtVwSearchContent = (TextView) findViewById(R.id.txtVwSearchContent);
		txtVwPaging = (TextView) findViewById(R.id.txtVwPaging);		
		
		initFilter();
		
		aHandler = new AsyncHandler();
		
		photoFeedContent = new PhotoFeedContent(this, this, aHandler, countPage);
	}

	public void prevBtnClick(View v) {
		if (countPage > 1) {
			photoFeedContent = new PhotoFeedContent(this, this, aHandler, --countPage);
		}
	}
	
	public void nextBtnClick(View v) {
		photoFeedContent = new PhotoFeedContent(this, this, aHandler, ++countPage);
	}
	
	private void initFilter() {
		IntentFilter filter = new IntentFilter();

		filter.addAction(FlickaUtil.KILLING_COMMAND);
		
		registerReceiver(receiver, filter);	
	}

	@Override
	public void gotResults() {
		// TODO Auto-generated method stub
		setListAdapter(new CustomPhotoListAdapter(this, R.layout.item_photo_list, photoFeedContent.listPhotoItems));
	}

	
	public class CustomPhotoListAdapter extends ArrayAdapter<PhotoItem> {
		private ImageView imgPhotoThumbnail;
		private TextView txtVwTitle;
    	private TextView txtVwArtist;
    	
		public CustomPhotoListAdapter(Context context, int resource, List<PhotoItem> objects) {
			super(context, resource, objects);
		}
		
		@Override
	    public View getView(int position, View convertView, ViewGroup parent) {
    		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			convertView = inflater.inflate(R.layout.item_photo_list, null);
			
			imgPhotoThumbnail = (ImageView) convertView.findViewById(R.id.imgPhotoThumbnail);
			txtVwTitle = (TextView) convertView.findViewById(R.id.txtVwTitle);
			txtVwArtist = (TextView) convertView.findViewById(R.id.txtVwArtist);
			
			PhotoItem mPhotoItem = getItem(position);
	    	
			txtVwTitle.setText(Html.fromHtml(mPhotoItem.photoTitle));
			txtVwArtist.setText(Html.fromHtml(mPhotoItem.photoUserName));
			imgPhotoThumbnail.setImageBitmap(mPhotoItem.photoThumbnail);
			
			return convertView;		
		}
		
	}
	
	@Override
    protected void onListItemClick(ListView aListVw, View aView, int aPhotoPosition, long id) {
		super.onListItemClick(aListVw, aView, aPhotoPosition, id);
        
        String mPhotoOwner = photoFeedContent.listPhotoItems.get(aPhotoPosition).photoOwner;
        String mPhotoUserName = photoFeedContent.listPhotoItems.get(aPhotoPosition).photoUserName;
        
        Bundle arguments = new Bundle();
		arguments.putString(PhotoItemFragment.PHOTO_OWNER, mPhotoOwner);
		arguments.putString(PhotoItemFragment.PHOTO_USERNAME, mPhotoUserName);
		PhotoItemFragment fragment = new PhotoItemFragment();
		fragment.setArguments(arguments);
		try {
			getFragmentManager().beginTransaction().replace(R.id.container, fragment).addToBackStack(null).commit();
		} catch (Exception e) {
			// TODO: handle exception
			Log.e("Flickazham", e.getMessage());
		}
	}
	
	
	@Override
	public void onDestroy() {
		unregisterReceiver(receiver);
		
		super.onDestroy();
	}

	@Override
	public void gotErrors(String aError) {
		if (aError.equalsIgnoreCase(PhotoFeedContent.NO_DATA_FOUND)) {
			showDialog(R.string.dialog_title_nodatafound, R.string.dialog_message_nodatafound);
		} else if (aError.equalsIgnoreCase(PhotoFeedContent.NO_DATA_CONNECTION)) {
			showDialog(R.string.dialog_title_nodataconnection, R.string.dialog_message_nodataconnection);	    
		} else if (aError.startsWith(PhotoFeedContent.ERROR_CAUGHT_PRE)) {
			showDialog(R.string.dialog_title_errorcaught, R.string.dialog_message_errorcaught);
		}
	}

	
	private void showDialog(int aTitleID, int aMessageID) {
		DialogFragment uiDialogMessage = UIDialogMessage.newInstance(aTitleID, aMessageID);
		uiDialogMessage.show(getFragmentManager(), "dialog");
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intentReceiver) {
			if (intentReceiver.getAction().equalsIgnoreCase(FlickaUtil.KILLING_COMMAND)) {
				PhotoList.this.finish();
			} 
		}
	};

	
	public class AsyncHandler extends Handler {
		public static final int ID_PHOTOLIST_FEED_DOWNLOADED = 0;
		public static final int ID_PHOTOLIST_FEED_GENERAL_DATA = 1;

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ID_PHOTOLIST_FEED_DOWNLOADED:
				gotResults();
				break;
			case ID_PHOTOLIST_FEED_GENERAL_DATA:
				if (msg.obj != null) {
					updateBar(SEARCHING_TAG, (String) msg.obj);
				}

				break;
			}
		}		
	}

	@Override
	public void updateBar(String aFirstValue, String aSecondValue) {
		final String mResult = aFirstValue;
		final String mOfPage = aSecondValue;
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	txtVwSearchContent.setText(String.format(getResources().getString(R.string.result_for), mResult));
        		txtVwPaging.setText(String.format(getResources().getString(R.string.paging_feed), mOfPage));    		
            }
        });
	}


	
	
}
