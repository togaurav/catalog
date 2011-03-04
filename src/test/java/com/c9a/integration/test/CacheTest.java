package com.c9a.integration.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.hibernate.criterion.Order;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.exception.CollectionModificationException;
import com.c9a.catalog.exception.CollectionNotFoundException;
import com.c9a.catalog.exception.InvalidCatalogNameException;
import com.c9a.catalog.exception.InvalidCollectionPathException;

@RunWith(SpringJUnit4ClassRunner.class)
public class CacheTest extends AbstractIntegrationTest {
	
	private static final Logger LOG = Logger.getLogger(CacheTest.class.getName());
	
	private StringBuilder log = new StringBuilder();
	
	@Test
	public void cacheTest2() throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException, InvalidCollectionPathException{
		int reads = 10000;
		Long start = System.currentTimeMillis();
		for(int i =0; i <= reads ; i++){
			CatalogCollection user1Root = catalogService.getCollectionByPath("user1@company.com", partitionId, "/users/user1@company.com/");
			readNested(user1Root);
		}
		LOG.info("Done : " + reads + " in " + (System.currentTimeMillis() - start) + " ms ");
		LOG.info(log.toString());
	}
	
	private void readNested(CatalogCollection collection) {
		for(CatalogCollection nc : collection.getNestedCollections()){
			readNested(nc);
		}
		log.append(collection.getPath() + "\n");
	}

	//	@Test
	public void cacheTest() throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException{
		String owner = "CacheTestOwner";
		CatalogCollection cc = catalogService.getRootApplicationCollectionForUser(owner, partitionId, "cacheTest");
		int reads = 10000;
		Long millStart = System.currentTimeMillis();
		for(int i =0; i <= reads ; i++){
			catalogService.getCollection(owner, partitionId, cc.getUniqueId());
		}
		LOG.info(reads + " reads : " + (System.currentTimeMillis() - millStart) + " ms");
		
		int writes = 1000;
		millStart = System.currentTimeMillis();
		List<String> uIds = new ArrayList<String>();
		for(int i = 0; i <= writes;i++){
			uIds.add(catalogService.addCollection(owner, partitionId, "NewName"+i, cc.getUniqueId(), null, null).getUniqueId());
		}
		LOG.info(writes + " writes : " + (System.currentTimeMillis() - millStart) + " ms");
		millStart = System.currentTimeMillis();
		for(String uuid : uIds){
			for(int i =0; i <= 100 ; i++){
				catalogService.getCollection(owner, partitionId, uuid);
			}	
		}
		LOG.info(100 + " reads of " + writes + " elements : " + (System.currentTimeMillis() - millStart) + " ms");
	}
	
//	@Test
//	public void testDeleteFromCache() throws CollectionNotFoundException, CollectionModificationException, InvalidCatalogNameException{
//		String owner = "testDeleteFromCache";
//		CatalogCollection cc = catalogService.getRootApplicationCollectionForUser(owner, partitionId, "cacheTest");
//		CatalogCollection toDelete = catalogService.addCollection(owner, partitionId, "NewToDeltet", cc.getUniqueId(), null, null);
//		catalogService.deleteCollection(owner, partitionId, toDelete.getUniqueId());
//		catalogService.getCollection(owner, partitionId, toDelete.getUniqueId());
//	}
//	
//	@Test
//	public void deleteAll(){
//		List<CatalogCollection> ccs = catalogDao.findAll(CatalogCollection.class, new Order[]{Order.asc("id")});
//		for(CatalogCollection cc : ccs){
//			catalogDao.delete(cc);
//		}
//		catalogDao.flush();
//		System.out.println("Here");
//	}
}