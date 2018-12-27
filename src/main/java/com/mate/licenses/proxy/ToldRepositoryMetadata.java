package com.mate.licenses.proxy;

public interface ToldRepositoryMetadata {

    /**
     * Returns the id class the given class is declared for.
     *
     * @return the id class of the entity managed by the repository.
     */
    Class<?> getIdType();

    /**
     * Returns the domain class the repository is declared for.
     *
     * @return the domain class the repository is handling.
     */
    Class<?> getDomainType();

    /**
     * Returns the repository interface.
     *
     * @return
     */
    Class<?> getRepositoryInterface();


}
