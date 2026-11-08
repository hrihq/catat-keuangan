# Catat Keuangan — Component Breakdown (Beranda / Home)

Sumber desain: `Catat Keuangan - Desain Aplikasi.pdf` (hal. 1 = Beranda, hal. 4 = Elemen UI).
Stack: Kotlin + XML layout + Material 3 (`Theme.Material3.DayNight.NoActionBar`), warna semantik `?attr/`.

## Pohon layer

```
Activity (MainActivity)
└── FrameLayout
    ├── FragmentContainerView          ← Home / Statistik / Dompet / Profil
    ├── MaterialButton (fabAdd)        ← Atomic: FAB (kuning, squircle 20dp)
    └── MaterialCardView (bottomNav)   ← Atomic: nav pill (radius = height/2, elevation 6dp)
        └── 4 × Tab (icon + label)     ← Atomic: nav item (state aktif = bg oval)

Fragment (HomeFragment)
└── FrameLayout
    ├── NestedScrollView               ← paddingBottom 120dp, clipToPadding=false
    │   └── LinearLayout
    │       ├── HeaderGreeting         ← Atomic: greeting header + bell
    │       ├── BalanceCard            ← Atomic: kartu saldo biru + 2 PillLabel
    │       ├── BudgetCard             ← Atomic: kartu anggaran kuning + progress
    │       ├── SectionHeader          ← Atomic: "Transaksi terbaru" + "Lihat semua"
    │       └── transactionList        ← dynamic: N × TransactionRow
    ├── LoadingState                   ← Atomic: spinner + label
    └── EmptyState                     ← Atomic: pesan kosong / error
```

## Atomic components (`ui/components/`)

| Komponen | Peran | Props (`bind`) |
|---|---|---|
| `HeaderGreeting` | Icon dompet + sapaan + judul + lonceng | `greeting`, `title` |
| `BalanceCard` | Kartu saldo biru; total + Pemasukan/Pengeluaran | `subtitle`, `balance`, `income`, `expense`, `formatter` |
| `PillLabel` | Pill kecil di dalam kartu saldo | `title`, `value`, `valueColorRes` |
| `BudgetCard` | Kartu anggaran kuning + LinearProgressIndicator | `title`, `percentUsed`, `remaining`, `formatter` |
| `SectionHeader` | Judul seksi + aksi kanan | `title`, `action?` |
| `TransactionRow` | Baris transaksi (icon tile + judul/sub + nominal) | `title`, `subtitle`, `amountText`, `isIncome`, `iconRes`, `iconTint`, `iconBackground` |
| `IconTile` | Tile bulat berwarna untuk ikon | `iconRes`, `tintColorRes`, `backgroundTint` |
| `LoadingState` | State loading | — |
| `EmptyState` | State kosong / error | `message` |

## Data layer (`data/`)

- `TransactionItem` (model, sudah ada) — `id, title, amount, isIncome, category, date, note`.
- `TransactionParser` — JSON → model (`parseTransactions`, `parseSummary`, `parseBudget`).
- `TransactionRepository` — mock JSON dari `assets/mock/*.json`, `suspend`, `Dispatchers.IO`. **Ganti isi `getTransactions/getSummary/getBudget` dengan API call saat backend live; UI tidak berubah.**
- `assets/mock/transactions.json`, `summary.json`, `budget.json` — dummy data presisi sesuai desain (saldo 8.450.000; anggaran 71%).

## State

`HomeUiState`: `Loading → Success(HomeData) | Empty | Error`. Dimuat lewat `viewLifecycleOwner.lifecycleScope`; `MainActivity` memanggil `homeFragment.loadData()` setelah menambah transaksi.

## Palet (token desain)

Biru `#0B57D0`/`brand_blue #0052CC` · Kuning `#FFE08A`/`brand_yellow #FFE680` · Biru muda `#C2DAFF`/`brand_light_blue #D6E4FF` · Teal `#C4EED0`/`brand_teal #D4F1DE` · Krem `md_theme_light_background #FAF6ED` · Income `#146C2E` · Expense `#B3261E` (WCAG 4.5:1).

## Sisa pengerjaan

1. Halaman Catat transaksi (hal. 2) — bottom sheet sudah ada, tinggal dirapikan.
2. Halaman Statistik (hal. 3) — donut + bar sudah ada.
3. API contract + integrasi backend (repository sudah siap diganti).
