package com.c9a.integration.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.ConstraintViolationException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.entities.Document;
import com.c9a.catalog.entities.Permission;
import com.c9a.catalog.entities.Reference;
import com.c9a.catalog.exception.CollectionModificationException;
import com.c9a.catalog.exception.CollectionNotFoundException;
import com.c9a.catalog.exception.CollectionSharingException;
import com.c9a.catalog.exception.DocumentTypeNotFoundException;
import com.c9a.catalog.exception.InvalidCatalogNameException;
import com.c9a.catalog.exception.ObjectLockedException;
import com.c9a.catalog.exception.ReferenceModificationException;
import com.c9a.catalog.exception.ReferenceNotFoundException;
import com.c9a.catalog.exception.ReferenceSharingException;
import com.c9a.catalog.utils.CatalogUtils;

@RunWith(SpringJUnit4ClassRunner.class)
public class CatalogServiceIntegrationTest extends AbstractIntegrationTest {
	
	private static final Logger LOG = Logger.getLogger(CatalogServiceIntegrationTest.class.getName());
	
	@Test(expected=CollectionModificationException.class)
	public void addCollectionNotEnoughPermission() throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException{
		String owner = "CModException";
		String app = "Some_app";
		CatalogCollection appRootCollection = catalogService.getRootApplicationCollectionForUser(owner , partitionId, app);
		Set<Long> sharePermissionIds = new HashSet<Long>();
		sharePermissionIds.add(Permission.EDIT_PERMISSION_ID);
		CatalogCollection newCollectionCanNotAdd = catalogService.addCollection(owner, partitionId, "new Collecton can not add to", appRootCollection.getUniqueId(), sharePermissionIds, null);
		
		catalogService.addCollection(owner, partitionId, "Wont happen!", newCollectionCanNotAdd.getUniqueId(), sharePermissionIds, null);
	}
	
	@Test
	public void sharedCollectionDelete() throws CollectionNotFoundException, CollectionModificationException, CollectionSharingException, InvalidCatalogNameException{
		
		String owner1 = "OWNER1";
		String owner2 = "OWNER2";
		String application = "PA";
		
		CatalogCollection applicationOwner1RootCollection = catalogService.getRootApplicationCollectionForUser(owner1, partitionId, application);
		Set<Long> sharePermissionIds = new HashSet<Long>();
		sharePermissionIds.add(Permission.ADD_PERMISSION_ID);
		sharePermissionIds.add(Permission.EDIT_PERMISSION_ID);
		sharePermissionIds.add(Permission.SHARE_PERMISSION_ID);
		sharePermissionIds.add(Permission.CLONE_PERMISSION_ID);
		
		CatalogCollection cc = catalogService.addCollection(owner1, partitionId, "Owner 1 Collection", applicationOwner1RootCollection.getUniqueId(), sharePermissionIds , null);
		
		Map<String, String> userAndPartitionsToShareWith = new HashMap<String, String>();
		userAndPartitionsToShareWith.put(owner2, partitionId);
		
		catalogService.shareCollection(owner1, partitionId, application, cc.getUniqueId(), userAndPartitionsToShareWith , sharePermissionIds);
		catalogDao.flush();
		CatalogCollection applicationOwner2RootShareCollection = catalogService.getApplicationShareCollectionForUser(owner2, partitionId, application);
		
		assertEquals(applicationOwner2RootShareCollection.getNestedCollections().size(), 1);
		assertEquals(applicationOwner2RootShareCollection.getNestedCollections().iterator().next().getSharedFrom().getUniqueId(), cc.getUniqueId());
		
		catalogService.deleteCollection(cc.getOwner(), cc.getPartitionId(), cc.getUniqueId(), true);
		
		applicationOwner2RootShareCollection = catalogService.getApplicationShareCollectionForUser(owner2, partitionId, application);
		
		assertEquals(applicationOwner2RootShareCollection.getNestedCollections().size(), 0);
	}
	
	@Test(expected=ReferenceNotFoundException.class)
	public void deleteSharedReference() throws CollectionNotFoundException, DocumentTypeNotFoundException, ReferenceModificationException, ReferenceNotFoundException, ObjectLockedException, ReferenceSharingException, CollectionModificationException, InvalidCatalogNameException{
		
		String owner = "OWNER_deleteSharedReference";
		String applicationName = "APPLICATION_NAME_deleteSharedReference";
		
		CatalogCollection applicationCollection = catalogService.getRootApplicationCollectionForUser(owner, partitionId, applicationName);
		
		Set<Long> permissionIds = new HashSet<Long>();
		permissionIds.add(Permission.EDIT_PERMISSION_ID);
		permissionIds.add(Permission.SHARE_PERMISSION_ID);
		
		String documentName = "NEW_DOCUMENT_NAME_deleteSharedReference";
		
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("MY_ATTR_1_deleteSharedReference", "SOME VALUE1_deleteSharedReference");
		attributes.put("MY_ATTR_2_deleteSharedReference", "SOME VALUE1_deleteSharedReference");
		
		Reference ref = catalogService.addDocument(owner, partitionId, documentName, applicationCollection.getUniqueId(), "NEW_PAYLOAD_deleteSharedReference".getBytes(), permissionIds, null);
		ref= catalogService.modifyReference(owner, partitionId, ref.getUniqueId(), null, null, attributes);
		
		assertEquals(ref.getParentCollection().getUniqueId(), applicationCollection.getUniqueId());
		
		Map<String, String> userAndPartitionsToShareWith = new HashMap<String, String>();
		
		String otherOwner = "NEWOWNER_deleteSharedReference";
		userAndPartitionsToShareWith.put(otherOwner, partitionId);
		
		Set<Long> sharePermissionIds =  new HashSet<Long>();
		sharePermissionIds.add(Permission.EDIT_PERMISSION_ID);
		
		catalogService.shareReference(owner, partitionId, ref.getUniqueId(), applicationName, userAndPartitionsToShareWith, sharePermissionIds);

		CatalogCollection otherOwnerShareCollection = catalogService.getApplicationShareCollectionForUser(otherOwner, partitionId, applicationName);
		
		assertEquals(otherOwnerShareCollection.getReferences().size(), 1);
		
		String uniqueRefId = otherOwnerShareCollection.getReferences().iterator().next().getUniqueId();
		CatalogCollection collection = catalogService.deleteReferenceFromCollection(otherOwner, partitionId, uniqueRefId, false);
		assertEquals(collection.getReferences().size(), 0);
		
		otherOwnerShareCollection = catalogService.getApplicationShareCollectionForUser(otherOwner, partitionId, applicationName);
		assertEquals(otherOwnerShareCollection.getReferences().size(), 0);
		catalogDao.flush();
		catalogService.getReference(owner, partitionId, uniqueRefId);
	}

	@Test
	public void testCatalogCollectionClone() throws CollectionNotFoundException, CollectionModificationException, ObjectLockedException, CollectionSharingException, InvalidCatalogNameException{
		String user1 = "user1"; 
		String application = "PA";
		CatalogCollection user1RootCollection = catalogService.getRootApplicationCollectionForUser(user1, partitionId, application);
		
		Set<Long> allPermissions = new HashSet<Long>();
		allPermissions.add(Permission.ADD_PERMISSION_ID);
		allPermissions.add(Permission.SHARE_PERMISSION_ID);
		allPermissions.add(Permission.CLONE_PERMISSION_ID);
		allPermissions.add(Permission.EDIT_PERMISSION_ID);
		CatalogCollection newCollection = catalogService.addCollection(user1, partitionId, "My New Collection", user1RootCollection.getUniqueId(), allPermissions, null);
		catalogDao.flush();
		CatalogCollection newNestedCollection = catalogService.addCollection(user1, partitionId, "Nested in New", newCollection.getUniqueId(), allPermissions, null);
		
		Map<String, String> userAndPartitionsToShareWith = new HashMap<String, String>();
		userAndPartitionsToShareWith.put("user2", partitionId);
		catalogDao.flush();
		catalogService.shareCollection(user1, partitionId, application, newCollection.getUniqueId(), userAndPartitionsToShareWith , allPermissions);
		
		newCollection = catalogService.getCollection(user1, partitionId, newCollection.getUniqueId());
		newNestedCollection = catalogService.getCollection(user1, partitionId, newNestedCollection.getUniqueId());
		
		assertEquals(newCollection.getSharedTo().size(), 1);
		assertEquals(newCollection.getNestedCollections().size(), 1);
		
		assertEquals(newNestedCollection.getSharedTo().size(), 1);

		catalogService.cloneCollection(user1, partitionId, newNestedCollection.getUniqueId());
		
		assertEquals(newCollection.getNestedCollections().size(), 2);
		
		CatalogCollection user2ShareCollection = catalogService.getApplicationShareCollectionForUser("user2", partitionId, application);
		assertEquals(user2ShareCollection.getNestedCollections().size(), 1);
		
		CatalogCollection sharedCollectionWithClone = user2ShareCollection.getNestedCollections().iterator().next(); 
		assertEquals(sharedCollectionWithClone.getNestedCollections().size(), 2);
	}
	
