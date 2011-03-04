package com.c9a.catalog.service;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

import com.c9a.service.hibernate.ContextHolder;

@Service("asyncProcessService")
public class AsyncProcessServiceImpl {

	private static final Logger LOG = Logger
			.getLogger(AsyncProcessServiceImpl.class.getName());

	@Autowired
	@Qualifier("jmsTemplate")
	private JmsTemplate jmsTemplate;

	@Autowired
	private Queue queue;

	public void addToCache(final Object object) {
		try{
			jmsTemplate.send(queue, new MessageCreator() {
				public Message createMessage(Session session)
						throws JMSException {
					ObjectMessage msg = session.createObjectMessage();
					msg.setStringProperty(
							AsyncProcessServiceListenerImpl.MESSAGE_FIELD_TYPE,
							AsyncProcessServiceListenerImpl.PUT_CACHE_MSG_TYPE);
					msg.setStringProperty(
							AsyncProcessServiceListenerImpl.PARTITION_ID_FIELD,
							ContextHolder.getCustomerContext());
					msg.setObject((Serializable)object);
					return msg;
				}
			});
		} catch (Exception e) {
			LOG.info("Error : " + e.getMessage());
			e.printStackTrace();
		}
	}

}
