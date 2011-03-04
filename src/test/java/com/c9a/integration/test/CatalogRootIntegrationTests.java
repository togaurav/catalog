package com.c9a.integration.test;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.entities.Permission;
import com.c9a.catalog.entities.Reference;
import com.c9a.catalog.exception.CollectionModificationException;
import com.c9a.catalog.exception.CollectionNotFoundException;
import com.c9a.catalog.exception.CollectionSharingException;
import com.c9a.catalog.exception.InvalidCatalogNameException;
import com.c9a.catalog.exception.InvalidCollectionPathException;
import com.c9a.catalog.exception.InvalidReferencePathException;
import com.c9a.catalog.exception.ReferenceModificationException;
import com.c9a.catalog.exception.ReferenceNotFoundException;
import com.c9a.catalog.exception.ReferenceSharingException;

@RunWith(SpringJUnit4ClassRunner.class)
@TransactionConfiguration
public class CatalogRootIntegrationTests extends AbstractIntegrationTest {
	
	private static final String GROUP_SERVICE_NAME = "Groups";
	private String partitionId = "000345Eds78QAW";
	private String systemOwner = CatalogCollection.SYSTEM_OWNER;
	private int customers = 1000;
	
	@Test
	@Transactional
	@Rollback(value=false)
	public void createRootCollection() throws InvalidCollectionPathException, CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException, ReferenceNotFoundException, ReferenceSharingException, ReferenceModificationException, CollectionSharingException, InvalidReferencePathException{
		Long startTime = System.currentTimeMillis();
		System.out.println("Starting provision test");
		provisionNewCustomer(partitionId);
		System.out.println("Provision Time for " + customers + " customers : " + (System.currentTimeMillis() - startTime) + " ms");
//		CatalogCollection allUsersGroup = catalogService.getCollectionByPath(systemOwner, partitionId, "/groups/all_users/");
		
//		String applicationName = "SomeApp";
//		CatalogCollection collectionFromUser1 = catalogService.getRootApplicationCollectionForUser("user1@company.com", partitionId, applicationName);
//		Set<Long> sharePermissionIds = new HashSet<Long>();
//		sharePermissionIds.add(Permission.SHARE_PERMISSION_ID);
//		sharePermissionIds.add(Permission.ADD_PERMISSION_ID);
////		
//		CatalogCollection collectionToShare = catalogService.getCollectionByPath("user1@company.com", partitionId, "/users/user1@company.com/someapp/my_cool_stuff/");
////			catalogService.addCollection("user1@company.com", partitionId, "My Cool Stuff", collectionFromUser1.getUniqueId(), sharePermissionIds , null);
//		Reference referenceToShare = catalogService.addDocument("user1@company.com", partitionId, "NewRefToShare", collectionFromUser1.getUniqueId(), "NewRefPayload".getBytes(), sharePermissionIds, null);
//		CatalogCollection allUsersGroup = catalogService.getCollectionByPath(systemOwner, partitionId, "/groups/all_users/");
//		//		
//		startTime = System.currentTimeMillis();
//		shareCollectionWithGroupFromApplication(allUsersGroup , collectionToShare, applicationName);
//		System.out.println("shareCollectionWithGroupFromApplication : " + (System.currentTimeMillis() - startTime) + " ms");
//		startTime = System.currentTimeMillis();
//		shareReferenceWithGroupFromApplication(allUsersGroup, referenceToShare, applicationName);
//		System.out.println("shareReferenceWithGroupFromApplication : " + (System.currentTimeMillis() - startTime) + " ms");
////		
//		CatalogCollection shareableApplications = catalogService.getCollectionByPath(systemOwner, partitionId, allUsersGroup.getPath() + "sharable/applications/");
//		CatalogCollection shareableApp = null;
//		try{
//			shareableApp = catalogService.getCollectionByPath(systemOwner, partitionId, shareableApplications.getPath()+applicationName+"/");
//		} catch(CollectionNotFoundException e){
//			shareableApp = catalogService.addCollection(systemOwner, partitionId, applicationName, shareableApplications.getUniqueId(), sharePermissionIds , null);
//		}
////		
//		startTime = System.currentTimeMillis();
//		catalogService.addDocument(systemOwner, partitionId, "NewRefInGroup", shareableApp.getUniqueId(), "payload".getBytes(), null, null);
//		System.out.println("addDocument : " + (System.currentTimeMillis() - startTime) + " ms");
////		
//		CatalogCollection user2Groups = catalogService.getCollectionByPath("user2@company.com", partitionId, "/users/user2@company.com/groups/all_users/sharable/applications/someapp/");
//		assertEquals(user2Groups.getNestedCollections().size(), 1);
	}
	
