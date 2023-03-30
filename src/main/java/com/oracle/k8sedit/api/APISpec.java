/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates. All rights reserved.
 */

package com.oracle.k8sedit.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class APISpec {
    @JsonProperty("api-name")
    private String apiName;

    @JsonProperty("api-id")
    private String apiId;

    @JsonProperty("api-version")
    private String apiVersion;

    @JsonCreator
    public APISpec(@JsonProperty(value = "api-name", required = true) final String apiName,
                   @JsonProperty(value = "api-id", required = true) final String apiId,
                   @JsonProperty(value = "api-version", required = true) final String apiVersion
    ) {
        this.apiName = apiName;
        this.apiId = apiId;
        this.apiVersion = apiVersion;

    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(final String apiName) {
        this.apiName = apiName;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(final String apiId) {
        this.apiId = apiId;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(final String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public String toString() {
        return "APISpec{" +
               "apiName='" + apiName + '\'' +
               ", apiId='" + apiId + '\'' +
               ", apiVersion='" + apiVersion + '\'' +
               '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof APISpec)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        APISpec apiSpec = (APISpec) o;
        return apiName.equals(apiSpec.apiName) &&
               apiId.equals(apiSpec.apiId) &&
               apiVersion.equals(apiSpec.apiVersion);

    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), apiName, apiId, apiVersion);
    }
}
