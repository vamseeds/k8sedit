package com.oracle.k8sedit;



import java.util.function.Supplier;

import com.oracle.k8sedit.api.API;
import com.oracle.k8sedit.api.APIList;
import com.oracle.k8sedit.api.APISpec;

public class APIs extends AbstractCRDRepository<APISpec, API, APIList> {
    static final Supplier<APIs> DEFAULT_SUPPLIER = APIs::new;
    private static Supplier<APIs> supplierFunction;
    private static APIs theInstance;

    public static APIs getInstance() {
        if (null == theInstance) {
            theInstance = (APIs) supplierFunction.get();
            theInstance.startSyncIfNeeded();
        }

        return theInstance;
    }

    private APIs() {
        super(API.class, APIList.class, "API");
    }

    public static void setSupplierFunction(Supplier<APIs> supplierFunction) {
        theInstance = null;
        APIs.supplierFunction = supplierFunction;
    }

    static {
        supplierFunction = DEFAULT_SUPPLIER;
        theInstance = null;
    }
}
