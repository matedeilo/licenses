package com.mate.licenses.proxy;

import java.lang.reflect.Method;

public interface ToldRepositoryInformation extends ToldRepositoryMetadata {

    /**
     * Returns the base class to be used to create the proxy backing instance.
     *
     * @return
     */
    Class<?> getRepositoryBaseClass();

    /**
     * Returns whether the given method is logically a base class method. This also includes methods (re)declared in the
     * repository interface that match the signatures of the base implementation.
     *
     * @param method must not be {@literal null}.
     * @return
     */
    boolean isBaseClassMethod(Method method);


    /**
     * Returns the target class method that is backing the given method. This can be necessary if a repository interface
     * redeclares a method of the core repository interface (e.g. for transaction behavior customization). Returns the
     * method itself if the target class does not implement the given method. Implementations need to make sure the
     * {@link Method} returned can be invoked via reflection, i.e. needs to be accessible.
     *
     * @param method must not be {@literal null}.
     * @return
     */
    Method getTargetClassMethod(Method method);
}
