package com.android.flickazham;

import java.util.List;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.flickazham.PhotoFeedContent.PhotoItem;

public class PhotoItemFragment extends Fragment implements FeedContentCallback {
	public final static String PHOTO_OWNER = "com.android.flickazham.photo_owner";
	public final static String PHOTO_USERNAME = "com.android.flickazham.photo_username";
	
	private String mPhotoOwner;
	private String mPhotoUserName;
	
	private GridView galleryPhotoUser;
	private TextView txtVwSearchContent;
	private TextView txtVwPaging;
	
	private ImageButton imgBtnPrevFragment;
	private ImageButton imgBtnNexFragment;
	
	private GalleryImagePagerAdapter mAdapter;
	
	private PhotoFeedContent photoFeedContent;
	
	public FragmentAsyncHandler aHandler;
	
	private int countPage = 1;
	
	

	public PhotoItemFragment() {
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		if (getArguments().containsKey(PHOTO_OWNER)) 
			mPhotoOwner = getArguments().getString(PHOTO_OWNER);
		
		if (getArguments().containsKey(PHOTO_USERNAME)) 
			mPhotoUserName = getArguments().getString(PHOTO_USERNAME);
		
		aHandler = new FragmentAsyncHandler();
				
		photoFeedContent = new PhotoFeedContent(this, getActivity(), 
				aHandler, countPage, mPhotoOwner);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_user_photo_gallery, container, false);
		galleryPhotoUser = (GridView) rootView.findViewById(R.id.galleryPhotoUser);
		txtVwSearchContent = (TextView) rootView.findViewById(R.id.txtVwSearchContent);
		txtVwPaging = (TextView) rootView.findViewById(R.id.txtVwPaging);
		imgBtnPrevFragment = (ImageButton) rootView.findViewById(R.id.imgBtnPrevFragment);
		imgBtnPrevFragment.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (countPage > 1) {
					photoFeedContent = new PhotoFeedContent(PhotoItemFragment.this, getActivity(), aHandler, --countPage, mPhotoOwner);
				}
			}
		});
		
		imgBtnNexFragment = (ImageButton) rootView.findViewById(R.id.imgBtnNexFragment);
		imgBtnNexFragment.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				photoFeedContent = new PhotoFeedContent(PhotoItemFragment.this, getActivity(), aHandler, ++countPage, mPhotoOwner);
			}
			
		});
		
		return rootView;
	}
	
	public class GalleryImagePagerAdapter extends ArrayAdapter<PhotoItem> {

		public GalleryImagePagerAdapter(Context context, int resource, List<PhotoItem> objects) {
			super(context, resource, objects);
		}
		
		@Override
	    public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView;
	        if (convertView == null) { 
	            imageView = new ImageView(getActivity().getApplicationContext());
	            imageView.setLayoutParams(new GridView.LayoutParams(125, 125));
	            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
	            imageView.setPadding(8, 8, 8, 8);
	        } else {
	            imageView = (ImageView) convertView;
	        }

	        PhotoItem mPhotoItem = getItem(position);
	        
	        imageView.setImageBitmap(mPhotoItem.photoThumbnail);
			
			return imageView;
		}

	}
	
	public class FragmentAsyncHandler extends Handler {
		public static final int ID_PHOTOLIST_USER_DOWNLOADED = 0;
		public static final int ID_PHOTOLIST_FEED_GENERAL_DATA = 1;
		public static final int ID_PHOTOLIST_USER_PHOTO_DOWNLOADED = 2;

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ID_PHOTOLIST_USER_DOWNLOADED:

				break;
			case ID_PHOTOLIST_FEED_GENERAL_DATA:
				if (msg.obj != null) {
					updateBar("", (String) msg.obj);
				}
				break;

			case ID_PHOTOLIST_USER_PHOTO_DOWNLOADED:

				gotResults();
			}
		}		
	}

	@Override
	public void gotResults() {
		galleryPhotoUser.setAdapter(new GalleryImagePagerAdapter(getActivity().getApplicationContext(), countPage, photoFeedContent.listPhotoItems));
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
	
	@Override
	public void updateBar(String aFirstValue, String aSecondValue) {
		final String mResult = aFirstValue;
		final String mOfPage = aSecondValue;
		txtVwSearchContent.setText(String.format(getResources().getString(R.string.result_for_user), mPhotoUserName));
		txtVwPaging.setText(String.format(getResources().getString(R.string.paging_feed), mOfPage)); 
	}


}
