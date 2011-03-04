package com.c9a.catalog.hibernate;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.entities.CollectionAttribute;
import com.c9a.catalog.entities.Document;
import com.c9a.catalog.entities.DocumentAttribute;
import com.c9a.catalog.entities.Permission;
import com.c9a.catalog.entities.Reference;
import com.c9a.catalog.entities.ReferenceAttribute;
import com.c9a.service.hibernate.CustomerSpecificDataSource;

public class CatalogSpecificDataSource extends CustomerSpecificDataSource {
	
	private static final Logger LOG = Logger.getLogger(CatalogSpecificDataSource.class.getName());
	private static final String CATALOG_SERVICE_NAME = "CATALOG_SERVICE";

	@Override
	protected void createDefaultDataForSchema(SessionFactory sessionFactory) {
		Session session = sessionFactory.openSession();
		Transaction transaction = session.beginTransaction();
		transaction.begin();
		Permission editPermission = (Permission)session.get(Permission.class, Permission.EDIT_PERMISSION_ID);
		if(editPermission == null){
			LOG.log(Level.FINE, "Adding default permissions to the schema");
			// Must add in this order...The Id's must match
			// Edit=1, Share=2, CLone=3, Add=4
			session.save(new Permission("Edit"));
			session.save(new Permission("Share"));
			session.save(new Permission("Clone"));
			session.save(new Permission("Add"));
			session.flush();
		}
		transaction.commit();
		if(session.isOpen()){
			session.close();
		}
		if(!sessionFactory.isClosed()){
			sessionFactory.close();
		}
		// Throw it away, since all we need it for is the schema creation, and its expensive
		// Let GC get the object
		session = null;
		transaction = null;
	}

	@Override
	protected void createOrUpdateSchemaAndSetDefaultData(String partitionId)
			throws Exception {
		Configuration hibernateConfig = new Configuration();
		// Add Domain model classes here
		// If you add a domain class, it needs to be added here!
		hibernateConfig.addAnnotatedClass(CatalogCollection.class);
		hibernateConfig.addAnnotatedClass(Permission.class);
		hibernateConfig.addAnnotatedClass(Reference.class);
		hibernateConfig.addAnnotatedClass(Document.class);
		hibernateConfig.addAnnotatedClass(CollectionAttribute.class);
		hibernateConfig.addAnnotatedClass(ReferenceAttribute.class);
		hibernateConfig.addAnnotatedClass(DocumentAttribute.class);
		
		hibernateConfig.setProperties(hibernateProperties);
		
		// Set properties from bean in spring config
		// Set the jdbc url to the newly created schema
		hibernateConfig.setProperty(CustomerSpecificDataSource.HIBERNATE_CONNECTION_URL, c3p0Properties.getProperty(JDBC_URL_PROPERTY) + partitionId);
		hibernateConfig.setProperty(CustomerSpecificDataSource.HIBERNATE_CONNECTION_USERNAME, c3p0Properties.getProperty(USER_PROPERTY));
		hibernateConfig.setProperty(CustomerSpecificDataSource.HIBERNATE_CONNECTION_PASSWORD, c3p0Properties.getProperty(PASSWORD_PROPERTY));
		
		SessionFactory sessionFactory = hibernateConfig.buildSessionFactory();
		
		createDefaultDataForSchema(sessionFactory);
		
		if(!sessionFactory.isClosed()){
			sessionFactory.close();
		}
		// Allow for GC
		sessionFactory = null;
	}

	@Override
	protected String getServiceName() {
		return CATALOG_SERVICE_NAME;
	}
}