package com.c9a.catalog.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.c9a.catalog.dao.CatalogDao;
import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.entities.CatalogObject;
import com.c9a.catalog.entities.CollectionAttribute;
import com.c9a.catalog.entities.Document;
import com.c9a.catalog.entities.DocumentAttribute;
import com.c9a.catalog.entities.Permission;
import com.c9a.catalog.entities.Reference;
import com.c9a.catalog.entities.ReferenceAttribute;
import com.c9a.catalog.exception.CollectionModificationException;
import com.c9a.catalog.exception.CollectionNotFoundException;
import com.c9a.catalog.exception.CollectionSharingException;
import com.c9a.catalog.exception.InvalidCatalogNameException;
import com.c9a.catalog.exception.InvalidCollectionPathException;
import com.c9a.catalog.exception.InvalidReferencePathException;
import com.c9a.catalog.exception.ObjectLockedException;
import com.c9a.catalog.exception.ReferenceModificationException;
import com.c9a.catalog.exception.ReferenceNotFoundException;
import com.c9a.catalog.exception.ReferenceSharingException;
import com.c9a.catalog.utils.CatalogUtils;

/**
 * 
 * @author Carter Youngblood
 *
 */
@Service("catalogService")
public class CatalogServiceImpl implements ICatalogService {
	
	private static final Logger LOG = Logger.getLogger(CatalogServiceImpl.class.getName());
	
	@Autowired
	private CatalogDao catalogDao;
	
	@Autowired
	private CatalogCacheManager cacheManager;
	
	@Override
	public CatalogCollection getRootCollectionForUser(String owner, String partitionId) {
		return catalogDao.getRootCollectionForUser(owner, partitionId);
	}

	@Override
	public List<CatalogCollection> getRootCollectionForPartition(String partitionId) {
		return catalogDao.getRootCollectionForPartition(partitionId);
	}

	@Override
	public CatalogCollection getRootApplicationCollectionForUser(String owner, String partitionId, String application) {
		return catalogDao.getRootApplicationForUser(owner, partitionId, application);
	}
	
	@Override
	public CatalogCollection getApplicationShareCollectionForUser(String owner, String partitionId, String application) {
		return catalogDao.getApplicationShareCollectionForUser(owner, partitionId, application);
	}

	@Override
	public CatalogCollection getCollection(String owner, String partitionId, String uniqueId) throws CollectionNotFoundException {
		CatalogCollection collection = catalogDao.findByUniqueId(CatalogCollection.class, uniqueId);
		if(collection == null){
			throw new CollectionNotFoundException();
		}
		return collection;
	}

	@Override
	public CatalogCollection addCollection(String owner, String partitionId, String collectionName, String parentCollectionUniqueId, Set<Long> sharePermissionIds, Map<String, String> attributes)	throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException {
		CatalogCollection parentCollection = getCollection(owner, partitionId, parentCollectionUniqueId);
		if(!CatalogUtils.canAdd(parentCollection.getPermissions())){
			throw new CollectionModificationException(CollectionModificationException.NOT_ENOUGH_PERMISSIONS_TO_ADD);
		}
		
		Set<Permission> permissions = null;
		if(sharePermissionIds != null && sharePermissionIds.size() > 0){
			permissions = catalogDao.getPermissionsForIds(sharePermissionIds);
		} 
		if(!CatalogObject.isValidName(collectionName)){
			throw new InvalidCatalogNameException();
		}
		
		if(CatalogUtils.isSharedCollection(parentCollection)){
			CatalogCollection newCollection = new CatalogCollection(owner, partitionId, collectionName, permissions, parentCollection);
			if(attributes!=null){
				for(Map.Entry<String, String> attr : attributes.entrySet()){
					newCollection.addAttribute(new CollectionAttribute(attr.getKey(), attr.getValue()));
				}
			}
			newCollection = save(CatalogCollection.class, newCollection);
			CatalogCollection addedCollection = addCollectionToShareCollections(CatalogUtils.getRootSharedCollection(parentCollection), newCollection, null, parentCollection.getUniqueId(), null);

			return addedCollection;
		} else {
			CatalogCollection addedCollection = new CatalogCollection(owner, partitionId, collectionName, permissions, parentCollection);
			
			if(attributes!=null){
				for(Map.Entry<String, String> attr : attributes.entrySet()){
					addedCollection.getAttributes().add(new CollectionAttribute(attr.getKey(), attr.getValue()));
				}
			}
			
			addedCollection = save(CatalogCollection.class, addedCollection);
			parentCollection.getNestedCollections().add(addedCollection);
			return addedCollection;
		}
	}
	
	@Override
	public CatalogCollection cloneCollection(String owner, String partitionId, String collectionToCloneUniqueId) throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException {
		CatalogCollection collectionToClone = catalogDao.findByUniqueId(CatalogCollection.class, collectionToCloneUniqueId);
		if(collectionToClone == null){
			throw new CollectionNotFoundException();
		}
		if(!CatalogUtils.canClone(collectionToClone.getPermissions())){
			throw new CollectionModificationException(CollectionModificationException.NOT_ENOUGH_PERMISSIONS_TO_CLONE);
		}
		return createCloneCollection(owner, partitionId, collectionToClone, collectionToClone.getParentCollection(), collectionToClone.getPermissions(), CatalogUtils.clonedName(collectionToClone.getName()));
	}

