package com.mate.licenses.proxy;

import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.List;

public class DefaultToldRepositoryMetadata extends AbstractToldRepositoryMetadata {

    private static final String MUST_BE_A_REPOSITORY = String.format("Given type must be assignable to %s!",
            ToldRepository.class);

    private final Class<? extends Serializable> idType;
    private final Class<?> domainType;


    public DefaultToldRepositoryMetadata(Class<?> repositoryInterface) {

        super(repositoryInterface);
        Assert.isTrue(ToldRepository.class.isAssignableFrom(repositoryInterface), MUST_BE_A_REPOSITORY);

        this.idType = resolveIdType(repositoryInterface);
        this.domainType = resolveDomainType(repositoryInterface);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.RepositoryMetadata#getDomainType()
     */
    @Override
    public Class<?> getDomainType() {
        return this.domainType;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.RepositoryMetadata#getIdType()
     */
    @Override
    public Class<? extends Serializable> getIdType() {
        return this.idType;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Serializable> resolveIdType(Class<?> repositoryInterface) {

        TypeInformation<?> information = ClassTypeInformation.from(repositoryInterface);
        List<TypeInformation<?>> arguments = information.getSuperTypeInformation(ToldRepository.class).getTypeArguments();

        if (arguments.size() < 2 || arguments.get(1) == null) {
            throw new IllegalArgumentException(String.format("Could not resolve id type of %s!", repositoryInterface));
        }

        return (Class<? extends Serializable>) arguments.get(1).getType();
    }

    private Class<?> resolveDomainType(Class<?> repositoryInterface) {

        TypeInformation<?> information = ClassTypeInformation.from(repositoryInterface);
        List<TypeInformation<?>> arguments = information.getSuperTypeInformation(ToldRepository.class).getTypeArguments();

        if (arguments.isEmpty() || arguments.get(0) == null) {
            throw new IllegalArgumentException(String.format("Could not resolve domain type of %s!", repositoryInterface));
        }

        return arguments.get(0).getType();
    }
}
