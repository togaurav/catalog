package com.c9a.catalog.service;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.c9a.catalog.entities.CatalogCollection;
import com.c9a.catalog.entities.CollectionAttribute;
import com.c9a.catalog.exception.CollectionModificationException;
import com.c9a.catalog.exception.CollectionNotFoundException;
import com.c9a.catalog.exception.InvalidCatalogNameException;
import com.c9a.service.hibernate.ContextHolder;

@Service("asyncProcessServiceListener")
public class AsyncProcessServiceListenerImpl implements MessageListener {
	
	private static final Logger LOG = Logger.getLogger(AsyncProcessServiceListenerImpl.class.getName());

	//For messaging
	public static final String MESSAGE_FIELD_TYPE = "MESSAGE_TYPE";
	public static final String SAVE_STATS = "SAVE_STATS";
	
	//Static constants for catalog structure of the stats
	public static final String STAT_FIELD = "STAT_FIELD";
	private static final String STATS_APPLICATION_NAME = "STATS";

	public static final String PUT_CACHE_MSG_TYPE = "PUT_CACHE";

	public static final String PARTITION_ID_FIELD = "PARTITION_ID_FIELD";
	
	@Autowired
	private ICatalogService catalogService;
	
	@Autowired
	private CatalogCacheManager cacheManger;
	
	@Autowired
	@Qualifier("c3p0Properties")
	protected Properties c3p0Properties;
	
	@Override
	@Transactional
	public void onMessage(Message message) {
		if (message instanceof MapMessage) {
			String partitionId = "STATSCHEMA";
			ContextHolder.setCustomerContext(partitionId);
			try {
				MapMessage mapMessage = (MapMessage) message;
				String msgType = mapMessage.getString(MESSAGE_FIELD_TYPE);
				if(SAVE_STATS.equals(msgType)){
					String stat = mapMessage.getString(STAT_FIELD);
					LOG.info("RECIEVED A SAVE STATS MSG of " + stat);
					CatalogCollection statsCollection = catalogService.getRootApplicationCollectionForUser(CatalogCollection.SYSTEM_OWNER, partitionId, STATS_APPLICATION_NAME);
					Map<String, String> attributes = new HashMap<String, String>();
					for(CollectionAttribute ca : statsCollection.getAttributes()){
						attributes.put(ca.getKey(), ca.getValue());
					}
					attributes.put(("NEWKEY"+new Date().getTime()), stat);
					try {
						catalogService.modifyCollection(CatalogCollection.SYSTEM_OWNER, partitionId, statsCollection.getUniqueId(), null, null, null, attributes);
					} catch (CollectionNotFoundException e) {
						e.printStackTrace();
					} catch (CollectionModificationException e) {
						e.printStackTrace();
					} catch (InvalidCatalogNameException e) {
						e.printStackTrace();
					}
				}
			} catch (JMSException e) {
				e.printStackTrace();
			}
		} else if(message instanceof ObjectMessage){
			try {
				ObjectMessage mapMessage = (ObjectMessage) message;
				String msgType = mapMessage.getStringProperty(MESSAGE_FIELD_TYPE);
				if (PUT_CACHE_MSG_TYPE.equals(msgType)) {
					ContextHolder.setCustomerContext(mapMessage.getStringProperty(PARTITION_ID_FIELD));
					cacheManger.cacheIfAppropriate(mapMessage.getObject());
				}
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}

}
