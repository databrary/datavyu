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
package org.datavyu.views;

import com.google.common.collect.Lists;
import org.datavyu.plugins.Plugin;

import javax.swing.*;
import java.util.List;


// TODO: What does JFC stand for?  Explain or write it out.
// Java File Chooser?
public abstract class BaseJFC extends PluginChooser {
    protected JComboBox pluginsBox;
    protected List<PluginCallback> plugins = Lists.newArrayList();

    @Override
    public void addPlugin(final Plugin plugin) {
        PluginCallback pc = new PluginCallback(plugin);
        plugins.add(pc);
    }

    @Override
    public void addPlugin(final Iterable<Plugin> plugins) {

        for (Plugin plugin : plugins) {
            PluginCallback pc = new PluginCallback(plugin);
            this.plugins.add(pc);
        }
    }

    @Override
    public Plugin getSelectedPlugin() {
        Object selected = pluginsBox.getSelectedItem();

        return ((PluginCallback) selected).plugin;
    }

    private static final class PluginCallback {
        final Plugin plugin;

        PluginCallback(final Plugin plugin) {
            this.plugin = plugin;
        }

        public String toString() {
            return plugin.getPluginName();
        }
    }
}
