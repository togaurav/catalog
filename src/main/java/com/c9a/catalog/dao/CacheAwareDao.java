package com.c9a.catalog.dao;

import java.io.Serializable;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;

import com.c9a.catalog.service.CatalogCacheManager;

public class CacheAwareDao extends DaoSupport {
	
	@Autowired
	protected CatalogCacheManager cacheManager;
	
	@Override
	public <T> T findById(Class<T> clazz, Serializable id) {
		String cacheKey = clazz.getSimpleName() + ":" + id;
		T o = (T)getFromCacheAndAssociateWithSession(clazz, cacheKey);
		if(o != null){
			return o;
		}
		return (T) getHibernateTemplate().get(clazz, id);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T findByUniqueId(final Class<T> clazz, final String uniqueId){
		T o = (T)getFromCacheAndAssociateWithSession(clazz, uniqueId);
		if(o != null){
			return o;
		}
		return (T)getHibernateTemplate().execute(new HibernateCallback<T>() {
			public T doInHibernate(Session session) throws HibernateException, SQLException {
				return (T)session.createCriteria(clazz).add(Restrictions.eq("uniqueId", uniqueId)).uniqueResult();
			}
		});
	}
	
	public <T> T findByKey(Class<T> clazz, String key) {
		return  getFromCacheAndAssociateWithSession(clazz, key);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T getFromCacheAndAssociateWithSession(Class<T> clazz, String key) {
		Long id = (Long)cacheManager.getCatalogObjectFromCache(key);
		if(id != null){
			return (T) getHibernateTemplate().getSessionFactory().getCurrentSession().load(clazz, id);
		}
		return null;
	}
	
	@Override
	public void delete(Object entity) {
		getHibernateTemplate().delete(entity);
		cacheManager.removeFromCache(entity);
	}

}
