package com.c9a.catalog.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.validation.constraints.NotNull;

import com.c9a.catalog.exception.InvalidCatalogNameException;

@MappedSuperclass
public abstract class CatalogObject implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3394221463518845471L;
	
	public static final String unSupportedCharRegExForName = "[^a-zA-Z0-9_ @.]";
	public static final String unSupportedCharRegExForPath = "[^a-zA-Z0-9_@.]";
	public static final String unSupportedCharRegExForPathFull = "[^a-zA-Z0-9_/.@]";
	public static final String replacementChar = "_";
	
	public abstract Long getId();

	@NotNull
	@Column(name="owner_id")
	protected String owner;
	
	@NotNull
	@Column(name="partition_id")
	protected String partitionId;
	
	@NotNull
	@Column(name="unique_id")
	protected String uniqueId;
	
	@NotNull
	@Column(name="last_modified_date")
	protected Date lastModifiedDate;

	@NotNull
	@Column(name="create_date")
	protected Date createDate;
	
	@NotNull
	@Column(name="archived")
	protected boolean archived = false;
	
	@NotNull
	@Column(name="locked")
	protected boolean locked = false;
	
	@NotNull
	@Column(name="name")
	protected String name;
	
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public String getPartitionId() {
		return partitionId;
	}

	public void setPartitionId(String partitionId) {
		this.partitionId = partitionId;
	}

	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) throws InvalidCatalogNameException {		
		if(!isValidName(name)){
			throw new InvalidCatalogNameException();
		}
		this.name = name;
		
	}

	public static boolean isValidName(String name) {
		return name.replaceAll(unSupportedCharRegExForName, replacementChar).equals(name);
	}

	@PreUpdate
	@PrePersist
	public void updateTimeStampsAndSetUniqueId() {
		lastModifiedDate = new Date();
		if (createDate == null) 
			createDate = new Date();

		if(uniqueId == null)
			uniqueId = UUID.randomUUID().toString();
	}
	
	public abstract Set<? extends ExtendedAttribute> getAttributes();
	public abstract void addAttribute(ExtendedAttribute attribute);
	
	public ExtendedAttribute getAttributeByKey(String key){
		for(ExtendedAttribute ea : getAttributes()){
			if(ea.getKey().toLowerCase().equals(key.toLowerCase())){
				return ea;
			}
		}
		return null;
	}
	
}