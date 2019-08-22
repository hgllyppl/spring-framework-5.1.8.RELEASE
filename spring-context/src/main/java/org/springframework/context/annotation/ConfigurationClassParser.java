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
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.annotation.DeferredImportSelector.Group;
import org.springframework.core.NestedIOException;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.springframework.beans.BeanUtils.instantiateClass;
import static org.springframework.context.annotation.AnnotationConfigUtils.attributesFor;
import static org.springframework.context.annotation.AnnotationConfigUtils.attributesForRepeatable;
import static org.springframework.context.annotation.ConfigurationClassUtils.isConfigurationCandidate;
import static org.springframework.context.annotation.ParserStrategyUtils.invokeAwareMethods;

/**
 * Parses a {@link Configuration} class definition, populating a collection of
 * {@link ConfigurationClass} objects (parsing a single Configuration class may result in
 * any number of ConfigurationClass objects because one Configuration class may import
 * another using the {@link Import} annotation).
 *
 * <p>This class helps separate the concern of parsing the structure of a Configuration
 * class from the concern of registering BeanDefinition objects based on the content of
 * that model (with the exception of {@code @ComponentScan} annotations which need to be
 * registered immediately).
 *
 * <p>This ASM-based implementation avoids reflection and eager class loading in order to
 * interoperate effectively with lazy class loading in a Spring ApplicationContext.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 3.0
 * @see ConfigurationClassBeanDefinitionReader
 */
class ConfigurationClassParser {

	private static final PropertySourceFactory DEFAULT_PROPERTY_SOURCE_FACTORY = new DefaultPropertySourceFactory();

	private static final Comparator<DeferredImportSelectorHolder> DEFERRED_IMPORT_COMPARATOR =
			(o1, o2) -> AnnotationAwareOrderComparator.INSTANCE.compare(o1.getImportSelector(), o2.getImportSelector());


	private final Log logger = LogFactory.getLog(getClass());

	private final MetadataReaderFactory metadataReaderFactory;

	private final ProblemReporter problemReporter;

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	private final BeanDefinitionRegistry registry;
	// 解析 ComponentScan 注解
	private final ComponentScanAnnotationParser componentScanParser;
	// 条件判断器
	private final ConditionEvaluator conditionEvaluator;
	// 已解析的配置类
	private final Map<ConfigurationClass, ConfigurationClass> configurationClasses = new LinkedHashMap<>();
	// 已解析的超类
	private final Map<String, ConfigurationClass> knownSuperclasses = new HashMap<>();
	// 已解析的 PropertySource
	private final List<String> propertySourceNames = new ArrayList<>();
	// 已导入的类信息
	private final ImportStack importStack = new ImportStack();
	// 延迟导入的 DeferredImportSelector
	private final DeferredImportSelectorHandler deferredImportSelectorHandler = new DeferredImportSelectorHandler();

	public ConfigurationClassParser(MetadataReaderFactory metadataReaderFactory,
			ProblemReporter problemReporter, Environment environment, ResourceLoader resourceLoader,
			BeanNameGenerator componentScanBeanNameGenerator, BeanDefinitionRegistry registry) {

		this.metadataReaderFactory = metadataReaderFactory;
		this.problemReporter = problemReporter;
		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.registry = registry;
		this.componentScanParser = new ComponentScanAnnotationParser(environment, resourceLoader, componentScanBeanNameGenerator, registry);
		this.conditionEvaluator = new ConditionEvaluator(registry, environment, resourceLoader);
	}

