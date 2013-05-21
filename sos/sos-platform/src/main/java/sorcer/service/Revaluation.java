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
package sorcer.service;

/**
 * If an Evaluation that is Revaluation isRevaluable then it returns the
 * revaluated value, otherwise returns the evaluated value (
 *
 * @return if isEvaluable true, otherwise false
 */
public interface Revaluation {

	/**
	 * Returns true if this dataContext is revaluable, otherwise false
	 * 
	 * @return true if this dataContext is revaluable
	 */
	public boolean isRevaluable();

	/**
	 * <p>
	 * Assign revaluability of this dataContext.
	 * </p>
	 * 
	 * @param isRevaluable
	 */
	public void setRevaluable(boolean isRevaluable);

}