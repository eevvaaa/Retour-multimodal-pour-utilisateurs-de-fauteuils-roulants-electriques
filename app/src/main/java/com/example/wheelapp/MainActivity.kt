package com.example.wheelapp

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    val statusText : TextView = findViewById(R.id.StatusText)
    val enableBluetooth : Button = findViewById(R.id.enableBluetooth)
    val connectSystem : Button = findViewById(R.id.connect)
    val lvDiscovered: ListView = findViewById(R.id.discoveredDevice)
    val lvPaired : ListView = findViewById(R.id.pairedDevices)
    val scanBluetooth : ToggleButton = findViewById(R.id.toggleScan)
    var connectBluetooth: ConnectThread? = null
    var bluetoothDevice: BluetoothDevice? = null

    val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter

    val btArrayAdapter = ArrayAdapter<Any>(this, android.R.layout.simple_list_item_1)
    val btPairedDevice = ArrayAdapter<Any>(this,android.R.layout.simple_list_item_1)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        lvPaired.adapter = btPairedDevice
        lvDiscovered.adapter = btArrayAdapter

        //fonction a faire
        enableBluetooth.setOnClickListener(btnEnableBluetoothOnClickListener)
        scanBluetooth.setOnCheckedChangeListener(btnScanDeviceOnCheckedListener)
        connectSystem.setOnClickListener(btnConnectOnCLickListener)


        connectSystem.isEnabled = false


        //pb avec ACCESS_COARSE_LOCATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
            when (ContextCompat.checkSelfPermission(
                baseContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )) {
                PackageManager.PERMISSION_DENIED -> AlertDialog.Builder(this)
                    .setTitle("Runtime Permissions up ahead")
                    .setMessage("Permission is needed to scan for bluetooth devices")
                    .setPositiveButton("Okay", DialogInterface.OnClickListener { dialog, which ->
                        if (ContextCompat.checkSelfPermission(
                                baseContext,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                this@MainActivity,
                                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                                1
                            )
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
                PackageManager.PERMISSION_GRANTED -> {}
            }
        }

        CheckAndEnableBluetoothState()

        val filter : IntentFilter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        registerReceiver(ActionFoundReceiver, filter)

    }



    fun CheckAndEnableBluetoothState(){
        if(bluetoothAdapter == null){
            statusText.setText("Bluetooth NOT supported by phone")
            AlertDialog.Builder(this)
                .setTitle("Not compatible")
                .setMessage("Your phone does not support Bluetooth")
                .setPositiveButton("Exit") { dialog, which -> System.exit(0) }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        } else {
            if (bluetoothAdapter.isEnabled()) {
                //aucune idee de ce que c'est
                listPairedDevices()

                if (bluetoothAdapter.isDiscovering()) {     //PERMISSION CHECK
                    statusText.text = "Bluetooth is currently in device discovery process.";
                    scanBluetooth.isChecked = true
                } else {
                    statusText.text = "Bluetooth is Enabled."
                    enableBluetooth.isEnabled = false
                    scanBluetooth.isEnabled = true
                    scanBluetooth.isChecked = false
                }
            } else {
                statusText.text = "Bluetooth is Disabled."
                scanBluetooth.isChecked = false
                scanBluetooth.isEnabled = false
                connectSystem.isEnabled = false
                val enableBtIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestBluetooth.launch(enableBtIntent)


            }
        }
    }

    private fun listPairedDevices() {
        val pairedDevices = bluetoothAdapter.bondedDevices      //PERMISSION CHECK
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                btPairedDevice.add(""" ${device.name} ${device.address} """.trimIndent())
            }
            btPairedDevice.notifyDataSetChanged()
        }
    }

    private val btnEnableBluetoothOnClickListener =
        View.OnClickListener{
            fun onClick(view: View?) {
                CheckAndEnableBluetoothState()
            }
        }

    private val btnConnectOnCLickListener =
        View.OnClickListener {
        statusText.text = "Establishing connection..."
        connectBluetooth = ConnectThread(bluetoothDevice, bluetoothAdapter)
        connectBluetooth.start()}   //A VOIR DANS CLASSE CONNECTIONTHREAD


    @SuppressLint("SetTextI18n")
    val btnScanDeviceOnCheckedListener =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(
                        baseContext,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("Permission is needed to scan for bluetooth devices.\nPlease allow the App to access the device's location in order to proceed.")
                        .setPositiveButton(
                            "Okay"
                        ) { dialog, which ->
                            if (ContextCompat.checkSelfPermission(
                                    baseContext,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                ActivityCompat.requestPermissions(
                                    this,
                                    arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                                    1
                                )
                            }
                        }
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show()
                    scanBluetooth.isChecked = false
                } else {
                    btArrayAdapter.clear()
                    bluetoothAdapter.startDiscovery()   //PERMISSION CHECK
                    statusText.text = "Bluetooth is currently in device discovery process."
                }
            } else {
                bluetoothAdapter.cancelDiscovery() //PERMISSION CHECK
                statusText.text = "Bluetooth has ended discovery process."
            }
        }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                listPairedDevices()
                if (bluetoothAdapter.isDiscovering()) {     //PERMISSION CHECK
                    statusText.setText("Bluetooth is currently in device discovery process.")
                    scanBluetooth.setChecked(true)
                } else {
                    statusText.setText("Bluetooth is Enabled.")
                    enableBluetooth.setEnabled(false)
                    scanBluetooth.setEnabled(true)
                    scanBluetooth.setChecked(false)
                }
            } else {
                statusText.setText("Bluetooth is Disabled.")
                scanBluetooth.setChecked(false)
                scanBluetooth.setEnabled(false)
                connectSystem.setEnabled(false)
            }
        }
    }

    private val ActionFoundReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                btArrayAdapter.add(
                    """ ${device!!.name} ${device.address} """.trimIndent()) //PERMISSION CHECK
                btArrayAdapter.notifyDataSetChanged()
                if (device.name.matches("BlueMilk")) {      //CHANGER FORMAT REGEX
                    bluetoothDevice = device
                    connectSystem.isEnabled = true
                    bluetoothAdapter.cancelDiscovery()      //PERMISSION CHECK
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                scanBluetooth.isChecked = false
                statusText.text = "Bluetooth has ended discovery process."
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                if (bluetoothAdapter.isEnabled) {
                    enableBluetooth.isEnabled = false
                    scanBluetooth.isEnabled = true
                    statusText.text = "Bluetooth is Enabled."
                } else {
                    enableBluetooth.isEnabled = true
                    scanBluetooth.isEnabled = false
                    connectSystem.isEnabled = false
                    statusText.text = "Bluetooth is Disabled."
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED == action) {
                statusText.text = "Connected to Plant Watering System"
                if (connectBluetooth.mmSocket != null) {        //A VOIR AVEC CONNECTIONTHREAD
                    Log.d("Socket", "Successfully created!")
                    SocketSingleton.setSocket(connectBluetooth.mmSocket)
                    startActivity(Intent(this@MainActivity, PlantWateringActivity::class.java)) //CHANGER CLASSE POUR CORRESPINDRE PROJET
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                statusText.text = "Disconnected from Plant Watering System"
            }
        }
    }

}




    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            //granted
        }else{
            //deny
        }
    }


