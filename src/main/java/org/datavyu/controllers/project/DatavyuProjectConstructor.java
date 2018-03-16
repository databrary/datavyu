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
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;

import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Used to construct a datavyu project from its YAML representation.
 */
public class DatavyuProjectConstructor extends Constructor {

    /**
     * Default Constructor.
     */
    public DatavyuProjectConstructor() {
        yamlMultiConstructors.put("!vs", new ConstructViewerSetting());
        yamlMultiConstructors.put("!project", new ConstructProject());
        yamlMultiConstructors.put("!ts", new ConstructTrackSettings());
    }

    /**
     * Used to construct the {@link Project} class.
     */
    private class ConstructProject extends AbstractConstruct {

        public Object construct(final Node node) {
            MappingNode mnode = (MappingNode) node;
            Map values = constructMapping(mnode);
            Project project = new Project();
            project.setProjectName((String) values.get("name"));
            project.setDatabaseFileName((String) values.get("dbFile"));

            final int projectVersion = (Integer) values.get("version");

            if (projectVersion <= 2) {
                Map vs = (Map) values.get("viewerSettings");
                vs.values();
                project.setViewerSettings(vs.values());
            }

            if (projectVersion >= 3) {
                project.setViewerSettings((List) values.get("viewerSettings"));
            }

            if ((3 <= projectVersion) && (projectVersion <= 4)) {
                project.setTrackSettings((List) values.get("trackSettings"));
            }

            if (projectVersion >= 4) {
                project.setOriginalProjectDirectory((String) values.get("origpath"));
            }

            return project;
        }
    }

    /**
     * Used to construct the {@link ViewerSetting} class.
     */
    private class ConstructViewerSetting extends AbstractConstruct {

        public Object construct(final Node node) {
            MappingNode mnode = (MappingNode) node;
            Map values = constructMapping(mnode);
            ViewerSetting vs = new ViewerSetting();
            vs.setFilePath((String) values.get("feed"));
//            vs.setPluginName((String) values.get("plugin"));
            try {
                vs.setPluginUUID(UUID.fromString((String) values.get("plugin"))); // Look for an UUID
            }catch (IllegalArgumentException e){
                vs.setPluginName((String) values.get("plugin")); //if not an UUID try regular plugin name
            }

            // WARNING: SnakeYAML refuses to parse this as a Long.
            // TODO: remove this
            String offset = (String) values.get("offset");

            if (offset != null) {
                vs.setOffset(Long.parseLong(offset));
            }

            // BugzID:1806
            Object versionObj = values.get("version");

            if (versionObj == null) {
                vs.setSettingsId(null);
            } else {
                int version = (Integer) versionObj;

                vs.setSettingsId((String) values.get("settingsId"));

                if (version >= 3) {
                    vs.setTrackSettings((TrackSettings) values.get(
                            "trackSettings"));
                    vs.setPluginClassifier((String) values.get("classifier"));
                }

            }

            return vs;
        }
    }

    /**
     * Used to construct the {@link TrackSettings} class.
     */
    private class ConstructTrackSettings extends AbstractConstruct {

        public Object construct(final Node node) {
            MappingNode mnode = (MappingNode) node;
            Map values = constructMapping(mnode);

            int version = 0;
            Object versionObj = values.get("version");

            if (versionObj != null) {
                version = (Integer) versionObj;
            }

            TrackSettings ts = new TrackSettings();

            if (version == 1) {
                ts.setFilePath((String) values.get("feed"));
            }

            ts.setLocked((Boolean) values.get("locked"));

            if (values.containsKey("bookmark")) {

                // DEPRECATED - only included for backwards compatibility
                ts.addMarkerPosition(Long.parseLong(
                        (String) values.get("bookmark")));
            }

            if (values.containsKey("bookmarks")) {
                List bookmarks = (List) values.get("bookmarks");

                for (Object time : bookmarks) {

                    // Ugly; the deserializer chooses the smallest data
                    // type which can represent the value.
                    if (time instanceof Integer) {
                        ts.addMarkerPosition((Integer) time);
                    } else if (time instanceof Long) {
                        ts.addMarkerPosition((Long) time);
                    }
                }
            }

            return ts;
        }
    }

}
