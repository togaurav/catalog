package com.c9a.unit.test;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.entities.CollectionAttribute;
import com.c9a.catalog.entities.Document;
import com.c9a.catalog.entities.DocumentAttribute;
import com.c9a.catalog.entities.ExtendedAttribute;
import com.c9a.catalog.entities.Permission;
import com.c9a.catalog.entities.Reference;
import com.c9a.catalog.entities.ReferenceAttribute;
import com.c9a.catalog.exception.CollectionSharingException;
import com.c9a.catalog.exception.InvalidCatalogNameException;
import com.c9a.catalog.utils.CatalogUtils;

@RunWith(JUnit4.class)
public class CatalogUtilsTest extends TestCase {
	
	@Test
	public void canCloneMethod(){
		Set<Permission> permissions = new HashSet<Permission>();
		assertFalse(CatalogUtils.canClone(permissions));
		permissions.add(getPermissionForId(Permission.EDIT_PERMISSION_ID));
		assertFalse(CatalogUtils.canClone(permissions));
		permissions.add(getPermissionForId(Permission.SHARE_PERMISSION_ID));
		assertFalse(CatalogUtils.canClone(permissions));
		permissions.add(getPermissionForId(Permission.CLONE_PERMISSION_ID));
		assertTrue(CatalogUtils.canClone(permissions));
	}
	
	@Test
	public void canAddMethod(){
		Set<Permission> permissions = new HashSet<Permission>();
		assertFalse(CatalogUtils.canAdd(permissions));
		permissions.add(getPermissionForId(Permission.EDIT_PERMISSION_ID));
		assertFalse(CatalogUtils.canAdd(permissions));
		permissions.add(getPermissionForId(Permission.SHARE_PERMISSION_ID));
		assertFalse(CatalogUtils.canAdd(permissions));
		permissions.add(getPermissionForId(Permission.CLONE_PERMISSION_ID));
		assertFalse(CatalogUtils.canAdd(permissions));
		permissions.add(getPermissionForId(Permission.ADD_PERMISSION_ID));
		assertTrue(CatalogUtils.canAdd(permissions));
	}
	
	@Test
	public void canModifyMethod(){
		Set<Permission> permissions = new HashSet<Permission>();
		assertFalse(CatalogUtils.canModify(permissions));
		permissions.add(getPermissionForId(Permission.SHARE_PERMISSION_ID));
		assertFalse(CatalogUtils.canModify(permissions));
		permissions.add(getPermissionForId(Permission.CLONE_PERMISSION_ID));
		assertFalse(CatalogUtils.canModify(permissions));
		permissions.add(getPermissionForId(Permission.EDIT_PERMISSION_ID));
		assertTrue(CatalogUtils.canModify(permissions));
	}
	
	@Test
	public void canShareFromMethod(){
		Set<Permission> permissions = new HashSet<Permission>();
		assertFalse(CatalogUtils.canShare(permissions));
		permissions.add(getPermissionForId(Permission.EDIT_PERMISSION_ID));
		assertFalse(CatalogUtils.canShare(permissions));
		permissions.add(getPermissionForId(Permission.CLONE_PERMISSION_ID));
		assertFalse(CatalogUtils.canShare(permissions));
		permissions.add(getPermissionForId(Permission.SHARE_PERMISSION_ID));
		assertTrue(CatalogUtils.canShare(permissions));
	}
	
	@Test 
	public void hasAtLeastPermissions(){
		Set<Permission> permissionsToShare = new HashSet<Permission>();
		Set<Permission> permissions = new HashSet<Permission>();
		
		permissions.add(getPermissionForId(Permission.ADD_PERMISSION_ID));
		
		permissionsToShare.add(getPermissionForId(Permission.CLONE_PERMISSION_ID));
		
		assertFalse(CatalogUtils.hasAtLeastPermissions(permissions , permissionsToShare));
		
		permissions.add(getPermissionForId(Permission.CLONE_PERMISSION_ID));
		
		assertTrue(CatalogUtils.hasAtLeastPermissions(permissions , permissionsToShare));
	}
	
