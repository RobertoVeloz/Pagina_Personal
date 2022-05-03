import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URL;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.List;
import java.lang.Math; 
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebServer{

    private static final String TASK_ENDPOINT = "/task";
    private static final String STATUS_ENDPOINT = "/status";

    private final int port;
    private HttpServer server;
    String url = "https://raw.githubusercontent.com/RobertoVeloz/Pagina_Personal/master/BIBLIA_COMPLETA.txt"; //dirección url del recurso a descargar
    String name = "BIBLIA_COMPLETA.TXT"; //nombre del archivo destino

        //Directorio destino para las descargas
    String folder = "Descargas/";
    
    public static void main(String[] args) throws MalformedURLException, IOException {
        

        String url = "https://raw.githubusercontent.com/RobertoVeloz/Pagina_Personal/master/BIBLIA_COMPLETA.txt"; //dirección url del recurso a descargar
        String name = "BIBLIA_COMPLETA.TXT"; //nombre del archivo destino

        //Directorio destino para las descargas
        String folder = "Descargas/";

        //Crea el directorio de destino en caso de que no exista
        File dir = new File(folder);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                return; // no se pudo crear la carpeta de destino
            }
        }

        File file = new File(folder + name);

        URLConnection conn = new URL(url).openConnection();
        conn.connect();
        System.out.println("\nempezando descarga: \n");
        System.out.println(">> URL: " + url);
        System.out.println(">> Nombre: " + name);
        System.out.println(">> tamaño: " + conn.getContentLength() + " bytes");

        try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(file)) {
            
            int b = 0;
            while (b != -1) {
                b = in.read();
                if (b != -1) {
                    out.write(b);
                }
            }
            
        }


        int serverPort = 8080;
        if (args.length == 1) {
            serverPort = Integer.parseInt(args[0]);
        }

        WebServer webServer = new WebServer(serverPort);
        webServer.startServer();

        System.out.println("Servidor escuchando en el puerto " + serverPort);
    }

    public WebServer(int port) {
        this.port = port;
    }

    public void startServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        HttpContext statusContext = server.createContext(STATUS_ENDPOINT);
        HttpContext taskContext = server.createContext(TASK_ENDPOINT);

        statusContext.setHandler(this::handleStatusCheckRequest);
        taskContext.setHandler(this::handleTaskRequest);

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
    }

    private void handleTaskRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
            exchange.close();
            return;
        }

        Headers headers = exchange.getRequestHeaders();
        if (headers.containsKey("X-Test") && headers.get("X-Test").get(0).equalsIgnoreCase("true")) {
            String dummyResponse = "123\n";
            sendResponse(dummyResponse.getBytes(), exchange);
            return;
        }

        boolean isDebugMode = false;
        if (headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true")) {
            isDebugMode = true;
        }

        long startTime = System.nanoTime();

        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        byte[] responseBytes = null;
        try {
            responseBytes = calculateResponse(requestBytes);
        } catch (InterruptedException ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        long finishTime = System.nanoTime();

        if (isDebugMode) {
            String debugMessage = String.format("La operación tomó %d nanosegundos", finishTime - startTime);
            exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        sendResponse(responseBytes, exchange);
    }

    public byte[] calculateResponse(byte[] requestBytes) throws IOException, InterruptedException {
        
        
        
        String bodyString = new String(requestBytes);
        String[] stringNumbers = bodyString.split(",");
        ArrayList<String> Lista_de_Palabras = new ArrayList<>();
        int auxi=1;
        int numero_de_conexiones = 0,numero_de_computadora=0;
        for (String number : stringNumbers) {
            if (auxi==1) {
                 numero_de_conexiones= Integer.parseInt(number); //numero de conexiones a generaL
                 //System.out.println("Soy el numero_de_conexiones: .."+number+" ............. ");
                auxi++;
            }
            else if (auxi==2) {
                 numero_de_computadora= Integer.parseInt(number); //numero de computadora particular
                 //System.out.println("Soy el numero_de_computadora: ..."+number+" .......... ");
                auxi++;
            }
            else if (auxi==3) {
                Lista_de_Palabras.add(number) ; //numero de computadora particular
                //System.out.println("Soy La Lista_de_Palabras: ...."+number+" -----------  ");
                auxi=1;
            }
            
        }
        //Thread.sleep(15000);
        
        //usamos modulo para dividir la biblia exactamente las 35187 lineas
        int lineas_de_la_biblia = 35187, numero_de_lineas_por_servidor = 0, lineas_agregadas = 0;
        while (Math.floorMod(lineas_de_la_biblia, numero_de_conexiones) != 0) {
            //lineas_de_la_biblia++;
            lineas_agregadas++;
        }
        //System.out.println("pasamos el while");
        numero_de_lineas_por_servidor = lineas_de_la_biblia / numero_de_conexiones;  //numero exacto equitativo para cada servidor
        int wq;
        
        //Reparticion de linneas de la biblia
        int trabajo_de_la_linea = 0, a_la_linea = 0;
        for (int i = 1; i <= numero_de_conexiones; i++) {
            //System.out.println("entramos al final for");
            if (numero_de_computadora == numero_de_conexiones) { //Ultimo caso  //computadora 1 *que puede ser con menos lineas COMPU 1 DE 21-30
                trabajo_de_la_linea = ((numero_de_computadora - 1) * numero_de_lineas_por_servidor) + 1;
                a_la_linea = (numero_de_computadora * numero_de_lineas_por_servidor)-1 ; //si no hay agregadas no pasa nada
                
            } else {

                if (numero_de_computadora == 1) { //computadora 1 ej 3 compus   30 lineas en total    COMPU 1 DE 1-10
                    trabajo_de_la_linea = 0;          //numero_de_lineas_por_servidor =10
                    a_la_linea = numero_de_lineas_por_servidor;
                } else {                          //computadora 2 a n-1       COMPU 2 DE 11-20
                    trabajo_de_la_linea = ((numero_de_computadora - 1) * numero_de_lineas_por_servidor) + 1;
                    a_la_linea = numero_de_computadora * numero_de_lineas_por_servidor;
                }
                
            }

           
            //System.out.println("LINEAS PARA EL SERVIDOR" + wq);
        }

        for (int i = 0; i < 10; i++) {

        }
        System.out.println("Soy el Cliente: "+numero_de_computadora+" y me corresponde el rango de ");
        Archivo Biblia = new Archivo(folder + name);
        ArrayList<String> Biblia_por_lineas = new ArrayList<>();
        //System.out.println(Biblia.leerLinea()); lee una linea de la biblia
        String Linea_de_biblia;
        for (int i = 0; i < lineas_de_la_biblia; i++) {
            Biblia_por_lineas.add(Biblia.leerLinea());

        }
        StringTokenizer Palabras_de_la_linea = null;
        int contador = trabajo_de_la_linea-1;
        String aux;
        String Retornar;
        String coma=",";
        int concurrencia = 0;
        int num_palabras_en_linea;
        String Token_Palabra = null;
        String Token_Palabra_minuscula = null;
        for (int i = trabajo_de_la_linea; i <= a_la_linea; i++) { //Solo el segmento que le corresponde a cada uno
            Linea_de_biblia = Biblia_por_lineas.get(i);
            StringTokenizer Palabras_auxiliar_2 = Palabras_de_la_linea;
            contador++;
            System.out.println("Analizando linea..." + contador);
            Palabras_auxiliar_2 = new StringTokenizer(Linea_de_biblia, " ,/().:'-_<>;*!¡?¿[]{}+|^»««»", false);
            num_palabras_en_linea = Palabras_auxiliar_2.countTokens();
            System.out.println("");
            int rango = 0;
            
            while (Palabras_auxiliar_2.hasMoreTokens()) { //recorremos cada palabra de esa linea
                String nextPalabra = Palabras_auxiliar_2.nextToken();
                String nextPalabraminuscula;
                  nextPalabraminuscula =nextPalabra.toLowerCase();
                
                

                System.out.println("   => Buscando palabra: "+nextPalabraminuscula);

                for (int k = 0; k < Lista_de_Palabras.size(); k++) {
                    Token_Palabra = Lista_de_Palabras.get(k);
                     Token_Palabra_minuscula =Token_Palabra.toLowerCase();
                    if (Token_Palabra_minuscula.equals(nextPalabraminuscula)) {

                        //envio.setEstatus(aux = "Ok");
                        concurrencia++;
                        //System.out.println("hubo una concurrencia");
                        //Thread.sleep(15000);
                    }
                }

            }
        }
        
        System.out.println("Termino analisis para '"+Token_Palabra+"' de las lineas");
        System.out.println("inicio-> "+trabajo_de_la_linea+" final-> "+a_la_linea);
        
        Retornar= String.valueOf(concurrencia);
        System.out.println("ESTO ESTOY INTENTANDO ENVIAR.. +concurrencia+"+Retornar);
        return String.format("La concurrencia de la palabra... encontradas es: /*%s*/\n", Retornar).getBytes();
    }

    private void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.close();
            return;
        }

        String responseMessage = "El servidor está vivo\n";
        sendResponse(responseMessage.getBytes(), exchange);
    }

    private void sendResponse(byte[] responseBytes, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
        exchange.close();
    }
}


class Archivo 
{
    private File archivo;
    private FileReader archivoLector;
    private BufferedReader buferLector;
    private String linea;
    
    public Archivo(String rutaArchivo)
    {
      archivo = new File(rutaArchivo);
      try 
      {
        archivoLector = new FileReader(archivo);
      } catch (FileNotFoundException error) 
       {
         System.out.println(error.getMessage());
       }
      buferLector = new BufferedReader(archivoLector);
    }
    
    public String leerLinea() throws IOException
    {
      while(buferLector.ready())
      {
        if(!(linea = buferLector.readLine()).equals("\000"))
        return linea;
      }
      buferLector.close();
      return null;
    }
}

