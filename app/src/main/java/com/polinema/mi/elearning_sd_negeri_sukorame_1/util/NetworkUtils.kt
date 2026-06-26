package com.polinema.mi.elearning_sd_negeri_sukorame_1.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.widget.TextView

object NetworkUtils {

    /** True jika ada koneksi internet aktif */
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps    = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Tampilkan/sembunyikan banner offline.
     * Panggil setelah API call gagal dengan exception jaringan.
     */
    fun showOfflineBanner(bannerView: View, messageView: TextView, message: String = "Tidak ada koneksi internet. Periksa jaringan Anda.") {
        messageView.text = message
        bannerView.visibility = View.VISIBLE
    }

    fun hideOfflineBanner(bannerView: View) {
        bannerView.visibility = View.GONE
    }

    /** Pesan ramah berdasarkan jenis exception */
    fun friendlyError(e: Exception, context: Context): String {
        return when {
            !isOnline(context)                             -> "Tidak ada koneksi internet. Periksa jaringan Anda."
            e is java.net.SocketTimeoutException           -> "Server tidak merespons (timeout). Coba lagi nanti."
            e is java.net.UnknownHostException             -> "Tidak dapat menghubungi server. Periksa URL API di pengaturan."
            e is java.net.ConnectException                 -> "Koneksi ke server gagal. Server mungkin sedang offline."
            else                                           -> "Terjadi kesalahan: ${e.message}"
        }
    }
}
