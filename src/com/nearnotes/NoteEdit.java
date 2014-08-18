/*
 * 	Copyright 2014 Braedon Reid
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 * 	You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
 
package com.nearnotes;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class NoteEdit extends Fragment implements OnItemClickListener {
	private static final String LOG_TAG = "NearNotes.com";

	private EditText mTitleText;
	private EditText mBodyText;
	private DelayAutoCompleteTextView autoCompView;
	public Long mRowId;
	private PlacesAutoCompleteAdapter acAdapter;

	private String location;
	private double latitude = 0;
	private double longitude = 0;
	private int tempPosition = 0;

	private double mLongitude;
	private double mLatitude;
	private boolean mChecklist = false;
	private String checkString;
	private CheckBox mCheckBox;
	private List<String> mLines = new ArrayList<String>();
	private ArrayList<Integer> myArrayList = new ArrayList<Integer>();
	private ArrayList<NoteRow> mRealRow = new ArrayList<NoteRow>();

	private NotesDbAdapter mDbHelper;
	private TableLayout mTblAddLayout;
	noteEditListener mCallback;

	private ArrayList<String> referenceList;

	public interface noteEditListener { // Container Activity must implement this interface
		public void setEditItems();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mCallback = (noteEditListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement noteEditListener");
		}
	}

	public void getCheckNumber(int which) {
		if (which == 0) {
			mBodyText.removeTextChangedListener(bodyTextWatcher);
			mBodyText.setText("");
			mTblAddLayout.removeAllViews();
			mBodyText.addTextChangedListener(bodyTextWatcher);
		}
		Log.e("fragnent which", String.valueOf(which));
	}

	@Override
	public void onStart() {
		super.onStart();
		mTitleText = (EditText) getActivity().findViewById(R.id.title_edit);

		mCallback.setEditItems();
		if (mRowId == null) {
			Bundle extras = getArguments();
			if (!extras.containsKey(NotesDbAdapter.KEY_ROWID)) {
				//mTitleText.setText("");
			}
			mRowId = extras.containsKey(NotesDbAdapter.KEY_ROWID) ? extras.getLong(NotesDbAdapter.KEY_ROWID) : null;
		}

		getActivity().setTitle(R.string.edit_note);

		Bundle bundle = getArguments();
		mLongitude = bundle.getDouble("longitude");
		mLatitude = bundle.getDouble("latitude");

		mTitleText = (EditText) getActivity().findViewById(R.id.title_edit);
		mBodyText = (EditText) getView().findViewById(R.id.body);

		mTblAddLayout = (TableLayout) getActivity().findViewById(R.id.checkbody);
		mTblAddLayout.setPadding(0, 0, 0, 0);

		acAdapter = new PlacesAutoCompleteAdapter(getActivity(), R.layout.list_item);

		mCheckBox = (CheckBox) getActivity().findViewById(R.id.checkbox_on_top);
		mCheckBox.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (((CheckBox) v).isChecked() && mRowId != null) {
					Log.e("db", String.valueOf(mRowId));
					mDbHelper.updateSetting(mRowId);

				}

				else if (mRowId != null) {
					mDbHelper.removeSetting();
				}
			}
		});

		autoCompView = (DelayAutoCompleteTextView) getView().findViewById(R.id.autoCompleteTextView1);
		autoCompView.setAdapter(acAdapter);
		autoCompView.setOnItemClickListener(this);
		autoCompView.setLoadingIndicator((ProgressBar) getView().findViewById(R.id.progressAPI),
				(ImageView) getView().findViewById(R.id.location_icon));

		mBodyText.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				//This listener is added to make sure the globalLayout has been displayed
				// before accessing getLayout().getLineEnd()
				ViewTreeObserver obs = mBodyText.getViewTreeObserver();
				obs.removeGlobalOnLayoutListener(this);

				Log.e("globallayout checklist", String.valueOf(mChecklist));
				if (!mChecklist) {
					return;
				}
				mBodyText.addTextChangedListener(bodyTextWatcher);

				// Run the code below just once on startup to populate the global listArray mRealRow
				String tempBoxes = mBodyText.getText().toString();
				if (mBodyText.getLayout() != null) {
					mRealRow = populateBoxes(tempBoxes);

					int row = 0;
					for (NoteRow line : mRealRow) {
						switch (line.getType()) {
						case 0:
							TableRow inflate = (TableRow) View.inflate(getActivity(), R.layout.table_row_invisible, null);
							mTblAddLayout.addView(inflate);
							break;
						case 1:
							TableRow checkRow = (TableRow) View.inflate(getActivity(), R.layout.table_row, null);
							CheckBox temp = (CheckBox) checkRow.getChildAt(0);
							temp.setTag(Integer.valueOf(row));
							mTblAddLayout.addView(checkRow);
							temp.setOnClickListener(checkBoxListener);
							break;
						case 2:
							int spanstart = 0;
							StrikethroughSpan STRIKE_THROUGH_SPAN = new StrikethroughSpan();
							Spannable spannable = (Spannable) mBodyText.getText();

							TableRow checkRow1 = (TableRow) View.inflate(getActivity(), R.layout.table_row, null);
							CheckBox temp1 = (CheckBox) checkRow1.getChildAt(0);

							temp1.setTag(Integer.valueOf(row));
							temp1.setChecked(true);
							for (int j = 0; j < row; j++) {
								spanstart += mLines.get(j).length() + 1;
							}
							// text.insert(spanstart, "[X] ");
							// mBodyText.setSelection(spanstart);
							// spannable.setSpan(STRIKE_THROUGH_SPAN, spanstart, spanstart + mLines.get(row).length() + 1, Spanned.SPAN_PARAGRAPH);
							mTblAddLayout.addView(checkRow1);
							temp1.setOnClickListener(checkBoxListener);
							break;
						}

						for (int k = 1; line.getSize() > k; k++) {
							TableRow inflate = (TableRow) View.inflate(getActivity(), R.layout.table_row_invisible, null);
							mTblAddLayout.addView(inflate);
						}
						row++;
					}
				}
			}
		});
	}

	public void toggleChecklist() {
		if (mChecklist) {
			Toast.makeText(getActivity(), "Checklist off", Toast.LENGTH_SHORT).show();
			mChecklist = false;
			mTblAddLayout.removeAllViews();
			mBodyText.removeTextChangedListener(bodyTextWatcher);
			Spannable spannable = (Spannable) mBodyText.getText();
			Object spansToRemove[] = spannable.getSpans(0, spannable.length(), Object.class);
			for (Object span : spansToRemove) {
				if (span instanceof CharacterStyle)
					spannable.removeSpan(span);
			}
		} else {
			Toast.makeText(getActivity(), "Checklist on", Toast.LENGTH_SHORT).show();
			mChecklist = true;
			mBodyText.addTextChangedListener(bodyTextWatcher);

			String tempBoxes = mBodyText.getText().toString();
			if (mBodyText.getLayout() != null) {
				mRealRow = populateBoxes(tempBoxes);

				int row = 0;
				for (NoteRow line : mRealRow) {
					switch (line.getType()) {
					case 0:
						TableRow inflate = (TableRow) View.inflate(getActivity(), R.layout.table_row_invisible, null);
						mTblAddLayout.addView(inflate);
						break;
					case 1:
						TableRow checkRow = (TableRow) View.inflate(getActivity(), R.layout.table_row, null);
						CheckBox temp = (CheckBox) checkRow.getChildAt(0);
						temp.setTag(Integer.valueOf(row));
						mTblAddLayout.addView(checkRow);
						temp.setOnClickListener(checkBoxListener);
						break;
					case 2:
						int spanstart = 0;
						StrikethroughSpan STRIKE_THROUGH_SPAN = new StrikethroughSpan();
						Spannable spannable = (Spannable) mBodyText.getText();

						TableRow checkRow1 = (TableRow) View.inflate(getActivity(), R.layout.table_row, null);
						CheckBox temp1 = (CheckBox) checkRow1.getChildAt(0);

						temp1.setTag(Integer.valueOf(row));
						temp1.setChecked(true);
						for (int j = 0; j < row; j++) {
							spanstart += mLines.get(j).length() + 1;
						}
						// text.insert(spanstart, "[X] ");
						// mBodyText.setSelection(spanstart);
						// spannable.setSpan(STRIKE_THROUGH_SPAN, spanstart, spanstart + mLines.get(row).length() + 1, Spanned.SPAN_PARAGRAPH);
						mTblAddLayout.addView(checkRow1);
						temp1.setOnClickListener(checkBoxListener);
						break;
					}

					for (int k = 1; line.getSize() > k; k++) {
						TableRow inflate = (TableRow) View.inflate(getActivity(), R.layout.table_row_invisible, null);
						mTblAddLayout.addView(inflate);
					}
					row++;
				}
			}

		}

	}

	/**
	 * Populates the checkboxes on the side by analyzing the current text from
	 * the body of the note.
	 * 
	 * @param currentString
	 *            the current body of the note.
	 * 
	 */
	public ArrayList<NoteRow> populateBoxes(String currentString) {

		// Load ArrayList<String> mLines with the current bodytext seperated into seperate lines.
		mLines = Arrays.asList(currentString.split(System.getProperty("line.separator")));

		// row counter to determine what the current line number is for the for loop
		int row = 0;

		// realRow counter to determine what line of text in the actual display we are on
		// used to get the number of characters on each line
		int realRow = 0;
		int activeRow = 0;
		int finishedCount = 0;

		ArrayList<NoteRow> tempRealRow = new ArrayList<NoteRow>();
		for (String line : mLines) {
			NoteRow temp = new NoteRow(0, 1, row); // Create a note row object with rowType of 0 (invisible), lineSize of 1 and the current row number

			if (!line.isEmpty()) {
				activeRow++;
				temp.setType(1); // Set the NoteRow object to 1 (visible)

				// Determine how many lines the note takes up
				int internalCounter = 0;
				try {
					float lineLength = (float) line.length();
					for (int k = 0; (lineLength / (getFloatLineEnd(realRow + k) - getFloatLineEnd(realRow - 1))) > 1; k++) {
						internalCounter++;
					}
				} catch (NullPointerException e) {
					e.printStackTrace();
				}

				// Detemine if the note is supposed to be checked and set the NoteRow object to 2 (Checked)
				if (line.startsWith("[X]")) {
					finishedCount++;
					int spanstart = 0;
					StrikethroughSpan STRIKE_THROUGH_SPAN = new StrikethroughSpan();
					Spannable spannable = (Spannable) mBodyText.getText();
					// TableRow checkRow1 = (TableRow) View.inflate(getActivity(), R.layout.table_row, null);

					for (int j = 0; j < row; j++) {
						spanstart += mLines.get(j).length() + 1;
					}

					Object spansToRemove[] = spannable.getSpans(spanstart, spanstart + mLines.get(row).length(), Object.class);
					for (Object span : spansToRemove) {
						if (span instanceof CharacterStyle)
							spannable.removeSpan(span);
					}

					spannable.setSpan(STRIKE_THROUGH_SPAN, spanstart, spanstart + mLines.get(row).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					temp.setType(2);
				}

				temp.setSize(1 + internalCounter); // Set the amount of rows the note takes up
				realRow = realRow + internalCounter; // Determine the real line on the display text we are on
			}

			tempRealRow.add(temp); // NoteRow object has been finalized - add to the ListArray<NoteRow>
			realRow++; // Increase the noteRow and the displayRow for the next line
			row++;
		}
		Log.e("finishedCount", String.valueOf(finishedCount));
		Log.e("row", String.valueOf(row));
		if (finishedCount == activeRow && finishedCount != 0) {

			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
			boolean useListPref = sharedPref.getBoolean("pref_key_use_checklist_default", false);
			String stringlistPref = sharedPref.getString("pref_key_checklist_listPref", "2");
			int listPref = Integer.parseInt(stringlistPref);
			Log.e("useListPref", String.valueOf(useListPref));

			ChecklistDialog newFragment = new ChecklistDialog();
			if (mRowId == null) {
				saveState();
			}
			Bundle args = new Bundle();
			args.putLong("_id", mRowId);
			args.putBoolean("useDefault", useListPref);
			args.putInt("listPref", listPref);
			newFragment.setArguments(args);
			if (listPref == 2 && useListPref) {
				return tempRealRow;
			}
			if (getFragmentManager().findFragmentByTag("MyDialog") == null) {
				newFragment.show(getFragmentManager(), "MyDialog");
			}

			Log.e("done", "done");
		}

		return tempRealRow;
	}

	public float getFloatLineEnd(int line) {
		float x = 0;
		if (line >= 0)
			x = (float) mBodyText.getLayout().getLineEnd(line);
		return x;
	}

	public void itemClicked(View v) {
		//code to check if this checkbox is checked!
		CheckBox checkBox = (CheckBox) v;
		if (checkBox.isChecked()) {

		}
	}

	private TextWatcher bodyTextWatcher = new TextWatcher() {
		@Override
		public void afterTextChanged(Editable s) {
			String tempString = s.toString();

			if (mBodyText.getLayout() != null) {
				// Convert the old NoteRow listArray and the one just generated into collections and
				// remove all objects in each collection to determine if they are different if
				// either Collection is not empty
				ArrayList<NoteRow> temporaryNote = populateBoxes(tempString);
				Collection<NoteRow> listOne = temporaryNote;
				Collection<NoteRow> listTwo = mRealRow;

				List<NoteRow> sourceList = new ArrayList<NoteRow>(listOne);
				List<NoteRow> destinationList = new ArrayList<NoteRow>(listTwo);

				destinationList.removeAll(listOne);
				sourceList.removeAll(listTwo);

				if (!sourceList.isEmpty() || !destinationList.isEmpty()) {
					// Should only be triggered if the two listArrays are different
					// ie. if a note is longer than the one line, or if a line is added or removed.
					mTblAddLayout.removeAllViews();
					mRealRow = temporaryNote;

					// Use the ListArray<NoteRow> to populate the checkboxes, either check, visible or invisible.
					// Invisible checkboxes are added if the current note is longer than 1 line.
					int row = 0;
					for (NoteRow line : mRealRow) {
						switch (line.getType()) {
						case 0:
							TableRow inflate = (TableRow) View.inflate(getActivity(), R.layout.table_row_invisible, null);
							mTblAddLayout.addView(inflate);
							break;
						case 1:
							TableRow checkRow = (TableRow) View.inflate(getActivity(), R.layout.table_row, null);
							CheckBox temp = (CheckBox) checkRow.getChildAt(0);
							temp.setTag(Integer.valueOf(row));
							mTblAddLayout.addView(checkRow);
							temp.setOnClickListener(checkBoxListener);
							break;
						case 2:
							TableRow checkRow1 = (TableRow) View.inflate(getActivity(), R.layout.table_row, null);
							CheckBox temp1 = (CheckBox) checkRow1.getChildAt(0);
							temp1.setTag(Integer.valueOf(row));
							temp1.setChecked(true);
							mTblAddLayout.addView(checkRow1);
							temp1.setOnClickListener(checkBoxListener);
							break;
						}

						for (int k = 1; line.getSize() > k; k++) {
							TableRow inflate = (TableRow) View.inflate(getActivity(), R.layout.table_row_invisible, null);
							mTblAddLayout.addView(inflate);
						}
						row++;
					}
				}
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {

		}
	};

	private OnClickListener checkBoxListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			String tempBoxes = mBodyText.getText().toString();
			mLines = Arrays.asList(tempBoxes.split(System.getProperty("line.separator")));

			int i = (Integer) v.getTag();
			int spanstart = 0;
			StrikethroughSpan STRIKE_THROUGH_SPAN = new StrikethroughSpan();
			Spannable spannable = (Spannable) mBodyText.getText();

			Editable text = mBodyText.getText();
			if (((CheckBox) v).isChecked()) {

				for (int j = 0; j < i; j++) {
					spanstart += mLines.get(j).length() + 1;
				}
				text.insert(spanstart, "[X] ");
				spannable.setSpan(STRIKE_THROUGH_SPAN, spanstart, spanstart + mLines.get(i).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else {

				spanstart = 0;
				for (int j = 0; j < i; j++) {
					spanstart += mLines.get(j).length() + 1;
				}

				if (text.subSequence(spanstart, spanstart + 4).toString().contains("[X] ")) {
					Log.e("finding", "the x");
					text.delete(spanstart, spanstart + 4);
					Object spansToRemove[] = spannable.getSpans(spanstart, spanstart + mLines.get(i).length(), Object.class);
					for (Object span : spansToRemove) {
						if (span instanceof CharacterStyle)
							spannable.removeSpan(span);
					}
				}
			}
		}
	};

	public static int countOccurrences(String haystack, char needle) {
		int count = 0;
		for (int i = 0; i < haystack.length(); i++) {
			if (haystack.charAt(i) == needle) {
				count++;
			}
		}
		return count;
	}

	private class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
		private ArrayList<String> resultList;

		public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
		}

		@Override
		public int getCount() {
			return resultList.size();
		}

		@Override
		public String getItem(int index) {
			return resultList.get(index);
		}

		@Override
		public Filter getFilter() {

			Filter filter = new Filter() {
				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					FilterResults filterResults = new FilterResults();

					if (constraint != null) {
						// Retrieve the autocomplete results.
						resultList = autocomplete(constraint.toString());

						// Assign the data to the FilterResults
						if (resultList != null) {
							filterResults.values = resultList;
							filterResults.count = resultList.size();
						}

					}
					return filterResults;
				}

				@Override
				protected void publishResults(CharSequence constraint, FilterResults results) {
					if (results != null && results.count > 0) {
						notifyDataSetChanged();
					} else {
						notifyDataSetInvalidated();
					}
				}
			};
			return filter;
		}
	}

	private class NetworkTask extends AsyncTask<String, Void, JSONObject> {
		@Override
		protected void onPreExecute() {
			ProgressBar tempBar = (ProgressBar) getView().findViewById(R.id.progressAPI);
			ImageView tempImageView = (ImageView) getView().findViewById(R.id.location_icon);
			tempBar.setVisibility(View.VISIBLE);
			tempImageView.setVisibility(View.GONE);

		}

		@Override
		protected JSONObject doInBackground(String... params) {
			String link = params[0];
			HttpGet request = new HttpGet(link);
			AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
			try {
				HttpResponse result = client.execute(request);
				if (result != null) {
					StringBuilder jsonResults = new StringBuilder();

					try {
						InputStreamReader in = new InputStreamReader(result.getEntity().getContent());
						int read;
						char[] buff = new char[1024];
						while ((read = in.read(buff)) != -1) {
							jsonResults.append(buff, 0, read);
						}

						JSONObject jsonObj = new JSONObject(jsonResults.toString());
						return jsonObj;

					} catch (JSONException e) {
						Log.e(LOG_TAG, "Cannot process JSON results", e);
						return null;
					} catch (IOException e) {
						Log.e(LOG_TAG, "Error connecting to Places API", e);
						return null;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			} finally {
				client.close();
			}
			return null;

		}

		@Override
		protected void onPostExecute(JSONObject result) {
			try {
				result = result.getJSONObject("result");
				result = result.getJSONObject("geometry");
				result = result.getJSONObject("location");

				Log.e(LOG_TAG, result.toString());
				latitude = result.getDouble("lat");
				longitude = result.getDouble("lng");
			} catch (JSONException e) {
				Log.e(LOG_TAG, "Cannot process JSON results", e);
			}

			ProgressBar tempBar = (ProgressBar) getView().findViewById(R.id.progressAPI);
			ImageView tempImageView = (ImageView) getView().findViewById(R.id.location_icon);
			tempBar.setVisibility(View.GONE);
			tempImageView.setVisibility(View.VISIBLE);

		}
	}

	private ArrayList<String> autocomplete(String input) {
		ArrayList<String> resultList = null;

		HttpURLConnection conn = null;
		StringBuilder jsonResults = new StringBuilder();
		try {
			StringBuilder sb = new StringBuilder("http://www.nearnotes.com/index.php");
			sb.append("?longitude=" + String.valueOf(mLongitude));
			sb.append("&latitude=" + String.valueOf(mLatitude));
			sb.append("&input=" + URLEncoder.encode(input, "utf8"));

			URL url = new URL(sb.toString());
			conn = (HttpURLConnection) url.openConnection();
			InputStreamReader in = new InputStreamReader(conn.getInputStream());

			// Load the results into a StringBuilder
			int read;
			char[] buff = new char[1024];
			while ((read = in.read(buff)) != -1) {
				jsonResults.append(buff, 0, read);
			}
		} catch (MalformedURLException e) {
			Log.e(LOG_TAG, "Error processing Places API URL", e);
			return resultList;
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error connecting to Places API", e);
			return resultList;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		try {
			// Create a JSON object hierarchy from the results
			// Log.e("JSON Result",jsonResults.status.toString());
			JSONObject jsonObj = new JSONObject(jsonResults.toString());
			JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");
			if (predsJsonArray.length() == 0) {
				Toast.makeText(getActivity(), "No locations found", Toast.LENGTH_SHORT).show();
			}
			// Log.e("isEmpty",String.valueOf(predsJsonArray.length()));

			// Extract the Place descriptions from the results
			resultList = new ArrayList<String>(predsJsonArray.length());
			referenceList = new ArrayList<String>(predsJsonArray.length());
			for (int i = 0; i < predsJsonArray.length(); i++) {
				resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
				referenceList.add(predsJsonArray.getJSONObject(i).getString("reference"));
			}
		} catch (JSONException e) {
			Log.e(LOG_TAG, "Cannot process JSON results", e);
		}
		return resultList;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// access options menu from fragment
		setHasOptionsMenu(true);
		mDbHelper = new NotesDbAdapter(getActivity());
		mDbHelper.open();
		mRowId = (savedInstanceState == null) ? null : (Long) savedInstanceState.getSerializable(NotesDbAdapter.KEY_ROWID);

		return inflater.inflate(R.layout.note_edit, container, false);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.findItem(R.id.action_done).setVisible(true);
		menu.findItem(R.id.action_new).setVisible(false);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@SuppressWarnings("deprecation")
	private void populateFields() {

		if (mRowId != null) {
			int settingsResult = mDbHelper.fetchSetting();
			if (settingsResult == mRowId) {
				mCheckBox.setChecked(true);
			} else
				mCheckBox.setChecked(false);
			Cursor note = mDbHelper.fetchNote(mRowId);
			getActivity().startManagingCursor(note);
			mTitleText.setText(note.getString(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_TITLE)));
			mBodyText.setText(note.getString(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_BODY)), TextView.BufferType.SPANNABLE);
			autoCompView.setAdapter(null);
			autoCompView.setText(note.getString(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_LOCATION)));
			autoCompView.setAdapter(new PlacesAutoCompleteAdapter(getActivity(), R.layout.list_item));
			location = note.getString(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_LOCATION));
			longitude = note.getDouble(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_LNG));
			latitude = note.getDouble(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_LAT));
			checkString = note.getString(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_CHECK));
			mChecklist = Boolean.parseBoolean(checkString);
			Log.e("populate fields", String.valueOf(checkString));
		} else {
			autoCompView.requestFocus();
			InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(autoCompView, InputMethodManager.SHOW_IMPLICIT);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (this.getView() == null) {
			return;
		} else {
			// saveState();
			outState.putSerializable(NotesDbAdapter.KEY_ROWID, mRowId);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.isHidden()) {
			return;
		} else {
			saveState();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		mCallback.setEditItems();
		if (mRowId == null) {
			Bundle extras = getArguments();
			if (!extras.containsKey(NotesDbAdapter.KEY_ROWID)) {
				mTitleText.setText("");
			}
			mRowId = extras.containsKey(NotesDbAdapter.KEY_ROWID) ? extras.getLong(NotesDbAdapter.KEY_ROWID) : null;
		}
		populateFields();
	}

	public void saveState() {
		String title = mTitleText.getText().toString();
		String body = mBodyText.getText().toString();
		if (title.isEmpty()) {
			title = body.substring(0, Math.min(body.length(), 7));
		}
		Log.e("saveState", String.valueOf(mChecklist));
		String listString = String.valueOf(mChecklist);
		/*
		 * for (int i = 0; i < myArrayList.size(); i++) {
		 * 
		 * if (i == (myArrayList.size() - 1)) { listString +=
		 * String.valueOf(myArrayList.get(i)); } else listString +=
		 * String.valueOf(myArrayList.get(i)) + "-";
		 * 
		 * }
		 */
		Log.w("Checkbox string", listString);

		if (mRowId == null) {
			long id = mDbHelper.createNote(title, body, latitude, longitude, location, listString);
			if (id > 0) {
				mRowId = id;
				if (mCheckBox.isChecked()) {
					Log.e("db", String.valueOf(mRowId));
					mDbHelper.updateSetting(mRowId);
				}
				Toast.makeText(getActivity(), "Note Created", Toast.LENGTH_SHORT).show();
			}
		} else {
			mDbHelper.updateNote(mRowId, title, body, latitude, longitude, location, listString);
		}
	}

	public void onCheckboxClicked(View view) {
		boolean checked = ((CheckBox) view).isChecked(); // Is the view now checked?

		// Check which checkbox was clicked
		switch (view.getId()) {
		case R.id.checkbox_on_top:
			if (checked)
				mDbHelper.updateSetting(mRowId);
			else
				mDbHelper.removeSetting();
			break;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		mBodyText.requestFocus();
		autoCompView.setSelection(0);
		tempPosition = position;
		Log.e(LOG_TAG, String.valueOf(tempPosition));
		location = (String) adapterView.getItemAtPosition(position);
		Log.e(LOG_TAG, location);
		Geocoder find = new Geocoder(getActivity());
		try {
			List<Address> AddressList = find.getFromLocationName(location, 10);
			if (AddressList.size() > 0) {
				longitude = AddressList.get(0).getLongitude();
				latitude = AddressList.get(0).getLatitude();
				Log.e(LOG_TAG, String.valueOf(longitude));
				//if 1st provider does not have data, loop through other providers to find it.
				int count = 0;
				while (longitude == 0 && count < AddressList.size()) {
					longitude = AddressList.get(count).getLongitude();
					latitude = AddressList.get(count).getLatitude();
					count++;
				}
			} else {
				StringBuilder sb = new StringBuilder("http://www.nearnotes.com/geocode.php");
				sb.append("?reference=" + String.valueOf(referenceList.get(tempPosition)));
				Log.e(LOG_TAG, sb.toString());

				new NetworkTask().execute(sb.toString());
			}
		} catch (IOException e) {
			Log.e(LOG_TAG, "Couldnt received coordinates");
			e.printStackTrace();
		}
	}
}
