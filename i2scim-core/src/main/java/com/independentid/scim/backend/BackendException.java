/*
 * Copyright 2021.  Independent Identity Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.independentid.scim.backend;

public class BackendException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BackendException() {
		// TODO Auto-generated constructor stub
	}

	public BackendException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public BackendException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public BackendException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public BackendException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
