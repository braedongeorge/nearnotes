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

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class DelayAutoCompleteTextView extends AutoCompleteTextView {

	private ProgressBar mLoadingIndicator;
	private ImageView mIconLocation;

	public void setLoadingIndicator(ProgressBar view, ImageView icon) {
		mLoadingIndicator = view;
		mIconLocation = icon;
	}

	public DelayAutoCompleteTextView(Context context, AttributeSet attrs) {
		super(context, attrs);

	}

	public DelayAutoCompleteTextView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public DelayAutoCompleteTextView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			DelayAutoCompleteTextView.super.performFiltering(
					(CharSequence) msg.obj, msg.arg1);
		}
	};

	@Override
	protected void performFiltering(CharSequence text, int keyCode) {
		mLoadingIndicator.setVisibility(View.VISIBLE);
		mIconLocation.setVisibility(View.GONE);

		mHandler.removeMessages(0);
		mHandler.sendMessageDelayed(
				mHandler.obtainMessage(0, keyCode, 0, text), 750);
	}

	@Override
	public void onFilterComplete(int count) {
		// the AutoCompleteTextView has done its job and it's about to show
		// the drop down so close/hide the ProgreeBar
		mLoadingIndicator.setVisibility(View.GONE);
		mIconLocation.setVisibility(View.VISIBLE);
		super.onFilterComplete(count);
		
	}

}