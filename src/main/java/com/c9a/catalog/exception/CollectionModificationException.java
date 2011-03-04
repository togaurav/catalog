package com.c9a.catalog.exception;

public class CollectionModificationException extends Exception {
	
	private String message;

	/**
	 * 
	 */
	private static final long serialVersionUID = -1068235588437412156L;

	public static final String NOT_COLLECTION_OWNER = "You can not modify the collection if you are not the owner.";
	public static final String NOT_ENOUGH_PERMISSIONS_TO_MODIFY = "You can not modify the collection, since you do not have enough permissions.";
	public static final String NOT_ENOUGH_PERMISSIONS_TO_CLONE = "You can not clone the collection, since you do not have enough permissions.";
	public static final String CAN_NOT_MOVE_SHARED_COLLECION_INTO_SHARED_COLLECTION = "Can not move a shared collection into another shared collection.  Please unshare the collection then move it into the shared collection.";

	public static final String NOT_ENOUGH_PERMISSIONS_TO_ADD = "Not enough permissions to add.";
	
	public CollectionModificationException(String message){
		this.message = message;
	}
	
	public CollectionModificationException(){
		throw new IllegalArgumentException("Use the constructor that take in a string.");
	}

	@Override
	public String getMessage() {
		return message + " : " + super.getMessage();
	}
}