	private void shareReferenceWithGroupFromApplication(
			CatalogCollection group, Reference referenceToShare,
			String applicationName) throws InvalidCollectionPathException, CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException, ReferenceNotFoundException, ReferenceSharingException {
		CatalogCollection shareableApplications = catalogService.getCollectionByPath(systemOwner, partitionId, group.getPath() + "sharable/applications/");
		Set<Long> sharePermissionIds = new HashSet<Long>();
		sharePermissionIds.add(Permission.SHARE_PERMISSION_ID);
		sharePermissionIds.add(Permission.ADD_PERMISSION_ID);
		CatalogCollection shareableApp = null;
		try{
			shareableApp = catalogService.getCollectionByPath(systemOwner, partitionId, shareableApplications.getPath()+applicationName+"/");
		} catch(CollectionNotFoundException e){
			shareableApp = catalogService.addCollection(systemOwner, partitionId, applicationName, shareableApplications.getUniqueId(), sharePermissionIds , null);
		}
		catalogService.shareReferenceIntoCollection(systemOwner, partitionId, referenceToShare.getUniqueId(), shareableApp.getUniqueId(), null);
		
	}

	private void shareCollectionWithGroupFromApplication(CatalogCollection group, CatalogCollection collectionFromUser, String applicationName) throws InvalidCollectionPathException, CollectionNotFoundException, CollectionSharingException, CollectionModificationException, InvalidCatalogNameException {
		CatalogCollection shareableApplications = catalogService.getCollectionByPath(systemOwner, partitionId, group.getPath() + "sharable/applications/");
		Set<Long> sharePermissionIds = new HashSet<Long>();
		sharePermissionIds.add(Permission.SHARE_PERMISSION_ID);
		sharePermissionIds.add(Permission.ADD_PERMISSION_ID);
		CatalogCollection shareableApp = null;
		try{
			shareableApp = catalogService.getCollectionByPath(systemOwner, partitionId, shareableApplications.getPath()+applicationName+"/");
		} catch(CollectionNotFoundException e){
			shareableApp = catalogService.addCollection(systemOwner, partitionId, applicationName, shareableApplications.getUniqueId(), sharePermissionIds , null);
		}
		catalogService.shareCollectionIntoCollection(systemOwner, partitionId, collectionFromUser.getUniqueId(), shareableApp.getUniqueId(), null);
	}

	private CatalogCollection createNewGroup(String newGroupName) throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException, InvalidCollectionPathException {
		// First Verify that the group does not already exist..
		// Next add all base child collections with appropriate permissions
		
		CatalogCollection groupBase = getGroupBase();
		
		String entities = "Entities";
		String sharable = "Sharable";
		String applications = "Applications";
		String childGroups = "ChildGroups";
		
		Set<Long> permissions = new HashSet<Long>();
		permissions .add(Permission.SHARE_PERMISSION_ID);
		permissions.add(Permission.EDIT_PERMISSION_ID);
		permissions.add(Permission.ADD_PERMISSION_ID);
		
		//Collection for all the user references
		CatalogCollection newGroupCollection = catalogService.addCollection(systemOwner, partitionId, newGroupName, groupBase.getUniqueId(), permissions, null);
		catalogService.addCollection(systemOwner, partitionId, entities, newGroupCollection.getUniqueId(), permissions, null);
		
		//Collection for all things shared to the users
		CatalogCollection sharableForNewGroup = catalogService.addCollection(systemOwner, partitionId, sharable, newGroupCollection.getUniqueId(), permissions, null);
		catalogService.addCollection(systemOwner, partitionId, applications, sharableForNewGroup.getUniqueId(), permissions, null);
		catalogService.addCollection(systemOwner, partitionId, childGroups, sharableForNewGroup.getUniqueId(), permissions, null);
		
		return newGroupCollection;
	}

