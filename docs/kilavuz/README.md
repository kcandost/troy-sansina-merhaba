# Kullanım Kılavuzu kaynağı

`kilavuz.html` — kılavuzun tek dosyalık HTML kaynağı (görseller data URI olarak gömülü).
PDF'i yeniden üretmek için:

```bash
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless --disable-gpu \
  --no-pdf-header-footer \
  --print-to-pdf=../Troy_Sansina_Merhaba_Kullanim_Kilavuzu.pdf kilavuz.html
```
