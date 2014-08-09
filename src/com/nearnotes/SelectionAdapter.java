package com.nearnotes;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

	public class SelectionAdapter extends SimpleCursorAdapter {

		public SelectionAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
			super(context, layout, c, from, to);
		}

		private SparseBooleanArray mSelection = new SparseBooleanArray();

		public void setNewSelection(int position, boolean value) {
			mSelection.put(position, value);
			notifyDataSetChanged();
		}

		public boolean isPositionChecked(int position) {
			Boolean result = mSelection.get(position);
			return result == null ? false : result;
		}

		public void removeSelection(int position) {
			mSelection.delete(position);
			notifyDataSetChanged();
		}

		public void clearSelection() {
			mSelection = new SparseBooleanArray();
			notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = super.getView(position, convertView, parent);//let the adapter handle setting up the row views
			v.setBackgroundColor(Color.parseColor("#00000000")); //default color

			if (mSelection.get(position)) {
				v.setBackgroundColor(Color.parseColor("#ff33b5e5"));// this is a selected position so make it red
			}
			return v;
		}
	}