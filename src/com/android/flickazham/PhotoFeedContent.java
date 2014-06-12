package com.android.flickazham;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import android.R.fraction;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.flickazham.PhotoItemFragment.FragmentAsyncHandler;
import com.android.flickazham.PhotoList.AsyncHandler;

public class PhotoFeedContent {
	private FeedContentCallback mCallback;
	private Context ctx;

	private final String NULL_USER = "Unknown User Name";
	private final String NUMBERS_OF_PHOTO_PER_PAGES = "15";
	private final String NUMBERS_OF_USER_PHOTO_PAGES = "30";
	private final String SEARCH_TAG = "squirrel";
	
	/*--------------------------------------------------
	 * Errors Management Constants
	 * --------------------------------------------------
	 */
	public static final String NO_DATA_FOUND = "com.android.flickazham.NO_DATA_FOUND";
	public static final String NO_DATA_CONNECTION = "com.android.flickazham.NO_DATA_CONNECTION";
	public static final String ERROR_CAUGHT_PRE = "com.android.flickazham.ERROR_CAUGHT: ";
	public static final String ERROR_CAUGHT = "com.android.flickazham.ERROR_CAUGHT: %1s";


	/*--------------------------------------------------
	 * HTTP/HTTPS Connection Timeout
	 * --------------------------------------------------
	 */
	private final int FEED_CONTENTS_CONNECTION_TIMEOUT = 5000;

	
	/*--------------------------------------------------
	 * Common URI for Flickr API tags
	 * --------------------------------------------------
	 */
	private final String FLICKR_API_BASE_URI = "https://api.flickr.com/services/rest/";
	private final String FLICKR_API_KEY = "&api_key=91adbc4b8a91c5a9cf7c73a0521011c4";
	private final String FLICKR_API_FORMAT = "&format=json";
	private final String FLICKR_API_PAGE = "&page=";
	private final String FLICKR_API_PER_PAGE = "&per_page=";
	private final String FLICKR_API_MEDIA = "&media=photos";
	private final String FLICKR_API_NOJSONCALLBACK = "&nojsoncallback=1";

	
	/*--------------------------------------------------
	 * PhhotoItem List
	 * --------------------------------------------------
	 */
	public List<PhotoItem> listPhotoItems = new ArrayList<PhotoItem>();

	
	/*--------------------------------------------------
	 * Handlers
	 * --------------------------------------------------
	 */
	private DownloadHandler downloadHandler;
	private AsyncHandler mHandler;
	private FragmentAsyncHandler sHandler;	
	
	private ProgressDialog ringProgressDialog;
	
	/*--------------------------------------------------
	 * PhotoFeedContent Constructor for Main View
	 * --------------------------------------------------
	 */
	public PhotoFeedContent(FeedContentCallback aCallback, Context aCtx, 
			AsyncHandler aHandler, int aPage) {
		
		instPhotoFeedContent(aCallback, aCtx);
		
		this.mHandler = aHandler;
		
		
		startWaitingProgress();
		MainDownloadPhotoFeed mainDwnPhotoFeedTask = new MainDownloadPhotoFeed();
		mainDwnPhotoFeedTask.execute(new String[] {String.valueOf(aPage)});
	}

	/*--------------------------------------------------
	 * PhotoFeedContent Constructor for Detail Fragment View
	 * --------------------------------------------------
	 */
	public PhotoFeedContent(FeedContentCallback aCallback, Context aCtx, 
			FragmentAsyncHandler aHandler, int aPage, String usrID) {
		
		instPhotoFeedContent(aCallback, aCtx);
		
		this.sHandler = aHandler;
		
		startWaitingProgress();
		DetailDownloadUserPhotoFeed detailDwnUserPhotoFeedTask = new DetailDownloadUserPhotoFeed();
		detailDwnUserPhotoFeedTask.execute(new String[] {String.valueOf(aPage), usrID});
	}

	public void instPhotoFeedContent(FeedContentCallback aCallback, Context aCtx) {
		this.mCallback = aCallback;
		this.ctx = aCtx;
		
		downloadHandler = new DownloadHandler();
	}
	
	/*--------------------------------------------------
	 * PhotoItem and List PhotoItem Objects
	 * --------------------------------------------------
	 */
	
	private void addItem(PhotoItem item) {
		listPhotoItems.add(item);
	}

