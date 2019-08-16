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

import org.aopalliance.aop.Advice;
import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.AspectJPrecedenceInformation;
import org.springframework.aop.aspectj.InstantiationModelAwarePointcutAdvisor;
import org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactory.AspectJAnnotation;
import org.springframework.aop.support.DynamicMethodMatcherPointcut;
import org.springframework.aop.support.Pointcuts;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;

import static org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod;

/**
 * Internal implementation of AspectJPointcutAdvisor.
 * Note that there will be one instance of this advisor for each target method.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
final class InstantiationModelAwarePointcutAdvisorImpl
		implements InstantiationModelAwarePointcutAdvisor, AspectJPrecedenceInformation, Serializable {

	private static final Advice EMPTY_ADVICE = new Advice() {};

	// 切点表达式
	private final AspectJExpressionPointcut declaredPointcut;
	// 声明 advice 的 class
	private final Class<?> declaringClass;
	// 声明 advice 的 methodName
	private final String methodName;
	// method 上的参数类型
	private final Class<?>[] parameterTypes;
	// 声明 advice 的 method
	private transient Method aspectJAdviceMethod;
	// advisorFactory
	private final AspectJAdvisorFactory aspectJAdvisorFactory;
	// aspectInstanceFactory
	private final MetadataAwareAspectInstanceFactory aspectInstanceFactory;
	// 声明顺序
	private final int declarationOrder;
	// 声明 advice 的 aspectName
	private final String aspectName;
	/** @see #InstantiationModelAwarePointcutAdvisorImpl */
	private final Pointcut pointcut;
	/** @see #InstantiationModelAwarePointcutAdvisorImpl */
	private final boolean lazy;
	// advice
	@Nullable
	private Advice instantiatedAdvice;
	// 是否 before 类型的 advice
	@Nullable
	private Boolean isBeforeAdvice;
	// 是否 after 类型的 advice
	@Nullable
	private Boolean isAfterAdvice;

	public InstantiationModelAwarePointcutAdvisorImpl(AspectJExpressionPointcut declaredPointcut,
			Method aspectJAdviceMethod, AspectJAdvisorFactory aspectJAdvisorFactory,
			MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {
		// 赋值
		this.declaredPointcut = declaredPointcut;
		this.declaringClass = aspectJAdviceMethod.getDeclaringClass();
		this.methodName = aspectJAdviceMethod.getName();
		this.parameterTypes = aspectJAdviceMethod.getParameterTypes();
		this.aspectJAdviceMethod = aspectJAdviceMethod;
		this.aspectJAdvisorFactory = aspectJAdvisorFactory;
		this.aspectInstanceFactory = aspectInstanceFactory;
		this.declarationOrder = declarationOrder;
		this.aspectName = aspectName;
		// ???
		if (aspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
			// Static part of the pointcut is a lazy type.
			Pointcut perClausePointcut = aspectInstanceFactory.getAspectMetadata().getPerClausePointcut();
			Pointcut preInstantiationPointcut = Pointcuts.union(perClausePointcut, this.declaredPointcut);
			// Make it dynamic: must mutate from pre-instantiation to post-instantiation state.
			// If it's not a dynamic pointcut, it may be optimized out
			// by the Spring AOP infrastructure after the first evaluation.
			this.pointcut = new PerTargetInstantiationModelPointcut(this.declaredPointcut, preInstantiationPointcut, aspectInstanceFactory);
			this.lazy = true;
		}
		else {
			this.pointcut = this.declaredPointcut;
			this.lazy = false;
			this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
		}
	}

	/**
	 * The pointcut for Spring AOP to use.
	 * Actual behaviour of the pointcut will change depending on the state of the advice.
	 */
	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

	@Override
	public boolean isLazy() {
		return this.lazy;
	}

	@Override
	public synchronized boolean isAdviceInstantiated() {
		return (this.instantiatedAdvice != null);
	}

	/**
	 * Lazily instantiate advice if necessary.
	 */
	@Override
	public synchronized Advice getAdvice() {
		if (this.instantiatedAdvice == null) {
			this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
		}
		return this.instantiatedAdvice;
	}

	// 构建 advice
	private Advice instantiateAdvice(AspectJExpressionPointcut pointcut) {
		Advice advice = this.aspectJAdvisorFactory.getAdvice(this.aspectJAdviceMethod, pointcut,
				this.aspectInstanceFactory, this.declarationOrder, this.aspectName);
		return (advice != null ? advice : EMPTY_ADVICE);
	}

	/**
	 * This is only of interest for Spring AOP: AspectJ instantiation semantics
	 * are much richer. In AspectJ terminology, all a return of {@code true}
	 * means here is that the aspect is not a SINGLETON.
	 */
	@Override
	public boolean isPerInstance() {
		return (getAspectMetadata().getAjType().getPerClause().getKind() != PerClauseKind.SINGLETON);
	}

	/**
	 * Return the AspectJ AspectMetadata for this advisor.
	 */
	public AspectMetadata getAspectMetadata() {
		return this.aspectInstanceFactory.getAspectMetadata();
	}

	public MetadataAwareAspectInstanceFactory getAspectInstanceFactory() {
		return this.aspectInstanceFactory;
	}

	public AspectJExpressionPointcut getDeclaredPointcut() {
		return this.declaredPointcut;
	}

	@Override
	public int getOrder() {
		return this.aspectInstanceFactory.getOrder();
	}

	@Override
	public String getAspectName() {
		return this.aspectName;
	}

	@Override
	public int getDeclarationOrder() {
		return this.declarationOrder;
	}

	@Override
	public boolean isBeforeAdvice() {
		if (this.isBeforeAdvice == null) {
			determineAdviceType();
		}
		return this.isBeforeAdvice;
	}

	@Override
	public boolean isAfterAdvice() {
		if (this.isAfterAdvice == null) {
			determineAdviceType();
		}
		return this.isAfterAdvice;
	}

	/**
	 * 取当前 advice 注解, 并判断其类型
	 */
	private void determineAdviceType() {
		AspectJAnnotation<?> aspectJAnnotation = findAspectJAnnotationOnMethod(this.aspectJAdviceMethod);
		if (aspectJAnnotation == null) {
			this.isBeforeAdvice = false;
			this.isAfterAdvice = false;
		}
		else {
			switch (aspectJAnnotation.getAnnotationType()) {
				case AtPointcut:
				case AtAround:
					this.isBeforeAdvice = false;
					this.isAfterAdvice = false;
					break;
				case AtBefore:
					this.isBeforeAdvice = true;
					this.isAfterAdvice = false;
					break;
				case AtAfter:
				case AtAfterReturning:
				case AtAfterThrowing:
					this.isBeforeAdvice = false;
					this.isAfterAdvice = true;
					break;
			}
		}
	}


	private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
		inputStream.defaultReadObject();
		try {
			this.aspectJAdviceMethod = this.declaringClass.getMethod(this.methodName, this.parameterTypes);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Failed to find advice method on deserialization", ex);
		}
	}

	@Override
	public String toString() {
		return "InstantiationModelAwarePointcutAdvisor: expression [" + getDeclaredPointcut().getExpression() +
				"]; advice method [" + this.aspectJAdviceMethod + "]; perClauseKind=" +
				this.aspectInstanceFactory.getAspectMetadata().getAjType().getPerClause().getKind();
	}


	/**
	 * Pointcut implementation that changes its behaviour when the advice is instantiated.
	 * Note that this is a <i>dynamic</i> pointcut; otherwise it might be optimized out
	 * if it does not at first match statically.
	 */
	private final class PerTargetInstantiationModelPointcut extends DynamicMethodMatcherPointcut {

		private final AspectJExpressionPointcut declaredPointcut;

		private final Pointcut preInstantiationPointcut;

		@Nullable
		private LazySingletonAspectInstanceFactoryDecorator aspectInstanceFactory;

		public PerTargetInstantiationModelPointcut(AspectJExpressionPointcut declaredPointcut,
				Pointcut preInstantiationPointcut, MetadataAwareAspectInstanceFactory aspectInstanceFactory) {

			this.declaredPointcut = declaredPointcut;
			this.preInstantiationPointcut = preInstantiationPointcut;
			if (aspectInstanceFactory instanceof LazySingletonAspectInstanceFactoryDecorator) {
				this.aspectInstanceFactory = (LazySingletonAspectInstanceFactoryDecorator) aspectInstanceFactory;
			}
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			// We're either instantiated and matching on declared pointcut,
			// or uninstantiated matching on either pointcut...
			return (isAspectMaterialized() && this.declaredPointcut.matches(method, targetClass)) ||
					this.preInstantiationPointcut.getMethodMatcher().matches(method, targetClass);
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, Object... args) {
			// This can match only on declared pointcut.
			return (isAspectMaterialized() && this.declaredPointcut.matches(method, targetClass));
		}

		private boolean isAspectMaterialized() {
			return (this.aspectInstanceFactory == null || this.aspectInstanceFactory.isMaterialized());
		}
	}

}
