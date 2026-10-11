# Uygulama Denemelerim

Android uygulama denemelerim — telefon üzerinde geliştirilmiş, `aapt2 + ecj + d8 + apksigner` derleme hattı ile derlenmiş projeler.

## Projeler

### 1. Bilimsel Hesap Makinesi
Bilimsel fonksiyonlar (üs, log, trigonometri vb.) içeren hesap makinesi uygulaması.

**Yapı:**
- `src/` — Java kaynak kodları
- `res/` — Layout, drawable, strings
- `AndroidManifest.xml` — Manifest
- `build.sh` — Derleme scripti

### 2. Görev Listesi
Yapılacaklar listesi tutmaya yarayan basit görev yönetim uygulaması.

**Yapı:**
- `src/` — Java kaynak kodları
- `res/` — Layout, drawable, strings
- `AndroidManifest.xml` — Manifest
- `build.sh` — Derleme scripti

## Derleme

Her proje klasöründe:
```bash
bash build.sh
```
Çıktı: `out/uygulama.apk`

---
*Bu projeler Ajantik (AI kod asistanı) ile geliştirilmiştir.*
