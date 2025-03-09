import java.io.*;
import java.net.*;

public class AdministradorTrafico {
    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("Uso: java AdministradorTrafico <puerto_proxy> <ip_servidor1> <puerto_servidor1> <ip_servidor2> <puerto_servidor2>");
            return;
        }

        int puertoProxy = Integer.parseInt(args[0]);
        String ipServidor1 = args[1];
        int puertoServidor1 = Integer.parseInt(args[2]);
        String ipServidor2 = args[3];
        int puertoServidor2 = Integer.parseInt(args[4]);

        try (ServerSocket serverSocket = new ServerSocket(puertoProxy)) {
            System.out.println("[Proxy] Escuchando en el puerto " + puertoProxy);

            while (true) {
                Socket socketCliente = serverSocket.accept();
                String clienteIP = socketCliente.getInetAddress().getHostAddress();
                System.out.println("\n[Nuevo Cliente] Conexión aceptada desde " + clienteIP);

                new Thread(() -> manejarSolicitud(socketCliente, ipServidor1, puertoServidor1, ipServidor2, puertoServidor2)).start();
            }
        } catch (IOException e) {
            System.err.println("[Error] No se pudo iniciar el proxy: " + e.getMessage());
        }
    }

    private static void manejarSolicitud(Socket socketCliente, String ipServidor1, int puertoServidor1, String ipServidor2, int puertoServidor2) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
             OutputStream out = socketCliente.getOutputStream()) {

            String solicitud = in.readLine();
            if (solicitud == null) {
                System.out.println("[Advertencia] Solicitud vacía desde " + socketCliente.getInetAddress());
                return;
            }

            System.out.println("[Solicitud] " + solicitud);
            String[] partesSolicitud = solicitud.split(" ");
            if (partesSolicitud.length < 2) {
                System.out.println("[Advertencia] Solicitud mal formada: " + solicitud);
                return;
            }
            String ruta = partesSolicitud[1];

            if (ruta.equals("/favicon.ico") || ruta.equals("/robots.txt")) {
                System.out.println("[Info] Ignorando solicitud a " + ruta);
                String respuesta = "HTTP/1.1 404 Not Found\r\n\r\n";
                out.write(respuesta.getBytes());
                return;
            }

            try (Socket socketServidor1 = new Socket(ipServidor1, puertoServidor1);
                 Socket socketServidor2 = new Socket(ipServidor2, puertoServidor2)) {

                System.out.println("[Proxy] Conectado a Servidor-1: " + ipServidor1 + ":" + puertoServidor1);
                System.out.println("[Proxy] Conectado a Servidor-2: " + ipServidor2 + ":" + puertoServidor2);

                reenviarSolicitud(solicitud, socketServidor1.getOutputStream());
                reenviarSolicitud(solicitud, socketServidor2.getOutputStream());

                InputStream inputServidor1 = socketServidor1.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesLeidos;

                while ((bytesLeidos = inputServidor1.read(buffer)) != -1) {
                    if (socketCliente.isClosed()) {
                        System.out.println("[Advertencia] El cliente cerró la conexión antes de recibir la respuesta.");
                        break;
                    }
                    out.write(buffer, 0, bytesLeidos);
                    out.flush();
                }
                System.out.println("[Proxy] Respuesta del Servidor-1 enviada al cliente.");

            } catch (IOException e) {
                System.err.println("[Error] Error al conectar con los servidores: " + e.getMessage());
            }

            out.close();
            socketCliente.close();
            System.out.println("[Proxy] Conexión con el cliente cerrada.");

        } catch (IOException e) {
            System.err.println("[Error] Error en manejarSolicitud: " + e.getMessage());
        }
    }

    private static void reenviarSolicitud(String solicitud, OutputStream output) {
        try {
            output.write((solicitud + "\r\n\r\n").getBytes());
            output.flush();
        } catch (IOException e) {
            System.err.println("[Error] Error al reenviar la solicitud: " + e.getMessage());
        }
    }
}
