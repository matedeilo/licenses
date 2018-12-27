package com.mate.licenses.proxy;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AnnotationRepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultCrudMethods;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

public abstract class AbstractToldRepositoryMetadata implements ToldRepositoryMetadata {

    private final TypeInformation<?> typeInformation;
    private final Class<?> repositoryInterface;

    public AbstractToldRepositoryMetadata(Class<?> repositoryInterface) {

        Assert.notNull(repositoryInterface, "Given type must not be null!");
        Assert.isTrue(repositoryInterface.isInterface(), "Given type must be an interface!");

        this.repositoryInterface = repositoryInterface;
        this.typeInformation = ClassTypeInformation.from(repositoryInterface);
    }

    /**
     * Creates a new {@link RepositoryMetadata} for the given repsository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     * @since 1.9
     * @return
     */
    public static ToldRepositoryMetadata getMetadata(Class<?> repositoryInterface) {

        Assert.notNull(repositoryInterface, "Repository interface must not be null!");

        return Repository.class.isAssignableFrom(repositoryInterface) ? new DefaultToldRepositoryMetadata(repositoryInterface)
                : new AnnotationToldRepositoryMetadata(repositoryInterface);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.RepositoryMetadata#getReturnedDomainClass(java.lang.reflect.Method)
     */
    public Class<?> getReturnedDomainClass(Method method) {
        return unwrapWrapperTypes(typeInformation.getReturnType(method));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.RepositoryMetadata#getRepositoryInterface()
     */
    public Class<?> getRepositoryInterface() {
        return this.repositoryInterface;
    }

    /**
     * Recursively unwraps well known wrapper types from the given {@link TypeInformation}.
     *
     * @param type must not be {@literal null}.
     * @return
     */
    private static Class<?> unwrapWrapperTypes(TypeInformation<?> type) {

        Class<?> rawType = type.getType();

        boolean needToUnwrap = Iterable.class.isAssignableFrom(rawType) || rawType.isArray()
                || QueryExecutionConverters.supports(rawType);

        return needToUnwrap ? unwrapWrapperTypes(type.getComponentType()) : rawType;
    }

}
