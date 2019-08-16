/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper for retrieving @AspectJ beans from a BeanFactory and building
 * Spring Advisors based on them, for use with auto-proxying.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see AnnotationAwareAspectJAutoProxyCreator
 */
public class BeanFactoryAspectJAdvisorsBuilder {

	private final ListableBeanFactory beanFactory;
	// advisorFactory
	private final AspectJAdvisorFactory advisorFactory;
	// 缓存的 aspectBeanNames
	@Nullable
	private volatile List<String> aspectBeanNames;
	// 缓存的 Advisor
	// beanName -> advisors
	private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();
	// 缓存的 AspectInstanceFactory
	// beanName -> AspectInstanceFactory
	private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();

	// 创建 BeanFactoryAspectJAdvisorsBuilder, 用于扫描所有带 @Aspect 的bean, 并生成所有的 advisor
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory) {
		this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
	}

	// 创建 BeanFactoryAspectJAdvisorsBuilder, 用于扫描所有带 @Aspect 的bean, 并生成所有的 advisor
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		Assert.notNull(advisorFactory, "AspectJAdvisorFactory must not be null");
		this.beanFactory = beanFactory;
		this.advisorFactory = advisorFactory;
	}

	/**
	 * 查找带 @Aspect 的 bean, 并构建 springAOP Advisors
	 */
	public List<Advisor> buildAspectJAdvisors() {
		// DCL for aspectBeanNames
		// 如果 aspectBeanNames 为 null
		List<String> aspectNames = this.aspectBeanNames;
		if (aspectNames == null) {
			synchronized (this) {
				aspectNames = this.aspectBeanNames;
				if (aspectNames == null) {
					List<Advisor> advisors = new ArrayList<>();
					aspectNames = new ArrayList<>();
					// 读取所有 beanNames
					String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory, Object.class, true, false);
					// 迭代 beanNames
					for (String beanName : beanNames) {
						// 是否合格的 aspect
						if (!isEligibleBean(beanName)) {
							continue;
						}
						// 获取 beanType
						Class<?> beanType = this.beanFactory.getType(beanName);
						if (beanType == null) {
							continue;
						}
						// 如果 bean 是一个 aspect
						if (this.advisorFactory.isAspect(beanType)) {
							// 构建 AspectMetadata
							aspectNames.add(beanName);
							AspectMetadata amd = new AspectMetadata(beanType, beanName);
							// 如果是 PerClauseKind.SINGLETON, 则构建 advisor 并将其加入 advisorsCache
							if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
								// 构建 AspectInstanceFactory 并获取 advisors
								MetadataAwareAspectInstanceFactory factory = new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
								List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
								// 加入 advisorsCache or aspectFactoryCache
								if (this.beanFactory.isSingleton(beanName)) {
									this.advisorsCache.put(beanName, classAdvisors);
								}
								else {
									this.aspectFactoryCache.put(beanName, factory);
								}
								advisors.addAll(classAdvisors);
							}
							// 反之, 则将 AspectInstanceFactory 加入 aspectFactoryCache
							else {
								if (this.beanFactory.isSingleton(beanName)) {
									throw new IllegalArgumentException("Bean with name '" + beanName +
											"' is a singleton, but aspect instantiation model is not singleton");
								}
								// 构建 AspectInstanceFactory 并获取 advisors
								MetadataAwareAspectInstanceFactory factory = new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
								advisors.addAll(this.advisorFactory.getAdvisors(factory));
								this.aspectFactoryCache.put(beanName, factory);
							}
						}
					}
					this.aspectBeanNames = aspectNames;
					return advisors;
				}
			}
		}
		// 如果没有 aspect, 则返回空列表
		if (aspectNames.isEmpty()) {
			return Collections.emptyList();
		}
		// 迭代 aspectNames
		List<Advisor> advisors = new ArrayList<>();
		for (String aspectName : aspectNames) {
			// 从 advisorsCache 取 advisors 并加入结果集
			List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
			if (cachedAdvisors != null) {
				advisors.addAll(cachedAdvisors);
			}
			// 使用 AspectInstanceFactory 构建 advisors, 并加入结果集
			else {
				MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
				advisors.addAll(this.advisorFactory.getAdvisors(factory));
			}
		}
		return advisors;
	}

	/**
	 * 检查给定的 bean 是否是一个合格的 aspect
	 */
	protected boolean isEligibleBean(String beanName) {
		return true;
	}

}
