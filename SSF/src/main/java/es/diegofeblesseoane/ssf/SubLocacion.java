package es.diegofeblesseoane.ssf;

/**
 * Clase para almacenar la asignación de una subred en el modo VLSM.
 * 
 * @author: diego-febles-seoane
 * @version: 1.1.4
 */
public class SubLocacion {
    int id;
    int hostsRequired;
    int prefix;
    long networkAddress;
    long broadcastAddress;
    long blockSize;

    /**
     * Constructor de la clase SubLocacion.
     * @param id
     * @param hostsRequired
     * @param prefix
     * @param networkAddress
     */
    public SubLocacion(int id, int hostsRequired, int prefix, long networkAddress) {
        this.id = id;
        this.hostsRequired = hostsRequired;
        this.prefix = prefix;
        this.blockSize = 1L << (32 - prefix);
        this.networkAddress = networkAddress;
        this.broadcastAddress = networkAddress + blockSize - 1;
    }
}
