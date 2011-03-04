package com.c9a.catalog.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.entities.Reference;
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

/**
 * 
 * @author Carter Youngblood
 *
 */
public interface ICatalogService {
	
	// Collections
	/**
	 * This method returns the root collection for a owner (user id) and a partitionId (customer id).  If there is none yet in the database, this method is responsible for creating the collection.
	 * 
	 * @param owner			the owner (owner of collection) needed to find the root collection
	 * @param partitionId	the partitionId (customer id) needed to find the root collection
	 * 
	 * @return				the root collection for a user				
	 */
	CatalogCollection getRootCollectionForUser(
			String owner, 
			String partitionId);
	
	/**
	 * This method returns the root collection for a owner (user id) and a partitionId (customer id).  If there is none yet in the database, this method is responsible for creating the collection.
	 * 
	 * @param owner			the owner (owner of collection) needed to find the root collection
	 * @param partitionId	the partitionId (customer id) needed to find the root collection
	 * @param path			The path to the collection
	 * 
	 * @return				the root collection for a user				
	 * @throws InvalidCollectionPathException 
	 */
	CatalogCollection getCollectionByPath(
			String owner, 
			String partitionId,
			String path) throws CollectionNotFoundException, InvalidCollectionPathException;
	
	CatalogCollection createCollectionAtPath(
			String owner,
			String partitionId, 
			String path,
			Set<Long> permissionIds) throws CollectionNotFoundException, CollectionModificationException, InvalidCollectionPathException;
		
	
	/**
	 * This method returns the root collection for a partitionId (customer id).  If there is none yet in the database, this method is responsible for creating the collection.
	 * 
	 * @param partitionId	the partitionId (customer id) needed to find the root collection
	 * 
	 * @return				all colelctions at the root
	 */
	List<CatalogCollection> getRootCollectionForPartition(
			String partitionId); 
	
	/**
	 * This method returns the root collection for a owner (user id), partitionId (customer id) and application.  If there is none yet in the database, this method is responsible for creating the collection.
	 * 
	 * @param owner			the owner (owner of collection) needed to find the root collection
	 * @param partitionId	the partitionId (customer id) needed to find the root collection
	 * @param application	the application name needed to find the root collection
	 * 
	 * @return				the root application collection for a partition id and application
	 */
	CatalogCollection getRootApplicationCollectionForUser(
			String owner,
			String partitionId, 
			String application); // Main method for supporting applications
	
	/**
	 * This method returns the root "shared with me" collection for a owner (user id), partitionId (customer id) and application.  If there is none yet in the database, this method is responsible for creating the collection.
	 * 
	 * @param owner			the owner (owner of collection) needed to find the collection
	 * @param partitionId	the partitionId (customer id) needed to find the collection
	 * @param application	the application name needed to find the collection
	 * 
	 * @return				the root application share collection for a partition id and application
	 */
	CatalogCollection getApplicationShareCollectionForUser(
			String owner,
			String partitionId,
			String application); // Main method for supporting applications
	
	/**
	 * This method returns the a collection based on the collectionUniqueId.
	 * 
	 * @param owner			the owner (owner of collection) needed to find the collection
	 * @param partitionId	the partitionId (customer id) needed to find the collection
	 * @param uniqueId		the collection unique id needed to find the collection
	 * 
	 * @throws CollectionNotFoundException
	 * 
	 * @return				the collection for a given unique id
	 */
	CatalogCollection getCollection(
			String owner, 
			String partitionId, 
			String uniqueId) 
				throws CollectionNotFoundException;
	
	/**
	 * This method adds a collection to an existing collection.  Nulls are ignore, only filled out sets and maps will override current values.
	 * 
	 * @param owner								the owner (owner of collection) needed to find the collection
	 * @param partitionId						the partitionId (customer id) needed to find the collection
	 * @param collectionName					the name of the new collection
	 * @param parentCollectionUniqueId			the unique id of the parent collection to add the new collection
	 * @param sharePermissionIds				the set of permissions to add to the new collection
	 * @param attributes						the set of attributes to be added to the new collection
	 * 
	 * @return									the newly created collection
	 * 
	 * @throws CollectionNotFoundException		thrown when the parent collection is not found
	 * @throws CollectionModificationException	thrown when the parent collection can not be modified - so the new collection can not be added
	 * @throws InvalidCatalogNameException 
	 * @throws ObjectLockedException			thrown when the parent collection is locked
	 */
	CatalogCollection addCollection(
			String owner, 
			String partitionId, 
			String collectionName, 
			String parentCollectionUniqueId, 
			Set<Long> sharePermissionIds, 
			Map<String, String> attributes) 
				throws CollectionNotFoundException, 
					   CollectionModificationException, InvalidCatalogNameException;
	
