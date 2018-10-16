/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.datavyu.plugins;

import com.google.common.collect.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.Datavyu;
import org.datavyu.plugins.ffmpegplayer.FFmpegPlugin;
import org.datavyu.plugins.javafx.JfxPlugin;
import org.datavyu.util.MacOS;
import org.jdesktop.application.LocalStorage;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * This class manages and wrangles all the viewer plugins currently available in Datavyu. It is implemented as a
 * singleton, so only one instance is available to Datavyu. This single instance will load all plugins that implement
 * the Plugin interface.
 */
public final class PluginManager {

    /* WARNING: pluginClass, static { pluginClass }, logger and pluginManager must appear in this order */
    /** A reference to the interface that plugins must override */
    private static final Class<?> pluginClass; // = Plugin.class;

    static {
        pluginClass = Plugin.class;
    }

    /** The logger for this class */
    private static Logger logger = LogManager.getLogger(PluginManager.class.getName());

    /** Single instance of the PluginManager for Datavyu */
    private static final PluginManager pluginManager = new PluginManager();

    /** Set of plugins */
    private Set<Plugin> plugins;

    /** Set of names of the plugins that we added */
    private Set<String> pluginNames;

    /** Mapping between plugin classifiers and plugins */
    private Multimap<String, Plugin> pluginClassifiers;

    /** The list of plugins associated with data viewer class name */
    private Map<String, Plugin> viewerClassToPlugin;

    /** Merge file filters for plugins of the same name */
    private Map<String, GroupFileFilter> filters;

    /**
     * Default constructor. Searches for valid plugins in the classpath by looking for classes that implement the
     * plugin interface.
     */
    private PluginManager() {
        plugins = Sets.newLinkedHashSet();
        pluginNames = Sets.newHashSet();
        viewerClassToPlugin = Maps.newHashMap();
        pluginClassifiers = HashMultimap.create();
        filters = Maps.newLinkedHashMap();
        initialize();
    }

    /**
     * Get the singleton instance of this PluginManager.
     *
     * @return The single instance of the PluginManager object in Datavyu.
     */
    public static PluginManager getInstance() {
        return pluginManager;
    }

