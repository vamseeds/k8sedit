package com.oracle.k8sedit;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.fabric8.kubernetes.client.CustomResource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class ForgivingCustomResource<T, R> extends CustomResource<T, R> {
    /**
     * container for all attributes that are not either part of the existing definition or one of the subclasses
     */
    private final Map<String, Object> additionalAttributes = new HashMap<>();

    @JsonAnySetter
    public void addAdditionalAttribute(final String jsonKey,
                                       final Object node) {
        additionalAttributes.put(jsonKey, node);
    }

    @JsonAnyGetter
    public Object getAdditionalAttribute(final String key) {
        return additionalAttributes.get(key);
    }

    @Override
    public String toString() {
        return "ForgivingCustomResource{" +
               "additionalAttributes=" + additionalAttributes +
               '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ForgivingCustomResource)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ForgivingCustomResource<?, ?> that = (ForgivingCustomResource<?, ?>) o;
        return additionalAttributes.equals(that.additionalAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), additionalAttributes);
    }
}
