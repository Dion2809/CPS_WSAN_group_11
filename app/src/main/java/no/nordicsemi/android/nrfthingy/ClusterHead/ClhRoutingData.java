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

    private final String LOG_TAG = "CLH Routing";
    private static final int CLH_ARRAY_SIZE = 5 + ROUTING_BYTES;

    public ClhRoutingData() {
        //set route bytes to -1 as undefined value
        for (int i = ROUTING0; i < CLH_ARRAY_SIZE; i++) {
            ClhAdvData[i] = -1;
        }
    }

    // Add the current id to the route list
    public void addToRouting(byte ID) {
        boolean routeAdded = false;
        for (int i = ROUTING0; i < ROUTING0 + ROUTING_BYTES; i++) {
            if (!routeAdded) {
                if (ClhAdvData[i] == -1) {
                    ClhAdvData[i] = ID;
                    routeAdded = true;
                }
            }
        }
        if (!routeAdded) {
            Log.e(LOG_TAG ,"Packet full, route can't be added.");
        }
    }

    // Compile routing bytes into a single array and return it
    public byte[] getRouting() {
        return Arrays.copyOfRange(ClhAdvData, ClhAdvData.length - ROUTING_BYTES, ClhAdvData.length);
    }

    public String logData() {
        StringBuilder packet = new StringBuilder();
        for (int i = 0; i < ClhAdvData.length; i++) {
            packet.append(ClhAdvData[i]);
        }
        Log.i("Data","Data: " + packet);
        return packet.toString();
    }
}
