# Ghi chu fix dang nhap FshareProvider v56

## Trieu chung

- Mo Settings khong con crash, nhung dang nhap tai khoan Fshare that bai.
- Tai khoan va mat khau dung van khong tao duoc phien dang nhap.
- Cac nguon phim, giao dien Settings va xu ly link dang hoat dong nen khong duoc sua lan man.

## Nguyen nhan

Phan `loginAccount` trong `FshareAPIKt` cua ban local da dung sai cap thong tin nhan dien API so voi plugin goc dang dang nhap duoc:

- `app_key` local sai: `O5UTCpIlQez7xCjdzXzKDR+tAEnV51PosWxXIouT`
- `User-Agent` local sai: `thuviencine-FSWXQQ`
- `app_key` dung theo ban goc: `dMnqMMZMUnN5YpvKENaEhdQQ5jxDqddt`
- `User-Agent` dung theo ban goc: `pyLoad-B1RS5N`

Hai gia tri nay nam trong method `loginAccount` cua:

`smali/com/anhdaden/FshareAPIKt.smali`

Khong thay tat ca chuoi User-Agent tren toan plugin. Cac method khac van giu nguyen de tranh anh huong lay link, tai file va cac nguon dang hoat dong.

## Cach sua

1. Tai ban goc de doi chieu:
   `https://gitlab.com/tearrs/cloudstream-vietnamese/-/raw/main/FshareProvider.cs3`
2. Giai nen file `.cs3`, disassemble `classes.dex` bang Baksmali 2.5.2.
3. Mo `FshareAPIKt.smali`, tim method `loginAccount`.
4. Chi thay hai chuoi `app_key` va `User-Agent` neu tren.
5. Assemble lai bang Smali 2.5.2, thay `classes.dex` trong goi `.cs3`.
6. Tang `version` trong `manifest.json` de CloudStream tai ban moi.
7. Cap nhat `fileSize` va `fileHash` SHA-256 trong `plugins.json`.

## Ban da phat hanh

- Version: `56`
- File: `FshareProvider.cs3`
- Commit: `df01775 Fix Fshare login with original API credentials`
- SHA-256: `0ab36b16b87f7f84c2d3c922bfbc1701fa46efb72b9ce0a6eb787659339d3553`

## Kiem tra sau khi sua

- Cai lai plugin va mo banh rang Settings.
- Chon Login, nhap tai khoan thu nghiem va Save.
- Log/toast phai hien `Login success`.
- Mo thu cac nguon Fshare va phat mot phim de chac chan phan lay link khong bi anh huong.

## Luu y an toan

- Khong ghi tai khoan va mat khau Fshare vao file ghi chu, source, log hoac GitHub.
- Neu Fshare doi API lan nua, uu tien so method `loginAccount` voi ban goc moi nhat truoc khi sua cac phan khac.