	public int getCount() {
		return listPhotoItems.size();
	}
	
	public void setThumbnail(Thumbnail aThumbnail) {
		PhotoItem mPhotoItem = listPhotoItems.get(aThumbnail.id);
		mPhotoItem.photoThumbnail = aThumbnail.thumbnailPhoto;
		listPhotoItems.set(aThumbnail.id, mPhotoItem);
	}

	public void setInfoUser(InfoUser aInfoUser) {
		PhotoItem mPhotoItem = listPhotoItems.get(aInfoUser.id);
		mPhotoItem.photoUserName = aInfoUser.infoUser;
		listPhotoItems.set(aInfoUser.id, mPhotoItem);
	}
	
	public class PhotoItem {
		public String id;
		public String photoId;
		public String photoOwner;
		public String photoUserName;
		public String photoSecret;
		public String photoServer;
		public String photoFarm;
		public String photoTitle;
		public String photoLink;
		public Bitmap photoThumbnail;
		

		public PhotoItem(String id, String photoId, String photoOwner, 
				String photoSecret, String photoServer, String photoFarm, 
				String photoTitle, String photolLink) {
			this.id = id;
			this.photoId = photoId;
			this.photoOwner = photoOwner;
			this.photoUserName = null;
			this.photoSecret = photoSecret;
			this.photoServer = photoServer;
			this.photoFarm = photoFarm;
			this.photoTitle = photoTitle;
			this.photoLink = photolLink;
			this.photoThumbnail = null; //Or Standard Image
			
		}

		@Override
		public String toString() {
			return photoTitle;
		}
	}
	
	private class Thumbnail {
		int id;
		Bitmap thumbnailPhoto;
		
		private Thumbnail(int id, Bitmap thumbnailPhoto) {
			this.id = id;
			this.thumbnailPhoto = thumbnailPhoto;
		}
	}

	private class InfoUser {
		int id;
		String infoUser;
		
		private InfoUser(int id, String infoUser) {
			this.id = id;
			this.infoUser = infoUser;
		}
	}
	
	/*
	 *-------------------------------------------------- 
	 */
	
	

	/*
	 * Main View Async Class
	 */	
	public class MainDownloadPhotoFeed extends AsyncTask<String, Void, String> {
		private final String FLICKR_API_METHOD_SEARCH = "?method=flickr.photos.search";
		private final String FLICKR_API_TAGS = "&tags=";
		
		@Override
		protected String doInBackground(String... params) {
			String mFlickrURI = FLICKR_API_BASE_URI +
					FLICKR_API_METHOD_SEARCH +
					FLICKR_API_KEY +
					FLICKR_API_TAGS + SEARCH_TAG +
					FLICKR_API_FORMAT +
					FLICKR_API_PAGE + params[0] +
					FLICKR_API_PER_PAGE + NUMBERS_OF_PHOTO_PER_PAGES +
					FLICKR_API_MEDIA +
					FLICKR_API_NOJSONCALLBACK;


			String resStr = NO_DATA_FOUND;
			try {
				resStr = getConnectingSTR(mFlickrURI);

			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				resStr = String.format(ERROR_CAUGHT, e.getMessage());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				resStr = String.format(ERROR_CAUGHT, e.getMessage());
			}

			return resStr;
		}

		@Override
		protected void onPostExecute(String result) {
			if (!isResultInError(result)) {
				JSONObject jObjRoot;
				try {
					jObjRoot = new JSONObject(result);

					if (isResultValid(jObjRoot)) {
						JSONObject jObj = jObjRoot.getJSONObject("photos");

						if (isResultValid(jObj)) {
							String mPage = String.format("%1s of %1s", jObj.getString("page"), jObj.getString("pages"));

							Message generalDataMsg = Message.obtain(mHandler, AsyncHandler.ID_PHOTOLIST_FEED_GENERAL_DATA);
							generalDataMsg.obj = mPage;
							mHandler.sendMessage(generalDataMsg);

							fillingListPhotoItem(jObj);
						}

						Message msg = Message.obtain(downloadHandler, DownloadHandler.ID_PHOTOLIST_START_THUMBNAIL_DOWNLOADING);
						downloadHandler.sendMessage(msg);
					} else {
						if (mCallback != null)
							mCallback.gotErrors(result);
					}

				} catch (JSONException e) {
					e.printStackTrace();
					if (mCallback != null)
						mCallback.gotErrors(result);
				}
			} else {
				if (mCallback != null)
					mCallback.gotErrors(result);
			}
		}
	}
	

