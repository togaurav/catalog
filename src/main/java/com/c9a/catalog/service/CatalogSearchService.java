package com.c9a.catalog.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.c9a.catalog.dao.CatalogDao;
import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.entities.Document;
import com.c9a.catalog.entities.Reference;
import com.c9a.catalog.exception.CollectionNotFoundException;

@Service("catalogSearchService")
public class CatalogSearchService implements ICatalogSearchService {
	
	@Autowired 
	private CatalogDao catalogDao;
	
	@Autowired
	private ICatalogService catalogService;
	
	@Override
	public Set<CatalogCollection> findCollectionsWithAttributesKeyAndValue(
			String owner, String partitionId,
			String uniqueColelctionIdToSearch, boolean nestedSearch,
			Map<String, String> attributesToSearch, boolean matchAll)
			throws CollectionNotFoundException {
		CatalogCollection cc = catalogService.getCollection(owner, partitionId, uniqueColelctionIdToSearch);
		Set<Long> ids = new HashSet<Long>();
		populateCollectionIdsForSearch(ids, cc, nestedSearch);
		
		Set<CatalogCollection> matchingCollections = new HashSet<CatalogCollection>();
		matchingCollections.addAll(catalogDao.findObjectsThatMatchAttributeKeysAndValues(CatalogCollection.class, attributesToSearch, matchAll, ids));
		
		return matchingCollections;
	}

	@Override
	public Set<CatalogCollection> findCollectionsWithAttributesKey(
			String owner, String partitionId,
			String uniqueColelctionIdToSearch, boolean nestedSearch,
			List<String> attributeKeys, boolean matchAll)
			throws CollectionNotFoundException {
		
		CatalogCollection cc = catalogService.getCollection(owner, partitionId, uniqueColelctionIdToSearch);
		Set<Long> ids = new HashSet<Long>();
		populateCollectionIdsForSearch(ids, cc, nestedSearch);
		
		Set<CatalogCollection> matchingCollections = new HashSet<CatalogCollection>();
		matchingCollections.addAll(catalogDao.findObjectsThatMatchAttributeKeys(CatalogCollection.class, attributeKeys, matchAll, ids));
		
		return matchingCollections;
	}

	@Override
	public Set<CatalogCollection> findCollectionsWithAttributesValue(
			String owner, String partitionId,
			String uniqueColelctionIdToSearch, boolean nestedSearch,
			List<String> attributValues, boolean matchAll)
			throws CollectionNotFoundException {
		
		CatalogCollection cc = catalogService.getCollection(owner, partitionId, uniqueColelctionIdToSearch);
		Set<Long> ids = new HashSet<Long>();
		populateCollectionIdsForSearch(ids, cc, nestedSearch);
		
		Set<CatalogCollection> matchingCollections = new HashSet<CatalogCollection>();
		matchingCollections.addAll(catalogDao.findObjectsThatMatchAttributeValues(CatalogCollection.class, attributValues, matchAll, ids));
		
		return matchingCollections;
	}

	@Override
	public Set<Reference> findReferencesWithAttributesKeyAndValue(
			String owner, String partitionId,
			String uniqueColelctionIdToSearch, boolean nestedSearch,
			Map<String, String> attributesToSearch, boolean matchAll)
			throws CollectionNotFoundException {
		
		CatalogCollection cc = catalogService.getCollection(owner, partitionId, uniqueColelctionIdToSearch);
		Set<Long> ids = new HashSet<Long>();
		populateReferenceIdsForSearch(ids, cc, nestedSearch);
		
		Set<Reference> matchingReferences = new HashSet<Reference>();
		matchingReferences.addAll(catalogDao.findObjectsThatMatchAttributeKeysAndValues(Reference.class, attributesToSearch, matchAll, ids));
		
		return matchingReferences;
	}

	@Override
	public Set<Reference> findReferencesWithAttributesKey(String owner,
			String partitionId, String uniqueColelctionIdToSearch,
			boolean nestedSearch, List<String> attributeKeys, boolean matchAll)
			throws CollectionNotFoundException {
		
		CatalogCollection cc = catalogService.getCollection(owner, partitionId, uniqueColelctionIdToSearch);
		Set<Long> ids = new HashSet<Long>();
		populateReferenceIdsForSearch(ids, cc, nestedSearch);
		
		Set<Reference> matchingReferences = new HashSet<Reference>();
		matchingReferences.addAll(catalogDao.findObjectsThatMatchAttributeKeys(Reference.class, attributeKeys, matchAll, ids));
		
		return matchingReferences;
	}

