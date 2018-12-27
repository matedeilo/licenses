package com.mate.licenses.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.*;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class ToldFactorySupport implements BeanClassLoaderAware, BeanFactoryAware {

    private static final Class<?> TRANSACTION_PROXY_TYPE = getTransactionProxyType();
    private static final Logger LOG = LoggerFactory.getLogger(ToldFactorySupport.class);

    private ClassLoader classLoader;
    private EvaluationContextProvider evaluationContextProvider;
    private BeanFactory beanFactory;
    private Class<?> repositoryBaseClass;
    private List<ToldRepositoryProxyPostProcessor> postProcessors;


    private final Map<RepositoryInformationCacheKey, ToldRepositoryInformation> repositoryInformationCache;

    @SuppressWarnings("null")
    public ToldFactorySupport() {
        this.repositoryInformationCache = new ConcurrentReferenceHashMap<RepositoryInformationCacheKey, ToldRepositoryInformation>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);

        this.classLoader = org.springframework.util.ClassUtils.getDefaultClassLoader();
        this.postProcessors = new ArrayList<ToldRepositoryProxyPostProcessor>();
    }


    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader == null ? org.springframework.util.ClassUtils.getDefaultClassLoader() : classLoader;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public <T> T getRepository(Class<T> repositoryInterface, Object customImplementation) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Initializing repository instance for {}…", repositoryInterface.getName());
        }

        Assert.notNull(repositoryInterface, "Repository interface must not be null!");

        ToldRepositoryMetadata metadata = getRepositoryMetadata(repositoryInterface);
        Class<?> customImplementationClass = null == customImplementation ? null : customImplementation.getClass();
        ToldRepositoryInformation information = getRepositoryInformation(metadata, customImplementationClass);

        validate(information, customImplementation);

        Object target = getTargetRepository(information);

        // Create proxy
        ProxyFactory result = new ProxyFactory();
        result.setTarget(target);
        result.setInterfaces(new Class[] { repositoryInterface, Repository.class });

        result.addAdvice(ExposeInvocationInterceptor.INSTANCE);

        if (TRANSACTION_PROXY_TYPE != null) {
            result.addInterface(TRANSACTION_PROXY_TYPE);
        }

        for (ToldRepositoryProxyPostProcessor processor : postProcessors) {
            processor.postProcess(result, information);
        }

        result.addAdvice(new DefaultMethodInvokingMethodInterceptor());

        T repository = (T) result.getProxy(classLoader);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Finished creation of repository instance for {}.", repositoryInterface.getName());
        }

        return repository;
    }

    protected ToldRepositoryMetadata getRepositoryMetadata(Class<?> repositoryInterface) {
        return AbstractToldRepositoryMetadata.getMetadata(repositoryInterface);
    }

    protected abstract Class<?> getRepositoryBaseClass(ToldRepositoryMetadata metadata);

    protected ToldRepositoryInformation getRepositoryInformation(ToldRepositoryMetadata metadata, Class<?> customImplementationClass) {

        RepositoryInformationCacheKey cacheKey = new RepositoryInformationCacheKey(metadata, customImplementationClass);
        ToldRepositoryInformation repositoryInformation = repositoryInformationCache.get(cacheKey);

        if (repositoryInformation != null) {
            return repositoryInformation;
        }

        Class<?> repositoryBaseClass = this.repositoryBaseClass == null ? getRepositoryBaseClass(metadata)
                : this.repositoryBaseClass;

        repositoryInformation = new DefaultToldRepositoryInformation(metadata, repositoryBaseClass, customImplementationClass);
        repositoryInformationCache.put(cacheKey, repositoryInformation);
        return repositoryInformation;
    }

    private void validate(ToldRepositoryInformation repositoryInformation, Object customImplementation) {

        if (null == customImplementation) {

            throw new IllegalArgumentException(
                    String.format("You have custom methods in %s but not provided a custom implementation!",
                            repositoryInformation.getRepositoryInterface()));
        }

        validate(repositoryInformation);
    }

    protected void validate(ToldRepositoryMetadata repositoryMetadata) {

    }

    protected abstract Object getTargetRepository(ToldRepositoryInformation metadata);

    private static Class<?> getTransactionProxyType() {

        try {
            return org.springframework.util.ClassUtils
                    .forName("org.springframework.transaction.interceptor.TransactionalProxy", null);
        } catch (ClassNotFoundException o_O) {
            return null;
        }
    }

    private static class RepositoryInformationCacheKey {

        private final String repositoryInterfaceName;
        private final String customImplementationClassName;

        public RepositoryInformationCacheKey(ToldRepositoryMetadata metadata, Class<?> customImplementationType) {
            this.repositoryInterfaceName = metadata.getRepositoryInterface().getName();
            this.customImplementationClassName = customImplementationType == null ? null : customImplementationType.getName();
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {

            if (!(obj instanceof RepositoryInformationCacheKey)) {
                return false;
            }

            RepositoryInformationCacheKey that = (RepositoryInformationCacheKey) obj;
            return this.repositoryInterfaceName.equals(that.repositoryInterfaceName)
                    && ObjectUtils.nullSafeEquals(this.customImplementationClassName, that.customImplementationClassName);
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {

            int result = 31;

            result += 17 * repositoryInterfaceName.hashCode();
            result += 17 * ObjectUtils.nullSafeHashCode(customImplementationClassName);

            return result;
        }
    }
}
