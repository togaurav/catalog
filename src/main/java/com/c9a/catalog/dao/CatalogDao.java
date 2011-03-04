package com.c9a.catalog.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Repository;

import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.entities.Permission;

/**
 * 
 * @author Carter Youngblood
 *
 */
@Repository(value = "catalogDao")
public class CatalogDao extends CacheAwareDao {
	
	private static final Logger LOG = Logger.getLogger(CatalogDao.class.getName());
	
	public CatalogCollection getRootApplicationForUser(final String owner, final String partitionId, final String application) {
		String keyPath = "/" + CatalogCollection.USERS_COLLECTION_NAME + "/" + owner + "/" + application;
		keyPath = keyPath.replaceAll("[^a-zA-Z0-9]", "_");
		CatalogCollection rootApplicationCollection = this.findByKey(CatalogCollection.class, keyPath);
		
		if(rootApplicationCollection!=null){
			return rootApplicationCollection;
		}
		
		return (CatalogCollection) getHibernateTemplate().execute(new HibernateCallback<CatalogCollection>() {
			public CatalogCollection doInHibernate(Session session) throws HibernateException,	SQLException {
				CatalogCollection userRootCollection = getRootCollectionForUser(owner, partitionId);
				CatalogCollection rootApplicationCollection = (CatalogCollection)session.createCriteria(CatalogCollection.class)
																.add(Restrictions.eq("owner", owner))
																.add(Restrictions.eq("partitionId", partitionId))
																.add(Restrictions.eq("name", application))
																.createAlias("parentCollection", "parentCollection")
																.add(Restrictions.eq("parentCollection.id", userRootCollection.getId()))
																.uniqueResult();
				if(rootApplicationCollection == null){
					LOG.fine("Creating new application collection for  partitionId " + partitionId);
					Set<Permission> permissions = new HashSet<Permission>();
					permissions.add((Permission)session.load(Permission.class, Permission.EDIT_PERMISSION_ID));
					permissions.add((Permission)session.load(Permission.class, Permission.ADD_PERMISSION_ID));
					rootApplicationCollection = new CatalogCollection(owner, partitionId, application, permissions, userRootCollection);
					rootApplicationCollection = (CatalogCollection)session.load(CatalogCollection.class, session.save(rootApplicationCollection));
					session.refresh(userRootCollection);
					session.flush();
				}
				return rootApplicationCollection;
			}
		});
	}
	
	public CatalogCollection getApplicationShareCollectionForUser(final String owner, final String partitionId, final String application) {
		String keyPath = "/" + CatalogCollection.USERS_COLLECTION_NAME + "/" + owner + "/" + application + "/" + CatalogCollection.SHARE_COLLECTION_NAME;
		keyPath = keyPath.replaceAll("[^a-zA-Z0-9]", "_");
		CatalogCollection rootApplicationCollection = this.findByKey(CatalogCollection.class, keyPath);
		if(rootApplicationCollection!=null){
			return rootApplicationCollection;
		}
		return (CatalogCollection) getHibernateTemplate().execute(new HibernateCallback<CatalogCollection>() {
			public CatalogCollection doInHibernate(Session session) throws HibernateException,	SQLException {
				CatalogCollection rootCollection = getRootApplicationForUser(owner, partitionId, application);
				
				CatalogCollection rootApplicationCollection = (CatalogCollection)session.createCriteria(CatalogCollection.class)
																.add(Restrictions.eq("owner", owner))
																.add(Restrictions.eq("partitionId", partitionId))
																.add(Restrictions.eq("name", CatalogCollection.SHARE_COLLECTION_NAME))
																.createAlias("parentCollection", "parentCollection")
																.add(Restrictions.eq("parentCollection.id", rootCollection.getId()))
																.uniqueResult();
				if(rootApplicationCollection == null){
					LOG.fine("Creating new application share collection for  partitionId " + partitionId);
					Set<Permission> permissions = new HashSet<Permission>();
					permissions.add((Permission)session.load(Permission.class, Permission.EDIT_PERMISSION_ID));
					permissions.add((Permission)session.load(Permission.class, Permission.ADD_PERMISSION_ID));
					rootApplicationCollection = new CatalogCollection(owner, partitionId, CatalogCollection.SHARE_COLLECTION_NAME, permissions, rootCollection);
					rootApplicationCollection = (CatalogCollection)session.load(CatalogCollection.class, session.save(rootApplicationCollection));
					session.refresh(rootCollection);
					session.flush();
				}
				return rootApplicationCollection;
			}
		});
	}
	
	/**
	 * This method will return the root collection for a given partition id
	 * 
	 * @param partitionId	the partitionId to use to find the root collection for a partition id (customer id)
	 * 
	 * @return 				the root collection for a given partitionId		
	 */

