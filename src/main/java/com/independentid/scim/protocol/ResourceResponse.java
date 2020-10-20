/**********************************************************************
 *  Independent Identity - Big Directory                              *
 *  (c) 2015,2020 Phillip Hunt, All Rights Reserved                        *
 *                                                                    *
 *  Confidential and Proprietary                                      *
 *                                                                    *
 *  This unpublished source code may not be distributed outside       *
 *  “Independent Identity Org”. without express written permission of *
 *  Phillip Hunt.                                                     *
 *                                                                    *
 *  People at companies that have signed necessary non-disclosure     *
 *  agreements may only distribute to others in the company that are  *
 *  bound by the same confidentiality agreement and distribution is   *
 *  subject to the terms of such agreement.                           *
 **********************************************************************/

package com.independentid.scim.protocol;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.independentid.scim.resource.ScimResource;
import com.independentid.scim.server.ScimException;

/**
 * ResourceResponse is used to generate a SCIM response per RFC7644. This response
 * format returns a ScimResource format directly. 
 * @author pjdhunt
 *
 */
public class ResourceResponse extends ScimResponse {
	private static final Logger logger = LoggerFactory.getLogger(ListResponse.class);
	final static SimpleDateFormat headDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	
	protected Date lastMod;
	protected int totalRes;
	protected RequestCtx ctx;
	protected int smax;  // max server response size
	protected String id;

	protected ArrayList<ScimResource> entries = new ArrayList<ScimResource>();
	
	
	public ResourceResponse(ScimResource val, RequestCtx ctx) {
		super();
		this.ctx = ctx;
		
		this.smax = this.ctx.getConfigMgr().getMaxResults();
		if (this.ctx.count == 0 || this.ctx.count > this.smax)  
			this.ctx.count = this.smax;
		
		if (val.getMeta() == null) {
			// This typically happens in server config endpoints
			String cp = ctx.sctx.getContextPath();
			setLocation(cp + ctx.getPath());
		} else {
			setLocation(val.getMeta().getLocation());
		
			this.etag = val.getMeta().getVersion();
			this.lastMod = val.getMeta().getLastModifiedDate();
		}
		this.id = val.getId();
		this.entries.add(val);
		this.totalRes = 1;
		
	}
	
	public String getId() {
		return this.id;
	}
	
	public int getSize() {
		return this.entries.size();
	}

	/* (non-Javadoc)
	 * @see com.independentid.scim.protocol.ScimResponse#serialize(com.fasterxml.jackson.core.JsonGenerator, com.independentid.scim.protocol.RequestCtx)
	 */
	@Override
	public void serialize(JsonGenerator gen, RequestCtx sctx) throws IOException {
		serialize(gen, sctx, false);
	}

	/* (non-Javadoc)
	 * @see com.independentid.scim.protocol.ScimResponse#serialize(com.fasterxml.jackson.core.JsonGenerator, com.independentid.scim.protocol.RequestCtx)
	 */
	@Override
	public void serialize(JsonGenerator gen, RequestCtx ctx, boolean forHash) throws IOException {
		
		//TODO: What happens if getStatus = HttpServletResponse.SC_OK
		//TODO: What if entries.size == 0?
		
		/* Note: Normally this.ctx and sctx are the same. However server may modify
		 * sctx after result set creation (or have chosen to insert an override). 
		 */
		
		HttpServletResponse resp = ctx.getHttpServletResponse(); 
		
		
		// For single results, just return the object itself.
		ScimResource resource = this.entries.get(0);
		try {
			resource.serialize(gen, ctx, false);
		} catch (ScimException e) {
			//TODO This should not happen
			logger.error("Unexpected exception serializing a response value: "+e.getMessage(),e);
		}
		
		resp.setStatus(getStatus());
		if (this.lastMod != null)
			resp.setHeader(ScimParams.HEADER_LASTMOD, headDate.format(this.lastMod));
		if (this.getLocation() != null)	
			resp.setHeader("Location", this.getLocation());
		if (this.etag != null) {
			resp.setHeader("ETag", "\""+this.etag+"\"");
		}
		
	}
	
}
