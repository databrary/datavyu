package org.datavyu.util;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.text.Collator;
import java.util.*;


/**
 * A TreeModel implementation for a disk directory structure.
 * <p/>
 * Typical usage:
 * <p/>
 * <pre>
 *   FileSystemModel model = new FileSystemModel (new File ("/"));
 *   JTree tree = new JTree (model);
 *   </pre>
 *
 * @author <a href="mailto:info@geosoft.no">GeoSoft</a>
 */
public class FileSystemTreeModel implements TreeModel {
    private Collection listeners_;
    private FileTreeNode root_;
    private HashMap sortedChildren_; // File -> List<File>
    private HashMap lastModified_;


    /**
     * Create a tree model using the specified file as root.
     *
     * @param root Root file (directory typically).
     */
    public FileSystemTreeModel(File root) {
        root_ = new FileTreeNode(root);

        listeners_ = new ArrayList();

        sortedChildren_ = new HashMap();
        lastModified_ = new HashMap();
    }


    public Object getRoot() {
        return root_;
    }


    public Object getChild(Object parent, int index) {
        List children = (List) sortedChildren_.get(parent);
        return children == null ? null : children.get(index);
    }


    public int getChildCount(Object parent) {
        File file = (File) parent;
        if (!file.isDirectory()) {
            return 0;
        }

        File[] children = file.listFiles();
        int nChildren = children == null ? 0 : children.length;

        long lastModified = file.lastModified();

        boolean isFirstTime = lastModified_.get(file) == null;
        boolean isChanged = false;

        if (!isFirstTime) {
            Long modified = (Long) lastModified_.get(file);
            long diff = Math.abs(modified.longValue() - lastModified);
            isChanged = diff > 4000; // MS/Win or Samba HACK. Check this!
        }

        int opfCount = 0;
        // Sort and register children info
        if (isFirstTime || isChanged) {
            lastModified_.put(file, new Long(lastModified));

            TreeSet sorted = new TreeSet();
            for (int i = 0; i < nChildren; i++) {
                if (children[i].getName().toLowerCase().endsWith(".opf") || children[i].getName().toLowerCase().endsWith(".rb") || children[i].isDirectory()) {
                    sorted.add(new FileTreeNode(children[i]));
                    opfCount++;
                }
            }

            sortedChildren_.put(file, new ArrayList(sorted));
        }

        // Notify listeners (visual tree typically) if changes
        if (isChanged) {
            TreeModelEvent event = new TreeModelEvent(this, getTreePath(file));
            fireTreeStructureChanged(event);
        }

        return opfCount;
    }

    private Object[] getTreePath(File file) {
        List path = new ArrayList();
        while (!file.equals(root_)) {
            path.add(file);
            file = file.getParentFile();
        }
        path.add(root_);

        int nElements = path.size();

        Object[] treePath = new Object[nElements];
        for (int i = 0; i < nElements; i++)
            treePath[i] = path.get(nElements - i - 1);

        return treePath;
    }


    public boolean isLeaf(Object node) {
        return ((File) node).isFile();
    }


    public void valueForPathChanged(TreePath path, Object newValue) {
    }


    public int getIndexOfChild(Object parent, Object child) {
        List children = (List) sortedChildren_.get(parent);
        return children.indexOf(child);
    }


    public void addTreeModelListener(TreeModelListener listener) {
        if (listener != null && !listeners_.contains(listener))
            listeners_.add(listener);
    }


    public void removeTreeModelListener(TreeModelListener listener) {
        if (listener != null)
            listeners_.remove(listener);
    }


    public void fireTreeNodesChanged(TreeModelEvent event) {
        for (Iterator i = listeners_.iterator(); i.hasNext(); ) {
            TreeModelListener listener = (TreeModelListener) i.next();
            listener.treeNodesChanged(event);
        }
    }


    public void fireTreeNodesInserted(TreeModelEvent event) {
        for (Iterator i = listeners_.iterator(); i.hasNext(); ) {
            TreeModelListener listener = (TreeModelListener) i.next();
            listener.treeNodesInserted(event);
        }
    }


    public void fireTreeNodesRemoved(TreeModelEvent event) {
        for (Iterator i = listeners_.iterator(); i.hasNext(); ) {
            TreeModelListener listener = (TreeModelListener) i.next();
            listener.treeNodesRemoved(event);
        }
    }


    public void fireTreeStructureChanged(TreeModelEvent event) {
        for (Iterator i = listeners_.iterator(); i.hasNext(); ) {
            TreeModelListener listener = (TreeModelListener) i.next();
            listener.treeStructureChanged(event);
        }
    }


    /**
     * Extension to the java.io.File object but with more
     * appropriate compare rules
     *
     * @author <a href="mailto:info@geosoft.no">GeoSoft</a>
     */
    private class FileTreeNode extends File
            implements Comparable<File> {
        public FileTreeNode(File file) {
            super(file, "");
        }


        /**
         * Compare two FileTreeNode objects so that directories
         * are sorted first.
         *
         * @param object Object to compare to.
         * @return Compare identifier.
         */
        public int compareTo(File object) {
            File file1 = this;
            File file2 = object;

            Collator collator = Collator.getInstance();

            if (file1.isDirectory() && file2.isFile())
                return -1;
            else if (file1.isFile() && file2.isDirectory())
                return +1;
            else
                return collator.compare(file1.getName(), file2.getName());
        }


        /**
         * Return a string representation of this node.
         * The inherited toString() method returns the entire path.
         * For use in a tree structure, the name is more appropriate.
         *
         * @return String representation of this node.
         */
        public String toString() {
            return getName();
        }
    }


}
