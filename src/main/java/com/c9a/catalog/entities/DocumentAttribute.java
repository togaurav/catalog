package com.c9a.catalog.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Index;

@Entity
@Table(name="document_attribute", uniqueConstraints=@UniqueConstraint(columnNames={"document_id", "lookup_key"}))
@org.hibernate.annotations.Table(
		appliesTo="document_attribute", 
		indexes = {
        @Index(name = "idx_doc_key",
                columnNames = {"lookup_key"}
        ),
        @Index(name = "idx_doc_value",
                columnNames = {"value"}
        )
})
public class DocumentAttribute extends ExtendedAttribute {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2166133844896639002L;
	@Column(name="document_id")
	private Long documentId;

	public DocumentAttribute(){}
	
	public DocumentAttribute(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public Long getDocumentId() {
		return documentId;
	}

	public void setDocumentId(Long documentId) {
		this.documentId = documentId;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof DocumentAttribute)){
			return false;
		}
		DocumentAttribute other = (DocumentAttribute)obj;
		return other.getKey().equals(key) && other.getValue().equals(value);
	}

	@Override
	public int hashCode() {
		return key.hashCode() * 11 + value.hashCode() * 4; 
	}
}
