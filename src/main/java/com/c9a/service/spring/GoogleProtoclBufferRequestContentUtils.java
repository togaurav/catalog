package com.c9a.service.spring;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;


public class GoogleProtoclBufferRequestContentUtils {
	
	public static byte[] getContentForRequest(HttpServletRequest request) throws IOException{
		// This is assuming that the data has already been read and put into request attribute
		// It has already been read as part of the service structure to determine the schema to send all db requests to based on the partition id, which is read from the original request
		// The MultipleSchemaOpenSessionInViewFilter will pull out the GPB message and use the partition id to make the db determination, after it reads the inputstream, it is no longer avail, so the GPB message 
		// is put as a Request Attribute and then extracted here, and can actually be "read" multiple times now.
		return (byte[])request.getAttribute(MultipleSchemaOpenSessionInViewInterceptor.GPB_REQUEST_MESSAGE);
	}
}
