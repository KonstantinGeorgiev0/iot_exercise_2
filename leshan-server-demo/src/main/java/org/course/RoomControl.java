/*
 *  Extension to leshan-server-demo for application code.
 */

package org.course;


import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.io.PrintWriter;

import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.SingleObservation;


public class RoomControl {
    
    //
    // Static reference to the server.
    //
    private static LeshanServer lwServer;

    //
    // 2IMN15:  TODO  : fill in
    //
    // Declare variables to keep track of the state of the room.
    //
	// total power budget
	private static int powerBudget;

	// track current presence status
	private static boolean presence;

	// track total maximum power of all connected luminaires
	private static int totalMaxRoomPower;

	// map to store registered luminaires
	private static Map<String, Integer> luminaireRegistry;

	// map to track state of each sensor
	private static Map<String, Boolean> sensorStates;

    public static void Initialize(LeshanServer server) {
		// Register the LWM2M server object for future use
		lwServer = server;

		// 2IMN15:  TODO  : fill in
		//
		// Initialize the state variables.
		presence = false;
		powerBudget = Integer.MAX_VALUE;
		totalMaxRoomPower = 0;
		luminaireRegistry = Collections.synchronizedMap(new HashMap<>());
		sensorStates = Collections.synchronizedMap(new HashMap<>());

		System.out.println("RomCom Logic Init");
    }

    //
    // Suggested support methods:
    //
    // * set the dim level of all luminaires.
    // * set the power flag of all luminaires.
    // * show the status of the room.

    
    public static void handleRegistration(Registration registration)
    {
        // Check which objects are available.
        Map<Integer,org.eclipse.leshan.core.LwM2m.Version> supportedObject =
	    registration.getSupportedObject();

		// if presence detector is available, register it
		if (supportedObject.get(Constants.PRESENCE_DETECTOR_ID) != null) {
			String endpoint = registration.getEndpoint();
			System.out.println("Presence Detector Registered: " + endpoint);

			// add to map with default false
			sensorStates.put(endpoint, false);
			System.out.println(">>> Active Sensors Count: " + sensorStates.size() + " <<<");

			// observe presence resource
			try {
				ObserveRequest obRequest = new ObserveRequest(Constants.PRESENCE_DETECTOR_ID, 0, Constants.RES_PRESENCE);
				lwServer.send(registration, obRequest, 1000);
				System.out.println(">> ObserveRequest sent << ");
			} catch (Exception e) {
				System.out.println("Observe request failed: " + e.getMessage());
			}
		}

		// if luminaire is available, register it
        if (supportedObject.get(Constants.LUMINAIRE_ID) != null) {
	    	System.out.println("Luminaire");

			// take peak power from resource
			int peakPower =
					readInteger(registration, Constants.LUMINAIRE_ID, 0, Constants.RES_PEAK_POWER);

			// update state
			synchronized (luminaireRegistry) {
				luminaireRegistry.put(registration.getEndpoint(), peakPower);
				totalMaxRoomPower += peakPower;
			}
			System.out.println("Added Luminaire. Total Room Max Power: " + totalMaxRoomPower);
			System.out.println(">>> Luminaire count: " + luminaireRegistry.size() + " <<<");

			// apply current states to the new light immediately
			switchLights(presence); // sync power state
			updatePresence();		// sync presence state
			recalculateDimLevel();  // sync dim level based on new totals
        }

        if (supportedObject.get(Constants.DEMAND_RESPONSE_ID) != null) {
	    	System.out.println("Demand Response Registered");
			//
			// The registerDemandResponse() method contains example code
			// on how handle a registration.
			//
			int newPowerBudget = registerDemandResponse(registration);
			// prevent negative values
			if (newPowerBudget >= 0) {
				System.out.println("Registered Demand Response. Budget: " + powerBudget);
				powerBudget = newPowerBudget;
				recalculateDimLevel();
			}

        }

	//  2IMN15: don't forget to update the other luminaires.
    }


    public static void handleDeregistration(Registration registration)
    {
	//
	// 2IMN15:  TODO  :  fill in
	//
	// The device identified by the given registration will
	// disappear.  Update the state accordingly.
		String endpoint = registration.getEndpoint();

		//  handle luminaire deregistration
		if (luminaireRegistry.containsKey(endpoint)) {
			int removedPower = luminaireRegistry.get(endpoint);

			synchronized (luminaireRegistry) {
				luminaireRegistry.remove(endpoint);
				totalMaxRoomPower -= removedPower;
				// prevent negative values
				if (totalMaxRoomPower < 0) totalMaxRoomPower = 0;
			}

			System.out.println("Luminaire deregistered: " + endpoint);
			System.out.println("Updated Total Room Max Power: " + totalMaxRoomPower);
			System.out.println(">>> Luminaire count: " + luminaireRegistry.size() + " <<<");

			// recalculate dim levels
			recalculateDimLevel();
		}

		// handle sensor deregistration
		if (sensorStates.containsKey(endpoint)) {
			sensorStates.remove(endpoint);
			System.out.println(">>> Sensor deregistered: " + endpoint + " <<<");
			System.out.println(">>> Active Sensors Count: " + sensorStates.size() + " <<<");
			updatePresence();
		}
    }
    
