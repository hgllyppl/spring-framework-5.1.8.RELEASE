/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ConfigurationClassEnhancer.EnhancedConfiguration;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR;
import static org.springframework.context.annotation.ConfigurationClassUtils.checkConfigurationClassCandidate;
import static org.springframework.context.annotation.ConfigurationClassUtils.isFullConfigurationClass;
import static org.springframework.context.annotation.ConfigurationClassUtils.isLiteConfigurationClass;

/**
 * {@link BeanFactoryPostProcessor} used for bootstrapping processing of
 * {@link Configuration @Configuration} classes.
 *
 * <p>Registered by default when using {@code <context:annotation-config/>} or
 * {@code <context:component-scan/>}. Otherwise, may be declared manually as
 * with any other BeanFactoryPostProcessor.
 *
 * <p>This post processor is priority-ordered as it is important that any
 * {@link Bean} methods declared in {@code @Configuration} classes have
 * their corresponding bean definitions registered before any other
 * {@link BeanFactoryPostProcessor} executes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 */
public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor,
		PriorityOrdered, ResourceLoaderAware, BeanClassLoaderAware, EnvironmentAware {

	private static final String IMPORT_REGISTRY_BEAN_NAME = ConfigurationClassPostProcessor.class.getName() + ".importRegistry";

	private final Log logger = LogFactory.getLog(getClass());

	private SourceExtractor sourceExtractor = new PassThroughSourceExtractor();
	// 错误报告器
	private ProblemReporter problemReporter = new FailFastProblemReporter();
	// environment
	@Nullable
	private Environment environment;
	// resourceLoader
	private ResourceLoader resourceLoader = new DefaultResourceLoader();
	// beanClassLoader
	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();
	// metadataReaderFactory
	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();
	// 设置 metadataReaderFactory flag
	private boolean setMetadataReaderFactoryCalled = false;
	// 已调用 postProcessBeanDefinitionRegistry 的 beanFactoryId
	private final Set<Integer> registriesPostProcessed = new HashSet<>();
	// 已调用 postProcessBeanFactory 的 beanFactoryId
	private final Set<Integer> factoriesPostProcessed = new HashSet<>();
	// Bean Definition Reader
	@Nullable
	private ConfigurationClassBeanDefinitionReader reader;
	// 设置 BeanNameGenerator flag
	private boolean localBeanNameGeneratorSet = false;
	// 为 bean 使用短名称
	private BeanNameGenerator componentScanBeanNameGenerator = new AnnotationBeanNameGenerator();
	// 为 bean 使用全名称
	private BeanNameGenerator importBeanNameGenerator = new AnnotationBeanNameGenerator() {
		@Override
		protected String buildDefaultBeanName(BeanDefinition definition) {
			String beanClassName = definition.getBeanClassName();
			Assert.state(beanClassName != null, "No bean class name set");
			return beanClassName;
		}
	};

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;  // within PriorityOrdered
	}

	/**
	 * Set the {@link SourceExtractor} to use for generated bean definitions
	 * that correspond to {@link Bean} factory methods.
	 */
	public void setSourceExtractor(@Nullable SourceExtractor sourceExtractor) {
		this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new PassThroughSourceExtractor());
	}

	/**
	 * Set the {@link ProblemReporter} to use.
	 * <p>Used to register any problems detected with {@link Configuration} or {@link Bean}
	 * declarations. For instance, an @Bean method marked as {@code final} is illegal
	 * and would be reported as a problem. Defaults to {@link FailFastProblemReporter}.
	 */
	public void setProblemReporter(@Nullable ProblemReporter problemReporter) {
		this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
	}

	/**
	 * Set the {@link MetadataReaderFactory} to use.
	 * <p>Default is a {@link CachingMetadataReaderFactory} for the specified
	 * {@linkplain #setBeanClassLoader bean class loader}.
	 */
	public void setMetadataReaderFactory(MetadataReaderFactory metadataReaderFactory) {
		Assert.notNull(metadataReaderFactory, "MetadataReaderFactory must not be null");
		this.metadataReaderFactory = metadataReaderFactory;
		this.setMetadataReaderFactoryCalled = true;
	}

	/**
	 * Set the {@link BeanNameGenerator} to be used when triggering component scanning
	 * from {@link Configuration} classes and when registering {@link Import}'ed
	 * configuration classes. The default is a standard {@link AnnotationBeanNameGenerator}
	 * for scanned components (compatible with the default in {@link ClassPathBeanDefinitionScanner})
	 * and a variant thereof for imported configuration classes (using unique fully-qualified
	 * class names instead of standard component overriding).
	 * <p>Note that this strategy does <em>not</em> apply to {@link Bean} methods.
	 * <p>This setter is typically only appropriate when configuring the post-processor as
	 * a standalone bean definition in XML, e.g. not using the dedicated
	 * {@code AnnotationConfig*} application contexts or the {@code
	 * <context:annotation-config>} element. Any bean name generator specified against
	 * the application context will take precedence over any value set here.
	 * @since 3.1.1
	 * @see AnnotationConfigApplicationContext#setBeanNameGenerator(BeanNameGenerator)
	 * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		Assert.notNull(beanNameGenerator, "BeanNameGenerator must not be null");
		this.localBeanNameGeneratorSet = true;
		this.componentScanBeanNameGenerator = beanNameGenerator;
		this.importBeanNameGenerator = beanNameGenerator;
	}

	@Override
	public void setEnvironment(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
		if (!this.setMetadataReaderFactoryCalled) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
		if (!this.setMetadataReaderFactoryCalled) {
			this.metadataReaderFactory = new CachingMetadataReaderFactory(beanClassLoader);
		}
	}

	// 读取所有的 BeanDefinition
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
		int registryId = System.identityHashCode(registry);
		// 如果是已处理过的 beanFactory, 则抛出异常
		if (this.registriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException("postProcessBeanDefinitionRegistry already called on this post-processor against " + registry);
		}
		if (this.factoriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException("postProcessBeanFactory already called on this post-processor against " + registry);
		}
		// 将 registryId 加入 registriesPostProcessed
		this.registriesPostProcessed.add(registryId);
		// 读取所有的 BeanDefinition
		processConfigBeanDefinitions(registry);
	}

	// 增强带 @Configuration 的类
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		int factoryId = System.identityHashCode(beanFactory);
		// 如果是已处理过的 beanFactory, 则抛出异常
		if (this.factoriesPostProcessed.contains(factoryId)) {
			throw new IllegalStateException("postProcessBeanFactory already called on this post-processor against " + beanFactory);
		}
		this.factoriesPostProcessed.add(factoryId);
		// 如果还没有读取 BeanDefinition, 则读取之
		if (!this.registriesPostProcessed.contains(factoryId)) {
			processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
		}
		// 增强带 @Configuration 的类
		enhanceConfigurationClasses(beanFactory);
		// 添加 ImportAwareBeanPostProcessor
		beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
	}

	/**
	 * 基于通过以下两个方法验证的类来构建 beanFactory
	 * 即是带 Configuration/Component/ComponentScan/Import/ImportResource 等注解的类
	 * @see ConfigurationClassUtils#isFullConfigurationCandidate
	 * @see ConfigurationClassUtils#isLiteConfigurationCandidate
 	 */
	public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
		List<BeanDefinitionHolder> originalConfigs = new ArrayList<>();
		String[] currentBeanNames = registry.getBeanDefinitionNames();
		// 查找带 Configuration/Component/ComponentScan/Import/ImportResource 等注解的类
		for (String beanName : currentBeanNames) {
			BeanDefinition beanDef = registry.getBeanDefinition(beanName);
			if (isFullConfigurationClass(beanDef) || isLiteConfigurationClass(beanDef)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Bean definition has already been processed as a configuration class: " + beanDef);
				}
			}
			else if (checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
				originalConfigs.add(new BeanDefinitionHolder(beanDef, beanName));
			}
		}
		// 如果没有可配置的类, 则返回
		if (originalConfigs.isEmpty()) {
			return;
		}
		// 排序配置类
		originalConfigs.sort((bd1, bd2) -> {
			int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());
			int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());
			return Integer.compare(i1, i2);
		});
		// 如果没有设置 BeanNameGenerator 且检测到 BeanNameGenerator, 则设置之
		SingletonBeanRegistry sbr = null;
		if (registry instanceof SingletonBeanRegistry) {
			sbr = (SingletonBeanRegistry) registry;
			if (!this.localBeanNameGeneratorSet) {
				BeanNameGenerator generator = (BeanNameGenerator) sbr.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
				if (generator != null) {
					this.componentScanBeanNameGenerator = generator;
					this.importBeanNameGenerator = generator;
				}
			}
		}
		// environment
		if (this.environment == null) {
			this.environment = new StandardEnvironment();
		}
		// 准备解析配置类
		ConfigurationClassParser parser = new ConfigurationClassParser(
				this.metadataReaderFactory, this.problemReporter, this.environment,
				this.resourceLoader, this.componentScanBeanNameGenerator, registry);
		Set<BeanDefinitionHolder> bootstrapConfigs = new LinkedHashSet<>(originalConfigs);
		Set<ConfigurationClass> alreadyParsedConfigs = new HashSet<>(originalConfigs.size());
		do {
			// 解析并验证配置类
			parser.parse(bootstrapConfigs);
			parser.validate();
			// 准备读取 BeanDefinition
			Set<ConfigurationClass> currentConfigs = new LinkedHashSet<>(parser.getConfigurationClasses());
			currentConfigs.removeAll(alreadyParsedConfigs);
			if (this.reader == null) {
				this.reader = new ConfigurationClassBeanDefinitionReader(
						registry, this.sourceExtractor, this.resourceLoader, this.environment,
						this.importBeanNameGenerator, parser.getImportRegistry());
			}
			// 读取 BeanDefinition
			this.reader.loadBeanDefinitions(currentConfigs);
			// 记录已解析的配置类
			alreadyParsedConfigs.addAll(currentConfigs);
			bootstrapConfigs.clear();
			// 如果有新的 BeanDefinition
			if (registry.getBeanDefinitionCount() > currentBeanNames.length) {
				String[] newBeanNames = registry.getBeanDefinitionNames();
				Set<String> oldBeanNames = new HashSet<>(Arrays.asList(currentBeanNames));
				Set<String> alreadyParsedConfigNames = alreadyParsedConfigs
						.stream()
						.map(configurationClass -> configurationClass.getMetadata().getClassName())
						.collect(toSet());
				// 从新的 BeanDefinition 查找没有解析过的配置类
				Arrays.stream(newBeanNames)
						.filter(beanName -> !oldBeanNames.contains(beanName))
						.map(beanName -> new BeanDefinitionHolder(registry.getBeanDefinition(beanName), beanName))
						.filter(bdh -> checkConfigurationClassCandidate(bdh.getBeanDefinition(), metadataReaderFactory))
						.filter(bdh -> !alreadyParsedConfigNames.contains(bdh.getBeanDefinition().getBeanClassName()))
						.forEach(bootstrapConfigs::add);
				// 从新赋值 currentBeanNames
				currentBeanNames = newBeanNames;
			}
		}
		while (!bootstrapConfigs.isEmpty());

		// 注册 ImportStack, 用于 ImportAwareBeanPostProcessor
		if (sbr != null && !sbr.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
			sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
		}
		// 清除缓存
		if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
			((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
		}
	}

	/**
	 * 增强配置类
	 * @see ConfigurationClassEnhancer
	 */
	public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
		Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<>();
		// 遍历 BeanDefinitionNames
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
			// 如果是配置类
			if (isFullConfigurationClass(beanDef)) {
				// 如果非 AbstractBeanDefinition, 将抛出异常
				if (!(beanDef instanceof AbstractBeanDefinition)) {
					throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" +
							beanName + "' since it is not stored in an AbstractBeanDefinition subclass");
				}
				else if (logger.isInfoEnabled() && beanFactory.containsSingleton(beanName)) {
					logger.info("Cannot enhance @Configuration bean definition '" + beanName +
							"' since its singleton instance has been created too early. The typical cause " +
							"is a non-static @Bean method with a BeanDefinitionRegistryPostProcessor " +
							"return type: Consider declaring such methods as 'static'.");
				}
				// 加入结果集
				configBeanDefs.put(beanName, (AbstractBeanDefinition) beanDef);
			}
		}
		// 如果 configBeanDefs 为空, 则返回
		if (configBeanDefs.isEmpty()) {
			return;
		}
		// 遍历 configBeanDefs 并增强
		ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
		for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
			AbstractBeanDefinition beanDef = entry.getValue();
			// 设置代理属性
			beanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
			// 增强 class
			try {
				Class<?> configClass = beanDef.resolveBeanClass(this.beanClassLoader);
				if (configClass != null) {
					// TODO 代理行为
					Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
					if (configClass != enhancedClass) {
						if (logger.isTraceEnabled()) {
							logger.trace(String.format("Replacing bean definition '%s' existing class '%s' with " +
									"enhanced class '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));
						}
						// 设置成增强后的 class
						beanDef.setBeanClass(enhancedClass);
					}
				}
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
			}
		}
	}

	// 为 @Configuration 类导入 beanFactory 和注解信息
	private static class ImportAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter {

		private final BeanFactory beanFactory;

		public ImportAwareBeanPostProcessor(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		// 为 @Configuration 类导入 beanFactory
		@Override
		public PropertyValues postProcessProperties(@Nullable PropertyValues pvs, Object bean, String beanName) {
			if (bean instanceof EnhancedConfiguration) {
				((EnhancedConfiguration) bean).setBeanFactory(this.beanFactory);
			}
			return pvs;
		}

		/**
		 * 如果 (bean 实现了 ImportAware) 且 (是一个 Configuration 类), 则注入注解信息
         */
		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			if (bean instanceof ImportAware) {
				ImportRegistry ir = this.beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
				// 查找并注入注解信息
				AnnotationMetadata importingClass = ir.getImportingClassFor(bean.getClass().getSuperclass().getName());
				if (importingClass != null) {
					((ImportAware) bean).setImportMetadata(importingClass);
				}
			}
			return bean;
		}
	}
}