	// 解析配置类
	public void parse(Set<BeanDefinitionHolder> configCandidates) {
		for (BeanDefinitionHolder holder : configCandidates) {
			BeanDefinition bd = holder.getBeanDefinition();
			try {
				if (bd instanceof AnnotatedBeanDefinition) {
					parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());
				}
				else if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
					parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
				}
				else {
					parse(bd.getBeanClassName(), holder.getBeanName());
				}
			}
			catch (BeanDefinitionStoreException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new BeanDefinitionStoreException(
						"Failed to parse configuration class [" + bd.getBeanClassName() + "]", ex);
			}
		}
		// 导入"延迟导入"的类
		this.deferredImportSelectorHandler.process();
	}

	// 解析配置类
	protected final void parse(@Nullable String className, String beanName) throws IOException {
		Assert.notNull(className, "No bean class name for configuration class bean definition");
		MetadataReader reader = this.metadataReaderFactory.getMetadataReader(className);
		processConfigurationClass(new ConfigurationClass(reader, beanName));
	}

	// 解析配置类
	protected final void parse(Class<?> clazz, String beanName) throws IOException {
		processConfigurationClass(new ConfigurationClass(clazz, beanName));
	}

	// 解析配置类
	protected final void parse(AnnotationMetadata metadata, String beanName) throws IOException {
		processConfigurationClass(new ConfigurationClass(metadata, beanName));
	}

	/**
	 * 验证配置类
	 * 配置类如果带有 @Configuration, 则不能是 final 类型的 class
	 * 配置类上如果有 beanMethod, 则此 beanMethod 必须是可重载的
	 * @see MethodMetadataReadingVisitor#isOverridable
	 */
	public void validate() {
		for (ConfigurationClass configClass : this.configurationClasses.keySet()) {
			configClass.validate(this.problemReporter);
		}
	}

	public Set<ConfigurationClass> getConfigurationClasses() {
		return this.configurationClasses.keySet();
	}

	// 解析配置类
	protected void processConfigurationClass(ConfigurationClass configClass) throws IOException {
		// 是否跳过当前配置类
		if (this.conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
			return;
		}
		// 如果已存在和当前相同的配置类
		ConfigurationClass existingClass = this.configurationClasses.get(configClass);
		if (existingClass != null) {
			// 如果当前配置类是被导入的
			if (configClass.isImported()) {
				// 如果已存在的类也是被导入的, 则合并导入属性
				if (existingClass.isImported()) {
					existingClass.mergeImportedBy(configClass);
				}
				// 反之, 则忽略当前配置类
				return;
			}
			// 反之, 从 configurationClasses、knownSuperclasses 移除已解析的配置类
			else {
				this.configurationClasses.remove(configClass);
				this.knownSuperclasses.values().removeIf(configClass::equals);
			}
		}
		// 递归的处理配置类和它的超类
		SourceClass sourceClass = asSourceClass(configClass);
		do {
			sourceClass = doProcessConfigurationClass(configClass, sourceClass);
		}
		while (sourceClass != null);
		// 将配置类加入 configurationClasses
		this.configurationClasses.put(configClass, configClass);
	}

	/**
	 * 真∙解析配置类, 处理过程分为8个步骤, 每个步骤可递归的调用本方法, 主要出口是步骤6
	 * @param configClass 此参数用于存储当前正在解析的class的 beanMethods、importedResources、importBeanDefinitionRegistrars
	 * @param sourceClass 此参数用于获取当前正在解析的class的信息
	 */
	@Nullable
	protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass)
			throws IOException {
		// 1、处理 @Component
		// 如果类上有 @Component, 则可能需要解析内部类
		if (configClass.getMetadata().isAnnotated(Component.class.getName())) {
			processMemberClasses(configClass, sourceClass);
		}
		// 2、处理 @PropertySource
		// 读取 PropertySource, 并将其加入 environment
		Set<AnnotationAttributes> propertySources = attributesForRepeatable(sourceClass.getMetadata(), PropertySources.class, org.springframework.context.annotation.PropertySource.class);
		// 遍历 PropertySource 并加入 environment
		for (AnnotationAttributes propertySource : propertySources) {
			if (this.environment instanceof ConfigurableEnvironment) {
				processPropertySource(propertySource);
			}
			else {
				logger.info("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() + "]. Reason: Environment must implement ConfigurableEnvironment");
			}
		}
		// 3、处理 @ComponentScans/@ComponentScan
		// 扫描指定的目录或当前配置类所在的目录并查找新的配置类并注册之
		Set<AnnotationAttributes> componentScans = attributesForRepeatable(sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
		if (!componentScans.isEmpty() && !this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
			// 遍历 componentScans
			for (AnnotationAttributes componentScan : componentScans) {
				// 扫描指定的目录或当前配置类所在的目录并查找新的配置类并注册之
				Set<BeanDefinitionHolder> scannedBeanDefinitions = this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
				// 遍历新的配置类并完成递归调用
				for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
					BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
					bdCand = (bdCand != null) ? bdCand : holder.getBeanDefinition();
					// 递归调用
					if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
						parse(bdCand.getBeanClassName(), holder.getBeanName());
					}
				}
			}
		}
		// 4、处理 @Import
		// 读取并导入新的类
		Set<SourceClass> importSourceClasses = getImports(sourceClass);
		processImports(configClass, sourceClass, importSourceClasses, true);
		// 5、处理 @ImportResource
		// 通过 resource 导入新的类
		AnnotationAttributes importResource = attributesFor(sourceClass.getMetadata(), ImportResource.class);
		if (importResource != null) {
			// 读取资源位置和处理器
			String[] resources = importResource.getStringArray("locations");
			Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
			// 遍历资源并将其加入 importedResources 并延迟处理
			for (String resource : resources) {
				String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
				configClass.addImportedResource(resolvedResource, readerClass);
			}
		}
		// 6、处理 @Bean
		// 读取所有带 @Bean 的 method, 并将其加入 configClass
		Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);
		beanMethods.forEach(methodMetadata -> configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass)));
		// 7、处理接口
		// 读取所有带 @Bean 的 method, 并将其加入 configClass
		processInterfaces(configClass, sourceClass);
		// 8、处理超类
		// 如果当前配置类有超类, 则将其返回并递归处理
		if (sourceClass.getMetadata().hasSuperClass()) {
			String superclass = sourceClass.getMetadata().getSuperClassName();
			// 超类非 jdk 类 且 没有被处理过
			if (superclass != null && !superclass.startsWith("java") && !this.knownSuperclasses.containsKey(superclass)) {
				this.knownSuperclasses.put(superclass, configClass);
				return sourceClass.getSuperClass();
			}
		}
		return null;
	}

	/**
	 * 解析内部类
	 */
	private void processMemberClasses(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
		// 读取内部类
		Collection<SourceClass> memberClasses = sourceClass.getMemberClasses();
		if (!memberClasses.isEmpty()) {
			List<SourceClass> candidates = new ArrayList<>(memberClasses.size());
			// 遍历内部类
			for (SourceClass memberClass : memberClasses) {
				// 如果内部类是配置类且不是当前正在处理的配置类, 则将加入结果集
				if (isConfigurationCandidate(memberClass.getMetadata()) &&
						!memberClass.getMetadata().getClassName().equals(configClass.getMetadata().getClassName())) {
					candidates.add(memberClass);
				}
			}
			// 排序结果集
			OrderComparator.sort(candidates);
			// 遍历结果集
			for (SourceClass candidate : candidates) {
				// 如果已经在处理 candidate, 则抛出异常
				if (this.importStack.contains(configClass)) {
					this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
				}
				// 反之, 则处理 candidate
				else {
					this.importStack.push(configClass);
					try {
						processConfigurationClass(candidate.asConfigClass(configClass));
					}
					finally {
						this.importStack.pop();
					}
				}
			}
		}
	}

	/**
	 * 遍历接口上所有带 @bean 的 default method
	 */
	private void processInterfaces(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
		// 遍历接口
		for (SourceClass ifc : sourceClass.getInterfaces()) {
			Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(ifc);
			// 遍历 method
			for (MethodMetadata methodMetadata : beanMethods) {
				// 如果是 default method, 则将其加入配置类
				if (!methodMetadata.isAbstract()) {
					configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
				}
			}
			// 递归当前接口
			processInterfaces(configClass, ifc);
		}
	}

	/**
	 * 读取带 @bean 的 method
	 */
	private Set<MethodMetadata> retrieveBeanMethodMetadata(SourceClass sourceClass) {
		// 读取 beanMethods
		AnnotationMetadata original = sourceClass.getMetadata();
		Set<MethodMetadata> beanMethods = original.getAnnotatedMethods(Bean.class.getName());
		// 如果通过 StandardAnnotationMetadata 读取的 beanMethods 数量大于1, 则需要对 beanMethods 排序
		// 因为通过 jvm reflect 读取的 methods 顺序是不固定
		// 所以通过 asm 读取带顺序的 methods
		if (beanMethods.size() > 1 && original instanceof StandardAnnotationMetadata) {
			try {
				// 通过 asm 读取 beanMethods
				AnnotationMetadata asm = this.metadataReaderFactory.getMetadataReader(original.getClassName()).getAnnotationMetadata();
				Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Bean.class.getName());
				if (asmMethods.size() >= beanMethods.size()) {
					// 遍历 asmMethods
					Set<MethodMetadata> selectedMethods = new LinkedHashSet<>(asmMethods.size());
					for (MethodMetadata asmMethod : asmMethods) {
						// 遍历 beanMethods
						for (MethodMetadata beanMethod : beanMethods) {
							// 如果 asm 读取的 method 和 反射读取的 method 匹配, 则加入结果集
							if (beanMethod.getMethodName().equals(asmMethod.getMethodName())) {
								selectedMethods.add(beanMethod);
								break;
							}
						}
					}
					// 如果顺序结果集和反射结果集数量相同, 则返回顺序结果集
					if (selectedMethods.size() == beanMethods.size()) {
						beanMethods = selectedMethods;
					}
				}
			}
			catch (IOException ex) {
				logger.debug("Failed to read class file via ASM for determining @Bean method order", ex);
				// No worries, let's continue with the reflection metadata we started with...
			}
		}
		// 返回
		return beanMethods;
	}

	/**
	 * 将 PropertySource 加入 environment
	 */
	private void processPropertySource(AnnotationAttributes attributes) throws IOException {
		// 读取注解信息
		String name = attributes.getString("name");
		if (!StringUtils.hasLength(name)) {
			name = null;
		}
		String encoding = attributes.getString("encoding");
		if (!StringUtils.hasLength(encoding)) {
			encoding = null;
		}
		String[] locations = attributes.getStringArray("value");
		// locations 不能为空
		Assert.isTrue(locations.length > 0, "At least one @PropertySource(value) location is required");
		boolean ignoreResourceNotFound = attributes.getBoolean("ignoreResourceNotFound");
		// 决定 PropertySourceFactory 类型
		Class<? extends PropertySourceFactory> factoryClass = attributes.getClass("factory");
		PropertySourceFactory factory = (factoryClass == PropertySourceFactory.class ? DEFAULT_PROPERTY_SOURCE_FACTORY : instantiateClass(factoryClass));
		// 遍历 locations
		for (String location : locations) {
			try {
				// 动态替换 location
				String resolvedLocation = this.environment.resolveRequiredPlaceholders(location);
				// 加载 location 所在的资源
				Resource resource = this.resourceLoader.getResource(resolvedLocation);
				// 将资源转成 propertySource
				PropertySource<?> propertySource = factory.createPropertySource(name, new EncodedResource(resource, encoding));
				// 将 propertySource 加入 environment
				addPropertySource(propertySource);
			}
			catch (IllegalArgumentException | FileNotFoundException | UnknownHostException ex) {
				if (ignoreResourceNotFound) {
					if (logger.isInfoEnabled()) {
						logger.info("Properties location [" + location + "] not resolvable: " + ex.getMessage());
					}
				}
				else {
					throw ex;
				}
			}
		}
	}

	// 将 propertySource 加入 environment
	private void addPropertySource(PropertySource<?> propertySource) {
		String name = propertySource.getName();
		MutablePropertySources propertySources = ((ConfigurableEnvironment) this.environment).getPropertySources();
		// 如果已存在 propertySource
		if (this.propertySourceNames.contains(name)) {
			// 读取已存在的 propertySource
			PropertySource<?> existing = propertySources.get(name);
			if (existing != null) {
				PropertySource<?> newSource = (propertySource instanceof ResourcePropertySource ?
						((ResourcePropertySource) propertySource).withResourceName() : propertySource);
				// 如果已存在的 propertySource 是 CompositePropertySource, 则直接合并新的 propertySource
				if (existing instanceof CompositePropertySource) {
					((CompositePropertySource) existing).addFirstPropertySource(newSource);
				}
				// 反之, 则合并两个 propertySource 并替换旧的 propertySource
				else {
					if (existing instanceof ResourcePropertySource) {
						existing = ((ResourcePropertySource) existing).withResourceName();
					}
					// 将新的 propertySource 和已存在的 propertySource 合并
					CompositePropertySource composite = new CompositePropertySource(name);
					composite.addPropertySource(newSource);
					composite.addPropertySource(existing);
					// 替换已存在的 propertySource
					propertySources.replace(name, composite);
				}
				return;
			}
		}
		// 将 propertySource 加入 environment
		if (this.propertySourceNames.isEmpty()) {
			propertySources.addLast(propertySource);
		}
		else {
			String firstProcessed = this.propertySourceNames.get(this.propertySourceNames.size() - 1);
			propertySources.addBefore(firstProcessed, propertySource);
		}
		// 将 propertySource 加入 propertySourceNames
		this.propertySourceNames.add(name);
	}

	/**
	 * 读取要导入的类
	 */
	private Set<SourceClass> getImports(SourceClass sourceClass) throws IOException {
		Set<SourceClass> imports = new LinkedHashSet<>();
		Set<SourceClass> visited = new LinkedHashSet<>();
		collectImports(sourceClass, imports, visited);
		return imports;
	}

	/**
	 * 递归的读取 @import 注解值
	 * 例: {@link Configuration} -> {@link Component} -> {@link Indexed}
	 * @param sourceClass 需要查找 import 注解的类
	 * @param imports import 注解值集合
	 * @param visited 已访问过的注解
	 * @throws IOException if there is any problem reading metadata from the named class
	 */
	private void collectImports(SourceClass sourceClass, Set<SourceClass> imports, Set<SourceClass> visited)
			throws IOException {
		// 如果 sourceClass 没有被访问过
		if (visited.add(sourceClass)) {
			// 遍历注解
			Set<SourceClass> annoSourceClasses = sourceClass.getAnnotations();
			for (SourceClass annoSourceClass : annoSourceClasses) {
				String annName = annoSourceClass.getMetadata().getClassName();
				// 如果不是 import 注解, 则递归当前注解
				if (!annName.equals(Import.class.getName())) {
					collectImports(annoSourceClass, imports, visited);
				}
			}
			// 加入 import 注解值集合
			Collection<SourceClass> importSourceClasses = sourceClass.getAnnotationAttributes(Import.class.getName(), "value");
			imports.addAll(importSourceClasses);
		}
	}

	// 处理 import 注解值
	private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass,
			Collection<SourceClass> importCandidates, boolean checkForCircularImports) {
		// 如果注解值集合为空, 则返回
		if (importCandidates.isEmpty()) {
			return;
		}
		// 循环处理???
		if (checkForCircularImports && isChainedImportOnStack(configClass)) {
			this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
		}
		else {
			this.importStack.push(configClass);
			try {
				// 遍历注解值集合
				for (SourceClass candidate : importCandidates) {
					// 如果是 ImportSelector
					if (candidate.isAssignable(ImportSelector.class)) {
						// 实例化并注入 beanFactory
						Class<?> candidateClass = candidate.loadClass();
						ImportSelector selector = instantiateClass(candidateClass, ImportSelector.class);
						invokeAwareMethods(selector, this.environment, this.resourceLoader, this.registry);
						// 如果是 DeferredImportSelector, 则将其加入 deferredImportSelectorHandler 并延迟处理
						if (selector instanceof DeferredImportSelector) {
							this.deferredImportSelectorHandler.handle(configClass, (DeferredImportSelector) selector);
						}
						// 反之, 则递归地导入新类
						else {
							String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());
							Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames);
							processImports(configClass, currentSourceClass, importSourceClasses, false);
						}
					}
					// 如果是 ImportBeanDefinitionRegistrar, 则延迟处理
					else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
						// 实例化并注入 beanFactory
						Class<?> candidateClass = candidate.loadClass();
						ImportBeanDefinitionRegistrar registrar = instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class);
						invokeAwareMethods(registrar, this.environment, this.resourceLoader, this.registry);
						// 将其加入 importBeanDefinitionRegistrars
						configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());
					}
					// 以上都不是, 则开始处理新类(递归主要出口)
					else {
						/**
                         * 将新类和注解信息注册进 importStack
						 * @see ConfigurationClassPostProcessor#ImportAwareBeanPostProcessor
						 * @see ImportAware
						 */
						this.importStack.registerImport(currentSourceClass.getMetadata(), candidate.getMetadata().getClassName());
						// 处理新类
						processConfigurationClass(candidate.asConfigClass(configClass));
					}
				}
			}
			catch (BeanDefinitionStoreException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new BeanDefinitionStoreException(
						"Failed to process import candidates for configuration class [" +
						configClass.getMetadata().getClassName() + "]", ex);
			}
			finally {
				this.importStack.pop();
			}
		}
	}

	private boolean isChainedImportOnStack(ConfigurationClass configClass) {
		if (this.importStack.contains(configClass)) {
			String configClassName = configClass.getMetadata().getClassName();
			AnnotationMetadata importingClass = this.importStack.getImportingClassFor(configClassName);
			while (importingClass != null) {
				if (configClassName.equals(importingClass.getClassName())) {
					return true;
				}
				importingClass = this.importStack.getImportingClassFor(importingClass.getClassName());
			}
		}
		return false;
	}

	ImportRegistry getImportRegistry() {
		return this.importStack;
	}


	/**
	 * ConfigurationClass -> class     -> SourceClass
	 * 					  -> classname -> SourceClass
	 */
	private SourceClass asSourceClass(ConfigurationClass configurationClass) throws IOException {
		AnnotationMetadata metadata = configurationClass.getMetadata();
		// class -> SourceClass
		if (metadata instanceof StandardAnnotationMetadata) {
			return asSourceClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass());
		}
		// classname -> SourceClass
		return asSourceClass(metadata.getClassName());
	}

	/**
	 * classNames -> SourceClasses
	 */
	private Collection<SourceClass> asSourceClasses(String... classNames) throws IOException {
		List<SourceClass> annotatedClasses = new ArrayList<>(classNames.length);
		for (String className : classNames) {
			annotatedClasses.add(asSourceClass(className));
		}
		return annotatedClasses;
	}

	/**
	 * class -> SourceClass
	 * 主要通过反射读取 class
	 */
	SourceClass asSourceClass(@Nullable Class<?> classType) throws IOException {
		if (classType == null) {
			return new SourceClass(Object.class);
		}
		try {
			// 验证 class 上的注解
			for (Annotation ann : classType.getAnnotations()) {
				AnnotationUtils.validateAnnotation(ann);
			}
			// 通过反射读取 class
			return new SourceClass(classType);
		}
		catch (Throwable ex) {
			// 验证失败, 则通过 asm 读过class
			return asSourceClass(classType.getName());
		}
	}

	/**
	 * classname -> SourceClass
	 * 主要通过 asm 读过class
	 */
	SourceClass asSourceClass(@Nullable String className) throws IOException {
		if (className == null) {
			return new SourceClass(Object.class);
		}
		// 如果是 jdk 类, 则通过反射读取
		if (className.startsWith("java")) {
			try {
				return new SourceClass(ClassUtils.forName(className, this.resourceLoader.getClassLoader()));
			}
			catch (ClassNotFoundException ex) {
				throw new NestedIOException("Failed to load class [" + className + "]", ex);
			}
		}
		// 反之, 则通过 asm 读取
		return new SourceClass(this.metadataReaderFactory.getMetadataReader(className));
	}

	// 存储被导入类和导入时的注解信息
	@SuppressWarnings("serial")
	private static class ImportStack extends ArrayDeque<ConfigurationClass> implements ImportRegistry {

		private final MultiValueMap<String, AnnotationMetadata> imports = new LinkedMultiValueMap<>();

		public void registerImport(AnnotationMetadata importingClass, String importedClass) {
			this.imports.add(importedClass, importingClass);
		}

		@Override
		@Nullable
		public AnnotationMetadata getImportingClassFor(String importedClass) {
			return CollectionUtils.lastElement(this.imports.get(importedClass));
		}

		@Override
		public void removeImportingClass(String importingClass) {
			for (List<AnnotationMetadata> list : this.imports.values()) {
				for (Iterator<AnnotationMetadata> iterator = list.iterator(); iterator.hasNext();) {
					if (iterator.next().getClassName().equals(importingClass)) {
						iterator.remove();
						break;
					}
				}
			}
		}

		/**
		 * Given a stack containing (in order)
		 * <ul>
		 * <li>com.acme.Foo</li>
		 * <li>com.acme.Bar</li>
		 * <li>com.acme.Baz</li>
		 * </ul>
		 * return "[Foo->Bar->Baz]".
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder("[");
			Iterator<ConfigurationClass> iterator = iterator();
			while (iterator.hasNext()) {
				builder.append(iterator.next().getSimpleName());
				if (iterator.hasNext()) {
					builder.append("->");
				}
			}
			return builder.append(']').toString();
		}
	}

	// DeferredImportSelectorHandler
	private class DeferredImportSelectorHandler {

		@Nullable
		private List<DeferredImportSelectorHolder> selectorHolders = new ArrayList<>();

		/**
		 * 将 configClass 和 importSelector 加入集合
		 * 如果 selectorHolders 为空, 则直接导入和处理新的类
		 */
		public void handle(ConfigurationClass configClass, DeferredImportSelector importSelector) {
			DeferredImportSelectorHolder selectorHolder = new DeferredImportSelectorHolder(configClass, importSelector);
			if (this.selectorHolders == null) {
				DeferredImportSelectorGroupingHandler groupingHandler = new DeferredImportSelectorGroupingHandler();
				groupingHandler.register(selectorHolder);
				groupingHandler.processGroupImports();
			}
			else {
				this.selectorHolders.add(selectorHolder);
			}
		}

		/**
		 * 将 selectorHolders 排序并分组, 然后导入和处理新的类
		 */
		public void process() {
			List<DeferredImportSelectorHolder> tmpSelectorHolders = this.selectorHolders;
			this.selectorHolders = null;
			try {
				if (tmpSelectorHolders != null) {
					DeferredImportSelectorGroupingHandler groupingHandler = new DeferredImportSelectorGroupingHandler();
					// 排序
					tmpSelectorHolders.sort(DEFERRED_IMPORT_COMPARATOR);
					// 分组
					tmpSelectorHolders.forEach(groupingHandler::register);
					// 遍历分组并导入和处理新的类
					groupingHandler.processGroupImports();
				}
			}
			finally {
				this.selectorHolders = new ArrayList<>();
			}
		}
	}

	// DeferredImportSelectorGroupingHandler
	private class DeferredImportSelectorGroupingHandler {

		private final Map<Object, DeferredImportSelectorGrouping> groupings = new LinkedHashMap<>();

		private final Map<AnnotationMetadata, ConfigurationClass> configurationClasses = new HashMap<>();

		// 将 deferredImportHolder 分组
		public void register(DeferredImportSelectorHolder selectorHolder) {
			// 取分组key
			Class<? extends Group> groupClass = selectorHolder.getImportSelector().getImportGroup();
			Object groupingKey = (groupClass != null ? groupClass : selectorHolder);
			// 分组key -> 分组
			Function<Object, DeferredImportSelectorGrouping> groupingFun = new Function<Object, DeferredImportSelectorGrouping>() {
				@Override
				public DeferredImportSelectorGrouping apply(Object groupingKey) {
					// 创建分组
					// DeferredImportSelectorGrouping 与 Group 可以合并成一个类, 这里搞麻烦了
					Group group = createGroup(groupClass);
					return new DeferredImportSelectorGrouping(group);
				}
			};
			// 将分组加入 groupings
			DeferredImportSelectorGrouping grouping = this.groupings.computeIfAbsent(groupingKey, groupingFun);
			// 将 selectorHolder 加入分组
			grouping.add(selectorHolder);
			// 记录当前处理的配置类
			this.configurationClasses.put(selectorHolder.getConfigurationClass().getMetadata(), selectorHolder.getConfigurationClass());
		}

		/**
		 * 遍历分组并导入和处理新的类
		 * @see DeferredImportSelector#selectImports
		 * @see ConfigurationClassParser#processImports
		 */
		public void processGroupImports() {
			for (DeferredImportSelectorGrouping grouping : this.groupings.values()) {
				// 导入新的类
				Iterable<Group.Entry> imports = grouping.getImports();
				// 处理新的类
				imports.forEach(new Consumer<Group.Entry>() {
					@Override
					public void accept(Group.Entry entry) {
						ConfigurationClass configurationClass = configurationClasses.get(entry.getMetadata());
						try {
							processImports(configurationClass, asSourceClass(configurationClass), asSourceClasses(entry.getImportClassName()), false);
						}
						catch (BeanDefinitionStoreException ex) {
							throw ex;
						}
						catch (Throwable ex) {
							throw new BeanDefinitionStoreException(
									"Failed to process import candidates for configuration class [" +
											configurationClass.getMetadata().getClassName() + "]", ex);
						}
					}
				});
			}
		}

		// 创建分组
		private Group createGroup(@Nullable Class<? extends Group> type) {
			Class<? extends Group> effectiveType = (type != null ? type : DefaultDeferredImportSelectorGroup.class);
			Group group = instantiateClass(effectiveType);
			invokeAwareMethods(group,
					ConfigurationClassParser.this.environment,
					ConfigurationClassParser.this.resourceLoader,
					ConfigurationClassParser.this.registry);
			return group;
		}
	}

	private static class DeferredImportSelectorHolder {

		private final ConfigurationClass configurationClass;

		private final DeferredImportSelector importSelector;

		public DeferredImportSelectorHolder(ConfigurationClass configClass, DeferredImportSelector selector) {
			this.configurationClass = configClass;
			this.importSelector = selector;
		}

		public ConfigurationClass getConfigurationClass() {
			return this.configurationClass;
		}

		public DeferredImportSelector getImportSelector() {
			return this.importSelector;
		}
	}

	private static class DeferredImportSelectorGrouping {
		// 分组
		private final Group group;
		// selectorHolders
		private final List<DeferredImportSelectorHolder> selectorHolders = new ArrayList<>();

		DeferredImportSelectorGrouping(Group group) {
			this.group = group;
		}

		// 将 selectorHolder 加入 selectorHolders
		public void add(DeferredImportSelectorHolder selectorHolder) {
			this.selectorHolders.add(selectorHolder);
		}

		/**
		 * 遍历 selectorHolders 并导入新的类
		 */
		public Iterable<Group.Entry> getImports() {
			selectorHolders.stream().forEach(selectorHolder -> {
				AnnotationMetadata metadata = selectorHolder.getConfigurationClass().getMetadata();
				DeferredImportSelector importSelector = selectorHolder.getImportSelector();
				group.process(metadata, importSelector);
			});
			return this.group.selectImports();
		}
	}

	private static class DefaultDeferredImportSelectorGroup implements Group {

		private final List<Entry> imports = new ArrayList<>();

		// 导入新的类并加入 imports
		@Override
		public void process(AnnotationMetadata metadata, DeferredImportSelector selector) {
			String[] importClasses = selector.selectImports(metadata);
			Arrays.stream(importClasses).forEach(importClass -> imports.add(new Entry(metadata, importClass)));
		}

		// 返回 imports
		@Override
		public Iterable<Entry> selectImports() {
			return this.imports;
		}
	}


	/**
	 * 包装 class 已提供统一的接口来访问 class 信息
	 */
	private class SourceClass implements Ordered {
		// Class or MetadataReader
		private final Object source;
		// StandardAnnotationMetadata or AnnotationMetadataReadingVisitor
		private final AnnotationMetadata metadata;

		public SourceClass(Object source) {
			this.source = source;
			if (source instanceof Class) {
				this.metadata = new StandardAnnotationMetadata((Class<?>) source, true);
			}
			else {
				// AnnotationMetadataReadingVisitor
				this.metadata = ((MetadataReader) source).getAnnotationMetadata();
			}
		}

		public final AnnotationMetadata getMetadata() {
			return this.metadata;
		}

		@Override
		public int getOrder() {
			Integer order = ConfigurationClassUtils.getOrder(this.metadata);
			return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
		}

		public Class<?> loadClass() throws ClassNotFoundException {
			if (this.source instanceof Class) {
				return (Class<?>) this.source;
			}
			String className = ((MetadataReader) this.source).getClassMetadata().getClassName();
			return ClassUtils.forName(className, resourceLoader.getClassLoader());
		}

		public boolean isAssignable(Class<?> clazz) throws IOException {
			if (this.source instanceof Class) {
				return clazz.isAssignableFrom((Class<?>) this.source);
			}
			return new AssignableTypeFilter(clazz).match((MetadataReader) this.source, metadataReaderFactory);
		}

		public ConfigurationClass asConfigClass(ConfigurationClass importedBy) {
			if (this.source instanceof Class) {
				return new ConfigurationClass((Class<?>) this.source, importedBy);
			}
			return new ConfigurationClass((MetadataReader) this.source, importedBy);
		}

		public Collection<SourceClass> getMemberClasses() throws IOException {
			Object sourceToProcess = this.source;
			if (sourceToProcess instanceof Class) {
				Class<?> sourceClass = (Class<?>) sourceToProcess;
				try {
					Class<?>[] declaredClasses = sourceClass.getDeclaredClasses();
					List<SourceClass> members = new ArrayList<>(declaredClasses.length);
					for (Class<?> declaredClass : declaredClasses) {
						members.add(asSourceClass(declaredClass));
					}
					return members;
				}
				catch (NoClassDefFoundError err) {
					// getDeclaredClasses() failed because of non-resolvable dependencies
					// -> fall back to ASM below
					sourceToProcess = metadataReaderFactory.getMetadataReader(sourceClass.getName());
				}
			}

			// ASM-based resolution - safe for non-resolvable classes as well
			MetadataReader sourceReader = (MetadataReader) sourceToProcess;
			String[] memberClassNames = sourceReader.getClassMetadata().getMemberClassNames();
			List<SourceClass> members = new ArrayList<>(memberClassNames.length);
			for (String memberClassName : memberClassNames) {
				try {
					members.add(asSourceClass(memberClassName));
				}
				catch (IOException ex) {
					// Let's skip it if it's not resolvable - we're just looking for candidates
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to resolve member class [" + memberClassName +
								"] - not considering it as a configuration class candidate");
					}
				}
			}
			return members;
		}

		public SourceClass getSuperClass() throws IOException {
			if (this.source instanceof Class) {
				return asSourceClass(((Class<?>) this.source).getSuperclass());
			}
			return asSourceClass(((MetadataReader) this.source).getClassMetadata().getSuperClassName());
		}

		public Set<SourceClass> getInterfaces() throws IOException {
			Set<SourceClass> result = new LinkedHashSet<>();
			if (this.source instanceof Class) {
				Class<?> sourceClass = (Class<?>) this.source;
				for (Class<?> ifcClass : sourceClass.getInterfaces()) {
					result.add(asSourceClass(ifcClass));
				}
			}
			else {
				for (String className : this.metadata.getInterfaceNames()) {
					result.add(asSourceClass(className));
				}
			}
			return result;
		}

		public Set<SourceClass> getAnnotations() {
			Set<SourceClass> result = new LinkedHashSet<>();
			if (this.source instanceof Class) {
				Class<?> sourceClass = (Class<?>) this.source;
				for (Annotation ann : sourceClass.getAnnotations()) {
					Class<?> annType = ann.annotationType();
					if (!annType.getName().startsWith("java")) {
						try {
							result.add(asSourceClass(annType));
						}
						catch (Throwable ex) {
							// An annotation not present on the classpath is being ignored
							// by the JVM's class loading -> ignore here as well.
						}
					}
				}
			}
			else {
				for (String className : this.metadata.getAnnotationTypes()) {
					if (!className.startsWith("java")) {
						try {
							result.add(getRelated(className));
						}
						catch (Throwable ex) {
							// An annotation not present on the classpath is being ignored
							// by the JVM's class loading -> ignore here as well.
						}
					}
				}
			}
			return result;
		}

		public Collection<SourceClass> getAnnotationAttributes(String annType, String attribute) throws IOException {
			Map<String, Object> annotationAttributes = this.metadata.getAnnotationAttributes(annType, true);
			if (annotationAttributes == null || !annotationAttributes.containsKey(attribute)) {
				return Collections.emptySet();
			}
			String[] classNames = (String[]) annotationAttributes.get(attribute);
			Set<SourceClass> result = new LinkedHashSet<>();
			for (String className : classNames) {
				result.add(getRelated(className));
			}
			return result;
		}

		private SourceClass getRelated(String className) throws IOException {
			if (this.source instanceof Class) {
				try {
					Class<?> clazz = ClassUtils.forName(className, ((Class<?>) this.source).getClassLoader());
					return asSourceClass(clazz);
				}
				catch (ClassNotFoundException ex) {
					// Ignore -> fall back to ASM next, except for core java types.
					if (className.startsWith("java")) {
						throw new NestedIOException("Failed to load class [" + className + "]", ex);
					}
					return new SourceClass(metadataReaderFactory.getMetadataReader(className));
				}
			}
			return asSourceClass(className);
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof SourceClass &&
					this.metadata.getClassName().equals(((SourceClass) other).metadata.getClassName())));
		}

		@Override
		public int hashCode() {
			return this.metadata.getClassName().hashCode();
		}

		@Override
		public String toString() {
			return this.metadata.getClassName();
		}
	}

	/**
	 * {@link Problem} registered upon detection of a circular {@link Import}.
	 */
	private static class CircularImportProblem extends Problem {

		public CircularImportProblem(ConfigurationClass attemptedImport, Deque<ConfigurationClass> importStack) {
			super(String.format("A circular @Import has been detected: " +
					"Illegal attempt by @Configuration class '%s' to import class '%s' as '%s' is " +
					"already present in the current import stack %s", importStack.element().getSimpleName(),
					attemptedImport.getSimpleName(), attemptedImport.getSimpleName(), importStack),
					new Location(importStack.element().getResource(), attemptedImport.getMetadata()));
		}
	}
}
