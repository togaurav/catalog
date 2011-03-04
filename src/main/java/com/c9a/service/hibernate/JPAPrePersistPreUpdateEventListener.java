package com.c9a.service.hibernate;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import org.hibernate.event.SaveOrUpdateEvent;
import org.hibernate.event.def.DefaultSaveEventListener;
import org.springframework.stereotype.Component;

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
@Component("JPAPrePersistPreUpdateEventListener")
public class JPAPrePersistPreUpdateEventListener extends DefaultSaveEventListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final Logger LOG = Logger.getLogger(JPAPrePersistPreUpdateEventListener.class.getName());
	
	@Override
	protected Serializable performSaveOrUpdate(SaveOrUpdateEvent event) {
		if(event.getEntry() == null){
			LOG.log(Level.FINEST, "Object is not in db, so look for pre-persist methods");
			performPrePersist(event);
		} else {
			LOG.log(Level.FINEST, "Object is already in db, so look for pre-persist methods");
			performPreUpdate(event);
		}
		Serializable s = super.performSaveOrUpdate(event);
		return s;
	}

	private void performPreUpdate(SaveOrUpdateEvent event) {
		Method[] methods = event.getObject().getClass().getMethods();
		for(Method m : methods){
			// Find the first method with the prePersist and invoke
			// Assume no args, as per @PreUpdate Spec
			PreUpdate p = m.getAnnotation(PreUpdate.class);
			if(p!=null){
				try {
					m.invoke(event.getObject());
					break;
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void performPrePersist(SaveOrUpdateEvent event) {
		Method[] methods = event.getObject().getClass().getMethods();
		for(Method m : methods){
			// Find the first method with the prePersist and invoke
			// Assume no args, as per @PrePersist Spec
			PrePersist p = m.getAnnotation(PrePersist.class);
			if(p!=null){
				try {
					m.invoke(event.getObject());
					break;
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}
}