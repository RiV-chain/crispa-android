package org.mesh.app.crispa.models.config

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.acra.ACRA
import org.mesh.app.crispa.models.DNSInfo
import org.mesh.app.crispa.models.PeerInfo
import java.lang.reflect.Type
import java.net.*


class Utils {

    companion object {

        @JvmStatic
        fun deserializeStringList2PeerInfoSet(list: List<String>?): MutableSet<PeerInfo> {
            var gson = Gson()
            var out = mutableSetOf<PeerInfo>()
            if (list != null) {
                for(s in list) {
                    out.add(gson.fromJson(s, PeerInfo::class.java))
                }
            }
            return out
        }

        @JvmStatic
        fun deserializeStringList2DNSInfoSet(list: List<String>?): MutableSet<DNSInfo> {
            var gson = Gson()
            var out = mutableSetOf<DNSInfo>()
            if (list != null) {
                for(s in list) {
                    out.add(gson.fromJson(s, DNSInfo::class.java))
                }
            }
            return out
        }

        @JvmStatic
        fun deserializeStringSet2PeerInfoSet(list: Set<String>): MutableSet<PeerInfo> {
            var gson = Gson()
            var out = mutableSetOf<PeerInfo>()
            for(s in list) {
                out.add(gson.fromJson(s, PeerInfo::class.java))
            }
            return out
        }

        @JvmStatic
        fun deserializeStringSet2DNSInfoSet(list: Set<String>): MutableSet<DNSInfo> {
            var gson = Gson()
            var out = mutableSetOf<DNSInfo>()
            for(s in list) {
                out.add(gson.fromJson(s, DNSInfo::class.java))
            }
            return out
        }

        @JvmStatic
        fun serializePeerInfoSet2StringList(list: Set<PeerInfo>): ArrayList<String> {
            var gson = Gson()
            var out = ArrayList<String>()
            for(p in list) {
                out.add(gson.toJson(p))
            }
            return out
        }

        @JvmStatic
        fun serializeDNSInfoSet2StringList(list: Set<DNSInfo>): ArrayList<String> {
            var gson = Gson()
            var out = ArrayList<String>()
            for(p in list) {
                out.add(gson.toJson(p))
            }
            return out
        }

        @JvmStatic
        fun ping(hostname: String, port:Int): Int {
            val start = System.currentTimeMillis()
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(hostname, port), 5000)
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
                print(hostname)
                return Int.MAX_VALUE
            }
            return (System.currentTimeMillis() - start).toInt()
        }

        @JvmStatic
        fun convertPeerInfoSet2PeerIdSet(list: Set<PeerInfo>): Set<String> {
            var out = mutableSetOf<String>()
            for(p in list) {
                out.add(p.toString())
            }
            return out
        }

        @JvmStatic
        fun convertPeer2PeerStringList(list: List<Peer>): ArrayList<String> {
            var out = ArrayList<String>()
            var gson = Gson()
            for(p in list) {
                out.add(gson.toJson(p))
            }
            return out
        }

        @JvmStatic
        fun deserializePeerString2PeerInfoSet(s: String): MutableSet<PeerInfo> {
            val gson = Gson()
            val listType: Type = object : TypeToken<ArrayList<Peer>>() {}.type
            ACRA.errorReporter.putCustomData("Peer list", s)
            val out = mutableSetOf<PeerInfo>()
            val peers: List<Peer> = gson.fromJson(s, listType)
            for (p in peers) {
                val fixedUrlString = if (p.remote.indexOf('%') > 0 && p.remote.indexOf(']') > 0) {
                    val fixWlanPart =
                        p.remote.substring(p.remote.indexOf('%'), p.remote.indexOf(']'))
                    p.remote.replace(fixWlanPart, "")
                } else {
                    p.remote
                }
                try {
                    val url = URI(fixedUrlString)
                    out.add(
                        PeerInfo(
                            url.scheme,
                            InetAddress.getByName(url.host),
                            url.port,
                            p.country_short,
                            p.multicast
                        )
                    )
                } catch (ex: URISyntaxException) {
                    //skip peer when Remote URL invalid:
                    //see https://github.com/yggdrasil-network/yggdrasil-go/issues/973
                    ex.printStackTrace()
                }
            }
            return out
        }
    }
}