	/**
	 * This method clones a collection, in the same location that the current collection exists.  The method names the cloned collection with (Copy) at the end of the name.
	 * 
	 * @param owner								the owner (owner of collection) needed to find the collection
	 * @param partitionId						the partitionId (customer id) needed to find the collection
	 * @param collectionToCloneUniqueId			the unique id of the collection to be cloned
	 * 
	 * @return									the newly cloned collection
	 * 
	 * @throws CollectionNotFoundException		thrown if the collection is not found
	 * @throws CollectionModificationException  thrown when collection does not have the clone permissions
	 * @throws InvalidCatalogNameException 
	 */
	CatalogCollection cloneCollection(
			String owner, 
			String partitionId, 
			String collectionToCloneUniqueId) 
				throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException;
	
	/**
	 * This method modifies a collection.  The method can move the collection, rename the collection, set new permissions, and change the attributes 
	 * 
	 * @param ownerId							the owner (owner of collection) needed to find the collection
	 * @param partitionId						the partitionId (customer id) needed to find the collection
	 * @param collectionName					the new collection name, ignored if null or empty
	 * @param collectionUniqueId				the unique id of the collection to modify
	 * @param parentCollectionUniqueId			the new parent unique id, ignored if null or empty
	 * @param sharePermissionIds				the set of permissions to add to the modified collection
	 * @param attributes						the map of attributes to be added to the modified collection
	 * 
	 * @return									the modified collection
	 * 
	 * @throws CollectionNotFoundException		thrown when either the parent collection is not found, or the collection to modify is not found
	 * @throws CollectionModificationException	thrown when the parent collection or the collection to be modified can not be modified
	 * @throws InvalidCatalogNameException 
	 */
	CatalogCollection modifyCollection(
			String ownerId, 
			String partitionId, 
			String collectionUniqueId,
			String collectionName, 
			String parentCollectionUniqueId, 
			Set<Long> sharePermissionIds, 
			Map<String, String> attributes) 
				throws CollectionNotFoundException, 
					   CollectionModificationException, InvalidCatalogNameException;
	
	
	/**
	 * This method shared a collection with a set of owner (owner of collection) and gives them a set of permissions for the shared collection.  The method will first try to validate that all the objects can be shared, as well as have the minimum set of permissions
	 * 
	 * @param ownerId							the owner (owner of collection) needed to find the collection
	 * @param partitionId						the partitionId (customer id) needed to find the collection
	 * @param collectionToShareUniqueId			the collection to be shared
	 * @param userAndPartitionsToShareWith		a map of users and partition id's (customer id's) to share the collection with
	 * @param sharePermissionIds				the set of permissions to add to the newly shared collection
	 * 
	 * @throws CollectionNotFoundException		thrown when either the parent collection is not found, or the collection to modify is not found
	 * @throws CollectionSharingException		thrown when the collection can not be shared, or one of its objects can not be shared or if the collection or one of the objects does not have the minimum sharing permissions
	 * @throws InvalidCatalogNameException 
	 */
	void shareCollection(
			String ownerId,
			String partitionId,
			String application,
			String collectionToShareUniqueId,
		    Map<String, String> userAndPartitionsToShareWith,
		    Set<Long> sharePermissionIds) 
				throws  CollectionNotFoundException, CollectionSharingException, InvalidCatalogNameException;
	
	/**
	 * This method shared a collection with a set of owner (owner of collection) and gives them a set of permissions for the shared collection.  The method will first try to validate that all the objects can be shared, as well as have the minimum set of permissions
	 * 
	 * @param ownerId							the owner (owner of collection) n	eeded to find the collection
	 * @param partitionId						the partitionId (customer id) needed to find the collection
	 * @param collectionToShareUniqueId			the collection to be shared
	 * @param collectionToShareIntToUniqueId	the collection to add the shared collection to
	 * @param userAndPartitionsToShareWith		a map of users and partition id's (customer id's) to share the collection with
	 * @param sharePermissionIds				the set of permissions to add to the newly shared collection
	 * 
	 * @throws CollectionNotFoundException		thrown when either the parent collection is not found, or the collection to modify is not found
	 * @throws CollectionSharingException		thrown when the collection can not be shared, or one of its objects can not be shared or if the collection or one of the objects does not have the minimum sharing permissions
	 * @throws InvalidCatalogNameException 
	 * @Throws CollectionModificationException
	 */
	void shareCollectionIntoCollection(
			String ownerId,
			String partitionId,
			String collectionToShareUniqueId,
			String collectionToShareIntToUniqueId,
		    Set<Long> sharePermissionIds) 
				throws  CollectionNotFoundException, CollectionSharingException, CollectionModificationException, InvalidCatalogNameException;
	
