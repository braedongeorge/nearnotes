package com.nearnotes;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastore.Role;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxPrincipal;
import com.nearnotes.OverflowDialog.OverflowDialogListener;

// This is the sharing dialog, which allows users to change permissions and send links to other users.
public class ShareDialog extends DialogFragment {
	private DbxDatastore datastore;
	private View v;
	DbxDatastore.SyncStatusListener listener;
	private DbxAccount mAccount;
	private boolean mIsManaged;
	ShareDialogListener mListener;

	public ShareDialog(DbxDatastore datastore, DbxAccount account, boolean isManaged) {
		super();
		this.datastore = datastore;
		mAccount = account;
		mIsManaged = isManaged;
		if (!isManaged) {
			if (datastore.isWritable()) {
				datastore.setRole(DbxPrincipal.PUBLIC, Role.EDITOR);
				try {
					datastore.sync();
				} catch (DbxException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	public interface ShareDialogListener {
		public void onUnshareSelected(DbxDatastore store);
		
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Verify that the host activity implements the callback interface
		try {
			mListener = (ShareDialogListener) activity; 	// Instantiate the NoticeDialogListener so we can send events to the host
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement ShareDialogListener");  // The activity doesn't implement the interface, throw exception
		}
	}


	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		v = inflater.inflate(R.layout.sharing, null);

		TextView text = (TextView) v.findViewById(R.id.note_url);
		text.setText("https://www.nearnotes.com/#" + datastore.getId());
		text.setTextIsSelectable(true);

		String team = mAccount.getAccountInfo().orgName;
		v.findViewById(R.id.teamRoleRow).setVisibility(team.isEmpty() ? View.INVISIBLE : View.VISIBLE);
		CheckBox box = (CheckBox) v.findViewById(R.id.checkbox_team);
		box.setText(team.isEmpty() ? "" : "Restrict to my team (" + team + ")");
		box.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				boolean checked = ((CheckBox) v).isChecked();
				if (checked) {
					datastore.setRole(DbxPrincipal.TEAM, Role.EDITOR);
					datastore.setRole(DbxPrincipal.PUBLIC, Role.NONE);
					try {
						datastore.sync();
					} catch (DbxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else {
					datastore.setRole(DbxPrincipal.PUBLIC, Role.EDITOR);
					try {
						datastore.sync();
					} catch (DbxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}

		});
		
		if (mIsManaged && !team.isEmpty()) {
			if (datastore.getRole(DbxPrincipal.TEAM).equals(Role.EDITOR)) {
				box.setChecked(true);
			} else {
				box.setChecked(false);
			}
			
			
		}
		
		
		if (mIsManaged) {
			
			Button unShareBtn = (Button) v.findViewById(R.id.unShareButton);
			unShareBtn.setVisibility(View.VISIBLE);
			unShareBtn.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					datastore.removeSyncStatusListener(listener);
					mListener.onUnshareSelected(datastore);
					dismiss();
				}
			});
		}
		
		
		
		// Send a text message with a link.
		((Button) v.findViewById(R.id.shareButton)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Intent emailIntent = new Intent();
				emailIntent.setAction(Intent.ACTION_SEND);
				// Native email client doesn't currently support HTML, but it doesn't hurt to try in case they fix it
				emailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml("Here's the link: <a href=\"https://www.nearnotes.com/#" + datastore.getId() + "\">https://www.nearnotes.com/#" + datastore.getId() + "</a>"));
				emailIntent.putExtra(Intent.EXTRA_SUBJECT, "I want to share a note with you.");
				emailIntent.setType("message/rfc822");

				PackageManager pm = getActivity().getPackageManager();
				Intent sendIntent = new Intent(Intent.ACTION_SEND);
				sendIntent.setType("text/plain");

				Intent openInChooser = Intent.createChooser(emailIntent, "Share note using...");

				List<ResolveInfo> resInfo = pm.queryIntentActivities(sendIntent, 0);
				List<LabeledIntent> intentList = new ArrayList<LabeledIntent>();
				for (int i = 0; i < resInfo.size(); i++) {
					// Extract the label, append it, and repackage it in a LabeledIntent
					ResolveInfo ri = resInfo.get(i);
					String packageName = ri.activityInfo.packageName;
					if (packageName.contains("android.email")) {
						emailIntent.setPackage(packageName);
					} else if (packageName.contains("twitter") || packageName.contains("facebook") || packageName.contains("mms") || packageName.contains("android.gm") || packageName.contains("whatsapp") || packageName.contains("skype")) {
						Intent intent = new Intent();
						intent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
						intent.setAction(Intent.ACTION_SEND);
						intent.setType("text/plain");

						if (packageName.contains("android.gm")) {
							intent.putExtra(Intent.EXTRA_TEXT, "I want to share a NearNote: https://www.nearnotes.com/#" + datastore.getId());
							intent.putExtra(Intent.EXTRA_SUBJECT, "I want to share a note with you.");
							intent.setType("message/rfc822");
						} else {
							intent.putExtra(Intent.EXTRA_TEXT, "Here's the link: https://www.nearnotes.com/#" + datastore.getId());
						}

						intentList.add(new LabeledIntent(intent, packageName, ri.loadLabel(pm), ri.icon));
					}
				}

				// convert intentList to array
				LabeledIntent[] extraIntents = intentList.toArray(new LabeledIntent[intentList.size()]);

				openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);

				
				
				try {
					startActivity(openInChooser);
				} catch (ActivityNotFoundException e) {
					Toast.makeText(getActivity(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
				}

			}
		});
		

		((Button) v.findViewById(R.id.copyButton)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("NearNotes", "https://www.nearnotes.com/#" + datastore.getId());
				clipboard.setPrimaryClip(clip);
				Toast.makeText(getActivity(), "Copied link to clipboard.", Toast.LENGTH_SHORT).show();
			}
		});

		// Set up a listener to follow changes to the datastore (including permissions changes).
		listener = new DbxDatastore.SyncStatusListener() {
			@Override
			public void onDatastoreStatusChange(DbxDatastore dbxDatastore) {
				try {
					dbxDatastore.sync();
				} catch (DbxException e) {
					e.printStackTrace();
				}
			}
		};
		datastore.addSyncStatusListener(listener);

		return new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.CustomActionBarTheme))
				.setTitle(mIsManaged ? "Manage Shared Note" : "Share Note")
				.setView(v)
				.create();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		datastore.removeSyncStatusListener(listener);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		dismiss();
	}
}
