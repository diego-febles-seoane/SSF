package es.diegofeblesseoane.ssf;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * @author: diego-febles-seoane
 * @version: 1.0.8
 */
public class ssf {

    /**
     * Variable global para modo debug
     */
    static boolean debug = false;

    /**
     * Convierte una IP (en formato "dotted") a un valor numérico (long).
     * 
     * @param ipStr IP en string
     * @return long IP en formato numérico
     */
    public static long ipToLong(String ipStr) {
        String[] parts = ipStr.split("\\.");
        if (parts.length != 4)
            throw new IllegalArgumentException("IP inválida: " + ipStr);
        long result = 0;
        for (int i = 0; i < 4; i++) {
            int octet = Integer.parseInt(parts[i]);
            if (octet < 0 || octet > 255)
                throw new IllegalArgumentException("Cada octeto debe estar entre 0 y 255");
            result = (result << 8) | octet;
        }
        return result;
    }

    /**
     * Convierte un valor numérico (long) a una IP en formato "dotted".
     * 
     * @param ip IP en formato numérico (long)
     * @return StringIP en string
     */
    public static String longToIp(long ip) {
        return ((ip >> 24) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                (ip & 0xFF);
    }

    /**
     * Dado un prefijo (por ejemplo, 24) devuelve la máscara de red en formato
     * numérico (long).
     * 
     * @param prefix Prefijo de la máscara (0-32)
     * @return String Máscara de red en formato numérico (long)
     */
    public static long prefixToMask(int prefix) {
        if (prefix < 0 || prefix > 32)
            throw new IllegalArgumentException("Prefijo inválido: " + prefix);
        return (prefix == 0) ? 0 : ((0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL);
    }

    /**
     * Devuelve la clase de la IP (A, B, C, D o E) según su primer octeto.
     * 
     * @param ip IP en formato numérico (long)
     * @return String Clase de la IP (A, B, C, D o E)
     */
    public static String getIpClass(long ip) {
        int firstOctet = (int) ((ip >> 24) & 0xFF);
        if (firstOctet < 128)
            return "A";
        else if (firstOctet < 192)
            return "B";
        else if (firstOctet < 224)
            return "C";
        else if (firstOctet < 240)
            return "D";
        else
            return "E";
    }

    /**
     * Comprueba si la IP es privada.
     * 
     * @param ip IP en formato numérico (long)
     * @return true/false
     */
    public static boolean isPrivate(long ip) {
        int firstOctet = (int) ((ip >> 24) & 0xFF);
        int secondOctet = (int) ((ip >> 16) & 0xFF);
        return (firstOctet == 10) ||
                (firstOctet == 172 && (secondOctet >= 16 && secondOctet <= 31)) ||
                (firstOctet == 192 && secondOctet == 168);
    }

    /**
     * Devuelve una representación binaria en 8 bits para un número.
     * 
     * @param num Número a convertir
     * @return String Representación binaria en 8 bits
     */
    public static String toBinary8(int num) {
        StringBuilder listaBinaria = new StringBuilder(Integer.toBinaryString(num));
        while (listaBinaria.length() < 8) {
            listaBinaria.insert(0, "0");
        }
        return listaBinaria.toString();
    }

    /**
     * Devuelve la representación en binario (en 4 grupos de 8 bits) de un IP.
     * @param ip IP en formato numérico (long)
     * @return String Representación binaria de la IP
     */
    public static String ipToBinaryString(long ip) {
        return toBinary8((int) ((ip >> 24) & 0xFF)) + " " +
                toBinary8((int) ((ip >> 16) & 0xFF)) + " " +
                toBinary8((int) ((ip >> 8) & 0xFF)) + " " +
                toBinary8((int) (ip & 0xFF));
    }

    /**
     * Muestra los cálculos básicos de una IP/MASCARA.
     * @param ipStr IP en formato string
     * @param prefix Prefijo de la máscara
     */
    public static void displayNormalCalculations(String ipStr, int prefix) {
        long ip = ipToLong(ipStr);
        long mask = prefixToMask(prefix);
        long network = ip & mask;
        long blockSize = 1L << (32 - prefix);
        long broadcast = network + blockSize - 1;

        System.out.println("Clase: " + getIpClass(ip));
        System.out.println("Tipo: " + (isPrivate(ip) ? "Privada" : "Pública"));
        System.out.println("Dirección de red: " + longToIp(network));
        System.out.println("Broadcast: " + longToIp(broadcast));

        if (blockSize > 2) {
            long firstHost = network + 1;
            long lastHost = broadcast - 1;
            System.out.println("Rango disponible: " + longToIp(firstHost) + " - " + longToIp(lastHost));
        } else {
            System.out.println("No hay hosts disponibles en este bloque.");
        }

        if (debug) {
            System.out.println("\n[DEBUG] Detalles en binario:");
            System.out.println("IP:         " + ipToBinaryString(ip));
            System.out.println("Máscara:    " + ipToBinaryString(mask));
            System.out.println("Red:        " + ipToBinaryString(network));
            System.out.println("Broadcast:  " + ipToBinaryString(broadcast));
        }
    }

    /**
     * Calcula el prefijo mínimo necesario para alojar la cantidad de hosts
     * requeridos.
     * Fórmula: 2^(32 - nuevoPrefijo) - 2 >= hostsRequeridos
     * @param hostsRequired Cantidad de hosts requeridos
     * @return int Prefijo mínimo necesario
     */
    public static int requiredPrefix(int hostsRequired) {
        double bitsNeeded = Math.ceil(Math.log(hostsRequired + 2) / Math.log(2));
        return 32 - (int) bitsNeeded;
    }

    /**
     * Aplica la asignación VLSM: Se ordenan las subredes según hosts requeridos (de
     * mayor a menor) y se asigna de la red base cada bloque calculado.
     * @param baseNetwork Dirección de red base
     * @param baseBroadcast Dirección de broadcast base
     * @param reqList Lista de requisitos de subredes (Subred, Hosts requeridos)
     * @return List<SubnetAllocation> Lista de asignaciones de subredes
     */
    public static List<SubLocacion> allocateSubnets(long baseNetwork, long baseBroadcast,
            List<SubLocacion> reqList) {
        reqList.sort((a, b) -> b.hostsRequired - a.hostsRequired);
        List<SubLocacion> allocations = new ArrayList<>();

        long current = baseNetwork;
        for (SubLocacion req : reqList) {
            int reqPrefix = requiredPrefix(req.hostsRequired);
            long blockSize = 1L << (32 - reqPrefix);
            if (debug) {
                System.out.println("[DEBUG] Subred " + req.id + ": Hosts requeridos = " + req.hostsRequired +
                        ", prefijo mínimo calculado: /" + reqPrefix +
                        " (bloque = " + blockSize + ")");
            }
            if (current % blockSize != 0) {
                current = ((current / blockSize) + 1) * blockSize;
            }
            if (current + blockSize - 1 > baseBroadcast) {
                System.err.println("Error: No se puede asignar " + req.hostsRequired +
                        " hosts a la subred " + req.id + ". Espacio insuficiente.");
                System.exit(1);
            }
            SubLocacion alloc = new SubLocacion(req.id, req.hostsRequired, reqPrefix, current);
            allocations.add(alloc);
            if (debug) {
                System.out.println("[DEBUG] Asignada Subred " + req.id + ": " +
                        "IP Red = " + longToIp(alloc.networkAddress) +
                        ", Máscara = /" + alloc.prefix +
                        ", Primer Host = " + longToIp(alloc.networkAddress + 1) +
                        ", Último Host = " + longToIp(alloc.broadcastAddress - 1) +
                        ", Broadcast = " + longToIp(alloc.broadcastAddress));
            }
            current += blockSize;
        }

        allocations.sort(Comparator.comparingInt(a -> a.id));
        return allocations;
    }

    /**
     * Muestra una tabla con la asignación VLSM.
     * @param allocations Lista de asignaciones de subredes
     */
    public static void displayVlsmTable(List<SubLocacion> allocations) {
        String header = String.format("%-6s %-8s %-15s %-9s %-15s %-15s %-15s",
                "Subred", "Nº Hosts", "IP Red", "Máscara", "Primer Host", "Último Host", "Broadcast");
        System.out.println("\n" + header);
        System.out.println("-".repeat(header.length()));
        for (SubLocacion alloc : allocations) {
            long blockSize = alloc.blockSize;
            String firstHost = (blockSize > 2) ? longToIp(alloc.networkAddress + 1) : longToIp(alloc.networkAddress);
            String lastHost = (blockSize > 2) ? longToIp(alloc.broadcastAddress - 1) : longToIp(alloc.broadcastAddress);
            System.out.printf("%-6d %-8d %-15s /%-8d %-15s %-15s %-15s%n",
                    alloc.id,
                    alloc.hostsRequired,
                    longToIp(alloc.networkAddress),
                    alloc.prefix,
                    firstHost,
                    lastHost,
                    longToIp(alloc.broadcastAddress));
        }
    }

    /**
     * Exporta la tabla VLSM a un archivo CSV.
     * @param allocations Lista de asignaciones de subredes
     * @param filename Nombre del archivo CSV
     */
    public static void exportCsv(List<SubLocacion> allocations, String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("Subred,Nº Hosts,IP Red,Máscara,Primer Host,Último Host,Broadcast");
            for (SubLocacion alloc : allocations) {
                long blockSize = alloc.blockSize;
                String firstHost = (blockSize > 2) ? longToIp(alloc.networkAddress + 1)
                        : longToIp(alloc.networkAddress);
                String lastHost = (blockSize > 2) ? longToIp(alloc.broadcastAddress - 1)
                        : longToIp(alloc.broadcastAddress);
                pw.printf("%d,%d,%s,/%d,%s,%s,%s%n",
                        alloc.id,
                        alloc.hostsRequired,
                        longToIp(alloc.networkAddress),
                        alloc.prefix,
                        firstHost,
                        lastHost,
                        longToIp(alloc.broadcastAddress));
            }
            System.out.println("\nLos resultados se han exportado a '" + filename + "'");
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo CSV: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java Netip IP/MASCARA [opciones]");
            System.err.println("Opciones disponibles: -vlsm, -d|--debug, --csv <archivo.csv>");
            System.exit(1);
        }

        String ipMask = args[0];
        boolean vlsmMode = false;
        String csvFilename = null;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-vlsm")) {
                vlsmMode = true;
            } else if (arg.equals("-d") || arg.equals("--debug")) {
                debug = true;
            } else if (arg.equals("--csv")) {
                if (i + 1 < args.length) {
                    csvFilename = args[++i];
                } else {
                    System.err.println("Se requiere un nombre de archivo CSV tras --csv");
                    System.exit(1);
                }
            }
        }

