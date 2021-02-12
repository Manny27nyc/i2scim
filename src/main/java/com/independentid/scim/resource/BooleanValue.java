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

package com.independentid.scim.resource;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.independentid.scim.protocol.RequestCtx;
import com.independentid.scim.schema.Attribute;
import com.independentid.scim.schema.SchemaException;
import com.independentid.scim.serializer.JsonUtil;

import java.io.IOException;
import java.text.ParseException;

public class BooleanValue extends Value {
	public Boolean value;
	
	public BooleanValue() {
	}

	public BooleanValue(Attribute cfg, JsonNode node) throws SchemaException, ParseException {
		super(cfg,node);
		parseJson(node);
	}
	
	public BooleanValue(Attribute name, boolean value) {
		super();
		this.value = value;
		this.jtype = JsonNodeType.BOOLEAN;
	}

	@Override
	public void serialize(JsonGenerator gen, RequestCtx ctx) throws IOException {
		gen.writeBoolean(value);		
	}

	@Override
	public void parseJson(JsonNode node) throws SchemaException, ParseException {
		if (!this.jtype.equals(JsonNodeType.BOOLEAN))
			throw new SchemaException("Invalid field data endpoint. Expecting boolean."+node.toString());
		this.value = node.asBoolean();
	}

	@Override
	public JsonNode toJsonNode(ObjectNode parent, String aname) {
		if (parent == null)
			parent = JsonUtil.getMapper().createObjectNode();
		parent.put(aname,value);
		return parent;
	}

	@Override
	public Boolean getValueArray() {
		return this.value;
	}

}
