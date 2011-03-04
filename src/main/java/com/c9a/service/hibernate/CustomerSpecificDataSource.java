package com.c9a.service.hibernate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.certive.util.ServiceConfiguration;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public abstract class CustomerSpecificDataSource extends AbstractRoutingDataSource {
	
	private static final Logger LOG = Logger.getLogger(CustomerSpecificDataSource.class.getName());

	// Hibernate property constants
	protected static final String HIBERNATE_CONNECTION_URL = "hibernate.connection.url";
	protected static final String HIBERNATE_CONNECTION_USERNAME = "hibernate.connection.username";
	protected static final String HIBERNATE_CONNECTION_PASSWORD = "hibernate.connection.password";

	// Property names
	protected static final String USER_PROPERTY = "user";
	protected static final String PASSWORD_PROPERTY = "password";
	protected static final String JDBC_URL_PROPERTY = "jdbcUrl";
	protected static final String DRIVER_CLASS_PROPERTY = "driverClass";
	protected static final String ENTITY_PACKAGE_TO_SCAN = "entityPackageToScan";

	public static final String SERVICE_SCHEMA_PROPERTY = "serviceSchema";
	

	@Autowired
	@Qualifier("hibernateProperties")
	protected Properties hibernateProperties;
	
	@Autowired
	@Qualifier("c3p0Properties")
	protected Properties c3p0Properties;

	// Simple map to hold the data source instances
	private Map<String, DataSource> resolvedDataSources = new HashMap<String, DataSource>();
	
	@Override
	protected DataSource determineTargetDataSource() {
		String partitionId = ContextHolder.getCustomerContext() == null ? c3p0Properties.getProperty(SERVICE_SCHEMA_PROPERTY) : ContextHolder.getCustomerContext();
		try {
			LOG.log(Level.FINEST, "Looking up DataSource for : " + partitionId);
			DataSource dataSource = resolvedDataSources.get(partitionId);
			if (dataSource == null) {
				dataSource = createOrGetDataSourceForCustomer(partitionId);
				processOverrides(c3p0Properties, getServiceName(), partitionId);
				processOverrides(hibernateProperties, getServiceName(), partitionId);
				resolvedDataSources.put(partitionId, dataSource);
			}
			return dataSource; 
		} catch (SQLException e) {
			throw new RuntimeException("Error creating datasource for partitionId: " + partitionId, e);
		} catch (HibernateException e) {
			throw new RuntimeException("Error creating datasource for partitionId: " + partitionId, e);
		} catch (Exception e) {
			throw new RuntimeException("Error creating datasource for partitionId: " + partitionId, e);
		} 
	}

	protected abstract String getServiceName();

	private DataSource createOrGetDataSourceForCustomer(String partitionId) throws HibernateException, Exception {
		//Simple connection to test if the schema is present
		Connection conn = null;
	    Properties connectionProps = new Properties();
	    connectionProps.put(USER_PROPERTY, c3p0Properties.getProperty(USER_PROPERTY));
	    connectionProps.put(PASSWORD_PROPERTY, c3p0Properties.getProperty(PASSWORD_PROPERTY));
	    
	    String jdbcUrl = c3p0Properties.getProperty(JDBC_URL_PROPERTY)+partitionId;
		
		Boolean customerSchemaExists = false;
		try{
			LOG.log(Level.FINEST, "Trying jdbc : " + jdbcUrl);
			conn = DriverManager.getConnection(jdbcUrl, connectionProps);
			customerSchemaExists = true;
		} catch(Throwable e){
			LOG.log(Level.FINEST, "customer schema does not exists : " + jdbcUrl);
			// Change the jdbcUrl to a know good schema name, this will allow us to create new schema's
			// Assume it will not fail...hence there is no try/catch wrapping the getConnection
			jdbcUrl = c3p0Properties.getProperty(JDBC_URL_PROPERTY);
			conn = DriverManager.getConnection(jdbcUrl, connectionProps);
		}
		
		if(!customerSchemaExists){
			provisionNewCustomer(conn, partitionId);
			if(!conn.isClosed()){
				conn.close();
			}
			LOG.log(Level.FINEST, "Schema has been generated for " + partitionId);
		} else {
			createOrUpdateSchemaAndSetDefaultData(partitionId);
		}
		
		jdbcUrl = c3p0Properties.getProperty(JDBC_URL_PROPERTY)+partitionId;
		DataSource customerDataSource = newDataSource(partitionId, jdbcUrl);
		return customerDataSource;
	}

	private DataSource newDataSource(String partitionId, String jdbcUrl) {
		ComboPooledDataSource ds = new ComboPooledDataSource();
		// If we need more control over the c3p0 properties then add them to the properties in the xml
		ds.setProperties(c3p0Properties);
		ds.setJdbcUrl(jdbcUrl);
		
		return ds;
	}

	protected void provisionNewCustomer(Connection conn, String partitionId) throws HibernateException, Exception {
		// Create the new schema using the default available schema
		createDatabase(conn, partitionId);
		// This is where the schema objects will be auto generated
		createOrUpdateSchemaAndSetDefaultData(partitionId);
	}
	
	protected static void createDatabase(Connection cConnection, String sDatabaseName) {
		if(cConnection == null){
			throw new IllegalArgumentException("connection can not be null");
		}
		if(sDatabaseName == null){
			throw new IllegalArgumentException("database name can not be null");
		}
		
		try {
			Statement cStatement = cConnection.createStatement();
			cStatement.execute("create database " + sDatabaseName);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (cConnection != null) {
				try {
					cConnection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	protected Object determineCurrentLookupKey() {
		return ContextHolder.getCustomerContext();
	}
	
	protected static Properties processOverrides(Properties properties, String serviceName, String partitionId){
		Properties propertyOverrides = ServiceConfiguration.getPropertiesForService(serviceName);
		for(String propertyName : propertyOverrides.stringPropertyNames()){
			Object propValue = properties.getProperty(propertyName);
			if(propValue !=null){
				properties.put(propertyName, propertyOverrides.getProperty(propertyName));
			}
		}
		return properties;
	}

	protected abstract void createDefaultDataForSchema(SessionFactory sessionFactory);
	protected abstract void createOrUpdateSchemaAndSetDefaultData(String partitionId) throws Exception;
}