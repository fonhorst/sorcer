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
package sorcer.util.bdb.objects;

import com.sleepycat.bind.EntityBinding;
import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.SerialSerialBinding;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.collections.StoredValueSet;
import net.jini.id.Uuid;
import sorcer.core.provider.ProviderRuntime;
import sorcer.service.Context;
import sorcer.service.Exertion;
import sorcer.service.ServiceExertion;

import java.io.IOException;

/**
 * ExertionDatabaseViews defines the data bindings and collection views for the
 * exertion database.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SorcerDatabaseViews {
	private StoredMap exertionMap;
	private StoredMap runtimeMap;
	private StoredMap runtimeByProviderNameMap;
	private StoredMap contextMap;
//	private StoredMap tableMap;
//	private StoredMap varMap;
//	private StoredMap varModelMap;
	private StoredMap uuidObjectMap;
	
	public enum Store {
		context, exertion, var, varmodel, table, object, all
	}
	/**
	 * Create the data bindings and collection views.
	 */
	public SorcerDatabaseViews(SorcerDatabase db) {
		// Create the data bindings.
		ClassCatalog catalog = db.getClassCatalog();
		SerialBinding runtimeKeyBinding = new SerialBinding(catalog, Uuid.class);
		EntityBinding runtimeDataBinding = new RuntimeBinding(catalog,
				UuidKey.class, MarshalledData.class);
		EntryBinding providerNameKeyBinding = TupleBinding
			.getPrimitiveBinding(String.class);

		runtimeMap = new StoredSortedMap(db.getRuntimeDatabase(),
				runtimeKeyBinding, runtimeDataBinding, true);
		runtimeByProviderNameMap = new StoredSortedMap(
				db.getRuntimeByProviderNameDatabase(), providerNameKeyBinding,
				runtimeDataBinding, true);
		
		SerialBinding exertiontKeyBinding = new SerialBinding(catalog, UuidKey.class);
		EntityBinding exertionDataBinding = new ExertionBinding(catalog,
				UuidKey.class, ServiceExertion.class);
	
		exertionMap = new StoredSortedMap(db.getExertionDatabase(),
				exertiontKeyBinding, exertionDataBinding, true);
		
		SerialBinding contextKeyBinding = new SerialBinding(catalog, UuidKey.class);
		EntityBinding contextDataBinding = new ContextBinding(catalog,
				UuidKey.class, MarshalledData.class);
		
		contextMap = new StoredMap(db.getContextDatabase(),
				contextKeyBinding, contextDataBinding, true);
		
		SerialBinding objectKeyBinding = new SerialBinding(catalog, UuidKey.class);
		EntityBinding objectDataBinding = new UuidObjectBinding(catalog,
				UuidKey.class, MarshalledData.class);
		
		uuidObjectMap = new StoredMap(db.getUuidObjectDatabase(),
				objectKeyBinding, objectDataBinding, true);
	}

	// The views returned below can be accessed using the java.util.Map or
	// java.util.Set interfaces, or using the StoredSortedMap and
	// StoredValueSet classes, which provide additional methods. The entity
	// sets could be obtained directly from the Map.values() method but
	// convenience methods are provided here to return them in order to avoid
	// down-casting elsewhere.
	/**
	 * Return a map view of the Exertion storage container.
	 */
	public StoredMap<UuidKey, Exertion> getExertionMap() {
		return exertionMap;
	}

	/**
	 * Return a map view of the Context storage container.
	 */
	public StoredMap<UuidKey, Context> getContextMap() {
		return contextMap;
	}

	/**
	 * Return a map view of the VarModel storage container.
	 */
	public StoredMap<UuidKey, UuidObject> getUuidObjectMap() {
		return uuidObjectMap;
	}

	/**
	 * Return an entity set view of the Exertion storage container.
	 */
	public StoredValueSet<Exertion> getExertionSet() {
		return (StoredValueSet) exertionMap.values();
	}

	/**
	 * Return an entity set view of the Runtime storage container.
	 */
	public StoredValueSet<ProviderRuntime> getRuntimeSet() {
		return (StoredValueSet) runtimeMap.values();
	}

	/**
	 * Return a map view of the runtime-by-name index.
	 */
	public StoredMap getRuntimeByProviderNameMap() {
		return runtimeByProviderNameMap;
	}
	
	/**
	 * Return an entity set view of the Context storage container.
	 */
	public StoredValueSet<Context> getContextSet() {
		return (StoredValueSet) contextMap.values();
	}
	
	/**
	 * Return an entity set view of the UuidObject storage container.
	 */
	public StoredValueSet<UuidObject> getUuidObjectSet() {
		return (StoredValueSet) uuidObjectMap.values();
	}
	
	/**
	 * ExertionBinding is used to bind the stored key/data entry pair to a
	 * combined data object (entity - Exertion).
	 */
	private static class ExertionBinding extends SerialSerialBinding {

		/**
		 * Construct the binding object.
		 */
		private ExertionBinding(ClassCatalog classCatalog, Class keyClass, Class dataClass) {
			super(classCatalog, keyClass, dataClass);
		}

		/**
		 * Create the entity by combining the stored key and data. 
		 */
		public Object entryToObject(Object keyInput, Object object) {
			((ServiceExertion)object).setId(((UuidKey)keyInput).getId());
			return object;
		}

		/**
		 * Create the stored key from the entity.
		 */
		public Object objectToKey(Object object) {
			UuidKey key = new UuidKey(((Exertion)object).getId());
			return key;
		}

		/**
		 * Return the entity as the stored data. There is nothing to do here
		 * since the entity's key fields are transient.
		 */
		public Object objectToData(Object object) {
			return object;
		}
	}

	/**
	 * RuntimeBinding is used to bind the stored key/data entry pair for a
	 * runtime to a combined data object (entity).
	 * 
	 * <p>
	 * The binding is "tricky" in that it uses the Runtime class for both the
	 * stored data entry and the combined entity object. To do this, Runtime's
	 * key field(s) are transient and are set by the binding after the data
	 * object has been deserialized. This avoids the use of a SupplierData class
	 * completely.
	 * </p>
	 */
	private static class RuntimeBinding extends SerialSerialBinding {

		/**
		 * Construct the binding object.
		 */
		private RuntimeBinding(ClassCatalog classCatalog, Class keyClass, Class dataClass) {
			super(classCatalog, keyClass, dataClass);
		}

		/**
		 * Return the entity as the stored data. 
		 */
		public Object objectToData(Object object) {
			MarshalledData runtime = null;
			try {
				runtime = new MarshalledData(object);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return runtime;
		}
		
		/* (non-Javadoc)
		 * @see com.sleepycat.bind.serial.SerialSerialBinding#entryToObject(java.lang.Object, java.lang.Object)
		 */
		@Override
		public Object entryToObject(Object keyInput, Object object) {
			ProviderRuntime runtime = null;
			MarshalledData md = (MarshalledData)object;
			try {
				runtime = (ProviderRuntime)md.get();
				runtime.setRuntimeId(((UuidKey)keyInput).getId());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return runtime;
		}

		/* (non-Javadoc)
		 * @see com.sleepycat.bind.serial.SerialSerialBinding#objectToKey(java.lang.Object)
		 */
		@Override
		public Object objectToKey(Object object) {
			return new UuidKey(((ProviderRuntime)object).getRuntimeId());
		}
	}
	
	/**
	 * ContextBinding is used to bind the stored key/data entry pair to a
	 * combined data object (entity).
	 */
	public static class ContextBinding extends SerialSerialBinding {

		/**
		 * Construct the binding object.
		 */
		private ContextBinding(ClassCatalog classCatalog, Class keyClass, Class dataClass) {
			super(classCatalog, keyClass, dataClass);
		}
		
		/**
		 * Return the entity as the stored data. 
		 */
		public Object objectToData(Object object) {
			MarshalledData mContext = null;
			try {
				mContext = new MarshalledData(object);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return mContext;
		}

		/* (non-Javadoc)
		 * @see com.sleepycat.bind.serial.SerialSerialBinding#entryToObject(java.lang.Object, java.lang.Object)
		 */
		@Override
		public Object entryToObject(Object keyInput, Object object) {
			Context context = null;
			MarshalledData md = (MarshalledData)object;
			try {
				context = (Context)md.get();
				context.setId(((UuidKey)keyInput).getId());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return context;
		}

		/* (non-Javadoc)
		 * @see com.sleepycat.bind.serial.SerialSerialBinding#objectToKey(java.lang.Object)
		 */
		@Override
		public Object objectToKey(Object object) {
			return new UuidKey(((Context)object).getId());
		}
	}
		
	/**
	 * ObjectBinding is used to bind the stored key/data entry pair to a
	 * combined data object (entity).
	 */
	public static class UuidObjectBinding extends SerialSerialBinding {

		/**
		 * Construct the binding object.
		 */
		private UuidObjectBinding(ClassCatalog classCatalog, Class keyClass,
				Class dataClass) {
			super(classCatalog, keyClass, dataClass);
		}

		/**
		 * Return the entity as the stored data.
		 */
		public Object objectToData(Object object) {
			MarshalledData mObject = null;
			try {
				if (object instanceof MarshalledData)
					mObject = (MarshalledData)object;
				else {
					mObject = new MarshalledData(object);
					
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return mObject;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sleepycat.bind.serial.SerialSerialBinding#entryToObject(java.
		 * lang.Object, java.lang.Object)
		 */
		@Override
		public Object entryToObject(Object keyInput, Object object) {
			UuidObject obj = null;
			MarshalledData md = (MarshalledData) object;
			try {
				obj = (UuidObject) md.get();
				obj.setId(((UuidKey) keyInput).getId());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return obj;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sleepycat.bind.serial.SerialSerialBinding#objectToKey(java.lang
		 * .Object)
		 */
		@Override
		public Object objectToKey(Object object) {
			return new UuidKey(((UuidObject) object).getId());
		}
	}
	
	public static Store getStoreType(String storeName) {
		for (Store s : Store.values()) {
			if (storeName.equals(""+s))
				return s;
		}
		return null;
	}
}