    /**
     * Initializes the plugin manager by searching for valid plugins to insert into the manager.
     */
    private void initialize() {
        logger.info("Initializing the PluginManager");
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            URL resource = loader.getResource("");
            // Quaqua workaround
            if ((resource != null) && resource.toString().equals("file:/System/Library/Java/")) {
                resource = null;
            }
            if (resource != null && resource.getPath().endsWith("lib/")) {
                resource = null;
            }
            // The classloader references a jar - open the jar file up and iterate through all the entries and add
            // the entries that are concrete Plugins.
            if (resource == null) {
                logger.info("Loading plugins from jar");
                resource = loader.getResource("org/datavyu");
                if (resource == null) {
                    throw new ClassNotFoundException("Can't get class loader.");
                }
                String file = resource.getFile();
                file = file.substring(0, file.indexOf("!"));

                URI uri = new URI(file);
                File f = new File(uri);
                JarFile jar = new JarFile(f);

                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (name.endsWith(".class")) {
                        addPlugin(name);
                    }
                }
                // The classloader references a bunch of .class files on disk, recursively inspect contents of each
                // of the resources. If it is a directory add it to our workStack, otherwise check to see if it is a
                // concrete plugin.
            } else {
                logger.info("Loading plugins from resource bundle.");
                // If we are running from a test we need to look in more than one place for classes - add all these
                // places to the work stack.
                Enumeration<URL> resources = loader.getResources("");

                Stack<File> workStack = new Stack<>();
                Stack<String> pathNames = new Stack<>();

                while (resources.hasMoreElements()) {
                    workStack.clear();
                    File res = new File(resources.nextElement().getFile());

                    logger.info("Looking for plugins in: " + res.getAbsolutePath());

                    // Dirty hack for Quaqua.
                    if (res.equals(new File("/System/Library/Java"))) {
                        continue;
                    }
                    workStack.push(res);
                    pathNames.clear();
                    pathNames.push("");

                    while (!workStack.empty()) {
                        // We must handle spaces in the directory name
                        File f = workStack.pop();
                        String s = f.getCanonicalPath();
                        s = s.replaceAll("%20", " ");
                        File dir = new File(s);
                        String pathName = pathNames.pop();
                        // For each of the children of the directory - look for
                        // Plugins or more directories to recurse inside.
                        String[] files = dir.list();
                        for (int i = 0; i < files.length; i++) {
                            File file = new File(dir.getAbsolutePath() + "/" + files[i]);
                            logger.info("Inspecting file/directory: " + file.getAbsolutePath());
                            // If the file is a directory, add to work list.
                            if (file.isDirectory()) {
                                workStack.push(file);
                                pathNames.push(pathName + file.getName() + ".");
                                // If we are dealing with a class file - attempt to add it to our list of plugins
                            } else if (files[i].endsWith(".class")) {
                                addPlugin(pathName.concat(files[i]));
                                // If it's the datavyu jar get the contents
                            } else if (files[i].startsWith("datavyu") && files[i].endsWith(".jar")) {
                                JarFile jar = new JarFile(file);
                                // For each file in the jar file check to see if it could be a plugin.
                                Enumeration<JarEntry> entries = jar.entries();
                                while (entries.hasMoreElements()) {
                                    String name = entries.nextElement().getName();
                                    // Found a class file - attempt to add it as a plugin.
                                    if (name.endsWith(".class")) {
                                        addPlugin(name);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // We scanned the Datavyu classpath - but we should also look in the "plugins" directory for jar files that
            // correctly conform to the Datavyu plugin interface
            LocalStorage localStorage = Datavyu.getApplication().getContext().getLocalStorage();
            File pluginDir = new File(localStorage.getDirectory().toString() + "/plugins");

            // Unable to find plugin directory or any entries within the plugin directory. Don't bother attempting to
            // add more plugins to Datavyu
            if (pluginDir.list() == null) {
                logger.info("Unable to find the 'plugins' directory.");
                return;
            }

            // For each of the files in the plugin directory - check to see if they conform to the plugin interface.
            for (String file : pluginDir.list()) {
                File f = new File(pluginDir.getAbsolutePath() + "/" + file);
                if (file == null) {
                    throw new ClassNotFoundException("Null file");
                    // If the file is a jar file open it and look for plugins
                } else if (file.endsWith(".jar")) {
                    injectPlugin(f);
                    JarFile jar = new JarFile(f);
                    // For each file in the jar file check to see if it could be a plugin.
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        // Found a class file - attempt to add it as a plugin.
                        if (name.endsWith(".class")) {
                            addPlugin(name);
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            logger.error("Unable to build Plugin", e);
        } catch (IOException ie) {
            logger.error("Unable to load jar file", ie);
        } catch (URISyntaxException se) {
            logger.error("Unable to build path to jar file", se);
        }
    }

    /**
     * Injects A plugin into the classpath.
     *
     * @param file The jar file to inject into the classpath.
     *
     * @throws IOException If unable to inject the plugin into the class path.
     */
    private void injectPlugin(final File file) throws IOException {
        URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<?> sysclass = URLClassLoader.class;
        logger.info("Injecting plugin from file: " + file.getAbsolutePath());

        try {
            Class<?>[] parameters = new Class[]{URL.class};
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(sysLoader, new Object[]{file.toURL()});
        } catch (Throwable t) {
            logger.error("Unable to inject class into class path.", t);
        }
    }

    /**
     * Attempts to add an instance of the supplied class name as a plugin to the plugin manager. Will only add the
     * class if it implements the plugin interface.
     *
     * @param className The fully qualified class name to attempt to add to the list of plugins.
     */
    private void addPlugin(final String className) {
        try {
            String cName = className.replaceAll("\\.class$", "").replace('/','.');
            /*
             * Ignore classes that: - Belong to the UITests (traditionally this
             * was because of the UISpec4J interceptor, which interrupted the
             * UI. We still ignore UITest classes as these will not be plugins)
             * - Are part of GStreamer, or JUnitX (these cause issues and are
             * certainly not going to be plugins either)
             */
            if (!cName.contains("org.datavyu.uitests") && !cName.contains("org.gstreamer")
                    && !cName.contains("ch.randelshofer") && !cName.startsWith("junitx")) {

                logger.info("Loading " + cName);
                Class<?> testClass = Class.forName(cName);

                if (pluginClass.isAssignableFrom(testClass) &&
                        (testClass.getModifiers() & (Modifier.ABSTRACT | Modifier.INTERFACE)) == 0)
                {
                    Plugin plugin = (Plugin) testClass.newInstance();

                    if (!plugin.getValidPlatforms().contains(Datavyu.getPlatform())) {
                        // Not valid for this operating system
                        return;
                    }

                    if (Datavyu.getPlatform() == Datavyu.Platform.MAC) {
                        if (!plugin.getValidVersions().isInRange(MacOS.getOSVersion())) {
                            return;
                        }
                    }

                    String pluginName = plugin.getPluginName();

                    if (pluginNames.contains(plugin.getPluginName())) {

                        // We already have this plugin; stop processing it
                        return;
                    }

                    pluginNames.add(pluginName);

                    buildGroupFilter(plugin);

                    // Ensure we have at least one file filter
                    assert plugin.getFilters() != null;
                    assert plugin.getFilters().length > 0;
                    assert plugin.getFilters()[0] != null;

                    plugins.add(plugin);

                    // BugzID:2110
                    pluginClassifiers.put(plugin.getNamespace(), plugin);

                    final Class<? extends StreamViewer> cdv = plugin.getViewerClass();

                    if (cdv != null) {
                        viewerClassToPlugin.put(cdv.getName(), plugin);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            logger.error("Unable to find plugin. Error: ", e);
        } catch (ClassFormatError e) {
            logger.error("Plugin with bad class format. Error: ", e);
        } catch (Exception e) {
            logger.error("Unable to instantiate plugin. Error: ", e);
        }
    }

    private void buildGroupFilter(final Plugin p) {

        for (Filter filter : p.getFilters()) {
            GroupFileFilter groupFileFilter;

            if (filters.containsKey(filter.getName())) {
                groupFileFilter = filters.get(filter.getName());
            } else {
                groupFileFilter = new GroupFileFilter(filter.getName());
                filters.put(filter.getName(), groupFileFilter);
            }

            groupFileFilter.addFileFilter(filter);
        }
    }

    public Iterable<? extends FileFilter> getFileFilters() {
        return filters.values();
    }

    public Iterable<Plugin> getPlugins() {
        List<Plugin> p = Lists.newArrayList(plugins);
        if (Datavyu.getPlatform() == Datavyu.Platform.MAC) {
            p.sort(new Comparator<Plugin>() {
                @Override
                public int compare(final Plugin o1, final Plugin o2) {

                    if ("Native OSX Video".equals(o1.getPluginName())) {
                        return -1;
                    }

                    if ("Native OSX Video".equals(o2.getPluginName())) {
                        return 1;
                    }

                    if ("QTKit Video".equals(o1.getPluginName())) {
                        return -1;
                    }

                    if ("QTKit Video".equals(o2.getPluginName())) {
                        return 1;
                    }

                    return o1.getPluginName().compareTo(o2.getPluginName());
                }
            });

            for (int i = 0; i < p.size(); i++) {
                if (p.get(i).getPluginName().equals("QuickTime Video") || p.get(i).getPluginName().equals("VLC Video")) {
                    p.remove(i);
                    break;
                }
            }
        } else if (Datavyu.getPlatform() == Datavyu.Platform.WINDOWS) {
            p.sort(new Comparator<Plugin>() {
                @Override
                public int compare(final Plugin o1, final Plugin o2) {

                    if ("JavaFX Video".equals(o1.getPluginName())) {
                        return -1;
                    }

                    if ("JavaFX Video".equals(o2.getPluginName())) {
                        return 1;
                    }

                    return o1.getPluginName().compareTo(o2.getPluginName());
                }
            });
            for (int i = 0; i < p.size(); i++) {
                if (p.get(i).getPluginName().equals("QTKit Video")) {
                    p.remove(i);
                    break;
                }
            }
        } else {
            p.sort(new Comparator<Plugin>() {
                @Override
                public int compare(final Plugin o1, final Plugin o2) {

                    if ("JavaFX Video".equals(o1.getPluginName())) {
                        return -1;
                    }

                    if ("JavaFX Video".equals(o2.getPluginName())) {
                        return 1;
                    }

                    return o1.getPluginName().compareTo(o2.getPluginName());
                }
            });
        }

        return p;
    }

    /**
     * Searches for and returns a plugin compatible with the given classifier and data file
     *
     * @param classifier Plugin classifier string
     * @param file       The data file to open
     * @return The first compatible plugin that is found, null otherwise
     */
    public Plugin getCompatiblePlugin(final String classifier, final File file) {

        // Hard-code plugins for Windows, OSX, and Linux
        if (classifier.equals("datavyu.video")) {

            // Mac default is OSXPlugin
            if (Datavyu.getPlatform() == Datavyu.Platform.MAC) {
                return MacOS.getNativeOSXPlugin();
            }

            // Windows default is FFmpegPlugin
            if (Datavyu.getPlatform() == Datavyu.Platform.WINDOWS) {
                FFmpegPlugin fFmpegPlugin = new FFmpegPlugin();
                logger.info("Loading windows plugin: " + fFmpegPlugin.getPluginName());
                return fFmpegPlugin;
            }

            // Linux default is JFXPlugin
            if (Datavyu.getPlatform() == Datavyu.Platform.LINUX) {
                return new JfxPlugin();
            }
        }

        for (Plugin candidate : pluginClassifiers.get(classifier)) {

            for (Filter filter : candidate.getFilters()) {

                if (filter.getFileFilter().accept(file)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    /**
     * @param dataViewer The fully-qualified class name of the data viewer
     *                   implementation
     * @return The {@link Plugin} used to build the data viewer if it exists,
     * {@code null} otherwise.
     */
    public Plugin getAssociatedPlugin(final String dataViewer) {
        return viewerClassToPlugin.get(dataViewer);
    }

    /** Given short name, return associated plugin.
     * @param shortName Short name of plugin (e.g., ffmpeg, jfx, nativeosx.
     * @return plugin Associated plugin.
     */
    public Plugin getPluginFromShortName(String shortName){
        UUID id = UUID.nameUUIDFromBytes(("plugin." + shortName).getBytes());
        for(Plugin p : getPlugins()) {
            if(p.getPluginUUID().equals(id)) return p;
        }
        return null;
    }
}