	@Override
	public CatalogCollection modifyCollection(String owner, String partitionId, String collectionUniqueId, String collectionName,
			String parentCollectionUniqueId, Set<Long> sharePermissionIds, Map<String, String> attributes)	throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException {
		
		CatalogCollection collectionToModify = this.getCollection(owner, partitionId, collectionUniqueId);
		
		if(CatalogUtils.isSharedCollection(collectionToModify) && !CatalogUtils.canModify(collectionToModify.getPermissions())){
			throw new CollectionModificationException(CollectionModificationException.NOT_ENOUGH_PERMISSIONS_TO_MODIFY);
		}
		
		if(!collectionToModify.getOwner().equals(owner)){
			throw new CollectionModificationException(CollectionModificationException.NOT_COLLECTION_OWNER);
		}
		
		
		boolean moveCollection = parentCollectionUniqueId != null && !parentCollectionUniqueId.equals(collectionToModify.getParentCollection().getUniqueId());
		CatalogCollection newParentCollection = moveCollection ? this.getCollection(owner, partitionId, parentCollectionUniqueId) : null;
		
		if(newParentCollection != null && CatalogUtils.isSharedCollection(newParentCollection) && CatalogUtils.isSharedCollection(collectionToModify)){
			// Do not support moving a shared collection into another shared collection.  Must unshare the collection, then move into a shared collection
			throw new CollectionModificationException(CollectionModificationException.CAN_NOT_MOVE_SHARED_COLLECION_INTO_SHARED_COLLECTION);
		} else if(newParentCollection != null && CatalogUtils.isSharedCollection(newParentCollection)){
			//We are moving this collection into a shared collection, s traverse to the root of the shared collection and propagate			
			if(!CatalogUtils.canModify(newParentCollection.getPermissions())){
				throw new CollectionModificationException(CollectionModificationException.NOT_ENOUGH_PERMISSIONS_TO_MODIFY);
			}
			CatalogCollection rootShareCollection = CatalogUtils.getRootSharedCollection(newParentCollection);
			addCollectionToShareCollections(rootShareCollection, collectionToModify, new CatalogCollection(), collectionToModify.getParentCollection().getUniqueId(), null);
			collectionToModify.setParentCollection(newParentCollection);
		} else if(newParentCollection !=null){
			if(!CatalogUtils.canModify(newParentCollection.getPermissions())){
				throw new CollectionModificationException(CollectionModificationException.NOT_ENOUGH_PERMISSIONS_TO_MODIFY);
			}
			collectionToModify.setParentCollection(newParentCollection);
		}
		
		if(sharePermissionIds != null && sharePermissionIds.size() > 0){
			collectionToModify.getPermissions().clear();
			collectionToModify.getPermissions().addAll(catalogDao.getPermissionsForIds(sharePermissionIds));
		}
		
		if(attributes!=null){
			collectionToModify.getAttributes().clear();
			for(Map.Entry<String, String> attr : attributes.entrySet()){
				collectionToModify.addAttribute(new CollectionAttribute(attr.getKey(), attr.getValue()));
			}
		}
		
		if(collectionName != null && collectionName.length() > 0 && !collectionName.equals(collectionToModify.getName())){
			collectionToModify.setName(collectionName);
		}
		return save(CatalogCollection.class, collectionToModify);
	}

	@Override
	public CatalogCollection deleteReferenceFromCollection(String owner, String partitionId, String referenceUniqueId, boolean deleteSharedTo) 
			throws CollectionModificationException, ReferenceNotFoundException {
		
		Reference reference = catalogDao.findByUniqueId(Reference.class, referenceUniqueId);
		if(reference == null){
			throw new ReferenceNotFoundException();
		}
		CatalogCollection collection = reference.getParentCollection();
		if(!CatalogUtils.canModify(collection.getPermissions())){
			throw new CollectionModificationException(CollectionModificationException.NOT_ENOUGH_PERMISSIONS_TO_MODIFY);
		}
		
		if(CatalogUtils.isSharedReference(reference) && deleteSharedTo){
			removeAllSharedReferences(reference);
			archiveCatalogObject(reference);
			collection.getReferences().remove(reference);
			collection.getArchivedReferences().add(reference);
			save(Reference.class, reference);
			return collection;
		} else {
			collection.getReferences().remove(reference);
			catalogDao.delete(reference);
			return collection;
		}
	}

	@Override
	public void shareCollection(String owner, 
			String partitionId, 
			String application,
			String collectionToShareUniqueId,
			Map<String, String> userAndPartitionsToShareWith,
			Set<Long> sharePermissionIds) throws CollectionSharingException,
			CollectionNotFoundException, InvalidCatalogNameException {
		
		CatalogCollection collectionToShare = getCollection(owner, partitionId, collectionToShareUniqueId);
		
		if(!CatalogUtils.canShare(collectionToShare.getPermissions())){
			throw new CollectionSharingException();
		}
		
		Set<Permission> permissionsToShare = null;
		if(sharePermissionIds != null)
			permissionsToShare = catalogDao.getPermissionsForIds(sharePermissionIds);
		
		CatalogUtils.validateCollectionCanBeShared(collectionToShare, permissionsToShare);
		
		for(Map.Entry<String, String> userAndPartition : userAndPartitionsToShareWith.entrySet()){
			String newOwner = userAndPartition.getKey();
			String newPartitionId = userAndPartition.getValue();
			CatalogCollection usersShareCollection = getApplicationShareCollectionForUser(newOwner, newPartitionId, application);
			createSharedCollection(newOwner, newPartitionId, collectionToShare, usersShareCollection, permissionsToShare, collectionToShare.getName());
		}
	}
	
