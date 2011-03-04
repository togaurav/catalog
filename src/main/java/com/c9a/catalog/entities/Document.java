package com.c9a.catalog.entities;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

@Entity
@Table(name="document", uniqueConstraints=@UniqueConstraint(columnNames={"unique_id"}))
@org.hibernate.annotations.Table(appliesTo = "document", 
		indexes = {
        @Index(name = "idx_doc_owner",
                columnNames = {"owner_id"}
        ),
        @Index(name = "idx_doc_name",
                columnNames = {"name"}
        )
})
@CatalogObjectCache
public class Document extends CatalogObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5619806372864897119L;

	public Document() {}
	
	public Document(String name, String owner, String partitionId, byte[] payload){
		this.name = name;
		this.owner = owner;
		this.partitionId = partitionId;
		this.payload = payload;
	}
	
	public Document(Document document) {
		this.archived = document.isArchived();
		this.description = document.getDescription();
		this.locked = document.isLocked();
		this.name = document.getName();
		this.notes = document.getNotes();
		this.owner = document.getOwner();
		this.partitionId = document.getPartitionId();
		this.payload = document.getPayload();
	}

	@Id
	@GeneratedValue
	@Column(name="document_id")
	private Long id;
	
	@Column(name="document_description")
	private String description;
	
	@Column(name="document_notes")
	private String notes;

	@OneToMany(mappedBy="document")
	private Set<Reference> references;
	
	@Lob
	@NotNull
	@Column(name="document_payload")
	private byte[] payload;
	
	@OneToOne
	@ForeignKey(name="fk_cf_document_id")
	@JoinColumn(name="document_created_from_id")
	private Document createdFrom;

	@OneToMany(mappedBy="createdFrom")
	private Set<Document> createdTo;
	
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name="document_id")
	private Set<DocumentAttribute> attributes;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public Set<Reference> getReferences() {
		return references;
	}

	public void setReferences(Set<Reference> references) {
		this.references = references;
	}

	public Set<DocumentAttribute> getAttributes() {
		if(attributes==null){
			attributes = new HashSet<DocumentAttribute>();
		}
		
		return attributes;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	@Transient
	public int getReferenceCount(){
		if(references == null)
			return 0;
		return references.size();
	}

	public Document getCreatedFrom() {
		return createdFrom;
	}

	public void setCreatedFrom(Document createdFrom) {
		this.createdFrom = createdFrom;
	}

	public Set<Document> getCreatedTo() {
		return createdTo;
	}

	public void setCreatedTo(Set<Document> createdTo) {
		this.createdTo = createdTo;
	}
	
	@Override
	public void addAttribute(ExtendedAttribute attribute) {
		for(ExtendedAttribute ca : getAttributes()){
			if(ca.getKey().toLowerCase().equals(attribute.getKey().toLowerCase())){
				ca.setValue(attribute.getValue().toLowerCase());
				return;
			}
		}
		getAttributes().add((DocumentAttribute)attribute);
	}
}