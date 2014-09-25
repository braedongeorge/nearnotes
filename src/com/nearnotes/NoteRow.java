/*
k
 */

package com.nearnotes;

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
	     if (!(o instanceof NoteRow)) return false;
	     NoteRow lhs = (NoteRow) o;  	 // Cast to the appropriate type. This will succeed because of the instanceof, and lets us access private fields.
	    
	     return mRowType == lhs.mRowType && mRowSize == lhs.mRowSize && mRow == lhs.mRow;	// Check each field. Primitive fields, reference fields, and nullable reference fields are all treated differently.
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
