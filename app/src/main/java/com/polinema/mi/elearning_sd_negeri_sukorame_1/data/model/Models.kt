package com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model

// ── AUTH & PROFIL USER ──────────────────────────────────
data class User(
    val uid: String = "",         // UID asli dari Firebase Auth
    val name: String? = "",
    val email: String? = "",
    val role: String? = "",       // "admin", "guru", "siswa", "wali_murid", "kepala_sekolah"
    val foto: String? = "",
    val noHp: String? = "",
    val tipeGuru: String? = null, // "umum", "mulok", "agama"
    val idSiswa: String? = null,  // Menunjuk ke ID Document di koleksi siswa
    val idGuru: String? = null,   // Menunjuk ke ID Document di koleksi guru
    val kelasId: String? = null   // Menunjuk ke ID Document di koleksi kelas
)

// ── DATA MASTER SEKOLAH ──────────────────────────────────
data class Sekolah(
    val npsn: String = "",
    val nama: String? = "",
    val alamat: String? = "",
    val akreditasi: String? = "",
    val kepalaSekolah: String? = ""
)

data class Kelas(
    val id: String = "",
    val namaKelas: String? = "",
    val tingkat: String? = "",
    val rombel: String? = ""
)

data class MataPelajaranData(
    val id: String = "",
    val nama: String = "",
    val kode: String = "",
    val jenis: String = ""        // "wajib" atau "mulok"
)

// ── SINKRONISASI ENTITAS ROLE ────────────────────────────
data class Siswa(
    val id: String = "",          // ID Document Firestore (e.g., "SISWA001")
    val userId: String = "",      // UID dari Firebase Auth milik si siswa
    val nisn: String? = "",
    val namaLengkap: String? = "",
    val jenisKelamin: String? = "",
    val tanggalLahir: String? = "",
    val kelasId: String? = null,
    val waliMuridId: String? = null // UID dari Firebase Auth milik si wali
)

data class Guru(
    val id: String = "",          // ID Document Firestore (e.g., "GURU001")
    val userId: String = "",      // UID dari Firebase Auth milik si guru
    val nip: String? = "",
    val tipeGuru: String? = ""    // "umum", "mulok", "agama"
)

// ── AKADEMIK & KBM ──────────────────────────────────────
data class Jadwal(
    val id: String = "",
    val kelasId: String? = "",
    val mapelId: String? = "",
    val guruId: String? = "",
    val hari: String? = "",
    val waktuMulai: String? = "",
    val waktuSelesai: String? = "",
    // FIX: Field denormalisasi yang disimpan admin saat buat jadwal
    val namaMapel: String? = "",
    val namaGuru: String? = "",
    val namaKelas: String? = ""
)

data class Pengumuman(
    val id: String = "",
    val judul: String? = "",
    val isi: String? = "",
    val kategori: String? = "",  // "Umum", "Akademik"
    val untuk: String? = "semua", // "semua", "siswa", "guru", "wali_murid"
    val tanggal: String? = "",
    val status: String? = ""     // "Aktif"
)

data class Materi(
    val id: String = "",
    val judul: String? = "",
    val deskripsi: String? = "",
    val urlVideo: String? = "",
    val namaMapel: String? = "",
    val namaGuru: String? = "",
    val kelasId: String? = "",
    val tipe: String? = "materi"  // "video" atau "materi"
)

// ── TUGAS & CBT (UJIAN) ──────────────────────────────────
data class Tugas(
    val id: String = "",
    val judul: String? = "",
    val namaMapel: String? = "",
    val deadline: String? = "",
    val jumlahSoal: Int? = 0,
    val durasi: Int? = null,
    val kelasId: String? = "",
    @field:JvmField               // Amankan penamaan boolean bertema 'sudah' di Firestore
    val sudahDikerjakan: Boolean = false
)

data class SoalRaw(
    val id: String = "",
    val soal: String? = "",
    val gambarSoal: String? = "",
    val pilihanA: String? = "",
    val pilihanB: String? = "",
    val pilihanC: String? = "",
    val pilihanD: String? = "",
    val jawabanBenar: String? = "" // "A", "B", "C", atau "D"
)

// FIX: Model lokal untuk tampilan CBT — tidak disimpan ke Firestore
data class PilihanData(
    val id: String = "",          // e.g. "SOAL001_A"
    val pilihan: String = "",     // "A", "B", "C", "D"
    val isiPilihan: String = "",  // teks isi pilihan
    val isSelected: Boolean = false
)

data class SoalData(
    val id: String = "",
    val pertanyaan: String = "",
    val gambarSoal: String? = null,
    val pilihan: List<PilihanData> = emptyList()
)

data class TugasDetail(
    val id: String = "",
    val judul: String? = "",
    val totalSoal: Int? = 0,
    val soal: List<SoalRaw>? = emptyList()
)

// ── DATA TRANSAKSIONAL (DINAMIS) ─────────────────────────
data class Absensi(
    val id: String = "",
    val siswaId: String? = "",
    val kelasId: String? = "",
    val tanggal: String? = "",
    val status: String? = "",     // "hadir", "sakit", "izin", "alpha"
    val keterangan: String? = "",
    // Variabel titipan lokal untuk mempermudah adapter RecyclerView di Android
    val namaSiswa: String? = "",
    val namaKelas: String? = ""
)

data class Nilai(
    val id: String = "",
    val siswaId: String = "",     // FIX: Sekarang sinkron dengan data seed transaksional!
    val namaMapel: String? = "",
    val jenisNilai: String? = "", // "Tugas 1", "UTS Ganjil", dll.
    val nilai: Double = 0.0,
    val semester: String? = ""
)

data class Rapor(
    val id: String = "",
    val siswaId: String = "",     // FIX: Sekarang sinkron, wali murid gak bakal kehilangan data anak!
    val semester: String? = "",
    val tahunAjaran: String? = "",
    val totalHadir: Int = 0,
    val totalSakit: Int = 0,
    val totalIzin: Int = 0,
    val totalAlpha: Int = 0,
    val catatanWali: String? = "",
    val statusNaik: String? = ""
)

data class HasilCbt(
    val id: String = "",
    val tugasId: String = "",
    val siswaId: String = "",
    val totalSoal: Int = 0,
    val jawabanBenar: Int = 0,
    val jawabanSalah: Int = 0,
    val jumlahDijawab: Int = 0,
    val nilai: Double = 0.0
)

data class SystemLog(
    val id: String = "",
    val activity: String = "",
    val user: String = "",        // Diisi email user yang beraktivitas
    val createdAt: String = ""
)