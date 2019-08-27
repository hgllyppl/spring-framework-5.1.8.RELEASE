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

package org.springframework.web.method.annotation;

import org.springframework.core.ExceptionDepthComparator;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Discovers {@linkplain ExceptionHandler @ExceptionHandler} methods in a given class,
 * including all of its superclasses, and helps to resolve a given {@link Exception}
 * to the exception types supported by a given {@link Method}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ExceptionHandlerMethodResolver {

	/**
	 * A filter for selecting {@code @ExceptionHandler} methods.
	 */
	public static final MethodFilter EXCEPTION_HANDLER_METHODS = method ->
			AnnotatedElementUtils.hasAnnotation(method, ExceptionHandler.class);

	// 异常类型与处理方法的映射
	private final Map<Class<? extends Throwable>, Method> mappedMethods = new HashMap<>(16);
	// 缓存运行时产生的异常类型与处理方法之间的映射
	private final Map<Class<? extends Throwable>, Method> exceptionLookupCache = new ConcurrentReferenceHashMap<>(16);

	public ExceptionHandlerMethodResolver(Class<?> handlerType) {
		// 查找带 @ExceptionHandler 的方法
		Set<Method> exMethods = MethodIntrospector.selectMethods(handlerType, EXCEPTION_HANDLER_METHODS);
		for (Method method : exMethods) {
			// 查找要处理的异常列表
			List<Class<? extends Throwable>> exMappings = detectExceptionMappings(method);
			// 映射异常类型与处理方法
			for (Class<? extends Throwable> exceptionType : exMappings) {
				addExceptionMapping(exceptionType, method);
			}
		}
	}

	/**
	 * 查找给定 method 要处理的异常类型
	 */
	@SuppressWarnings("unchecked")
	private List<Class<? extends Throwable>> detectExceptionMappings(Method method) {
		List<Class<? extends Throwable>> result = new ArrayList<>();
		// 从 @ExceptionHandler 读取异常类型
		detectAnnotationExceptionMappings(method, result);
		// 从方法参数读取异常类型
		if (result.isEmpty()) {
			for (Class<?> paramType : method.getParameterTypes()) {
				if (Throwable.class.isAssignableFrom(paramType)) {
					result.add((Class<? extends Throwable>) paramType);
				}
			}
		}
		// 如果异常列表为空, 则抛出异常
		if (result.isEmpty()) {
			throw new IllegalStateException("No exception types mapped to " + method);
		}
		return result;
	}

	private void detectAnnotationExceptionMappings(Method method, List<Class<? extends Throwable>> result) {
		ExceptionHandler ann = AnnotatedElementUtils.findMergedAnnotation(method, ExceptionHandler.class);
		Assert.state(ann != null, "No ExceptionHandler annotation");
		result.addAll(Arrays.asList(ann.value()));
	}

	// 映射异常类型与其处理方法
	private void addExceptionMapping(Class<? extends Throwable> exceptionType, Method method) {
		Method oldMethod = this.mappedMethods.put(exceptionType, method);
		if (oldMethod != null && !oldMethod.equals(method)) {
			throw new IllegalStateException("Ambiguous @ExceptionHandler method mapped for [" +
					exceptionType + "]: {" + oldMethod + ", " + method + "}");
		}
	}

	/**
	 * Whether the contained type has any exception mappings.
	 */
	public boolean hasExceptionMappings() {
		return !this.mappedMethods.isEmpty();
	}

	/**
	 * 找到能够处理异常的方法
	 */
	@Nullable
	public Method resolveMethod(Exception exception) {
		return resolveMethodByThrowable(exception);
	}

	/**
	 * 找到能够处理异常的方法
	 */
	@Nullable
	public Method resolveMethodByThrowable(Throwable exception) {
		Method method = resolveMethodByExceptionType(exception.getClass());
		// 如果方法为空, 则使用 cause 再找一次
		if (method == null) {
			Throwable cause = exception.getCause();
			if (cause != null) {
				method = resolveMethodByExceptionType(cause.getClass());
			}
		}
		return method;
	}

	/**
	 * 找到能够处理异常的方法
	 */
	@Nullable
	public Method resolveMethodByExceptionType(Class<? extends Throwable> exceptionType) {
		// 从 exceptionLookupCache 中查找方法
		Method method = this.exceptionLookupCache.get(exceptionType);
		// 找不到, 则从 mappedMethods 中查找方法
		if (method == null) {
			method = getMappedMethod(exceptionType);
			// 缓存方法
			this.exceptionLookupCache.put(exceptionType, method);
		}
		return method;
	}

	/**
	 * 真•找到能够处理异常的方法
	 */
	@Nullable
	private Method getMappedMethod(Class<? extends Throwable> exceptionType) {
		List<Class<? extends Throwable>> matches = new ArrayList<>();
		// 迭代 mappedMethods 并找到符合异常类型的处理方法
		for (Class<? extends Throwable> mappedException : this.mappedMethods.keySet()) {
			if (mappedException.isAssignableFrom(exceptionType)) {
				matches.add(mappedException);
			}
		}
		// 排序结果集并取第一个作为结果返回
		if (!matches.isEmpty()) {
			matches.sort(new ExceptionDepthComparator(exceptionType));
			return this.mappedMethods.get(matches.get(0));
		}
		else {
			return null;
		}
	}

}
