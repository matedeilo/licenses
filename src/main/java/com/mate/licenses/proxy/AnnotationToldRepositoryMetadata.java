package com.mate.licenses.proxy;

import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.util.Assert;

import java.io.Serializable;

public class AnnotationToldRepositoryMetadata extends AbstractToldRepositoryMetadata {

    private static final String NO_ANNOTATION_FOUND = String.format("Interface must be annotated with @%s!",
            RepositoryDefinition.class.getName());

    private final Class<? extends Serializable> idType;
    private final Class<?> domainType;

    public AnnotationToldRepositoryMetadata(Class<?> repositoryInterface) {

        super(repositoryInterface);
        Assert.isTrue(repositoryInterface.isAnnotationPresent(RepositoryDefinition.class), NO_ANNOTATION_FOUND);

        this.idType = resolveIdType(repositoryInterface);
        this.domainType = resolveDomainType(repositoryInterface);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.RepositoryMetadata#getIdType()
     */
    @Override
    public Class<? extends Serializable> getIdType() {
        return this.idType;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.RepositoryMetadata#getDomainType()
     */
    @Override
    public Class<?> getDomainType() {
        return this.domainType;
    }

    private Class resolveIdType(Class<?> repositoryInterface) {

        RepositoryDefinition annotation = repositoryInterface.getAnnotation(RepositoryDefinition.class);

        if (annotation == null || annotation.idClass() == null) {
            throw new IllegalArgumentException(String.format("Could not resolve id type of %s!", repositoryInterface));
        }

        return annotation.idClass();
    }

    private Class<?> resolveDomainType(Class<?> repositoryInterface) {

        RepositoryDefinition annotation = repositoryInterface.getAnnotation(RepositoryDefinition.class);

        if (annotation == null || annotation.domainClass() == null) {
            throw new IllegalArgumentException(String.format("Could not resolve domain type of %s!", repositoryInterface));
        }

        return annotation.domainClass();
    }
}
