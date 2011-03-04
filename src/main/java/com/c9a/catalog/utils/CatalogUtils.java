package com.c9a.catalog.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.entities.CatalogObject;
import com.c9a.catalog.entities.CollectionAttribute;
import com.c9a.catalog.entities.Document;
import com.c9a.catalog.entities.DocumentAttribute;
import com.c9a.catalog.entities.ExtendedAttribute;
import com.c9a.catalog.entities.Permission;
import com.c9a.catalog.entities.Reference;
import com.c9a.catalog.entities.ReferenceAttribute;
import com.c9a.catalog.exception.CollectionSharingException;
import com.c9a.catalog.exception.InvalidCatalogNameException;

public class CatalogUtils {
	
	private static final String CLONED = "Copy of ";
	
	public static String clonedName(String name){
		return CLONED + name;
	}
	
	/**
	 * This method will return true/false depending on if the set of permissions passed in have the EDIT_PERMISSION permission
	 * 
	 * @param permissions  	the permission set to find the needed permission for edit/modify
	 * 
	 * @return canModify 	the boolean value based on if the permissions have the edit permissions contained within them 
	 * 
	 * @see Permission here for possible permissions
	 */
	public static boolean canModify(Set<Permission> permissions) {
		return hasPermission(permissions, Permission.EDIT_PERMISSION_ID);
	}
	
	/**
	 * This method will return true/false depending on if the set of permissions passed in have the SHARE_PERMISSION permission
	 * 
	 * @param permissions  	the permission set to find the needed permission for share
	 * 
	 * @return canShare		the boolean value based on if the permissions have the share permissions contained within them 
	 * 
	 * @see Permission here for possible permissions
	 */
	public static boolean canShare(Set<Permission> permissions) {
		return hasPermission(permissions, Permission.SHARE_PERMISSION_ID);
	}
	
	/**
	 * This method will return true/false depending on if the set of permissions passed in have the CLONE_PERMISSION permission
	 * 
	 * @param permissions  	the permission set to find the needed permission for share
	 * 
	 * @return canModify 	the boolean value based on if the permissions have the share permissions contained within them 
	 * 
	 * @see Permission here for possible permissions
	 */
	public static boolean canClone(Set<Permission> permissions) {
		return hasPermission(permissions, Permission.CLONE_PERMISSION_ID);
	}
	
	public static boolean canAdd(Set<Permission> permissions) {
		return hasPermission(permissions, Permission.ADD_PERMISSION_ID);
	}
	
