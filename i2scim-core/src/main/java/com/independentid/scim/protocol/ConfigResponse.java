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
package com.independentid.scim.protocol;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.independentid.scim.core.ConfigMgr;
import com.independentid.scim.core.err.ForbiddenException;
import com.independentid.scim.core.err.NotFoundException;
import com.independentid.scim.core.err.ScimException;
import com.independentid.scim.resource.ScimResource;
import com.independentid.scim.schema.ResourceType;
import com.independentid.scim.schema.Schema;
import com.independentid.scim.schema.SchemaManager;
import com.independentid.scim.serializer.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Iterator;

/**
 * ConfigResponse acts as a special response provider by acting as a virtual endpoint for Configuration endpoints (e.g.
 * /ResourceTypes, /Schemas, or /ServiceProviderConfig).
 * @author pjdhunt
 */
public class ConfigResponse extends ListResponse {
    private final static Logger logger = LoggerFactory.getLogger(ConfigResponse.class);

    //private RequestCtx ctx;

    final ConfigMgr cmgr;
    final SchemaManager smgr;

    private boolean isResourceResponse = false;

    /**
     * Builds a SCIM response based on the internal SCIM operational configuration (rather than pulled from backend)
     * @param ctx        The {@link RequestCtx} object containing the GET request (either by URL or by POST .search)
     * @param configMgr  The system {@link ConfigMgr} object containing the operational configuration and schema
     * @param maxResults The maximum number of results that may be returned.
     */
    public ConfigResponse(RequestCtx ctx, ConfigMgr configMgr, int maxResults) {
        super(ctx, maxResults);
        this.cmgr = configMgr;

        this.smgr = configMgr.getSchemaManager();

        switch (ctx.endpoint) {
            case ScimParams.PATH_SERV_PROV_CFG:
                if (ctx.getPathId() != null) {
                    setError(new NotFoundException(ctx.getPath() + " not Found"));
                    break;
                }
                try {
                    // If the response has a filter, then ListResponse is required, otherwise just return the config (as ResourceResponse).
                    if (ctx.getFilter() == null)
                        isResourceResponse = true;
                    StringWriter writer = new StringWriter();
                    JsonGenerator gen = JsonUtil.getGenerator(writer, true);
                    serializeServiceProviderConfig(ctx, gen);
                    gen.close();
                    writer.close();
                    JsonNode jCfg = JsonUtil.getJsonTree(writer.toString());
                    ScimResource res = new ScimResource(smgr, jCfg, ScimParams.PATH_SERV_PROV_CFG);

                    if (Filter.checkMatch(res, ctx))
                        this.entries.add(res);

                } catch (ParseException | ScimException | IOException e) {
                    logger.warn("Exception occurred serializing ServiceProviderCponfig", e);

                }

                break;

            case ScimParams.PATH_TYPE_SCHEMAS:
                if (ctx.getFilter() != null) {
                    setError(new ForbiddenException("Filters not permitted on Schemas endpoint"));
                    break;
                }

                if (ctx.getPathId() != null) {
                    Schema sch = smgr.getSchemaById(ctx.getPathId());
                    if (sch == null) {
                        this.setError(new NotFoundException("Not Found: " + ctx.path));
                        break;
                    }

                    // Since we are returning one resource without a filter, just return the resource.
                    isResourceResponse = true;
                    try {
                        JsonNode jsch = sch.toJsonNode();
                        ScimResource res = new ScimResource(smgr, jsch, ScimParams.PATH_TYPE_SCHEMAS);
                        this.entries.add(res);
                    } catch (Exception e) {
                        logger.warn("Exception occurred serializing Schema", e);
                    }
                } else {
                    // Return all schemas

                    for (Schema sch : smgr.getSchemas()) {
                        try {
                            if (sch.getId().equals(ScimParams.SCHEMA_SCHEMA_Common))
                                continue;
                            this.entries.add(new ScimResource(smgr, sch.toJsonNode(), ScimParams.PATH_TYPE_SCHEMAS));
                        } catch (ParseException | ScimException e) {
                            // THis should not happen because it would be caught elsewhere in boot
                            logger.warn("Exception occurred serializing Schema id: " + sch.getId(), e);
                        }
                    }

                }
                break;

            case ScimParams.PATH_TYPE_RESOURCETYPE:
                if (ctx.getFilter() != null) {
                    setError(new ForbiddenException("Filters not permitted on ResourceTypes endpoint"));
                    break;
                }

                if (ctx.getPathId() != null) {
                    Iterator<ResourceType> iter = smgr.getResourceTypes().iterator();
                    ResourceType rt = null;
                    String id = ctx.getPathId();
                    while (iter.hasNext() && rt == null) {
                        ResourceType type = iter.next();
                        if (type.getId().contentEquals(id)) {
                            rt = type;
                        }
                    }

                    if (rt == null) {
                        this.setError(new NotFoundException("Not Found: " + ctx.path));
                        break;
                    }

                    // Since we are returning one resource without a filter, just return the resource.
                    isResourceResponse = true;
                    try {
                        JsonNode jrt = rt.toJsonNode();
                        ScimResource res = new ScimResource(smgr, jrt, ScimParams.PATH_TYPE_RESOURCETYPE);
                        this.entries.add(res);
                    } catch (Exception e) {
                        logger.warn("Exception occurred serializing ResourceType id: " + rt.getId(), e);
                    }
                } else {
                    // Return all ResourceTypes

                    for (ResourceType rt : smgr.getResourceTypes()) {
                        try {

                            this.entries.add(new ScimResource(smgr, rt.toJsonNode(), ScimParams.PATH_TYPE_RESOURCETYPE));
                        } catch (ParseException | ScimException e) {
                            // THis should not happen because it would be caught elsewhere in boot
                            logger.warn("Exception occurred serializing ResourceType id: " + rt.getId(), e);
                        }
                    }

                }
                break;
            default:
                // should not happen
                setError(new NotFoundException("Not Found: " + ctx.getPath()));
        }


        this.totalRes = entries.size();
    }

