# 📚 WordWidget (Kelime Widget)

**WordWidget**, Android ana ekranınızda İngilizce-Türkçe kelime çiftlerini şık ve asimetrik bir widget ile gösteren, hafif ve son derece stabil bir kelime öğrenme uygulamasıdır.

![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blue.svg)
![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

## ✨ Özellikler

### 📱 Widget Özellikleri
- **Ultra Stabil Saat**: Android'in yerel `TextClock` bileşeni kullanılarak, pil dostu ve %100 güvenilir saat gösterimi (Sistem 12/24 saat ayarına otomatik uyum sağlar).
- **Akıllı Yerleşim**: Kısa kelimeler yan yana, uzun kelimeler alt alta gösterilir. Kelimeler asla "..." ile kısaltılmaz, her zaman tam olarak okunur.
- **Tekrar Önleme**: Aynı kelime art arda iki kez gösterilmez.
- **Özelleştirilebilir Aralık**: Kelime değişim sıklığını 1 dakika ile 120 dakika arasında ayarlayabilirsiniz.

### 📝 Uygulama Özellikleri
- **Anlık Arama**: Yazdığınız anda kelimeler filtrelenir. Tek tuşla temizlenip son eklenen 10 kelimeye geri dönülür.
- **Hızlı Düzenleme**: Listede herhangi bir kelimeye tıklayarak anında düzenleyebilirsiniz.
- **Gelişmiş CSV Yönetimi**: 
  - Dışa aktarılan dosyalar otomatik olarak `KelimeWidget_YYYYMMDD_HHMM.csv` formatında zaman damgası ile kaydedilir.
  - İçe aktarırken **duplicate (yinelenen) kelime kontrolü** yapılır, aynı kelimeler tekrar eklenmez.
  - Türkçe karakterler (UTF-8) tam desteklenir.

## 📸 Ekran Görüntüleri

*(Buraya uygulamanızın ekran görüntülerini ekleyebilirsiniz. `screenshots` klasörü oluşturup resimleri yükledikten sonra aşağıdaki satırları güncelleyin)*

| Ana Ekran | Widget Görünümü | Ayarlar |
| :---: | :---: | :---: |
| ![Ana Ekran](screenshots/main.png) | ![Widget](screenshots/widget.png) | ![Ayarlar](screenshots/settings.png) |

## 🛠️ Teknoloji Yığını

- **Dil**: Kotlin
- **Mimari**: MVVM / Activity-based
- **Veri Saklama**: SharedPreferences (Hafif ve hızlı JSON/CSV işleme)
- **Arka Plan İşlemleri**: AlarmManager (`setExactAndAllowWhileIdle`) ve BroadcastReceiver
- **UI**: RecyclerView, AppWidgetProvider, TextClock

##  Katkıda Bulunma

Katkılarınıza açığız! Lütfen bir özellik istemek veya hata bildirmek için bir "Issue" açın veya bir "Pull Request" gönderin.

## 📜 Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.