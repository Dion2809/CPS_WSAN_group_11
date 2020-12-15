
package no.nordicsemi.android.nrfthingy.ClusterHead;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ClhScan {
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeScanner mCLHscanner ;
    private final String LOG_TAG="CLH Scanner:";

    private Handler handler = new Handler();
    private Handler handler2 = new Handler();
    private boolean mScanning;
    private byte mClhID=1;
    private boolean mIsSink=false;
    private ScanSettings mScanSettings;

    private SparseArray<Integer> ClhScanHistoryArray=new SparseArray();

    //private static final int MAX_PROCESS_LIST_ITEM=128;
    //private ClhAdvertisedData clhAdvData=new ClhAdvertisedData();
    private ClhAdvertise mClhAdvertiser;
    private ArrayList<ClhAdvertisedData> mClhProcDataList ;
    private ClhProcessData mClhProcessData;
    private ArrayList<ClhAdvertisedData> mClhAdvDataList;
    private static final int MAX_ADVERTISE_LIST_ITEM=128;

    public ClhScan()
    {

    }

    public ClhScan(ClhAdvertise clhAdvObj,ClhProcessData clhProcDataObj)
    {//constructor, set 2 alias to Clh advertiser and processor
        mClhAdvertiser=clhAdvObj;
        mClhAdvDataList=mClhAdvertiser.getAdvertiseList();
        mClhProcessData=clhProcDataObj;
        mClhProcDataList=clhProcDataObj.getProcessDataList();
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public int BLE_scan() {
        boolean result=true;
        byte[] advsettings=new byte[16];
        byte[] advData= new byte[256];
        int length;
        final List<ScanFilter> filters = new ArrayList<>();

        if (!mScanning) {
            //verify BLE available
            mCLHscanner = mAdapter.getBluetoothLeScanner();
            if (mCLHscanner == null) {
                Log.i(LOG_TAG, "BLE not supported");
                return ClhErrors.ERROR_CLH_BLE_NOT_ENABLE;
            }

            //setting
            ScanSettings ClhScanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .build();

            //set filter: filter name
            ScanFilter filter = new ScanFilter.Builder()
                    .setDeviceName(ClhConst.clusterHeadName)
                    .build();
            filters.add(filter);
            Log.i(LOG_TAG, "filters"+ filters.toString());

            mScanSettings =ClhScanSettings;
// Stops scanning after 60 seconds.

            // Create a timer to stop scanning after a pre-defined scan period.
            //rest, then restart to avoid auto disable from Android
           handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mCLHscanner.stopScan(CLHScanCallback);
                    Log.i(LOG_TAG, "Stop scan");
                    //start another timer for resting in 1s
                    handler2.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mScanning = true;
                            mCLHscanner.startScan(filters, mScanSettings, CLHScanCallback);
                        }
                    },ClhConst.REST_PERIOD);
                }
            }, ClhConst.SCAN_PERIOD);

            mScanning = true;
            mCLHscanner.startScan(filters, ClhScanSettings, CLHScanCallback);
            Log.i(LOG_TAG, "Start scan");
        }
        else
        {
            return ClhErrors.ERROR_CLH_SCAN_ALREADY_START;
        }

        return ClhErrors.ERROR_CLH_NO;
    }

    public void stopScanCLH()
    {
        mScanning = false;
        mCLHscanner.stopScan(CLHScanCallback);
        Log.i(LOG_TAG, "Stop scan");
    }


    private ScanCallback CLHScanCallback = new ScanCallback() {
        @Override
        public final void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            //no need this code since already have name filter
            /*if( result == null
                    || result.getDevice() == null
                    || TextUtils.isEmpty(result.getDevice().getName()) ) {
                Log.i(LOG_TAG, "Empty name space");
                return;
                //if( result == null || result.getDevice() == null)  return;
            }*/

            //check RSSI to remove weak signal ones
            if (result.getRssi()<ClhConst.MIN_SCAN_RSSI_THRESHOLD) {
                Log.i(LOG_TAG,"low RSSI");
                return;
            }

            Log.i(LOG_TAG, "Ja hier2: " + result);

            SparseArray<byte[]> manufacturerData = result.getScanRecord().getManufacturerSpecificData(); //get data
            processScanData(manufacturerData);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e( "BLE", "Discovery onScanFailed: " + errorCode );
            super.onScanFailed(errorCode);
        }
    };