	/**
	 * Deletes a collection by unique id.  This method will also delete all nested collections and references within this collection.
	 * 
	 * @param owner							the owner/user id of the person requesting the operation
	 * @param partitionId					the partitionId where the collection lives
	 * @param collectionUniqueId			the collection unique id to delete
	 * @throws CollectionNotFoundException 	thrown when collecion is not found for the given uniqueId
	 * 
	 */
	void deleteCollection(String owner, String partitionId, String collectionUniqueId) throws CollectionNotFoundException;
	
	/**
	 * Deletes a collection by unique id.  	This method will also delete all nested collections and references within this collection.
	 * 
	 * @param owner							the owner/user id of the person requesting the operation
	 * @param collectionUniqueId			the collection unique id to delete
	 * @param deleteCreatedShares			flag used to delete all shares created from this collection
	 * @throws CollectionNotFoundException 	thrown when collecion is not found for the given uniqueId
	 * 
	 */
	void deleteCollection(String owner, String partitionId, String collectionUniqueId, boolean deleteCreatedShares) throws CollectionNotFoundException;
	
	//References
	/**
	 * Finds a reference by unique id
	 * 
	 * @param owner							the owner (owner of collection) of the reference
	 * @param partitionId					the partition id (customer id) of the reference
	 * @param referenceUniqueId				the reference unique id to find
	 * 
	 * @return								the reference by unique id
	 * 
	 * @throws ReferenceNotFoundException	thrown if the reference is not found				
	 */
	Reference getReference(
			String owner, 
			String partitionId, 
			String referenceUniqueId) 
				throws ReferenceNotFoundException;
	
	/**
	 * Finds a reference by path
	 * 
	 * @param owner							the owner (owner of collection) of the reference
	 * @param partitionId					the partition id (customer id) of the reference
	 * @param referenceUniqueId				the reference unique id to find
	 * 
	 * @return								the reference by unique id
	 * 
	 * @throws ReferenceNotFoundException	thrown if the reference is not found				
	 * @throws InvalidReferencePathException 
	 */
	Reference getReferenceByPath(
			String owner, 
			String partitionId, 
			String path,
			Boolean createIfNotExist,
			List<Long> permissions) 
				throws ReferenceNotFoundException, InvalidReferencePathException;
	
	/**
	 * This method clones a given reference.  It assumes that the validity check on whether or not it can be cloned has already happened.
	 * 
	 * @param owner										the owner (owner of collection) needed to find the collection
	 * @param partitionId								the partitionId (customer id) needed to find the collection
	 * @param referenceUniqueId							the reference to be shared
	 * @param sharePermissionIds						the set of permissions to add to the newly cloned reference
	 * @param collectionToPutClonedReferenceUniqueId	the collection that the reference will be placed in
	 * 
	 * @return											the cloned reference
	 * 
	 * @throws ReferenceNotFoundException				thrown when reference to clone is not found
	 * @throws ReferenceModificationException   		thrown when the reference can not be cloned 
	 * @throws CollectionModificationException  		thrown when the collection can not be modified (ie have the collection added to it)
	 * @throws CollectionNotFoundException 				thrown when the collection can not be found 
	 * @throws InvalidCatalogNameException 
	 */
	Reference cloneRefernence(
			String owner, 
			String partitionId, 
			String referenceUniqueId, 
			Set<Long> sharePermissionIds,
			String collectionToPutClonedReferenceUniqueId) 
				throws ReferenceNotFoundException, CollectionModificationException, ReferenceModificationException, CollectionNotFoundException, InvalidCatalogNameException;
	
