/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sorcer.service;

public class ServiceException extends Exception {

	private static final long serialVersionUID = 1L;
	/**  Embedded exception for this ServiceException */
	
	private Exception embedded;
	
	public ServiceException() {
	}

	public ServiceException(String msg) {
		super(msg);
	}
	
	/**
	 * Constructs a new ServiceException with an embedded exception.
	 * 
	 * @param exception
	 *            embedded exception
	 */
	public ServiceException(Exception exception) {
		this();
		embedded = exception;
	}

	public ServiceException(String msg, Exception e) {
		super(msg);
		e.printStackTrace();
	}
}
