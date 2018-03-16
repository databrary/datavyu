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
package org.datavyu.controllers.project;

import org.datavyu.models.project.Project;
import org.datavyu.models.project.TrackSettings;
import org.datavyu.models.project.ViewerSetting;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Used to create a custom YAML representation of a Datavyu project.
 */
public class DatavyuProjectRepresenter extends Representer {

    /**
     * Default Constructor.
     */
    public DatavyuProjectRepresenter() {
        representers.put(Project.class, new RepresentProject());
        representers.put(ViewerSetting.class, new RepresentViewerSetting());
        representers.put(TrackSettings.class, new RepresentTrackSettings());
    }

    /**
     * Used to represent the {@link Project} class.
     */
    private class RepresentProject implements Represent {
        public Node representData(final Object obj) {
            Project project = (Project) obj;
            Map<String, Object> map = new TreeMap<>();
            map.put("version", Project.VERSION);
            map.put("name", project.getProjectName());
            map.put("origpath", project.getOriginalProjectDirectory());
            map.put("dbFile", project.getDatabaseFileName());
            map.put("viewerSettings", project.getViewerSettings());

            return representMapping(new Tag("!project"), map, Boolean.FALSE);
        }
    }

    /**
     * Used to represent the {@link ViewerSetting} class.
     */
    private class RepresentViewerSetting implements Represent {
        public Node representData(final Object obj) {
            ViewerSetting viewerSetting = (ViewerSetting) obj;
            Map<String, Object> map = new TreeMap<String, Object>();
            map.put("feed", viewerSetting.getFilePath());
//            map.put("plugin", viewerSetting.0());
            map.put("plugin", viewerSetting.getPluginUUID().toString());

            // BugzID:2108
            map.put("classifier", viewerSetting.getPluginClassifier());

            // BugzID:1806
            map.put("settingsId", viewerSetting.getSettingsId());
            map.put("version", ViewerSetting.VERSION);

            // BugzID:2107
            map.put("trackSettings", viewerSetting.getTrackSettings());

            return representMapping(new Tag("!vs"), map, Boolean.FALSE);
        }
    }

    /**
     * Used to represent the {@link TrackSettings} class.
     */
    private class RepresentTrackSettings implements Represent {
        public Node representData(final Object obj) {
            TrackSettings interfaceSettings = (TrackSettings) obj;
            Map<String, Object> map = new TreeMap<String, Object>();
            map.put("locked", interfaceSettings.isLocked());

            final List<Long> bookmarks =
                    interfaceSettings.getMarkers();

            if (bookmarks.isEmpty()) {

                // DEPRECATED - only included for backwards compatibility
                map.put("bookmark", "-1");
                map.put("bookmarks", bookmarks);
            } else {

                // DEPRECATED - only included for backwards compatibility
                map.put("bookmark", Long.toString(bookmarks.get(0)));

                map.put("bookmarks", bookmarks);
            }

            map.put("version", TrackSettings.VERSION);

            return representMapping(new Tag("!ts"), map, Boolean.FALSE);
        }
    }

}
