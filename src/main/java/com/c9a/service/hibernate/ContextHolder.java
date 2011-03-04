package com.c9a.service.hibernate;

public class ContextHolder {

	private static final ThreadLocal<String> partition = new ThreadLocal<String>();

	public static void setCustomerContext(String context) {
		partition.set(context);
	}

	public static String getCustomerContext() {
		return (String) partition.get();
	}
}