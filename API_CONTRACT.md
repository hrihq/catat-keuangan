# API Contract — Catat Keuangan

Frontend: `app/src/main/java/com/keuangan/app/data/TransactionRepository.kt`
(berkarang membaca `assets/mock/*.json`; ganti 3 getter dengan endpoint di bawah).

## Standard envelope (semua endpoint)

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { },
  "error": null
}
```

Error:

```json
{
  "success": false,
  "message": "Deskripsi error",
  "data": null,
  "error": { "code": "VALIDATION_ERROR", "details": { "field": "amount" } }
}
```

Headers: `Authorization: Bearer <token>`, `Content-Type: application/json`, `Accept: application/json`.

## GET /api/v1/home/summary — Beranda

`data`: `{ "balance": 8450000, "income": 12000000, "expense": 3550000 }`

## GET /api/v1/home/budget — Beranda

`data`: `{ "limit": 5000000, "spent": 3550000, "month": "Nov 2026" }`

## GET /api/v1/transactions?limit=3 — Beranda / riwayat

`data`: array of

```json
{ "id": 1, "title": "Makan siang", "amount": 25000, "isIncome": false,
  "category": "Makanan", "date": "Hari ini", "note": "" }
```

## POST /api/v1/transactions — Catat transaksi

Body:

```json
{ "title": "Makan siang", "amount": 25000, "isIncome": false,
  "category": "Makanan", "date": "2026-11-05", "note": "" }
```

`data`: transaksi yang dibuat (bentuk sama dengan GET).
