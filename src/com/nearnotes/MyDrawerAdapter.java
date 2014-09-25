package com.nearnotes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MyDrawerAdapter extends ArrayAdapter<String> {
	private final Context context;
	private final String[] values;

	public MyDrawerAdapter(Context context, String[] values) {
		super(context, R.layout.drawer_list_item, values);
		this.context = context;
		this.values = values;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		if (convertView == null) 
			convertView = inflater.inflate(R.layout.drawer_list_item, parent, false);
		
		TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
		textView.setText(values[position]);
		String s = values[position];
		
		if (s.startsWith("All")) 
			textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_select_all, 0, 0, 0);
		else if (s.startsWith("Nearest")) 
			textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_location_found, 0, 0, 0);
		else if (s.startsWith("Sync") || s.startsWith("Unsync")) 
			textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_dropbox_logo, 0, 0, 0);
		else if (s.startsWith("Settings")) 
			textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_settings, 0, 0, 0);
	
		textView.setCompoundDrawablePadding(30);

		return convertView;
	}
	
}