	@Test
	public void cloneReference() throws InvalidCatalogNameException{
		String owner = "owner";
		String partitionId = "partitionId";
		String refName = "refName";
		String docName = "docName";
		CatalogCollection owningCollection = new CatalogCollection(owner, partitionId, "name");
		owningCollection.updateTimeStampsAndSetUniqueId();
		Document d = new Document(docName, owner, partitionId, "payload".getBytes());
		Set<Permission> permissions = new HashSet<Permission>();
		permissions.add(getPermissionForId(Permission.EDIT_PERMISSION_ID));
		Reference refToBeCloned = new Reference(owner, partitionId, refName, d, owningCollection,permissions);
		
		ReferenceAttribute refAttr1 = new ReferenceAttribute("KEY1", "VALUE1");
		ReferenceAttribute refAttr2 = new ReferenceAttribute("Key2", "VALUE2");
		refToBeCloned.addAttribute(refAttr1);
		refToBeCloned.addAttribute(refAttr2);
		refToBeCloned.updateTimeStampsAndSetUniqueId();
		
		Reference clonedReference = CatalogUtils.clone(refToBeCloned, null, null);
		clonedReference.updateTimeStampsAndSetUniqueId();
		
		assertSame(clonedReference.getParentCollection(), refToBeCloned.getParentCollection());
		assertSame(clonedReference.getDocument(), refToBeCloned.getDocument());
		assertNotSame("Cloned reference's name should start with Copy of ", clonedReference.getName(), refToBeCloned.getName());
		assertNotSame(clonedReference.getUniqueId(), refToBeCloned.getUniqueId());
		assertEquals(clonedReference.getAttributes().size(), refToBeCloned.getAttributes().size());
		for(ExtendedAttribute ea : clonedReference.getAttributes()){
			assertTrue(refToBeCloned.getAttributes().contains(ea));
		}
	}
	
	@Test
	public void cloneDocument() throws InvalidCatalogNameException{
		String owner = "owner";
		String partitionId = "partitionId";
		String docName = "docName";
		byte[] payLoad = "payload".getBytes();
		
		Document d = new Document(owner, partitionId, docName, payLoad);
		d.setNotes("NOTES");
		d.setDescription("DESC");
		d.addAttribute(new DocumentAttribute("key1", "value1"));
		d.addAttribute(new DocumentAttribute("key2", "value2"));
		d.addAttribute(new DocumentAttribute("key3", "value2"));
		Document clonedDocument = CatalogUtils.clone(d);
		assertNotSame(d.getName(), clonedDocument.getName());
		assertSame(d.getOwner(), clonedDocument.getOwner());
		assertSame(d.getPartitionId(), clonedDocument.getPartitionId());
		assertSame(d.getNotes(), clonedDocument.getNotes());
		assertSame(d.getDescription(), clonedDocument.getDescription());
		assertEquals(clonedDocument.getAttributes().size(), 3);
		
		
	}
	
	@Test
	public void documentAttributes(){
		String owner = "owner";
		String partitionId = "partitionId";
		String docName = "docName";
		byte[] payLoad = "payload".getBytes();
		
		Document d = new Document(owner, partitionId, docName, payLoad);
		d.setNotes("NOTES");
		d.setDescription("DESC");
		d.addAttribute(new DocumentAttribute("key1", "value1"));
		d.addAttribute(new DocumentAttribute("key2", "value2"));
		d.addAttribute(new DocumentAttribute("key3", "value2"));
		assertEquals(d.getAttributes().size(), 3);
		assertEquals(d.getAttributeByKey("key3").getValue(), "value2");
		// Now try to add a attribute that is already there, for that key
		d.addAttribute(new DocumentAttribute("key3", "value3"));
		// Last one in wins
		assertEquals(d.getAttributeByKey("key3").getValue(), "value3");
		
	}
	
	@Test
	public void cloneCollection() throws InvalidCatalogNameException{
		String owner = "owner";
		String partitionId = "partitionId";
		String collectionName = "collectionName";
		Set<Permission> permissions = new HashSet<Permission>();
		permissions.add(getPermissionForId(Permission.EDIT_PERMISSION_ID));
		permissions.add(getPermissionForId(Permission.CLONE_PERMISSION_ID));
		permissions.add(getPermissionForId(Permission.ADD_PERMISSION_ID));
		
		String parentCollectionName = "parentCollection";
		CatalogCollection parentCollection = new CatalogCollection(owner, partitionId, parentCollectionName, permissions, null);
		parentCollection.addAttribute(new CollectionAttribute("key1", "value1"));
		parentCollection.addAttribute(new CollectionAttribute("key2", "value1"));
		CatalogCollection cc = new CatalogCollection(owner, partitionId, collectionName , permissions, parentCollection);
		parentCollection.getNestedCollections().add(cc);
		Document document = new Document("docName", owner, partitionId, "payload".getBytes());
		Reference newReference = new Reference(owner, partitionId, "refName", document, cc, permissions);
		
		cc.getReferences().add(newReference);
		
		CatalogCollection clonedCollection = CatalogUtils.clone(parentCollection, null);
		assertEquals(clonedCollection.getAttributes().size(), 2);
		assertEquals(clonedCollection.getNestedCollections().size(), 1);
		CatalogCollection nestedClonedCollection = clonedCollection.getNestedCollections().iterator().next();
		assertNotSame(clonedCollection.getName(), parentCollection.getName());
		assertNotSame(nestedClonedCollection.getName(), cc.getName());
		
		assertEquals(nestedClonedCollection.getReferences().size(), 1);
		assertSame(nestedClonedCollection.getReferences().iterator().next().getDocument(), cc.getReferences().iterator().next().getDocument());
	}
	