	@SuppressWarnings("unchecked")
	public List<CatalogCollection> getRootCollectionForPartition(final String partitionId) {
		return (List<CatalogCollection>) getHibernateTemplate().execute(new HibernateCallback<List<CatalogCollection>>() {
			public List<CatalogCollection> doInHibernate(Session session) throws HibernateException,	SQLException {
				return (List<CatalogCollection>)session.createCriteria(CatalogCollection.class).add(Restrictions.isNull("parentCollection")).list();
			}
		});
	}
	
	/**
	 * This method will return the root collection for a given user and partition id
	 * 
	 * @param owner			the owner for a given collection
	 * @param partitionId	the partitionId to use to find the root collection for a partition id (customer id) and owner
	 * 
	 * @return 				the root collection for a given owner and partitionId		
	 */
	public CatalogCollection getRootCollectionForUser(final String owner, final String partitionId) {
		String keyPath = "/"+CatalogCollection.USERS_COLLECTION_NAME+"/"+owner;
		keyPath = keyPath.replaceAll("[^a-zA-Z0-9]", "_");
		CatalogCollection rootApplicationCollection = this.findByKey(CatalogCollection.class, keyPath);
		if(rootApplicationCollection!=null){
			return rootApplicationCollection;
		}
		
		return (CatalogCollection) getHibernateTemplate().execute(new HibernateCallback<CatalogCollection>() {
			public CatalogCollection doInHibernate(Session session) throws HibernateException,	SQLException {
				CatalogCollection usersRootCollection = getUsersCollection(partitionId);
				
				CatalogCollection rootCollection = (CatalogCollection)session.createCriteria(CatalogCollection.class)
																.add(Restrictions.eq("owner", owner))
																.add(Restrictions.eq("partitionId", partitionId))
																.add(Restrictions.eq("name", owner))
																.createAlias("parentCollection", "parentCollection")
																.add(Restrictions.eq("parentCollection.id", usersRootCollection.getId()))
																.uniqueResult();
				if(rootCollection == null){
					LOG.fine("Creating new root collection for " + owner + " of partitionId " + partitionId);
					Set<Permission> permissions = new HashSet<Permission>();
					permissions.add((Permission)session.load(Permission.class, Permission.ADD_PERMISSION_ID));
					rootCollection = new CatalogCollection(owner, partitionId, owner, permissions, usersRootCollection);
					rootCollection = (CatalogCollection)session.load(CatalogCollection.class, session.save(rootCollection));
					session.flush();
				}
				return rootCollection;
			}
		});
	}

	protected CatalogCollection getUsersCollection(final String partitionId) {
		return (CatalogCollection) getHibernateTemplate().execute(new HibernateCallback<CatalogCollection>() {
			public CatalogCollection doInHibernate(Session session) throws HibernateException,	SQLException {
				
				CatalogCollection usersCollection = (CatalogCollection)session.createCriteria(CatalogCollection.class)
																.add(Restrictions.isNull("parentCollection"))
																.add(Restrictions.eq("name", CatalogCollection.USERS_COLLECTION_NAME))
																.add(Restrictions.eq("owner", CatalogCollection.SYSTEM_OWNER))
																.uniqueResult();
				if(usersCollection == null){
					LOG.fine("Creating new users collection in partition " + partitionId);
					Set<Permission> permissions = new HashSet<Permission>();
					permissions.add((Permission)session.load(Permission.class, Permission.ADD_PERMISSION_ID));
					usersCollection= new CatalogCollection(CatalogCollection.SYSTEM_OWNER, partitionId, CatalogCollection.USERS_COLLECTION_NAME, permissions, null);
					usersCollection = (CatalogCollection)session.load(CatalogCollection.class, session.save(usersCollection));
				}
				return usersCollection;
			}
		});
	}

