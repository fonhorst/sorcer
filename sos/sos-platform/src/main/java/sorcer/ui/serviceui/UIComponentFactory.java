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
package sorcer.ui.serviceui;

import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.ui.factory.JComponentFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

/**
 * The UIComponentFactory class is a helper for use with the ServiceUI
 */
public class UIComponentFactory implements JComponentFactory, Serializable {
	
	static final long serialVersionUID = 5806535989492809459L;

	private String className;

	private URL[] exportURL;

	private URLClassLoader uiLoader;

	private JFrame frame = null;

	private boolean isFrame = false;

	public UIComponentFactory(URL exportUrl, String className) {
		this.className = className;
		this.exportURL = new URL[] { exportUrl };
	}

	public UIComponentFactory(URL[] exportURL, String className) {
		this.className = className;
		this.exportURL = exportURL;
	}

	public UIComponentFactory(URL exportUrl, String className, boolean isFrame) {
		this.className = className;
		this.exportURL = new URL[] { exportUrl };
		this.isFrame = isFrame;
	}

	public UIComponentFactory(URL[] exportURL, String className, boolean isFrame) {
		this.className = className;
		this.exportURL = exportURL;
		this.isFrame = isFrame;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JComponent getJComponent(Object roleObject) {
		if (!(roleObject instanceof ServiceItem)) {
			throw new IllegalArgumentException("ServiceItem required");
		}
		ClassLoader cl = ((ServiceItem) roleObject).service.getClass()
				.getClassLoader();
		JComponent component = null;
		try {
			uiLoader = URLClassLoader.newInstance(exportURL, cl);
			final Thread currentThread = Thread.currentThread();
			final ClassLoader parentLoader = (ClassLoader) AccessController
					.doPrivileged(new PrivilegedAction() {
						public Object run() {
							return (currentThread.getContextClassLoader());
						}
					});

			try {
				AccessController.doPrivileged(new PrivilegedAction() {
					public Object run() {
						currentThread.setContextClassLoader(uiLoader);
						return (null);
					}
				});
				Class clazz = null;
				try {
					clazz = uiLoader.loadClass(className);
				} catch (ClassNotFoundException ex) {
					ex.printStackTrace();
					throw ex;
				}
				Constructor constructor = clazz
						.getConstructor(new Class[] { Object.class });
				Object instanceObj = constructor
						.newInstance(new Object[] { roleObject });
				if (isFrame && frame == null)
					return getPanel(instanceObj);
				else
					component = (JComponent) instanceObj;
			} finally {
				AccessController.doPrivileged(new PrivilegedAction() {
					public Object run() {
						currentThread.setContextClassLoader(parentLoader);
						return (null);
					}
				});
			}
		} catch (Throwable e) {
			e.printStackTrace();
			throw new IllegalArgumentException(
					"Unable to instantiate ServiceUI:"
							+ className + ":"
							+ e.getClass().getName() + ":"
							+ e.getLocalizedMessage());
		}
		if (frame == null)
			return (component);
		else
			return new JPanel();
	}

	private void showFrame() {
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				frame.dispose();
				frame = null;
			}
		});
		frame.setVisible(true);
	}

	private JPanel getPanel(Object object) {
		frame = (JFrame)object;
		JButton button = new JButton(frame.getAccessibleContext()
				.getAccessibleName());

		class FrameListener implements ActionListener {
			public void actionPerformed(ActionEvent event) {
				showFrame();
			}
		}

		ActionListener listener = new FrameListener();
		button.addActionListener(listener);

		JPanel panel = new JPanel();
		panel.add(button);

		return panel;
	}

	public String toString() {
		return "UIComponentFactory for: " + className + ", exportURL: "
				+ Arrays.toString(exportURL);
	}
}
