package com.c9a.catalog.dao;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

public class DaoSupport extends HibernateDaoSupport {
	
	@Autowired
	public void setSessionFactoryAutowired(SessionFactory sessionFactory) {
		this.setSessionFactory(sessionFactory);
	}
	
	public <T> T findById(Class<T> clazz, Serializable id) {
		return (T) getHibernateTemplate().get(clazz, id);
	}

	public Serializable save(Object entity) {
		return getHibernateTemplate().save(entity);
	}

	public void update(Object entity) {
		getHibernateTemplate().update(entity);
	}
	
	public void delete(Object entity) {
		getHibernateTemplate().delete(entity);
	}

	public <T> T merge(T entity) {
		T mergedObject = (T) getHibernateTemplate().merge(entity); 
		return mergedObject;
	}

	public void flush() {
		getHibernateTemplate().flush();
	}

	public void refresh(Object entity) {
		getHibernateTemplate().refresh(entity);
	}

	@SuppressWarnings("unchecked")
	public <T> T findByUniqueId(final Class<T> clazz, final String uniqueId){
		return (T)getHibernateTemplate().execute(new HibernateCallback<T>() {
			public T doInHibernate(Session session) throws HibernateException, SQLException {
				return (T)session.createCriteria(clazz).add(Restrictions.eq("uniqueId", uniqueId)).uniqueResult();
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> findAll(final Class<T> clazz, final Order... order) {
		return (List<T>) getHibernateTemplate().execute(
				new HibernateCallback<Object>() {
					public Object doInHibernate(Session session)
							throws HibernateException, SQLException {
						Criteria c = session.createCriteria(clazz);
						for (Order o : order) {
							c.addOrder(o);
						}
						return c.list();
					}
				});
	}
}
