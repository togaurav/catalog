package com.c9a.catalog.exception;

import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.entities.Document;
import com.c9a.catalog.entities.Reference;

public class ObjectLockedException extends Exception {
	
	public ObjectLockedException(){}
	
	public ObjectLockedException(CatalogCollection lockedCollection){
		this.lockedCollection = lockedCollection;
	}
	
	public ObjectLockedException(Reference lockedReference){
		this.lockedReference = lockedReference;
	}
	
	public ObjectLockedException(Document lockedDocument){
		this.lockedDocument = lockedDocument;
	}

	private static final long serialVersionUID = -8726580763334601795L;
	
	private CatalogCollection lockedCollection;
	private Reference lockedReference;
	private Document lockedDocument;
	
	public CatalogCollection getLockedCollection() {
		return lockedCollection;
	}
	public void setLockedCollection(CatalogCollection lockedCollection) {
		this.lockedCollection = lockedCollection;
	}
	public Reference getLockedReference() {
		return lockedReference;
	}
	public void setLockedReference(Reference lockedReference) {
		this.lockedReference = lockedReference;
	}
	public Document getLockedDocument() {
		return lockedDocument;
	}
	public void setLockedDocument(Document lockedDocument) {
		this.lockedDocument = lockedDocument;
	}

	@Override
	public String getMessage() {
		if(lockedCollection!= null){
			return "The following collection is locked.  Unique collection id : " + lockedCollection.getUniqueId();
		}
		if(lockedReference!= null){
			return "The following reference is locked.  Unique reference id : " + lockedReference.getUniqueId();
		}
		if(lockedDocument!= null){
			return "The following document is locked.  Unique document id : " + lockedDocument.getUniqueId();
		}
		return super.getMessage();
	}
}