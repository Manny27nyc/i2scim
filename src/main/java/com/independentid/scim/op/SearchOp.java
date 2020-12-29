/*
 * Copyright (c) 2020.
 *
 * Confidential and Proprietary
 *
 * This unpublished source code may not be distributed outside
 * “Independent Identity Org”. without express written permission of
 * Phillip Hunt.
 *
 * People at companies that have signed necessary non-disclosure
 * agreements may only distribute to others in the company that are
 * bound by the same confidentiality agreement and distribution is
 * subject to the terms of such agreement.
 */
package com.independentid.scim.op;

import com.fasterxml.jackson.databind.JsonNode;
import com.independentid.scim.backend.BackendException;
import com.independentid.scim.core.err.InternalException;
import com.independentid.scim.core.err.InvalidSyntaxException;
import com.independentid.scim.core.err.ScimException;
import com.independentid.scim.protocol.ScimParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * The SCIM Search Operation is invoked by an HTTP POST request. Instead of parsing the URL, Search processes the
 * HTTP Payload as per RFC7644 Sec 3.4.3
 * @author pjdhunt
 *
 */
public class SearchOp extends Operation {

	private static final long serialVersionUID = -3586153424932556487L;
	private final static Logger logger = LoggerFactory.getLogger(SearchOp.class);

	/**
	 * @param req HttpServletRequest object
	 * @param resp HttpServletResponse object
	 */
	public SearchOp(HttpServletRequest req,
					HttpServletResponse resp) {
		super(req, resp );

		if (!req.getRequestURI().endsWith(ScimParams.PATH_SEARCH)) {
			InternalException ie = new InternalException(
					"Was expecting a search request, got: "
							+ req.getRequestURI());
			setCompletionError(ie);
		}
	}

	@Override
	protected void doPreOperation() {
		parseRequestUrl();
		if (opState == OpState.invalid)
			return;

		ServletInputStream bodyStream;
		try {
			bodyStream = getRequest().getInputStream();
			//Because the backendhalder logic just uses RequestCtx, the search body is handled by RequestCtx
			this.ctx.parseSearchBody(bodyStream);
		} catch (IOException | ScimException e) {
			setCompletionError(new InvalidSyntaxException(
					"Unable to parse request body (SCIM JSON Search Schema format expected)."));
			this.opState = OpState.invalid;
		}
		try {
			pluginHandler.doPreOperations(this);
		} catch (ScimException e) {
			e.printStackTrace();
		}

	}

	/* (non-Javadoc)
	 * @see com.independentid.scim.op.Operation#doOperation()
	 */
	@Override
	protected void doOperation() {
		try {
			this.scimresp = backendHandler.get(ctx);

		} catch (ScimException e) {
			setCompletionError(e);

		} catch (BackendException e) {
			ScimException se = new InternalException("Unknown backend exception during SCIM Search: "+e.getLocalizedMessage(),e);
			setCompletionError(se);
			logger.error(
					"Received backend error while processing SCIM Search for: ["
							+ this.ctx.getPath() + "] " + e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see com.independentid.scim.op.Operation#parseJson(com.fasterxml.jackson.databind.JsonNode)
	 */
	@Override
	protected void parseJson(JsonNode node) {
		// not used. {@link RequestCtx#parseSearchBody} used instead.

	}

}
