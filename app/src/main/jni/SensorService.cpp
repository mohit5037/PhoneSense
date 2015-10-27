#include <jni.h>
#include <android/sensor.h>

#include <cassert>

// Constants
const int LOOPER_ID_USER = 3;
const int SENSOR_REFRESH_RATE = 100;

using namespace std;

class SensorService{

public:
    // Sensor Manager Declaration
	ASensorManager* sensorManager;
	
	// Sensor Declaration
	const ASensor* accelerometer;
	
	// Sensor event queue declaration
	ASensorEventQueue* accelerometerEventQueue;

	// Looper Object declaration
	ALooper* looper;
	
	// Structure to hold data
	struct AccelerometerData {
		double x;
		double y;
		double z;
	};
	

		
    SensorService() {}

    void init(){
        // Creating an object of sensor manager
        sensorManager = ASensorManager_getInstance();
        assert(sensorManager != NULL);

        // Getting accelerometer sensor
        accelerometer = ASensorManager_getDefaultSensor(sensorManager,
                            ASENSOR_TYPE_ACCELEROMETER);
        assert(accelerometer != NULL);

        // Getting a looper object
        looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
        assert(looper != NULL);

        // Getting a sensor event queue
        accelerometerEventQueue = ASensorManager_createEventQueue(sensorManager, looper,
                                                              LOOPER_ID_USER, NULL, NULL);
        assert(accelerometerEventQueue != NULL);

        // Setting the events rate to 1000 usec or 1ms
        int setEventRateResult = ASensorEventQueue_setEventRate(accelerometerEventQueue,
                                                            accelerometer,
                                                            int32_t(1000000 /
                                                                    SENSOR_REFRESH_RATE));

        // Enable Sensors
        int enableSensorResult = ASensorEventQueue_enableSensor(accelerometerEventQueue,
                                                            accelerometer);
        assert(enableSensorResult >= 0);
    }

    // Function to update sensor data
    int update(JNIEnv *env, jobject currentSensorValues){



        // Looping the looper
        ALooper_pollAll(0,NULL, NULL, NULL);

        // Declaring a sensor event
        ASensorEvent event;

        // initialize the class
        jclass c_Map = env->FindClass("java/util/TreeMap");


        if(ASensorEventQueue_getEvents(accelerometerEventQueue, &event, 1) > 0){

            float accelerometer_x = event.acceleration.x;
            float accelerometer_y = event.acceleration.y;
            float accelerometer_z = event.acceleration.z;


            // Get a class reference for java.lang.Float
            jclass classFloat = env->FindClass("java/lang/Float");


            // Allocate a jobjectArray of 2 java.lang.Float
            jobjectArray sensorDataArray = (env)->NewObjectArray( 3, classFloat, NULL);

            // Construct 2 Float objects by calling the constructor
            jmethodID midDoubleInit = (env)->GetMethodID(classFloat, "<init>", "(F)V");
            if (NULL == midDoubleInit) return -1;

            jobject acc_x = (env)->NewObject( classFloat, midDoubleInit, accelerometer_x);
            jobject acc_y = (env)->NewObject( classFloat, midDoubleInit, accelerometer_y);
            jobject acc_z = (env)->NewObject( classFloat, midDoubleInit, accelerometer_z);

            // Set to the jobjectArray
            (env)->SetObjectArrayElement( sensorDataArray, 0, acc_x);
            (env)->SetObjectArrayElement( sensorDataArray, 1, acc_y);
            (env)->SetObjectArrayElement( sensorDataArray, 2, acc_z);


            // Getting the method id
            jmethodID putMethodID = (env)->GetMethodID( c_Map, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

            (env)->CallObjectMethod(currentSensorValues, putMethodID, env->NewStringUTF("Accelerometer"), sensorDataArray);

            return 1;
        }

        return -1;
    }

    void stopCapture(){
        ASensorEventQueue_disableSensor(accelerometerEventQueue, accelerometer);
    }

};


// Creating an object of SensorService Class
SensorService sensorService;

extern "C" {

// Initialization function
JNIEXPORT void JNICALL
Java_com_probisticktechnologies_PhoneSense_SensorServiceNDK_init(JNIEnv *env, jclass type) {
    (void)type;
    sensorService.init();
}

JNIEXPORT jint JNICALL
Java_com_probisticktechnologies_PhoneSense_SensorServiceNDK_update(JNIEnv *env, jclass type, jobject currentSensorValues) {
    return sensorService.update(env, currentSensorValues);
}

JNIEXPORT void JNICALL
Java_com_probisticktechnologies_PhoneSense_SensorServiceNDK_stopCapture(JNIEnv *env, jclass type) {
    (void)env;
    (void)type;
    sensorService.stopCapture();
}

}