	@Override
	public void shareCollectionIntoCollection(
			String owner,
			String partitionId,
			String collectionToShareUniqueId,
			String collectionToShareIntToUniqueId,
		    Set<Long> sharePermissionIds) 
				throws  CollectionNotFoundException, CollectionSharingException, CollectionModificationException, InvalidCatalogNameException{
		
		CatalogCollection collectionToShare = getCollection(owner, partitionId, collectionToShareUniqueId);
		CatalogCollection collectionToShareInto = getCollection(owner, partitionId, collectionToShareIntToUniqueId);
		
		if(!CatalogUtils.canShare(collectionToShare.getPermissions())){
			throw new CollectionSharingException();
		}
		
		if(!CatalogUtils.canAdd(collectionToShareInto.getPermissions())){
			throw new CollectionModificationException(CollectionModificationException.NOT_ENOUGH_PERMISSIONS_TO_ADD);
		}
		
		Set<Permission> permissionsToShare = null;
		if(sharePermissionIds != null)
			permissionsToShare = catalogDao.getPermissionsForIds(sharePermissionIds);
		
		CatalogUtils.validateCollectionCanBeShared(collectionToShare, permissionsToShare);
		
		if(CatalogUtils.isSharedCollection(collectionToShareInto)){
			CatalogCollection rootSharedCollection = CatalogUtils.getRootSharedCollection(collectionToShareInto);
			for(CatalogCollection cc : rootSharedCollection.getSharedTo()){
				createSharedCollection(cc.getOwner(), partitionId, collectionToShare, cc, permissionsToShare, collectionToShare.getName());
			}
			createSharedCollection(rootSharedCollection.getOwner(), partitionId, collectionToShare, rootSharedCollection, permissionsToShare, collectionToShare.getName());
		} else {
			createSharedCollection(owner, partitionId, collectionToShare, collectionToShareInto, permissionsToShare, collectionToShare.getName());
		}
		
	}
	
	@Override
	public void deleteCollection(String owner, String partitionId, String collectionUniqueId, boolean deleteCreatedShares) throws CollectionNotFoundException {
		CatalogCollection collection = getCollection(owner, partitionId, collectionUniqueId);
		deleteCatalogCollection(collection, deleteCreatedShares);
	}
	
	private void deleteCatalogCollection(CatalogCollection collection, boolean deleteCreatedShares) throws CollectionNotFoundException {
		archiveCatalogObject(collection);
		for(Reference ref : collection.getReferences()){
			archiveCatalogObject(ref);
		}
		for(CatalogCollection nestedCollection : collection.getNestedCollections()){
			deleteCollection(nestedCollection.getOwner(), nestedCollection.getPartitionId(), nestedCollection.getUniqueId(), deleteCreatedShares);
		}
		if(deleteCreatedShares){
			for(CatalogCollection sharedCollection : collection.getSharedTo()){
				// should all created shares be deleted, from the 2nd jump?
				// Example share 
				//     A -> B
				//			B -> C
				// Do we delete C if A is deleted with delete created share flag set to true
				// Right now we do...
				deleteCollection(sharedCollection.getOwner(), sharedCollection.getPartitionId(), sharedCollection.getUniqueId(), deleteCreatedShares);
			}
		}
		if(collection.getParentCollection()!=null){
			collection.getParentCollection().getNestedCollections().remove(collection);
		}
	}

	private void archiveCatalogObject(CatalogObject catalogObject){
		catalogObject.setArchived(true);
		cacheManager.removeFromCache(catalogObject);
		catalogDao.update(catalogObject);
	}
	
	@Override
	public void deleteCollection(String owner, String partitionId, String collectionUniqueId) throws CollectionNotFoundException {
		deleteCollection(owner, partitionId, collectionUniqueId, false);
	}
	
	@Override
	public Reference getReference(String owner, String partitionId,
			String referenceUniqueId) throws ReferenceNotFoundException {
		Reference ref = catalogDao.findByUniqueId(Reference.class, referenceUniqueId);
		if(ref == null){
			throw new ReferenceNotFoundException();
		}
		return ref;
	}
	
	@Override
	public Reference moveReference(String owner, String partitionId, String referenceUniqueId,
			String owningCollectionUniqueId) throws CollectionNotFoundException, CollectionModificationException,
			ReferenceModificationException, ReferenceNotFoundException, InvalidCatalogNameException {
		
		Reference referenceToModify = catalogDao.findByUniqueId(Reference.class, referenceUniqueId);
		if(referenceToModify == null){
			throw new ReferenceNotFoundException();
		}
		
		CatalogCollection newParentCollection = catalogDao.findByUniqueId(CatalogCollection.class, owningCollectionUniqueId);
		if(newParentCollection == null)
			throw new CollectionNotFoundException();
		
		if(CatalogUtils.isSharedReference(referenceToModify)){
			if(!CatalogUtils.canModify(referenceToModify.getPermissions())){
				throw new ReferenceModificationException(ReferenceModificationException.NOT_ENOUGH_PERMISSIONS_TO_MODIFY);
			}
			// Shared Collection exists as part of a shared collection
			if(CatalogUtils.isSharedCollection(referenceToModify.getParentCollection())){
				// Make sure that the user can modify the parent shared collection
				if(!CatalogUtils.canModify(referenceToModify.getParentCollection().getPermissions())){
					throw new CollectionModificationException(CollectionModificationException.NOT_ENOUGH_PERMISSIONS_TO_MODIFY);
				}
				removeAllSharedReferences(CatalogUtils.getRootSharedReference(referenceToModify));
			}
		}
		
		if(CatalogUtils.isSharedCollection(newParentCollection)){
			if(!CatalogUtils.canAdd(newParentCollection.getPermissions())){
				throw new CollectionModificationException(CollectionModificationException.NOT_ENOUGH_PERMISSIONS_TO_ADD);
			}
			Reference newRef = CatalogUtils.clone(referenceToModify, newParentCollection, referenceToModify.getPermissions(), referenceToModify.getName());
			
			newRef.setCreatedFrom(null);
			newRef = catalogDao.findById(Reference.class, catalogDao.save(newRef));
			newParentCollection.getReferences().add(newRef);
			CatalogCollection rootShareCollection = CatalogUtils.getRootSharedCollection(newParentCollection);
			addReferenceToShareCollections(newRef, newParentCollection.getUniqueId(), null,	referenceToModify.getPermissions(), rootShareCollection);
			
			return newRef;
		} else {
			Reference newRef = CatalogUtils.clone(referenceToModify, newParentCollection, referenceToModify.getPermissions(), referenceToModify.getName());
			newRef.setCreatedFrom(null);
			newRef = catalogDao.findById(Reference.class, catalogDao.save(newRef));
			newParentCollection.getReferences().add(newRef);
			
			return newRef;
		}
	}

