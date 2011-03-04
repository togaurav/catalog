package com.c9a.catalog.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.entities.Document;
import com.c9a.catalog.entities.Reference;
import com.c9a.catalog.exception.CollectionNotFoundException;


/**
 * 
 * @author Carter Youngblood
 *
 */
public interface ICatalogSearchService {
	
	
	public Set<CatalogCollection> findCollectionsWithAttributesKeyAndValue(String owner, String partitionId, String uniqueColelctionIdToSearch, boolean nestedSearch, Map<String, String> attributesToSearch, boolean matchAll) throws CollectionNotFoundException;
	public Set<CatalogCollection> findCollectionsWithAttributesKey(String owner, String partitionId, String uniqueColelctionIdToSearch, boolean nestedSearch, List<String> attribuKeys, boolean matchAll) throws CollectionNotFoundException;
	public Set<CatalogCollection> findCollectionsWithAttributesValue(String owner, String partitionId, String uniqueColelctionIdToSearch, boolean nestedSearch, List<String> attributValues, boolean matchAll) throws CollectionNotFoundException;
	
	public Set<Reference> findReferencesWithAttributesKeyAndValue(String owner, String partitionId, String uniqueColelctionIdToSearch, boolean nestedSearch, Map<String, String> attributesToSearch, boolean matchAll) throws CollectionNotFoundException;
	public Set<Reference> findReferencesWithAttributesKey(String owner, String partitionId, String uniqueColelctionIdToSearch, boolean nestedSearch, List<String> attributeKeys, boolean matchAll) throws CollectionNotFoundException;
	public Set<Reference> findReferencesWithAttributesValue(String owner, String partitionId, String uniqueColelctionIdToSearch, boolean nestedSearch, List<String> attributeValues, boolean matchAll) throws CollectionNotFoundException;
	
	public Set<Document> findDocumentsWithAttributesKeyAndValue(String owner, String partitionId, String uniqueColelctionIdToSearch, boolean nestedSearch, Map<String, String> attributesToSearch, boolean matchAll) throws CollectionNotFoundException;
	public Set<Document> findDocumentsWithAttributesKey(String owner, String partitionId, String uniqueColelctionIdToSearch, boolean nestedSearch, List<String> attributeKeys, boolean matchAll) throws CollectionNotFoundException;
	public Set<Document> findDocumentsWithAttributesValue(String owner, String partitionId, String uniqueColelctionIdToSearch, boolean nestedSearch, List<String> attributeValues, boolean matchAll) throws CollectionNotFoundException;
	
}