	@Test
	public void multiDepthShareCollection() throws CollectionNotFoundException, CollectionModificationException, ObjectLockedException, DocumentTypeNotFoundException, CollectionSharingException, ReferenceModificationException, ReferenceNotFoundException, InvalidCatalogNameException {
		String owner1 = "OWNER_1_multiDepthShareCollection";
		String owner2 = "OWNER_2_multiDepthShareCollection";
		
		String application = "PA";
		
		CatalogCollection owner1RootCollection = catalogService.getRootApplicationCollectionForUser(owner1, partitionId, application);
		
		//Set new collection with all permissions
		Set<Long> allPermission = new HashSet<Long>();
		allPermission.add(Permission.EDIT_PERMISSION_ID);
		allPermission.add(Permission.SHARE_PERMISSION_ID);
		allPermission.add(Permission.CLONE_PERMISSION_ID);
		allPermission.add(Permission.ADD_PERMISSION_ID);
		
		Set<Long> allDocPermission = new HashSet<Long>();
		allDocPermission.add(Permission.EDIT_PERMISSION_ID);
		allDocPermission.add(Permission.SHARE_PERMISSION_ID);
		allDocPermission.add(Permission.CLONE_PERMISSION_ID);
		
		//Create a collection
		CatalogCollection collectionWith1Ref = catalogService.addCollection(owner1, partitionId, "Sample Collection Name _multiDepthShareCollection", owner1RootCollection.getUniqueId(), allPermission, null);

		//Add refererence to collection
		Reference newReference = catalogService.addDocument(owner1, partitionId, "Sample Doc Name_shareChainForCollectionAndPermissions", collectionWith1Ref.getUniqueId(), "PAYLOAD__multiDepthShareCollection".getBytes(), allPermission, null);
		assertEquals(newReference.getParentCollection().getUniqueId(), collectionWith1Ref.getUniqueId());

		//Just making sure that the collection was added...
		owner1RootCollection = catalogService.getRootApplicationCollectionForUser(owner1, partitionId, application);
		assertEquals(owner1RootCollection.getNestedCollections().size(), 1);
		
		CatalogCollection inOwner1RootCollection1 = owner1RootCollection.getNestedCollections().iterator().next();
		
		// Set the owne and partition id to share the collection with, this will create the share collection chain 
		Map<String, String> userAndPartitionsToShareWith = new HashMap<String, String>();
		userAndPartitionsToShareWith.put(owner2, partitionId);
		
		// Share the collection with this set of user's and their partition id's
		catalogService.shareCollection(owner1, partitionId, application, inOwner1RootCollection1.getUniqueId(), userAndPartitionsToShareWith, allPermission);
		
		CatalogCollection owner2ShareCollection = catalogService.getApplicationShareCollectionForUser(owner2, partitionId, application);
		
		assertEquals(owner2ShareCollection.getNestedCollections().size(), 1);
		
		String uniqueId = owner2ShareCollection.getNestedCollections().iterator().next().getUniqueId(); 
		
		String docName = "New Document in Shared Collection Triggering Share Chain";
		
		// add a document to the shared collection, which should create a ref and add then new ref to all collections in the share chain
		Reference newRefAdded = catalogService.addDocument(owner2, partitionId, docName, uniqueId, docName.getBytes(), allPermission, null);
		
		CatalogCollection owner2BaseAppCollection = catalogService.getApplicationShareCollectionForUser(owner2, partitionId, application);
		
		CatalogCollection collectionInOwner2NotShared = catalogService.addCollection(owner1, partitionId, "Sample Collection  not shared _addToSharedReference", owner2BaseAppCollection.getUniqueId(), allPermission, null);
		
		Reference movedReference = catalogService.moveReference(owner2, partitionId, newRefAdded.getUniqueId(), collectionInOwner2NotShared.getUniqueId());
		assertEquals(movedReference.getParentCollection().getUniqueId(), collectionInOwner2NotShared.getUniqueId());
//		catalogDao.flush();
		
		owner1RootCollection = catalogService.getRootApplicationCollectionForUser(owner1, partitionId, application);
		
		CatalogCollection owner1Collection1 = owner1RootCollection.getNestedCollections().iterator().next();
		assertEquals(owner1Collection1.getReferences().size(),1);
		
		owner2BaseAppCollection = catalogService.getApplicationShareCollectionForUser(owner2, partitionId, application);
		
		collectionInOwner2NotShared = null;
		for(CatalogCollection cc : owner2BaseAppCollection.getNestedCollections()){
			if(cc.getName().equals("Sample Collection  not shared _addToSharedReference")){
				collectionInOwner2NotShared = cc;
			}
		}
		if(collectionInOwner2NotShared == null){
			fail("This should have been found!!");
		}
		assertEquals(collectionInOwner2NotShared.getReferences().size(), 1);
	}
	
//	@Test
//	public void verifyMultiDepth() throws CollectionNotFoundException{
//		String owner1 = "OWNER_1_multiDepthShareCollection";
//		String owner2 = "OWNER_2_multiDepthShareCollection";
//		
//		String application = "PA";
//		
//		CatalogCollection owner1RootCollection = catalogService.getRootApplicationCollectionForUser(owner1, partitionId, application);
//		
//		CatalogCollection owner1Collection1 = owner1RootCollection.getNestedCollections().iterator().next();
//		assertEquals(owner1Collection1.getReferences().size(),1);
//		
//		CatalogCollection owner2BaseAppCollection = catalogService.getApplicationShareCollectionForUser(owner2, partitionId, application);
//		
//		CatalogCollection collectionInOwner2NotShared = null;
//		for(CatalogCollection cc : owner2BaseAppCollection.getNestedCollections()){
//			if(cc.getName().equals("Sample Collection  not shared _addToSharedReference")){
//				collectionInOwner2NotShared = cc;
//			}
//		}
//		if(collectionInOwner2NotShared == null){
//			fail("This should have been found!!");
//		}
//		assertEquals(collectionInOwner2NotShared.getReferences().size(), 1);
//	}
	
	
	@Test
	public void moveSharedReference() throws CollectionNotFoundException, CollectionModificationException, ObjectLockedException, DocumentTypeNotFoundException, CollectionSharingException, ReferenceModificationException, ReferenceNotFoundException, InvalidCatalogNameException {
		
		String owner1 = "OWNER_1_moveSharedReference";
		String owner2 = "OWNER_2_moveSharedReference";
		
		String application = "PA";
		
		CatalogCollection owner1RootCollection = catalogService.getRootApplicationCollectionForUser(owner1, partitionId, application);
		
		//Set new collection with all permissions
		Set<Long> allPermission = new HashSet<Long>();
		allPermission.add(Permission.EDIT_PERMISSION_ID);
		allPermission.add(Permission.SHARE_PERMISSION_ID);
		allPermission.add(Permission.CLONE_PERMISSION_ID);
		allPermission.add(Permission.ADD_PERMISSION_ID);
		
		Set<Long> allDocPermission = new HashSet<Long>();
		allDocPermission.add(Permission.EDIT_PERMISSION_ID);
		allDocPermission.add(Permission.SHARE_PERMISSION_ID);
		allDocPermission.add(Permission.CLONE_PERMISSION_ID);
		
		//Create a collection
		CatalogCollection collectionWith1Ref = catalogService.addCollection(owner1, partitionId, "Sample Collection Name _moveSharedReference", owner1RootCollection.getUniqueId(), allPermission, null);
		
		//Add refererence to collection
		
		Reference newReference = catalogService.addDocument(owner1, partitionId, "Sample Doc Name_shareChainForCollectionAndPermissions", collectionWith1Ref.getUniqueId(), "PAYLOAD__moveSharedReference".getBytes(), allPermission, null);
		assertEquals(newReference.getParentCollection().getUniqueId(), collectionWith1Ref.getUniqueId());
		
		//Just making sure that the collection was added...
		owner1RootCollection = catalogService.getRootApplicationCollectionForUser(owner1, partitionId, application);
		assertEquals(owner1RootCollection.getNestedCollections().size(), 1);
		
		CatalogCollection inOwner1RootCollection1 = owner1RootCollection.getNestedCollections().iterator().next();
		
		// Set the owne and partition id to share the collection with, this will create the share collection chain 
		Map<String, String> userAndPartitionsToShareWith = new HashMap<String, String>();
		userAndPartitionsToShareWith.put(owner2, partitionId);
		
		// Share the collection with this set of user's and their partition id's
		catalogService.shareCollection(owner1, partitionId, application, inOwner1RootCollection1.getUniqueId(), userAndPartitionsToShareWith, allPermission);
		
		CatalogCollection owner2ShareCollection = catalogService.getApplicationShareCollectionForUser(owner2, partitionId, application);
		assertEquals(owner2ShareCollection.getNestedCollections().size(), 1);
		
		String docName = "New Document in Shared Collection Triggering Share Chain";
		
		// add a document to the shared collection, which should create a ref and add then new ref to all collections in the share chain
		Reference newRefAdded = catalogService.addDocument(owner2, partitionId, docName, owner2ShareCollection.getNestedCollections().iterator().next().getUniqueId(), docName.getBytes(), allPermission, null);
		
		CatalogCollection owner2BaseAppCollection = catalogService.getRootApplicationCollectionForUser(owner2, partitionId, application);
		
		CatalogCollection collectionInOwner2NotShared = catalogService.addCollection(owner2, partitionId, "Sample Collection  not shared _moveSharedReference", owner2BaseAppCollection.getUniqueId(), allPermission, null);
		
		catalogService.moveReference(owner2, partitionId, newRefAdded.getUniqueId(), collectionInOwner2NotShared.getUniqueId());
		
		owner1RootCollection = catalogService.getRootApplicationCollectionForUser(owner1, partitionId, application);
		
		owner2ShareCollection = catalogService.getCollection(owner2, partitionId, owner2ShareCollection.getUniqueId());
		
		assertEquals(owner2ShareCollection.getNestedCollections().size(), 1);
		
		assertEquals(owner2ShareCollection.getParentCollection().getNestedCollections().size(), 2);
		
		collectionInOwner2NotShared = catalogService.getCollection(owner2, partitionId, collectionInOwner2NotShared.getUniqueId());
		assertEquals(collectionInOwner2NotShared.getReferences().size(), 1);
	}
	
	
	@Test
	public void shareChainForCollectionAndPermissions() throws CollectionNotFoundException, CollectionModificationException, ObjectLockedException, CollectionSharingException, DocumentTypeNotFoundException, InvalidCatalogNameException{
		String owner1 = "OWNER_1_shareChainForCollection";
		String owner2 = "OWNER_2_shareChainForCollection";
		
		String application = "PA";
		
		CatalogCollection owner1RootCollection = catalogService.getRootApplicationCollectionForUser(owner1, partitionId, application);
		
		//Set new collection with all permissions
		Set<Long> allPermission = new HashSet<Long>();
		allPermission.add(Permission.EDIT_PERMISSION_ID);
		allPermission.add(Permission.SHARE_PERMISSION_ID);
		allPermission.add(Permission.CLONE_PERMISSION_ID);
		allPermission.add(Permission.ADD_PERMISSION_ID);
		
		Set<Long> allDocPermission = new HashSet<Long>();
		allDocPermission.add(Permission.EDIT_PERMISSION_ID);
		allDocPermission.add(Permission.SHARE_PERMISSION_ID);
		allDocPermission.add(Permission.CLONE_PERMISSION_ID);
		
		//Create a collection
		CatalogCollection collectionWith1Ref = catalogService.addCollection(owner1, partitionId, "Sample Collection Name _shareChainForCollectionAndPermissions", owner1RootCollection.getUniqueId(), allPermission, null);
		//Add refererence to collection
		Reference newReference = catalogService.addDocument(owner1, partitionId, "Sample Doc Name_shareChainForCollectionAndPermissions", collectionWith1Ref.getUniqueId(), "PAYLOAD_shareChainForCollectionAndPermissions".getBytes(), allPermission, null);
		assertEquals(newReference.getParentCollection().getUniqueId(), collectionWith1Ref.getUniqueId());
		
		//Just making sure that the collection was added...
		owner1RootCollection = catalogService.getRootApplicationCollectionForUser(owner1, partitionId, application);
		assertEquals(owner1RootCollection.getNestedCollections().size(), 1);
		
		CatalogCollection inOwner1RootCollection1 = owner1RootCollection.getNestedCollections().iterator().next();
		
		// Set the owne and partition id to share the collection with, this will create the share collection chain 
		Map<String, String> userAndPartitionsToShareWith = new HashMap<String, String>();
		userAndPartitionsToShareWith.put(owner2, partitionId);
		
		// Share the collection with this set of user's and their partition id's
		catalogService.shareCollection(owner1, partitionId, application, inOwner1RootCollection1.getUniqueId(), userAndPartitionsToShareWith, allPermission);
		
		CatalogCollection owner2ShareCollection = catalogService.getApplicationShareCollectionForUser(owner2, partitionId, application);
		assertEquals(owner2ShareCollection.getNestedCollections().size(), 1);
		
		String docName = "New Document in Shared Collection (Triggering Share Chain)";
		// add a document to the shared collection, which should create a ref and add then new ref to all collections in the share chain
		Reference newRefAdded = catalogService.addDocument(owner2, partitionId, docName, owner2ShareCollection.getNestedCollections().iterator().next().getUniqueId(), docName.getBytes(), allPermission, null);
		assertEquals(newRefAdded.getDocumentName(), docName);
		
		// Now check the affected (original shared collection)
		owner1RootCollection = catalogService.getRootApplicationCollectionForUser(owner1, partitionId, application);
		
		catalogService.getApplicationShareCollectionForUser(owner2, partitionId, application);
		assertEquals(owner1RootCollection.getNestedCollections().size(), 1);	
		
		CatalogCollection owner1Collection1 = owner1RootCollection.getNestedCollections().iterator().next();
//		 Verify the creation of the doc in the shared collection, modifies the original
		assertEquals(owner1Collection1.getReferences().size(),2);
		assertEquals(owner2ShareCollection.getNestedCollections().size(), 1);
		assertEquals(owner2ShareCollection.getNestedCollections().iterator().next().getReferences().size(), 2);
	}
	
