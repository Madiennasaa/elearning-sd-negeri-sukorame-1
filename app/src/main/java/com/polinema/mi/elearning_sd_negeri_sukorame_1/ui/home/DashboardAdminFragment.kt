package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentDashboardAdminBinding
import kotlinx.coroutines.launch

class DashboardAdminFragment : Fragment() {

    private var _binding: FragmentDashboardAdminBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = SessionManager(requireContext()).getUser()

        // BUG FIX: Sebelumnya pakai findViewWithTag("tvGreeting") yang tidak pernah ketemu
        // karena layout admin memakai id tvTitle, bukan tvGreeting dan tidak ada tag.
        binding.tvTitle.text    = user?.name ?: "Administrator"
        binding.tvSubtitle.text = "Sistem Kendali E-Learning"

        val homeActivity = activity as? HomeActivity
        binding.menuManageUsers.setOnClickListener    { homeActivity?.replaceFragment(AdminManageUserFragment()) }
        binding.menuManageAnnounce.setOnClickListener { homeActivity?.replaceFragment(AdminManageAnnounceFragment()) }
        binding.menuManageSchool.setOnClickListener   { homeActivity?.replaceFragment(AdminSchoolInfoFragment()) }
        binding.menuManageJadwal.setOnClickListener   { homeActivity?.replaceFragment(AdminManageJadwalFragment()) }
        binding.menuManageSiswa.setOnClickListener    { homeActivity?.replaceFragment(AdminManageSiswaFragment()) }
        binding.menuManageGuru.setOnClickListener     { homeActivity?.replaceFragment(AdminManageGuruFragment()) }
        binding.menuManageKelas.setOnClickListener    { homeActivity?.replaceFragment(AdminManageKelasFragment()) }
        binding.menuManageMapel.setOnClickListener    { homeActivity?.replaceFragment(AdminManageMapelFragment()) }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