	public class MainDownloadPhotoThumb extends ParentDownloadPhotoOfUser {
		@Override
		protected Void doInBackground(List<PhotoItem>... params) {
			return super.doInBackground(params);
		}

		@Override
		protected void onPostExecute(Void dummy) {
			Message msg = Message.obtain(downloadHandler, DownloadHandler.ID_PHOTOLIST_START_USERINFO_DOWNLOADING);
			downloadHandler.sendMessage(msg);
		}		
	}
	
	
	public class MainDownloadPhotoUserInfo extends AsyncTask<List<PhotoItem>, Void, Void> {
		private InfoUser mInfoUser;
		private final String FLICKR_API_METHOD_GETINFO = "?method=flickr.people.getInfo";
		private final String FLICKR_API_PARAM_USERID = "&user_id=";
		
	
		@Override
		protected Void doInBackground(List<PhotoItem>... params) {
			String aURLPhotoUserInfo = "";
			
			List<PhotoItem> mListPhItm = params[0];
			
			String resUserName = NULL_USER;
			
			for (int i = 0; i < mListPhItm.size(); i++) {
				aURLPhotoUserInfo = FLICKR_API_BASE_URI + 
						FLICKR_API_METHOD_GETINFO +
						FLICKR_API_KEY + 
						FLICKR_API_PARAM_USERID + mListPhItm.get(i).photoOwner +
						FLICKR_API_FORMAT +
						FLICKR_API_NOJSONCALLBACK;
							
				try {
					resUserName = extractUserNameJSON(getConnectingSTR(aURLPhotoUserInfo));
					
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					resUserName = NULL_USER;
				} catch (IOException e) {
					e.printStackTrace();
					resUserName = NULL_USER;
				}
				
				mInfoUser = new InfoUser(i, resUserName);
				setInfoUser(mInfoUser);
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void dummy) {
			//mCallback.gotResults();
			
			Message msg = Message.obtain(mHandler, AsyncHandler.ID_PHOTOLIST_FEED_DOWNLOADED);			
			mHandler.sendMessage(msg);	
			
			stopWaitingProgress();
		}		
	}
	