	@Test
	public void collectionBeanValidation() throws InvalidCatalogNameException{
		this.flush = false;
		CatalogCollection c = new CatalogCollection();
		try{
			catalogDao.save(c);
		} catch(ConstraintViolationException e){
			assertEquals(e.getConstraintViolations().size(), 4);
		}
		c = new CatalogCollection();
		c.setOwner("Carter");
		try{
			catalogDao.save(c);
		} catch(ConstraintViolationException e){
			assertEquals(e.getConstraintViolations().size(), 3);
		}
		c = new CatalogCollection();
		c.setOwner("Carter");
		c.setPartitionId("partitionId");
		try{
			catalogDao.save(c);
		} catch(ConstraintViolationException e){
			assertEquals(e.getConstraintViolations().size(), 2);
		}
		c = new CatalogCollection();
		c.setOwner("Carter");
		c.setPartitionId("partitionId");
		c.setName("name");
		try{
			catalogDao.save(c);
		} catch(ConstraintViolationException e){
			assertEquals(e.getConstraintViolations().size(), 1);
		}
		c = new CatalogCollection();
		c.setOwner("Carter");
		c.setPartitionId("partitionId");
		c.setName("name");
		c.getPermissions().add(new Permission(""));
		catalogDao.save(c);
	}
	
	@Test
	public void documentBeanValidation() throws InvalidCatalogNameException{
		this.flush = false;
		Document d = new Document();
		try{
			catalogDao.save(d);
		} catch(ConstraintViolationException e){
			assertEquals(e.getConstraintViolations().size(), 4);
		}
		d = new Document();
		d.setName("My Doc Name");
		try{
			catalogDao.save(d);
		} catch(ConstraintViolationException e){
			assertEquals(e.getConstraintViolations().size(), 3);
		}
		d = new Document();
		d.setName("My Doc Name");
		d.setOwner("Carter");
		try{
			catalogDao.save(d);
		} catch(ConstraintViolationException e){
			assertEquals(e.getConstraintViolations().size(), 2);
		}
		d = new Document();
		d.setName("My Doc Name");
		d.setOwner("Carter");
		d.setPayload("Payload".getBytes());
		try{
			catalogDao.save(d);
		} catch(ConstraintViolationException e){
			assertEquals(e.getConstraintViolations().size(), 1);
		}
		d = new Document();
		d.setName("My Doc Name");
		d.setOwner("Carter");
		d.setPartitionId("partition id");
		d.setPayload("Payload".getBytes());
		catalogDao.save(d);
	}
	
	@Test
	public void referenceBeanValidation() throws InvalidCatalogNameException{
		this.flush = false;
		Reference r = new Reference();
		try{
			catalogDao.save(r);
		} catch(ConstraintViolationException e){
			assertEquals(e.getConstraintViolations().size(), 6);
		}
		r= new Reference();
		r.setParentCollection(new CatalogCollection("owner", "partitionId","name"));
		try{
			catalogDao.save(r);
		} catch(ConstraintViolationException e1){
			assertEquals(e1.getConstraintViolations().size(), 5);
		}
		r= new Reference();
		r.setParentCollection(new CatalogCollection());
		r.setDocument(new Document());
		try{
			catalogDao.save(r);
		} catch(ConstraintViolationException e2){
			assertEquals(e2.getConstraintViolations().size(), 4);
		}
		r = new Reference();
		r.setParentCollection(new CatalogCollection());
		Document d = new Document("docName", "owner", "partitionId", "pay".getBytes());
		r.setDocument(d);
		r.setOwner("owner1");
		try{
			catalogDao.save(r);
		} catch(ConstraintViolationException e3){
			assertEquals(e3.getConstraintViolations().size(), 3);
		}
		r= new Reference();
		r.setParentCollection(new CatalogCollection());
		r.setDocument(d);
		r.setOwner("");
		r.setName("RefName");
		try{
			catalogDao.save(r);
		} catch(ConstraintViolationException e){
			assertEquals(e.getConstraintViolations().size(), 1);
		}
	}
	
	@Test
	public void collections(){
		String owner = "OWNER_collections";
		String applicationName = "APPLICATION_NAME_collections";
		CatalogCollection c = new CatalogCollection(owner, partitionId, applicationName);
		c.getPermissions().add((Permission)catalogDao.findById(Permission.class, Permission.EDIT_PERMISSION_ID));
		c = (CatalogCollection)catalogDao.findById(CatalogCollection.class, catalogDao.save(c));
		assertEquals(c.getOwner(), owner);
		Date oldLastUpdateTime = c.getLastModifiedDate();
		String newOwner = "NEWOWNER_collections";
		c.setOwner(newOwner);
		catalogDao.update(c);
		catalogDao.refresh(c);
		assertNotSame(oldLastUpdateTime, c.getLastModifiedDate());
		assertNotSame(c.getOwner(), newOwner);
		catalogDao.delete(c);
	}
	
	@Test
	public void modifyDocument() throws CollectionNotFoundException, DocumentTypeNotFoundException, ReferenceNotFoundException, ReferenceModificationException, ObjectLockedException, InvalidCatalogNameException, CollectionModificationException{
		
		String owner = "OWNER_modifyDocument";
		String application = "APPLICATION_NAME_modifyDocument";
		
		CatalogCollection applicationCollection = catalogService.getRootApplicationCollectionForUser(owner, partitionId, application);
		
		Set<Long> permissions = new HashSet<Long>();
		permissions.add(Permission.EDIT_PERMISSION_ID);
		
		String documentName = "DOC_NAME_modifyDocument";
		byte[] payload = "PAYLOAD_modifyDocument".getBytes();
		
		Set<Long> sharePermissionIds = new HashSet<Long>();
		sharePermissionIds.add(Permission.EDIT_PERMISSION_ID);
		Reference ref = catalogService.addDocument(owner, partitionId, documentName, applicationCollection.getUniqueId(), payload, sharePermissionIds, null);

		String newDocName = "NEW_DOC_NAME_modifyDcoument";
		String documentNotes = "NEW_NOTES";
		String documentDescription = "NEW_DESCRIPTION";
		Boolean locked = true;
		Boolean archive = false;
		byte[] newPayload = "NEW_PAYLOAD".getBytes();
		Reference modifiedDocReference = catalogService.modifyDocumentByReferenceId(owner, partitionId, ref.getUniqueId(), newDocName, documentNotes, documentDescription, locked, archive, newPayload, sharePermissionIds, null);
		
		assertEquals(modifiedDocReference.getDocument().getName(), newDocName);
		assertEquals(modifiedDocReference.getDocument().getNotes(), documentNotes);
		assertEquals(modifiedDocReference.getDocument().getDescription(), documentDescription);
		assertEquals(modifiedDocReference.getUniqueId(), ref.getUniqueId());
		
	}
	