    public static void handleObserveResponse(SingleObservation observation,
					     Registration registration,
					     ObserveResponse response)
    {
        if (registration != null && observation != null && response != null) {
	    //
	    // 2IMN15:  TODO  :  fill in
	    //
	    // When the registration and observation are known,
	    // process the value contained in the response.
	    //
	    // Useful methods:
	    //    registration.getEndpoint()
	    //    observation.getPath()
			LwM2mPath path = observation.getPath();

			if (path.getObjectId() == Constants.PRESENCE_DETECTOR_ID &&
					path.getResourceId() == Constants.RES_PRESENCE) {

				String strVal = ((LwM2mResource) response.getContent()).getValue().toString();
				boolean newPresence = Boolean.parseBoolean(strVal);
				String endpoint = registration.getEndpoint();

				System.out.println("Update from " + endpoint + ": Presence is " + newPresence);

				// update only sensors state
				sensorStates.put(endpoint, newPresence);
				updatePresence();
			}
	    

	    // For processing an update of the Demand Response object.
	    // It contains some example code.

		if (path.getObjectId() == Constants.DEMAND_RESPONSE_ID &&
				path.getResourceId() == Constants.RES_TOTAL_BUDGET) {

			int newPowerBudget = observedDemandResponse(observation, response);
			if (newPowerBudget != -1) {
				System.out.println("Observation: New Power Budget: " + newPowerBudget);
				powerBudget = newPowerBudget;
				// recalculate dimming for all lights
				recalculateDimLevel();
			}
		}
//	    int newPowerBudget = observedDemandResponse(observation, response);

        }
    }

	// toggle power of all registered luminaires
	private static void switchLights(boolean state) {
		System.out.println("Switching all lights: " + (state ? "ON" : "OFF"));

		synchronized (luminaireRegistry) {
			for (String endpoint : luminaireRegistry.keySet()) {
				Registration reg = lwServer.getRegistrationService().getByEndpoint(endpoint);
				if (reg != null) {
					writeBoolean(reg, Constants.LUMINAIRE_ID, 0, Constants.RES_POWER, state);
				}
			}
		}
	}

	private static void recalculateDimLevel() {
		if (totalMaxRoomPower == 0) return;

		double ratio = (double) powerBudget / (double) totalMaxRoomPower;
		int dimLevel = (int) (ratio * 100);

		// cap
		if (dimLevel > 100) dimLevel = 100;
		if (dimLevel < 0) dimLevel = 0;

		System.out.println("Recalculating Dim Level: Budget=" + powerBudget +
				", TotalMax=" + totalMaxRoomPower +
				" -> Setting Dim Level to: " + dimLevel + "%");

		synchronized (luminaireRegistry) {
			for (String endpoint : luminaireRegistry.keySet()) {
				Registration reg = lwServer.getRegistrationService().getByEndpoint(endpoint);
				if (reg != null) {
					writeInteger(reg, Constants.LUMINAIRE_ID, 0, Constants.RES_DIM_LEVEL, dimLevel);
				}
			}
		}
	}

	private static void updatePresence() {
		boolean roomPresence = false;

		// room is occupied if any sensor is true
		synchronized(sensorStates) {
			for (boolean state : sensorStates.values()) {
				if (state) {
					roomPresence = true;
					break;
				}
			}
		}

		switchLights(roomPresence);
	}

    // Support functions for reading and writing resources of
    // certain types.

    // Returns the current power budget.
    private static int registerDemandResponse(Registration registration)
    {
	int powerBudget = readInteger(registration,
				      Constants.DEMAND_RESPONSE_ID,
				      0,
				      Constants.RES_TOTAL_BUDGET);
	System.out.println("Power budget is " + powerBudget);
	// Observe the total budget information for updates.
	try {
	    ObserveRequest obRequest =
		new ObserveRequest(Constants.DEMAND_RESPONSE_ID,
				   0,
				   Constants.RES_TOTAL_BUDGET);
	    System.out.println(">>ObserveRequest created << ");
	    ObserveResponse coResponse =
		lwServer.send(registration, obRequest, 1000);
	    System.out.println(">>ObserveRequest sent << ");
	    if (coResponse == null) {
		System.out.println(">>ObserveRequest null << ");
	    }
	}
	catch (Exception e) {
	    System.out.println("Observe request failed for Demand Response.");
	}
	return powerBudget;
    }

