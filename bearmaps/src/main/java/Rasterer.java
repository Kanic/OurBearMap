
/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    /** The max image depth level. */
    public static final int MAX_DEPTH = 7;

    /**
     * Takes a user query and finds the grid of images that best matches the query. These images
     * will be combined into one big image (rastered) by the front end. The grid of images must obey
     * the following properties, where image in the grid is referred to as a "tile".
     * <ul>
     *     <li>The tiles collected must cover the most longitudinal distance per pixel (LonDPP)
     *     possible, while still covering less than or equal to the amount of longitudinal distance
     *     per pixel in the query box for the user viewport size.</li>
     *     <li>Contains all tiles that intersect the query bounding box that fulfill the above
     *     condition.</li>
     *     <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     * </ul>
     * @param params The RasterRequestParams containing coordinates of the query box and the browser
     *               viewport width and height.
     * @return A valid RasterResultParams containing the computed results.
     */
    public RasterResultParams getMapRaster(RasterRequestParams params) {
        System.out.println(
                "Since you haven't implemented getMapRaster, nothing is displayed in the browser.");

        double londpp = lonDPP(params.lrlon, params.ullon, params.w);
        int d;
        for (d = 0; d < 8; d += 1) {
            double lon = MapServer.ROOT_LONDPP / Math.pow(2, d);
            if (lon <= londpp) {
                break;
            }
        }
        if (d == 8) {
            d -= 1;
        }
        double londelta = MapServer.ROOT_LON_DELTA / Math.pow(2, d);
        double latdelta = MapServer.ROOT_LAT_DELTA / Math.pow(2, d);
        int left, up, right, down;
        left = (int) ((params.ullon - MapServer.ROOT_ULLON) / londelta);
        right = (int) ((params.lrlon - MapServer.ROOT_ULLON) / londelta);
        up = (int) ((MapServer.ROOT_ULLAT - params.ullat) / latdelta);
        down = (int) ((MapServer.ROOT_ULLAT - params.lrlat) / latdelta);
        String [][] renderGrid = new String [down - up + 1][right - left + 1];
        for (int i = 0; i <= down - up; i += 1) {
            for (int j = 0; j <= right - left; j += 1) {
                renderGrid[i][j] = "d" + d + "_x" + (j + left) + "_y" + (i + up) + ".png";
            }
        }
        double rasterullon = MapServer.ROOT_ULLON + left * londelta;
        double rasterlrlon = MapServer.ROOT_ULLON + (right + 1) * londelta;
        double rasterullat = MapServer.ROOT_ULLAT - up * latdelta;
        double rasterlrlat = MapServer.ROOT_ULLAT - (down + 1) * latdelta;
        RasterResultParams resultParams = new RasterResultParams.Builder()
                      .setRenderGrid(renderGrid)
                .setRasterUlLon(rasterullon)
                .setRasterUlLat(rasterullat)
                .setRasterLrLon(rasterlrlon)
                .setRasterLrLat(rasterlrlat)
                .setDepth(d)
                .setQuerySuccess(true)
                .create();
        if (rasterlrlat > rasterullat || rasterlrlon < rasterullon) {
            return RasterResultParams.queryFailed();
        } else if (rasterlrlat < MapServer.ROOT_LRLAT || rasterlrlon > MapServer.ROOT_LRLON
                || rasterullat > MapServer.ROOT_ULLAT || rasterullon < MapServer.ROOT_ULLON) {
            return RasterResultParams.queryFailed();
        } else {
            return resultParams;
        }
    }


    /**
     * Calculates the lonDPP of an image or query box
     * @param lrlon Lower right longitudinal value of the image or query box
     * @param ullon Upper left longitudinal value of the image or query box
     * @param width Width of the query box or image
     * @return lonDPP
     */
    private double lonDPP(double lrlon, double ullon, double width) {
        return (lrlon - ullon) / width;
    }
}