	@Test
	public void cloneDocument() throws CollectionNotFoundException, DocumentTypeNotFoundException, ReferenceNotFoundException, CollectionSharingException, CollectionModificationException, ReferenceModificationException, InvalidCatalogNameException {
		String owner = "OWNER_cloneDocument";
		String application = "APPLICATION_NAME_cloneDocument";
		
		CatalogCollection applicationCollection = catalogService.getRootApplicationCollectionForUser(owner, partitionId, application);
		
		Set<Long> permissions = new HashSet<Long>();
		permissions.add(Permission.EDIT_PERMISSION_ID);
		permissions.add(Permission.SHARE_PERMISSION_ID);
		permissions.add(Permission.CLONE_PERMISSION_ID);
		
		String documentName = "DOC_NAME_cloneDocument";
		byte[] payload = "PAYLOAD_cloneDocument".getBytes();
		
		Reference ref = catalogService.addDocument(owner, partitionId, documentName, applicationCollection.getUniqueId(), payload, permissions, null);
		Reference clonedDocReference = catalogService.cloneDocument(owner, partitionId, ref.getUniqueId(), null);
		
		assertEquals(clonedDocReference.getDocument().getName(), CatalogUtils.clonedName(ref.getDocumentName()));
		assertEquals(clonedDocReference.getDocument().getNotes(), ref.getDocument().getNotes());
		assertEquals(clonedDocReference.getDocument().getDescription(), ref.getDocumentDescription());
		assertNotSame(clonedDocReference.getUniqueId(), ref.getUniqueId());
		
		assertEquals(clonedDocReference.getDocument().getName(), CatalogUtils.clonedName(ref.getDocument().getName()));
		assertEquals(clonedDocReference.getDocument().getNotes(), ref.getDocument().getNotes());
		assertEquals(clonedDocReference.getDocument().getDescription(), ref.getDocument().getDescription());
	}

	@Test
	public void cloneReference() throws CollectionNotFoundException, DocumentTypeNotFoundException, ReferenceNotFoundException, CollectionModificationException, ReferenceModificationException, InvalidCatalogNameException{
		
		String owner = "OWNER_cloneReference";
		String application = "APPLICATION_NAME_cloneReference";
		
		CatalogCollection applicationCollection = catalogService.getRootApplicationCollectionForUser(owner, partitionId, application);
		
		Set<Long> permissions = new HashSet<Long>();
		permissions.add(Permission.EDIT_PERMISSION_ID);
		permissions.add(Permission.SHARE_PERMISSION_ID);
		permissions.add(Permission.CLONE_PERMISSION_ID);
		
		Reference ref = catalogService.addDocument(owner, partitionId, "DOC_NAME_cloneReference", applicationCollection.getUniqueId(), "payload".getBytes(), permissions, null);

		Reference clonedReference = catalogService.cloneRefernence(owner, partitionId, ref.getUniqueId(), permissions, applicationCollection.getUniqueId());
		
		assertEquals(ref.getDocumentName(), clonedReference.getDocumentName());
		assertNotSame(ref.getName(), clonedReference.getName());
		assertEquals(ref.getPermissions(), clonedReference.getPermissions());
		assertEquals(CatalogUtils.clonedName(ref.getName()), clonedReference.getName());
	}
	
	@Test
	public void cloneCollection() throws CollectionNotFoundException, DocumentTypeNotFoundException, CollectionModificationException, ObjectLockedException, InvalidCatalogNameException{
		String owner = "OWNER_cloneCollection";
		String application = "APPLICATION_NAME_cloneCollection";
		
		CatalogCollection applicationCollection = catalogService.getRootApplicationCollectionForUser(owner, partitionId, application);
		
		Set<Long> permissions = new HashSet<Long>();
		permissions.add(Permission.EDIT_PERMISSION_ID);
		permissions.add(Permission.SHARE_PERMISSION_ID);
		permissions.add(Permission.CLONE_PERMISSION_ID);
		CatalogCollection newCollection = catalogService.addCollection(owner, partitionId, "NEW_COLLECTION_cloneCollection", applicationCollection.getUniqueId(), permissions , null);
		
		catalogService.addDocument(owner, partitionId, "DOC_NAME_cloneCollection", newCollection.getUniqueId(), "payload".getBytes(), permissions, null);
		newCollection = catalogService.getCollection(owner, partitionId, newCollection.getUniqueId());
		
		CatalogCollection clonedCollection = catalogService.cloneCollection(owner, partitionId, newCollection.getUniqueId());
		
		assertEquals(clonedCollection.getName(), CatalogUtils.clonedName(newCollection.getName()));
		assertEquals(newCollection.getReferences().size(), clonedCollection.getReferences().size());
		assertEquals(CatalogUtils.clonedName(newCollection.getReferences().iterator().next().getName()), clonedCollection.getReferences().iterator().next().getName());
	}
	
	@Test
	public void addNewCollection() throws CollectionNotFoundException, CollectionModificationException, ObjectLockedException, InvalidCatalogNameException{
		catalogDao.setSessionFactory(sessionFactory);
		
		String owner = "OWNER_addNewCollection";
		String applicationName = "APPLICATION_NAME_addNewCollection";
		
		CatalogCollection parentCollection = catalogService.getRootApplicationCollectionForUser(owner, partitionId, applicationName);
		
		Set<Long> permissionIds = new HashSet<Long>();
		permissionIds.add(Permission.EDIT_PERMISSION_ID);
		permissionIds.add(Permission.SHARE_PERMISSION_ID);
		
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("MY_ATTRIBUTE_1_addNewCollection", "ATTRIBUTE VALUE 1_addNewCollection");
		attributes.put("MY_ATTRIBUTE_2_addNewCollection", "ATTRIBUTE VALUE 2_addNewCollection");
		attributes.put("MY_ATTRIBUTE_3_addNewCollection", "ATTRIBUTE VALUE 3_addNewCollection");
		
		String newCollectionName = "NEW_COLLECTION_NAME_addNewCollection";
		CatalogCollection collection = catalogService.addCollection(owner, partitionId, newCollectionName, parentCollection.getUniqueId(), permissionIds, attributes);
		assertEquals(collection.getName(), newCollectionName);
		assertEquals(collection.getPermissions().size(), 2);
		assertEquals(collection.getAttributes().size(), 3);
		assertEquals(collection.getParentCollection().getUniqueId(), parentCollection.getUniqueId());
		assertEquals(collection.getParentCollection().getName(), applicationName);
		assertEquals(collection.getParentCollection().getParentCollection().getName(), owner);
		assertEquals(collection.getParentCollection().getParentCollection().getParentCollection().getParentCollection(), null);
		
		catalogService.deleteCollection(collection.getOwner(), collection.getPartitionId(), collection.getUniqueId());
		
		CatalogCollection deletedCollection = null;
		deletedCollection = catalogService.getCollection(owner, partitionId, collection.getUniqueId());
		assertEquals(deletedCollection.isArchived(), true);
	}
	
	@Test
	public void modifyCollection() throws CollectionNotFoundException, CollectionModificationException, ObjectLockedException, InvalidCatalogNameException{
		String owner = "OWNER_modifyCollection";
		String applicationName = "APPLICATION_NAME_modifyCollection";
		
		CatalogCollection parentCollection = catalogService.getRootApplicationCollectionForUser(owner, partitionId, applicationName);
		
		Set<Long> permissionIds = new HashSet<Long>();
		permissionIds.add(Permission.EDIT_PERMISSION_ID);
		permissionIds.add(Permission.SHARE_PERMISSION_ID);
		
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("MY_ATTRIBUTE_1_modifyCollection", "ATTRIBUTE VALUE 1_modifyCollection");
		attributes.put("MY_ATTRIBUTE_2_modifyCollection", "ATTRIBUTE VALUE 2_modifyCollection");
		attributes.put("MY_ATTRIBUTE_3_modifyCollection", "ATTRIBUTE VALUE 3_modifyCollection");
		
		String newCollectionName = "NEW_COLLECTION_NAME_modifyCollection";
		CatalogCollection collection = catalogService.addCollection(owner, partitionId, newCollectionName, parentCollection.getUniqueId(), permissionIds, attributes);
		assertEquals(collection.getName(), newCollectionName);
		assertEquals(collection.getPermissions().size(), 2);
		assertEquals(collection.getAttributes().size(), 3);
		assertEquals(collection.getParentCollection().getUniqueId(), parentCollection.getUniqueId());
		assertEquals(collection.getParentCollection().getName(), applicationName);
		assertEquals(collection.getParentCollection().getParentCollection().getName(), owner);
		assertEquals(collection.getParentCollection().getParentCollection().getParentCollection().getParentCollection(), null);
		
		String newParentCollectionForMove = "NEW_COLLECTION_FOR_MOVE_modifyCollection";
		CatalogCollection newCollection = catalogService.addCollection(owner, partitionId, newParentCollectionForMove, parentCollection.getUniqueId(), permissionIds, attributes);
		assertEquals(newCollection.getName(), newParentCollectionForMove);
		assertEquals(newCollection.getPermissions().size(), 2);
		assertEquals(newCollection.getAttributes().size(), 3);
		assertEquals(newCollection.getParentCollection().getUniqueId(), parentCollection.getUniqueId());
		assertEquals(newCollection.getParentCollection().getName(), applicationName);
		assertEquals(newCollection.getParentCollection().getParentCollection().getName(), owner);
		assertEquals(newCollection.getParentCollection().getParentCollection().getParentCollection().getParentCollection(), null);
		String anotherNewCollection = "ANOTHER_NEW_COLLECTION_modifyCollection";
		collection = catalogService.modifyCollection(owner, partitionId, collection.getUniqueId(), anotherNewCollection, null, null, null);
		assertEquals(collection.getName(), anotherNewCollection);
		assertEquals(collection.getPermissions().size(), 2);
		assertEquals(collection.getAttributes().size(), 3);
		assertEquals(collection.getParentCollection().getUniqueId(), parentCollection.getUniqueId());
		assertEquals(collection.getParentCollection().getName(), applicationName);
		assertEquals(collection.getParentCollection().getParentCollection().getName(), owner);
		
		Set<Long> newPermissionIds = new HashSet<Long>();
		newPermissionIds.add(Permission.EDIT_PERMISSION_ID);
		
		String collectionWithNoSharePermissions = "NEW_COLLECTION_NO_SHARE_PERMISSIONS_modifyCollection";
		collection = catalogService.modifyCollection(owner, partitionId, collection.getUniqueId(), collectionWithNoSharePermissions, null, newPermissionIds, null);
		assertEquals(collection.getName(), collectionWithNoSharePermissions);
		assertEquals(collection.getPermissions().size(), 1);
		assertEquals(collection.getAttributes().size(), 3);
		assertEquals(collection.getParentCollection().getUniqueId(), parentCollection.getUniqueId());
		assertEquals(collection.getParentCollection().getName(), applicationName);
		assertEquals(collection.getParentCollection().getParentCollection().getName(), owner);
		
		Map<String, String> newAttributes = new HashMap<String, String>();
		newAttributes.put("MY_ATTRIBUTE_1_modifyCollection", "ATTRIBUTE VALUE 1_modifyCollection");
		newAttributes.put("MY_ATTRIBUTE_2_modifyCollection", "ATTRIBUTE VALUE 2_modifyCollection");
		newAttributes.put("MY_ATTRIBUTE_3_modifyCollection", "ATTRIBUTE VALUE 3_modifyCollection");
		newAttributes.put("MY_ATTRIBUTE_4_modifyCollection", "ATTRIBUTE VALUE 4_modifyCollection");
		
		String modifyCollectionWithAttributeChanges = "MODIFY_COLLECTION_ATTRIBUTE_CHANGES_modifyCollection";
		
		collection = catalogService.modifyCollection(owner, partitionId, collection.getUniqueId(), modifyCollectionWithAttributeChanges, null, newPermissionIds, newAttributes);
		assertEquals(collection.getName(), modifyCollectionWithAttributeChanges);
		assertEquals(collection.getPermissions().size(), 1);
		assertEquals(collection.getAttributes().size(), 4);
		assertEquals(collection.getParentCollection().getUniqueId(), parentCollection.getUniqueId());
		assertEquals(collection.getParentCollection().getName(), applicationName);
		assertEquals(collection.getParentCollection().getParentCollection().getName(), owner);
		
		String newCollectionNameAndMoveCollection = "NEW_COLLECTION_AND_MOVE_COLLECTION_modifyCollection";
		collection = catalogService.modifyCollection(owner, partitionId, collection.getUniqueId(), newCollectionNameAndMoveCollection, newCollection.getUniqueId(), null, null);
		assertEquals(collection.getName(), newCollectionNameAndMoveCollection);
		assertEquals(collection.getPermissions().size(), 1);
		assertEquals(collection.getAttributes().size(), 4);
		assertEquals(collection.getParentCollection().getUniqueId(), newCollection.getUniqueId());
		assertEquals(collection.getParentCollection().getName(), newParentCollectionForMove);
		assertEquals(collection.getParentCollection().getParentCollection().getName(), applicationName);
		assertEquals(collection.getParentCollection().getParentCollection().getParentCollection().getName(), owner);
		
		catalogService.deleteCollection(collection.getOwner(), collection.getPartitionId(), collection.getUniqueId());
		catalogService.deleteCollection(newCollection.getOwner(), newCollection.getPartitionId(), newCollection.getUniqueId());
	}
	
