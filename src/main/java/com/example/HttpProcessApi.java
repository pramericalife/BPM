package com.example;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpProcessApi {

    private final ProcessStarter starter;

    public HttpProcessApi(ProcessStarter starter) {
        this.starter = starter;
    }

    public void startServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        // POST /start-process
        server.createContext("/start-process", new StartProcessHandler());
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        System.out.println("Server started on http://localhost:" + port);
    }

    private class StartProcessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String body;
            try (InputStream inputStream = exchange.getRequestBody();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                body = reader.lines().collect(Collectors.joining("\n"));
            }

            Map<String, Object> params = new HashMap<>();
            String processId = null;
            if (body != null && !body.isEmpty()) {
                body = body.trim();
                if (body.startsWith("{") && body.endsWith("}")) {
                    String inner = body.substring(1, body.length() - 1);
                    String[] parts = inner.split(",");
                    for (String p : parts) {
                        String[] kv = p.split(":", 2);
                        if (kv.length == 2) {
                            String key = kv[0].trim().replaceAll("^\"|\"$", "");
                            String val = kv[1].trim().replaceAll("^\"|\"$", "");
                            if ("processId".equals(key)) {
                                processId = val;
                            } else {
                                params.put(key, val);
                            }
                        }
                    }
                }
            }

            if (processId == null || processId.isEmpty()) {
                sendResponse(exchange, 400, "Missing processId in request body");
                return;
            }

            try {
                long piId = starter.startProcess(processId, params);
                String resp = "{\"status\":\"started\",\"processInstanceId\":" + piId + "}";
                sendResponse(exchange, 200, resp);
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }

        private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\"", "\\\"");
        }
    }

    public static void main(String[] args) throws Exception {
        String ksessionName = "ksession-processes"; 
        ProcessStarter starter = new ProcessStarter(ksessionName);
        HttpProcessApi api = new HttpProcessApi(starter);
        api.startServer(8080);
    }
}
