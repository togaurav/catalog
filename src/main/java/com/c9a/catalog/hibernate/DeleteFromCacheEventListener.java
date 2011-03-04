package com.c9a.catalog.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.event.DeleteEvent;
import org.hibernate.event.def.DefaultDeleteEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.c9a.catalog.service.CatalogCacheManager;

@Component("DeleteFromCacheEventListener")
public class DeleteFromCacheEventListener extends DefaultDeleteEventListener {
	
	@Autowired
	private CatalogCacheManager cacheManager;

	/**
	 * 
	 */
	private static final long serialVersionUID = -4381702448320998227L;

	@Override
	public void onDelete(DeleteEvent event) throws HibernateException {
		super.onDelete(event);
		removeFromCache(event);
	}

	private void removeFromCache(DeleteEvent event) {
		cacheManager.removeFromCache(event.getObject());
	}
}
