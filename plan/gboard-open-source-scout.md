# Open-Source Scouting Report: Google Android 鍵盤長按符號與輸入模式

> Decision Verdict: Option D
> Estimated MVP Delivery: 2 Days（既有鍵盤增量實作與實機驗證的估計）
> Primary Recommendation: 在 FreeShiamy 自行實作，使用實機功能規格與可確認授權的資料／圖示

## 1. Requirement Scope and Boundaries

- Core Business Loop: 點按／長按按鍵 → 依英文鎖定、Shift 與符號頁狀態解讀 → 送出字元到編輯器，或累積中文原碼供選字。
- Target Delivery Type: Mobile（Android InputMethodService）。
- Licensing Constraint: Permissive Open-Source；不引入 GPL 實作，外部素材須有可確認授權。專案根目錄尚無整體 LICENSE，本次不自行指定全專案授權。
- 使用者選定的實機參考是「中文注音／Google注音輸入法」的英文鍵盤：`com.google.android.apps.inputmethod.zhuyin`，2.4.5.164561151-arm64-v8a，versionCode 2451413。這與 Play 上的 Gboard `com.google.android.inputmethod.latin` 是不同套件，不能混稱為同一版本。
- 查核日期：2026-10-23。Google 官方 Gboard 文件只說明長按可見重音字母與更多選項、連按 Shift 鎖定大寫，未提供完整逐鍵表。[Google 使用說明](https://support.google.com/gboard/answer/2842292?hl=en)。在本次 Google GitHub／AOSP 搜尋及候選路徑查核中，沒有找到 Gboard Android 完整公開實作；此結論限於搜尋範圍，不主張證明所有來源都不存在。
- AOSP LatinIME 提供 Apache-2.0 的鍵盤 XML 與文字表，可作公開比較來源；它不是 Google 注音或目前 Gboard 的原始碼。[AOSP 倉庫](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/)、[文字表與 English locale](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/refs/heads/main/java/src/com/android/inputmethod/keyboard/internal/KeyboardTextsTable.java)。另讀取同版已安裝 APK 的編譯 XML 字元輸入／輸出資料；這不使 APK 成為開源，也沒有複製 Java／smali 實作或原始版面。實機與資源對應另存於 `gboard-symbols-english-lock-plan.md`，不把 AOSP 的選項冒充成實機觀察。

## 2. Shortlisted Candidates

| Repository | URL | Stars | Last Commit | Tech Stack | License | Risk Level | Rating |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HeliBoard | [HeliBorg/HeliBoard](https://github.com/HeliBorg/HeliBoard) | 6,402 | 2026-10-18，`e97acdbe7ad1` | Android／Java／Kotlin，AOSP 衍生 | GPL-3.0 | Warning：授權與整體搬入成本 | 3/5 |
| FlorisBoard | [florisboard/florisboard](https://github.com/florisboard/florisboard) | 8,714 | 2026-09-30，`fe1241f4921b` | Android／Kotlin／Compose／native | Apache-2.0 | Warning：建置與整合範圍較大 | 3/5 |
| AOSP LatinIME（比較來源） | [Android Gitiles](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/) | 不適用，非 GitHub 主倉庫 | 2025-02-26，`127336e9f29d6` | Android／Java／C++／XML | Apache-2.0 | Warning：主線超過 12 個月、舊獨立建置 | 2/5 |

星數與最新 commit 均查自當日倉庫資料，而非搜尋摘要；`pushed_at` 不等於 default branch 最新 commit。[HeliBoard API](https://api.github.com/repos/HeliBorg/HeliBoard)、[FlorisBoard API](https://api.github.com/repos/florisboard/florisboard)。已執行 `site:github.com keyboard android` 與 `site:github.com awesome-keyboard OR topics/android-keyboard` 兩種發現查詢；索引僅用於找候選。

## 3. Five-Pillar Due Diligence

### HeliBoard

- Maintenance & Governance: 最新 main commit 2026-10-18；同日發布 v4.3-beta1，穩定版 v4.2 於 2026-10-12。貢獻量前三為 Helium314 1,682、dslul 158、MajeurAndroid 118；歷史多人貢獻不等於可證明 bus factor ≥3，正式治理與當前審查分工未確認。[releases](https://github.com/HeliBorg/HeliBoard/releases)、[contributors](https://api.github.com/repos/HeliBorg/HeliBoard/contributors?per_page=8)。
- Community Health: 617 forks、862 open issues；有公開協作入口，但本次沒有長期星數成長資料、PR 處理時間或參與者獨立性證據，不能宣稱有機成長已通過檢驗。
- Setup Friction: README 提供 Android Studio 與 `./gradlew assembleDebug`、`adb install`；此為文件路徑，沒有實際另建整套鍵盤驗證三步可啟動。[README](https://github.com/HeliBorg/HeliBoard/blob/main/README.md)。
- Modularity & Reusability: layouts／locale_key_texts 提供排版與 popup seams，KeyboardParser／TextKeyData 可擴充；作為整套 IME 覆蓋輸入核心，但直接替換會牽動現有嘸蝦米原碼、候選與反查流程。[CONTRIBUTING](https://github.com/HeliBorg/HeliBoard/blob/main/CONTRIBUTING.md)。
- Supply Chain & Licensing: App 為 GPL-3.0，不直接複製其實作。README 說明無網路權限，滑行輸入使用外部封閉程式庫，字典另有來源工具。未做全依賴／CVE 審計，不能視為「沒有漏洞」。本次只查文件與 metadata，沒有參考 GPL 實作來寫程式。
- Summary: 活躍且接近傳統 Android 鍵盤；對本次幾個既有功能增量而言，整套採用與授權負擔過大。

### FlorisBoard

- Maintenance & Governance: main 最新 commit 2026-09-30；最新 alpha 2026-01-23，穩定版 v0.5.2 2025-11-28。前三位公開貢獻者 patrickgold 2,166、lm41 183、X-yl 107；無足夠資料確認 bus factor 或目前 PR 審查速度。[releases](https://github.com/florisboard/florisboard/releases)、[contributors](https://api.github.com/repos/florisboard/florisboard/contributors?per_page=8)。
- Community Health: 748 forks、475 open issues；公開討論與貢獻管道可見，未驗證星數成長或機器人比例。
- Setup Friction: 要求 Java 17、SDK／NDK、CMake 3.22+、Android／Compose IDE 支援；CLI 可 `./gradlew clean` 後 `./gradlew assembleDebug`，較現有 XML KeyboardView 複雜，未實際建置。[CONTRIBUTING](https://github.com/florisboard/florisboard/blob/main/CONTRIBUTING.md)。
- Modularity & Reusability: 有 extension 架構；官方 Addons Store 目前主要接受 theme，語言與 keyboard extension 支援仍在規劃，不能假設能以外掛完整接入現有字根核心。[官方文件](https://docs.florisboard.org/)。
- Supply Chain & Licensing: Apache-2.0，可評估採用；README 列出 AndroidX、KotlinX、Material Icons 等依賴與 APK 簽章雜湊。未做依賴／CVE 審計。[README](https://github.com/florisboard/florisboard/blob/main/README.md)。
- Summary: 授權相容、功能完整；為本次需求引入新的 IME／Compose／native 核心，節省不了現有增量的整合工作。

### AOSP LatinIME

- Maintenance & Governance: main 最新 commit 已超過 12 個月，故不通過本次活躍專案篩選，只保留為可追溯資料來源；不推測其治理能維持此專案 fork。
- Community Health: 非 GitHub 主倉庫，星數不適用；本輪未分析 Gerrit 審查與活躍 maintainer 數。
- Setup Friction: 提供 Android.bp；legacy Gradle 使用 AGP 3.2.0-beta03、compileSdk 28，缺乏確認過的現代獨立啟動流程。[build.gradle](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/refs/heads/main/build.gradle)。
- Modularity & Reusability: XML rows／styles、產生的文字表能清楚追查 key hints 與 alternates，適合確認功能資料；不適合為本次修改搬入整個引擎。
- Supply Chain & Licensing: Android.bp 與 NOTICE 聲明 Apache-2.0，對映資料來源檔有 Apache header。Manifest 含聯絡人／下載等權限，本次不複製其 Manifest 或依賴，也沒有安全審計。[NOTICE](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/refs/heads/main/NOTICE)。
- Summary: 可追溯的公開規格比較來源；不是本次直接採用候選，更不是現行 Google 鍵盤的來源證明。

## 4. Architectural Decision Verdict

- Recommended Path: Option D（Custom Build）。
- Key Justification: 現有專案已有 popup、固定 QWERTY、原碼與選字機制。本次只擴充對應資料、英文直接輸出與 Shift 狀態，沒有節省六個月以上工作量的整套 fork 理由。採用自己的互動實作、使用者選定 Google 注音版的實測對應與既有 Apache-2.0 Material 圖示，避免把整套外部 IME 或無法證實的 Gboard mapping 搬入產品。未使用 GPL 實作，不需以 clean-room 迴避其衍生著作義務。

## 5. Minimalist MVP Plan (1-1-1-1 Rule)

- Core User Story: As a FreeShiamy 使用者, I can 在同一個鍵盤長按取符號、鎖定英文及大寫, so that 中英文與符號輸入不用反覆切換其他輸入法。
- Starter Architecture: 沿用一個 KeyboardView 主介面、一位裝置使用者、一組 KeyboardInputState，透過 InputConnection 完成按鍵 → 狀態判斷 → 編輯器保存文字 → 顯示結果。沒有新增資料庫；英文鎖定在目前輸入法工作階段保存，程序重啟回預設。
- Minimal Schema Draft:

  ```text
  Entity: KeyboardInputState（概念結構，沿用 IME 欄位）
  - id: 目前輸入法工作階段
  - mode: 字根 | 英文鎖定
  - shift: 關閉 | 單次大寫 | 大寫鎖定
  ```

- Milestones:
  - Phase 1: 固定參考版本與逐鍵對照、保留原碼的模式切換、直接英文輸出完整迴圈。
  - Phase 2: 符號兩頁底列、長按選項與 Shift 圖示整合；最少 unit test，實機驗證狀態與截圖。
- Scope Blacklist:
  - [x] No authentication or multi-user accounts
  - [x] No microservice decomposition
  - [x] No non-core exports or styling features

## 6. Next Actions

- [x] 確認使用者實機參考為 Google 注音英文模式，保留版本與來源邊界。
- [x] 完成長按對照與 FreeShiamy 增量實作，記錄於 [實作計畫](gboard-symbols-english-lock-plan.md)。
- [x] 固定 APK 後完成獨立實機的主要操作與畫面驗證；補測日誌缺段與未覆蓋項目已記錄於實作計畫。
