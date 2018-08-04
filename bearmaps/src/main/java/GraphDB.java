import org.xml.sax.SAXException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Kevin Lowe, Antares Chen, Kevin Lin
 */
public class GraphDB {

    static class Node {
        long id;
        String name;
        double lon;
        double lat;
        double x;
        double y;
        Node(long id, double lon, double lat) {
            this.id = id;
            this.lon = lon;
            this.lat = lat;
            this.x = projectToX(lon, lat);
            this.y = projectToY(lon, lat);
        }

        public void setName(String name) {
            this.name = name;
        }
        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }
    }
    static class Edge {
        long id;
        ArrayList<Long> nodes;
        Boolean valid;
        String name;

        Edge(long id) {
            this.id = id;
            this.nodes = new ArrayList<>();
            this.valid = false;
        }

        public void addVertices(long v) {
            nodes.add(v);
        }

        public void setValid(Boolean valid) {
            this.valid = valid;
        }
        public void setName(String name) {
            this.name = name;
        }
    }
    HashMap<Long, Node> nodes = new HashMap();
    HashMap<Long, Edge> edges = new HashMap<>();
    HashMap<Long, HashSet<Long>> neighbors = new HashMap<>();
    KdTree root;

    public void addNode(Node node) {
        nodes.put(node.id, node);
    }
    public void addEdge(Long id, Edge edge) {
        edges.put(id, edge);
    }
    public void addNeighbors(Long v, Long w) {
        if (!neighbors.containsKey(v) || neighbors.get(v) == null) {
            neighbors.put(v, new HashSet<>());
        }
        if (!neighbors.containsKey(w) || neighbors.get(w) == null) {
            neighbors.put(w, new HashSet<>());
        }
        neighbors.get(v).add(w);
        neighbors.get(w).add(v);
    }
    /**
     * This constructor creates and starts an XML parser, cleans the nodes, and prepares the
     * data structures for processing. Modify this constructor to initialize your data structures.
     * @param dbPath Path to the XML file to be parsed.
     */
    public GraphDB(String dbPath) {
        File inputFile = new File(dbPath);
        try (FileInputStream inputStream = new FileInputStream(inputFile)) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputStream, new GraphBuildingHandler(this));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
        createKdTree();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    private static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     * Remove nodes with no connections from the graph.
     * While this does not guarantee that any two nodes in the remaining graph are connected,
     * we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        HashMap<Long, Node> tmp = (HashMap) nodes.clone();
        for (Long v: tmp.keySet()) {
            if (!neighbors.containsKey(v) || neighbors.get(v) == null) {
                neighbors.remove(v);
                nodes.remove(v);
            }
        }
    }

    /**
     * Returns the longitude of vertex <code>v</code>.
     * @param v The ID of a vertex in the graph.
     * @return The longitude of that vertex, or 0.0 if the vertex is not in the graph.
     */
    double lon(long v) {
        if (!nodes.containsKey(v)) {
            return 0.0;
        }
        return nodes.get(v).getLon();
    }

    /**
     * Returns the latitude of vertex <code>v</code>.
     * @param v The ID of a vertex in the graph.
     * @return The latitude of that vertex, or 0.0 if the vertex is not in the graph.
     */
    double lat(long v) {
        if (!nodes.containsKey(v)) {
            return 0.0;
        }
        return nodes.get(v).getLat();
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of all vertex IDs in the graph.
     */
    Iterable<Long> vertices() {
        return nodes.keySet();
    }

    /**
     * Returns an iterable over the IDs of all vertices adjacent to <code>v</code>.
     * @param v The ID for any vertex in the graph.
     * @return An iterable over the IDs of all vertices adjacent to <code>v</code>, or an empty
     * iterable if the vertex is not in the graph.
     */
    Iterable<Long> adjacent(long v) {
        if (!neighbors.containsKey(v)) {
            return Collections.emptySet();
        }
        return neighbors.get(v);
    }

    /**
     * Returns the great-circle distance between two vertices, v and w, in miles.
     * Assumes the lon/lat methods are implemented properly.
     * @param v The ID for the first vertex.
     * @param w The ID for the second vertex.
     * @return The great-circle distance between vertices and w.
     * @source https://www.movable-type.co.uk/scripts/latlong.html
     */
    public double distance(long v, long w) {
        double phi1 = Math.toRadians(lat(v));
        double phi2 = Math.toRadians(lat(w));
        double dphi = Math.toRadians(lat(w) - lat(v));
        double dlambda = Math.toRadians(lon(w) - lon(v));

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    class KdTree {
        long median;
        KdTree left;
        KdTree right;
        boolean axis;
        double x;
        double y;
        KdTree(long median, KdTree left, KdTree right, boolean axis, double x, double y) {
            this.median = median;
            this.left = left;
            this.right = right;
            this.axis = axis;
            this.x = x;
            this.y = y;
        }
    }
    public KdTree kdTreeHelper(List<Long> lst, boolean axis) {
        if (lst.size() == 0) {
            return null;
        }
        if (axis) {
            lst.sort((v, w) -> (Double.compare(nodes.get(v).x,
                    nodes.get(w).x)));
        } else {
            lst.sort((v, w) -> (Double.compare(nodes.get(v).y,
                    nodes.get(w).y)));
        }
        int mid = lst.size() / 2;
        KdTree left =  kdTreeHelper(lst.subList(0, mid), !axis);
        KdTree right = kdTreeHelper(lst.subList(mid + 1, lst.size()), !axis);
        return new KdTree(lst.get(mid), left, right, axis,
                nodes.get(lst.get(mid)).x, nodes.get(lst.get(mid)).y);
    }
    public void createKdTree() {
        List<Long> lst = new ArrayList<>(nodes.keySet());
        root = kdTreeHelper(lst, true);
    }
    class Pair {
        KdTree currentbest;
        double bestdist;
        Pair(KdTree currentbest, double bestdist) {
            this.currentbest = currentbest;
            this.bestdist = bestdist;
        }
    }
    public Pair closestHelper(KdTree t, double x, double y,
                              KdTree currentbest, double bestdist) {
        if (t == null) {
            return new Pair(currentbest, bestdist);
        }
        double distance = euclidean(t.x, x, t.y, y);
        double diff;
        if (t.axis) {
            diff = t.x - x;
        } else {
            diff = t.y - y;
        }
        double abs = Math.abs(diff);
        if (distance < bestdist || bestdist == -1) {
            currentbest = t;
            bestdist = distance;
        }
        if (diff < 0) {
            Pair right = closestHelper(t.right, x, y, currentbest, bestdist);
            if (bestdist > abs) {
                return closestHelper(t.left, x, y, right.currentbest, right.bestdist);
            }
            return right;
        } else {
            Pair left = closestHelper(t.left, x, y, currentbest, bestdist);
            if (bestdist > abs) {
                return closestHelper(t.right, x, y, left.currentbest, left.bestdist);
            }
            return left;
        }
    }

    /**
     * Returns the ID of the vertex closest to the given longitude and latitude.
     * @param lon The given longitude.
     * @param lat The given latitude.
     * @return The ID for the vertex closest to the <code>lon</code> and <code>lat</code>.
     */
    public long closest(double lon, double lat) {
        double x = projectToX(lon, lat);
        double y = projectToY(lon, lat);
        return closestHelper(root, x, y, null, -1).currentbest.median;

    }
    static double euclidean(double x1, double x2, double y1, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    /**
     * Return the Euclidean x-value for some point, p, in Berkeley. Found by computing the
     * Transverse Mercator projection centered at Berkeley.
     * @param lon The longitude for p.
     * @param lat The latitude for p.
     * @return The flattened, Euclidean x-value for p.
     * @source https://en.wikipedia.org/wiki/Transverse_Mercator_projection
     */
    static double projectToX(double lon, double lat) {
        double dlon = Math.toRadians(lon - ROOT_LON);
        double phi = Math.toRadians(lat);
        double b = Math.sin(dlon) * Math.cos(phi);
        return (K0 / 2) * Math.log((1 + b) / (1 - b));
    }

    /**
     * Return the Euclidean y-value for some point, p, in Berkeley. Found by computing the
     * Transverse Mercator projection centered at Berkeley.
     * @param lon The longitude for p.
     * @param lat The latitude for p.
     * @return The flattened, Euclidean y-value for p.
     * @source https://en.wikipedia.org/wiki/Transverse_Mercator_projection
     */
    static double projectToY(double lon, double lat) {
        double dlon = Math.toRadians(lon - ROOT_LON);
        double phi = Math.toRadians(lat);
        double con = Math.atan(Math.tan(phi) / Math.cos(dlon));
        return K0 * (con - Math.toRadians(ROOT_LAT));
    }

    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public List<String> getLocationsByPrefix(String prefix) {
        return Collections.emptyList();
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     * @param locationName A full name of a location searched for.
     * @return A <code>List</code> of <code>LocationParams</code> whose cleaned name matches the
     * cleaned <code>locationName</code>
     */
    public List<LocationParams> getLocations(String locationName) {
        return Collections.emptyList();
    }

    /**
     * Returns the initial bearing between vertices <code>v</code> and <code>w</code> in degrees.
     * The initial bearing is the angle that, if followed in a straight line along a great-circle
     * arc from the starting point, would take you to the end point.
     * Assumes the lon/lat methods are implemented properly.
     * @param v The ID for the first vertex.
     * @param w The ID for the second vertex.
     * @return The bearing between <code>v</code> and <code>w</code> in degrees.
     * @source https://www.movable-type.co.uk/scripts/latlong.html
     */
    double bearing(long v, long w) {
        double phi1 = Math.toRadians(lat(v));
        double phi2 = Math.toRadians(lat(w));
        double lambda1 = Math.toRadians(lon(v));
        double lambda2 = Math.toRadians(lon(w));

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /** Radius of the Earth in miles. */
    private static final int R = 3963;
    /** Latitude centered on Berkeley. */
    private static final double ROOT_LAT = (MapServer.ROOT_ULLAT + MapServer.ROOT_LRLAT) / 2;
    /** Longitude centered on Berkeley. */
    private static final double ROOT_LON = (MapServer.ROOT_ULLON + MapServer.ROOT_LRLON) / 2;
    /**
     * Scale factor at the natural origin, Berkeley. Prefer to use 1 instead of 0.9996 as in UTM.
     * @source https://gis.stackexchange.com/a/7298
     */
    private static final double K0 = 1.0;
}
