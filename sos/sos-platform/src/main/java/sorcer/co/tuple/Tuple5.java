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
package sorcer.co.tuple;

import java.io.Serializable;

public class Tuple5<T1, T2, T3, T4, T5>  implements Serializable {
	public T1 _1 = null;
	public T2 _2 = null;
	public T3 _3 = null;
	public T4 _4 = null;
	public T5 _5 = null;

	public Tuple5(T1 x1, T2 x2, T3 x3, T4 x4, T5 x5) {
		_1 = x1;
		_2 = x2;
		_3 = x3;
		_4 = x4;
		_5 = x5;
	}
}