	/**
	 * This method modifies a reference.  The method can move the reference, rename the reference, set new permissions, and change the attributes.  This method can not change the document that the reference points to.
	 * 
	 * @param owner								the owner (owner of collection) needed to find the collection
	 * @param partitionId						the partitionId (customer id) needed to find the collection
	 * @param referenceUniqueId					the new collection name, ignored if null or empty
	 * @param referenceName						the unique id of the collection to modify
	 * @param sharePermissionIds				the set of permissions to add to the modified collection
	 * @param attributes						the map of attributes to be added to the modified collection
	 * 
	 * @return									the modified reference
	 * 
	 * @throws ReferenceNotFoundException		thrown when the reference can not be found
	 * @throws ReferenceModificationException	thrown when the reference can not be modified
	 * @throws InvalidCatalogNameException 
	 */
	Reference modifyReference(
			String owner, 
			String partitionId, 
			String referenceUniqueId,
			String referenceName,
			Set<Long> sharePermissionIds, 
			Map<String, String> attributes) 
				throws CollectionNotFoundException, ReferenceModificationException, ReferenceNotFoundException, InvalidCatalogNameException;
	
	/**
	 * This method modifies a reference.  The method can move the reference, rename the reference, set new permissions, and change the attributes.  This method can not change the document that the reference points to.
	 * 
	 * @param owner								the owner (owner of collection) needed to find the collection
	 * @param partitionId						the partitionId (customer id) needed to find the collection
	 * @param referenceUniqueId					the new collection name, ignored if null or empty
	 * @param newParentCollectionUniqueId		the new parent unique id, ignored if null or empty
	 * 
	 * @return									the modified reference
	 * 
	 * @throws ReferenceNotFoundException		thrown when the reference can not be found
	 * @throws ReferenceModificationException	thrown when the reference can not be modified
	 * @throws InvalidCatalogNameException 
	 */
	Reference moveReference(
			String owner, 
			String partitionId, 
			String referenceUniqueId,
			String newParentCollectionUniqueId) 
				throws CollectionNotFoundException, ReferenceModificationException, ReferenceNotFoundException, CollectionModificationException, InvalidCatalogNameException;
	
	/**
	 * This method shares a reference with a set of users and gives them a set of permissions for the shared reference.  
	 * This method will add the shared reference to the user's "shared with me" collection, under the root application collection for the user.
	 * 
	 * @param owner								the owner (owner of collection) needed to find the collection
	 * @param partitionId						the partitionId (customer id) needed to find the collection
	 * @param referenceUniqueId					the reference to be shared
	 * @param userAndPartitionsToShareWith		a map of users and partition id's (customer id's) to share the reference with
	 * @param sharePermissionIds				the set of permissions to add to the newly shared collection
	 * 
	 * @throws ReferenceNotFoundException		thrown when reference to share is not found
	 * @throws ReferenceSharingException		thrown when reference does not have the sharing permissions, or does not have the minimum permissions to share
	 * @throws InvalidCatalogNameException 
	 */
	void shareReference(
			String owner, 
			String partitionId, 
			String referenceUniqueId, 
			String application,
			Map<String, String> userAndPartitionsToShareWith, 
			Set<Long> sharePermissionIds) 
				throws ReferenceNotFoundException, ReferenceSharingException, InvalidCatalogNameException;
	
	/**
	 * This method shares a reference into a collection.
	 * This method will add the shared reference to the user's "shared with me" collection, under the root application collection for the user.
	 * 
	 * @param owner								the owner (owner of collection) needed to find the collection
	 * @param partitionId						the partitionId (customer id) needed to find the collection
	 * @param referenceUniqueId					the reference to be shared
	 * @param collectionUniqueId				the collection unique id to share a reference into
	 * @param sharePermissionIds				the set of permissions to add to the newly shared collection
	 * 
	 * @throws ReferenceNotFoundException		thrown when reference to share is not found
	 * @throws ReferenceSharingException		thrown when reference does not have the sharing permissions, or does not have the minimum permissions to share
	 * @throws InvalidCatalogNameException 
	 * @throws CollectionNotFoundException
	 * @throws CollectionModificationException 
	 */
	Reference shareReferenceIntoCollection(
			String owner, 
			String partitionId, 
			String referenceUniqueId,
			String collectionUniqueId,
			Set<Long> sharePermissionIds) 
				throws ReferenceNotFoundException, ReferenceSharingException, InvalidCatalogNameException, CollectionNotFoundException, CollectionModificationException;
	