	private CatalogCollection getGroupBase() throws InvalidCollectionPathException, CollectionNotFoundException, CollectionModificationException {
		
		Set<Long> permissions = new HashSet<Long>();
		permissions.add(Permission.ADD_PERMISSION_ID);
		CatalogCollection groupBase = null;
		try{
			groupBase = catalogService.getCollectionByPath(systemOwner, partitionId, "/Groups/");
		} catch(CollectionNotFoundException e){
			groupBase = catalogService.createCollectionAtPath(systemOwner, partitionId, "/Groups/", permissions);
		}
		return groupBase;
	}

	private byte[] createUserBytes(String key) {
		return ("UserId : " + key).getBytes();
	}
	
	
	private void provisionNewCustomer(String partitionId) throws InvalidCollectionPathException, CollectionNotFoundException, CollectionModificationException, ReferenceNotFoundException, ReferenceSharingException, InvalidCatalogNameException, CollectionSharingException, ReferenceModificationException, InvalidReferencePathException{
		//This would be a lookup for the user info
		CatalogCollection allUsersGroup = createNewGroup("All Users");
		
		// Permissions for user ref to user doc, allows it to be shared into the group
		Set<Long> sharePermissionIds = new HashSet<Long>();
		sharePermissionIds.add(Permission.SHARE_PERMISSION_ID);		
		
		String user;
		String com = "@company.com";
		for(int i = 0; i < customers; i++){
			user = "user"+i+com;
			CatalogCollection userRoot = catalogService.getRootCollectionForUser(user, partitionId);
			// TODO : Create the correct payload of a user
			Reference userDocRef = catalogService.addDocument(user, partitionId, user, userRoot.getUniqueId(), createUserBytes(user), sharePermissionIds, null);
//			catalogDao.flush();
//			addUserToGroup(user, allUsersGroup, userDocRef);
		}
	}
	
	private void addUserToGroup(String user, CatalogCollection allUsersGroup, Reference userDocRef) throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException, InvalidCollectionPathException, CollectionSharingException, InvalidReferencePathException, ReferenceNotFoundException, ReferenceSharingException {
		CatalogCollection shareableForAllUsersGroup = catalogService.getCollectionByPath(systemOwner, partitionId, allUsersGroup.getPath() + "sharable/");
		CatalogCollection entititesForAllUsersGroup = catalogService.getCollectionByPath(systemOwner, partitionId, allUsersGroup.getPath() + "entities/");
		//Create the user and group collections
		CatalogCollection userGroupRoot = catalogService.getRootApplicationCollectionForUser(user, partitionId, GROUP_SERVICE_NAME);
		
		Set<Long> addPermissionIds = new HashSet<Long>();
		addPermissionIds.add(Permission.ADD_PERMISSION_ID);
		Set<Long> sharePermissionIds = new HashSet<Long>();
		sharePermissionIds.add(Permission.SHARE_PERMISSION_ID);
		
		catalogService.shareReferenceIntoCollection(systemOwner, partitionId, userDocRef.getUniqueId(), entititesForAllUsersGroup.getUniqueId(), sharePermissionIds);
		CatalogCollection allUsersCollection = catalogService.addCollection(systemOwner, partitionId, "All Users", userGroupRoot.getUniqueId(), addPermissionIds, null);
		catalogService.shareCollectionIntoCollection(user, partitionId, shareableForAllUsersGroup.getUniqueId(), allUsersCollection.getUniqueId(), null);
	}
}
