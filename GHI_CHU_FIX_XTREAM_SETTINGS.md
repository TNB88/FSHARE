# Ghi chu fix Xtream IPTV Settings v6

## Trieu chung

- Plugin Xtream IPTV tai va hien nguon binh thuong.
- Bam nut banh rang Settings lam CloudStream/Phim Rap crash.
- Mo lai app thi hien thong bao `Che do an toan duoc bat` va tat toan bo extension.

## Logcat chinh xac

```text
FATAL EXCEPTION: main
android.content.res.Resources$NotFoundException:
File res/layout/settings.xml from xml type layout resource ID #0x7f030003
Caused by: java.io.FileNotFoundException: res/layout/settings.xml
at com.anhdaden.XtreamIPTVSettingsFragment.getLayout
```

## Nguyen nhan

File `settings.xml` thuc te co trong goi, nhung ban local duoc dong ZIP tren Windows voi ten entry dung dau gach nguoc:

```text
res\layout\settings.xml
```

Android AssetManager chi tim duong dan chuan APK/ZIP bang dau gach xuoi:

```text
res/layout/settings.xml
```

Vi vay file co trong `.cs3` nhung Android van bao `FileNotFoundException`.

Ngoai ra method mo Settings trong `XtreamIPTVPlugin.smali` tung bi thay cach su dung thanh ghi. Da khoi phuc method `load$lambda$0` giong ban goc de tranh verifier/runtime khac biet. Rieng dieu kien nhan repository van giu `startsWith("http")` de ho tro CloudStream va Phim Rap doi ten.

## Cach sua dung

1. Doi chieu ban goc:
   `https://gitlab.com/tearrs/cloudstream-vietnamese/-/raw/main/XtreamIPTVProvider.cs3`
2. Disassemble `classes.dex` bang Baksmali 2.5.2.
3. Khoi phuc method `load$lambda$0` trong `XtreamIPTVPlugin.smali` theo ban goc.
4. Assemble lai bang Smali 2.5.2.
5. Khong dong `.cs3` bang `Compress-Archive` tren Windows vi no tao entry co dau `\`.
6. Dong goi bang Java `jar` de ten entry luon dung dau `/`:

```powershell
Push-Location <thu_muc_da_giai_nen>
jar --create --file XtreamIPTVProvider.cs3 --no-manifest manifest.json classes.dex res resources.arsc
Pop-Location
```

7. Kiem tra truoc khi phat hanh:

```powershell
tar -tf XtreamIPTVProvider.cs3
```

Ket qua bat buoc phai co:

```text
res/layout/settings.xml
```

Khong duoc co:

```text
res\layout\settings.xml
```

8. Tang version, cap nhat `fileSize` va `fileHash` SHA-256 trong `plugins.json`.

## Ban da phat hanh

- Version: `6`
- File: `XtreamIPTVProvider.cs3`
- Commit: `0bb5697 Fix Xtream settings resource paths`
- SHA-256: `a4f3f43ec99d88c5e857c4897a0b9d4f4b1a114c16802af838162e7355d7020f`
- Ban sao luu: `C:\Users\Admin\Desktop\fshare plugin\XtreamIPTVProvider-localfix-v6-settings-resourcefix.cs3`

## Cach test

1. Mo repo extension va xac nhan Xtream IPTV hien v6.
2. Neu app dang Safe Mode, dong va mo lai app mot lan sau khi v6 da cai.
3. Bam banh rang cua Xtream IPTV.
4. Phai hien `Xtream IPTV Settings`, `Add link`, `List link`.
5. Logcat khong duoc co `FATAL EXCEPTION`, `Resources$NotFoundException` hoac `VerifyError`.
