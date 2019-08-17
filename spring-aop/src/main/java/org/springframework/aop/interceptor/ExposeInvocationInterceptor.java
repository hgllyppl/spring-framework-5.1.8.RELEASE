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

package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.PriorityOrdered;

import java.io.Serializable;

/**
 * Interceptor that exposes the current {@link org.aopalliance.intercept.MethodInvocation}
 * as a thread-local object. We occasionally need to do this; for example, when a pointcut
 * (e.g. an AspectJ expression pointcut) needs to know the full invocation context.
 *
 * <p>Don't use this interceptor unless this is really necessary. Target objects should
 * not normally know about Spring AOP, as this creates a dependency on Spring API.
 * Target objects should be plain POJOs as far as possible.
 *
 * <p>If used, this interceptor will normally be the first in the interceptor chain.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public final class ExposeInvocationInterceptor implements MethodInterceptor, PriorityOrdered, Serializable {

	public static final ExposeInvocationInterceptor INSTANCE = new ExposeInvocationInterceptor();

	public static final Advisor ADVISOR = new DefaultPointcutAdvisor(INSTANCE) {
		@Override
		public String toString() {
			return ExposeInvocationInterceptor.class.getName() +".ADVISOR";
		}
	};

	private static final ThreadLocal<MethodInvocation> invocation = new NamedThreadLocal<>("Current AOP method invocation");


	private ExposeInvocationInterceptor() {
	}

	/**
	 * 获取当前正在执行的 MethodInvocation
	 */
	public static MethodInvocation currentInvocation() throws IllegalStateException {
		MethodInvocation mi = invocation.get();
		if (mi == null) {
			throw new IllegalStateException(
					"No MethodInvocation found: Check that an AOP invocation is in progress, and that the " +
							"ExposeInvocationInterceptor is upfront in the interceptor chain. Specifically, note that " +
							"advices with order HIGHEST_PRECEDENCE will execute before ExposeInvocationInterceptor!");
		}
		return mi;
	}

	// 将当前正在执行的 MethodInvocation 设置进 TL
	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		MethodInvocation oldInvocation = invocation.get();
		invocation.set(mi);
		try {
			return mi.proceed();
		}
		finally {
			invocation.set(oldInvocation);
		}
	}

	@Override
	public int getOrder() {
		return PriorityOrdered.HIGHEST_PRECEDENCE + 1;
	}

	/**
	 * 支持序列化操作
	 */
	private Object readResolve() {
		return INSTANCE;
	}

}
