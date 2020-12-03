package no.nordicsemi.android.nrfthingy.ClusterHead;

public class ClhRoutingData extends ClhAdvertisedData {
    private static final int SOURCE_CLH_ID_POS=0;
    private static final int PACKET_CLH_ID_POS=1;
    private static final int ROUTING0 = 2;
    private static final int ROUTING1 = 3;
    private static final int ROUTING2 = 4;
    private static final int ROUTING3 = 5;
    private static final int ROUTING4 = 6;
    private static final int ROUTING5 = 7;
    private static final int ROUTING6 = 8;
    private static final int ROUTING7 = 9;
    private static final int ROUTING_BYTES = 8;



    private static final int CLH_ARRAY_SIZE=SOURCE_CLH_ID_POS+PACKET_CLH_ID_POS+ROUTING_BYTES;
    byte[] ClhAdvData=new byte[CLH_ARRAY_SIZE];

    // Add the current id to the route list
    public void addToRouting(byte[] route, byte sourceID) {
        boolean routeAdded = false;
        for (int i = 0; !routeAdded && i < ROUTING_BYTES; i++) {
            if (route[i] == 0) {
                route[i] = sourceID;
                routeAdded = true;
            }
        }
        if (!routeAdded) {
            System.out.println("Packet full, route can't be added.");
        }
    }
}
