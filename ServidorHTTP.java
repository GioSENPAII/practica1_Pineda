import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ServidorHTTP {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java ServidorHTTP <puerto>");
            return;
        }

        int puerto = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor HTTP escuchando en el puerto " + puerto);

            while (true) {
                Socket socketCliente = serverSocket.accept();
                System.out.println("Nueva conexión desde " + socketCliente.getInetAddress());

                new Thread(() -> manejarSolicitud(socketCliente)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void manejarSolicitud(Socket socketCliente) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
             OutputStream out = socketCliente.getOutputStream()) {

            String linea;
            String ifModifiedSince = null;
            String solicitud = null;

            while ((linea = in.readLine()) != null && !linea.isEmpty()) {
                System.out.println(linea);
                if (linea.startsWith("If-Modified-Since:")) {
                    ifModifiedSince = linea.substring("If-Modified-Since:".length()).trim();
                }
                if (solicitud == null) {
                    solicitud = linea;
                }
            }

            if (solicitud == null) {
                socketCliente.close();
                return;
            }

            String[] partesSolicitud = solicitud.split(" ");
            if (partesSolicitud.length < 2) {
                socketCliente.close();
                return;
            }
            String ruta = partesSolicitud[1];

            if (ruta.startsWith("/suma")) {
                int resultado = calcularSuma(ruta);
                String respuesta = "HTTP/1.1 200 OK\r\n" +
                                   "Content-Type: text/plain\r\n" +
                                   "Content-Length: " + String.valueOf(resultado).length() + "\r\n" +
                                   "Connection: close\r\n\r\n" +
                                   resultado;

                out.write(respuesta.getBytes());
                out.flush();
                socketCliente.close();
                System.out.println("[Respuesta] " + resultado);
                return;
            }

            // Página HTML
            String contenido = "<html>" +
                    "<head>" +
                    "<script>" +
                    "function get(req, callback) {" +
                    "  const xhr = new XMLHttpRequest();" +
                    "  xhr.open('GET', req, true);" +
                    "  xhr.onload = function() {" +
                    "    if (callback != null) callback(xhr.status, xhr.responseText);" +
                    "  };" +
                    "  xhr.onerror = function() {" +
                    "    alert('Error en la solicitud');" +
                    "  };" +
                    "  xhr.send();" +
                    "}" +
                    "</script>" +
                    "</head>" +
                    "<body>" +
                    "<button onclick=\"get('/suma?a=1&b=2&c=3', function(status, response) { alert(status + ' ' + response); })\">" +
                    "Aceptar" +
                    "</button>" +
                    "</body>" +
                    "</html>";

            String respuesta = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: " + contenido.length() + "\r\n" +
                    "Connection: close\r\n\r\n" +
                    contenido;

            out.write(respuesta.getBytes());
            out.flush();
            socketCliente.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int calcularSuma(String ruta) {
        int a = 0, b = 0, c = 0;
        try {
            String[] partes = ruta.split("\\?");
            if (partes.length > 1) {
                String[] parametros = partes[1].split("&");
                for (String param : parametros) {
                    String[] kv = param.split("=");
                    if (kv.length == 2) {
                        int valor = Integer.parseInt(kv[1]);
                        if (kv[0].equals("a")) a = valor;
                        if (kv[0].equals("b")) b = valor;
                        if (kv[0].equals("c")) c = valor;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return a + b + c;
    }
}