	@Test(expected=CollectionModificationException.class)
	public void cannotModifyCollectionWithoutEditPermission() throws CollectionNotFoundException, CollectionModificationException, ObjectLockedException, CollectionSharingException, InvalidCatalogNameException{
		
		String owner = "OWNER_cannotModifyCollectionWithoutEditPermission";
		String application = "APPLICATION_NAME_cannotModifyCollectionWithoutEditPermission";
		
		CatalogCollection parentCollection = catalogService.getRootApplicationCollectionForUser(owner, partitionId, application);
		
		Set<Long> permissionIds = new HashSet<Long>();
		permissionIds.add(Permission.SHARE_PERMISSION_ID);
		
		String newCollectionName = "NEW_COLLECTION_NAME_cannotModifyCollectionWithoutEditPermission";
		CatalogCollection collection = catalogService.addCollection(owner, partitionId, newCollectionName, parentCollection.getUniqueId(), permissionIds, null);
		Map<String, String> userAndPartitionsToShareWith = new HashMap<String, String>();
		userAndPartitionsToShareWith.put("newOwner", partitionId);
		catalogService.shareCollection(owner, partitionId, application, collection.getUniqueId(), userAndPartitionsToShareWith , permissionIds);
		
		CatalogCollection newOwnerSharedToCollection = catalogService.getApplicationShareCollectionForUser("newOwner", partitionId, application);
		
		catalogService.modifyCollection(owner, partitionId, newOwnerSharedToCollection.getNestedCollections().iterator().next().getUniqueId(), newCollectionName, null, null, null);
	}
	
	@Test
	public void catalogUtilsvalidateCollectionCanBeShared(){
		
		Permission editPermission = (Permission)catalogDao.findById(Permission.class, Permission.EDIT_PERMISSION_ID);
		Permission sharePermission = (Permission)catalogDao.findById(Permission.class, Permission.SHARE_PERMISSION_ID);
		
		CatalogCollection collectionToShare = new CatalogCollection();
		collectionToShare.getPermissions().add(editPermission);
		collectionToShare.getPermissions().add(sharePermission);
		
		Reference referenceThatCanBeShared = new Reference();
		referenceThatCanBeShared.getPermissions().add(sharePermission);
		
		collectionToShare.getReferences().add(referenceThatCanBeShared);
		
		CatalogCollection collectionThatStopsSharing = new CatalogCollection();
		
		Reference referenceThatCanNotBeShared = new Reference();
		
		collectionThatStopsSharing.setParentCollection(collectionToShare);
		collectionThatStopsSharing.getReferences().add(referenceThatCanNotBeShared);
		
		collectionToShare.getNestedCollections().add(collectionThatStopsSharing);

		Map<CatalogCollection, String> collectionsThatCanNotBeShared = new HashMap<CatalogCollection, String>();
		Map<Reference, String> referencesThatCanNotBeShared = new HashMap<Reference, String>();
		
		Set<Permission> permissionsToShareWith = new HashSet<Permission>();
		
		try{
			CatalogUtils.validateCollectionCanBeShared(collectionToShare, permissionsToShareWith);
		} catch (CollectionSharingException e){
			collectionsThatCanNotBeShared = e.getCollectionsThatCanNotBeShared();
			referencesThatCanNotBeShared = e.getReferencesThatCanNotBeShared();
		}
		
		assertEquals(collectionsThatCanNotBeShared.size(), 1);
		assertEquals(referencesThatCanNotBeShared.size(), 1);
		
		assertEquals(collectionsThatCanNotBeShared.entrySet().iterator().next().getValue(), Permission.CAN_NOT_SHARE_COLLECTION_MESSAGE);
		assertEquals(referencesThatCanNotBeShared.entrySet().iterator().next().getValue(), Permission.CAN_NOT_SHARE_REFERENCE_MESSAGE);
		
		permissionsToShareWith.add(sharePermission);
		permissionsToShareWith.add(editPermission);
		
		try{
			CatalogUtils.validateCollectionCanBeShared(collectionToShare, permissionsToShareWith);
		} catch (CollectionSharingException e){
			collectionsThatCanNotBeShared = e.getCollectionsThatCanNotBeShared();
			referencesThatCanNotBeShared = e.getReferencesThatCanNotBeShared();
		}
		
		assertEquals(collectionsThatCanNotBeShared.size(), 1);
		assertEquals(referencesThatCanNotBeShared.size(), 2);
		
		assertEquals(collectionsThatCanNotBeShared.entrySet().iterator().next().getValue(), Permission.CAN_NOT_SHARE_COLLECTION_MESSAGE);
		
		assertEquals(referencesThatCanNotBeShared.get(referenceThatCanBeShared), Permission.CAN_NOT_SHARE_REFERENCE_NOT_ENOUGH_PERMISSION_MESSAGE);
		assertEquals(referencesThatCanNotBeShared.get(referenceThatCanNotBeShared), Permission.CAN_NOT_SHARE_REFERENCE_MESSAGE);
	}
	
	@Test
	public void canShareCollection() throws CollectionNotFoundException, CollectionModificationException, CollectionSharingException, DocumentTypeNotFoundException, ObjectLockedException, InvalidCatalogNameException{
		String owner = "OWNER_canShareCollection";
		String applicationName = "APPLICATION_NAME_canShareCollection";
		
		CatalogCollection parentCollection = catalogService.getRootApplicationCollectionForUser(owner, partitionId, applicationName);
		
		Set<Long> permissionIds = new HashSet<Long>();
		permissionIds.add(Permission.EDIT_PERMISSION_ID);
		permissionIds.add(Permission.ADD_PERMISSION_ID);
		permissionIds.add(Permission.SHARE_PERMISSION_ID);
		
		String collectionNameToShare = "COLLECTION_TO_SHARE_canShareCollection";
		CatalogCollection collection = catalogService.addCollection(owner, partitionId, collectionNameToShare, parentCollection.getUniqueId(), permissionIds, null);
		
		String documentName = "DOCUMENT_NAME_canShareCollection";
		
		Reference ref = catalogService.addDocument(owner, partitionId, documentName, collection.getUniqueId(), "PAYLOAD_canShareCollection".getBytes(), permissionIds, null);
		
		catalogService.addCollection(owner, partitionId, collectionNameToShare, collection.getUniqueId(), permissionIds, null);
//		catalogDao.flush();
		Map<String, String> userAndPartitionsToShareWith = new HashMap<String, String>();
		
		String newUser = "NEW_OWNER_canShareCollection";
		userAndPartitionsToShareWith.put(newUser, partitionId);
		
		Set<Long> sharePermissionIds = new HashSet<Long>();
		sharePermissionIds.add(Permission.EDIT_PERMISSION_ID);
		collection = catalogService.getCollection(collection.getOwner(), collection.getPartitionId(), collection.getUniqueId());
//		catalogDao.flush();
		catalogService.shareCollection(owner, partitionId, applicationName, collection.getUniqueId(), userAndPartitionsToShareWith, sharePermissionIds);
		collection = catalogService.getCollection(owner, partitionId, collection.getUniqueId());
		
		CatalogCollection newUserApplicationCollection = catalogService.getApplicationShareCollectionForUser(newUser, partitionId, applicationName);
		
		assertEquals(newUserApplicationCollection.getNestedCollections().size(), 1);
		CatalogCollection newUserSharedCollection = newUserApplicationCollection.getNestedCollections().iterator().next();
		assertEquals(newUserSharedCollection.getName(), collectionNameToShare);
		assertEquals(newUserSharedCollection.getReferences().size(), 1);
		
		assertEquals(newUserSharedCollection.getReferences().iterator().next().getCreatedFrom().getUniqueId(), ref.getUniqueId());
		
		assertEquals(newUserSharedCollection.getPermissions().size(), 1);
		assertEquals(newUserSharedCollection.getAttributes().size(), 0);
		assertEquals(collection.getSharedTo().size(), 1);
		assertEquals(newUserSharedCollection.getSharedFrom().getUniqueId(), collection.getUniqueId());
		
		catalogService.deleteCollection(collection.getOwner(), collection.getPartitionId(), collection.getUniqueId());
	}
	
