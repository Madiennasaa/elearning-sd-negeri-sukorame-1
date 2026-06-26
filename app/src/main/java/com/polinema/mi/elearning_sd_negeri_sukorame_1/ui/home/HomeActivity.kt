package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.ActivityHomeBinding
import com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.login.LoginActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var sessionManager: SessionManager

    private var currentRole: String = "siswa"
    private var currentUserId: String = ""
    private var currentSiswaId: String = ""
    private var currentKelasId: String = ""
    private var currentGuruId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        val user = sessionManager.getUser()

        if (user == null) {
            performLogout()
            return
        }

        currentRole    = user.role ?: "siswa"
        currentUserId  = user.uid
        currentSiswaId = user.idSiswa ?: ""
        currentKelasId = user.kelasId ?: ""
        currentGuruId  = user.idGuru ?: ""

        // Bottom nav hanya untuk siswa
        binding.bottomNavigation.visibility =
            if (currentRole == "siswa") View.VISIBLE else View.GONE

        setupBottomNavigation()
        setupTopBar()

        // Langsung load dashboard saat startup
        if (savedInstanceState == null) {
            loadDashboardByRole()
        }
    }

    // ── Top Bar ────────────────────────────────────────────────────────────────

    private fun setupTopBar() {
        binding.ivNotif.setOnClickListener {
            replaceFragment(NotificationFragment())
        }
        binding.ivProfile.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.menu_view_profile -> { replaceFragment(ProfileFragment()); true }
                    R.id.menu_logout       -> { performLogout(); true }
                    else -> false
                }
            }
            popup.show()
        }
    }

    fun performLogout() {
        sessionManager.logout()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    // ── Dashboard per role ─────────────────────────────────────────────────────

    private fun loadDashboardByRole() {
        val fragment = when (currentRole) {
            "guru"           -> DashboardGuruFragment()
            "admin"          -> DashboardAdminFragment()
            "wali_murid"      -> DashboardWaliFragment()
            "kepala_sekolah" -> DashboardKepalaFragment()
            else             -> DashboardFragment()
        }
        replaceFragment(fragment, isMainTab = true)
    }

    // ── Bottom Navigation ──────────────────────────────────────────────────────

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // Pastikan tidak re-load jika sudah di fragment yang sama (tanpa backstack)
            if (binding.bottomNavigation.selectedItemId == item.itemId && supportFragmentManager.backStackEntryCount == 0) {
                return@setOnItemSelectedListener false
            }

            // Bersihkan backstack secara sinkron agar tidak ada konflik transaksi
            supportFragmentManager.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            
            when (item.itemId) {
                R.id.navHome   -> { loadDashboardByRole(); true }
                R.id.navMateri -> { navigateToList("MATERI", fromNav = true); true }
                R.id.navTugas  -> { navigateToList("TUGAS", fromNav = true); true }
                else -> false
            }
        }
    }

    private var backPressedTime: Long = 0
    private lateinit var backToast: android.widget.Toast

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else if (binding.bottomNavigation.selectedItemId != R.id.navHome) {
            backToHome()
        } else {
            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                backToast.cancel()
                super.onBackPressed()
                return
            } else {
                backToast = android.widget.Toast.makeText(baseContext, "Tekan sekali lagi untuk keluar", android.widget.Toast.LENGTH_SHORT)
                backToast.show()
            }
            backPressedTime = System.currentTimeMillis()
        }
    }

    /**
     * Kembali ke Home dan pastikan bottom nav sinkron
     */
    fun backToHome() {
        if (binding.bottomNavigation.selectedItemId != R.id.navHome) {
            binding.bottomNavigation.selectedItemId = R.id.navHome
        } else {
            supportFragmentManager.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            loadDashboardByRole()
        }
    }

    // ── Navigate ───────────────────────────────────────────────────────────────

    fun navigateToList(type: String, fromNav: Boolean = false) {
        val siswaOnly = listOf("MATERI", "TUGAS", "HADIR", "NILAI", "RAPORT")
        if (type in siswaOnly && currentRole != "siswa") return

        // Jika ini adalah tab utama dan tidak dipicu dari bottom nav,
        // kita sinkronkan bottom nav selection-nya.
        if (!fromNav) {
            if (type == "MATERI" && binding.bottomNavigation.selectedItemId != R.id.navMateri) {
                binding.bottomNavigation.selectedItemId = R.id.navMateri
                return
            }
            if (type == "TUGAS" && binding.bottomNavigation.selectedItemId != R.id.navTugas) {
                binding.bottomNavigation.selectedItemId = R.id.navTugas
                return
            }
        }

        val fragment = when (type) {
            "MATERI" -> MateriFragment()
            "TUGAS"  -> TugasFragment()
            "HADIR"  -> AbsensiFragment()
            "NILAI"  -> NilaiFragment()
            "RAPORT" -> RaporFragment()
            "JADWAL" -> JadwalFragment()
            "SISWA"  -> ListFragment.newInstance("SISWA", currentKelasId, currentSiswaId)
            else     -> DashboardFragment()
        }
        
        // Cek apakah ini tab utama
        val isMainTab = (type == "MATERI" || type == "TUGAS")
        replaceFragment(fragment, isMainTab)
    }

    fun replaceFragment(fragment: Fragment, isMainTab: Boolean = false) {
        val existingArgs = fragment.arguments ?: Bundle()
        val bundle = Bundle().apply {
            putAll(existingArgs)
            putString("USER_ID",    currentUserId)
            putString("SISWA_ID",   currentSiswaId)
            putString("KELAS_ID",   currentKelasId)
            putString("GURU_ID",    currentGuruId)
            putString("USER_ROLE",  currentRole)
        }
        fragment.arguments = bundle
        
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            
        // Jika bukan tab utama (materi/tugas/home), masukkan ke backstack
        if (!isMainTab) {
            transaction.addToBackStack(null)
        }
        
        transaction.commit()
    }
}