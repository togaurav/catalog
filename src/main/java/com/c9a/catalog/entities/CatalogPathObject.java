package com.c9a.catalog.entities;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.validation.constraints.NotNull;

@MappedSuperclass
public abstract class CatalogPathObject extends CatalogObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3311277885081335722L;
	
	public abstract CatalogPathObject getParentCollection();
	
	@NotNull
	@Column(name="path")
	private String path;

	@Override
	@PreUpdate
	@PrePersist
	public void updateTimeStampsAndSetUniqueId() {
		if(path==null && name != null){
			path = generatePath();
		}
		super.updateTimeStampsAndSetUniqueId();
	}
	
	protected abstract String generatePath();

	public String getPath(){
		if(path==null && name != null){
			path = generatePath();	
		}
		return path;
	}
}