	@Test
	public void rootSharedCollection(){
		String owner = "owner";
		String partitionId = "partitionId";
		Set<Permission> permissions = new HashSet<Permission>();
		permissions.add(getPermissionForId(Permission.EDIT_PERMISSION_ID));
		permissions.add(getPermissionForId(Permission.CLONE_PERMISSION_ID));
		permissions.add(getPermissionForId(Permission.ADD_PERMISSION_ID));
		
		String parentCollectionName = "parentCollection";
		CatalogCollection sharedFromCollection = new CatalogCollection(owner, partitionId, parentCollectionName, permissions, null);
		
		String diffOwner = "diffOwner";
		CatalogCollection sharedToCollection = new CatalogCollection(diffOwner , partitionId, parentCollectionName, permissions, null);
		sharedToCollection.setCreatedFrom(sharedFromCollection);
		sharedFromCollection.getCreatedTo().add(sharedToCollection);
		
		assertEquals(CatalogUtils.getRootSharedCollection(sharedToCollection), sharedFromCollection);
		assertTrue(CatalogUtils.isSharedCollection(sharedToCollection));
		assertTrue(CatalogUtils.isSharedCollection(sharedFromCollection));
	}
	
	@Test
	public void rootSharedReference(){
		String owner = "owner";
		String partitionId = "partitionId";
		Set<Permission> permissions = new HashSet<Permission>();
		permissions.add(getPermissionForId(Permission.EDIT_PERMISSION_ID));
		permissions.add(getPermissionForId(Permission.CLONE_PERMISSION_ID));
		permissions.add(getPermissionForId(Permission.ADD_PERMISSION_ID));
		
		CatalogCollection sharedFromCollection = new CatalogCollection(owner, partitionId, null, permissions, null);
		
		Document document = new Document("docName", owner, partitionId, "payload".getBytes());
		Reference sharedFromReference = new Reference("refName", document, sharedFromCollection, permissions);
		sharedFromReference.setOwner("Owner1");
		
		Reference sharedToReference = new Reference("refName", document, sharedFromCollection, permissions);
		sharedToReference.setOwner("DIFFOwner");
		
		sharedToReference.setCreatedFrom(sharedFromReference);
		sharedFromReference.getCreatedTo().add(sharedToReference);
		
		
		assertEquals(CatalogUtils.getRootSharedReference(sharedToReference), sharedFromReference);
		assertTrue(CatalogUtils.isSharedReference(sharedToReference));
		assertTrue(CatalogUtils.isSharedReference(sharedFromReference));
	}
	
	@Test(expected=CollectionSharingException.class)
	public void collectionCanNotBeShared() throws CollectionSharingException{
		String owner = "owner";
		String partitionId = "partitionId";
		String collectionName = "collectionName";
		Set<Permission> permissions = new HashSet<Permission>();
		permissions.add(getPermissionForId(Permission.EDIT_PERMISSION_ID));
		permissions.add(getPermissionForId(Permission.CLONE_PERMISSION_ID));
		permissions.add(getPermissionForId(Permission.ADD_PERMISSION_ID));
		
		String parentCollectionName = "parentCollection";
		CatalogCollection parentCollection = new CatalogCollection(owner, partitionId, parentCollectionName, permissions, null);
		parentCollection.addAttribute(new CollectionAttribute("key1", "value1"));
		parentCollection.addAttribute(new CollectionAttribute("key2", "value1"));
		CatalogCollection cc = new CatalogCollection(owner, partitionId, collectionName , permissions, parentCollection);
		parentCollection.getNestedCollections().add(cc);
		Document document = new Document("docName", owner, partitionId, "payload".getBytes());
		Reference newReference = new Reference(owner, partitionId, "refName", document, cc, permissions);
		
		cc.getReferences().add(newReference);
		
		Set<Permission> permissionsToShare = new HashSet<Permission>();
		CatalogUtils.validateCollectionCanBeShared(parentCollection, permissionsToShare );
	}
	
	@Test
	public void testPathValidity(){
		assertTrue(CatalogUtils.isValidCollectionPath("//applications//dd/"));
		assertTrue(CatalogUtils.isValidCollectionPath("///sc//asdf/adf//asfdg__sadf/applications////dd////"));
		assertFalse(CatalogUtils.isValidCollectionPath("/applications/dd"));
		assertFalse(CatalogUtils.isValidCollectionPath("/applications/dd!@#/"));
		
		assertTrue(CatalogUtils.isValidReferencePath("//applications//dd"));
		assertTrue(CatalogUtils.isValidReferencePath("///sc//asdf/adf//asfdg__sadf/applications////dd"));
		assertFalse(CatalogUtils.isValidReferencePath("/applications/dd/"));
		assertFalse(CatalogUtils.isValidReferencePath("/applications/dd!@#/"));
	}
	
	

	private Permission getPermissionForId(Long id) {
		Permission permission = new Permission();
		permission.setId(id);
		permission.setDescription(id+"");
		return permission;
	}
}