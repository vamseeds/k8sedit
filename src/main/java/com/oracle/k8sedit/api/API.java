/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates. All rights reserved.
 */

package com.oracle.k8sedit.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oracle.k8sedit.ForgivingCustomResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Singular;
import io.fabric8.kubernetes.model.annotation.Version;


@Singular("api")
@Plural("apis")
@Group("test.oracle.com")
@Version("v1")
@Kind("API")
public class API extends ForgivingCustomResource<APISpec, Void> implements Namespaced {
    @JsonProperty("metadata")
    private  ObjectMeta metadata;

    @JsonProperty("spec")
    private  APISpec spec;

    @JsonCreator
    public API(@JsonProperty(value = "metadata", required = true) final  ObjectMeta metadata,
               @JsonProperty(value = "spec", required = true) final  APISpec spec) {
        this.metadata = metadata;
        this.spec = spec;
    }

    @Override
    public  ObjectMeta getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(final  ObjectMeta metadata) {
        this.metadata = metadata;
    }

    @Override
    public  APISpec getSpec() {
        return spec;
    }

    @Override
    public void setSpec(final  APISpec spec) {
        this.spec = spec;
    }

    @Override
    public String toString() {
        return "API{" +
                "metadata=" + metadata +
                ", spec=" + spec +
                '}';
    }

    @Override
    public boolean equals(final  Object o) {
        if (this == o) return true;
        if (!(o instanceof API)) return false;
        if (!super.equals(o)) return false;
        API api = (API) o;
        return metadata.equals(api.metadata) && spec.equals(api.spec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), metadata, spec);
    }
}
