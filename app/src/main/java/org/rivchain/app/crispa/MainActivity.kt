package org.mesh.app.crispa

import android.app.Activity
import android.app.ActivityManager
import android.content.*
import android.graphics.Color
import android.net.VpnService
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.preference.PreferenceManager
import dalvik.system.DexFile
import mobile.Mobile
import org.mesh.app.crispa.models.DNSInfo
import org.mesh.app.crispa.models.PeerInfo
import org.mesh.app.crispa.models.config.DNSInfoListAdapter
import org.mesh.app.crispa.models.config.PeerInfoListAdapter
import org.mesh.app.crispa.models.config.Utils.Companion.deserializePeerString2PeerInfoSet
import org.mesh.app.crispa.models.config.Utils.Companion.deserializeStringList2DNSInfoSet
import org.mesh.app.crispa.models.config.Utils.Companion.deserializeStringList2PeerInfoSet
import org.mesh.app.crispa.models.config.Utils.Companion.deserializeStringSet2DNSInfoSet
import org.mesh.app.crispa.models.config.Utils.Companion.deserializeStringSet2PeerInfoSet
import org.mesh.app.crispa.models.config.Utils.Companion.serializeDNSInfoSet2StringList
import org.mesh.app.crispa.models.config.Utils.Companion.serializePeerInfoSet2StringList
import java.io.ByteArrayOutputStream
import java.net.URL
import java.nio.charset.Charset
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    companion object {
        const val STATIC_IP = "STATIC_IP_FLAG"
        const val privateKey = "privateKey"
        const val publicKey = "publicKey"
        const val COMMAND = "COMMAND"
        const val STOP = "STOP"
        const val START = "START"
        const val UPDATE_DNS = "UPDATE_DNS"
        const val PARAM_PINTENT = "pendingIntent"
        const val STATUS_START = 7
        const val STATUS_FINISH = 8
        const val STATUS_STOP = 9
        const val IPv6: String = "IPv6"
        const val PEER_LIST_CODE = 1000
        const val DNS_LIST_CODE = 2000
        const val VPN_LIST_CODE = 3000
        const val PEER_LIST = "PEERS_LIST"
        const val DNS_LIST = "DNS_LIST"
        const val CURRENT_PEERS = "CURRENT_PEERS_v1.2.1"
        const val CURRENT_DNS = "CURRENT_DNS_v1.2"
        const val START_VPN = "START_VPN"
        const val REST_URL = "http://localhost:19019/api/"
        private const val TAG = "Mesh"
        private const val VPN_REQUEST_CODE = 0x0F

        @JvmStatic
        var isStarted = false
        @JvmStatic
        var isCancelled = false
        @JvmStatic
        var address: String? = ""

        lateinit var wifiManager: WifiManager
    }

    private var currentPeers = setOf<PeerInfo>()
    private var currentDNS = setOf<DNSInfo>()
    private var networkStateReceiver: BroadcastReceiver? = null
    private lateinit var switchOn: ImageButton
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        isStarted = isYggServiceRunning(this)
        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        //save to shared preferences
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(this.baseContext)
        val staticIP = findViewById<SwitchCompat>(R.id.staticIP)
        staticIP.isChecked =
            preferences.getString(STATIC_IP, null) != null
        val peersListView = findViewById<ListView>(R.id.peers)

        currentPeers = deserializeStringSet2PeerInfoSet(
            preferences.getStringSet(
                CURRENT_PEERS,
                HashSet()
            )!!
        )
        val adapter = PeerInfoListAdapter(this, currentPeers.sortedWith(compareBy { it.ping }))
        peersListView.adapter = adapter

        if (adapter.count > 10) {
            val item = adapter.getView(0, null, peersListView)
            item.measure(0, 0)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (10 * item.measuredHeight).toInt()
            )
            peersListView.layoutParams = params
        }

        if(isStarted && this.currentPeers.isEmpty()) {
            updatePeers()
        }

        val editPeersButton = findViewById<Button>(R.id.edit)
        editPeersButton.setOnClickListener {
            if(isStarted){
                showToast("Service is running. Please stop service before edit Peers list")
                return@setOnClickListener
            }
            val intent = Intent(this@MainActivity, PeerListActivity::class.java)
            intent.putStringArrayListExtra(PEER_LIST, serializePeerInfoSet2StringList(currentPeers))
            startActivityForResult(intent, PEER_LIST_CODE)
        }

        val listViewDNS = findViewById<ListView>(R.id.dns)
        currentDNS = deserializeStringSet2DNSInfoSet(
            preferences.getStringSet(
                CURRENT_DNS,
                HashSet()
            )!!
        )
        val adapterDns = DNSInfoListAdapter(this, currentDNS.sortedWith(compareBy { it.ping }))
        listViewDNS.adapter = adapterDns
        val editDnsButton = findViewById<Button>(R.id.editDNS)
        editDnsButton.setOnClickListener {
            if(!isStarted){
                showToast("Service is not running. DNS ping will not be run")
                return@setOnClickListener
            }
            val intent = Intent(this@MainActivity, DNSListActivity::class.java)
            intent.putStringArrayListExtra(DNS_LIST, serializeDNSInfoSet2StringList(currentDNS))
            startActivityForResult(intent, DNS_LIST_CODE)
        }
        val editVPNButton = findViewById<Button>(R.id.editVPN)
        editVPNButton.setOnClickListener {
            val intent = Intent(this@MainActivity, VpnActivity::class.java)
            startActivityForResult(intent, VPN_LIST_CODE)
        }
        val nodeInfoButton = findViewById<Button>(R.id.nodeInfo)
        nodeInfoButton.setOnClickListener {
            if(isStarted) {
                val intent = Intent(this@MainActivity, CopyLocalNodeInfoActivity::class.java)
                intent.putExtra(IPv6, findViewById<TextView>(R.id.ip).text.toString())
                startActivity(intent)
            }
        }
        if(isStarted){
            val ipLayout = findViewById<LinearLayout>(R.id.ipLayout)
            ipLayout.visibility = View.VISIBLE
            findViewById<TextView>(R.id.ip).text = address
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val sourceDir: String = this.applicationInfo.sourceDir
            val dexFile = DexFile(sourceDir)
            val cl = classLoader
            val c: Class<*> = dexFile.loadClass("dummy/Dummy", cl)
        }
        val versionName = findViewById<Button>(R.id.about)
        """version:${BuildConfig.VERSION_NAME} build:${BuildConfig.VERSION_CODE} core:${Mobile.getVersion()}""".also { versionName.text = it }
        versionName.setOnClickListener {
            val intent = Intent(this@MainActivity, AboutActivity::class.java)
            startActivity(intent)
        }

        switchOn = findViewById(R.id.onOff)
        switchOn.setOnClickListener {
            if(isCancelled){
                isCancelled = false
                return@setOnClickListener
            }
            switchOn.background = resources.getDrawable(R.drawable.circle_button_inprogress)
            val animation = AnimationUtils.loadAnimation(this, R.anim.rotate_animation)
            animation.repeatCount = Animation.INFINITE
            animation.repeatMode = Animation.RESTART
            switchOn.startAnimation(animation)
            status = findViewById(R.id.status)
            status.text = "Connecting..."
            if (!isStarted) {
                startVpn()
            } else {
                stopVpn()
            }
        }
    }

    private fun stopVpn(){
        Log.i(TAG, "Stop")
        val intent = Intent(this, MeshTunService::class.java)
        val TASK_CODE = 100
        val pi = createPendingResult(TASK_CODE, intent, 0)
        intent.putExtra(PARAM_PINTENT, pi)
        intent.putExtra(COMMAND, STOP)
        startService(intent)
    }

    private fun startVpn(){
        Log.i(TAG, "Start")
        val intent= VpnService.prepare(this)
        if (intent!=null){
            startActivityForResult(intent, VPN_REQUEST_CODE)
        }else{
            onActivityResult(VPN_REQUEST_CODE, Activity.RESULT_OK, null)
        }
    }

    private fun updateDNS(){
        Log.i(TAG, "Update DNS")
        val intent = Intent(this, MeshTunService::class.java)
        val TASK_CODE = 100
        val pi = createPendingResult(TASK_CODE, intent, 0)
        intent.putExtra(PARAM_PINTENT, pi)
        intent.putExtra(COMMAND, UPDATE_DNS)
        intent.putStringArrayListExtra(CURRENT_DNS, serializeDNSInfoSet2StringList(currentDNS))
        startService(intent)
    }

    private fun updatePeers(){
        Log.d(TAG, "Update Peers")
        //moved to REST api
        var peers = downloadJson("$REST_URL/peers")
        //update UI
        val meshPeers = deserializePeerString2PeerInfoSet(peers)
        val listView = findViewById<ListView>(R.id.peers)
        val adapter = PeerInfoListAdapter(
            this@MainActivity,
            meshPeers
                .sortedWith(compareBy { !it.isMeshPeer })
                .sortedWith(compareBy { it.ping })
        )
        runOnUiThread {
            listView.adapter = adapter
        }
    }

    fun downloadJson(link: String): String {
        URL(link).openStream().use { input ->
            val outStream = ByteArrayOutputStream()
            outStream.use { output ->
                input.copyTo(output)
            }
            return String(outStream.toByteArray(), Charset.forName("UTF-8"))
        }
    }

    private fun checkPeers(){
        //this is Mesh mode, send Peers update every 5 sec
        thread(start = true) {
            while(true) {
                Thread.sleep(5000)
                if(isStarted) {
                    try {
                        updatePeers()
                    } catch (e: Exception){
                        runOnUiThread {
                            showToast("Could not get peers.$e")
                        }
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VPN_REQUEST_CODE && resultCode== Activity.RESULT_OK){

            val intent = Intent(this, MeshTunService::class.java)
            val TASK_CODE = 100
            val pi = createPendingResult(TASK_CODE, intent, 0)
            intent.putExtra(PARAM_PINTENT, pi)
            intent.putExtra(COMMAND, START)
            intent.putStringArrayListExtra(
                CURRENT_PEERS, serializePeerInfoSet2StringList(
                    currentPeers
                )
            )
            intent.putStringArrayListExtra(CURRENT_DNS, serializeDNSInfoSet2StringList(currentDNS))
            intent.putExtra(STATIC_IP, findViewById<SwitchCompat>(R.id.staticIP).isChecked)

            startService(intent)
        }
        if (requestCode == VPN_REQUEST_CODE && resultCode== Activity.RESULT_CANCELED){
            isCancelled = true
        }
        if (requestCode == PEER_LIST_CODE && resultCode== Activity.RESULT_OK){
            if(data!!.extras!=null){
                var currentPeers = data.extras!!.getStringArrayList(PEER_LIST)
                /*WiFi Direct test. need peer empty list*/
                this.currentPeers = deserializeStringList2PeerInfoSet(currentPeers)
                val adapter = PeerInfoListAdapter(
                    this,
                    this.currentPeers.sortedWith(compareBy { it.ping })
                )
                val listView = findViewById<ListView>(R.id.peers)
                listView.adapter = adapter

                //save to shared preferences
                val preferences =
                    PreferenceManager.getDefaultSharedPreferences(this.baseContext)
                preferences.edit().putStringSet(CURRENT_PEERS, currentPeers!!.toHashSet()).apply()
                if(isStarted){
                    //TODO implement UpdateConfig method in native interface and apply peer changes
                    stopVpn()
                    val i = baseContext.packageManager
                        .getLaunchIntentForPackage(baseContext.packageName)
                    i!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    i.putExtra(START_VPN, true)
                    startActivity(i)
                }
            }
        }

        if (requestCode == DNS_LIST_CODE && resultCode== Activity.RESULT_OK){
            if(data!!.extras!=null){
                var currentDNS = data.extras!!.getStringArrayList(DNS_LIST)
                this.currentDNS = deserializeStringList2DNSInfoSet(currentDNS)
                val adapter = DNSInfoListAdapter(
                    this,
                    this.currentDNS.sortedWith(compareBy { it.ping })
                )
                val listView = findViewById<ListView>(R.id.dns)
                listView.adapter = adapter
                //save to shared preferences
                val preferences =
                    PreferenceManager.getDefaultSharedPreferences(this.baseContext)
                preferences.edit().putStringSet(CURRENT_DNS, currentDNS!!.toHashSet()).apply()
                if(isStarted){
                    updateDNS()
                }
            }
        }

        when (resultCode) {
            STATUS_START -> {
                print("service started")
                checkPeers()
            }
            STATUS_FINISH -> {
                isStarted = true
                val ipLayout = findViewById<LinearLayout>(R.id.ipLayout)
                ipLayout.visibility = View.VISIBLE
                address = data!!.getStringExtra(IPv6)
                findViewById<TextView>(R.id.ip).text = address
                Thread {
                    Thread.sleep(500)
                    switchOn.clearAnimation()
                    runOnUiThread {
                        switchOn.background = resources.getDrawable(R.drawable.circle_button_on)
                        status.text = "ON"
                        status.setTextColor(Color.GREEN)
                    }
                }.start()
            }
            STATUS_STOP -> {
                isStarted = false
                val ipLayout = findViewById<LinearLayout>(R.id.ipLayout)
                ipLayout.visibility = View.GONE
                Thread {
                    Thread.sleep(500)
                    switchOn.clearAnimation()
                    runOnUiThread {
                        switchOn.background = resources.getDrawable(R.drawable.circle_button_off)
                        status.text = "OFF"
                        status.setTextColor(Color.WHITE)
                    }
                }.start()
            }
            else -> { // Note the block

            }
        }
    }

    private fun showToast(text: String){
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(applicationContext, text, duration)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }

    //TODO reimplement it
    private fun isYggServiceRunning(context: Context): Boolean {
        if(this.intent.hasExtra(MeshTunService.IS_VPN_SERVICE_STOPPED)){
            return !this.intent.getBooleanExtra(MeshTunService.IS_VPN_SERVICE_STOPPED, true)
        }
        val manager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (MeshTunService::class.java.getName() == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(this.baseContext)
        findViewById<SwitchCompat>(R.id.staticIP).isChecked =
            preferences.getString(STATIC_IP, null) != null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (networkStateReceiver != null){
            unregisterReceiver(networkStateReceiver);
        }
    }





    protected fun setWiFiMulticastLock(enable: Boolean) {
        var multicastLock = wifiManager.createMulticastLock(javaClass.simpleName)

        if (enable) {
            if (multicastLock.isHeld()) {
                //log.warning("WiFi multicast lock already acquired")
            } else {
                //log.info("WiFi multicast lock acquired")
                multicastLock.acquire()
            }
        } else {
            if (multicastLock.isHeld()) {
                //log.info("WiFi multicast lock released")
                multicastLock.release()
            } else {
                //log.warning("WiFi multicast lock already released")
            }
        }
    }

}