	private String extractUserNameJSON(String aJSONResult) {
		String resStrUserInfo = NULL_USER;
		
		try {
			JSONObject jObject = new JSONObject(aJSONResult).getJSONObject("person").getJSONObject("username");
			resStrUserInfo = jObject.getString("_content");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		
		
		return resStrUserInfo;
	}
	/*
	 *-------------------------------------------------- 
	 */

	
	
	
	
	/*
	 * Detail View Async Class
	 */	
	public class DetailDownloadUserPhotoFeed extends AsyncTask<String, Void, String> {
		private final String FLICKR_API_METHOD_USER = "?method=flickr.people.getPhotos";
		private final String FLICKR_API_PARAM_USER_ID = "&user_id=";
		private final String FLICKR_API_PARAM_PRIVACY_FILTER = "&privacy_filter=1";
		private final String FLICKR_API_PARAM_CONTENT_TYPE = "&content_type=1";
		
		@Override
		protected String doInBackground(String... params) {
			String mFlickrURI = FLICKR_API_BASE_URI +
					FLICKR_API_METHOD_USER +
					FLICKR_API_KEY +
					FLICKR_API_PARAM_USER_ID +  params[1] +
					FLICKR_API_PARAM_PRIVACY_FILTER +
					FLICKR_API_PARAM_CONTENT_TYPE +
					FLICKR_API_FORMAT +
					FLICKR_API_PAGE + params[0] +
					FLICKR_API_PER_PAGE + NUMBERS_OF_USER_PHOTO_PAGES +
					FLICKR_API_MEDIA +
					FLICKR_API_NOJSONCALLBACK;
			
			String resStr = NO_DATA_FOUND;
			try {
				FlickaUtil mFlickaUtil = new FlickaUtil();
				if (mFlickaUtil.isConnectivityOn(ctx)) {
					Connection.Response respObj = Jsoup.connect(mFlickrURI)
							.ignoreContentType(true)
							.timeout(FEED_CONTENTS_CONNECTION_TIMEOUT)
							.execute();

					if (respObj.statusCode() == 200)  
						resStr = respObj.body();
				} else 
					resStr = NO_DATA_CONNECTION;

			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				resStr = String.format(ERROR_CAUGHT, e.getMessage());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				resStr = String.format(ERROR_CAUGHT, e.getMessage());
			}

			return resStr;
		}

		@Override
		protected void onPostExecute(String result) {
			JSONObject jObjRoot;
			try {
				jObjRoot = new JSONObject(result);


				if (isResultValid(jObjRoot)) {
					JSONObject jObj = jObjRoot.getJSONObject("photos");

					if (isResultValid(jObj)) {
						String mPage = String.format("%1s of %1s", jObj.getString("page"), jObj.getString("pages"));

						Message generalDataMsg = Message.obtain(mHandler, FragmentAsyncHandler.ID_PHOTOLIST_FEED_GENERAL_DATA);
						generalDataMsg.obj = mPage;
						sHandler.sendMessage(generalDataMsg);

						fillingListPhotoItem(jObj);
					}

					Message msg = Message.obtain(downloadHandler, DownloadHandler.ID_PHOTOLIST_START_USER_PHOTOS_DOWNLOADING);
					downloadHandler.sendMessage(msg);

				} else {
					if (mCallback != null)
						mCallback.gotErrors(result);
				}
			} catch (JSONException e) {
				e.printStackTrace();
				if (mCallback != null)
					mCallback.gotErrors(result);
			}
		}	
	}

	public class DetailDownloadPhotoOfUser extends ParentDownloadPhotoOfUser {
		@Override
		protected Void doInBackground(List<PhotoItem>... params) {
			return super.doInBackground(params);
		}
		
		@Override
		protected void onPostExecute(Void dummy) {
			Message msg = Message.obtain(sHandler, FragmentAsyncHandler.ID_PHOTOLIST_USER_PHOTO_DOWNLOADED);
			sHandler.sendMessage(msg);
			
			super.onPostExecute(dummy);
		}		
	}
	/*
	 *-------------------------------------------------- 
	 */
	
	
	
	/*
	 * Parent Async Class to manage Photo BMP Object
	 */
	public class ParentDownloadPhotoOfUser extends AsyncTask<List<PhotoItem>, Void, Void> {
		private final String FLICKR_API_GETIMAGE = "https://farm%1s.staticflickr.com/%1s/%1s_%1s_%1s.jpg";
		
		@Override
		protected Void doInBackground(List<PhotoItem>... params) {
			Thumbnail mThumbnail;
			String aURLPhoto = "";
			Bitmap resBMP = null;
			
			List<PhotoItem> mListPhItm = params[0];

			for (int i = 0; i < mListPhItm.size(); i++) {
				aURLPhoto = String.format(FLICKR_API_GETIMAGE,
						mListPhItm.get(i).photoFarm,
						mListPhItm.get(i).photoServer,
						mListPhItm.get(i).photoId, 
						mListPhItm.get(i).photoSecret,
						"m");

				try {
					resBMP = getConnectingBMP(aURLPhoto);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					resBMP = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.no_photo);
				} catch (IOException e) {
					e.printStackTrace();
					resBMP = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.no_photo);
				}
				
				mThumbnail = new Thumbnail(i, resBMP);
				setThumbnail(mThumbnail);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void dummy) {
			stopWaitingProgress();
		}		
	}
	/*
	 *-------------------------------------------------- 
	 */

	
	/*
	 * Commons Filling PhotoItem List Methods from JSON	
	 */
	private void fillingListPhotoItem(JSONObject aJObj) throws JSONException {
		JSONArray jArray = new JSONArray();

		jArray = aJObj.getJSONArray("photo");

		for (int i = 0; i < jArray.length(); i++) {
			String photoId = jArray.getJSONObject(i).getString("id");
			String photoOwner = jArray.getJSONObject(i).getString("owner");
			String photoSecret = jArray.getJSONObject(i).getString("secret");
			String photoServer = jArray.getJSONObject(i).getString("server");
			String photoFarm = jArray.getJSONObject(i).getString("farm");
			String photoTitle = jArray.getJSONObject(i).getString("title");
			String photolLink = jArray.getJSONObject(i).getString("id");

			addItem(new PhotoItem(String.valueOf(i), 
					photoId, 
					photoOwner, 
					photoSecret,
					photoServer,
					photoFarm,
					photoTitle, 
					photolLink));
			Log.d("Flickazham", String.valueOf(i) + " - " + photoTitle);
		}
	}
	/*
	 *-------------------------------------------------- 
	 */
	
	
	/*--------------------------------------------------
	 * Commons Jsoup Connection Methods
	 * --------------------------------------------------
	 */
	private String getConnectingSTR(String aURI) throws IOException {
		String resStr = NO_DATA_FOUND;
		FlickaUtil mFlickaUtil = new FlickaUtil();
		if (mFlickaUtil.isConnectivityOn(ctx)) {
			Connection.Response respObj = Jsoup.connect(aURI)
					.ignoreContentType(true)
					.timeout(FEED_CONTENTS_CONNECTION_TIMEOUT)
					.execute();


			if (respObj.statusCode() == 200)  
				resStr = respObj.body();
		} else 
			resStr = NO_DATA_CONNECTION;

		return resStr;
	}
	
