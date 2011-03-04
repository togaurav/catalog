package com.c9a.catalog.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Index;

@Entity
@Table(name="collection_attribute", uniqueConstraints=@UniqueConstraint(columnNames={"collection_id", "lookup_key"}))
@org.hibernate.annotations.Table(
		appliesTo="collection_attribute", 
		indexes = {
        @Index(name = "idx_col_key",
                columnNames = {"lookup_key"}
        ),
        @Index(name = "idx_col_value",
                columnNames = {"value"}
        )
})
public class CollectionAttribute extends ExtendedAttribute {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4814945236923133003L;
	@Column(name="collection_id")
	private Long collectionId;
	
	public CollectionAttribute(){}

	public CollectionAttribute(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public Long getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(Long collectionId) {
		this.collectionId = collectionId;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof CollectionAttribute)){
			return false;
		}
		CollectionAttribute other = (CollectionAttribute)obj;
		return other.getKey().equals(key) && other.getValue().equals(value);
	}

	@Override
	public int hashCode() {
		return key.hashCode() * 14 + value.hashCode() * 3; 
	}

}