	@Test
	public void canShareReference() throws CollectionNotFoundException, ReferenceNotFoundException, ReferenceSharingException, ReferenceModificationException, DocumentTypeNotFoundException, ObjectLockedException, InvalidCatalogNameException, CollectionModificationException{
		String owner = "OWNER_canShareReference";
		String applicationName = "APPLICATION_NAME_canShareReference";
		
		CatalogCollection applicationCollection = catalogService.getRootApplicationCollectionForUser(owner, partitionId, applicationName);
		
		Set<Long> permissionIds = new HashSet<Long>();
		permissionIds.add(Permission.EDIT_PERMISSION_ID);
		permissionIds.add(Permission.SHARE_PERMISSION_ID);
		
		String documentName = "NEW_DOCUMENT_NAME_canShareReference";
		
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("MY_ATTR_1_canShareReference", "SOME VALUE1_canShareReference");
		attributes.put("MY_ATTR_2_canShareReference", "SOME VALUE1_canShareReference");
		Reference ref = catalogService.addDocument(owner, partitionId, documentName, applicationCollection.getUniqueId(), "NEW_PAYLOAD_canShareReference".getBytes(), permissionIds, null);
		ref= catalogService.modifyReference(owner, partitionId, ref.getUniqueId(), null, null, attributes);
		assertEquals(ref.getParentCollection().getUniqueId(), applicationCollection.getUniqueId());
		
		Map<String, String> userAndPartitionsToShareWith = new HashMap<String, String>();
		
		String otherOwner = "NEWOWNER_canShareReference";
		userAndPartitionsToShareWith.put(otherOwner, partitionId);
		
		Set<Long> sharePermissionIds =  new HashSet<Long>();
		sharePermissionIds.add(Permission.EDIT_PERMISSION_ID);
		
		catalogService.shareReference(owner, partitionId, ref.getUniqueId(), applicationName, userAndPartitionsToShareWith, sharePermissionIds);
		
		CatalogCollection sharedCollection = catalogService.getApplicationShareCollectionForUser(otherOwner, partitionId, applicationName);
		assertEquals(sharedCollection.getReferences().size(), 1);
		assertEquals(sharedCollection.getPermissions().size(), 2);
		
		Reference sharedReference = sharedCollection.getReferences().iterator().next();
		assertEquals(sharedReference.getPermissions().size(), 1);
		assertEquals(sharedReference.getAttributes().size(), 2);
		assertEquals(sharedReference.getName(), ref.getName());
		
		assertEquals(sharedReference.getSharedFrom().getUniqueId(), ref.getUniqueId());
		
		ref = catalogService.modifyReference(owner, partitionId, ref.getUniqueId(), "NEW_NAME_PERSIST", null, attributes);
		
		sharedReference = catalogService.getReference(otherOwner, partitionId, sharedReference.getUniqueId());
		
		assertEquals(ref.getName(), "NEW_NAME_PERSIST");
		assertEquals(sharedReference.getName(), ref.getName());
		
		sharedReference = catalogService.modifyReference(otherOwner, partitionId, sharedReference.getUniqueId(), "NEW_NAME_OWNER2_PERSIST", null, null);
		ref = catalogService.getReference(owner, partitionId, ref.getUniqueId());
		
		assertEquals(ref.getName(), "NEW_NAME_OWNER2_PERSIST");
		assertEquals(sharedReference.getName(), ref.getName());
		
		catalogService.deleteCollection(sharedCollection.getOwner(),sharedCollection.getPartitionId(), sharedCollection.getUniqueId());
		catalogService.deleteCollection(otherOwner, partitionId, catalogService.getRootApplicationCollectionForUser(otherOwner, partitionId, applicationName).getUniqueId());
	}
	
	
	@Test(expected=CollectionSharingException.class)
	public void canNotShareThisCollection() throws CollectionNotFoundException, CollectionModificationException, CollectionSharingException, ObjectLockedException, InvalidCatalogNameException{
		String owner = "OWNER_canNotShareThisCollection";
		String application = "APPLICATION_NAME_canNotShareThisCollection";
		
		CatalogCollection parentCollection = catalogService.getRootApplicationCollectionForUser(owner, partitionId, application);
		
		Set<Long> permissionIds = new HashSet<Long>();
		permissionIds.add(Permission.EDIT_PERMISSION_ID);
		
		String newCollectionToShareNotEnoughPermissions = "NEW_COLLECTION_CANT_SHARE_THIS_canNotShareThisCollection";
		CatalogCollection collection = catalogService.addCollection(owner, partitionId, newCollectionToShareNotEnoughPermissions, parentCollection.getUniqueId(), permissionIds, null);
		Map<String, String> userAndPartitionsToShareWith = new HashMap<String, String>();
		
		userAndPartitionsToShareWith.put("OWNER_CANT_RECIEVE_canNotShareThisCollection", partitionId);
		
		Set<Long> sharePermissionIds = new HashSet<Long>();
		sharePermissionIds.add(Permission.EDIT_PERMISSION_ID);
		catalogService.shareCollection(owner, partitionId, application, collection.getUniqueId(), userAndPartitionsToShareWith, sharePermissionIds);
	}
	
	@Test(expected=CollectionNotFoundException.class)
	public void uniqueCollectionNotFound() throws CollectionNotFoundException{
		String owner = "OWNER_uniqueCollectionNotFound";
		String collectionName = "MY_COLLECTION_NAME_uniqueCollectionNotFound";
		
		CatalogCollection c = new CatalogCollection(owner, partitionId, collectionName);
		c.getPermissions().add((Permission)catalogDao.findById(Permission.class, Permission.EDIT_PERMISSION_ID));
		catalogDao.findById(CatalogCollection.class, catalogDao.save(c));
		
		String someUUIDThatDoesNotMatch = UUID.randomUUID().toString();
		
		catalogService.getCollection(owner, partitionId, someUUIDThatDoesNotMatch);
	}
	
	@Test
	public void uniqueCollection() throws CollectionNotFoundException{
		String owner = "OWNER_uniqueCollection";
		String collectionName = "MY_COLLECTION_uniqueCollection";
		
		CatalogCollection c = new CatalogCollection(owner, partitionId, collectionName);
		c.getPermissions().add((Permission)catalogDao.findById(Permission.class, Permission.EDIT_PERMISSION_ID));
		c = (CatalogCollection)catalogDao.findById(CatalogCollection.class, catalogDao.save(c));
		
		String someUUIDThatDoesMatch = c.getUniqueId();
		CatalogCollection newCollection = catalogService.getCollection(owner, partitionId, someUUIDThatDoesMatch);
		assertEquals(newCollection.getUniqueId(), someUUIDThatDoesMatch);
		assertEquals(newCollection.getName(), collectionName);
	}
	
	@Test
	public void referencdCollection(){
		String owner = "OWNER_referencdCollection";
		String collectionName = "COLLECTION_NAME_referencdCollection";
		CatalogCollection c = new CatalogCollection(owner, partitionId, collectionName);
		c.getPermissions().add((Permission)catalogDao.findById(Permission.class, Permission.EDIT_PERMISSION_ID));
		c = (CatalogCollection)catalogDao.findById(CatalogCollection.class, catalogDao.save(c));
		
		Document doc = new Document(collectionName, owner, partitionId, "NEW_PAYLOAD_referencdCollection".getBytes());
		doc = (Document)catalogDao.findById(Document.class, catalogDao.save(doc));
		Reference r = new Reference(owner, partitionId, doc.getName(), doc, c, null);
		r = (Reference)catalogDao.findById(Reference.class, catalogDao.save(r));
		c.getReferences().add(r);
		
		c = (CatalogCollection)catalogDao.findById(CatalogCollection.class, c.getId());
		
		assertEquals(c.getReferences().size(), 1);
		Document d = c.getReferences().iterator().next().getDocument();
		catalogDao.refresh(d);
		assertEquals(d.getReferenceCount(), 1);
		Reference newRef = new Reference(owner, partitionId, doc.getName(), doc, c, null);
		newRef.getPermissions().add((Permission)catalogDao.findById(Permission.class, Permission.EDIT_PERMISSION_ID));
		newRef = (Reference)catalogDao.findById(Reference.class, catalogDao.save(newRef));
		c.getReferences().add(newRef);
		catalogDao.update(c);
		c = (CatalogCollection)catalogDao.findById(CatalogCollection.class, c.getId());
		assertEquals(c.getReferences().size(), 2);
		d = c.getReferences().iterator().next().getDocument();
		catalogDao.refresh(d);
		assertEquals(d.getReferenceCount(), 2);
		
		c.getReferences().remove(newRef);
		catalogDao.delete(newRef);
		catalogDao.flush();
		
		c = (CatalogCollection)catalogDao.findById(CatalogCollection.class, c.getId());
		assertEquals(c.getReferences().size(), 1);
		
		
		d = c.getReferences().iterator().next().getDocument();
		catalogDao.refresh(d);
		assertEquals(d.getReferenceCount(), 1);
		
		catalogDao.merge(c);
		
		catalogDao.refresh(c);
		catalogDao.refresh(r);
		
		assertEquals(c.getReferences().size(), 1);
		assertEquals(c, r.getParentCollection());
		
		c.getReferences().remove(r);
		catalogDao.delete(r);
		
		catalogDao.flush();
		
		c = (CatalogCollection)catalogDao.findById(CatalogCollection.class, c.getId());
		assertEquals(c.getReferences().size(), 0);
		
		catalogDao.refresh(d);
		assertEquals(d.getReferenceCount(), 0);
	}
	
