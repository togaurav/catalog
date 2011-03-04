package com.c9a.catalog.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;

@MappedSuperclass
public abstract class ExtendedAttribute implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7129301591887013641L;

	public ExtendedAttribute(){}
	
	@Id
	@GeneratedValue
	@Column(name="extended_attribute_id")
	protected Long id;

	@NotNull
	@Column(name="lookup_key")
	protected String key;

	@NotNull
	@Column(name="value")
	protected String value;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();
}