	@Override
	public Reference modifyReference(String owner, String partitionId, String referenceUniqueId, String referenceName,
			Set<Long> sharePermissionIds,
			Map<String, String> attributes) throws CollectionNotFoundException,
			ReferenceModificationException, ReferenceNotFoundException, InvalidCatalogNameException {
		
		Reference referenceToModify = catalogDao.findByUniqueId(Reference.class, referenceUniqueId);
		if(referenceToModify == null){
			throw new ReferenceNotFoundException();
		}
		if(CatalogUtils.isSharedReference(referenceToModify)){
			if(!CatalogUtils.canModify(referenceToModify.getPermissions())){
				throw new ReferenceModificationException(ReferenceModificationException.NOT_ENOUGH_PERMISSIONS_TO_MODIFY);
			}
		}
		
		modifyReference(CatalogUtils.getRootSharedReference(referenceToModify), referenceName, attributes, sharePermissionIds);
		
		return referenceToModify;
	}

	/**
	 * This helper method will remove all shared {@link Reference}(s)
	 * 
	 * @param reference		The root share chain for the {@link Reference}
	 */
	private void removeAllSharedReferences(Reference reference) {
		for(Reference ref : reference.getSharedTo()){
			removeAllSharedReferences(ref);
		}
		reference.getParentCollection().getReferences().remove(reference);
		LOG.fine("remove ref uniqueId : " + reference.getUniqueId());
		catalogDao.delete(reference);
	}

	@Override
	public void shareReference(String owner, 
			String partitionId,
			String referenceUniqueId,
			String application,
			Map<String, String> userAndPartitionsToShareWith,
			Set<Long> sharePermissionIds) throws ReferenceNotFoundException, ReferenceSharingException, InvalidCatalogNameException {
		
		Reference referenceToShare = catalogDao.findByUniqueId(Reference.class, referenceUniqueId);
		if(referenceToShare == null){
			throw new ReferenceNotFoundException();
		}
		Set<Permission> permissions = catalogDao.getPermissionsForIds(sharePermissionIds);
		if(!CatalogUtils.canShare(referenceToShare.getPermissions())){
			throw new ReferenceSharingException(Permission.CAN_NOT_SHARE_REFERENCE_NOT_ENOUGH_PERMISSION_MESSAGE);
		}
		if(!CatalogUtils.hasAtLeastPermissions(referenceToShare.getPermissions(), permissions)){
			throw new ReferenceSharingException(Permission.CAN_ADD_MORE_PERMISSION_THAN_EXISTS_MESSAGE);
		}
		for(Map.Entry<String, String>userAndPartition : userAndPartitionsToShareWith.entrySet()){
			String newOwner = userAndPartition.getKey();
			String newPartitionId = userAndPartition.getValue();
			CatalogCollection shareCollection = getApplicationShareCollectionForUser(newOwner, newPartitionId, application);
			shareReference(newOwner, newPartitionId, referenceToShare, permissions, shareCollection, referenceToShare.getName());
		}
	}
	
	public Reference shareReferenceIntoCollection(
			String owner, 
			String partitionId, 
			String referenceUniqueId,
			String collectionUniqueId,
			Set<Long> sharePermissionIds) 
				throws ReferenceNotFoundException, ReferenceSharingException, InvalidCatalogNameException, CollectionNotFoundException, CollectionModificationException{
		Reference referenceToShare = catalogDao.findByUniqueId(Reference.class, referenceUniqueId);
		if(referenceToShare == null){
			throw new ReferenceNotFoundException();
		}
		Set<Permission> permissions = null;
		if(sharePermissionIds!= null){
			permissions = catalogDao.getPermissionsForIds(sharePermissionIds);
		}
		if(!CatalogUtils.canShare(referenceToShare.getPermissions())){
			throw new ReferenceSharingException(Permission.CAN_NOT_SHARE_REFERENCE_NOT_ENOUGH_PERMISSION_MESSAGE);
		}
		if(permissions!=null  && !CatalogUtils.hasAtLeastPermissions(referenceToShare.getPermissions(), permissions)){
			throw new ReferenceSharingException(Permission.CAN_ADD_MORE_PERMISSION_THAN_EXISTS_MESSAGE);
		}
		CatalogCollection collectionToShareInto = catalogDao.findByUniqueId(CatalogCollection.class, collectionUniqueId);
		if(collectionToShareInto == null){
			throw new CollectionNotFoundException();
		}
		if(CatalogUtils.isSharedCollection(collectionToShareInto)){
			if(!CatalogUtils.canAdd(collectionToShareInto.getPermissions())){
				throw new CollectionModificationException();
			}
			
			CatalogCollection rootSharedCollection = CatalogUtils.getRootSharedCollection(collectionToShareInto);
			Reference refToReturn = null;
			
			for(CatalogCollection cc : rootSharedCollection.getSharedTo()){
				Reference sharedRef = shareReference(owner, partitionId, referenceToShare, permissions, cc, referenceToShare.getName());
				if(sharedRef.getParentCollection().getUniqueId().equals(collectionToShareInto.getUniqueId())){
					refToReturn = sharedRef;
				}
			}
			return refToReturn;
		} else {
			if(!collectionToShareInto.getOwner().equals(owner)){
				throw new CollectionModificationException();
			}
			return shareReference(owner, partitionId, referenceToShare, permissions, collectionToShareInto, referenceToShare.getName());
		}
	}

