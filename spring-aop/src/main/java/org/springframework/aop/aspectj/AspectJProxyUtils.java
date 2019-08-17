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

package org.springframework.aop.aspectj;

import org.springframework.aop.Advisor;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;

import java.util.List;

/**
 * Utility methods for working with AspectJ proxies.
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class AspectJProxyUtils {

	/**
	 * 如果需要, 则添加 ExposeInvocationInterceptor
	 */
	public static boolean makeAdvisorChainAspectJCapableIfNecessary(List<Advisor> advisors) {
		if (!advisors.isEmpty()) {
			// 查找是否有 aspectj 类型的 advice
			boolean foundAspectJAdvice = false;
			for (Advisor advisor : advisors) {
				if (isAspectJAdvice(advisor)) {
					foundAspectJAdvice = true;
					break;
				}
			}
			// 如果有 aspectj 类型的 advice 且不包含 ExposeInvocationInterceptor
			// 则添加 ExposeInvocationInterceptor
			if (foundAspectJAdvice && !advisors.contains(ExposeInvocationInterceptor.ADVISOR)) {
				advisors.add(0, ExposeInvocationInterceptor.ADVISOR);
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断切面是否包含 aspectj 类型的 advice
	 */
	private static boolean isAspectJAdvice(Advisor advisor) {
		return advisor instanceof InstantiationModelAwarePointcutAdvisor
				|| advisor.getAdvice() instanceof AbstractAspectJAdvice
				|| (advisor instanceof PointcutAdvisor && ((PointcutAdvisor) advisor).getPointcut() instanceof AspectJExpressionPointcut);
	}
}