/*process received data of BLE Manufacturer field
 include:
- Manufacturer Specification (in manufacturerData.key): "unique packet ID", include
            2 bytes: 0XAABB: AA: Source Cluster Head ID: 0-127
                            BB: Packet ID: 0-254 (unique for each packet)
 - Manufacturer Data (in manufacturerData.value): remained n data bytes (manufacturerData.size())
-------------------*/

    public void processScanData(SparseArray<byte[]> manufacturerData) {


        if(manufacturerData==null)
        {
            Log.i(LOG_TAG, "no Data");
            return;

        }
        int receiverID=manufacturerData.keyAt(0);

        Log.i(LOG_TAG, "Ja hier: " + manufacturerData.toString());

        //reflected data (received cluster head ID = device Clh ID) -> skip
        if(mClhID==(receiverID>>8))
        {
            Log.i(LOG_TAG,"reflected data, mClhID "+mClhID +", recv:" +(receiverID>>8) );
            return;
        }

        Log.i(LOG_TAG,"ID data "+ (receiverID>>8)+ "  "+(receiverID&0xFF) );

        /* check packet has been yet recieved by searching the "unique packet ID" history list
         - history list include:
                        Key: unique packet ID
                        life counter: time of the packet lived in history list
          --------------*/

        ClhAdvertisedData test = new ClhAdvertisedData();
        test.parcelAdvData(manufacturerData, 0);
        int id = test.getPacketID();

        if (ClhScanHistoryArray.indexOfKey(manufacturerData.keyAt(0))<0 || id == 0) // Not yet received or discovery packets
        {//not yet received
            //history not yet full, update new "unique packet ID" to history list, reset life counter
            if(ClhScanHistoryArray.size()<ClhConst.SCAN_HISTORY_LIST_SIZE)
            {
                ClhScanHistoryArray.append(manufacturerData.keyAt(0),0);
            }
            ClhAdvertisedData clhAdvData = new ClhAdvertisedData();

            //add receive data to Advertise list or Process List
            //Log.i(LOG_TAG," add history"+ (receiverID>>8)+ "  "+(receiverID&0xFF) );
            //Log.i(LOG_TAG," manufacturer value"+ Arrays.toString(manufacturerData.valueAt(0)) );
            //id 0 is discovery packet
            clhAdvData.parcelAdvData(manufacturerData,0);

//            Log.i("Packet received:", "\n" + "Source id:" + clhAdvData.getSourceID() + "\n" +
//                    "Destination id: " + clhAdvData.getDestinationID() + "\n" +
//                    "Packet id: " + clhAdvData.getPacketID() + "\n" +
//                    "Next hop: " + clhAdvData.getNextHop() + "\n" +
//                    "Hops left: " + clhAdvData.getHopCounts() + "\n" +
//                    Arrays.toString(clhAdvData.ClhAdvData) + "\n");

            if(mIsSink) {
                //route request received at sink, send route back
                if ((receiverID & 0xFF) == 0) {

                    Log.e(LOG_TAG, "SINK!!!!!");

                    //get the route
                    ClhRoutingData clhRouteData = new ClhRoutingData();
                    clhRouteData.parcelAdvData(manufacturerData, 0);
                    Byte[] route = clhRouteData.getRouting();
                    String routeString = "";
                    for (int i = 0; i < route.length; i++) {
                        routeString = routeString + route[i] + " ";
                    }
                    Log.i("Route received: ", routeString);

                    //get the next hop
                    byte dest = route[0];
                    List<Byte> reversed = Arrays.asList(route);
                    Collections.reverse(reversed);
                    byte nextHop = -1;
                    for (byte hop : reversed) {
                        if (hop != -1) {
                            nextHop = hop;
                            break;
                        }
                    }

                    //add it if the route is better or there was no route stored yet
                    if (mClhAdvertiser.getNextHop(dest) == -1 ||
                            (mClhAdvertiser.getNextHop(dest) != -1 &&
                            clhRouteData.getHopCounts() < mClhAdvertiser.hopsToDest(dest))) {
                        Log.e(LOG_TAG, "Better route added!!");

                        mClhAdvertiser.addRoute(dest, nextHop, clhRouteData.getHopCounts());
                    }

                    //send back reply with packet id 1
                    clhRouteData.addToRouting(mClhID);
                    clhRouteData.setPacketID((byte) 1);
                    clhRouteData.setSourceID(mClhID);
                    clhRouteData.setDestId(dest);
                    clhRouteData.setNextHop((byte) nextHop);
                    mClhAdvertiser.addAdvPacketToBuffer(clhRouteData, true);
                    Log.i("Reply sent:", "Route Reply sent to: " + dest);
                } else {// add data to waiting process list
                    mClhProcessData.addProcessPacketToBuffer(clhAdvData);
                    Log.i(LOG_TAG, "Add data to process list, len:" + mClhProcDataList.size());
                }
            }
            else {
                if (clhAdvData.getPacketID() == 0) {

                    Log.i("Discovery received:", "\n" + "Source id:" + clhAdvData.getSourceID() + "\n" +
                            "Destination id: " + clhAdvData.getDestinationID() + "\n" +
                            "Packet id: " + clhAdvData.getPacketID() + "\n" +
                            "Next hop: " + clhAdvData.getNextHop() + "\n" +
                            "Hops left: " + clhAdvData.getHopCounts());

                    //discovery message, add id to route
                    ClhRoutingData clhRouteData = new ClhRoutingData();
                    clhRouteData.parcelAdvData(manufacturerData, 0);

                    clhRouteData.addToRouting(clhAdvData.getSourceID()); // Moet hier source ID van incoming packet of eigen ID, hier stond eerst eigen ID maar lijkt me onlogisch
                    clhRouteData.setSourceID(mClhID); // Sets packet source ID as this CH's id before broadcasting it again
                    clhAdvData = (ClhAdvertisedData) clhRouteData; // Het lijkt erop dat hij alle routing info verliest, kun je krijgen met clhRouteData.getRouting()

                    mClhAdvertiser.addAdvPacketToBuffer(clhAdvData, true);
                    mClhAdvertiser.nextAdvertisingPacket(); //start advertising

                    // TODO: Soms stuurt hij wel packets terug en soms helemaal niks, super raar

                } else if (clhAdvData.getPacketID() == 1) {
                    //route response, add next hop data
                    ClhRoutingData clhRouteData = new ClhRoutingData();
                    clhRouteData.parcelAdvData(manufacturerData, 0);
                    Byte[] route = clhRouteData.getRouting();

                    //get the next hop
                    byte dest = 0; //always the sink
                    byte nextHop = -1;
                    for (int i = 0; i < route.length; i++) {
                        if (route[i] == mClhID) {
                            nextHop = route[i+1];
                            break;
                        }
                    }

                    if (nextHop != -1) {
                        //add it if the route is better or there was no route stored yet
                        if (mClhAdvertiser.getNextHop(dest) == -1 ||
                                (mClhAdvertiser.getNextHop(dest) != -1 &&
                                        clhRouteData.getHopCounts() < mClhAdvertiser.hopsToDest(dest))) {
                            mClhAdvertiser.addRoute(dest, nextHop, clhRouteData.getHopCounts());
                        }
                    }
                }

                if (clhAdvData.getDestinationID() != mClhID && (clhAdvData.getNextHop() == mClhID ||
                        clhAdvData.getNextHop() == -1)) {
                    //normal Cluster Head (ID 0..127) add data to advertising list to forward
                    if(clhAdvData.getNextHop() != -1) {
                        //set the next hop to the next hop for this cluster head as defined in the routing table
                        clhAdvData.setNextHop(mClhAdvertiser.getNextHop(clhAdvData.getDestinationID()));
                    }

                    mClhAdvertiser.clearAdvList();

                    mClhAdvertiser.addAdvPacketToBuffer(clhAdvData, false);
                    Log.i(LOG_TAG, "Add data to advertised list, len:" + mClhAdvDataList.size());
                    Log.i(LOG_TAG, "Advertise list at " + (mClhAdvDataList.size() - 1) + ":"
                            + Arrays.toString(mClhAdvDataList.get(mClhAdvDataList.size() - 1).getParcelClhData()));
                }
            }
        }

        mClhAdvertiser.stopAdvertiseClhData();

    }

    public void setClhID(byte clhID, boolean isSink){
        mClhID=clhID;
        mIsSink=isSink;
    }

    //set alias to Clh advertiser
    public void setAdvDataObject(ClhAdvertise clhAdvObj){
        mClhAdvertiser=clhAdvObj;
        mClhAdvDataList=mClhAdvertiser.getAdvertiseList();

    }

    //set alias to Clh processor
    public void setProcDataObject(ClhProcessData clhProObj){
        mClhProcessData=clhProObj;
        mClhProcDataList=mClhProcessData.getProcessDataList();
    }

}