	@Test
	public void nestedCollections() throws InvalidCatalogNameException{
		String owner = "OWNER_nestedCollections";
		String applicationName = "APPLICATION_NAME_nestedCollections";
		
		CatalogCollection c = new CatalogCollection(owner, partitionId, applicationName);
		Set<CatalogCollection> newCols = createNestedCollections(c);
		c.getNestedCollections().addAll(newCols);
		
		c = (CatalogCollection)catalogDao.findById(CatalogCollection.class, catalogDao.save(c));
		
		assertEquals(c.getNestedCollections().size(), newCols.size());
		
		for(CatalogCollection nec : c.getNestedCollections()){
			assertEquals(nec.getOwner(), c.getOwner());
			assertNotSame(nec.getUniqueId(), c.getUniqueId());
			assertEquals(nec.getParentCollection(), c);
		}
		
		c.getNestedCollections().removeAll(newCols);
		
		catalogDao.update(c);
		catalogDao.flush();
		catalogDao.refresh(c);
		
		assertEquals(c.getNestedCollections().size(), 0);
		
		newCols = createNestedCollections(c);
		
		catalogDao.update(c);
		catalogDao.refresh(c);
		
		for(CatalogCollection col : newCols){
			col.setNestedCollections(createNestedCollections(col));
			catalogDao.update(col);
		}
		
		catalogDao.update(c);
		catalogDao.refresh(c);
		
		CatalogServiceIntegrationTest.displayNestedCollections(c, "");
		
		for(CatalogCollection nc : c.getNestedCollections()){
			assertEquals(nc.getNestedCollections().size(), newCols.size());
			assertEquals(nc.getParentCollection(), c);
			for(CatalogCollection nnc : nc.getNestedCollections()){
				assertEquals(nnc.getParentCollection(), nc);
			}
		}
		catalogDao.update(c);
		catalogDao.refresh(c);
		assertEquals(c.getNestedCollections().size(), newCols.size());
	}
	
	@Test
	public void cascadeOfDocumentAndReference(){
		String owner = "OWNER_cascadeOfDocumentAndReference";
		
		String documentName = "DOCUMENT_NAME_cascadeOfDocumentAndReference";
		
		String collectionName = "COLLECTION_NAME_cascadeOfDocumentAndReference";
		CatalogCollection owningCollection = new CatalogCollection(owner, partitionId, collectionName);
		owningCollection.getPermissions().add((Permission)catalogDao.findById(Permission.class, Permission.EDIT_PERMISSION_ID));
		owningCollection = (CatalogCollection)catalogDao.findById(CatalogCollection.class, catalogDao.save(owningCollection));
		
		Document doc = (Document)catalogDao.findById(Document.class, catalogDao.save(new Document(documentName, owner, partitionId, "payload_cascadeOfDocumentAndReference".getBytes())));
		
		Reference r = new Reference(owner, partitionId, doc.getName(), doc, owningCollection, null);
		r.getPermissions().add((Permission)catalogDao.findById(Permission.class, Permission.EDIT_PERMISSION_ID));
		owningCollection.getReferences().add(r);
		catalogDao.update(owningCollection);
		r = (Reference)catalogDao.findById(Reference.class, catalogDao.save(r));
		catalogDao.delete(r);
		catalogDao.delete(owningCollection);
	}
	
	public static void displayNestedCollections(CatalogCollection root, String level) {
		LOG.log(Level.ALL, level + " col id : " + root.getUniqueId() + " parent col id : " + (root.getParentCollection() != null ? root.getParentCollection().getUniqueId() : " is root"));
		level += " -- ";
		for(CatalogCollection c : root.getNestedCollections()){
			if(c.getNestedCollections().size() == 0){
				LOG.log(Level.ALL, level + " col id : " + c.getUniqueId() + " parent col id : " + (c.getParentCollection() != null ? c.getParentCollection().getUniqueId() : " root"));
			} else {
				displayNestedCollections(c, level);
			}
		}
	}

	private Set<CatalogCollection> createNestedCollections(CatalogCollection parentCollection) throws InvalidCatalogNameException {
		Set<CatalogCollection> nestedCols = new HashSet<CatalogCollection>();
		CatalogCollection col1 = new CatalogCollection(parentCollection.getOwner(), parentCollection.getPartitionId(), "NAME1_createNestedCollections", parentCollection.getPermissions(), parentCollection);
		catalogDao.save(col1);
		CatalogCollection col2 = new CatalogCollection(parentCollection.getOwner(), parentCollection.getPartitionId(), "NAME2_createNestedCollections", parentCollection.getPermissions(), parentCollection);
		catalogDao.save(col2);
		CatalogCollection col3 = new CatalogCollection(parentCollection.getOwner(), parentCollection.getPartitionId(), "NAME3_createNestedCollections", parentCollection.getPermissions(), parentCollection);
		catalogDao.save(col3);
		CatalogCollection col4 = new CatalogCollection(parentCollection.getOwner(), parentCollection.getPartitionId(), "NAME4_createNestedCollections", parentCollection.getPermissions(), parentCollection);
		catalogDao.save(col4);
		CatalogCollection col5 = new CatalogCollection(parentCollection.getOwner(), parentCollection.getPartitionId(), "NAME5_createNestedCollections", parentCollection.getPermissions(), parentCollection);
		catalogDao.save(col5);
		CatalogCollection col6 = new CatalogCollection(parentCollection.getOwner(), parentCollection.getPartitionId(), "NAME6_createNestedCollections", parentCollection.getPermissions(), parentCollection);
		catalogDao.save(col6);
		CatalogCollection col7 = new CatalogCollection(parentCollection.getOwner(), parentCollection.getPartitionId(), "NAME7_createNestedCollections", parentCollection.getPermissions(), parentCollection);
		catalogDao.save(col7);
		nestedCols.add(col1);
		nestedCols.add(col2);
		nestedCols.add(col3);
		nestedCols.add(col4);
		nestedCols.add(col5);
		nestedCols.add(col6);
		nestedCols.add(col7);
		return nestedCols;
	}
	
	@Test
	public void testSearchForAtributes()
			throws CollectionNotFoundException, ReferenceModificationException, ReferenceNotFoundException, CollectionModificationException, ObjectLockedException, InvalidCatalogNameException {
		
		String owner = "testSearchesForAttributes";
		
		Map<String, String> attr = new HashMap<String, String>();
		for(int i = 0; i< 10;i++){
			attr.put("SOMEKEY"+i, "SOMEVALUE"+i);
		}
		
		CatalogCollection userRoot = catalogService.getRootCollectionForUser(owner, partitionId);
		
		Set<Long> sharePermissionIds = new HashSet<Long>();
		sharePermissionIds.add(Permission.EDIT_PERMISSION_ID);
		
		CatalogCollection insideRoot = catalogService.addCollection(owner, partitionId, "NEW COLLECTION FOR SEARCH", userRoot.getUniqueId(), sharePermissionIds, attr);
		Reference ref = new Reference();
		for(int i =0;i<10;i++){
			ref = catalogService.addDocument(owner, partitionId, "DOCNAME", userRoot.getUniqueId(), "<XML>MY PAYLOAD</XML>".getBytes(), sharePermissionIds , attr);
			// Just add attributes to the ref...
			ref = catalogService.modifyReference(owner, partitionId, ref.getUniqueId(), null, null, attr);
		}
		
		
		Map<String, String> attributes = new HashMap<String, String>();
		for(int i = 0; i< 5;i++){
			attributes.put("SOMEKEY"+i, "SOMEVALUE"+i);
		}
		catalogService.modifyDocumentByReferenceId(owner, partitionId, ref.getUniqueId(), null, null, null, null, null, null, null, attributes);
		
		for(int i =0;i<10;i++){
			ref = catalogService.addDocument(owner, partitionId, "DOCNAME", insideRoot.getUniqueId(), "<XML>MY PAYLOAD</XML>".getBytes(), sharePermissionIds , attr);
			// Just add attributes to the ref...
			ref = catalogService.modifyReference(owner, partitionId, ref.getUniqueId(), null, null, attr);
		}
		// TODO : Break this test into smaller parts, one for ref search one for doc search, one for collection search
		
		userRoot = catalogService.getRootCollectionForUser(owner, partitionId);
		List<String> attributeKeys = new ArrayList<String>();
		attributeKeys.add("SOMEKEY1");
		attributeKeys.add("SOMEKEY2");
		attributeKeys.add("SOMEKEY0");
		attributeKeys.add("SOMEKEY3");
		attributeKeys.add("SOMEKEY4");

		catalogDao.flush();
		this.flush=false;
		Set<Document> docS2 = catalogSearchService.findDocumentsWithAttributesKey(owner, partitionId, userRoot.getUniqueId(), true, attributeKeys , true);
		assertEquals(docS2.size(), 20);
		
		attributeKeys.add("SOMEKEY9NOMATCH");
		attributeKeys.add("SOMEKEY19NOMATCH");
		attributeKeys.add("SOMEKEY29NOMATCH");
		attributeKeys.add("SOMEKEY39NOMATCH");
		attributeKeys.add("SOMEKEY49NOMATCH");
		
		docS2 = catalogSearchService.findDocumentsWithAttributesKey(owner, partitionId, userRoot.getUniqueId(), true, attributeKeys , false);
		assertEquals(docS2.size(), 20);
		
		attributeKeys = new ArrayList<String>();
		attributeKeys.add("SOMEKEY9");
		attributeKeys.add("SOMEKEY8");
		
		docS2 = catalogSearchService.findDocumentsWithAttributesKey(owner, partitionId, userRoot.getUniqueId(), true, attributeKeys , true);
		assertEquals(docS2.size(), 19);
		
		attributeKeys = new ArrayList<String>();
		attributeKeys.add("SOMEKEY9");
		attributeKeys.add("SOMEKEY8");
		
		docS2 = catalogSearchService.findDocumentsWithAttributesKey(owner, partitionId, userRoot.getUniqueId(), false, attributeKeys , true);
		assertEquals(docS2.size(), 9);
		
		attributeKeys.add("SOMEKEY9NOMATCH");
		attributeKeys.add("SOMEKEY19NOMATCH");
		attributeKeys.add("SOMEKEY29NOMATCH");
		attributeKeys.add("SOMEKEY39NOMATCH");
		attributeKeys.add("SOMEKEY49NOMATCH");
		docS2 = catalogSearchService.findDocumentsWithAttributesKey(owner, partitionId, userRoot.getUniqueId(), true, attributeKeys , true);
		assertEquals(docS2.size(), 0);

		List<String> attributeValues = new ArrayList<String>();
		attributeValues.add("SOMEVALUE9");
		docS2 = catalogSearchService.findDocumentsWithAttributesValue(owner, partitionId, userRoot.getUniqueId(), true, attributeValues , true);
		assertEquals(docS2.size(), 19);

		Map<String, String> keyAndValue = new HashMap<String, String>();
		keyAndValue.put("SOMEKEY1", "SOMEVALUE1");
		keyAndValue.put("SOMEKEY2", "SOMEVALUE2");
		Set<Document> minD = new HashSet<Document>();
		minD = catalogSearchService.findDocumentsWithAttributesKeyAndValue(owner, partitionId, userRoot.getUniqueId(), true, keyAndValue, true);
		assertEquals(minD.size(), 20);
		
		minD = catalogSearchService.findDocumentsWithAttributesKeyAndValue(owner, partitionId, userRoot.getUniqueId(), false, keyAndValue, true);
		assertEquals(minD.size(), 10);
		
		keyAndValue = new HashMap<String, String>();
		keyAndValue.put("SOMEKEY1", "SOMEVALUE1");
		keyAndValue.put("SOMEKEY6", "SOMEVALUE6");
		minD = new HashSet<Document>();
		minD = catalogSearchService.findDocumentsWithAttributesKeyAndValue(owner, partitionId, userRoot.getUniqueId(), true, keyAndValue, true);
		assertEquals(minD.size(), 19);
		
		minD = catalogSearchService.findDocumentsWithAttributesKeyAndValue(owner, partitionId, userRoot.getUniqueId(), false, keyAndValue, true);
		assertEquals(minD.size(), 9);
		
		keyAndValue = new HashMap<String, String>();
		keyAndValue.put("SOMEKEY1", "SOMEVALUE1");
		keyAndValue.put("SOMEKEY2NOMATCH", "SOMEVALUE2NOTMATCH");
		keyAndValue.put("SOMEKEY3NOMATCH", "SOMEVALUE2NOTMATCH");
		keyAndValue.put("SOMEKEY4NOMATCH", "SOMEVALUE2NOTMATCH");
		keyAndValue.put("SOMEKEY5NOMATCH", "SOMEVALUE2NOTMATCH");
		keyAndValue.put("SOMEKEY6NOMATCH", "SOMEVALUE2NOTMATCH");
		minD = catalogSearchService.findDocumentsWithAttributesKeyAndValue(owner, partitionId, userRoot.getUniqueId(), true, keyAndValue, false);
		assertEquals(minD.size(), 20);
		
		minD = catalogSearchService.findDocumentsWithAttributesKeyAndValue(owner, partitionId, userRoot.getUniqueId(), false, keyAndValue, false);
		assertEquals(minD.size(), 10);
		
		keyAndValue = new HashMap<String, String>();
		keyAndValue.put("SOMEKEY1", "SOMEVALUE1");
		keyAndValue.put("SOMEKEY2NOMATCH", "SOMEVALUE2NOTMATCH");
		minD = catalogSearchService.findDocumentsWithAttributesKeyAndValue(owner, partitionId, userRoot.getUniqueId(), true, keyAndValue, true);
		assertEquals(minD.size(), 0);
		
		Set<Reference> minR = catalogSearchService.findReferencesWithAttributesKeyAndValue(owner, partitionId, userRoot.getUniqueId(), false, keyAndValue, false);
		assertEquals(minR.size(), 10);
		
		Set<CatalogCollection> minC = catalogSearchService.findCollectionsWithAttributesKeyAndValue(owner, partitionId, userRoot.getUniqueId(), false, keyAndValue, false);
		assertEquals(minC.size(), 1);
		
		minC = catalogSearchService.findCollectionsWithAttributesKeyAndValue(owner, partitionId, userRoot.getUniqueId(), false, keyAndValue, true);
		assertEquals(minC.size(), 0);
	}
	