	@Override
	public Reference cloneRefernence(String owner, String partitionId, String referenceUniqueId, Set<Long> sharePermissionIds, String collectionToPutClonedReferenceUniqueId) throws ReferenceNotFoundException, CollectionModificationException, ReferenceModificationException, CollectionNotFoundException, InvalidCatalogNameException {
		
		Reference referenceToClone = catalogDao.findByUniqueId(Reference.class, referenceUniqueId);
		if(referenceToClone == null ){
			throw new ReferenceNotFoundException();
		}
		CatalogCollection collection = getCollection(owner, partitionId, collectionToPutClonedReferenceUniqueId);
		
		if(!CatalogUtils.canClone(referenceToClone.getPermissions())){
			throw new ReferenceModificationException(ReferenceModificationException.NOT_ENOUGH_PERMISSIONS_TO_CLONE);
		}
		
		return cloneReference(owner, partitionId, referenceToClone, sharePermissionIds, collection, CatalogUtils.clonedName(referenceToClone.getName()));
	}


	@Override
	public Reference addDocument(
			String owner, 
			String partitionId,
			String documentName, 
			String collectionUniqueId, byte[] payload,
			Set<Long> sharePermissionIds,
			Map<String, String> attributes) throws CollectionNotFoundException, CollectionModificationException {
		
		CatalogCollection parentCollection = catalogDao.findByUniqueId(CatalogCollection.class, collectionUniqueId);
		if(parentCollection == null){
			throw new CollectionNotFoundException();
		}
		
		Set<Permission> permissions = parentCollection.getPermissions();
		if(sharePermissionIds != null && sharePermissionIds.size() > 0){
			permissions = catalogDao.getPermissionsForIds(sharePermissionIds);
		}
		
		if(CatalogUtils.isSharedCollection(parentCollection)){
			if(!CatalogUtils.canAdd(parentCollection.getPermissions())){
				throw new CollectionModificationException(CollectionModificationException.NOT_ENOUGH_PERMISSIONS_TO_ADD);
			}
			
			Document document = new Document(documentName, owner, partitionId, payload);
			if(attributes != null){
				for(Map.Entry<String, String> att : attributes.entrySet()){
					document.addAttribute(new DocumentAttribute(att.getKey(), att.getValue()));
				}
			}
			document = save(Document.class, document);
			
			return addDocumentToShareCollections(document, parentCollection.getUniqueId(), null, permissions, CatalogUtils.getRootSharedCollection(parentCollection), null);
			
		} else {
			Document document = new Document(documentName, owner, partitionId, payload);
			if(attributes != null){
				for(Map.Entry<String, String> att : attributes.entrySet()){
					document.addAttribute(new DocumentAttribute(att.getKey(), att.getValue()));
				}
			}
			document = save(Document.class, document);
			Reference reference = save(Reference.class, new Reference(owner, partitionId, documentName, document, parentCollection, permissions));
			parentCollection.getReferences().add(reference);
//			catalogDao.update(parentCollection);
			return reference;	
		}
	}
	
	@Override
	public Reference modifyDocumentByReferenceId(
			String owner, 
			String partitionId, 
			String referenceUniqueId,
			String documentName, 
			String documentNotes, 
			String documentDescription, 
			Boolean locked, 
			Boolean archive,
			byte[] payload, 
			Set<Long> sharePermissionIds,
			Map<String, String> attributes) 
				throws ReferenceNotFoundException, ReferenceModificationException, ObjectLockedException, InvalidCatalogNameException {
		
		Reference referenceToModify = catalogDao.findByUniqueId(Reference.class, referenceUniqueId);
		if(referenceToModify == null){
			throw new ReferenceNotFoundException();
		}
		if(CatalogUtils.isSharedReference(referenceToModify) && !CatalogUtils.canModify(referenceToModify.getPermissions())){
			throw new ReferenceModificationException(ReferenceModificationException.NOT_ENOUGH_PERMISSIONS_TO_MODIFY);
		}
		if(!owner.equals(referenceToModify.getDocument().getOwner())){
			throw new ReferenceModificationException(ReferenceModificationException.NOT_ENOUGH_PERMISSIONS_TO_MODIFY);
		}
		Document documentToModify = referenceToModify.getDocument();
		if(documentToModify.isLocked()){
			throw new ObjectLockedException(documentToModify);
		}
		
		updateDocument(documentToModify, documentName, documentNotes, documentDescription, locked, archive, payload, sharePermissionIds, attributes);
//		catalogDao.refresh(referenceToModify);
		return referenceToModify;
	}

	private Document updateDocument(Document documentToModify, String documentName,
			String documentNotes, String documentDescription, Boolean locked,
			Boolean archive, byte[] payload, Set<Long> sharePermissionIds, Map<String, String> attributes) throws InvalidCatalogNameException {
		if(documentName != null && documentName.length() > 0){
			documentToModify.setName(documentName);
		}
		if(documentNotes != null && documentNotes.length() > 0){
			documentToModify.setNotes(documentNotes);
		}
		if(documentDescription != null && documentDescription.length() > 0){
			documentToModify.setDescription(documentDescription);
		}
		if(locked != null){
			documentToModify.setLocked(locked);
		}
		if(archive != null){
			documentToModify.setArchived(archive);
		}
		if(payload != null && payload.length > 0){
			documentToModify.setPayload(payload);
		}
		if(attributes != null && attributes.size() > 0){
			documentToModify.getAttributes().clear();
			for(Map.Entry<String, String> att : attributes.entrySet()){
				documentToModify.addAttribute(new DocumentAttribute(att.getKey(), att.getValue()));
			}
		}
		return save(Document.class, documentToModify);
	}