    // If the response contains a new power budget, it returns that value.
    // Otherwise, it returns -1.
    private static int observedDemandResponse(SingleObservation observation,
					      ObserveResponse response)
    {
	// Alternative code:
	// String obsRes = observation.getPath().toString();
	// if (obsRes.equals("/33002/0/30005")) 
	LwM2mPath obsPath = observation.getPath();
	if ((obsPath.getObjectId() == Constants.DEMAND_RESPONSE_ID) &&
	    (obsPath.getResourceId() == Constants.RES_TOTAL_BUDGET)) {
	    String strValue = ((LwM2mResource)response.getContent()).getValue().toString();
	    try {
		int newPowerBudget = Integer.parseInt(strValue);

		return newPowerBudget;
	    }
	    catch (Exception e) {
		System.out.println("Exception in reading demand response:" + e.getMessage());
	    }	       
	}
	return -1;
    }
    
    
    private static int readInteger(Registration registration, int objectId, int instanceId, int resourceId)
    {
        try {
	    ReadRequest request = new ReadRequest(objectId, instanceId, resourceId);
	    ReadResponse cResponse = lwServer.send(registration, request, 5000);
	    if (cResponse.isSuccess()) {
		String sValue = ((LwM2mResource)cResponse.getContent()).getValue().toString();
		try {
		    int iValue = Integer.parseInt(((LwM2mResource)cResponse.getContent()).getValue().toString());
		    return iValue;
		}
		catch (Exception e) {
		}
		float fValue = Float.parseFloat(((LwM2mResource)cResponse.getContent()).getValue().toString());
		return (int)fValue;
	    } else {
		return 0;
	    }
        }
        catch (Exception e) {
	    System.out.println(e.getMessage());
	    System.out.println("readInteger: exception");
	    return 0;
        }
    }
    
    private static String readString(Registration registration, int objectId, int instanceId, int resourceId)
    {
        try {
	    ReadRequest request = new ReadRequest(objectId, instanceId, resourceId);
	    ReadResponse cResponse = lwServer.send(registration, request, 1000);
	    if (cResponse.isSuccess()) {
		String value = ((LwM2mResource)cResponse.getContent()).getValue().toString();
		return value;
	    } else {
		return "";
	    }
        }
        catch (Exception e) {
	    System.out.println(e.getMessage());
	    System.out.println("readString: exception");
	    return "";
        }
    }
    
    private static void writeInteger(Registration registration, int objectId, int instanceId, int resourceId, int value)
    {
	try {
	    WriteRequest request = new WriteRequest(objectId, instanceId, resourceId, value);
	    WriteResponse cResponse = lwServer.send(registration, request, 1000);
	    if (cResponse.isSuccess()) {
		System.out.println("writeInteger: Success");
	    } else {
		System.out.println("writeInteger: Failed, " + cResponse.toString());
	    }
	}
	catch (Exception e) {
	    System.out.println(e.getMessage());
	    System.out.println("writeInteger: exception");
	}
    }
    
    private static void writeString(Registration registration, int objectId, int instanceId, int resourceId, String value)
    {
	try {
	    WriteRequest request = new WriteRequest(objectId, instanceId, resourceId, value);
	    WriteResponse cResponse = lwServer.send(registration, request, 1000);
	    if (cResponse.isSuccess()) {
		System.out.println("writeString: Success");
	    } else {
		System.out.println("writeString: Failed, " + cResponse.toString());
	    }
	}
	catch (Exception e) {
	    System.out.println(e.getMessage());
	    System.out.println("writeString: exception");
	}
    }
    
    private static void writeBoolean(Registration registration, int objectId, int instanceId, int resourceId, boolean value)
    {
	try {
	    WriteRequest request = new WriteRequest(objectId, instanceId, resourceId, value);
	    WriteResponse cResponse = lwServer.send(registration, request, 1000);
	    if (cResponse.isSuccess()) {
		System.out.println("writeBoolean: Success");
	    } else {
		System.out.println("writeBoolean: Failed, " + cResponse.toString());
	    }
	}
	catch (Exception e) {
	    System.out.println(e.getMessage());
	    System.out.println("writeBoolean: exception");
	}
    }
    
}
