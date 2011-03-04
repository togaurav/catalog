package com.c9a.catalog.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name="permission")
public class Permission implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 234057309015116908L;
	public static final Long EDIT_PERMISSION_ID = 1L;
	public static final Long SHARE_PERMISSION_ID = 2L;
	public static final Long CLONE_PERMISSION_ID = 3L;
	public static final Long ADD_PERMISSION_ID = 4L;
	
	public static final String CAN_NOT_SHARE_COLLECTION_MESSAGE = "Can not share this collection because it does not have the share permission.";
	public static final String CAN_NOT_SHARE_REFERENCE_MESSAGE = "Can not share this reference because it does not have the share permission.";
	
	public static final String CAN_NOT_SHARE_COLLECTION_NOT_ENOUGH_PERMISSION_MESSAGE = "Can not share this collection because the share permissions do not meet minimal restrictions.";
	public static final String CAN_NOT_SHARE_REFERENCE_NOT_ENOUGH_PERMISSION_MESSAGE = "Can not share this reference because the share permissions do not meet minimal restrictions.";
	public static final String CAN_NOT_DELETE_REFERENCE_NOT_ENOUGH_PERMISSION = "Can not delete this reference, not enough permissions.";
	public static final String CAN_ADD_MORE_PERMISSION_THAN_EXISTS_MESSAGE = "Can not grant more permissions than exist on current reference.";
	public static final String CAN_NOT_MODIFY_COLLECTION_NOT_ENOUGH_PERMISSION = "Can not modify collection because it does not contain the modify permission.";
	public static final String CAN_NOT_CLONE_REFERENCE_NOT_ENOUGH_PERMISSION_MESSAGE = "Reference does not have the needed permission(s) to be cloned.";
	
	public Permission(String description) {
		this.description = description;
	}
	
	public Permission() {}
	
	@Id
	@GeneratedValue
	@Column(name="permission_id")
	private Long id;

	@NotNull
	@Column(name="description")
	private String description;

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

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Permission)){
			return false;
		}
		Permission otherP = (Permission)obj;
		return otherP.getId().equals(id);
	}

	@Override
	public int hashCode() {
		return this.description.hashCode() * 13;
	}
}