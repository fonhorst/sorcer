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
package sorcer.tools.webster;

import sorcer.core.SorcerConstants;
import sorcer.core.SorcerEnv;
import sorcer.resolver.Resolver;
import sorcer.util.Artifact;
import sorcer.util.JavaSystemProperties;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class for starting an Internal Webster
 *
 * @author Dennis Reedy, adapted for SORCER by Mike Sobolewski
 */
public class InternalWebster {
    private static Logger logger = Logger.getLogger("sorcer.tools.webster");
    private static boolean debug = false;
    public static final String WEBSTER_ROOTS = "webster.roots";
    private static Webster webster;

    /**
     * Start an internal webster, setting the webster root to the location of
     * SORCER lib-dl directories, and appending exportJars as the export jars
     * for the JVM.
     *
     * @param exportJars
     *            The jars to set for the codebase
     *
     * @return The port Webster has been started on
     *
     * @throws IOException
     *             If there are errors creating Webster
     */
    public static Webster startWebster(String[] exportJars, String[] websterRoots) throws IOException {
        String codebase = System.getProperty("java.rmi.server.codebase");
        if (codebase != null)
            throw new RuntimeException("Codebase is alredy specified: "
                    + codebase);

        String d = System.getProperty("webster.debug");
        if (d != null && d.equals("true"))
            debug = true;

        String roots;
        InetAddress ip = SorcerEnv.getLocalHost();
        String localIPAddress = ip.getHostAddress();
        String sorcerHome = System.getProperty("sorcer.home");
        roots = System.getProperty(WEBSTER_ROOTS);
        String fs = File.separator;
        StringBuffer sb = new StringBuffer();
        if (roots == null && websterRoots == null) {
            // defaults Sorcer roots
            sb.append(";").append(Resolver.getRepoDir()).append(";").append(Resolver.getRootDir());
        } else if (websterRoots != null) {
            for (int i=0; i<websterRoots.length; i++) {
                sb.append(';').append(websterRoots[i]);
            }
        }
        roots = sb.toString();

        String sMinThreads = System.getProperty("webster.minThreads",
                "1");
        int minThreads = 1;
        try {
            minThreads = Integer.parseInt(sMinThreads);
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Bad Min Threads Number [" + sMinThreads
                    + "], " + "default to " + minThreads, e);
        }
        String sMaxThreads = System.getProperty("webster.maxThreads",
                "10");
        int maxThreads = 10;
        try {
            maxThreads = Integer.parseInt(sMaxThreads);
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Bad Max Threads Number [" + sMaxThreads
                    + "], " + "default to " + maxThreads, e);
        }
        String sPort = System.getProperty("webster.port", "0");
        int port = 0;
        try {
            port = Integer.parseInt(sPort);
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Bad port Number [" + sPort + "], "
                    + "default to " + port, e);
        }

        String address = System.getProperty(SorcerConstants.S_WEBSTER_INTERFACE);
        webster = new Webster(port, roots, address, minThreads, maxThreads, true);
        port = webster.getPort();
        if (logger.isLoggable(Level.FINEST))
            logger.finest("Webster MinThreads=" + minThreads + ", "
                    + "MaxThreads=" + maxThreads);

        if (logger.isLoggable(Level.FINE))
            logger.fine("Webster serving on port=" + port);

        String[] jars = null;
        String jarsList = null;
        if (exportJars != null)
            jars = exportJars;
        else {
            jarsList = System.getProperty(SorcerConstants.CODEBASE_JARS);
            if (jarsList == null || jarsList.length() == 0)
                throw new RuntimeException(
                        "No jar files available for the webster codebase");
            else
                jars = toArray(jarsList);
        }

        codebase = "";
        sb = new StringBuffer();
        for (int i = 0; i < jars.length - 1; i++) {
            sb.append("http://").append(localIPAddress).append(":")
                    .append(port).append("/").append(jars[i]).append(" ");
        }
        sb.append("http://").append(localIPAddress).append(":").append(port)
                .append("/").append(jars[jars.length - 1]);
        codebase = sb.toString();
        System.setProperty("java.rmi.server.codebase", codebase);
        if (logger.isLoggable(Level.FINE))
            logger.fine("Setting 'java.rmi.server.codebase': " + codebase);
        return webster;
    }

    public static Webster startRequestorWebsterFromProperties() {
        SorcerEnv env = SorcerEnv.getEnvironment();
        String userCodebase = env.getRequestorWebsterCodebase();

        String roots = env.getWebsterRootsString();
        try {
            Webster webster = new Webster(roots, true);

            URL root = SorcerEnv.getCodebaseRoot(SorcerEnv.getHostAddress(), Webster.getPort());
            String codebase;
            if (userCodebase == null || userCodebase.isEmpty()) {
                codebase = Resolver.resolveCodeBase(root, Artifact.getSosEnv(), Artifact.getSosPlatform());
            } else {
                String[] jars = userCodebase.split(" ");
                codebase = SorcerEnv.getCodebase(root, jars);
            }
            System.setProperty(JavaSystemProperties.RMI_SERVER_CODEBASE, codebase);
            return webster;
        } catch (BindException e) {
            throw new IllegalStateException("Could not bind to " + Webster.getWebster().getAddress() + ":" + Webster.getPort(), e);
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Could not obtain local address", e);
        }
    }

    private static String[] toArray(String arg) {
        StringTokenizer token = new StringTokenizer(arg, " ,;");
        String[] array = new String[token.countTokens()];
        int i = 0;
        while (token.hasMoreTokens()) {
            array[i] = token.nextToken();
            i++;
        }
        return (array);
    }

    public static void stopWebster() {
        if (webster != null)
            webster.terminate();
    }

    public static void main(String[] args) {
        try {
            String home = System.getenv("user.home");
            startWebster(new String[] { "sorcer/sos-platform.jar" }, new String[] { home + "/.m2/repository/", home + "/lib/" });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}

