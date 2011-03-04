package com.c9a.catalog.hibernate;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.event.SaveOrUpdateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.c9a.catalog.service.AsyncProcessServiceImpl;
import com.c9a.catalog.service.CatalogCacheManager;
import com.c9a.service.hibernate.JPAPrePersistPreUpdateEventListener;

/**
 * Simple class to handle JPA @PrePersist, @PreUpdate annotations at the method level
 * Only supports one @PrePersist and one @PreUpdate per class
 * This class also support the custom cache mechanism for CatalogObjects
 * 
 * This is in response to no direct support for some JPA annotations while using HibernateTemplate (Hibernate Session) as opposed to JPATemplat
 * HibernateTemplate has more advanced features, but lacks some simple JPA style annotations
 * @author Carter Youngblood
 *
 */
@Component("JPAPrePersistPreUpdateCachableEventListener")
public class JPAPrePersistPreUpdateCachableEventListener extends JPAPrePersistPreUpdateEventListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final Logger LOG = Logger.getLogger(JPAPrePersistPreUpdateCachableEventListener.class.getName());
	
//	@Autowired
//	private AsyncProcessServiceImpl asyncProcessService;
	
	@Autowired
	private CatalogCacheManager cacheManager;
	
	@Override
	protected Serializable performSaveOrUpdate(SaveOrUpdateEvent event) {
		Serializable s = super.performSaveOrUpdate(event);
		addToCache(event);
		return s;
	}

	private void addToCache(SaveOrUpdateEvent event) {
		LOG.log(Level.FINEST, "Add Object to cache if supported by annotatons on the object.");
		cacheManager.cacheIfAppropriate(event.getObject());
//		asyncProcessService.addToCache(event.getObject());
	}
}