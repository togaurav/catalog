package com.c9a.catalog.entities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Where;

import com.c9a.catalog.exception.InvalidCatalogNameException;

@Entity
@Table(name="collection", uniqueConstraints={@UniqueConstraint(columnNames={"unique_id"}), @UniqueConstraint(columnNames={"name", "parent_collection_id"})})
@org.hibernate.annotations.Table(appliesTo = "collection", 
		indexes = {
        @Index(name = "idx_col_owner",
                columnNames = {"owner_id"}
        ),
        @Index(name = "idx_col_name",
                columnNames = {"name"}
        )
})
@CatalogPathCache
public class CatalogCollection extends CatalogPathObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1524825300243844277L;
	
	public static final String SHARE_COLLECTION_NAME = "Shared With Me";
	public static final String ROOT_COLLECTION_NAME = "Root Collection";
	public static final String SYSTEM_OWNER = "System";
	public static final String USERS_COLLECTION_NAME = "Users";

	public CatalogCollection(){}
	
	/**
	 * 
	 * @param owner
	 * @param partitionId
	 * @param name
	 */
	public CatalogCollection(String owner, String partitionId, String name) {
		this.owner = owner;
		this.partitionId = partitionId;
		this.name = name;
	}
	
	/**
	 * 
	 * @param owner
	 * @param partitionId
	 * @param name
	 * @param parentCollection
	 * @param permissions
	 * @throws InvalidCatalogNameException 
	 */
	public CatalogCollection(String owner, String partitionId, String name, Collection<Permission> permissions, CatalogCollection parentCollection) {
		this.owner = owner;
		this.partitionId = partitionId;
		this.name = name;
		if(permissions!=null){
			Set<Permission> p = new HashSet<Permission>();
			p.addAll(permissions);
			this.permissions = p;
			
		}
		this.parentCollection = parentCollection;
	}
	
	@Id
	@GeneratedValue
	@Column(name="collection_id")
	private Long id;
	
	@ManyToOne
	@JoinColumn(name="parent_collection_id")
	@ForeignKey(name="fk_parent_collection_id")
	private CatalogCollection parentCollection;
	
	@OneToMany(mappedBy="parentCollection", orphanRemoval = true)
	@Where(clause="archived=0")
	private Set<CatalogCollection> nestedCollections;
	
	@OneToMany(mappedBy="parentCollection", orphanRemoval = true)
	@Where(clause="archived=1")
	private Set<CatalogCollection> nestedArchivedCollections;
	
	@ManyToOne
	@ForeignKey(name="fk_cf_collection_id")
	@JoinColumn(name="created_from_collection_id")
	private CatalogCollection createdFrom;
	
	@OneToMany(mappedBy = "createdFrom")
	private Set<CatalogCollection> createdTo;
	
	@OneToMany(mappedBy = "parentCollection", cascade = CascadeType.ALL, orphanRemoval = true)
	@Where(clause="archived=0")
	private Set<Reference> references;
	
	@OneToMany(mappedBy = "parentCollection", cascade = CascadeType.ALL, orphanRemoval = true)
	@Where(clause="archived=1")
	private Set<Reference> archivedReferences;
	
	@ManyToMany
	@JoinTable(
	        name="collection_permission",
	        joinColumns=@JoinColumn(name="colelction_id"),
	        inverseJoinColumns=@JoinColumn(name="permission_id")
	)
	@ForeignKey(name="fk_col_permission", inverseName="fk_permission_col")
	private Set<Permission> permissions;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name="collection_id")
	private Set<CollectionAttribute> attributes;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public CatalogCollection getParentCollection() {
		return parentCollection;
	}

	public void setParentCollection(CatalogCollection parentCollection) {
		this.parentCollection = parentCollection;
	}

	public Set<CatalogCollection> getNestedCollections() {
		if(nestedCollections == null)
			nestedCollections = new HashSet<CatalogCollection>();
		return nestedCollections;
	}

	public void setNestedCollections(Set<CatalogCollection> nestedCollections) {
		this.nestedCollections = nestedCollections;
	}

	public Set<CatalogCollection> getNestedArchivedCollections() {
		if(nestedArchivedCollections == null)
			nestedArchivedCollections = new HashSet<CatalogCollection>();
		return nestedArchivedCollections;
	}

	public void setNestedArchivedCollections(
			Set<CatalogCollection> nestedArchivedCollections) {
		this.nestedArchivedCollections = nestedArchivedCollections;
	}

	public CatalogCollection getCreatedFrom() {
		return createdFrom;
	}

	public void setCreatedFrom(CatalogCollection createdFrom) {
		this.createdFrom = createdFrom;
	}

	public Set<CatalogCollection> getCreatedTo() {
		if(createdTo == null){
			createdTo = new HashSet<CatalogCollection>();
		}
		return createdTo;
	}

	public void setCreatedTo(Set<CatalogCollection> createdTo) {
		this.createdTo = createdTo;
	}

	public CatalogCollection getSharedFrom() {
		if(createdFrom != null && !createdFrom.getOwner().equals(owner)){
			return createdFrom;
		}
		return null;
	}

	public Set<CatalogCollection> getSharedTo() {
		Set<CatalogCollection> sharedTo = new HashSet<CatalogCollection>();
		if(createdTo == null){
			return sharedTo;
		}
		
		for (CatalogCollection cc : getCreatedTo()) {
			if (!cc.getOwner().equals(owner)) {
				sharedTo.add(cc);
			}
		}
		return sharedTo;
	}

	public Set<Reference> getReferences() {
		if(references == null)
			references = new HashSet<Reference>();
		return references;
	}

	public void setReferences(Set<Reference> references) {
		this.references = references;
	}

	public Set<Reference> getArchivedReferences() {
		if(archivedReferences == null){
			archivedReferences = new HashSet<Reference>();
		}
		return archivedReferences;
	}

	public void setArchivedReferences(Set<Reference> archivedReferences) {
		this.archivedReferences = archivedReferences;
	}

	public Set<Permission> getPermissions() {
		if(permissions == null)
			permissions = new HashSet<Permission>();

		return permissions;
	}

	public void setPermissions(Set<Permission> permissions) {
		this.permissions = permissions;
	}

	public Set<CollectionAttribute> getAttributes() {
		if(attributes == null)
			attributes = new HashSet<CollectionAttribute>();
		
		return attributes;
	}

	@Override
	public void addAttribute(ExtendedAttribute attribute) {
		for(ExtendedAttribute ca : getAttributes()){
			if(ca.getKey().toLowerCase().equals(attribute.getKey().toLowerCase())){
				ca.setValue(attribute.getValue().toLowerCase());
				return;
			}
		}
		getAttributes().add((CollectionAttribute)attribute);
	}

	@Override
	protected String generatePath() {
		String cleanName = name.replaceAll(unSupportedCharRegExForPath, replacementChar).toLowerCase() + "/";
		String genPath = getParentCollection() != null ? getParentCollection().getPath() + cleanName :  "/" + cleanName;
		return genPath;
	}
}