	/**
	 * This method will return true/false depending on if the set of permissions passed in has the permission id that is also passed in
	 * 
	 * @param permissions  		the collection of available permissions
	 * @param permissionId		the permission id to look for in the collection of permissions
	 * 
	 * @return hasPermission 	the boolean value based on if the permissions has the supplied permission id 
	 * 
	 * @see Permission here for possible permissions
	 */
	private static boolean hasPermission(Set<Permission> permissions, Long permissionId) {
		for(Permission p : permissions){
			if(p.getId().equals(permissionId)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * This will make sure that all objects in the collection can be shared, as well as all the objects can be shared with the given permissions.
	 * If there are object that can not be shared, they will be added to the CollectionSharingException, so they can be looked at by the calling method.
	 * 
	 * @param collectionToShare  			the collection that needs to be validated for sharing
	 * @param permissionsToShare 			the set of permissions to be given to the shared collection 
	 * 
	 * @throws CollectionSharingException 	the exception that will be thrown if the collection, or containing object can not be shared.
	 * 
	 * @see CollectionSharingException
	 */
	public static void validateCollectionCanBeShared(CatalogCollection collectionToShare, Set<Permission> permissionsToShare) throws CollectionSharingException {
		Map<CatalogCollection, String> collectionsThatCanNotBeShared = new HashMap<CatalogCollection, String>();
		Map<Reference, String> referencesThatCanNotBeShared = new HashMap<Reference, String>();
		
		validateCollectionAndReferences(collectionToShare, permissionsToShare, collectionsThatCanNotBeShared, referencesThatCanNotBeShared);
		
		if(collectionsThatCanNotBeShared.size() > 0 || referencesThatCanNotBeShared.size() > 0){
			throw new CollectionSharingException(collectionsThatCanNotBeShared, referencesThatCanNotBeShared);
		}
	}
	
	/**
	 * Helper method to recursively loop over the collection(s) and reference(s), and make sure that the objects can be shared and shared with the given permissions.
	 * 
	 * @param collectionToShare  				the collection that needs to be validated for sharing
	 * @param permissionsToShare 				the set of permissions to be given to the shared collection 
	 * @param collectionsThatCanNotBeShared 	the map of collections that can not be shared, with an error message saying why it can not be shared.
	 * @param referencesThatCanNotBeShared 	the map of references that can not be shared, with an error message saying why it can not be shared.
	 * 
	 */
	private static void validateCollectionAndReferences(CatalogCollection collectionToShare, Set<Permission> permissionsToShare, Map<CatalogCollection, String> collectionsThatCanNotBeShared, Map<Reference, String> referencesThatCanNotBeShared){
		if(collectionToShare.getNestedCollections() != null && collectionToShare.getNestedCollections().size() > 0){
			for(CatalogCollection cc : collectionToShare.getNestedCollections()){
				validateCollectionAndReferences(cc, permissionsToShare, collectionsThatCanNotBeShared, referencesThatCanNotBeShared);
			}
		}
		
		if(!CatalogUtils.canShare(collectionToShare.getPermissions())){
			collectionsThatCanNotBeShared.put(collectionToShare, Permission.CAN_NOT_SHARE_COLLECTION_MESSAGE);
		} else if (permissionsToShare!=null && !CatalogUtils.hasAtLeastPermissions(collectionToShare.getPermissions(), permissionsToShare)){
			collectionsThatCanNotBeShared.put(collectionToShare, Permission.CAN_NOT_SHARE_COLLECTION_NOT_ENOUGH_PERMISSION_MESSAGE);
		}
		
		if(collectionToShare.getReferences() !=null && collectionToShare.getReferences().size() > 0){
			for(Reference r : collectionToShare.getReferences()){
				if(!CatalogUtils.canShare(r.getPermissions())){
					referencesThatCanNotBeShared.put(r, Permission.CAN_NOT_SHARE_REFERENCE_MESSAGE);
				} else if (permissionsToShare!=null && !CatalogUtils.hasAtLeastPermissions(r.getPermissions(), permissionsToShare)){
					referencesThatCanNotBeShared.put(r, Permission.CAN_NOT_SHARE_REFERENCE_NOT_ENOUGH_PERMISSION_MESSAGE);
				}
			}
		}
	}

	/**
	 * Helper method to make sure the set of permissions have at least the needed permissions
	 * 
	 * @param permissions		 	the current set of permissions for a given object
	 * @param permissionsToShare 	the set of permissions that are trying to be used for the sharing of the object 
	 * 
	 * @return						true if the permissions set has the minimal
	 * 
	 */
	public static boolean hasAtLeastPermissions(Set<Permission> permissions, Set<Permission> permissionsToShare) {
		for(Permission p : permissionsToShare){
			if(!permissions.contains(p)){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * This method will traverse to the original share collection
	 * 
	 * @param collection		The collection that we are trying to see if there is a root shared from collection
	 * @return					The collection that is at the root of the sharing chain
	 */
	public static CatalogCollection getRootSharedCollection(CatalogCollection collection) {
		if(collection.getSharedFrom() == null){
			return collection;
		}
		return getRootSharedCollection(collection.getSharedFrom());
	}

	/**
	 * This method will determine if the collection is one that has been shared to or from
	 * @param collection	The collection to use when determining if its a shared collection
	 * @return true if a shared collection
	 */
	public static boolean isSharedCollection(CatalogCollection collection) {
		return collection.getSharedTo().size() > 0 || collection.getSharedFrom() != null;
	}
	
	/**
	 * This method will determine if the reference is one that has been shared to or from
	 * @param reference	The reference to use when determining if its a shared collection
	 * @return true if a shared reference
	 */
	public static boolean isSharedReference(Reference reference) {
		return reference.getSharedTo().size() > 0 || reference.getSharedFrom() != null;
	}

	/**
	 * This method will return the reference that started the sharing chain.
	 * @param reference  the reference to start from
	 * @return the root shared from reference
	 */
	public static Reference getRootSharedReference(Reference reference) {
		if(reference.getSharedFrom() == null){
			return reference;
		}
		return getRootSharedReference(reference.getSharedFrom());
	}
	
	public static Reference clone(Reference reference, CatalogCollection owningCollection, Set<Permission> permissionsForIds) throws InvalidCatalogNameException {
		return  clone(reference, owningCollection, permissionsForIds, null);
	}
	
	public static Reference clone(Reference reference, CatalogCollection owningCollection, Set<Permission> permissionsForIds, String name) throws InvalidCatalogNameException {
		if(owningCollection == null){
			owningCollection = reference.getParentCollection();
		}
		if(permissionsForIds == null){
			permissionsForIds = reference.getPermissions();
		}
		Reference clonedReference = new Reference(reference.getOwner(), reference.getPartitionId(), reference.getName(), reference.getDocument(), owningCollection, permissionsForIds);
		clonedReference.setName(name!=null ? name : CatalogUtils.clonedName(reference.getName()));
		clonedReference.setCreatedFrom(reference);
		for(ExtendedAttribute ea : reference.getAttributes()){
			clonedReference.addAttribute(new ReferenceAttribute(ea.getKey(), ea.getValue()));
		}
		return clonedReference;
	}
	
	public static Document clone(Document documentToClone) throws InvalidCatalogNameException {
		Document clonedDocument = new Document(documentToClone);
		clonedDocument.setName(CatalogUtils.clonedName(documentToClone.getName()));
		
		for(ExtendedAttribute ea : documentToClone.getAttributes()){
			clonedDocument.addAttribute(new DocumentAttribute(ea.getKey(), ea.getValue()));
		}
		return clonedDocument;
	}
	
	public static CatalogCollection clone(CatalogCollection collectionToClone,
			CatalogCollection parentCollection, boolean useClonedName) throws InvalidCatalogNameException {
		if (parentCollection == null) {
			parentCollection = collectionToClone.getParentCollection();
		}
		String newName = useClonedName ? clonedName(collectionToClone.getName()) : collectionToClone.getName();

		CatalogCollection clonedCollection = new CatalogCollection(
				collectionToClone.getOwner(),
				collectionToClone.getPartitionId(),
				newName,
				collectionToClone.getPermissions(), parentCollection);
		clonedCollection.setCreatedFrom(collectionToClone);

		for (CatalogCollection nestedCollection : collectionToClone.getNestedCollections()) {
			CatalogCollection collectionToAdd = CatalogUtils.clone(nestedCollection, clonedCollection, useClonedName);
			clonedCollection.getNestedCollections().add(collectionToAdd);
		}

		for (Reference ref : collectionToClone.getReferences()) {
			clonedCollection.getReferences().add(CatalogUtils.clone(ref, clonedCollection, ref.getPermissions(), CatalogUtils.clonedName(ref.getName())));
		}
		for(ExtendedAttribute ea : collectionToClone.getAttributes()){
			clonedCollection.addAttribute(new CollectionAttribute(ea.getKey(), ea.getValue()));
		}
		collectionToClone.getCreatedTo().add(clonedCollection);
		return clonedCollection;
	}
	
	public static CatalogCollection clone(CatalogCollection collectionToClone,
			CatalogCollection parentCollection) throws InvalidCatalogNameException {
		return clone(collectionToClone, parentCollection, true);
		
	}
	
	public static boolean isValidCollectionPath(String path) {
		if(!path.startsWith("/") || !path.endsWith("/")){
			return false;
		}
		StringTokenizer st = new StringTokenizer(path, "/");
		String cleanedPath = "";
		while(st.hasMoreTokens()){
			String token = st.nextToken();
			if(token.replaceAll(CatalogObject.unSupportedCharRegExForPath, "").toLowerCase().length() != token.length()){
				return false;
			}
			cleanedPath += "/" +token.replaceAll(CatalogObject.unSupportedCharRegExForPath, "").toLowerCase();
		}
		cleanedPath+="/";
		boolean validPath = cleanedPath.replaceAll(CatalogObject.unSupportedCharRegExForPathFull, "").toLowerCase().length() == cleanedPath.length();
		if(validPath){
			return true;
		}
		return false;
	}
	
	public static boolean isValidReferencePath(String path) {
		if(!path.startsWith("/") || path.endsWith("/")){
			return false;
		}
		StringTokenizer st = new StringTokenizer(path, "/");
		String cleanedPath = "";
		while(st.hasMoreTokens()){
			String token = st.nextToken();
			if(token.replaceAll(CatalogObject.unSupportedCharRegExForPath, "").toLowerCase().length() != token.length()){
				return false;
			}
			cleanedPath += "/" +token.replaceAll(CatalogObject.unSupportedCharRegExForPath, "").toLowerCase();
		}
		boolean validPath = cleanedPath.replaceAll(CatalogObject.unSupportedCharRegExForPathFull, "").toLowerCase().length() == cleanedPath.length();
		if(validPath){
			return true;
		}
		return false;
	}
	
	public static String sanitizePath(String path){
		return path.replaceAll(CatalogObject.unSupportedCharRegExForPathFull, "").toLowerCase();
	}
}