package com.c9a.service.spring;

import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

public class ControllerUtil {
	
	public static byte[] getContentForRequest(HttpServletRequest request) throws IOException {
		byte[] content = null;
		ServletInputStream sins = request.getInputStream();
		//
		// The Content is supposed to a serialized stream of Google protocol
		// buffers. We will decode the stream based
		// on the URL fragment.
		//
		int contentLen = request.getContentLength();
		if (contentLen > 0) {
			content = new byte[contentLen];
			sins.read(content, 0, contentLen);
		}
		return content;
	}

}
