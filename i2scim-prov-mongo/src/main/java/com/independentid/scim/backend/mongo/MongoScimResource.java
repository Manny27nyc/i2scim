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

package com.independentid.scim.backend.mongo;

import com.independentid.scim.core.err.ScimException;
import com.independentid.scim.protocol.RequestCtx;
import com.independentid.scim.resource.*;
import com.independentid.scim.schema.Attribute;
import com.independentid.scim.schema.Schema;
import com.independentid.scim.schema.SchemaException;
import com.independentid.scim.schema.SchemaManager;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Set;

/**
 * @author pjdhunt
 *
 */
/**
 * @author pjdhunt
 *
 */
public class MongoScimResource extends ScimResource {
	private final static Logger logger = LoggerFactory.getLogger(MongoScimResource.class);

	private Document originalResource;

	/**
	 * MongoScimResource wraps ScimResource in order to provide direct Mongo BSON Document mapping.
	 * @param schemaManager Handle to the SCIM server SchemaManager instance
	 */
	protected MongoScimResource(@NotNull SchemaManager schemaManager) {

		super(schemaManager);
	}

	/**
	 * Parses a Mongo <Document> and converts to <ScimResource> using <MongoMapUtil>.
	 * @param schemaManager A handle to SCIM <ConfigMgr> which holds the Schema definitions
	 * @param dbResource A Mongo <Document> object containing the Mongo record to be converted to ScimResource
	 * @param container A Mongo <String> representing the Resource Type path (e.g. Users) for the object. Used to lookup <ResourceType> and <Schema>.
	 * @throws SchemaException is thrown when unable to parse data not defined in SCIM <Schema> configuration
	 * @throws ParseException is thrown when a known format is invalid (e.g. URI, Date, etc)
	 * @throws  ScimException is thrown when a general SCIM protocol error has occurred.
	 */
	public MongoScimResource(SchemaManager schemaManager, Document dbResource, String container)
			throws SchemaException, ParseException, ScimException {
		super(schemaManager);
		this.smgr = schemaManager;
		//super(cfg, MongoMapUtil.toScimJsonNode(dbResource), null);
		
		this.originalResource = dbResource;	
		setResourceType(container);
		parseDocument(dbResource);
		
	}
	
	protected void parseDocument(Document doc) throws ParseException, ScimException {
		

		this.schemas = doc.getList("schemas", String.class);
		if (this.schemas == null)
			throw new SchemaException("Schemas attribute missing");
		
		ObjectId oid = doc.get("_id", ObjectId.class);
		if (oid != null)
			this.id = oid.toString();
		
		this.externalId = doc.getString("externalId");
		
		Document mdoc = doc.get("meta", Document.class);
		if (mdoc != null) {
			this.meta = new Meta();
			this.meta.setCreatedDate(mdoc.getDate(Meta.META_CREATED));
			this.meta.setLastModifiedDate(mdoc.getDate(Meta.META_LAST_MODIFIED));
			this.meta.setResourceType(mdoc.getString(Meta.META_RESOURCE_TYPE));
			this.meta.setLocation(mdoc.getString(Meta.META_LOCATION));
			try {
				this.meta.setVersion(mdoc.getString(Meta.META_VERSION));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Attribute rev = commonSchema.getAttribute(Meta.META).getSubAttribute(Meta.META_REVISIONS);
			this.meta.setRevisions((MultiValue) MongoMapUtil.mapBsonDocument(rev, mdoc));
		}
		
		parseAttributes(doc);
		
	}
	
	protected void parseAttributes(Document doc) throws ScimException, ParseException {
		//ResourceType type = cfg.getResourceType(getResourceType());
		//String coreSchemaId = type.getSchema();

		// Look for all the core schema vals
		//Schema core = cfg.getSchemaById(coreSchemaId);
		
		Attribute[] attrs = coreSchema.getAttributes();
		for (Attribute attr : attrs) {
			Value val = MongoMapUtil.mapBsonDocument(attr, doc);
			if (smgr.isVirtualAttr(attr)) {
				try {  // convert from value type to virtual type
					val = smgr.constructValue(this,attr,val);
					if (val == null)
						val = smgr.constructValue(this,attr);  // try a calculated attribute
				} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NullPointerException e) {
					logger.error("Error mapping attribute "+attr.getName()+": "+e.getMessage(),e);
				}
			}
			attrsInUse.add(attr);
			if (val != null)
				this.coreAttrVals.put(attr, val);
		}
		
		String[] eids = type.getSchemaExtension();
		for (String eid : eids) {
			Schema schema = smgr.getSchemaById(eid);
			ExtensionValues val = MongoMapUtil.mapBsonExtension(schema, doc);
			if (val != null) {
				this.extAttrVals.put(eid, val);
				Set<Attribute> eattrs = val.getAttributeSet();
				if (eattrs != null && !eattrs.isEmpty())
					this.attrsInUse.addAll(eattrs);
			}
		}
		
	}
	
	/**
	 * @return the original Mongo <Document> used to create this <ScimResource>.
	 */
	public Document getOriginalDBObject() {
		return this.originalResource;
	}
	
	public Document toMongoDocument(RequestCtx ctx) {
		return toMongoDocument(this,ctx);
	}
	
	/**
	 * Converts a <ScimResource> object to a Mongo <Document>. Conversion does not modify original ScimResource.
	 * Performs necessary "id" to "_id" conversion.
	 * @param res The <ScimResource> object to be converted
	 * @param ctx The <RequestCtx> indicating the container associated with the resource (usually contains original query).
	 * @return A <Document> representing the mapped <ScimResource>
	 */
	public static Document toMongoDocument(ScimResource res,RequestCtx ctx) {

		return MongoMapUtil.mapResource(res);
	}


	
}