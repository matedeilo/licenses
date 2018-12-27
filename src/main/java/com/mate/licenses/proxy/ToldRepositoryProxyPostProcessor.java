package com.mate.licenses.proxy;


import org.springframework.aop.framework.ProxyFactory;

/**
 * Callback interface used during repository proxy creation. Allows manipulating the {@link ProxyFactory} creating the
 * repository.
 *
 * @author Oliver Gierke
 */
public interface ToldRepositoryProxyPostProcessor {

    /**
     * Manipulates the {@link ProxyFactory}, e.g. add further interceptors to it.
     *
     * @param factory will never be {@literal null}.
     * @param repositoryInformation will never be {@literal null}.
     */
    void postProcess(ProxyFactory factory, ToldRepositoryInformation repositoryInformation);
}