	@Override
	public Reference cloneDocument(String owner, String partitionId, String referenceToDocumentUniqueId, String newParentCollectionUniqueId) throws ReferenceNotFoundException, CollectionNotFoundException, CollectionSharingException, CollectionModificationException, ReferenceModificationException, InvalidCatalogNameException {
		
		Reference currentReferenceToDocument = catalogDao.findByUniqueId(Reference.class, referenceToDocumentUniqueId);
		if(currentReferenceToDocument == null){
			throw new ReferenceNotFoundException();
		}
		if(!CatalogUtils.canClone(currentReferenceToDocument.getPermissions())){
			throw new ReferenceModificationException(Permission.CAN_NOT_CLONE_REFERENCE_NOT_ENOUGH_PERMISSION_MESSAGE); 
		}
		CatalogCollection owningCollection;
		if(newParentCollectionUniqueId != null && !newParentCollectionUniqueId.equals("")){
			owningCollection = catalogDao.findByUniqueId(CatalogCollection.class, newParentCollectionUniqueId);
		} else {
			owningCollection = currentReferenceToDocument.getParentCollection();
		}
		if(owningCollection  == null){
			throw new CollectionNotFoundException();
		}
		if(!CatalogUtils.canModify(owningCollection.getPermissions())){
			throw new CollectionModificationException(Permission.CAN_NOT_MODIFY_COLLECTION_NOT_ENOUGH_PERMISSION); 
		}
		
		Document clonedDocument = CatalogUtils.clone(currentReferenceToDocument.getDocument());
		clonedDocument = save(Document.class, clonedDocument);
		
		Reference clonedReference = CatalogUtils.clone(currentReferenceToDocument, owningCollection, currentReferenceToDocument.getPermissions());
		clonedReference.setDocument(clonedDocument);
		return save(Reference.class, clonedReference);
	}

	@Override
	public void deleteDocument(String owner, String partitionId, String documentUniqueId)  {
		Document document = catalogDao.findByUniqueId(Document.class, documentUniqueId);
		for(Reference r : document.getReferences()){
			archiveCatalogObject(r);
			catalogDao.update(r);
		}
		archiveCatalogObject(document);
		catalogDao.update(document);
	}

	private void modifyReference(Reference referenceToModify, String referenceName, Map<String, String> attributes, Set<Long> sharePermissionIds) throws InvalidCatalogNameException {
		for(Reference r : referenceToModify.getSharedTo()){
			modifyReference(r, referenceName, attributes, sharePermissionIds);
		}
		updateReference(referenceToModify, referenceName, attributes, sharePermissionIds);
	}

	private void updateReference(Reference referenceToModify, String referenceName,	Map<String, String> attributes, Set<Long> sharePermissionIds) throws InvalidCatalogNameException {
		if(sharePermissionIds != null && sharePermissionIds.size() > 0){
			referenceToModify.getPermissions().clear();
			referenceToModify.getPermissions().addAll(catalogDao.getPermissionsForIds(sharePermissionIds));
		}
		if(attributes != null){
			for(Map.Entry<String, String> attr : attributes.entrySet()){
				referenceToModify.addAttribute(new ReferenceAttribute(attr.getKey(), attr.getValue()));
			}
		}
		if(referenceName != null && referenceName.length() > 0 && !referenceName.equals(referenceToModify.getName())){
			referenceToModify.setName(referenceName);
		}
		save(Reference.class, referenceToModify);
		catalogDao.save(referenceToModify);
	}
	
	/**
	 * This method assumes that we start at a share root for a collection, and then adds the reference to each collection that is in the sharing chain.
	 * 
	 * @param document						The already created document to add to the new reference(s)
	 * @param collectionToAddDocUniqueId	The collection unique id, used to return the created reference for the collection that we care about
	 * @param sharedFromReference			The reference that we use to keep the share chain in tact
	 * @param permissions					The permissions to add to the Reference
	 * @param shareCollection				The Collection that we use to add the reference to
	 * @param refForCollection				The created Reference that is created in the collectionToAddDocUniqueId
	 * 
	 * @return the reference that is created in the collection that we care about
	 */
	private Reference addDocumentToShareCollections(Document document, String collectionToAddDocUniqueId, Reference sharedFromReference, Set<Permission> permissions, CatalogCollection shareCollection, Reference refForCollection) {
		String refName = sharedFromReference == null ? document.getName() : sharedFromReference.getName();
		Reference newRefForCollection = new Reference(refName, document, shareCollection, permissions);
		newRefForCollection.setCreatedFrom(sharedFromReference);
		newRefForCollection.setParentCollection(shareCollection);
		newRefForCollection = save(Reference.class, newRefForCollection);
		
		shareCollection.getReferences().add(newRefForCollection);
		catalogDao.update(shareCollection);
		if(collectionToAddDocUniqueId.equals(shareCollection.getUniqueId())){
			refForCollection = newRefForCollection;
		}
		for(CatalogCollection cc : shareCollection.getSharedTo()){
			Reference ref = addDocumentToShareCollections(document, collectionToAddDocUniqueId, newRefForCollection, permissions, cc, refForCollection);
			if(ref != null){
				refForCollection = ref;
			}
		}
		save(Reference.class, newRefForCollection);
		return refForCollection;
	}
	
	/**
	 * This method assumes that we start at a share root for a collection, and then adds the reference to each collection that is in the sharing chain.
	 * 
	 * @param document						The already created document to add to the new reference(s)
	 * @param collectionToAddDocUniqueId	The collection unique id, used to return the created reference for the collection that we care about
	 * @param sharedFromReference			The reference that we use to keep the share chain in tact
	 * @param permissions					The permissions to add to the Reference
	 * @param shareCollection				The Collection that we use to add the reference to
	 * 
	 * @return the reference that is created in the collection that we care about
	 * @throws InvalidCatalogNameException 
	 */
	private void addReferenceToShareCollections(Reference refToAdd, String collectionToAddDocUniqueId, Reference sharedFromReference, Set<Permission> permissions, CatalogCollection shareCollection) throws InvalidCatalogNameException {
		Reference newRefForCollection = CatalogUtils.clone(refToAdd, shareCollection, permissions);
		newRefForCollection = save(Reference.class, newRefForCollection);
		for(CatalogCollection cc : shareCollection.getSharedTo()){
			addReferenceToShareCollections(newRefForCollection, collectionToAddDocUniqueId, newRefForCollection, permissions, cc);
		}
	}
	
