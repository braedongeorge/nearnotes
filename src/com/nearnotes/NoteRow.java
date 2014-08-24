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
