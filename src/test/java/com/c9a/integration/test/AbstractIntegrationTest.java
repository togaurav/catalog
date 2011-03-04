package com.c9a.integration.test;

import java.util.logging.Logger;

import junit.framework.TestCase;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.c9a.catalog.dao.CatalogDao;
import com.c9a.catalog.entities.Permission;
import com.c9a.catalog.service.ICatalogSearchService;
import com.c9a.catalog.service.ICatalogService;

@ContextConfiguration(locations = { "classpath:applicationContext.xml"})
@TransactionConfiguration
@Transactional
public class AbstractIntegrationTest extends TestCase {
	
	private static final Logger LOG = Logger.getLogger(CatalogServiceIntegrationTest.class.getName());
	
	@Autowired
	protected CatalogDao catalogDao;
	
	@Autowired 
	protected ICatalogService catalogService;
	
	@Autowired 
	protected ICatalogSearchService catalogSearchService;
	
	protected static final String partitionId = "catalogservicetest";
	
	@Autowired
	protected SessionFactory sessionFactory;
	
	private Session session = null;
	
	protected boolean flush = true;
	
//    @Before
//    public void beforeMethod() {
//    	session = SessionFactoryUtils.getSession(sessionFactory, true);
//    	TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
//    	setupPermissions(session);
//    }
	
	@BeforeTransaction
	public void setupPermissions() {
		// Check to see if a permission is in the database, if so assume only the ones that you need are in the db, since this should run once, 
		// and rely's on the hibernate auto create/drop schema for the hibernate DDL setting, this should be abstracted to the properties file, 
		// eventually have a properties file for the integration tests
//    	session = SessionFactoryUtils.getSession(sessionFactory, true);
//		Transaction t = session.beginTransaction();
		LOG.info("GETTING PERMISSION TEST");
		Permission viewPermission = (Permission)catalogDao.findById(Permission.class, Permission.EDIT_PERMISSION_ID);
		if(viewPermission == null){
			catalogDao.save(new Permission("Edit"));
			catalogDao.save(new Permission("Share"));
			catalogDao.save(new Permission("Clone"));
			catalogDao.save(new Permission("Add"));
			catalogDao.flush();
		}
//		t.commit();
//		session
	}

//	@After
	public void afterMethod() {
		if(flush)
			catalogDao.flush();
		else 
			flush = true;
		
//		TransactionSynchronizationManager.unbindResource(sessionFactory);
		SessionFactoryUtils.closeSession(session);
	}
}