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
package sorcer.util.bdb.sdb;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import sorcer.core.StorageManagement;
import sorcer.core.context.ServiceContext;
import sorcer.service.*;
import sorcer.util.Sorcer;
import sorcer.util.bdb.SosURL;
import sorcer.util.bdb.objects.SorcerDatabaseViews;
import sorcer.util.bdb.objects.SorcerDatabaseViews.Store;

import java.net.URL;
import java.util.List;

import static sorcer.eo.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public class SdbUtil {

	public static Uuid getUuid(URL url) {
		String urlRef = url.getRef();
		int index = urlRef.indexOf('=');
		// storeType = SorcerDatabaseViews.getStoreType(reference.substring(0,
		// index));

		return UuidFactory.create(urlRef.substring(index + 1));
	}

	public static Store getStoreType(URL url) {
		String urlRef = url.getRef();
		int index = urlRef.indexOf('=');
		return SorcerDatabaseViews.getStoreType(urlRef.substring(0, index));
	}

	public static String getProviderName(URL url) {
		if (url == null)
			return null;
		else
			return url.getPath().substring(1);
	}

	public static String getServiceType(URL url) {
		return url.getHost();
	}

	/**
	 * Returns a dataContext to be used with
	 * {@link StorageManagement#contextStore(Context)}
	 *
	 * @param object
	 *            to be stored
	 * @return storage {@link Context}
	 * @throws ContextException
	 */
	static public Context getStoreContext(Object object)
			throws ContextException {
		ServiceContext cxt = new ServiceContext("store dataContext");
		cxt.putInValue(StorageManagement.object_stored, object);
		cxt.putInValue(StorageManagement.object_uuid,
				((Identifiable) object).getId());
		cxt.setReturnPath(StorageManagement.object_url);
		return cxt;
	}

	/**
	 * Returns a dataContext to be used with
	 * {@link StorageManagement#contextRetrieve(Context)}
	 * 
	 * @param uuid
	 *            {@link Uuid}
	 * @param type
	 *            one of: exertion, dataContext, var, table, varModel, object
	 * @return retrieval {@link Context}
	 * @throws ContextException
	 */
	static public Context getRetrieveContext(Uuid uuid, Store type)
			throws ContextException {
		ServiceContext cxt = new ServiceContext("retrieve dataContext");
		cxt.putInValue(StorageManagement.object_type, type);
		cxt.putInValue(StorageManagement.object_uuid, uuid);
		cxt.setReturnPath(StorageManagement.object_retrieved);
		return cxt;
	}

	static public Context getUpdateContext(Object object, URL url)
			throws ContextException {
		return getUpdateContext(object, getUuid(url));
	}

	/**
	 * Returns a dataContext to be used with
	 * {@link StorageManagement#contextUpdate(Context)}
	 * 
	 * @param object
	 *            to be updated
	 * @param uuid
	 *            {@link Uuid} og the updated object
	 * @return update {@link Context}
	 * @throws ContextException
	 */
	static public Context getUpdateContext(Object object, Uuid uuid)
			throws ContextException {
		ServiceContext cxt = new ServiceContext("update dataContext");
		cxt.putInValue(StorageManagement.object_uuid, uuid);
		cxt.putInValue(StorageManagement.object_updated, object);
		cxt.setReturnPath(StorageManagement.object_url);
		return cxt;
	}

	static public Context getListContext(Store storeType)
			throws ContextException {
		ServiceContext cxt = new ServiceContext("storage list dataContext");
		cxt.putInValue(StorageManagement.store_type, storeType);
		cxt.setReturnPath(StorageManagement.store_content_list);
		return cxt;
	}

	static public URL update(Object value) throws ExertionException,
			SignatureException, ContextException {
		if (!(value instanceof Identifiable)
				|| !(((Identifiable) value).getId() instanceof Uuid)) {
			throw new ContextException("Object is not Uuid Identifiable: "
					+ value);
		}
		return SdbUtil.update((Uuid) ((Identifiable) value).getId(), value);
	}

	static public URL update(URL storedURL, Object value)
			throws ExertionException, SignatureException, ContextException {
		return SdbUtil.update(SdbUtil.getUuid(storedURL), value);
	}

	static public URL update(Uuid storeUuid, Object value)
			throws ExertionException, SignatureException, ContextException {
		Task objectUpdateTask = task(
				"update",
				sig("contextUpdate", DatabaseStorer.class,
						Sorcer.getActualDatabaseStorerName()),
				SdbUtil.getUpdateContext(value, storeUuid));

		objectUpdateTask = exert(objectUpdateTask);
		return (URL) get(context(objectUpdateTask),
				StorageManagement.object_url);
	}

	public static int clear(Store type) throws ExertionException,
			SignatureException, ContextException {
		String storageName = Sorcer.getActualName(Sorcer
				.getDatabaseStorerName());
		Task objectStoreTask = task(
				"clear",
				sig("contextClear", DatabaseStorer.class, storageName),
				context("clear", in(StorageManagement.store_type, type),
						result(StorageManagement.store_size)));
		return (Integer) value(objectStoreTask);
	}

	public static int size(Store type) throws ExertionException,
			SignatureException, ContextException {
		String storageName = Sorcer.getActualName(Sorcer
				.getDatabaseStorerName());
		Task objectStoreTask = task(
				"size",
				sig("contextSize", DatabaseStorer.class, storageName),
				context("size", in(StorageManagement.store_type, type),
						result(StorageManagement.store_size)));
		return (Integer) value(objectStoreTask);
	}
	
	public static URL delete(Object object) throws ExertionException,
			SignatureException, ContextException {
		if (object instanceof URL) {
			return deleteURL((URL) object);
		} else {
			return deleteObject(object);
		}
	}

	public static URL deleteObject(Object object) throws ExertionException,
			SignatureException, ContextException {
		String storageName = Sorcer.getActualName(Sorcer
				.getDatabaseStorerName());
		Task objectStoreTask = task(
				"delete",
				sig("contextDelete", DatabaseStorer.class, storageName),
				context("delete", in(StorageManagement.object_deleted, object),
						result(StorageManagement.object_url)));
		return (URL) value(objectStoreTask);
	}

	public static URL deleteURL(URL url) throws ExertionException,
			SignatureException, ContextException {
		String serviceTypeName = getServiceType(url);
		String storageName = getProviderName(url);
		Task objectStoreTask = null;
		try {
			objectStoreTask = task(
					"delete",
					sig("contextDelete", Class.forName(serviceTypeName),
							storageName),
					context("delete",
							in(StorageManagement.object_deleted, url),
							result(StorageManagement.object_url)));
		} catch (ClassNotFoundException e) {
			throw new SignatureException("No such service type: "
					+ serviceTypeName, e);
		}
		return (URL) value(objectStoreTask);
	}

	public static URL store(Object object) throws ExertionException,
			SignatureException, ContextException {
		String storageName = Sorcer.getActualName(Sorcer
				.getDatabaseStorerName());
		Task objectStoreTask = task(
				"store",
				sig("contextStore", DatabaseStorer.class, storageName),
				context("store", in(StorageManagement.object_stored, object),
						result("result, stored/object/url")));
		return (URL) value(objectStoreTask);
	}

	public static SosURL sosStore(Object object) throws ExertionException,
			SignatureException, ContextException {
		return new SosURL(store(object));
	}
	
	public static URL write(Object object) throws ExertionException,
			SignatureException, ContextException {
		String storageName = Sorcer.getActualName(Sorcer.getSpacerName());
		Task objectStoreTask = task(
				"write",
				sig("contextWrite", DataspaceStorer.class, storageName),
				context("stored", in(StorageManagement.object_stored, object),
						result("result, stored/object/url")));
		return (URL) value(objectStoreTask);
	}

	static public Object retrieve(URL url) throws ExertionException,
			SignatureException, ContextException {
		return retrieve(SdbUtil.getUuid(url), getStoreType(url));
	}

	static public Object retrieve(Uuid storeUuid, Store storeType)
			throws ExertionException, SignatureException, ContextException {
		Task objectRetrieveTask = task(
				"retrieve",
				sig("contextRetrieve", DatabaseStorer.class,
						Sorcer.getActualDatabaseStorerName()),
				SdbUtil.getRetrieveContext(storeUuid, storeType));

		return get((Context) value(objectRetrieveTask));
	}

	static public List<String> list(URL url) throws ExertionException,
			SignatureException, ContextException {
		return list(url, null);

	}

	@SuppressWarnings("unchecked")
	static public List<String> list(URL url, Store storeType)
			throws ExertionException, SignatureException, ContextException {
		Store type = storeType;
		String providerName = getProviderName(url);
		if (providerName == null)
			providerName = Sorcer.getActualDatabaseStorerName();

		if (type == null) {
			type = getStoreType(url);
			if (type == null) {
				type = Store.object;
			}
		}
		Task listTask = task("list",
				sig("contextList", DatabaseStorer.class, providerName),
				SdbUtil.getListContext(type));

		return (List<String>) value(listTask);
	}

	@SuppressWarnings("unchecked")
	static public List<String> list(Store storeType) throws ExertionException,
			SignatureException, ContextException {
		String storageName = Sorcer.getActualName(Sorcer
				.getDatabaseStorerName());
	
		Task listTask = task("contextList",
				sig("contextList", DatabaseStorer.class, storageName),
				SdbUtil.getListContext(storeType));

		return (List<String>) value(listTask);
	}
}
