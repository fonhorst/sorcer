/**
 *
 * Copyright 2013 the original author or authors.
 * Copyright 2013 Sorcersoft.com S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sorcer.core.signature;

import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.SignatureException;
import sorcer.util.ObjectCloner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ObjectSignature extends ServiceSignature {

	//static final long serialVersionUID = 8042346568722803852L;

	Class<?> providerType;

	private Object target;

	// list of initialization arguments for the constructor
	private Object[] args;	

	private Class<?>[] argTypes;

	public ObjectSignature() {
        execType = Type.SRV;
	}

	public ObjectSignature(String selector, Object object, Class<?>[] argTypes)
			throws InstantiationException, IllegalAccessException {
		if (object instanceof Class)
			this.providerType = (Class<?>)object;
		else {
			target = object;
			this.providerType = object.getClass();
		}

		this.argTypes = argTypes;
		setSelector(selector);
	}

	public ObjectSignature(String selector, Object object, Class<?>[] argTypes,
			Object... args) throws InstantiationException,
			IllegalAccessException {
		if (object instanceof Class) 
			this.providerType = (Class<?>)object;
		else {
			target = object;
			this.providerType = object.getClass();
		}

		this.argTypes = argTypes;
		this.args = args;
		setSelector(selector);
	}

	public ObjectSignature(String selector, Class<?> providerClass,
			Class<?>... argClasses) {
		this.providerType = providerClass;
		this.argTypes = argClasses;
		setSelector(selector);
	}

	public ObjectSignature(String selector, Class<?> providerClass) {
		this.providerType = providerClass;
		setSelector(selector);
	}

	public ObjectSignature(Class<?> providerClass) {
		this.selector = null;
		this.providerType = providerClass;
	}

	/**
	 * <p>
	 * Returns the object being a provider of this signature.
	 * </p>
	 * 
	 * @return the object provider
	 */
	public Object getTarget() {
		return target;
	}

	/**
	 * <p>
	 * Assigns the object being a provider of this signature.
	 * </p>
	 * 
	 * @param target
	 *            the  object provider to set
	 */
	public void setTarget(Object target) {
		this.target = target;
	}

	/**
	 * <p>
	 * Returns a provider class for this signature.
	 * </p>
	 * 
	 * @return the providerClass
	 */
	public Class<?> getProviderType() {
		return providerType;
	}

	/**
	 * <p>
	 * Assigns a provider class for this signature.
	 * </p>
	 */
	public void setProviderType(Class<?> providerType) {
		this.providerType = providerType;
	}

	public Class<?>[] getTypes() throws ContextException {
		return argTypes;
	}

	public void setTypes(Class<?>... types) throws ContextException {
		argTypes = types;
	}

	/**
	 * Returns a new instance using a constructor as specified by this
	 * signature.
	 * 
	 * @return a new instance
	 * @throws SignatureException
	 */
	public Object newInstance() throws SignatureException {
		Constructor<?> constructor = null;
		Object obj = null;
		try {
			if (providerType != null) {
				//serviceType.cast(providerClass.newInstance());
				return providerType.newInstance();
			}
			if (argTypes == null) {
				constructor = providerType.getConstructor();
				obj = constructor.newInstance();
			} else {
				if (args == null)
					throw new SignatureException(
							"Missing args to the constructor: " + providerType);
				constructor = providerType.getConstructor(argTypes);
				obj = constructor.newInstance(args);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new SignatureException(e);
		}
		logger.fine(">>>>>>>>>>> instantiated: \n" + obj + "\n by signature: "
				+ this);
		return obj;
	}

	public Object newInstance(Object[] args) throws SignatureException {
		this.args = args;
		return newInstance();
	}

	public Object newInstance(Object[] args, Class<?>[] argClasses)
			throws SignatureException {
		this.args = args;
		this.argTypes = argClasses;
		return newInstance();
	}

	/**
	 * Returns a new instance using initialization by the instance or class method as
	 * specified by this signature.
	 * 
	 * @return a new instance
	 * @throws SignatureException
	 */
	public Object initInstance() throws SignatureException {
		Object obj = null;
		Method m = null;
		try {
			logger.info("providerType: " + providerType);
			logger.info("selector: " + selector);
			logger.info("argTypes: " + Arrays.toString(argTypes));

			obj = providerType.newInstance();
			if (argTypes != null)
				m = providerType.getMethod(selector, argTypes);
			else 
				m = providerType.getMethod(selector);

            if (args != null) {
				obj = m.invoke(obj, args);
			}
			else if (argTypes != null && argTypes.length == 1 && args == null) {
				obj = m.invoke(obj, new Object[] { null });
			}
			else {
				obj = m.invoke(obj);
			}
		} catch (Exception e) {
            logger.throwing(ObjectSignature.class.getName(), "initInstance", e);
			try {
				// check if that is SORCER service bean signature
				m = providerType.getMethod(selector, Context.class);
				if (m.getReturnType() == Context.class)  {
                    return obj;
                }
				else
					throw new SignatureException(e);
			} catch (Exception e1) {
				e.printStackTrace();
				throw new SignatureException(e);
			} 
		}
		// logger.fine(">>>>>>>>>>> instantiated: \n" + obj +
		// "\n by signature: " + this);
		return obj;
	}

	public boolean isSelectable() {
		if (selector == null && providerType == null) {
			return false;
		}
		Method[] methods = providerType.getMethods();
		for (Method m : methods) {
			if (m.getName().equals(selector)) {
				return true;
			}
		}
		return false;
	}

	public Object initInstance(Object[] args) throws SignatureException {
		this.args = args;
		return initInstance();
	}

	public Object initInstance(Object[] args, Class<?>[] argClasses)
			throws SignatureException {
		this.args = args;
		this.argTypes = argClasses;
		return initInstance();
	}

	public Object build() throws SignatureException {
		return build(null);
	}

	public Object build(Context<?> inContext) throws SignatureException {
		Object obj = null;
		Method m = null;
		try {
			if (argTypes != null) {
				m = providerType.getMethod(selector, argTypes);
			} else {
				m = providerType.getMethod(selector);
			}
			if (args != null) {
				// clone the arguments for a new parametric model
				Object[] clonedArgs = (Object[]) ObjectCloner.clone(args);
				obj = m.invoke(null, clonedArgs);
			} else {
				obj = m.invoke(null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new SignatureException(e);
		}
		return obj;
	}

	public String toString() {
		String provider = providerType == null ? ""+target.getClass() : ""+providerType;

		return this.getClass() + ";" + providerName + ";" + execType + ";" + isActive + ";"
		+ provider + ";" + selector;
	}
}
