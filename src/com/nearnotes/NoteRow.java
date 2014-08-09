package com.nearnotes;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

public class NoteRow {
	private int mRow;
	private int mRowType;
	private int mRowSize;

	@Override
	public int hashCode() {
		int result = 17;

		result = 31 * result + mRowType;
		result = 31 * result + mRowSize;
		result = 31 * result + mRow;

		return result;

	}
	
	
	@Override
	public boolean equals(Object o) {
		 // Return true if the objects are identical.
	     // (This is just an optimization, not required for correctness.)
	     //if (this == o) {
	     //  return true;
	     //}

	     // Return false if the other object has the wrong type.
	     // This type may be an interface depending on the interface's specification.
	     if (!(o instanceof NoteRow)) {
	    	// Log.e("returning false", "msg");
	       return false;
	      
	     }

	     // Cast to the appropriate type.
	     // This will succeed because of the instanceof, and lets us access private fields.
	     NoteRow lhs = (NoteRow) o;

	     // Check each field. Primitive fields, reference fields, and nullable reference
	     // fields are all treated differently.
	     //Log.e("info",String.valueOf(rowNo)+String.valueOf(rowSize));
	     //Log.e("boolean",String.valueOf(rowNo == lhs.rowNo && rowSize ==  lhs.rowSize));
	     return mRowType == lhs.mRowType && mRowSize == lhs.mRowSize && mRow == lhs.mRow;
	}

	public NoteRow(int type, int size, int row) {
		mRowType = type;
		mRowSize = size;
		mRow = row;

	}

	public int getType() {
		return mRowType;
	}

	public int getSize() {
		return mRowSize;
	}

	public void setType(int type) {
		mRowType = type;
	}

	public void setSize(int size) {
		mRowSize = size;
	}

}
