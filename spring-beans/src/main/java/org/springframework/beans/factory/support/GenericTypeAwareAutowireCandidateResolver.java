/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Basic {@link AutowireCandidateResolver} that performs a full generic type
 * match with the candidate's type if the dependency is declared as a generic type
 * (e.g. Repository&lt;Customer&gt;).
 *
 * <p>This is the base class for
 * {@link org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver},
 * providing an implementation all non-annotation-based resolution steps at this level.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class GenericTypeAwareAutowireCandidateResolver extends SimpleAutowireCandidateResolver
		implements BeanFactoryAware {

	@Nullable
	private BeanFactory beanFactory;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Nullable
	protected final BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * 根据超类和泛型判断是否允许自动注入
	 * 如果超类方法允许自动注入且泛型匹配成功, 则允许自动注入
	 * @param bdHolder 被依赖的资源 BeanDefinitionHolder
	 * @param descriptor 依赖描述符
	 * @return
	 */
	@Override
	public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		// 从超类方法判断是否允许自动注入
		if (!super.isAutowireCandidate(bdHolder, descriptor)) {
			return false;
		}
		// 被依赖方是否和依赖方的泛型类型匹配
		return checkGenericTypeMatch(bdHolder, descriptor);
	}

	protected boolean checkGenericTypeMatch(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		// 取依赖方类型
		// 如果依赖方类型不是泛型, 则直接返回 true
		ResolvableType dependencyType = descriptor.getResolvableType();
		if (dependencyType.getType() instanceof Class) {
			return true;
		}
		// 取被依赖方类型
		ResolvableType targetType = null;
		boolean cacheType = false;
		RootBeanDefinition rbd = null;
		if (bdHolder.getBeanDefinition() instanceof RootBeanDefinition) {
			rbd = (RootBeanDefinition) bdHolder.getBeanDefinition();
		}
		if (rbd != null) {
			targetType = rbd.targetType;
			// 如果 targetType 为 null, 则从 FactoryMethod 取
			if (targetType == null) {
				cacheType = true;
				targetType = getReturnTypeForFactoryMethod(rbd, descriptor);
				// 如果 targetType 还是为 null, 则从 DecoratedDefinition 取
				if (targetType == null) {
					RootBeanDefinition dbd = getResolvedDecoratedDefinition(rbd);
					if (dbd != null) {
						targetType = dbd.targetType;
						// 如果 targetType 仍然为 null, 则从 FactoryMethod 取
						if (targetType == null) {
							targetType = getReturnTypeForFactoryMethod(dbd, descriptor);
						}
					}
				}
			}
		}
		// 如果 targetType 仍然为 null, 则从 beanFactory 取 beanName 对应的类型
		if (targetType == null) {
			if (this.beanFactory != null) {
				Class<?> beanType = this.beanFactory.getType(bdHolder.getBeanName());
				if (beanType != null) {
					targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanType));
				}
			}
			// Fallback: no BeanFactory set, or no type resolvable through it
			// -> best-effort match against the target class if applicable.
			if (targetType == null && rbd != null && rbd.hasBeanClass() && rbd.getFactoryMethodName() == null) {
				Class<?> beanClass = rbd.getBeanClass();
				if (!FactoryBean.class.isAssignableFrom(beanClass)) {
					targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanClass));
				}
			}
		}
		// 取不到被依赖方类型, 则返回 true
		if (targetType == null) {
			return true;
		}
		// 缓存被依赖方类型
		if (cacheType) {
			rbd.targetType = targetType;
		}
		if (descriptor.fallbackMatchAllowed() &&
				(targetType.hasUnresolvableGenerics() || targetType.resolve() == Properties.class)) {
			// Fallback matches allow unresolvable generics, e.g. plain HashMap to Map<String,String>;
			// and pragmatically also java.util.Properties to any Map (since despite formally being a
			// Map<Object,Object>, java.util.Properties is usually perceived as a Map<String,String>).
			return true;
		}
		// 判断"被依赖方"是否和"依赖方"的泛型类型匹配
		return dependencyType.isAssignableFrom(targetType);
	}

	@Nullable
	protected RootBeanDefinition getResolvedDecoratedDefinition(RootBeanDefinition rbd) {
		BeanDefinitionHolder decDef = rbd.getDecoratedDefinition();
		if (decDef != null && this.beanFactory instanceof ConfigurableListableBeanFactory) {
			ConfigurableListableBeanFactory clbf = (ConfigurableListableBeanFactory) this.beanFactory;
			if (clbf.containsBeanDefinition(decDef.getBeanName())) {
				BeanDefinition dbd = clbf.getMergedBeanDefinition(decDef.getBeanName());
				if (dbd instanceof RootBeanDefinition) {
					return (RootBeanDefinition) dbd;
				}
			}
		}
		return null;
	}

	// 根据 BeanDefinition 取 bean 的类型
	@Nullable
	protected ResolvableType getReturnTypeForFactoryMethod(RootBeanDefinition rbd, DependencyDescriptor descriptor) {
		// 取 factoryMethodReturnType
		ResolvableType returnType = rbd.factoryMethodReturnType;
		// 如果 factoryMethodReturnType 为 null, 则取 factoryMethod
		if (returnType == null) {
			Method factoryMethod = rbd.getResolvedFactoryMethod();
			// 取 factoryMethod 的返回类型
			if (factoryMethod != null) {
				returnType = ResolvableType.forMethodReturnType(factoryMethod);
			}
		}
		// 判断 returnType 是否和"依赖方类型"匹配
		if (returnType != null) {
			Class<?> resolvedClass = returnType.resolve();
			if (resolvedClass != null && descriptor.getDependencyType().isAssignableFrom(resolvedClass)) {
				// Only use factory method metadata if the return type is actually expressive enough
				// for our dependency. Otherwise, the returned instance type may have matched instead
				// in case of a singleton instance having been registered with the container already.
				return returnType;
			}
		}
		return null;
	}

}
