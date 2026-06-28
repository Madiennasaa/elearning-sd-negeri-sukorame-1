package com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model

data class User(
    val uid: String = "",
    val name: String? = "",
    val email: String? = "",
    val role: String? = "",
    val foto: String? = "",
    val noHp: String? = "",
    val tipeGuru: String? = null,
    val idSiswa: String? = null,
    val idGuru: String? = null,
    val kelasId: String? = null,
    val nip: String? = null,
    val nisn: String? = null,
    val jenisKelamin: String? = null,
    val tanggalLahir: String? = null,
    val waliMuridId: String? = null
)

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
    val rombel: String? = "",
    val guruId: String? = null
)

data class MataPelajaranData(
    val id: String = "",
    val nama: String = "",
    val kode: String = "",
    val jenis: String = ""
)

data class Jadwal(
    val id: String = "",
    val kelasId: String? = "",
    val mapelId: String? = "",
    val guruId: String? = "",
    val hari: String? = "",
    val waktuMulai: String? = "",
    val waktuSelesai: String? = "",
    val namaMapel: String? = "",
    val namaGuru: String? = "",
    val namaKelas: String? = ""
)

data class Pengumuman(
    val id: String = "",
    val judul: String? = "",
    val isi: String? = "",
    val kategori: String? = "",
    val untuk: String? = "semua",
    val tanggal: String? = "",
    val status: String? = ""
)

data class Materi(
    val id: String = "",
    val judul: String? = "",
    val deskripsi: String? = "",
    val urlVideo: String? = "",
    val namaMapel: String? = "",
    val namaGuru: String? = "",
    val kelasId: String? = "",
    val tipe: String? = "materi"
)

data class Tugas(
    val id: String = "",
    val judul: String? = "",
    val namaMapel: String? = "",
    val deadline: String? = "",
    val jumlahSoal: Int? = 0,
    val durasi: Int? = null,
    val kelasId: String? = "",
    @field:JvmField
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
    val jawabanBenar: String? = ""
)

data class PilihanData(
    val id: String = "",
    val pilihan: String = "",
    val isiPilihan: String = "",
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

data class Absensi(
    val id: String = "",
    val siswaId: String? = "",
    val kelasId: String? = "",
    val tanggal: String? = "",
    val status: String? = "",
    val keterangan: String? = "",
    val namaSiswa: String? = "",
    val namaKelas: String? = ""
)

data class Nilai(
    val id: String = "",
    val siswaId: String = "",
    val namaMapel: String? = "",
    val jenisNilai: String? = "",
    val nilai: Double = 0.0,
    val semester: String? = ""
)

data class Rapor(
    val id: String = "",
    val siswaId: String = "",
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

data class JawabanCbt(
    val id: String = "",
    val jawaban: String? = "",
    val siswaId: String? = "",
    val soalId: String? = "",
    val tugasId: String? = ""
)

data class SystemLog(
    val id: String = "",
    val activity: String = "",
    val user: String = "",
    val createdAt: String = ""
)