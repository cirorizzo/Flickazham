package com.android.flickazham;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.android.flickazham.R;

public class UIDialogMessage extends DialogFragment {

	public static UIDialogMessage newInstance(int aTitleID, int aMessageID) {
		UIDialogMessage frag = new UIDialogMessage();
		Bundle args = new Bundle();
		args.putInt("titleID", aTitleID);
		args.putInt("messageID", aMessageID);
		frag.setArguments(args);
		return frag;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		int mTitleID = getArguments().getInt("titleID");
		int mMessageID = getArguments().getInt("messageID");
		
		
		return new AlertDialog.Builder(getActivity())
		.setTitle(mTitleID)
		.setMessage(mMessageID)
		.setPositiveButton(getResources().getString(R.string.dialog_button_gotcha),
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
				killApp();
			}
		})
		.create();
	}
	
	private void killApp() {
		Intent broadcastCallBack = new Intent();	
		broadcastCallBack.setAction(FlickaUtil.KILLING_COMMAND);
		getActivity().sendBroadcast(broadcastCallBack);
	}
}
