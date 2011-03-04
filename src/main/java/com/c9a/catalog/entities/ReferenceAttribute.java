
package com.c9a.catalog.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Index;

@Entity
@Table(name="reference_attribute", uniqueConstraints=@UniqueConstraint(columnNames={"reference_id", "lookup_key"}))
@org.hibernate.annotations.Table(
		appliesTo="reference_attribute", 
		indexes = {
        @Index(name = "idx_ref_key",
                columnNames = {"lookup_key"}
        ),
        @Index(name = "idx_ref_value",
                columnNames = {"value"}
        )
})
public class ReferenceAttribute extends ExtendedAttribute {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7346833535574616461L;
	@Column(name="reference_id")
	private Long referenceId;
	
	public ReferenceAttribute(){}

	public ReferenceAttribute(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public Long getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(Long referenceId) {
		this.referenceId = referenceId;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof ReferenceAttribute)){
			return false;
		}
		ReferenceAttribute other = (ReferenceAttribute)obj;
		return other.getKey().equals(key) && other.getValue().equals(value);
	}

	@Override
	public int hashCode() {
		return key.hashCode() * 13 + value.hashCode() * 2; 
	}
}