	/**
	 * This method shared a collection with a set of users and gives them a set of permissions for the shared collection.  The method will first try to validate that all the objects can be shared, as well as have the minimum set of permissions
	 * 
	 * @param owner								the owner (user) needed to find the collection
	 * @param partitionId						the partitionId (customer id) needed to find the collection
	 * @param collectionToShareUniqueId			the collection to be shared
	 * @param userAndPartitionsToShareWith		a map of users and partition id's (customer id's) to share the collection with
	 * @param sharePermissionIds				the set of permissions to add to the newly shared collection
	 * @throws InvalidCatalogNameException 
	 * 
	 * @throws CollectionNotFoundException		thrown when either the parent collection is not found, or the collection to modify is not found
	 * @throws CollectionSharingException		thrown when the collection can not be shared, or one of its objects can not be shared or if the collection or one of the objects does not have the minimum sharing permissions
	 */
	private CatalogCollection createSharedCollection(String newOwner, String newPartitionId, CatalogCollection collectionToShare, CatalogCollection parentCollection, Set<Permission> sharePermissionIds, String collectionName) throws InvalidCatalogNameException {
		CatalogCollection newCollection = new CatalogCollection(newOwner, newPartitionId, collectionName, sharePermissionIds, parentCollection);

		newCollection.setCreatedFrom(collectionToShare);
		newCollection = save(CatalogCollection.class, newCollection);
		
		if(collectionToShare.getNestedCollections() != null && collectionToShare.getNestedCollections().size() > 0){
			for(CatalogCollection cc : collectionToShare.getNestedCollections()){
				createSharedCollection(newOwner, newPartitionId, cc, newCollection, sharePermissionIds, cc.getName());
			}
		}
		
		if(collectionToShare.getReferences() != null && collectionToShare.getReferences().size() > 0){
			for(Reference r : collectionToShare.getReferences()){
				shareReference(newOwner, newPartitionId, r, sharePermissionIds, newCollection, collectionName);
			}
		}
		
		newCollection = save(CatalogCollection.class, newCollection);
		collectionToShare.getCreatedTo().add(newCollection);
		catalogDao.update(collectionToShare);
		parentCollection.getNestedCollections().add(newCollection);
		catalogDao.update(parentCollection);
		return newCollection;
	}
	
	/**
	 * This helper method will clone all the nested collections and references of a given collection.  This method should only be called after the collection has been validated that it can share/cloneable and has the minimum permissions.
	 * This is a recursive function and will call itself for all nested collections.
	 * 
	 * @param newOwner							the owner (user) of the cloned collection
	 * @param newPartitionId					the partitionId (customer id) of the cloned collection
	 * @param collectionToClone					the collection to be shared
	 * @param parentCollection					the collection that the share will belong to
	 * @param sharePermissionIds				the set of permissions to add to the newly shared collection
	 * @param collectionName					the name of the newly cloned collection
	 * @param isLocked							tells the method whether or not to lock all the items - used in the sharng case
	 * 
	 * @return									the created cloned collection
	 * @throws InvalidCatalogNameException 
	 * 
	 */
	private CatalogCollection createCloneCollection(String newOwner, String newPartitionId, CatalogCollection collectionToClone, CatalogCollection parentCollection, Set<Permission> sharePermissionIds, String collectionName) throws InvalidCatalogNameException {
		CatalogCollection newCollection = new CatalogCollection(newOwner, newPartitionId, collectionName, sharePermissionIds, parentCollection);
		newCollection.setCreatedFrom(collectionToClone);
		
		newCollection = save(CatalogCollection.class, newCollection);
		
		if(collectionToClone.getNestedCollections().size() > 0){
			for(CatalogCollection cc : collectionToClone.getNestedCollections()){
				createCloneCollection(newOwner, newPartitionId, cc, newCollection, sharePermissionIds, cc.getName());
			}
		}
		
		if(collectionToClone.getReferences().size() > 0){
			for(Reference r : collectionToClone.getReferences()){
				Reference newRef = CatalogUtils.clone(r, newCollection, sharePermissionIds);
				newRef = save(Reference.class, newRef);
				newCollection.getReferences().add(newRef);
			}
		}
		
		if(CatalogUtils.isSharedCollection(parentCollection)){
			for(CatalogCollection sharedTo : parentCollection.getSharedTo()){
				createCloneCollection(sharedTo.getOwner(), sharedTo.getPartitionId(), sharedTo, collectionToClone, sharePermissionIds, collectionToClone.getName());
			}
		}
		catalogDao.refresh(parentCollection);
		return save(CatalogCollection.class, newCollection);
	}
	
	/**
	 * This is a helper method method clones a given reference for sharing.  It assumes that the validity check on whether or not it can be cloned has already happened.  It will also lock the reference.
	 * 
	 * @param owner								the owner (user) needed to find the collection
	 * @param partitionId						the partitionId (customer id) needed to find the collection
	 * @param referenceUniqueId					the reference to be shared
	 * @param sharePermissionIds				the set of permissions to add to the newly cloned reference
	 * @param collection						the collection that the reference will be placed in
	 * @param newReferenceName					the name of the new reference
	 * 
	 * @return									the cloned reference
	 * @throws InvalidCatalogNameException 
	 * 
	 * @throws ReferenceNotFoundException		thrown when reference to clone is not found
	 */
	private Reference shareReference(String newOwner, String newPartitionId,
			Reference referenceToShare, Set<Permission> sharePermissions,
			CatalogCollection shareCollection, String name) throws InvalidCatalogNameException {
		Reference newReference = CatalogUtils.clone(referenceToShare, shareCollection, sharePermissions);
		newReference.setOwner(newOwner);
		newReference.setPartitionId(newPartitionId);
		newReference.setName(name);
		
		newReference = save(Reference.class, newReference);
		shareCollection.getReferences().add(newReference);
		catalogDao.save(shareCollection);
		
		referenceToShare.getCreatedTo().add(newReference);
		catalogDao.save(referenceToShare);
		return newReference;
	}

