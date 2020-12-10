package no.nordicsemi.android.nrfthingy.ClusterHead;

import android.util.Log;
import android.util.SparseArray;

import java.util.Arrays;

public class ClhRoutingData extends ClhAdvertisedData {
    private static final int SOURCE_CLH_ID_POS=0;
    private static final int PACKET_CLH_ID_POS=1;
    private static final int DEST_CLH_ID_POS=2;
    private static final int HOP_COUNT_POS=3;
    private static final int NEXT_HOP = 4;
    private static final int ROUTING0 = 5;
    private static final int ROUTING1 = 6;
    private static final int ROUTING2 = 7;
    private static final int ROUTING3 = 8;
    private static final int ROUTING4 = 9;
    public static final int ROUTING_BYTES = 5;



    private static final int CLH_ARRAY_SIZE=5+ROUTING_BYTES;
    byte[] ClhAdvData=new byte[CLH_ARRAY_SIZE];

    public ClhRoutingData() {
        //set route bytes to -1 as undefined value
        for (int i = ROUTING0; i < CLH_ARRAY_SIZE; i++) {
            ClhAdvData[i] = -1;
        }
    }

    // Add the current id to the route list
    public void addToRouting(byte ID) {
        boolean routeAdded = false;
        for (int i = ROUTING0; !routeAdded && i < ROUTING_BYTES; i++) {
            if (ClhAdvData[i] == 0) {
                ClhAdvData[i] = ID;
                routeAdded = true;
            }
        }
        if (!routeAdded) {
            Log.e("Packet full" ,"Packet full, route can't be added.");
        }
    }

    // Compile routing bytes into a single array and return it
    public Byte[] getRouting() {
        Byte[] routing = new Byte[8];
        int j = 0;
        for (int i = ROUTING0; i < (ROUTING_BYTES+ROUTING0); i++) {
            routing[j] = ClhAdvData[i];
        }
        return routing;
    }
}
