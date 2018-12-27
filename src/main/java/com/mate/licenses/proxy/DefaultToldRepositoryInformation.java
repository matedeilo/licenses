package com.mate.licenses.proxy;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.core.GenericTypeResolver.resolveParameterType;
import static org.springframework.util.ReflectionUtils.makeAccessible;

public class DefaultToldRepositoryInformation implements ToldRepositoryInformation {

    @SuppressWarnings("rawtypes") private static final TypeVariable<Class<ToldRepository>>[] PARAMETERS = ToldRepository.class
            .getTypeParameters();
    private static final String DOMAIN_TYPE_NAME = PARAMETERS[0].getName();
    private static final String ID_TYPE_NAME = PARAMETERS[1].getName();

    private final Map<Method, Method> methodCache = new ConcurrentHashMap<Method, Method>();

    private final ToldRepositoryMetadata metadata;
    private final Class<?> repositoryBaseClass;
    private final Class<?> customImplementationClass;


    public DefaultToldRepositoryInformation(ToldRepositoryMetadata metadata, Class<?> repositoryBaseClass,
                                        Class<?> customImplementationClass) {

        Assert.notNull(metadata);
        Assert.notNull(repositoryBaseClass);

        this.metadata = metadata;
        this.repositoryBaseClass = repositoryBaseClass;
        this.customImplementationClass = customImplementationClass;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.support.RepositoryMetadata#getDomainClass()
     */
    @Override
    public Class<?> getDomainType() {
        return metadata.getDomainType();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.support.RepositoryMetadata#getIdClass()
     */
    @Override
    public Class<?> getIdType() {
        return metadata.getIdType();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.support.RepositoryInformation#getRepositoryBaseClass()
     */
    @Override
    public Class<?> getRepositoryBaseClass() {
        return this.repositoryBaseClass;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.support.RepositoryInformation#getTargetClassMethod(java.lang.reflect.Method)
     */
    @Override
    public Method getTargetClassMethod(Method method) {

        if (methodCache.containsKey(method)) {
            return methodCache.get(method);
        }

        Method result = getTargetClassMethod(method, customImplementationClass);

        if (!result.equals(method)) {
            return cacheAndReturn(method, result);
        }

        return cacheAndReturn(method, getTargetClassMethod(method, repositoryBaseClass));
    }

    private Method cacheAndReturn(Method key, Method value) {

        if (value != null) {
            makeAccessible(value);
        }

        methodCache.put(key, value);
        return value;
    }

    /**
     * Returns whether the given method is considered to be a repository base class method.
     *
     * @param method
     * @return
     */
    private boolean isTargetClassMethod(Method method, Class<?> targetType) {

        Assert.notNull(method);

        if (targetType == null) {
            return false;
        }

        if (method.getDeclaringClass().isAssignableFrom(targetType)) {
            return true;
        }

        return !method.equals(getTargetClassMethod(method, targetType));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.RepositoryInformation#isBaseClassMethod(java.lang.reflect.Method)
     */
    @Override
    public boolean isBaseClassMethod(Method method) {

        Assert.notNull(method, "Method must not be null!");
        return isTargetClassMethod(method, repositoryBaseClass);
    }

    /**
     * Returns the given target class' method if the given method (declared in the repository interface) was also declared
     * at the target class. Returns the given method if the given base class does not declare the method given. Takes
     * generics into account.
     *
     * @param method must not be {@literal null}
     * @param baseClass
     * @return
     */
    Method getTargetClassMethod(Method method, Class<?> baseClass) {

        if (baseClass == null) {
            return method;
        }

        for (Method baseClassMethod : baseClass.getMethods()) {

            // Wrong name
            if (!method.getName().equals(baseClassMethod.getName())) {
                continue;
            }

            // Wrong number of arguments
            if (!(method.getParameterTypes().length == baseClassMethod.getParameterTypes().length)) {
                continue;
            }

            // Check whether all parameters match
            if (!parametersMatch(method, baseClassMethod)) {
                continue;
            }

            return baseClassMethod;
        }

        return method;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.RepositoryMetadata#getRepositoryInterface()
     */
    @Override
    public Class<?> getRepositoryInterface() {
        return metadata.getRepositoryInterface();
    }

    /**
     * Checks the given method's parameters to match the ones of the given base class method. Matches generic arguments
     * agains the ones bound in the given repository interface.
     *
     * @param method
     * @param baseClassMethod
     * @return
     */
    private boolean parametersMatch(Method method, Method baseClassMethod) {

        Class<?>[] methodParameterTypes = method.getParameterTypes();
        Type[] genericTypes = baseClassMethod.getGenericParameterTypes();
        Class<?>[] types = baseClassMethod.getParameterTypes();

        for (int i = 0; i < genericTypes.length; i++) {

            Type genericType = genericTypes[i];
            Class<?> type = types[i];
            MethodParameter parameter = new MethodParameter(method, i);
            Class<?> parameterType = resolveParameterType(parameter, metadata.getRepositoryInterface());

            if (genericType instanceof TypeVariable<?>) {

                if (!matchesGenericType((TypeVariable<?>) genericType, parameterType)) {
                    return false;
                }

                continue;
            }

            if (!type.isAssignableFrom(parameterType) || !type.equals(methodParameterTypes[i])) {
                return false;
            }
        }

        return true;
    }


    private boolean matchesGenericType(TypeVariable<?> variable, Class<?> parameterType) {

        Class<?> entityType = getDomainType();
        Class<?> idClass = getIdType();

        if (ID_TYPE_NAME.equals(variable.getName()) && parameterType.isAssignableFrom(idClass)) {
            return true;
        }

        Type boundType = variable.getBounds()[0];
        String referenceName = boundType instanceof TypeVariable ? boundType.toString() : variable.toString();

        boolean isDomainTypeVariableReference = DOMAIN_TYPE_NAME.equals(referenceName);
        boolean parameterMatchesEntityType = parameterType.isAssignableFrom(entityType);

        // We need this check to be sure not to match save(Iterable) for entities implementing Iterable
        boolean isNotIterable = !parameterType.equals(Iterable.class);

        if (isDomainTypeVariableReference && parameterMatchesEntityType && isNotIterable) {
            return true;
        }

        return false;
    }
}
