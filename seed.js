// seed.js - jalankan sekali buat isi data awal Firestore
const { initializeApp, cert } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const serviceAccount = require("./serviceAccount.json");

initializeApp({
  credential: cert(serviceAccount),
});

const db = getFirestore();

async function seed() {
  console.log("🔥 Mulai seeding data massal mengikuti model Kotlin terbaru...");

  // ── USERS ──────────────────────────────────────────────
  // PENTING: Gantilah nilai 'id' dan 'uid' di bawah ini dengan string UID asli dari Firebase Authentication milikmu!
  const users = [
    { id: "9uKY1rjBJTXwjtIKsydWMIfDoJh1",   data: { uid: "9uKY1rjBJTXwjtIKsydWMIfDoJh1",   name: "Admin E-Learning",           email: "admin@sdn.sch.id",   role: "admin",          foto: "", noHp: "081234567890", tipeGuru: null, idSiswa: null,     idGuru: null,     kelasId: null       }},
    { id: "UgZAGeK4aQhGlOQDK8GQIwN6J472",   data: { uid: "UgZAGeK4aQhGlOQDK8GQIwN6J472",   name: "Siti Mutmainah, S.Pd., M.Pd.", email: "kepsek@sdn.sch.id",  role: "kepala_sekolah", foto: "", noHp: "081234567891", tipeGuru: null, idSiswa: null,     idGuru: null,     kelasId: null       }},

    // GURU
    { id: "OHfDXmIGRtSa735INJcqYhachip1",   data: { uid: "OHfDXmIGRtSa735INJcqYhachip1",   name: "Nahartin, S.Pd",              email: "guru1@sdn.sch.id",   role: "guru",           foto: "", noHp: "085611112222", tipeGuru: "umum",  idSiswa: null, idGuru: "GURU001", kelasId: "KELAS001" }},
    { id: "qY2WpJgV4uMWi3Npxj2NYSwsSmT2",   data: { uid: "qY2WpJgV4uMWi3Npxj2NYSwsSmT2",   name: "Wayan Suastika, S.Pd.SD",     email: "guru2@sdn.sch.id",   role: "guru",           foto: "", noHp: "085633334444", tipeGuru: "mulok", idSiswa: null, idGuru: "GURU002", kelasId: null       }},
    { id: "zRE2y6FcQ6NNrzzAB8un9uAqGdt1",   data: { uid: "zRE2y6FcQ6NNrzzAB8un9uAqGdt1",   name: "Eko Prasetyo, S.Pd",          email: "guru3@sdn.sch.id",   role: "guru",           foto: "", noHp: "085655556666", tipeGuru: "umum",  idSiswa: null, idGuru: "GURU003", kelasId: "KELAS002" }},
    { id: "s6aV0YrsqFhpaQedwgQNjxHrOIV2",   data: { uid: "s6aV0YrsqFhpaQedwgQNjxHrOIV2",   name: "Ahmad Saefudin, S.Ag",        email: "guru4@sdn.sch.id",   role: "guru",           foto: "", noHp: "085677778888", tipeGuru: "agama", idSiswa: null, idGuru: "GURU004", kelasId: null       }},

    // SISWA & WALI
    { id: "xd3PzqIMj0aEwHBl7EH2DuASK563",   data: { uid: "xd3PzqIMj0aEwHBl7EH2DuASK563",   name: "Savitri Putri Utama",         email: "siswa1@sdn.sch.id",  role: "siswa",          foto: "", noHp: "",              tipeGuru: null, idSiswa: "SISWA001", idGuru: null,  kelasId: "KELAS001" }},
    { id: "XlZPyiWXEUWYM84NqAiQuh0vHik1",   data: { uid: "XlZPyiWXEUWYM84NqAiQuh0vHik1",   name: "Budi Santoso",                email: "wali1@sdn.sch.id",   role: "wali_murid",     foto: "", noHp: "082233445566", tipeGuru: null, idSiswa: "SISWA001", idGuru: null,     kelasId: null       }},

    { id: "s3yvdDVkgpbw5vUriyBMMEycIUi1",   data: { uid: "s3yvdDVkgpbw5vUriyBMMEycIUi1",   name: "Dimas Raditya",               email: "siswa2@sdn.sch.id",  role: "siswa",          foto: "", noHp: "",              tipeGuru: null, idSiswa: "SISWA002", idGuru: null,  kelasId: "KELAS001" }},
    { id: "OgW4yWg77MPVAXU0z1Vib4sbgqs2",   data: { uid: "OgW4yWg77MPVAXU0z1Vib4sbgqs2",   name: "Heri Setiawan",               email: "wali2@sdn.sch.id",   role: "wali_murid",     foto: "", noHp: "082255667788", tipeGuru: null, idSiswa: "SISWA002", idGuru: null,     kelasId: null       }},

    { id: "WQEEpdTMCCQ0XoigOE08Yxe1QJk2",   data: { uid: "WQEEpdTMCCQ0XoigOE08Yxe1QJk2",   name: "Rian Hidayat",                email: "siswa3@sdn.sch.id",  role: "siswa",          foto: "", noHp: "",              tipeGuru: null, idSiswa: "SISWA003", idGuru: null,  kelasId: "KELAS002" }},
    { id: "8soABVtVp7fbh269DOw3pFIbmm22",   data: { uid: "8soABVtVp7fbh269DOw3pFIbmm22",   name: "Joko Widodo D.",              email: "wali3@sdn.sch.id",   role: "wali_murid",     foto: "", noHp: "082299001122", tipeGuru: null, idSiswa: "SISWA003", idGuru: null,     kelasId: null       }},

    { id: "038MXhI6TtUXfDyNubvEyH2R9OX2",   data: { uid: "038MXhI6TtUXfDyNubvEyH2R9OX2",   name: "Amelia Kontesa",              email: "siswa4@sdn.sch.id",  role: "siswa",          foto: "", noHp: "",              tipeGuru: null, idSiswa: "SISWA004", idGuru: null,  kelasId: "KELAS003" }},
    { id: "Mxs7LR8b16WFDxff4t6rBgWq0a93",   data: { uid: "Mxs7LR8b16WFDxff4t6rBgWq0a93",   name: "Slamet Rahardjo",             email: "wali4@sdn.sch.id",   role: "wali_murid",     foto: "", noHp: "082211114444", tipeGuru: null, idSiswa: "SISWA004", idGuru: null,     kelasId: null       }}
  ];

  // ── KELAS ──────────────────────────────────────────────
  // FIX: Tambah field 'guruId' agar query whereEqualTo("guruId", ...) di fragment guru bisa bekerja
  const kelas = [
    { id: "KELAS001", data: { namaKelas: "1 (SATU)",  tingkat: "1", rombel: "A", guruId: "GURU001" }},
    { id: "KELAS002", data: { namaKelas: "2 (DUA)",   tingkat: "2", rombel: "A", guruId: "GURU003" }},
    { id: "KELAS003", data: { namaKelas: "3 (TIGA)",  tingkat: "3", rombel: "A", guruId: null       }},
    { id: "KELAS004", data: { namaKelas: "4 (EMPAT)", tingkat: "4", rombel: "A", guruId: null       }},
    { id: "KELAS005", data: { namaKelas: "5 (LIMA)",  tingkat: "5", rombel: "A", guruId: null       }},
    { id: "KELAS006", data: { namaKelas: "6 (ENAM)",  tingkat: "6", rombel: "A", guruId: null       }},
  ];

  // ── SISWA ──────────────────────────────────────────────
  const siswa = [
    { id: "SISWA001", data: { userId: "xd3PzqIMj0aEwHBl7EH2DuASK563", nisn: "01723451", namaLengkap: "Savitri Putri Utama", jenisKelamin: "Perempuan", tanggalLahir: "2018-04-12", kelasId: "KELAS001", waliMuridId: "XlZPyiWXEUWYM84NqAiQuh0vHik1" }},
    { id: "SISWA002", data: { userId: "s3yvdDVkgpbw5vUriyBMMEycIUi1", nisn: "01723452", namaLengkap: "Dimas Raditya",       jenisKelamin: "Laki-laki", tanggalLahir: "2018-02-20", kelasId: "KELAS001", waliMuridId: "OgW4yWg77MPVAXU0z1Vib4sbgqs2" }},
    { id: "SISWA003", data: { userId: "WQEEpdTMCCQ0XoigOE08Yxe1QJk2", nisn: "01623453", namaLengkap: "Rian Hidayat",        jenisKelamin: "Laki-laki", tanggalLahir: "2017-08-05", kelasId: "KELAS002", waliMuridId: "8soABVtVp7fbh269DOw3pFIbmm22" }},
    { id: "SISWA004", data: { userId: "038MXhI6TtUXfDyNubvEyH2R9OX2", nisn: "01523454", namaLengkap: "Amelia Kontesa",      jenisKelamin: "Perempuan", tanggalLahir: "2016-11-23", kelasId: "KELAS003", waliMuridId: "Mxs7LR8b16WFDxff4t6rBgWq0a93" }},
  ];

  // ── GURU ──────────────────────────────────────────────
  const guru = [
    { id: "GURU001", data: { userId: "OHfDXmIGRtSa735INJcqYhachip1", nip: "197906202022212004", tipeGuru: "umum"  }},
    { id: "GURU002", data: { userId: "qY2WpJgV4uMWi3Npxj2NYSwsSmT2", nip: "198412102014121002", tipeGuru: "mulok" }},
    { id: "GURU003", data: { userId: "zRE2y6FcQ6NNrzzAB8un9uAqGdt1", nip: "199103142023211005", tipeGuru: "umum"  }},
    { id: "GURU004", data: { userId: "s6aV0YrsqFhpaQedwgQNjxHrOIV2", nip: "198008022009011001", tipeGuru: "agama" }}
  ];

  // ── MATA PELAJARAN ─────────────────────────────────────
  // FIX: Nama variable diubah dari 'mapel' menjadi 'mata_pelajaran' agar konsisten
  const mata_pelajaran = [
    { id: "MAPEL001", data: { nama: "B. Indonesia",      kode: "BIN", jenis: "wajib" }},
    { id: "MAPEL002", data: { nama: "Matematika",        kode: "MTK", jenis: "wajib" }},
    { id: "MAPEL003", data: { nama: "PJOK",              kode: "PJK", jenis: "mulok" }},
    { id: "MAPEL004", data: { nama: "Pend. Agama Islam", kode: "PAI", jenis: "wajib" }},
    { id: "MAPEL005", data: { nama: "Pend. Pancasila",   kode: "PPN", jenis: "wajib" }},
    { id: "MAPEL006", data: { nama: "Seni Rupa",         kode: "SNR", jenis: "mulok" }},
    { id: "MAPEL007", data: { nama: "Bahasa Jawa",       kode: "BJW", jenis: "mulok" }},
    { id: "MAPEL008", data: { nama: "IPAS",              kode: "IPS", jenis: "wajib" }}
  ];

  // ── JADWAL ─────────────────────────────────────────────
  const jadwal = [
    // Jadwal Kelas 1 (KELAS001)
    { id: "JADWAL001", data: { kelasId: "KELAS001", mapelId: "MAPEL001", guruId: "GURU001", hari: "Senin",  waktuMulai: "07:30", waktuSelesai: "09:00", namaMapel: "B. Indonesia",      namaGuru: "Nahartin, S.Pd",          namaKelas: "1 (SATU)" }},
    { id: "JADWAL002", data: { kelasId: "KELAS001", mapelId: "MAPEL003", guruId: "GURU002", hari: "Senin",  waktuMulai: "09:30", waktuSelesai: "11:00", namaMapel: "PJOK",              namaGuru: "Wayan Suastika, S.Pd.SD", namaKelas: "1 (SATU)" }},
    { id: "JADWAL003", data: { kelasId: "KELAS001", mapelId: "MAPEL002", guruId: "GURU001", hari: "Selasa", waktuMulai: "07:30", waktuSelesai: "09:00", namaMapel: "Matematika",        namaGuru: "Nahartin, S.Pd",          namaKelas: "1 (SATU)" }},
    { id: "JADWAL004", data: { kelasId: "KELAS001", mapelId: "MAPEL004", guruId: "GURU004", hari: "Rabu",   waktuMulai: "07:30", waktuSelesai: "09:00", namaMapel: "Pend. Agama Islam", namaGuru: "Ahmad Saefudin, S.Ag",    namaKelas: "1 (SATU)" }},
    { id: "JADWAL005", data: { kelasId: "KELAS001", mapelId: "MAPEL005", guruId: "GURU001", hari: "Kamis",  waktuMulai: "07:30", waktuSelesai: "09:00", namaMapel: "Pend. Pancasila",   namaGuru: "Nahartin, S.Pd",          namaKelas: "1 (SATU)" }},
    { id: "JADWAL006", data: { kelasId: "KELAS001", mapelId: "MAPEL007", guruId: "GURU002", hari: "Jumat",  waktuMulai: "07:30", waktuSelesai: "09:00", namaMapel: "Bahasa Jawa",       namaGuru: "Wayan Suastika, S.Pd.SD", namaKelas: "1 (SATU)" }},

    // Jadwal Kelas 2 (KELAS002)
    { id: "JADWAL007", data: { kelasId: "KELAS002", mapelId: "MAPEL002", guruId: "GURU003", hari: "Senin",  waktuMulai: "07:30", waktuSelesai: "09:00", namaMapel: "Matematika",        namaGuru: "Eko Prasetyo, S.Pd",      namaKelas: "2 (DUA)"  }},
    { id: "JADWAL008", data: { kelasId: "KELAS002", mapelId: "MAPEL001", guruId: "GURU003", hari: "Selasa", waktuMulai: "07:30", waktuSelesai: "09:00", namaMapel: "B. Indonesia",      namaGuru: "Eko Prasetyo, S.Pd",      namaKelas: "2 (DUA)"  }},
    { id: "JADWAL009", data: { kelasId: "KELAS002", mapelId: "MAPEL006", guruId: "GURU003", hari: "Rabu",   waktuMulai: "07:30", waktuSelesai: "09:00", namaMapel: "Seni Rupa",         namaGuru: "Eko Prasetyo, S.Pd",      namaKelas: "2 (DUA)"  }}
  ];

  // ── PENGUMUMAN ─────────────────────────────────────────
  const pengumuman = [
    { id: "PENGUMUMAN001", data: { judul: "Selamat Datang di E-Learning Baru!", isi: "Aplikasi E-Learning resmi SDN Sukorame 1 Kediri berbasis Firebase siap digunakan untuk seluruh Guru, Siswa, dan Wali Murid.", kategori: "Umum", tanggal: "2026-06-01", status: "Aktif" }},
    { id: "PENGUMUMAN002", data: { judul: "Persiapan Libur Semester Ganjil", isi: "Diberitahukan kepada seluruh siswa bahwa libur semester akan dimulai tanggal 22 Juni 2026. Harap menyelesaikan semua tugas.", kategori: "Akademik", tanggal: "2026-06-10", status: "Aktif" }}
  ];

  // ── SEKOLAH ────────────────────────────────────────────
  const sekolah = [
    { id: "info", data: { npsn: "20534321", nama: "SD Negeri Sukorame 1 Kediri", alamat: "Jl. Raya Sukorame No. 1, Sukorame, Kec. Mojoroto, Kota Kediri, Jawa Timur", akreditasi: "A", kepalaSekolah: "Siti Mutmainah, S.Pd., M.Pd." }},
  ];

  // ── MATERI ─────────────────────────────────────────────
  const materi = [
    { id: "MATERI001", data: { judul: "Mengenal Huruf Alfabet dan Vokal", deskripsi: "Materi dasar membaca untuk kelas 1 SD. Silakan dipelajari berulang-ulang di rumah bersama orang tua.", urlVideo: "https://www.youtube.com/watch?v=xyz123", namaMapel: "B. Indonesia", namaGuru: "Nahartin, S.Pd", kelasId: "KELAS001", tipe: "video" }},
    { id: "MATERI002", data: { judul: "Penjumlahan Dasar 1-50", deskripsi: "Belajar menjumlahkan bilangan bulat menggunakan alat peraga lidi atau gambar menarik.", urlVideo: "", namaMapel: "Matematika", namaGuru: "Nahartin, S.Pd", kelasId: "KELAS001", tipe: "materi" }},
    { id: "MATERI003", data: { judul: "Pancasila sebagai Dasar Negara", deskripsi: "Menghafalkan 5 sila Pancasila beserta lambang-lambangnya dari bintang sampai padi kapas.", urlVideo: "https://www.youtube.com/watch?v=panca5", namaMapel: "Pend. Pancasila", namaGuru: "Eko Prasetyo, S.Pd", kelasId: "KELAS002", tipe: "video" }}
  ];

  // ── TUGAS + SOAL (sub-collection) ─────────────────────
  const tugas = [
    {
      id: "TUGAS001",
      data: { judul: "Ulangan Harian Matematika - Tambah Kurang", namaMapel: "Matematika", deadline: "2026-07-20", jumlahSoal: 3, durasi: 30, kelasId: "KELAS001", sudahDikerjakan: false },
      soal: [
        { id: "SOAL001", data: { soal: "Berapakah hasil akhir dari 5 + 4?",                                     gambarSoal: "", pilihanA: "7",    pilihanB: "8",    pilihanC: "9",      pilihanD: "10",  jawabanBenar: "C" }},
        { id: "SOAL002", data: { soal: "Ibu membeli 10 apel, lalu dimakan adik 3. Sisa berapa apel ibu?",       gambarSoal: "", pilihanA: "6",    pilihanB: "7",    pilihanC: "8",      pilihanD: "9",   jawabanBenar: "B" }},
        { id: "SOAL003", data: { soal: "Berapakah hasil dari 20 - 5?",                                          gambarSoal: "", pilihanA: "12",   pilihanB: "15",   pilihanC: "17",     pilihanD: "18",  jawabanBenar: "B" }},
      ]
    },
    {
      id: "TUGAS002",
      data: { judul: "Kuis Bahasa Indonesia - Membaca Suku Kata", namaMapel: "B. Indonesia", deadline: "2026-07-25", jumlahSoal: 2, durasi: 15, kelasId: "KELAS001", sudahDikerjakan: false },
      soal: [
        { id: "SOAL001", data: { soal: "Suku kata yang menyusun kata 'B-U-K-U' adalah...",                      gambarSoal: "", pilihanA: "Bu - ku", pilihanB: "B-uk-u", pilihanC: "Buk - u", pilihanD: "B - uku", jawabanBenar: "A" }},
        { id: "SOAL002", data: { soal: "Injektif kata 'B-A-P-A-K' dibaca...",                                   gambarSoal: "", pilihanA: "Bebek",   pilihanB: "Balon",  pilihanC: "Bapak",   pilihanD: "Baru",    jawabanBenar: "C" }}
      ]
    }
  ];

  // ── ABSENSI ───────────────────────────────────────────
  const absensi = [
    { id: "AB_001", data: { siswaId: "SISWA001", kelasId: "KELAS001", tanggal: "2026-06-01", status: "hadir", keterangan: "Masuk tepat waktu",       namaSiswa: "Savitri Putri Utama", namaKelas: "1 (SATU)" }},
    { id: "AB_002", data: { siswaId: "SISWA002", kelasId: "KELAS001", tanggal: "2026-06-01", status: "hadir", keterangan: "Masuk tepat waktu",       namaSiswa: "Dimas Raditya",       namaKelas: "1 (SATU)" }},
    { id: "AB_003", data: { siswaId: "SISWA001", kelasId: "KELAS001", tanggal: "2026-06-02", status: "sakit", keterangan: "Surat dokter terlampir",  namaSiswa: "Savitri Putri Utama", namaKelas: "1 (SATU)" }},
    { id: "AB_004", data: { siswaId: "SISWA002", kelasId: "KELAS001", tanggal: "2026-06-02", status: "hadir", keterangan: "Masuk tepat waktu",       namaSiswa: "Dimas Raditya",       namaKelas: "1 (SATU)" }},
    { id: "AB_005", data: { siswaId: "SISWA001", kelasId: "KELAS001", tanggal: "2026-06-03", status: "hadir", keterangan: "Masuk tepat waktu",       namaSiswa: "Savitri Putri Utama", namaKelas: "1 (SATU)" }},
    { id: "AB_006", data: { siswaId: "SISWA002", kelasId: "KELAS001", tanggal: "2026-06-03", status: "izin",  keterangan: "Acara keluarga",          namaSiswa: "Dimas Raditya",       namaKelas: "1 (SATU)" }},
  ];

  // ── NILAI ─────────────────────────────────────────────
  const nilai = [
    { id: "NILAI_001", data: { siswaId: "SISWA001", namaMapel: "Matematika",   jenisNilai: "Tugas 1",    nilai: 90.0, semester: "1" }},
    { id: "NILAI_002", data: { siswaId: "SISWA001", namaMapel: "Matematika",   jenisNilai: "UTS Ganjil", nilai: 85.5, semester: "1" }},
    { id: "NILAI_003", data: { siswaId: "SISWA001", namaMapel: "B. Indonesia", jenisNilai: "Tugas 1",    nilai: 95.0, semester: "1" }},
    { id: "NILAI_004", data: { siswaId: "SISWA002", namaMapel: "Matematika",   jenisNilai: "Tugas 1",    nilai: 75.0, semester: "1" }},
  ];

  // ── RAPOR ─────────────────────────────────────────────
  const rapor = [
    { id: "RAPOR_001", data: { siswaId: "SISWA001", semester: "1", tahunAjaran: "2025/2026", totalHadir: 18, totalSakit: 1, totalIzin: 0, totalAlpha: 0, catatanWali: "Savitri anak yang rajin dan cerdas, pertahankan prestasimu ya nak!", statusNaik: "Belum Waktunya" }},
    { id: "RAPOR_002", data: { siswaId: "SISWA002", semester: "1", tahunAjaran: "2025/2026", totalHadir: 17, totalSakit: 0, totalIzin: 2, totalAlpha: 0, catatanWali: "Dimas baik, tolong tingkatkan konsentrasi saat belajar berhitung.", statusNaik: "Belum Waktunya" }},
  ];

  // ── HASIL CBT ─────────────────────────────────────────
  const hasilCbt = [
    { id: "HCBT_001", data: { tugasId: "TUGAS001", siswaId: "SISWA001", totalSoal: 3, jawabanBenar: 2, jawabanSalah: 1, jumlahDijawab: 3, nilai: 66.67  }},
    { id: "HCBT_002", data: { tugasId: "TUGAS001", siswaId: "SISWA002", totalSoal: 3, jawabanBenar: 3, jawabanSalah: 0, jumlahDijawab: 3, nilai: 100.0  }},
  ];

  // ── SYSTEM LOG ────────────────────────────────────────
  const systemLog = [
    { id: "LOG_001", data: { activity: "User logged in to mobile system",   user: "guru1@sdn.sch.id", createdAt: "2026-06-14 07:00:12" }},
    { id: "LOG_002", data: { activity: "Downloaded Raport PDF",             user: "wali1@sdn.sch.id", createdAt: "2026-06-14 08:34:55" }},
    { id: "LOG_003", data: { activity: "Created new Task: TUGAS002",        user: "guru1@sdn.sch.id", createdAt: "2026-06-14 10:15:22" }},
  ];

  // ── HELPER FUNCTION ────────────────────────────────────
  async function insertCollection(collectionName, items) {
    const batch = db.batch();
    items.forEach(({ id, data }) => {
      batch.set(db.collection(collectionName).doc(id), data);
    });
    await batch.commit();
    console.log(`✅ ${collectionName} (${items.length} dokumen berhasil dimasukkan)`);
  }

  // ── RUN SEEDING ────────────────────────────────────────
  await insertCollection("users", users);
  await insertCollection("kelas", kelas);
  await insertCollection("siswa", siswa);
  await insertCollection("guru", guru);
  await insertCollection("mata_pelajaran", mata_pelajaran); // FIX: variable name sebelumnya 'mapel' (undefined)
  await insertCollection("jadwal", jadwal);
  await insertCollection("pengumuman", pengumuman);
  await insertCollection("sekolah", sekolah);
  await insertCollection("materi", materi);
  await insertCollection("absensi", absensi);
  await insertCollection("nilai", nilai);
  await insertCollection("rapor", rapor);
  await insertCollection("hasil_cbt", hasilCbt);
  await insertCollection("system_log", systemLog);

  // Tugas + sub-collection soal
  for (const t of tugas) {
    await db.collection("tugas").doc(t.id).set(t.data);
    for (const s of t.soal) {
      await db.collection("tugas").doc(t.id).collection("soal").doc(s.id).set(s.data);
    }
  }
  console.log(`✅ tugas + sub-collection soal berhasil disuntik!`);

  console.log("\n🎉 Seeding massal sukses total dan sudah sinkron 100%!");
  process.exit(0);
}

seed().catch((err) => {
  console.error("❌ Error:", err);
  process.exit(1);
});