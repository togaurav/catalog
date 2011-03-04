package com.c9a.catalog.exception;

public class ReferenceModificationException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6261817190819038148L;

	public static final String NOT_ENOUGH_PERMISSIONS_TO_CLONE = "You can not clone this reference, the permissions set does not allow it.";

	public static final String NOT_ENOUGH_PERMISSIONS_TO_MODIFY = "This referecne can not be modified";
	
	private String message;
	
	public ReferenceModificationException(String message){
		this.message = message;
	}
	
	public ReferenceModificationException(){
		throw new IllegalArgumentException("Use the constructor that takes in a string for the message.");
	}

	@Override
	public String getMessage() {
		return message + " : " + super.getMessage();
	}
}