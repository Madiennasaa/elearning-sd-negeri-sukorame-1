package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.User
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentGuruInputAbsensiBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GuruInputAbsensiFragment : Fragment() {

    private var _binding: FragmentGuruInputAbsensiBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var sessionManager: SessionManager

    private var guruId = ""
    private var kelasId = ""
    private val statusMap = mutableMapOf<String, String>()
    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private var kelasList = mutableListOf<Kelas>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuruInputAbsensiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        val user = sessionManager.getUser()
        guruId = user?.uid ?: "" // Menggunakan UID dari User model satu atap

        if (guruId.isEmpty()) {
            Toast.makeText(requireContext(), "Data guru tidak ditemukan", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        fetchKelasInfo()
        
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun fetchKelasInfo() {
        db.collection("kelas")
            .whereEqualTo("guruId", guruId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Kelas::class.java)?.copy(id = doc.id)
                }
                if (list.isNotEmpty()) {
                    kelasList.clear()
                    kelasList.addAll(list)
                    
                    val classNames = kelasList.map { it.namaKelas ?: "Kelas ${it.id}" }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, classNames)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerKelas.adapter = adapter
                    
                    binding.spinnerKelas.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                            kelasId = kelasList[position].id
                            loadSiswa()
                        }
                        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                    }
                    
                    setupUI()
                } else {
                    Toast.makeText(requireContext(), "Guru belum memiliki kelas atau jadwal.", Toast.LENGTH_LONG).show()
                    binding.btnSimpanAbsen.isEnabled = false
                    binding.labelKelas.visibility = View.GONE
                    binding.spinnerKelas.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat data kelas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupUI() {
        updateTanggalDisplay()
        binding.tvTanggal.setOnClickListener { showDatePicker() }
        binding.btnSimpanAbsen.setOnClickListener { simpanAbsensi() }
    }

    private fun loadSiswa() {
        // Query ke koleksi users dengan filter role siswa dan kelasId
        db.collection("users")
            .whereEqualTo("role", "siswa")
            .whereEqualTo("kelasId", kelasId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val siswaList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(uid = doc.id)
                }
                if (siswaList.isEmpty()) {
                    Toast.makeText(requireContext(), "Tidak ada siswa di kelas ini", Toast.LENGTH_SHORT).show()
                    binding.btnSimpanAbsen.isEnabled = false
                } else {
                    binding.btnSimpanAbsen.isEnabled = true
                    siswaList.forEach { statusMap[it.uid] = "Hadir" }
                    binding.rvAbsensi.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvAbsensi.adapter = AbsensiInputAdapter(siswaList) { sId, status ->
                        statusMap[sId] = status
                    }
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat data siswa: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun simpanAbsensi() {
        if (statusMap.isEmpty()) {
            Toast.makeText(requireContext(), "Belum ada data absensi", Toast.LENGTH_SHORT).show()
            return
        }

        val batch = db.batch()
        
        statusMap.forEach { (siswaId, status) ->
            val docRef = db.collection("absensi").document()
            val data = hashMapOf(
                "siswaId" to siswaId,
                "kelasId" to kelasId,
                "tanggal" to selectedDate,
                "status" to status,
                "keterangan" to "",
                "tahunAjaran" to "2024/2025"
            )
            batch.set(docRef, data)
        }

        batch.commit()
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Absensi berhasil disimpan!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal menyimpan absensi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTanggalDisplay() {
        val displayFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        val dbDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)
        binding.tvTanggal.text = "📅 ${dbDate?.let { displayFormat.format(it) } ?: selectedDate}"
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)
            if (parsed != null) cal.time = parsed
        } catch (e: Exception) {}

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                updateTanggalDisplay()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class AbsensiInputAdapter(
        private val list: List<User>,
        private val onStatusChanged: (String, String) -> Unit
    ) : RecyclerView.Adapter<AbsensiInputAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvNama: TextView = view.findViewById(R.id.tvStudentName)
            val spinner: Spinner = view.findViewById(R.id.spinnerStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_absensi_input, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val user = list[position]
            holder.tvNama.text = user.name
            val statusOptions = listOf("Hadir", "Sakit", "Izin", "Alpha")

            val spinnerAdapter = object : ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                statusOptions
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent) as TextView
                    view.setTypeface(null, android.graphics.Typeface.BOLD)
                    when (getItem(position)) {
                        "Hadir" -> view.setTextColor(android.graphics.Color.parseColor("#43A047"))
                        "Sakit" -> view.setTextColor(android.graphics.Color.parseColor("#FB8C00"))
                        "Izin"  -> view.setTextColor(android.graphics.Color.parseColor("#1E88E5"))
                        "Alpha" -> view.setTextColor(android.graphics.Color.parseColor("#E53935"))
                        else    -> view.setTextColor(android.graphics.Color.BLACK)
                    }
                    return view
                }
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getDropDownView(position, convertView, parent) as TextView
                    view.setTextColor(android.graphics.Color.BLACK)
                    return view
                }
            }

            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            holder.spinner.adapter = spinnerAdapter
            holder.spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    onStatusChanged(user.uid, statusOptions[pos])
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }

        override fun getItemCount() = list.size
    }
}