    @Override
    public void serialize(JsonGenerator gen, RequestCtx ctx, boolean forHash) throws IOException {
        // TODO Auto-generated method stub

        if (this.isResourceResponse && this.entries.size() == 1) {
            // Converts to a resource response for single entry non-filter GETs
            ResourceResponse rresp = new ResourceResponse(this.entries.get(0), ctx);
            rresp.serialize(gen, ctx, forHash);
            return;
        }
        super.serialize(gen, ctx, forHash);
    }

    /**
     * @param path A String containing the path to be checked. Routine checks the SCIM container level path element
     *             only.
     * @return True if the path is part of Schemas, ResourceTypes or ServiceProviderCpnfig endpoints.
     */
    public static boolean isConfigEndpoint(String path) {

        String checkPath = path;
        if (path.contains("/")) {
            String[] elems = path.split("/");
            checkPath = elems[1];
        }

        switch (checkPath) {

            case ScimParams.PATH_TYPE_SCHEMAS:

            case ScimParams.PATH_TYPE_RESOURCETYPE:

            case ScimParams.PATH_SERV_PROV_CFG:
                return true;

            default:
                return false;
        }


    }

    public void serializeServiceProviderConfig(RequestCtx ctx, JsonGenerator gen) throws IOException {

        gen.writeStartObject();

        gen.writeArrayFieldStart("schemas");
        gen.writeString(ScimParams.SCHEMA_SCHEMA_ServiceProviderConfig);
        gen.writeEndArray();

        // Identify the server
        gen.writeStringField("ProductName", "Independent Identity i2scim Directory");
        //gen.writeStringField("ProductId", "BigDirectory");
        gen.writeStringField("ProductVersion", "V0.6.0-Alpha");


        /* Not defined in standard schema.
        gen.writeArrayFieldStart("ScimVersionSupport");
        gen.writeString("2.0");
        gen.writeEndArray();
        */

        // Documentation
        gen.writeStringField("documentationUri", "http://i2scim.io");


        // Indicate Patch supported
        gen.writeFieldName("patch");
        gen.writeStartObject();
        gen.writeBooleanField("supported", true);
        gen.writeEndObject();

        // Indicate Bulk support

        gen.writeFieldName("bulk");
        gen.writeStartObject();
        gen.writeBooleanField("supported", false);
        gen.writeNumberField("maxOperations", 0);
        gen.writeNumberField("maxPayloadSize", 0);
        gen.writeEndObject();

        // Indicate Filter support
        gen.writeFieldName("filter");
        gen.writeStartObject();
        gen.writeBooleanField("supported", true);
        gen.writeNumberField("maxResults", cmgr.getMaxResults());
        gen.writeEndObject();

        // Change Password support
        gen.writeFieldName("changePassword");
        gen.writeStartObject();
        gen.writeBooleanField("supported", true);
        gen.writeEndObject();

        // Sorting
        gen.writeFieldName("sort");
        gen.writeStartObject();
        gen.writeBooleanField("supported", true);
        gen.writeEndObject();


        // ETag
        gen.writeFieldName("etag");
        gen.writeStartObject();
        gen.writeBooleanField("supported", true);
        gen.writeEndObject();

        // Authentication Schemes
        gen.writeArrayFieldStart("authenticationSchemes");
        if (this.cmgr.isAuthBasic()) {
            gen.writeStartObject();
            gen.writeStringField("name", "httpbasic");
            gen.writeStringField("description", "HTTP Basic Authentication");
            gen.writeStringField("specUri", "https://www.ietf.org/rfc/rfc2617.txt");
            gen.writeEndObject();

        }
        if (this.cmgr.isAuthJwt()) {
            gen.writeStartObject();
            gen.writeStringField("name", "oauthbearertoken");
            gen.writeStringField("description", "OAuth JWT Bearer Token");
            gen.writeStringField("specUri", "https://tools.ietf.org/html/rfc7523");
            gen.writeEndObject();
        }
        gen.writeEndArray();

        gen.writeEndObject();
    }


}