	/**
	 * This is a helper method method clones a given reference.  It assumes that the validity check on whether or not it can be cloned has already happened.
	 * 
	 * @param owner								the owner (user) needed to find the collection
	 * @param partitionId						the partitionId (customer id) needed to find the collection
	 * @param referenceUniqueId					the reference to be shared
	 * @param sharePermissionIds				the set of permissions to add to the newly cloned reference
	 * @param collection						the collection that the reference will be placed in
	 * @param newReferenceName					the name of the new reference
	 * 
	 * @return									the cloned reference
	 * 
	 * @throws ReferenceNotFoundException		thrown when reference to clone is not found
	 * @throws InvalidCatalogNameException 
	 */
	private Reference cloneReference(String owner, String partitionId, Reference reference, Set<Long> sharePermissionIds, CatalogCollection collection, String newReferenceName) throws ReferenceNotFoundException, InvalidCatalogNameException {
		if(CatalogUtils.isSharedCollection(collection)){
			return addDocumentToShareCollections(reference.getDocument(), collection.getUniqueId(), null, reference.getPermissions(), CatalogUtils.getRootSharedCollection(collection), null);
		} else {
			Reference newReference = CatalogUtils.clone(reference, collection, catalogDao.getPermissionsForIds(sharePermissionIds));
			newReference.setName(newReferenceName);
			return save(Reference.class, newReference);
		}
	}
	
	/**
	 * This method will make sure to propagate the adding of a collection to the root of the shared collection, and propagate to all the shared collections.
	 * 
	 * @param parentShareCollection			The root of the collection share chain
	 * @param newCollection					The new collection to add to the share chain
	 * @param sharedFromCollection			The shared from collection, to create the new share chain for the newly added collection
	 * @param originalParentCollectionUniqueId		The original parent collection 
	 * @return 
	 * @throws InvalidCatalogNameException 
	 */
	private CatalogCollection addCollectionToShareCollections(CatalogCollection parentShareCollection, CatalogCollection newCollection, CatalogCollection sharedFromCollection, String originalParentCollectionUniqueId, CatalogCollection collectionToReturn) throws InvalidCatalogNameException {
		if(!newCollection.getParentCollection().getId().equals(parentShareCollection.getId())){
			newCollection = CatalogUtils.clone(newCollection, parentShareCollection, false);
			newCollection.setOwner(parentShareCollection.getOwner());
			newCollection = catalogDao.findById(CatalogCollection.class, catalogDao.save(newCollection));
		}
		for(CatalogCollection nc : newCollection.getNestedCollections()){
			catalogDao.save(nc);
		}
		parentShareCollection.getNestedCollections().add(newCollection);
		catalogDao.update(parentShareCollection);
		for(CatalogCollection cc : parentShareCollection.getSharedTo()){
			CatalogCollection collection = addCollectionToShareCollections(cc, newCollection, newCollection, originalParentCollectionUniqueId, collectionToReturn);
			if(collection != null){
				collectionToReturn = collection;
			}
		}
		if(newCollection.getParentCollection().getUniqueId().equals(originalParentCollectionUniqueId)){
			collectionToReturn = newCollection;
		}
		newCollection = save(CatalogCollection.class, newCollection);
		return collectionToReturn;
	}
	
    protected <T> T save(final Class<T> clazz, Object entity){
		T object = catalogDao.findById(clazz, catalogDao.save(entity));
		return object;
	}

	@Override
	public CatalogCollection getCollectionByPath(String owner,
			String partitionId, String path) throws CollectionNotFoundException, InvalidCollectionPathException {
		if(!CatalogUtils.isValidCollectionPath(path)){
			throw new InvalidCollectionPathException();
		}
		Map<String, String> fieldAndValue = new HashMap<String, String>();
		fieldAndValue.put("path", path);
		List<CatalogCollection> collections = catalogDao.findObjectsThatMatchFieldValue(CatalogCollection.class, fieldAndValue , true);
		if(collections.size() == 0){
			throw new CollectionNotFoundException();
		}
		return collections.iterator().next();
	}
	
	public CatalogCollection createCollectionAtPath(
			String owner,
			String partitionId, 
			String path,
			Set<Long> permissionIds) throws CollectionNotFoundException, CollectionModificationException, InvalidCollectionPathException{
		if(!CatalogUtils.isValidCollectionPath(path)){
			throw new InvalidCollectionPathException();
		}
		StringTokenizer paths = new StringTokenizer(path, "/");
		List<String> sPaths = new ArrayList<String>();
		while(paths.hasMoreTokens()){
			sPaths.add(paths.nextToken());
		}
		Collections.reverse(sPaths);
		if(sPaths.size() == 1){
			return catalogDao.createRootCollectionForPartition(owner, partitionId, sPaths.get(0), permissionIds);
		} else {
			sPaths.get(1);
		}
		
		return null;
	}
	
	public static void main(String... args){
		StringTokenizer paths = new StringTokenizer("/");
		List<String> sPaths = new ArrayList<String>();
		while(paths.hasMoreTokens()){
			sPaths.add(paths.nextToken());
		}
		Collections.reverse(sPaths);
	}

	@Override
	public Reference getReferenceByPath(String owner, String partitionId,
			String path, Boolean createIfNotExist,
			List<Long> permissions) throws ReferenceNotFoundException, InvalidReferencePathException {
		Map<String, String> fieldAndValue = new HashMap<String, String>();
		fieldAndValue.put("path", path);
		List<Reference> refs = catalogDao.findObjectsThatMatchFieldValue(Reference.class, fieldAndValue, true);
		if(refs == null || refs.size() == 0){
			return null;
		}
		return refs.iterator().next();
	}
}	