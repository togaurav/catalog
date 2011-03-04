package com.c9a.catalog.exception;

import java.util.HashMap;
import java.util.Map;

import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.entities.Reference;

public class CollectionSharingException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1068235588437412156L;
	
	public CollectionSharingException(){}
	
	public CollectionSharingException(Map<CatalogCollection, String> collectionsThatCanNotBeShared, Map<Reference, String> referencesThatCanNotBeShared) {
		this.collectionsThatCanNotBeShared = collectionsThatCanNotBeShared;
		this.referencesThatCanNotBeShared = referencesThatCanNotBeShared;
	}
	
	private Map<CatalogCollection, String> collectionsThatCanNotBeShared = new HashMap<CatalogCollection, String>();
	private Map<Reference, String> referencesThatCanNotBeShared = new HashMap<Reference, String>();

	public Map<CatalogCollection, String> getCollectionsThatCanNotBeShared() {
		return collectionsThatCanNotBeShared;
	}

	public void setCollectionsThatCanNotBeShared(
			Map<CatalogCollection, String> collectionsThatCanNotBeShared) {
		this.collectionsThatCanNotBeShared = collectionsThatCanNotBeShared;
	}

	public Map<Reference, String> getReferencesThatCanNotBeShared() {
		return referencesThatCanNotBeShared;
	}

	public void setReferencesThatCanNotBeShared(
			Map<Reference, String> referencesThatCanNotBeShared) {
		this.referencesThatCanNotBeShared = referencesThatCanNotBeShared;
	}

}