	private Bitmap getConnectingBMP(String aURI) throws IOException {
		Bitmap resBMP = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.no_photo);
		FlickaUtil mFlickaUtil = new FlickaUtil();
		if (mFlickaUtil.isConnectivityOn(ctx)) {
			Connection.Response respObj = Jsoup.connect(aURI)
					.ignoreContentType(true)
					.timeout(FEED_CONTENTS_CONNECTION_TIMEOUT)
					.execute();


			if (respObj.statusCode() == 200)  
				resBMP = BitmapFactory.decodeByteArray(respObj.bodyAsBytes(), 0, respObj.bodyAsBytes().length);
		} else 
			resBMP = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.no_photo);

		return resBMP;
	}	
	/*
	 *-------------------------------------------------- 
	 */
	
	
	
	/*--------------------------------------------------
	 * Error Checking Methods
	 * --------------------------------------------------
	 */
	private boolean isResultInError(String result) {
		return (result.isEmpty() ||
				result.startsWith(NO_DATA_FOUND) || 
				result.startsWith(NO_DATA_CONNECTION) ||
				result.startsWith(ERROR_CAUGHT_PRE));
	}
	
	private boolean isResultValid(JSONObject result) {
		return (result != null);
	}
	/*
	 *-------------------------------------------------- 
	 */
	
	
	/*--------------------------------------------------
	 * Downloading Handler Class
	 * --------------------------------------------------
	 */
	
	public class DownloadHandler extends Handler {
		public static final int ID_PHOTOLIST_START_THUMBNAIL_DOWNLOADING = 0;
		public static final int ID_PHOTOLIST_START_USERINFO_DOWNLOADING = 1;
		public static final int ID_PHOTOLIST_START_USER_PHOTOS_DOWNLOADING = 2;
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ID_PHOTOLIST_START_THUMBNAIL_DOWNLOADING:
				
				MainDownloadPhotoThumb mainDwnPhotoThumb = new MainDownloadPhotoThumb();
				mainDwnPhotoThumb.execute(listPhotoItems);
				break;
			case ID_PHOTOLIST_START_USERINFO_DOWNLOADING:
				
				MainDownloadPhotoUserInfo mainDwnPhotoUserInfo = new MainDownloadPhotoUserInfo();
				mainDwnPhotoUserInfo.execute(listPhotoItems);
				break;				
			case ID_PHOTOLIST_START_USER_PHOTOS_DOWNLOADING:
				
				DetailDownloadPhotoOfUser detailDwnPhotoOfUser = new DetailDownloadPhotoOfUser();
				detailDwnPhotoOfUser.execute(listPhotoItems);				
				break;
			}
		}

	}	
	/*
	 *-------------------------------------------------- 
	 */
	

	
	
	
	
	/*--------------------------------------------------
	 * ProgressDialog Management
	 * --------------------------------------------------
	 */
	
	private void startWaitingProgress() {
		// TODO Auto-generated method stub
		ringProgressDialog = ProgressDialog.show(this.ctx, "Please wait ...", "Downloading Data ...", true);		
	}

	private void stopWaitingProgress() {
		// TODO Auto-generated method stub
		if (ringProgressDialog.isShowing())
			ringProgressDialog.dismiss();
	}
	
	/*
	 *-------------------------------------------------- 
	 */
}