	@Override
	public Set<Reference> findReferencesWithAttributesValue(String owner,
			String partitionId, String uniqueColelctionIdToSearch,
			boolean nestedSearch, List<String> attributeValues, boolean matchAll)
			throws CollectionNotFoundException {
		
		CatalogCollection cc = catalogService.getCollection(owner, partitionId, uniqueColelctionIdToSearch);
		Set<Long> ids = new HashSet<Long>();
		populateReferenceIdsForSearch(ids, cc, nestedSearch);
		
		Set<Reference> matchingReferences = new HashSet<Reference>();
		matchingReferences.addAll(catalogDao.findObjectsThatMatchAttributeValues(Reference.class, attributeValues, matchAll, ids));
		
		return matchingReferences;
	}

	@Override
	public Set<Document> findDocumentsWithAttributesKeyAndValue(String owner,
			String partitionId, String uniqueColelctionIdToSearch,
			boolean nestedSearch, Map<String, String> attributesToSearch,
			boolean matchAll) throws CollectionNotFoundException {
		
		CatalogCollection cc = catalogService.getCollection(owner, partitionId, uniqueColelctionIdToSearch);
		Set<Long> ids = new HashSet<Long>();
		populateDocumentIdsForSearch(ids, cc, nestedSearch);
		
		Set<Document> matchingDocs = new HashSet<Document>();
		matchingDocs.addAll(catalogDao.findObjectsThatMatchAttributeKeysAndValues(Document.class, attributesToSearch, matchAll, ids));
		
		return matchingDocs;
	}

	@Override
	public Set<Document> findDocumentsWithAttributesKey(String owner,
			String partitionId, String uniqueColelctionIdToSearch,
			boolean nestedSearch, List<String> attributeKeys, boolean matchAll)
			throws CollectionNotFoundException {
		
		CatalogCollection cc = catalogService.getCollection(owner, partitionId, uniqueColelctionIdToSearch);
		Set<Long> ids = new HashSet<Long>();
		
		populateDocumentIdsForSearch(ids, cc, nestedSearch);
		Set<Document> matchingDocs = new HashSet<Document>();
		if(ids.size() == 0){
			return matchingDocs;
		}
		matchingDocs.addAll(catalogDao.findObjectsThatMatchAttributeKeys(Document.class, attributeKeys, matchAll, ids));
		return matchingDocs;
	}
	
	@Override
	public Set<Document> findDocumentsWithAttributesValue(String owner,
			String partitionId, String uniqueColelctionIdToSearch,
			boolean nestedSearch, List<String> attributeValues, boolean matchAll)
			throws CollectionNotFoundException {
		
		CatalogCollection cc = catalogService.getCollection(owner, partitionId, uniqueColelctionIdToSearch);
		Set<Long> ids = new HashSet<Long>();
		
		populateDocumentIdsForSearch(ids, cc, nestedSearch);
		
		Set<Document> matchingDocs = new HashSet<Document>();
		matchingDocs.addAll(catalogDao.findObjectsThatMatchAttributeValues(Document.class, attributeValues, matchAll, ids));
		
		return matchingDocs;
	}

	private void populateDocumentIdsForSearch(Set<Long> ids,
			CatalogCollection cc, boolean nestedSearch) {
		for(Reference ref : cc.getReferences()){
			ids.add(ref.getDocument().getId());
		}
		if(nestedSearch){
			for(CatalogCollection nc : cc.getNestedCollections()){
				populateDocumentIdsForSearch(ids, nc, nestedSearch);
			}
		}
	}
	
	private void populateReferenceIdsForSearch(Set<Long> ids,
			CatalogCollection cc, boolean nestedSearch) {
		for(Reference ref : cc.getReferences()){
			ids.add(ref.getId());
		}
		if(nestedSearch){
			for(CatalogCollection nc : cc.getNestedCollections()){
				populateDocumentIdsForSearch(ids, nc, nestedSearch);
			}
		}
	}
	
	private void populateCollectionIdsForSearch(Set<Long> ids,
			CatalogCollection cc, boolean nestedSearch) {
		for(CatalogCollection nc : cc.getNestedCollections()){
			ids.add(nc.getId());
		}
		if(nestedSearch){
			for(CatalogCollection nc : cc.getNestedCollections()){
				populateDocumentIdsForSearch(ids, nc, nestedSearch);
			}
		}
	}
}