	/**
	 * This method will return the set of permissions for a given set of permissions id's
	 * 
	 * @param permissionIds	the owner for a given collection
	 * 
	 * @return 						the set of permissions for a given set of permission id's		
	 */
	@SuppressWarnings("unchecked")
	public Set<Permission> getPermissionsForIds(final Set<Long> permissionIds) {
		return (Set<Permission>) getHibernateTemplate().execute(new HibernateCallback<Set<Permission>>() {
			public Set<Permission> doInHibernate(Session session) throws HibernateException,	SQLException {
				Set<Permission> permissions = new HashSet<Permission>();
				permissions.addAll((List<Permission>)session.createCriteria(Permission.class).add(Restrictions.in("id", permissionIds)).list());
				return permissions;
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	private <T> List<T> findObjectsThatMatchKeyOrValue(
			final Class<T> objectToFind, final List<String> keys, final boolean matchAll, final Set<Long> ids, final String columnName) {
		return (List<T>) getHibernateTemplate().execute(new HibernateCallback<List<T>>() {
			public List<T> doInHibernate(Session session) throws HibernateException, SQLException {
				if(matchAll){
					Set<T> allMatches = new HashSet<T>();
					int count = 0;
					for(String key : keys){
						List<T> matches = session
							.createCriteria(objectToFind)
							.createAlias("attributes", "attribute")
							.add(Restrictions.eq("attribute."+columnName, key).ignoreCase())
							.add(Restrictions.in("id", ids))
							.list();
						if(matches.size() == 0){
							return new ArrayList<T>(); 
						}
						if(count > 0){
							Set<T> newMatches = new HashSet<T>();
							for(T obj : matches){
								if(allMatches.contains(obj)){
									newMatches.add(obj);
								}
							}
							allMatches = newMatches;
						} else {
							allMatches.addAll(matches);
						}
						count++;
					}
					List<T> allMatchesList = new ArrayList<T>();
					allMatchesList.addAll(allMatches);
					return allMatchesList;
				} else {
					Disjunction d = Restrictions.disjunction();
					for(String k : keys){
						d.add(Restrictions.eq("attribute."+columnName, k).ignoreCase());
					}
					return session
								.createCriteria(objectToFind)
								.createAlias("attributes", "attribute")
								.add(d)
								.add(Restrictions.in("id", ids))
								.list();
				}
			}
		});
		
	}
	
	public <T> List<T> findObjectsThatMatchAttributeKeys(
			final Class<T> objectToFind, final List<String> keys, final boolean matchAll, final Set<Long> ids) {
		return findObjectsThatMatchKeyOrValue(objectToFind, keys, matchAll, ids, "key");
	}
	
	public <T> List<T> findObjectsThatMatchAttributeValues(
			final Class<T> objectToFind, final List<String> values, final boolean matchAll, final Set<Long> ids) {
		return findObjectsThatMatchKeyOrValue(objectToFind, values, matchAll, ids, "value");
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> findObjectsThatMatchAttributeKeysAndValues(
			final Class<T> objectToFind, final Map<String, String> keyAndValue, final boolean matchAll, final Set<Long> ids) {
		return (List<T>) getHibernateTemplate().execute(new HibernateCallback<List<T>>() {
			public List<T> doInHibernate(Session session) throws HibernateException,	SQLException {
				Set<T> allMatches = new HashSet<T>();
				int count = 0;
				for(Map.Entry<String, String> kv : keyAndValue.entrySet()){
					List<T> matches = session
										.createCriteria(objectToFind)
										.createAlias("attributes", "attribute")
										.add(Restrictions.eq("attribute.key", kv.getKey()).ignoreCase())
										.add(Restrictions.eq("attribute.value", kv.getValue()).ignoreCase())
										.add(Restrictions.in("id", ids))
										.list();
					if(matches.size() == 0 && matchAll){
						return new ArrayList<T>();
					}
					if(count > 0 && matchAll){
						Set<T> newMatchingIds = new HashSet<T>();
						for(T obj : matches){
							if(allMatches.contains(obj)){
								newMatchingIds.add(obj);
							}
						}
						allMatches = newMatchingIds;
					} else {
						allMatches.addAll(matches);
					}
					count++;
				}
				List<T> matchesList = new ArrayList<T>();
				matchesList.addAll(allMatches);
				return matchesList;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> findObjectsThatMatchFieldValue(
			final Class<T> clazz, final Map<String, String> fieldAndValue, final boolean matchAll) {
		return (List<T>) getHibernateTemplate().execute(new HibernateCallback<List<T>>() {
			public List<T> doInHibernate(Session session) throws HibernateException,	SQLException {
				Criteria criteria = session.createCriteria(clazz);
				if (matchAll) {
					for (Map.Entry<String, String> entry : fieldAndValue.entrySet()) {
						Conjunction c = Restrictions.conjunction();
						c.add(Restrictions.eq(entry.getKey(), entry.getValue()).ignoreCase());
						criteria.add(c);
					}
				} else {
					Disjunction dj = Restrictions.disjunction();
					for (Map.Entry<String, String> entry : fieldAndValue.entrySet()) {
						dj.add(Restrictions.eq(entry.getKey(),entry.getValue()).ignoreCase());
					}
					criteria.add(dj);					
				}
				return (List<T>)criteria.list();
			}
		});
	}

	public CatalogCollection createRootCollectionForPartition(final String owner, final String partitionId, final String name, final Set<Long> permissionIds) {
		final Set<Permission> permissions = this.getPermissionsForIds(permissionIds);
		return (CatalogCollection) getHibernateTemplate().execute(new HibernateCallback<CatalogCollection>() {
			public CatalogCollection doInHibernate(Session session) throws HibernateException,	SQLException {
				LOG.fine("Creating new root collection for  partitionId " + partitionId + " with name " + name);
				CatalogCollection rootCollection = new CatalogCollection(owner, partitionId, name, permissions, null);
				rootCollection = findById(CatalogCollection.class, save(rootCollection));
				rootCollection.getPermissions();
				return rootCollection;
			}
		});
		
	}
}