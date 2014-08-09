package com.nearnotes;

public class Checkline {
	private String line;
	private Boolean check;
	
	public Checkline(String tempString) {
		line = tempString;
		check = false;
	}
	public String getLine() {
		return line;
	}
	
	public Boolean getChecked() {
		return check;
	}
	
	public void setChecked(boolean tempCheck) {
		check = tempCheck;
	}

}
