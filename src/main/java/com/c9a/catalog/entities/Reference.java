package com.c9a.catalog.entities;

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
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

@Entity
@Table(name="reference", uniqueConstraints=@UniqueConstraint(columnNames={"unique_id"}))
@org.hibernate.annotations.Table(appliesTo = "reference", 
		indexes = {
        @Index(name = "idx_ref_owner",
                columnNames = {"owner_id"}
        ),
        @Index(name = "idx_ref_name",
                columnNames = {"name"}
        )
})
@CatalogPathCache
public class Reference extends CatalogPathObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2873893862625507423L;

	public Reference(){}
	
	public Reference(String owner, String partitionId, String name, Document document, CatalogCollection owningCollection, Set<Permission> permissions) {
		this.owner = owner;
		this.partitionId = partitionId;
		this.document = document;
		this.parentCollection = owningCollection;
		this.name = name;
		if(permissions == null){
			permissions = owningCollection.getPermissions();
		}
		this.getPermissions().addAll(permissions);
	}

	public Reference(String name, Document document, CatalogCollection collection, Set<Permission> permissions) {
		this.owner = collection.getOwner();
		this.partitionId = collection.getPartitionId();
		this.document = document;
		this.name = name;
		this.parentCollection = collection;
		this.getPermissions().addAll(permissions);
	}

	@Id
	@GeneratedValue
	@Column(name="reference_id")
	private Long id;

	@NotNull
	@ManyToOne
	@JoinColumn(name="parent_collection_id")
	@ForeignKey(name="fk_collection_id")
	private CatalogCollection parentCollection;
	
	@ManyToOne
	@ForeignKey(name="fk_cf_reference_id")
	@JoinColumn(name="created_from_reference_id")
	private Reference createdFrom;
	
	@OneToMany(mappedBy = "createdFrom")
	private Set<Reference> createdTo;
	
	@NotNull
	@ManyToOne
	@JoinColumn(name="document_id")
	@ForeignKey(name="fk_doc")
	@org.hibernate.annotations.Cascade(value={org.hibernate.annotations.CascadeType.SAVE_UPDATE,org.hibernate.annotations.CascadeType.MERGE})
	private Document document;
	
	@ManyToMany
	@JoinTable(
	        name="reference_permission",
	        joinColumns=@JoinColumn(name="reference_id"),
	        inverseJoinColumns=@JoinColumn(name="permission_id")
	)
	@ForeignKey(name="fk_ref_permissions", inverseName="fk_permission_ref")
	private Set<Permission> permissions;
	
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name="reference_id")
	@org.hibernate.annotations.Cascade(value={org.hibernate.annotations.CascadeType.ALL})
	private Set<ReferenceAttribute> attributes;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public CatalogCollection getParentCollection() {
		return parentCollection;
	}

	public void setParentCollection(CatalogCollection parentCollection) {
		this.parentCollection = parentCollection;
	}

	public Reference getCreatedFrom() {
		return createdFrom;
	}

	public void setCreatedFrom(Reference createdFrom) {
		this.createdFrom = createdFrom;
	}

	public Reference getSharedFrom() {
		if(createdFrom!=null && !createdFrom.getOwner().equals(owner)){
			return createdFrom;
		}
		return null;
	}

	public Set<Reference> getSharedTo() {
		Set<Reference> sharedTo = new HashSet<Reference>();
		for(Reference r : getCreatedTo()){
			if(!r.getOwner().equals(owner)){
				sharedTo.add(r);
			}
		}
		return sharedTo;
	}

	public Set<Reference> getCreatedTo() {
		if(createdTo == null){
			createdTo = new HashSet<Reference>();
		}
		return createdTo;
	}

	public void setCreatedTo(Set<Reference> createdTo) {
		this.createdTo = createdTo;
	}

	public Set<Permission> getPermissions() {
		if(permissions == null)
			permissions = new HashSet<Permission>();
		return permissions;
	}

	public void setPermissions(Set<Permission> permissions) {
		this.permissions = permissions;
	}

	public Set<ReferenceAttribute> getAttributes() {
		if(attributes == null)
			attributes = new HashSet<ReferenceAttribute>();
		return attributes;
	}

	@Transient
	public String getDocumentName(){
		return this.document.getName();
	}
	
	@Transient
	public String getDocumentDescription(){
		return this.document.getDescription();
	}
	
	@Override
	public void addAttribute(ExtendedAttribute attribute) {
		for(ExtendedAttribute ca : getAttributes()){
			if(ca.getKey().toLowerCase().equals(attribute.getKey().toLowerCase())){
				ca.setValue(attribute.getValue().toLowerCase());
				return;
			}
		}
		ReferenceAttribute refAttribute = (ReferenceAttribute)attribute;
		refAttribute.setReferenceId(getId());
		getAttributes().add(refAttribute);
	}
	
	@Override
	protected String generatePath() {
		String cleanName = name.replaceAll(unSupportedCharRegExForPath, replacementChar).toLowerCase();
		String genPath = getParentCollection() != null ? getParentCollection().getPath() + cleanName :  "/" + cleanName;
		return genPath;
	}
}