        String[] ipParts = ipMask.split("/");
        if (ipParts.length != 2) {
            System.err.println("Error: Debe ingresar el parámetro en formato IP/MASCARA, por ejemplo 192.168.1.0/24");
            System.exit(1);
        }
        String ipStr = ipParts[0];
        int prefix = 0;
        try {
            prefix = Integer.parseInt(ipParts[1]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Prefijo de máscara no válido.");
            System.exit(1);
        }

        long ip;
        try {
            ip = ipToLong(ipStr);
        } catch (IllegalArgumentException e) {
            System.err.println("Error al parsear la IP: " + e.getMessage());
            System.exit(1);
            return;
        }
        long mask = prefixToMask(prefix);
        long network = ip & mask;
        long blockSize = 1L << (32 - prefix);
        long baseBroadcast = network + blockSize - 1;

        if (!vlsmMode) {
            displayNormalCalculations(ipStr, prefix);
        } else {
            System.out.println("Modo VLSM activado.");
            try (Scanner scanner = new Scanner(System.in)) {
                int numSubnets = 0;
                try {
                    System.out.print("¿Cuántas subredes necesitamos crear? ");
                    numSubnets = Integer.parseInt(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.err.println("Entrada no válida para el número de subredes.");
                    System.exit(1);
                }
                if (numSubnets <= 0) {
                    System.err.println("El número de subredes debe ser mayor que 0.");
                    System.exit(1);
                }
                // Crear lista de requisitos de subredes (usaremos SubnetAllocation como
                // contenedor temporal)
                List<SubLocacion> reqList = new ArrayList<>();
                for (int i = 1; i <= numSubnets; i++) {
                    int hostsReq = 0;
                    try {
                        System.out.print("Subred " + i + " - Ingrese el número de hosts requeridos: ");
                        hostsReq = Integer.parseInt(scanner.nextLine().trim());
                    } catch (NumberFormatException e) {
                        System.err.println("Entrada no válida para el número de hosts.");
                        System.exit(1);
                    }
                    if (hostsReq <= 0) {
                        System.err.println("El número de hosts debe ser mayor que 0.");
                        System.exit(1);
                    }
                    reqList.add(new SubLocacion(i, hostsReq, 0, 0));
                }

                List<SubLocacion> allocations = allocateSubnets(network, baseBroadcast, reqList);
                displayVlsmTable(allocations);

                if (csvFilename != null) {
                    exportCsv(allocations, csvFilename);
                }
            }
        }
    }
}