	@Test
	public void updateTimeStampOnSaveOrUpdate() throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException{
		
		String owner="updateTimeStampOnSaveOrUpdate";
		String application = "updateTimeStampOnSaveOrUpdate";
		
		CatalogCollection catalogCollection = catalogService.getRootApplicationCollectionForUser(owner, partitionId, application);
		CatalogCollection newCollection = catalogService.addCollection(owner, partitionId, "cname", catalogCollection.getUniqueId(), null, null);
		Date lastModDate = newCollection.getLastModifiedDate();
		
		CatalogCollection modifiedCollection = catalogService.modifyCollection(owner, partitionId, newCollection.getUniqueId(), "new Name", null, null, null);
		
		assertNotSame(lastModDate, modifiedCollection.getLastModifiedDate());
		
	}
	
	@Test
	public void cacheTest(){
		String owner="cacheTest";
		String application = "cacheTest";
		LOG.info("GET ROOT FIRST TIME");
		catalogService.getRootApplicationCollectionForUser(owner, partitionId, application);
		LOG.info("GET ROOT SECOND TIME");
		catalogService.getRootApplicationCollectionForUser(owner, partitionId, application);
	}
	
	@Test
	public void testUniqueNameSameLevel() throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException{
		this.flush = false;
		String owner = "testUniqueNameSameLevelOwner";
		String application = "testUniqueNameSameLevelApp";
		CatalogCollection catalogCollection = catalogService.getRootApplicationCollectionForUser(owner, partitionId, application);
		catalogService.addCollection(owner, partitionId, "SameName", catalogCollection.getUniqueId(), null, null);
		//This should throw and error...
		try{
			catalogService.addCollection(owner, partitionId, "SameName", catalogCollection.getUniqueId(), null, null);
		} catch(Exception e){
			return;
		}
		fail("Method should have caught a integrity constraint error and exited!");
	}
	
	@Test(expected=InvalidCatalogNameException.class)
	public void testInvalidName() throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException{
		
		String owner = "testInvalidName";
		String application = "testInvalidName";
		CatalogCollection catalogCollection = catalogService.getRootApplicationCollectionForUser(owner, partitionId, application);
		catalogService.addCollection(owner, partitionId, "Same!Name", catalogCollection.getUniqueId(), null, null);
	}
	
	@Test
	public void cacheExt1(){
		String owner = "cacheTestExt1";
		String application = "cacheTestExt1App";
		catalogService.getRootApplicationCollectionForUser(owner, partitionId, application);
	}
	
	@Test
	public void cacheExt2() throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException{
		String owner = "cacheTestExt1";
		String application = "cacheTestExt1App";
		CatalogCollection cc = catalogService.getRootApplicationCollectionForUser(owner, partitionId, application);
		CatalogCollection newCC = catalogService.addCollection(owner, partitionId, "My New Name_Cool", cc.getUniqueId(), null, null);
		assertEquals(cc.getParentCollection().getParentCollection().getName(),"Users");
		assertEquals(cc.getParentCollection().getParentCollection().getPath(),"/Users/".toLowerCase());
		assertEquals(cc.getParentCollection().getPath(),"/Users/cacheTestExt1/".toLowerCase());
		assertEquals(newCC.getPath(), "/Users/cacheTestExt1/cacheTestExt1App/My_New_Name_Cool/".toLowerCase());
		
		Reference r = catalogService.addDocument(owner, partitionId, "docName", newCC.getUniqueId(), "".getBytes(), null, null);
		assertEquals(r.getPath(), "/Users/cacheTestExt1/cacheTestExt1App/My_New_Name_Cool/docName".toLowerCase());
	}
	
//	@Test
//	public void testPath() throws CollectionNotFoundException, InvalidCollectionPathException{
//		String owner = "testPath";
//		
//		List<Long> permissions = new ArrayList<Long>();
//		permissions.add(Permission.EDIT_PERMISSION_ID);
//		permissions.add(Permission.ADD_PERMISSION_ID);
////		catalogService.getCollectionByPath(owner, partitionId, "/users/catalog/", true, permissions);
//	}
	
	@Test
	public void addCollectionToSharedCollection() throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException, CollectionSharingException{
		this.flush = false;
		String owner = "addCollectionToSharedCollection";
		String application = "addCollectionToSharedCollection";
		
		CatalogCollection rootUserCollection = catalogService.getRootApplicationCollectionForUser(owner, partitionId, application);
		Set<Long> sharePermissionIds = new HashSet<Long>();
		sharePermissionIds.add(Permission.SHARE_PERMISSION_ID);
		sharePermissionIds.add(Permission.EDIT_PERMISSION_ID);
		sharePermissionIds.add(Permission.ADD_PERMISSION_ID);
		CatalogCollection newCollectionToShare = catalogService.addCollection(owner, partitionId, "New Collection", rootUserCollection.getUniqueId(), sharePermissionIds , null);
		
		Map<String, String> userAndPartitionsToShareWith = new HashMap<String, String>();
		userAndPartitionsToShareWith.put("newOwner", partitionId);
		
		catalogService.shareCollection(owner, partitionId, application, newCollectionToShare.getUniqueId(), userAndPartitionsToShareWith , sharePermissionIds);
		Map<String, String> someAttributes = new HashMap<String, String>();
		someAttributes.put("key1", "value1");
		someAttributes.put("key2", "value1");
		catalogService.addCollection(owner, partitionId, "New Collection in a shared Colelction", newCollectionToShare.getUniqueId(), sharePermissionIds, someAttributes );
		CatalogCollection newOwnerShareRoot = catalogService.getApplicationShareCollectionForUser("newOwner", partitionId, application);
		assertEquals(newOwnerShareRoot.getNestedCollections().size(), 1);
		assertEquals(newOwnerShareRoot.getNestedCollections().iterator().next().getNestedCollections().size(), 1);
		assertEquals(newOwnerShareRoot.getNestedCollections().iterator().next().getNestedCollections().iterator().next().getAttributes().size(), 2);
	}
	
	@Test(expected=CollectionModificationException.class)
	public void canNotCloneCollection() throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException{
		String owner = "canNotCloneCollection";
		String application = "canNotCloneCollection";
		
		CatalogCollection newOwnerShareRoot = catalogService.getRootApplicationCollectionForUser(owner, partitionId, application);
		CatalogCollection nonCloneableCollection = catalogService.addCollection(owner, partitionId, "NewNonCloneableCollection", newOwnerShareRoot.getUniqueId(), null , null);
		catalogService.cloneCollection(owner, partitionId, nonCloneableCollection.getUniqueId());
	}
}