	/**
	 * This method deletes a reference from a given collection
	 * 
	 * @param owner								the owner (owner of collection) needed to find the collection
	 * @param partitionId						the partitionId (customer id) needed to find the collection
	 * @param referenceUniqueId					the unique id of the reference that is to be deleted
	 * @param deleteSharedTo					flag to let the catalog service know if all shared to references should be deleted as well
	 * 
	 * @return									the modified collection
	 * 
	 * @throws CollectionModificationException	thrown when the collection to be modified can not be modified
	 * @throws ReferenceNotFoundException		thrown if the given reference unique id is not found in the system
	 */
	CatalogCollection deleteReferenceFromCollection(
			String owner, 
			String partitionId, 
			String referenceUniqueId,
			boolean deleteSharedTo) 
				throws CollectionModificationException, ReferenceNotFoundException;
	
	//Documents
	/**
	 * This method will add a document and return a reference.  This method will add it to the given collection.
	 * 
	 * @param owner								the owner (owner of collection) needed to add a document
	 * @param partitionId						the partitionId (customer id) needed to add to a document
	 * @param documentName						the name of the document
	 * @param collectionUniqueId				the collection to add the reference
	 * @param payload							the byte array to store as the document payload
	 * @param sharePermissionIds				the share permissions to add to the newly created reference
	 * @param attributes						the attributes to add to a given document
	 * 
	 * @return									the created reference for the new document
	 * 
	 * @throws CollectionNotFoundException		thrown when reference to clone is not found
	 * @throws CollectionModificationException 
	 */
	Reference addDocument(
			String owner, 
			String partitionId,
			String documentName,
			String collectionUniqueId, 
			byte[] payload, 
			Set<Long> sharePermissionIds, 
			Map<String, String> attributes) 
				throws CollectionNotFoundException, CollectionModificationException;
	
	/**
	 * This method will modify a document by the reference id.  This method will also valid if the given document can be referenced by the reference.
	 * If null values are sent in they are ignored, to remove attributes, send in a new (empty) attribute map.
	 * 
	 * @param owner								the owner (owner of collection) needed to add a document
	 * @param partitionId						the partitionId (customer id) needed to add to a document
	 * @param referenceUniqueId					the reference to use to modify the document
	 * @param documentName						the new name of the document, this will be ignore if it is null or empty
	 * @param documentNotes						the new notes for given document, this will be ignore if it is null or empty
	 * @param documentDescription				the new document description, this will be ignore if it is null or empty
	 * @param locked							true/false property to set if the document should be locked, this will also lock the reference
	 * 
	 * @return									the reference of the modified document 
	 * 
	 * @throws ReferenceNotFoundException		throws an exception when the reference is not found
	 * @throws ReferenceModificationException	throws an exception when the reference does not have enough permissions to allow modification
	 * @throws ObjectLockedException			throws an exception if the reference or document is locked
	 * @throws InvalidCatalogNameException 
	 */
	Reference modifyDocumentByReferenceId(
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
				throws ReferenceNotFoundException, ReferenceModificationException, ObjectLockedException, InvalidCatalogNameException;
	
	/**
	 * This method clones a document.  This method will validate if the collection can be modified and a new cloned reference can be added.
	 * 
	 * @param owner								the owner (owner of collection) needed to add a document
	 * @param partitionId						the partitionId (customer id) needed to add to a document
	 * @param referenceUniqueId					the reference unique id to use to clone the document
	 * @param newParentCollectionUniqueId		the collection that will hold the new reference - if null or empty the parent will be the collection that the reference currently lives
	 * 
	 * @return									the cloned reference
	 *  
	 * @throws ReferenceNotFoundException		thrown when a reference can not be found
	 * @throws CollectionNotFoundException		thrown when a collection can not be found
	 * @throws CollectionSharingException		throws an exception if the parent collection can not be modified
	 * @throws CollectionModificationException  thrown when the new reference can not be added to the collection
	 * @throws ReferenceModificationException   thrown when the collection can not be cloned
	 * @throws InvalidCatalogNameException 
	 */
	Reference cloneDocument(
			String owner, 
			String partitionId, 
			String referenceUniqueId,
			String newParentCollectionUniqueId) 
				throws ReferenceNotFoundException, CollectionNotFoundException, CollectionSharingException, CollectionModificationException, ReferenceModificationException, InvalidCatalogNameException;
	
	/**
	 * This method will delete the document.   Need to decide if the method needs to also delete the references...
	 * 
	 * @param owner								the owner (owner of collection) needed to add a document
	 * @param partitionId						the partitionId (customer id) needed to add to a document
	 * @param documentUniqueId					the document unique id to delete
	 * 
	 */
	void deleteDocument(
			String owner, 
			String partitionId, 
			String documentUniqueId);
}