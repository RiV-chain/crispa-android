package org.mesh.app.crispa

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hbb20.CountryCodePicker
import org.mesh.app.crispa.models.DNSInfo
import org.mesh.app.crispa.models.config.SelectDNSInfoListAdapter
import org.mesh.app.crispa.models.config.Utils.Companion.deserializeStringList2DNSInfoSet
import org.mesh.app.crispa.models.config.Utils.Companion.ping
import org.mesh.app.crispa.models.config.Utils.Companion.serializeDNSInfoSet2StringList
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class DNSListActivity : AppCompatActivity() {

    companion object {
        val allDNS = emptyList<DNSInfo>()
    }

    var isLoading = true;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dns_list)
        setSupportActionBar(findViewById(R.id.toolbar))
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { _ ->
            addNewDNS()
        }
        val extras = intent.extras
        val dnsList = findViewById<ListView>(R.id.dnsList)
        val adapter = SelectDNSInfoListAdapter(this, arrayListOf(), mutableSetOf())
        dnsList.adapter = adapter
        val cd = deserializeStringList2DNSInfoSet(
            extras!!.getStringArrayList(MainActivity.DNS_LIST)!!
        )
        thread(start = true) {
            try {

                for (d in cd) {
                    var ping = ping(d.address.hostAddress, 53)
                    d.ping = ping
                }
                for (dns in allDNS) {
                    var ping = ping(dns.address.hostAddress, 53)
                    dns.ping = ping
                    runOnUiThread(
                        Runnable
                        {
                            adapter.addItem(dns)
                            adapter.sort()
                        }
                    )
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            runOnUiThread(
                Runnable
                {
                    val currentDNS = ArrayList(cd.sortedWith(compareBy { it.ping }))
                    adapter.addAll(0, currentDNS)
                    isLoading = false
                }
            )
        }

    }

    @Suppress("DEPRECATION")
    private fun addNewDNS() {
        val view: View = LayoutInflater.from(this).inflate(R.layout.new_dns_dialog, null)
        val countryCode: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.resources.configuration.locales[0].country
        } else {
            this.resources.configuration.locale.country
        }

        view.findViewById<CountryCodePicker>(R.id.ccp).setCountryForNameCode(countryCode)
        val ab: AlertDialog.Builder = AlertDialog.Builder(this)
        ab.setCancelable(true).setView(view)
        val ad = ab.show()
        val addButton = view.findViewById<Button>(R.id.add)
        addButton.setOnClickListener{
            val ipInput = view.findViewById<TextView>(R.id.ipInput)
            val ccpInput = view.findViewById<CountryCodePicker>(R.id.ccp)
            val ip = ipInput.text.toString().toLowerCase(Locale.ROOT)
            val ccp = ccpInput.selectedCountryNameCode
            thread(start = true) {
                try {
                    val di = DNSInfo(InetAddress.getByName("[$ip]"), ccp, "User DNS")
                    try {
                        val ping = ping(di.address.hostAddress, 53)
                        di.ping = ping
                    } catch (e: Throwable) {
                        di.ping = Int.MAX_VALUE
                    }

                    runOnUiThread {
                        val selectAdapter =
                            (findViewById<ListView>(R.id.dnsList).adapter as SelectDNSInfoListAdapter)
                        selectAdapter.addItem(0, di)
                        selectAdapter.notifyDataSetChanged()
                        ad.dismiss()
                    }
                } catch (e: UnknownHostException){
                    runOnUiThread {
                        ipInput.error = "Unknown DNS host. Enter valid IPv6 address."
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.save_dns, menu)
        val item = menu.findItem(R.id.saveItem) as MenuItem
        item.setActionView(R.layout.menu_save)
        val saveButton = item
            .actionView?.findViewById<Button>(R.id.saveButton)
        saveButton?.setOnClickListener {
            if(isLoading){
                return@setOnClickListener
            }
            val result = Intent(this, MainActivity::class.java)
            val adapter = findViewById<ListView>(R.id.dnsList).adapter as SelectDNSInfoListAdapter
            val selectedDNS = adapter.getSelectedDNS()
            result.putExtra(MainActivity.DNS_LIST, serializeDNSInfoSet2StringList(selectedDNS))
            setResult(Activity.RESULT_OK, result)
            finish()        }
        return true
    }
}