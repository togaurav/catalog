package com.c9a.catalog.exception;

public class InvalidCatalogPathException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2930466915770590131L;
	
	protected String path;
	protected String message;
	
	public InvalidCatalogPathException(String path, String message){
		this.path = path;
		this.message = message;
	}
	
	public InvalidCatalogPathException(String path){
		this.path = path;
	}
	
	public InvalidCatalogPathException(){}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
