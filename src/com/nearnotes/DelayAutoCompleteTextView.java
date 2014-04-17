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
        
        public DelayAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            // TODO Auto-generated constructor stub
        }
        
        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
            	
                DelayAutoCompleteTextView.super.performFiltering((CharSequence) msg.obj, msg.arg1);
            }
        };
        
        

        @Override
        protected void performFiltering(CharSequence text, int keyCode) {
        	mLoadingIndicator.setVisibility(View.VISIBLE);
        	mIconLocation.setVisibility(View.GONE);
        	
            mHandler.removeMessages(0);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(0, keyCode, 0, text), 1250);
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