package com.c9a.service.spring;

import java.io.IOException;
import java.util.logging.Logger;

import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.c9a.service.hibernate.ContextHolder;

public class MultipleSchemaOpenSessionInViewInterceptor extends OpenSessionInViewInterceptor {
	
	private static final Logger LOG = Logger.getLogger(MultipleSchemaOpenSessionInViewInterceptor.class.getName());

	public static final String GPB_REQUEST_MESSAGE = "GPB_REQUEST_MESSAGE";

	@Override
	public void preHandle(WebRequest request) throws DataAccessException {
		ServletWebRequest servletWebRequest = new ServletWebRequest(((ServletWebRequest)request).getRequest());
		try {
			byte[] message = FileCopyUtils.copyToByteArray(servletWebRequest.getRequest().getInputStream());
			com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GenericRequest genericRequest = com.c9a.buffers.CatalogServiceRequestProtocalBuffer.GenericRequest.parseFrom(message);
			LOG.info("Generic Request : " + genericRequest);
			ContextHolder.setCustomerContext(genericRequest.getPartitionId());
			// Put the GPB in a request attribute, since we can no longer read the input stream, its a read once type of situation..
			servletWebRequest.getRequest().setAttribute(GPB_REQUEST_MESSAGE, message);
		} catch (IOException e) {
			// No GPB, or not BaseRequest Message... Ignore, let the CustomerSpecificDataSource class return the default schema
		} 
		super.preHandle(request);
	}
}