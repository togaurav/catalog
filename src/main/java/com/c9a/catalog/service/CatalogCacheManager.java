package com.c9a.catalog.service;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.c9a.catalog.entities.CatalogObject;
import com.c9a.catalog.entities.CatalogObjectCache;
import com.c9a.catalog.entities.CatalogPathCache;
import com.c9a.catalog.entities.CatalogPathObject;
import com.c9a.service.hibernate.ContextHolder;
import com.c9a.service.hibernate.CustomerSpecificDataSource;

@Service("catalogCacheManager")
public class CatalogCacheManager {
	
	private static final Logger LOG = Logger.getLogger(CatalogCacheManager.class.getName());
	
	@Autowired
	@Qualifier("c3p0Properties")
	protected Properties c3p0Properties;
	
	private static Map<String, JCS> customerCaches = new HashMap<String, JCS>();
	
	public static String getPathKeyForCatalogObject(CatalogPathObject catalogObject){
		LOG.log(Level.FINEST, "GENERATED KEY : " + catalogObject.getPath());
		return catalogObject.getPath(); 
	}
	
	public Long getCatalogObjectFromCache(String key) {
		try {
			JCS customerCache = getCustomerCache();
			LOG.log(Level.FINEST, "LOOKING UP OBJECT IN UNIQUE ID CACHE WITH KEY : " + key);
			Long co = (Long) customerCache.get(key);
			if (co != null) {
				LOG.log(Level.FINEST, "FOUND CATALOG OBJECT IN UNIQUE ID CACHE WITH KEY : " + key);
			}
			return co;
		} catch (CacheException ce) {
			ce.printStackTrace();
			return null;
		}
	}
	
	private void putCatalogObject(CatalogObject co) {
		co.getAttributes();
		try {
			JCS customerCache = getCustomerCache();
			customerCache.put(co.getUniqueId(), co.getId());
			customerCache.put(generateObjectIdKey(co), co.getId());
			
		} catch (CacheException e) {
			e.printStackTrace();
		}
	}

	public void putCatalogPathObject(CatalogPathObject cpo) {
		cpo.getAttributes();
		try {
			JCS customerCache = getCustomerCache();
			LOG.log(Level.FINEST, "PUTTING IN CACHE WITH path : " + cpo.getPath());
			customerCache.put(cpo.getPath(), cpo.getId());
			LOG.log(Level.FINEST, "PUTTING IN CACHE WITH UNIQUEID : " + cpo.getUniqueId());
			customerCache.put(cpo.getUniqueId(), cpo.getId());
			LOG.log(Level.FINEST, "PUTTING IN CACHE WITH id : " + generateObjectIdKey(cpo));
			customerCache.put(generateObjectIdKey(cpo), cpo.getId());
		} catch (CacheException e) {
			e.printStackTrace();
		}
	}

	public String generateObjectIdKey(CatalogObject cpo) {
		return cpo.getClass().getSimpleName() + ":" + cpo.getId();
	}

	private JCS getCustomerCache() throws CacheException {
		String partitionId = ContextHolder.getCustomerContext();
		if(partitionId == null){
			 partitionId = c3p0Properties.getProperty(CustomerSpecificDataSource.SERVICE_SCHEMA_PROPERTY);
		}
		JCS customerCache = customerCaches.get(partitionId);
		if(customerCache == null){
			String cacheName = partitionId+"_cache";
			LOG.info("Creating cache : " + cacheName);
			customerCache = JCS.getInstance(cacheName);
			customerCaches.put(partitionId, customerCache);
		}
		return customerCache;
	}

	public void cacheIfAppropriate(Object object) {
		Annotation annotations[] = object.getClass().getAnnotations();
		for(Annotation a : annotations){
			LOG.log(Level.FINEST, "annotation : " + a.annotationType());
			if(a instanceof CatalogPathCache){
				CatalogPathObject cpo = (CatalogPathObject)object;
				if(cpo.getId()!=null)
					putCatalogPathObject(cpo);
				return;
			} else if(a instanceof CatalogObjectCache){
				CatalogObject co = (CatalogObject)object;
				if(co.getId()!=null)
					putCatalogObject(co);
				return;
			}
		}
	}

	public void removeFromCache(Object object) {
		Annotation annotations[] = object.getClass().getAnnotations();
		for(Annotation a : annotations){
			LOG.log(Level.FINEST, "annotation : " + a.annotationType());
			if(a instanceof CatalogPathCache){
				CatalogPathObject cpo = (CatalogPathObject)object;
				if(cpo.getId()!=null)
					removeCatalogPathObject(cpo);
				return;
			} else if(a instanceof CatalogObjectCache){
				CatalogObject co = (CatalogObject)object;
				if(co.getId()!=null)
					removeCatalogObject(co);
				return;
			}
		}
		
	}

	private void removeCatalogObject(CatalogObject co) {
		try{
			JCS customerCache = getCustomerCache();
			customerCache.remove(co.getUniqueId());
			customerCache.remove(generateObjectIdKey(co));
		} catch(CacheException e){}
	}

	private void removeCatalogPathObject(CatalogPathObject cpo) {
		try{
			JCS customerCache = getCustomerCache();
			customerCache.remove(cpo.getUniqueId());
			customerCache.remove(generateObjectIdKey(cpo));
			customerCache.remove(cpo.getPath());
		} catch(CacheException e){}
	}
}