# Değişiklik Günlüğü

TROY "Şansına Merhaba" tablet uygulaması. Sürüm adı GitHub yayın etiketiyle aynıdır; APK'lar [Releases](https://github.com/kcandost/troy-sansina-merhaba/releases) sayfasındadır. Her sürüm mevcut uygulamanın üzerine kurulur; ayarlar ve sayaçlar korunur.

## v1.3.1 — 2026-09-08

- Ayarlar panelinde **"Uygulamadan çık"** düğmesi: uygulamayı kapatır, tablet ana ekranına döner (güncelleme kurarken veya başka uygulamaya geçerken). Simgeden yeniden başlatılır.
- Bu dosya (CHANGELOG.md) eklendi.

## v1.3.0 — 2026-09-08

Ajansın güncellediği TROY_ROZYLABS Figma dosyasına göre metin ve düzen güncellemeleri:

- **Kart seçimi (kare 6):** "Kartlardan birini seç, Troy'dan kazanacağın indirimi öğren."
- **Çevirme (kare 7):** "Hemen çevir, indirimi gör!"
- **Sonuç (kare 8–11):** yeni düzen — "Troy mağazalarında kullanabileceğin" / tutar / "indirim kazandın." / "Hemen QR'ı okut, indirim kodunu al." / QR.
- Davet ekranı (kare 5) ve arka plan değişmedi.

## v1.2.0 — 2026-09-07

- Panelde "Çevrimiçi" artık oyunun tablet ekranında olduğunu gösterir; tablet ana ekrana dönünce veya ekran kapanınca anında çevrimdışı görünür.
- Panel, robotun ne zamandır ekranda olduğunu ve bugün toplam ekran süresini gösterir; yeni "Ekran süresi" kartı (robot × 14 gün).
- Tablet uygulama sürümünü bildirir; panelde eski sürümdeki robotlar kırmızı sürüm çipiyle işaretlenir.
- Telegram bildirimleri (TROY Sansina grubu) çevrimdışı/çevrimiçi ve eski sürüm uyarıları için bu sürümü kullanır.

## v1.1.1 — 2026-09-05

- Uygulama silinip yeniden kurulduğunda görülen yanıltıcı "Sunucuya ulaşılamadı" mesajı düzeltildi: tablet artık "Bu cihaz filoda zaten kayıtlı" der ve panelden "serbest bırak" adımına yönlendirir.
- Filo panelinde her cihaz için "serbest bırak" eylemi; serbest bırakılan tablet aynı kimlikle ve geçmiş verileri korunarak yeniden tanıtılır.

## v1.1.0 — 2026-09-04

- Cihazlar 5 dakikada bir sinyal gönderir: panel çevrimiçi/çevrimdışı durumu cihazın gerçekten açık olup olmadığını gösterir (oyun sırasında da).
- Toplam gösterim sayıları kupon ayarı değişince sıfırlanmaz; panel kayıttan bu yana toplamları gösterir, limit sayaçları dönem bazında çalışmaya devam eder.

## v1.0.0 — 2026-09-04

- Filo paneli sürümü: kurulumda cihaz adlandırma, robot başına limit ve kampanya geneli kota, uzaktan kupon yönetimi.
- Supabase arka ucu: kupon kayıtları, uzaktan yapılandırma, çevrimdışı kuyruk.

## v0.3.0 — 2026-09-01

- Topluluk PR #1 (@menesnas): sabit robot adresi (sahada ayar gerekmez), basılı/sürüklenen dokunuşlarda da duraklatma penceresi canlı kalır, ASGI 400 hatasını önleyen boş POST gövdesi, durum geçişi günlükleri, zengin durum sorgusu (acil stop, engellenen navigasyon, şarj).
- Kullanım kılavuzu PDF güncellendi.

## v0.2.0 — 2026-08-31

- Robot dokunmatik duraklatma: ekrana her dokunuş eşleşen Saha temizlik robotunu durdurur; son dokunuştan 60 sn sonra devam eder, çökme sonrası güvenli kurtarma.

## v0.1.0 — 2026-08-31

- Kampanya açılış sürümü: TROY_ROZYLABS Figma tasarımlarına birebir uygulama (kare 5–11), 1920×1200 yatay tablet, Android 8.0+.
- Mağaza kullanım kılavuzu: docs/Troy_Sansina_Merhaba_Kullanim_Kilavuzu.pdf
