package AndromeDraick.menuInteractivo.webmap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class WebMapServer {
    private final Logger log;
    private final WebMapConfig cfg;
    private HttpServer server;

    public WebMapServer(Logger log, WebMapConfig cfg) {
        this.log = log;
        this.cfg = cfg;
    }

    public void start() throws IOException {
        if (server != null) return;
        server = HttpServer.create(new InetSocketAddress(cfg.bind, cfg.puerto), 50);

        if (cfg.enableIndex) {
            server.createContext("/", new StaticHandler(cfg.rutaWeb, "index.html", log));
        }

        // Tiles: /tiles/{world}/{z}/{x}/{y}.png
        server.createContext("/tiles", new TileHandler(cfg.rutaTiles, log));

        // Archivos estáticos extra: /web/...
        server.createContext("/web", new StaticHandler(cfg.rutaWeb, null, log));

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        log.info("[WebMap] Servidor iniciado en http://" + cfg.bind + ":" + cfg.puerto + "/");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            log.info("[WebMap] Servidor detenido.");
        }
    }

    // ========== Handlers ==========
    static class StaticHandler implements HttpHandler {
        private final String root;
        private final String indexFile; // puede ser null
        private final Logger log;

        StaticHandler(String root, String indexFile, Logger log) {
            this.root = root;
            this.indexFile = indexFile;
            this.log = log;
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            String uri = ex.getRequestURI().getPath();
            String rel = uri.replaceFirst("^/(web/)?", "");
            if (rel.isEmpty() && indexFile != null) rel = indexFile;

            File f = new File(root, rel);
            if (f.isDirectory() && indexFile != null) {
                f = new File(f, indexFile);
            }

            if (!f.getCanonicalPath().startsWith(new File(root).getCanonicalPath())) {
                send404(ex); // path traversal
                return;
            }

            if (!f.exists() || f.isDirectory()) {
                send404(ex);
                return;
            }

            String mime = URLConnection.guessContentTypeFromName(f.getName());
            if (mime == null) mime = "application/octet-stream";
            ex.getResponseHeaders().set("Content-Type", mime);
            ex.sendResponseHeaders(200, f.length());
            try (OutputStream os = ex.getResponseBody(); InputStream in = new FileInputStream(f)) {
                in.transferTo(os);
            }
        }

        private void send404(HttpExchange ex) throws IOException {
            byte[] b = "404".getBytes();
            ex.sendResponseHeaders(404, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        }
    }

    static class TileHandler implements HttpHandler {
        private final String tilesRoot;
        private final Logger log;

        TileHandler(String tilesRoot, Logger log) {
            this.tilesRoot = tilesRoot;
            this.log = log;
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            // /tiles/{world}/{z}/{x}/{y}.png
            String path = ex.getRequestURI().getPath();
            String rel = path.replaceFirst("^/tiles/", "");
            Path f = Path.of(tilesRoot, rel);
            File file = f.toFile();

            if (!file.getCanonicalPath().startsWith(new File(tilesRoot).getCanonicalPath())) {
                send404(ex);
                return;
            }

            if (!file.exists() || file.isDirectory()) {
                // Devuelve un PNG transparente 256x256 si no existe (para no romper el visor)
                byte[] transparentPng = EmptyPngCache.getEmpty256();
                ex.getResponseHeaders().set("Content-Type", "image/png");
                ex.sendResponseHeaders(200, transparentPng.length);
                ex.getResponseBody().write(transparentPng);
                ex.close();
                return;
            }

            ex.getResponseHeaders().set("Content-Type", "image/png");
            ex.sendResponseHeaders(200, file.length());
            try (OutputStream os = ex.getResponseBody()) {
                Files.copy(f, os);
            }
        }

        private void send404(HttpExchange ex) throws IOException {
            byte[] b = "404".getBytes();
            ex.sendResponseHeaders(404, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        }
    }

    // Simple cache para PNG vacío 256x256
    static class EmptyPngCache {
        private static byte[] empty;

        static byte[] getEmpty256() throws IOException {
            if (empty != null) return empty;
            java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(256, 256, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                javax.imageio.ImageIO.write(bi, "png", baos);
                empty = baos.toByteArray();
                return empty;
            }
        }
    }
}
