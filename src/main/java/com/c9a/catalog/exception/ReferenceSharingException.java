package com.c9a.catalog.exception;

public class ReferenceSharingException extends Exception {
	
	private String message;
	
	public ReferenceSharingException(){}
	
	public ReferenceSharingException(String message){
		this.message = message;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 6261817190819038148L;

	@Override
	public String getMessage() {
		if(message != null){
			return message;
		}
		return super.getMessage();
	}
}