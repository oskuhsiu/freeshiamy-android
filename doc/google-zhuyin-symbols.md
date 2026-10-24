# Google 注音與肥肥注音：符號、Emoji、長按與翻頁資料

記錄日期：2026-10-23。本文件保存參考資料與採用範圍。App 已採用下述肥肥注音的 TW 符號、Emoji 與顏文字靜態全集，提供分類與平滑水平翻頁；原版最近使用與平板側區未照搬。

前半為手機上 Google 注音 `2.4.5` 的實測記錄；後半整理使用者提供的 `wdzeng/google-fatty-zhuin-ime` 固定版本靜態目錄與翻頁機制。兩個來源的版本與驗證方式分開記錄。

## 來源與覆蓋範圍

- 使用者指定名稱：**中文注音**，次標 **Google注音輸入法**；模式為英文小寫 QWERTY。
- 套件：`com.google.android.apps.inputmethod.zhuyin`，服務：`.ZhuyinInputMethodService`。
- 版本：`2.4.5.164561151-arm64-v8a`，versionCode：`2451413`。
- APK SHA-256：`5f6dc14d26194a29abfb3da71688b76417115054f078d22b1e13f512305a0dc3`。
- 實機記錄：26 個字母長按、逗號／句點長按、`1?#` 分類前兩個水平頁，以及符號 `1`、`0`、`$`、單引號、雙引號、`€` 的長按。
- 編譯 XML 比對：針對 FreeShiamy 現有的 56 個不同符號鍵，取得 23 組長按資料、32 個未配置長按資料的鍵、1 個未找到的鍵；另保存 base 常用符號資源的全部 64 個直接字元項目。
- **官方版實機尚未完整記錄**：`1?#` 第三個水平頁的實際排列、其他符號分類、完整 Emoji、其他語言與大寫模式。後半的完整靜態目錄取自肥肥注音 repo，不等於已逐頁實測官方版。

這版是 Google 注音，與現行 Gboard 不同。實機資料以手勢期間截圖記錄，資源資料由 Android SDK `aapt dump xmltree` 讀取同版 APK 的編譯 XML。沒有反編譯 Java／smali；Git 僅保存整理後的字符資料與來源資訊，不納入 APK、原始 XML 或截圖。

機器可讀資料：[google-zhuyin-symbols.json](google-zhuyin-symbols.json)。其中保留選單每列、空格（`null`）、實機預設、資源順序及逐鍵來源路徑，未混入測試輸入框的個人內容。

## 已拍下的符號表

以下是 `1?#` 分類前兩個水平頁的可輸入字元，由既有實機截圖轉錄。第三列右端另有刪除鍵；底列為返回、逗號、數字鍵盤入口、空白、句點、Enter。分類列的其他入口尚未逐一採集。

| 頁面 | 列 | 由左至右 |
| :--- | :--- | :--- |
| page1 | 1 | `1` `2` `3` `4` `5` `6` `7` `8` `9` `0` |
| page1 | 2 | `!` `@` `#` `$` `%` `^` `&` `*` `(` `)` |
| page1 | 3 | `'` `"` `=` `_` <code>&#96;</code> `:` `;` `?` |
| page2 | 1 | `~` <code>&#124;</code> `+` `-` `\` `/` `[` `]` `{` `}` |
| page2 | 2 | `<` `>` `“` `”` `·` `‘` `’` `¡` `¿` `￥` |
| page2 | 3 | `€` `£` `¢` `©` `®` `™` `℃` `℉` |

第二頁的 `￥` 是 U+FFE5 FULLWIDTH YEN SIGN；長按 `$` 選單中的 `¥` 是 U+00A5。`·` 是 U+00B7，溫度鍵 `℃`／`℉` 各為單一字元 U+2103／U+2109。這些字形已用同版資源核對，不能以外觀相近字元替換。

## 英文字母長按：26 鍵完整實機記錄

所有選項均列出，包含大寫字母，共 98 個選項位置。順序為截圖由上至下、由左至右；實際換列與空格另存 JSON。預設表示按住不移動時的反白項目。

| 按鍵 | 全部選項 | 實機預設 |
| :--- | :--- | :--- |
| `q` | `1` `Q` | `1` |
| `w` | `2` `W` | `2` |
| `e` | `ě` `ē` `é` `è` `ê` `3` `E` `ë` | `3` |
| `r` | `4` `R` | `4` |
| `t` | `5` `T` | `5` |
| `y` | `6` `Y` | `6` |
| `u` | `ǔ` `ū` `ú` `ù` `û` `7` `U` `ü` | `7` |
| `i` | `ì` `ǐ` `ī` `í` `ï` `î` `8` `I` | `8` |
| `o` | `œ` `ø` `õ` `ò` `ǒ` `ó` `ō` `ö` `ô` `O` `9` | `9` |
| `p` | `P` `0` | `0` |
| `a` | `ã` `å` `æ` `ā` `á` `ǎ` `à` `@` `A` `â` `ä` | `@` |
| `s` | `ß` `*` `S` | `*` |
| `d` | `+` `D` | `+` |
| `f` | `-` `F` | `-` |
| `g` | `=` `G` | `=` |
| `h` | `/` `H` | `/` |
| `j` | `#` `J` | `#` |
| `k` | `(` `K` | `(` |
| `l` | `L` `)` | `)` |
| `z` | `'` `Z` | `'` |
| `x` | `:` `X` | `:` |
| `c` | `ç` `"` `C` | `"` |
| `v` | `ǚ` `ǖ` `ǘ` `ǜ` `¿` `?` `V` `ü` | `?` |
| `b` | `¡` `!` `B` | `!` |
| `n` | `ñ` `~` `N` | `~` |
| `m` | `…` `M` | `…` |

原版 z 的預設是 ASCII 單引號。FreeShiamy 依使用者要求額外加入「、」並設為預設；這是刻意保留的差異。尚未採集原版 Shift 開啟後的全部長按選單。

## 字母頁的句點與逗號

句點長按共有 15 個選項，實機排列如下，預設為 `?`：

| 位置 | 由左至右 |
| :--- | :--- |
| 上列 | `^_^` `:)` `:(` `:P` `:D` |
| 中列 | `&` `^` `$` `_` `%` |
| 下列 | `;` `:` `…` `?` `!` |

逗號長按是動作選單：左側未確認用途的圖示、Emoji（笑臉，預設）、設定（齒輪）。已實際確認放開後開啟 Emoji；左側動作未觸發，不推測其功能。符號頁逗號沒有相同長按資料，符號頁句點的長按僅為 `…`。

## 符號長按：23 組資源對照

下表「資源順序」來自同版 APK 的 base 編譯 XML；「實機順序」僅填已長按確認者。未實測的預設只能稱為資源第一項，不能聲稱已驗證原版反白位置。

| 按鍵 | 資源順序 | 資源第一項 | 實機順序／預設 |
| :--- | :--- | :--- | :--- |
| `0` | `∅` `ⁿ` | `∅` | `ⁿ` `∅`；預設 `∅` |
| `1` | `¹` `½` `⅓` `¼` `⅛` | `¹` | `¹` `½` `⅓` `¼` `⅛`；預設 `¹` |
| `2` | `²` `⅔` | `²` | 未實測 |
| `3` | `³` `¾` `⅜` | `³` | 未實測 |
| `4` | `⁴` | `⁴` | 未實測 |
| `5` | `⅝` | `⅝` | 未實測 |
| `7` | `⅞` | `⅞` | 未實測 |
| `$` | `¢` `€` `£` `₱` `¥` | `¢` | `¥` `£` `¢` `€` `₱`；預設 `¢` |
| `%` | `‰` | `‰` | 未實測 |
| `*` | `★` `†` `‡` | `★` | 未實測 |
| `-` | `—` `_` `–` `·` | `—` | 未實測 |
| `=` | `≠` `≈` `∞` | `≠` | 未實測 |
| `(` | `<` `{` `[` | `<` | 未實測 |
| `)` | `>` `}` `]` | `>` | 未實測 |
| `!` | `¡` | `¡` | 未實測 |
| `"` | `„` `“` `”` `«` `»` | `„` | `”` `„` `“` `«` `»`；預設 `„` |
| `'` | `‚` `‘` `’` `‹` `›` | `‚` | `‚` `‘` `’` `‹` `›`；預設 `‚` |
| `?` | `¿` | `¿` | 未實測 |
| `.` | `…` | `…` | 未實測 |
| `^` | `↑` `↓` `←` `→` | `↑` | 未實測 |
| `+` | `±` | `±` | 未實測 |
| `<` | `≤` `‹` `«` | `≤` | 未實測 |
| `>` | `≥` `›` `»` | `≥` | 未實測 |

上述比對中，未配置 `long_press_data` 的 32 鍵：

`6` `8` `9` `@` `#` `&` `:` `;` `/` `,` `~` `±` `×` `÷` `•` `°` <code>&#96;</code> `{` `}` `©` `£` `€` `®` `¥` `_` `[` `]` `¡` `¢` <code>&#124;</code> `\` `¿`。

`€` 已實機長按，沒有出現選單。其他鍵的「沒有資源屬性」不等於已逐鍵排除動態程式行為。FreeShiamy 原有的 `´`（U+00B4）未出現在掃描的原版資源，因此沒有推測其長按選項。

## 常用符號資源：尚未完整照搬的項目

以下保存 `res/xml/softkeys_input_symbol_sub_category_favorate.xml` 的全部 64 個直接 `param_data`，按 XML 順序分組方便閱讀。該資源另外 include 數字與基本符號；這個清單不代表水平分頁的實際換行，也不包含其他分類。

`'` `"` `=` `_` <code>&#96;</code> `:` `;` `?` `~` <code>&#124;</code> `+` `-`

`\` `/` `[` `]` `{` `}` `<` `>` `“` `”` `·` `‘`

`’` `¡` `¿` `￥` `€` `£` `¢` `©` `®` `™` `℃` `℉`

`°` `§` `№` `†` `‡` `‥` `…` `‰` `※` `‾` `⁄` `‼`

`⁇` `⁈` `⁉` `√` `π` `±` `×` `÷` `¶` `∆` `¤` `µ`

`‹` `›` `«` `»`

數字 include：`res/xml/softkeys_input_symbol_numbers.xml`，基本符號 include：`res/xml/softkeys_input_symbol_basic_symbols.xml`。其他主要來源為 `softkeys_input_symbol_templates.xml` 與 `softkeys_input_non_prime_bottom_symbol.xml`，完整路徑／關聯存於 JSON。

TW 變體 `softkeys_input_symbol_sub_category_favorate_tw.xml` 的 ASCII 引號未配置相同長按資料；本文件使用與實機英文畫面相符的 base 版本。不要混合兩種變體。

## 本專案的採用狀態與後續邊界

- 已實作：26 鍵長按選項集合、字母頁句點 15 選項、符號頁 23 組長按、逗號開 Emoji；z 額外保留「、」。
- 本輪擴充：以肥肥注音靜態全集取代原本兩頁符號與 28 個 Emoji，加入六類 TW 符號與一類補充、八類 Emoji、五類顏文字及其長按變體。Google 注音官方實機與肥肥注音來源仍分開記錄，不能把後者等同於前者的逐頁實測。
- 尚未照搬：最近使用紀錄、逗號完整三動作，以及平板獨立版型；這些不影響已保存靜態字符全集的採用。
- 資料完整性：JSON 保留已取得的集合、預設及來源；畫面排列／拖選手感不能僅由 XML 推定。

原始決策與實測範圍見 [實作計畫](../plan/gboard-symbols-english-lock-plan.md)，公開原始碼查核見 [OSS 研究](../plan/gboard-open-source-scout.md)。

### App 完整目錄實作

- 符號以 TW 六類 532 個位置為基礎，新增「補充」類 10 項，最終七類共 542 個位置、87 個長按位置。補充類包含 base-only 的 `‾ ⁄ ⁉ ¶ ¤ µ ❥ ❦ ❧` 及原有 `´`；TW 缺少的 9 組長按關聯（單／雙引號、`? < > + - = %`）採 base 補齊。Emoji 在 API 23–24 使用 base 1,357／504，API 25+ 使用新版 1,404／880（主選項／長按位置）；顏文字共 90 項。原始順序、重複與完整 Unicode 字串保留，沒有把多碼位 Emoji 拆成單一 Char。
- App 符號每頁 28、Emoji 每頁 20、顏文字每頁 11 個可輸入位置；各頁第三列右側保留刪除鍵。顏文字比來源手機版型少一格，所以本節是本專案規則，後文仍記錄來源原有 12 格。
- 頂部可選符號／Emoji／顏文字及分類；左右翻頁可跨分類，頁碼顯示分類內位置，首尾不循環。直接選分類會定位該分類首頁。底列 ABC、逗號、空白、句點與 Enter 不跟著水平滑動。
- **資源跳脫字元**：研究 JSON 的 literal `.text` 保留原始 Android XML attribute 語法（`codepoints` 為空），例如 `\@`、`\#`、`\?`、雙反斜線與 `\^o^/`。執行用資料須解碼一次，輸出 `@`、`#`、`?`、單反斜線、`^o^/`；不能直接把 literal `.text` 當成最終輸入。轉換已以 Android AAPT2 compile／link／xmltree 實際比對，保留引號與顏文字內部空白；`U+` 解碼後的完整 Unicode 序列不再套用跳脫解碼。
- `scripts/generate_symbol_catalog.py` 由本文件附的 JSON 生成 `app/src/main/assets/catalog/`；總計 39,800 bytes，索引 2,153 bytes、單頁最多 3,775 bytes。執行 `--check` 逐項核對字符、變體、順序、分類與檔案。開發用 4.28 MB JSON 不打包進 App。
- 執行時只保留小索引及附著頁資料；ViewPager 的鄰頁保留值為 1，分類跳轉不跨數十頁做動畫。返回字母、隱藏、重建或離開輸入法時移除頁面、長按視窗與重複按鍵回呼。無額外字型、圖片快取、背景執行緒或目錄全集快取；字型支援依系統而定。
- 完整輸出沿用原碼規則：有待送原碼時附加選取的整段字串，英文鎖定或原碼為空時直接上屏；刪除待送原碼內的目錄選取會整段刪除，避免留下半個 surrogate 或 Emoji 序列。

實作及本輪驗證見 [完整符號與 Emoji 翻頁計畫](../plan/symbol-emoji-paging-plan.md)。

## 肥肥注音 repo：完整靜態符號、Emoji 與顏文字資料

來源：[wdzeng/google-fatty-zhuin-ime](https://github.com/wdzeng/google-fatty-zhuin-ime)，固定 commit [`af24a6faa849`](https://github.com/wdzeng/google-fatty-zhuin-ime/commit/af24a6faa849da013b01a6b0debae52948d19424)（2022-03-04）。讀取日期為 2026-10-23；這是該 commit 的資料全集，不代表最新 Unicode／現行 Gboard。

README 說明 `src/` 是 Google 注音 APK 反編譯後修改的檔案，主要增加 Emoji 與重分顏文字；不是 Google 官方公開原始碼。`apktool.yml` 記載 `h-1.0.0`／code `3000003`，與手機的官方 `2.4.5` 分開保存。repo 根目錄未找到 LICENSE／COPYING／NOTICE；內有第三方 notices，不能把公開可讀直接等同於整套程式已有明示授權。本次只保存整理後的功能資料、Unicode 字符與機制說明，不納入 smali、圖片、字型、APK 或金鑰。來源：[README](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/README.md#L18)、[版本資訊](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/apktool.yml#L30)。

完整可供下次直接使用的資料檔：[google-fatty-zhuyin-catalog.json](google-fatty-zhuyin-catalog.json)。此檔為 UTF-8 JSON；不需要再 clone repo 才能取得字符。

- `datasets`：37 份原始資料／模板清單，保留每項文字、原 `U+` 表示、碼位、長按變體、順序、來源行／XPath、模板與 `rendering_filter`／`span` 等屬性。
- `expanded_include_streams`、`route_category_summaries`：已展開 include 與 Unicode range 的完整分類資料流；base／TW 與 `xml-v25` 不混合。
- `auxiliary_datasets`：17 份補充按鍵／標點資料，包含英文 QWERTY、底列標點、平板數字區與直接 action 的字符；不灌入主目錄的數量。
- `resource_manifest`、`candidate_static_file_audit`：逐檔 SHA-256、範圍、解析結果與引用；`main_paging_analysis` 是下列翻頁規則的摘要。

**計數單位是選項位置，保留重複。** base 與 API 25+ 是替代版本，不能相加當成可見選項總數。長按清單也可能包含主選項本身。未填入最近使用的個人資料；它是動態清單，沒有固定全集。

### 符號分類與頁數

SUB_CATEGORY_1 為最近使用；其後六類如下。手機每頁 28 個字符位置，表中頁數由靜態資料與版型計算，未逐頁操作肥肥注音 APK。

| 分類 ID | 分類 | base 項目 | base 頁數 | TW 項目 | TW 頁數 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 2 | 常用 | 84 | 3 | 84 | 3 |
| 3 | 括號 | 56 | 2 | 56 | 2 |
| 4 | 箭頭 | 84 | 3 | 84 | 3 |
| 5 | 數學 | 112 | 4 | 112 | 4 |
| 6 | 數字 | 56 | 2 | 84 | 3 |
| 7 | 圖形 | 112 | 4 | 112 | 4 |

base 的六類共 **504 個位置／18 頁**，TW 共 **532 個位置／19 頁**，另加動態最近使用。共用字符與長按仍會重複出現在多類中，不能當成獨立字符數。常用類包含前 10 個數字、10 個基本符號，再接前節的 64 個項目，所以是 84 個位置／3 頁。Unicode range 已逐字展開，例如箭頭 U+2190–U+21E3 為 84 個項目，不能把 range 節點誤算為一顆鍵。

來源：[base 分類](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_symbol_sub_categories.xml)、[TW 分類](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_symbol_sub_categories_tw.xml)。

### Emoji 分類、長按與版本差異

SUB_CATEGORY_1 為最近使用；其後八類如下。base 通常用於 API 24 以下，API 25+ 會選取同名 `xml-v25` 資源。本專案 minSdk 23，未來採用時要明確決定資料版本。

| 分類 ID | 分類 | base 主選項 | API25+ 主選項 | base 長按位置 | API25+ 長按位置 | API25+ 預估頁數 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 2 | 笑臉與人物 | 271 | 297 | 420 | 728 | 15 |
| 3 | 動物與自然 | 107 | 107 | 0 | 0 | 6 |
| 4 | 食物與飲料 | 89 | 89 | 0 | 0 | 5 |
| 5 | 旅行與地點 | 209 | 210 | 12 | 6 | 11 |
| 6 | 活動 | 77 | 75 | 72 | 146 | 4 |
| 7 | 物品 | 168 | 162 | 0 | 0 | 9 |
| 8 | 符號 | 202 | 205 | 0 | 0 | 11 |
| 9 | 旗幟 | 234 | 259 | 0 | 0 | 13 |

base 共 **1,357 個主選項＋504 個長按位置**；API25+ 共 **1,404 個主選項＋880 個長按位置**。API25+ 八類按 20 個主選項分頁，未過濾時共 74 頁；base 為 72 頁，另加動態最近使用。旗幟、膚色、性別、ZWJ、VS16 均按完整序列保存，不能用單一 `Char` 當作一個 Emoji。

`rendering_filter=true` 表示實際可見項目會受系統字型／繪製支援影響；上述是未過濾資料的頁數估計，不是本機逐頁實測結果。長按模板指定 keep-order popup，`param_longpress_data` 以空白分隔不同選項；一個選項內連接的 `U+...U+...` 必須整段解碼。來源：[分類順序](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_body_smiley.xml)、[Emoji 長按模板](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_emoji_templates.xml)。

### 顏文字分類

顏文字也是多字元輸出，內部空白、標點和組合符號要完整保留；不能比照 Emoji 長按資料用空白切開。

| 分類 ID | 分類 | 項目 | 手機頁數 |
| :--- | :--- | :--- | :--- |
| 2 | 微笑 | 40 | 4 |
| 3 | 汗／尷尬 | 8 | 1 |
| 4 | 驚訝 | 15 | 2 |
| 5 | 悲傷 | 10 | 1 |
| 6 | 不滿 | 17 | 2 |

五類共 **90 個選項／10 頁**（依每類各自分頁計算），另有動態最近使用。模板只有一般 PRESS 文字輸出，未配置 Emoji 的膚色長按。來源：[顏文字分類](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_emoticon.xml)。

### 翻頁、刪除鍵與狀態

| 畫面 | 手機版型 | 每頁可輸入項目 | 刪除鍵 |
| :--- | :--- | :--- | :--- |
| 符號 | 10＋10＋8 | 28 | 第三列獨立一顆，權重 150；字符鍵權重 100 |
| Emoji | 7 欄 × 3 列，共 21 個 view | **20** | 最後一格是 Delete，不能算成第 21 個 Emoji |
| 顏文字 | 4 欄 × 3 列 | 12 | 不佔該 page 的固定格 |
| 平板符號 | 10 欄 × 3 列 | 30 | 與手機 page 不同，不能直接用 28 切資料 |
| 平板顏文字／笑臉 | 採顏文字 page，欄數覆寫為 8、3 列 | 24 | 依平板側區鍵位處理 |

`android:orientation` 在 page 內用來排格子，**不等於滑動方向**。外層 pager 預設為水平：左右滑動換頁，底部滑條顯示頁面進度。分類列維持在上方，輸入區與底列各自獨立。

```text
category A: [A1] -> [A2] -> [A3] -> [B1] -> [B2] -> ...
category B click --------------------^
indicator:  category-local page / category page count
```

1. **切換分類**：切到另一分類時，跳至該分類的第一頁；再次選擇已反白分類不會自動重設頁碼。
2. **連續翻頁**：各分類按資源順序串接成全域頁面；滑過分類邊界會同步切換上方分類反白。沒有把左右滑動限制在目前分類內。
3. **邊界**：`pageUp`／`pageDown` 在整個資料流的首／末頁停止，沒有循環回到另一端。底部指示器採分類內的頁碼和頁數。
4. **分頁計算**：每類分別取 `ceil(N / capacity)`，尾頁留空，不拿下一分類填滿。被標成 sticky 的空最近使用仍保留一頁，其他空分類可為零頁。
5. **固定功能鍵**：手機符號與 Emoji 的刪除鍵不是資料項目；輸入資料填入時會跳過具名的 Delete view。這也是 Emoji 容量從 21 減成 20 的原因。
6. **最近使用**：符號／顏文字設定為 `COMMITTED_ACTION_ONLY`，Emoji 為 `ALL_ACTIONS`；清單由 RecentKeyDataManager 在執行時提供。排序、保存上限與使用者內容未在本輪實機驗證，不填入猜測的靜態資料。
7. **重新開啟**：Emoji／顏文字的資源設定保存 `ALL_SUB_CATEGORY`，即分類狀態；目前讀到的定義未給每類獨立頁碼偏好。不要把它解讀成已證實會記住每類最後一頁。符號預設 SUB_CATEGORY_2（常用）。

上述為資源與程式流程的唯讀分析；沒有安裝或執行肥肥注音 APK，也沒有量測滑動距離門檻、速度門檻、動畫時間或慣性手感。下次修改若要相同手感，仍需針對新的 UI 實測這些動態行為。

主要核對位置（固定 commit）：

- [手機符號版型](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/layout/keyboard_non_prime_page_symbol.xml#L2)、[手機 Emoji 版型](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/layout/keyboard_non_prime_page_emoji.xml#L2)、[手機顏文字版型](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/layout/keyboard_non_prime_page_emoticon.xml#L2)、[手機欄列數](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/values/integers.xml#L51)。
- [符號格順序與 ceil 分頁](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/smali/com/google/android/apps/inputmethod/libs/framework/keyboard/widget/LinearSoftKeyViewsPage.smali#L86)；[固定格容量扣除 Delete](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/smali/com/google/android/apps/inputmethod/libs/framework/keyboard/widget/FixedSizeSoftKeyViewsPage.smali#L246)。
- [各分類分頁與空 sticky 分類](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/smali/avz.smali)；[分類首頁跳轉](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/smali/com/google/android/apps/inputmethod/libs/framework/keyboard/widget/PageableRecentSubCategorySoftKeyListHolderView.smali#L424)。
- [跨分類更新與局部頁碼](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/smali/com/google/android/apps/inputmethod/libs/framework/keyboard/widget/PageableRecentSubCategorySoftKeyListHolderView$a.smali#L36)；[首末頁停止](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/smali/com/google/android/apps/inputmethod/libs/framework/keyboard/widget/PageableSoftKeyListHolderView.smali#L636)。
- [頁數與滑條回呼](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/smali/com/google/android/apps/inputmethod/libs/framework/keyboard/PageableNonPrimeSubCategoryKeyboard.smali#L756)；[水平滑條](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/layout/non_prime_keyboard_page_indicator.xml)；[外層方向預設](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/smali/kz.smali#L281)。
- [Emoji 最近使用與分類保存](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/keyboard_non_prime_smiley.xml#L3)；[顏文字狀態](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/keyboard_non_prime_emoticon.xml#L3)；[分類偏好定義](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/values/strings.xml#L633)。

### 下次實作直接查用的方法

以 `dataset_id` 取得某個分類，不需要再對外找資料：

```python
import json
from pathlib import Path
catalog = json.loads(Path("doc/google-fatty-zhuyin-catalog.json").read_text())
people = next(d for d in catalog["datasets"]
              if d["dataset_id"] == "softkeys_input_emoji_category_smiley_people__xml-v25")
texts = [item["text"] for item in people["items"]]
alternates = [item["long_press_variants"] for item in people["items"]]
```

實作時以完整 UTF-8 字串送出，保留每類順序與長按順序；`source.raw_value` 可重建原 Unicode 表示，`source.line`／`source.xpath` 與 SHA-256 可追溯。符號常用類要採 include 展開後的 84 個項目，不只取本地 64 個。API25+ 目錄、TW 目錄與最近使用各自處理；不以文字去重後的數量計算原版頁碼。

### 全部字符清單

以下按資料檔列出全部靜態字符與長按選項，保留原順序與重複；清單中的分段只為閱讀，不代表實際換行。可用上方表格理解分類與頁數，以 JSON 的 item 順序／碼位作為實作輸入。包含所有 base、API25+、TW 與 TV 資料版本；auxiliary 的完整按鍵／標點與 action 字符保存在 JSON。

#### 符號：完整資料清單

<details>
<summary>softkeys_input_non_prime_bottom_symbol / base：4 項，長按 7 個位置</summary>

來源：[softkeys_input_non_prime_bottom_symbol.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_non_prime_bottom_symbol.xml)；SHA-256：`4a73fc380c55ad090efa95b2849d5f9457f2989f5b9ea871724fd5d8f3dd2957`。

`,` `.` `<` `>`

長按對應（含原選項的重複位置）：

- `.` → `…`
- `<` → `≤` `‹` `«`
- `>` → `≥` `›` `»`

</details>

<details>
<summary>softkeys_input_symbol_basic_symbols / base：10 項，長按 20 個位置</summary>

來源：[softkeys_input_symbol_basic_symbols.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_symbol_basic_symbols.xml)；SHA-256：`2be0a0be5411958e97d9c30f7769b3a78a5210909b794a031fba1cb60b3dda30`。

`!` `\@` `\#` `$` `%` `^` `&` `*` `(` `)`

長按對應（含原選項的重複位置）：

- `!` → `¡`
- `$` → `¢` `€` `£` `₱` `¥`
- `%` → `‰`
- `^` → `↑` `↓` `←` `→`
- `*` → `★` `†` `‡`
- `(` → `<` `{` `[`
- `)` → `>` `}` `]`

</details>

<details>
<summary>softkeys_input_symbol_numbers / base：10 項，長按 15 個位置</summary>

來源：[softkeys_input_symbol_numbers.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_symbol_numbers.xml)；SHA-256：`6aaa895dda2e390e6abcd7587dec4624ffdddadac9bf5ca1d0b403026423fd32`。

`1` `2` `3` `4` `5` `6` `7` `8` `9` `0`

長按對應（含原選項的重複位置）：

- `1` → `¹` `½` `⅓` `¼` `⅛`
- `2` → `²` `⅔`
- `3` → `³` `¾` `⅜`
- `4` → `⁴`
- `5` → `⅝`
- `7` → `⅞`
- `0` → `∅` `ⁿ`

</details>

<details>
<summary>softkeys_input_symbol_sub_category_arrow / base：84 項，長按 0 個位置</summary>

來源：[softkeys_input_symbol_sub_category_arrow.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_symbol_sub_category_arrow.xml)；SHA-256：`64519004bd2d04cfc8ffde8769b833004f128dd03af629c2218a323f5685e896`。

`←` `↑` `→` `↓` `↔` `↕` `↖` `↗` `↘` `↙` `↚` `↛`

`↜` `↝` `↞` `↟` `↠` `↡` `↢` `↣` `↤` `↥` `↦` `↧`

`↨` `↩` `↪` `↫` `↬` `↭` `↮` `↯` `↰` `↱` `↲` `↳`

`↴` `↵` `↶` `↷` `↸` `↹` `↺` `↻` `↼` `↽` `↾` `↿`

`⇀` `⇁` `⇂` `⇃` `⇄` `⇅` `⇆` `⇇` `⇈` `⇉` `⇊` `⇋`

`⇌` `⇍` `⇎` `⇏` `⇐` `⇑` `⇒` `⇓` `⇔` `⇕` `⇖` `⇗`

`⇘` `⇙` `⇚` `⇛` `⇜` `⇝` `⇞` `⇟` `⇠` `⇡` `⇢` `⇣`

</details>

<details>
<summary>softkeys_input_symbol_sub_category_brace / base：56 項，長按 6 個位置</summary>

來源：[softkeys_input_symbol_sub_category_brace.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_symbol_sub_category_brace.xml)；SHA-256：`eb595b1ebedd98c621a5d409edeb1b8c10356b0f954afba3d71df3800ec9b3d2`。

`(` `)` `[` `]` `{` `}` `（` `）` `［` `］` `｛` `｝`

`❨` `❩` `❲` `❳` `❴` `❵` `‘` `’` `“` `”` `❛` `❜`

`❝` `❞` `<` `>` `〈` `〉` `《` `》` `〔` `〕` `【` `】`

`〘` `〙` `「` `」` `『` `』` `︵` `︶` `︷` `︸` `︹` `︺`

`︻` `︼` `︽` `︾` `︿` `﹀` `﹁` `﹂`

長按對應（含原選項的重複位置）：

- `(` → `<` `{` `[`
- `)` → `>` `}` `]`

</details>

<details>
<summary>softkeys_input_symbol_sub_category_favorate / base：64 項，長按 25 個位置</summary>

來源：[softkeys_input_symbol_sub_category_favorate.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_symbol_sub_category_favorate.xml)；SHA-256：`cf7283dccdcb0fdd4f22e8334cf4cd1f3a6e143155c57dd3ef382eda679983e2`。

`'` `"` `=` `_` <code>`</code> `:` `;` `\?` `~` <code>|</code> `+` `-`

`\\` `/` `[` `]` `{` `}` `<` `>` `“` `”` `·` `‘`

`’` `¡` `¿` `￥` `€` `£` `¢` `©` `®` `™` `℃` `℉`

`°` `§` `№` `†` `‡` `‥` `…` `‰` `※` `‾` `⁄` `‼`

`⁇` `⁈` `⁉` `√` `π` `±` `×` `÷` `¶` `∆` `¤` `µ`

`‹` `›` `«` `»`

長按對應（含原選項的重複位置）：

- `'` → `‚` `‘` `’` `‹` `›`
- `"` → `„` `“` `”` `«` `»`
- `=` → `≠` `≈` `∞`
- `\?` → `¿`
- `+` → `±`
- `-` → `—` `_` `–` `·`
- `<` → `≤` `‹` `«`
- `>` → `≥` `›` `»`

</details>

<details>
<summary>softkeys_input_symbol_sub_category_favorate_tw / base：64 項，長按 14 個位置</summary>

來源：[softkeys_input_symbol_sub_category_favorate_tw.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_symbol_sub_category_favorate_tw.xml)；SHA-256：`268f4b1a2fedd428fc1ba65cda7bf8d686a171df751a329a3bdd5950a3855df6`。

`「` `」` `。` `、` `：` `；` `？` `！` `～` `=` `+` `-`

`/` `\\` <code>|</code> `·` `『` `』` `[` `]` `{` `}` `<` `>`

`…` `—` `“` `”` `‘` `’` `_` `'` `"` `:` `;` `\?`

<code>`</code> `~` `／` `￥` `€` `£` `¢` `©` `®` `™` `¡` `¿`

`‖` `‗` `†` `‡` `‥` `※` `‼` `⁇` `⁈` `℃` `℉` `°`

`§` `№` `々` `〒`

長按對應（含原選項的重複位置）：

- `=` → `≠` `≈` `∞`
- `+` → `±`
- `-` → `—` `_` `–` `·`
- `<` → `≤` `‹` `«`
- `>` → `≥` `›` `»`

</details>

<details>
<summary>softkeys_input_symbol_sub_category_math / base：112 項，長按 6 個位置</summary>

來源：[softkeys_input_symbol_sub_category_math.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_symbol_sub_category_math.xml)；SHA-256：`e8fdae07e9aa67aba9d6e138e06686b1a3cdb2235af5a706e8bf292ac174ffd3`。

`+` `-` `×` `÷` `±` `=` `≠` `∼` `≈` `≡` `<` `≤`

`≦` `≪` `≮` `>` `≥` `≧` `≫` `≯` `⊕` `⊗` `%` `‰`

`∀` `∂` `∃` `∅` `∆` `∇` `∈` `∉` `∋` `∌` `∏` `∑`

`√` `∝` `∞` `∟` `∠` `∧` `∨` `∣` `∥` `∩` `∪` `∫`

`∬` `∮` `∴` `∵` `∶` `∷` `⊂` `⊃` `⊄` `⊅` `⊆` `⊇`

`∪` `∩` `ⁿ` `α` `β` `γ` `δ` `ε` `ζ` `η` `θ` `ι`

`κ` `λ` `μ` `ν` `ξ` `ο` `π` `ρ` `ς` `σ` `τ` `υ`

`φ` `χ` `ψ` `ω` `Α` `Β` `Γ` `Δ` `Ε` `Ζ` `Η` `Θ`

`Ι` `Κ` `Λ` `Μ` `Ν` `Ξ` `Ο` `Π` `Ρ` `Σ` `Τ` `Υ`

`Φ` `Χ` `Ψ` `Ω`

長按對應（含原選項的重複位置）：

- `<` → `≤` `‹` `«`
- `>` → `≥` `›` `»`

</details>

<details>
<summary>softkeys_input_symbol_sub_category_number / base：56 項，長按 0 個位置</summary>

來源：[softkeys_input_symbol_sub_category_number.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_symbol_sub_category_number.xml)；SHA-256：`7877f68a87102ecbdf8afd206239a460b205da96edc39b28525b0d15b58e1eb3`。

`①` `②` `③` `④` `⑤` `⑥` `⑦` `⑧` `⑨` `⑩` `⑴` `⑵`

`⑶` `⑷` `⑸` `⑹` `⑺` `⑻` `⑼` `⑽` `Ⅰ` `Ⅱ` `Ⅲ` `Ⅳ`

`Ⅴ` `Ⅵ` `Ⅶ` `Ⅷ` `Ⅸ` `Ⅹ` `Ⅺ` `Ⅻ` `Ⅼ` `Ⅽ` `Ⅾ` `Ⅿ`

`ⅰ` `ⅱ` `ⅲ` `ⅳ` `ⅴ` `ⅵ` `ⅶ` `ⅷ` `ⅸ` `ⅹ` `ⅺ` `ⅻ`

`ⅼ` `ⅽ` `ⅾ` `ⅿ` `¹` `²` `³` `⁴`

</details>

<details>
<summary>softkeys_input_symbol_sub_category_number_zh / base：84 項，長按 0 個位置</summary>

來源：[softkeys_input_symbol_sub_category_number_zh.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_symbol_sub_category_number_zh.xml)；SHA-256：`48511bfebd8f5ec742664927f379af8e45b2b9807a861d8e2bb5947b8642bec3`。

`①` `②` `③` `④` `⑤` `⑥` `⑦` `⑧` `⑨` `⑩` `⑴` `⑵`

`⑶` `⑷` `⑸` `⑹` `⑺` `⑻` `⑼` `⑽` `❶` `❷` `❸` `❹`

`❺` `❻` `❼` `❽` `❾` `❿` `㈠` `㈡` `㈢` `㈣` `㈤` `㈥`

`㈦` `㈧` `㈨` `㈩` `Ⅰ` `Ⅱ` `Ⅲ` `Ⅳ` `Ⅴ` `Ⅵ` `Ⅶ` `Ⅷ`

`Ⅸ` `Ⅹ` `Ⅺ` `Ⅻ` `Ⅼ` `Ⅽ` `Ⅾ` `Ⅿ` `ⅰ` `ⅱ` `ⅲ` `ⅳ`

`ⅴ` `ⅵ` `ⅶ` `ⅷ` `ⅸ` `ⅹ` `ⅺ` `ⅻ` `ⅼ` `ⅽ` `ⅾ` `ⅿ`

`¹` `²` `³` `⁴` `₁` `₂` `₃` `₄` `½` `⅓` `⅔` `㊣`

</details>

<details>
<summary>softkeys_input_symbol_sub_category_shape / base：112 項，長按 0 個位置</summary>

來源：[softkeys_input_symbol_sub_category_shape.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_symbol_sub_category_shape.xml)；SHA-256：`cf0454f4ae43a0092088c386c795600aae1479add7e88c7eb572fae49e37582b`。

`⚫` `⚪` `●` `○` `■` `□` `⬛` `⬜` `★` `☆` `◆` `◇`

`▲` `△` `▶` `▷` `▼` `▽` `◀` `◁` `◐` `◑` `♀` `♂`

`♤` `♡` `♧` `♢` `♠` `♥` `♣` `♦` `•` `❖` `✓` `✔`

`✕` `✖` `✗` `✘` `✙` `✚` `✛` `✜` `✝` `✞` `✟` `✠`

`✡` `✢` `✣` `✤` `✥` `✦` `✧` `✨` `✩` `✪` `✫` `✬`

`✭` `✮` `✯` `✰` `✱` `✲` `✳` `✴` `✵` `✶` `✷` `✸`

`✹` `✺` `✻` `✼` `✽` `✾` `✿` `❀` `❁` `❂` `❃` `❄`

`❅` `❆` `❇` `❈` `❉` `❊` `❋` `❌` `❍` `❎` `❏` `❐`

`❑` `❒` `◈` `◉` `◊` `○` `◢` `◣` `◤` `◥` `❢` `❣`

`❤` `❥` `❦` `❧`

</details>

<details>
<summary>softkeys_input_symbol_sub_category_shape_zh / base：112 項，長按 0 個位置</summary>

來源：[softkeys_input_symbol_sub_category_shape_zh.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_symbol_sub_category_shape_zh.xml)；SHA-256：`70dec4f1d9d672b4ba6af09e769d18099ca383ba9816ce2adc8cb715a51c37ab`。

`⚫` `⚪` `●` `○` `■` `□` `⬛` `⬜` `★` `☆` `◆` `◇`

`▲` `△` `▶` `▷` `▼` `▽` `◀` `◁` `◐` `◑` `♀` `♂`

`♤` `♡` `♧` `♢` `♠` `♥` `♣` `♦` `•` `❖` `✓` `✔`

`✕` `✖` `✗` `✘` `✙` `✚` `✛` `✜` `✝` `✞` `✟` `✠`

`✡` `✢` `✣` `✤` `✥` `✦` `✧` `✨` `✩` `✪` `✫` `✬`

`✭` `✮` `✯` `✰` `✱` `✲` `✳` `✴` `✵` `✶` `✷` `✸`

`✹` `✺` `✻` `✼` `✽` `✾` `✿` `❀` `❁` `❂` `❃` `❄`

`❅` `❆` `❇` `❈` `❉` `❊` `❋` `❌` `❍` `❎` `❏` `❐`

`❑` `❒` `◈` `◉` `◊` `○` `◢` `◣` `◤` `◥` `❢` `❣`

`❤` `卍` `卐` `囍`

</details>

<details>
<summary>softkeys_input_symbol / xml-television-v13：42 項，長按 0 個位置</summary>

來源：[softkeys_input_symbol.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml-television-v13/softkeys_input_symbol.xml)；SHA-256：`d38b20c6aa7b006be92ba52d66379116a35571569199fb186deedb5ee5f27b9d`。

`!` `\@` `\#` `$` `%` `^` `&` `*` `(` `)` `,` `.`

`\?` `…` <code>`</code> `:` `;` `“` `”` `"` `'` `°` `[` `]`

`{` `}` `<` `>` `_` `-` `+` `=` `×` `÷` `￥` `£`

`€` `¢` `/` `\\` `~` <code>|</code>

</details>

#### Emoji：完整資料清單

<details>
<summary>softkeys_input_emoji_category_activity / base：77 項，長按 72 個位置</summary>

來源：[softkeys_input_emoji_category_activity.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_emoji_category_activity.xml)；SHA-256：`7c36e32fec93dbf0d3f29471218062ecfc17861c4a179bf24cdf0a99f9d7aed7`。

`🎃` `🎄` `🎆` `🎇` `✨` `🎈` `🎉` `🎊` `🎋` `🎍` `🎎` `🎏`

`🎐` `🎑` `🎀` `🎁` `🎗️` `🎟️` `🎫` `🎖️` `🏆` `🏅` `🥇` `🥈`

`🥉` `⚽️` `⚾️` `🏀` `🏐` `🏈` `🏉` `🎾` `🎱` `🎳` `🏏` `🏑`

`🏒` `🏓` `🏸` `🥊` `🥋` `⛳️` `🏌️` `⛸️` `🎣` `🎽` `🎿` `⛷️`

`🏂` `🏄` `🏇` `🏊` `⛹️` `🏋️` `🚴` `🚵` `🏎️` `🏍️` `🤸` `🤻`

`🤼` `🤽` `🤾` `🤺` `🥅` `🤹` `🎯` `🎮` `🕹️` `🎲` `♠️` `♥️`

`♦️` `♣️` `🃏` `🀄️` `🎴`

長按對應（含原選項的重複位置）：

- `🏄` → `🏄` `🏄🏻` `🏄🏼` `🏄🏽` `🏄🏾` `🏄🏿`
- `🏊` → `🏊` `🏊🏻` `🏊🏼` `🏊🏽` `🏊🏾` `🏊🏿`
- `⛹️` → `⛹️` `⛹️🏻` `⛹️🏼` `⛹️🏽` `⛹️🏾` `⛹️🏿`
- `🏋️` → `🏋️` `🏋️🏻` `🏋️🏼` `🏋️🏽` `🏋️🏾` `🏋️🏿`
- `🚴` → `🚴` `🚴🏻` `🚴🏼` `🚴🏽` `🚴🏾` `🚴🏿`
- `🚵` → `🚵` `🚵🏻` `🚵🏼` `🚵🏽` `🚵🏾` `🚵🏿`
- `🤸` → `🤸` `🤸🏻` `🤸🏼` `🤸🏽` `🤸🏾` `🤸🏿`
- `🤻` → `🤻` `🤻🏻` `🤻🏼` `🤻🏽` `🤻🏾` `🤻🏿`
- `🤼` → `🤼` `🤼🏻` `🤼🏼` `🤼🏽` `🤼🏾` `🤼🏿`
- `🤽` → `🤽` `🤽🏻` `🤽🏼` `🤽🏽` `🤽🏾` `🤽🏿`
- `🤾` → `🤾` `🤾🏻` `🤾🏼` `🤾🏽` `🤾🏾` `🤾🏿`
- `🤹` → `🤹` `🤹🏻` `🤹🏼` `🤹🏽` `🤹🏾` `🤹🏿`

</details>

<details>
<summary>softkeys_input_emoji_category_animals_nature / base：107 項，長按 0 個位置</summary>

來源：[softkeys_input_emoji_category_animals_nature.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_emoji_category_animals_nature.xml)；SHA-256：`4f0e527788a7e774471f7c3faa424a37acb3184dec3199287134ccbab20f0173`。

`🐵` `🐒` `🦍` `🐶` `🐕` `🐩` `🐺` `🦊` `🐱` `🐈` `🦁` `🐯`

`🐅` `🐆` `🐴` `🐎` `🦌` `🦄` `🐮` `🐂` `🐃` `🐄` `🐷` `🐖`

`🐗` `🐽` `🐏` `🐑` `🐐` `🐪` `🐫` `🐘` `🦏` `🐭` `🐁` `🐀`

`🐹` `🐰` `🐇` `🐿️` `🦇` `🐻` `🐨` `🐼` `🐾` `🦃` `🐔` `🐓`

`🐣` `🐤` `🐥` `🐦` `🐧` `🕊️` `🦅` `🦆` `🦉` `🐸` `🐊` `🐢`

`🦎` `🐍` `🐲` `🐉` `🐳` `🐋` `🐬` `🐟` `🐠` `🐡` `🦈` `🐙`

`🐚` `🦀` `🦐` `🦑` `🦋` `🐌` `🐛` `🐜` `🐝` `🐞` `🕷️` `🕸️`

`🦂` `💐` `🌸` `💮` `🏵️` `🌹` `🥀` `🌺` `🌻` `🌼` `🌷` `🌱`

`🌲` `🌳` `🌴` `🌵` `🌾` `🌿` `☘️` `🍀` `🍁` `🍂` `🍃`

</details>

<details>
<summary>softkeys_input_emoji_category_flags / base：234 項，長按 0 個位置</summary>

來源：[softkeys_input_emoji_category_flags.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_emoji_category_flags.xml)；SHA-256：`e1d581e10264f5768f3b941f8e88abcf8f9fd9b0619eef97699aee03dd2817fa`。

`🇦🇨` `🇦🇩` `🇦🇪` `🇦🇫` `🇦🇬` `🇦🇮` `🇦🇱` `🇦🇲` `🇦🇴` `🇦🇶` `🇦🇷` `🇦🇸`

`🇦🇹` `🇦🇺` `🇦🇼` `🇦🇽` `🇦🇿` `🇧🇦` `🇧🇧` `🇧🇩` `🇧🇪` `🇧🇫` `🇧🇬` `🇧🇭`

`🇧🇮` `🇧🇯` `🇧🇲` `🇧🇳` `🇧🇴` `🇧🇷` `🇧🇸` `🇧🇹` `🇧🇼` `🇧🇾` `🇧🇿` `🇨🇦`

`🇨🇨` `🇨🇩` `🇨🇫` `🇨🇬` `🇨🇭` `🇨🇮` `🇨🇰` `🇨🇱` `🇨🇲` `🇨🇳` `🇨🇴` `🇨🇷`

`🇨🇺` `🇨🇻` `🇨🇼` `🇨🇽` `🇨🇾` `🇨🇿` `🇩🇪` `🇩🇯` `🇩🇰` `🇩🇲` `🇩🇴` `🇩🇿`

`🇪🇨` `🇪🇪` `🇪🇬` `🇪🇷` `🇪🇸` `🇪🇹` `🇪🇺` `🇫🇮` `🇫🇯` `🇫🇲` `🇫🇴` `🇫🇷`

`🇬🇦` `🇬🇧` `🇬🇩` `🇬🇪` `🇬🇬` `🇬🇭` `🇬🇮` `🇬🇱` `🇬🇲` `🇬🇳` `🇬🇶` `🇬🇷`

`🇬🇹` `🇬🇺` `🇬🇼` `🇬🇾` `🇭🇰` `🇭🇳` `🇭🇷` `🇭🇹` `🇭🇺` `🇮🇨` `🇮🇩` `🇮🇪`

`🇮🇱` `🇮🇲` `🇮🇳` `🇮🇴` `🇮🇶` `🇮🇷` `🇮🇸` `🇮🇹` `🇯🇪` `🇯🇲` `🇯🇴` `🇯🇵`

`🇰🇪` `🇰🇬` `🇰🇭` `🇰🇮` `🇰🇲` `🇰🇳` `🇰🇵` `🇰🇷` `🇰🇼` `🇰🇾` `🇰🇿` `🇱🇦`

`🇱🇧` `🇱🇨` `🇱🇮` `🇱🇰` `🇱🇷` `🇱🇸` `🇱🇹` `🇱🇺` `🇱🇻` `🇱🇾` `🇲🇦` `🇲🇨`

`🇲🇩` `🇲🇪` `🇲🇬` `🇲🇭` `🇲🇰` `🇲🇱` `🇲🇲` `🇲🇳` `🇲🇴` `🇲🇵` `🇲🇷` `🇲🇸`

`🇲🇹` `🇲🇺` `🇲🇻` `🇲🇼` `🇲🇽` `🇲🇾` `🇲🇿` `🇳🇦` `🇳🇪` `🇳🇫` `🇳🇬` `🇳🇮`

`🇳🇱` `🇳🇴` `🇳🇵` `🇳🇷` `🇳🇺` `🇳🇿` `🇴🇲` `🇵🇦` `🇵🇪` `🇵🇫` `🇵🇬` `🇵🇭`

`🇵🇰` `🇵🇱` `🇵🇳` `🇵🇷` `🇵🇸` `🇵🇹` `🇵🇼` `🇵🇾` `🇶🇦` `🇷🇴` `🇷🇸` `🇷🇺`

`🇷🇼` `🇸🇦` `🇸🇧` `🇸🇨` `🇸🇩` `🇸🇪` `🇸🇬` `🇸🇭` `🇸🇮` `🇸🇰` `🇸🇱` `🇸🇲`

`🇸🇳` `🇸🇴` `🇸🇷` `🇸🇸` `🇸🇹` `🇸🇻` `🇸🇽` `🇸🇾` `🇸🇿` `🇹🇦` `🇹🇨` `🇹🇩`

`🇹🇬` `🇹🇭` `🇹🇯` `🇹🇰` `🇹🇱` `🇹🇲` `🇹🇳` `🇹🇴` `🇹🇷` `🇹🇹` `🇹🇻` `🇹🇼`

`🇹🇿` `🇺🇦` `🇺🇬` `🇺🇸` `🇺🇾` `🇺🇿` `🇻🇦` `🇻🇨` `🇻🇪` `🇻🇬` `🇻🇮` `🇻🇳`

`🇻🇺` `🇼🇸` `🇾🇪` `🇿🇦` `🇿🇲` `🇿🇼`

</details>

<details>
<summary>softkeys_input_emoji_category_food_drink / base：89 項，長按 0 個位置</summary>

來源：[softkeys_input_emoji_category_food_drink.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_emoji_category_food_drink.xml)；SHA-256：`317402e44a2479fa2727c816b88b0ac87594c982f16bf2dee98ea2ffd25124e2`。

`🍇` `🍈` `🍉` `🍊` `🍋` `🍌` `🍍` `🍎` `🍏` `🍐` `🍑` `🍒`

`🍓` `🍅` `🥝` `🥑` `🍆` `🥔` `🥕` `🌽` `🌶️` `🥒` `🍄` `🥜`

`🌰` `🍞` `🥐` `🥖` `🥞` `🧀` `🍖` `🍗` `🥓` `🍔` `🍟` `🍕`

`🌭` `🌮` `🌯` `🥙` `🥚` `🍳` `🥘` `🍲` `🥗` `🍿` `🍱` `🍘`

`🍙` `🍚` `🍛` `🍜` `🍝` `🍠` `🍢` `🍣` `🍤` `🍥` `🍡` `🍦`

`🍧` `🍨` `🍩` `🍪` `🎂` `🍰` `🍫` `🍬` `🍭` `🍮` `🍯` `🍼`

`🥛` `☕️` `🍵` `🍶` `🍾` `🍷` `🍸` `🍹` `🍺` `🍻` `🥂` `🥃`

`🍽️` `🍴` `🥄` `🔪` `🏺`

</details>

<details>
<summary>softkeys_input_emoji_category_objects / base：168 項，長按 0 個位置</summary>

來源：[softkeys_input_emoji_category_objects.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_emoji_category_objects.xml)；SHA-256：`cd671a4f8d9b546fe05647dfc861b5a10591c21148b15c3863c46dfc15c1d01a`。

`🔇` `🔈` `🔉` `🔊` `📢` `📣` `📯` `🔔` `🔕` `🎼` `🎵` `🎶`

`🎙️` `🎚️` `🎛️` `🎤` `🎧` `📻` `🎷` `🎸` `🎹` `🎺` `🎻` `🥁`

`📱` `📲` `☎️` `📞` `📟` `📠` `🔋` `🔌` `💻` `🖥️` `🖨️` `⌨️`

`🖱️` `🖲️` `💽` `💾` `💿` `📀` `🎥` `🎞️` `📽️` `🎬` `📺` `📷`

`📸` `📹` `📼` `🔍` `🔎` `🔬` `🔭` `📡` `🕯️` `💡` `🔦` `🏮`

`📔` `📕` `📖` `📗` `📘` `📙` `📚` `📓` `📒` `📃` `📜` `📄`

`📰` `🗞️` `📑` `🔖` `🏷️` `💰` `💴` `💵` `💶` `💷` `💸` `💳`

`💹` `💱` `💲` `✉️` `📧` `📨` `📩` `📤` `📥` `📦` `📫` `📪`

`📬` `📭` `📮` `🗳️` `✏️` `✒️` `🖋️` `🖊️` `🖌️` `🖍️` `📝` `💼`

`📁` `📂` `🗂️` `📅` `📆` `🗒️` `🗓️` `📇` `📈` `📉` `📊` `📋`

`📌` `📍` `📎` `🖇️` `📏` `📐` `✂️` `🗃️` `🗄️` `🗑️` `🔒` `🔓`

`🔏` `🔐` `🔑` `🗝️` `🔨` `⛏️` `⚒️` `🛠️` `🗡️` `⚔️` `🔫` `🥆`

`🏹` `🛡️` `🔧` `🔩` `⚙️` `🗜️` `⚗️` `⚖️` `🔗` `⛓️` `💉` `💊`

`🏁` `🏳️` `🏴` `🚩` `🎌` `🚬` `⚰️` `⚱️` `🗿` `🛢️` `🔮` `🛒`

</details>

<details>
<summary>softkeys_input_emoji_category_smiley_people / base：271 項，長按 420 個位置</summary>

來源：[softkeys_input_emoji_category_smiley_people.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_emoji_category_smiley_people.xml)；SHA-256：`20fadc1ffa9812306ebedacb7f4ee10a11f572fbbe228dd1bbc00038dea62ee2`。

`😀` `😃` `😄` `😁` `😆` `😅` `😂` `🤣` `😇` `😉` `😊` `🙂`

`🙃` `☺️` `😋` `😌` `😍` `🥰` `😘` `😗` `😙` `😚` `🥲` `🤪`

`😜` `😝` `😛` `🤑` `😎` `🤓` `🥸` `🧐` `🤠` `🥳` `🤗` `🤡`

`😏` `😶` `😐` `😑` `😒` `🙄` `🤨` `🤔` `🤫` `🤭` `🤥` `😳`

`😞` `😟` `😠` `😡` `🤬` `😔` `😕` `🙁` `☹️` `😬` `🥺` `😣`

`😖` `😫` `😩` `🥱` `😤` `😮‍💨` `😮` `😱` `😨` `😰` `😯` `😦`

`😧` `😢` `😥` `😪` `🤤` `😓` `😭` `🤩` `😵` `😵‍💫` `🥴` `😲`

`🤯` `🤐` `😷` `🤕` `🤒` `🤮` `🤢` `🤧` `🥵` `🥶` `😶‍🌫️` `😴`

`😺` `😸` `😹` `😻` `😼` `😽` `🙀` `😿` `😾` `🙈` `🙉` `🙊`

`👦` `👧` `👨` `👩` `👴` `👵` `👶` `👱` `👮` `👲` `👳` `👷`

`⛑️` `👸` `🤴` `💂` `🕵️` `🎅` `🤶` `👰` `🤵` `👼` `💆` `💇`

`🙍` `🙎` `🙅` `🙆` `💁` `🤷` `🙋` `🤦` `🙇` `🙌` `🙏` `🗣️`

`👤` `👥` `🤰` `🚶` `🏃` `👯` `💃` `🕺` `🕴️` `👫` `👬` `👭`

`💏` `👨‍❤️‍💋‍👨` `👩‍❤️‍💋‍👩` `💑` `👨‍❤️‍👨` `👩‍❤️‍👩` `👪` `👨‍👩‍👧` `👨‍👩‍👧‍👦` `👨‍👩‍👦‍👦` `👨‍👩‍👧‍👧` `👨‍👨‍👦`

`👨‍👨‍👧` `👨‍👨‍👧‍👦` `👨‍👨‍👦‍👦` `👨‍👨‍👧‍👧` `👩‍👩‍👦` `👩‍👩‍👧` `👩‍👩‍👧‍👦` `👩‍👩‍👦‍👦` `👩‍👩‍👧‍👧` `💪` `🤳` `👈`

`👉` `☝️` `👆` `🖕` `👇` `✌️` `🤞` `🖖` `🤘` `🤙` `🖐️` `✋`

`👌` `👍` `👎` `✊` `👊` `🤛` `🤜` `🤚` `👋` `👏` `👐` `✍️`

`🤝` `💅` `👂` `👃` `👣` `👀` `👁️` `👁️‍🗨️` `👅` `👄` `💋` `💘`

`❤️` `💓` `💔` `💕` `💖` `💗` `💙` `💚` `💛` `💜` `🖤` `💝`

`💞` `💟` `❣️` `💌` `💤` `💢` `💣` `💥` `💦` `💨` `💫` `💬`

`🗨️` `🗯️` `💭` `🕳️` `👓` `🕶️` `👔` `👕` `👖` `👗` `👘` `👙`

`👚` `👛` `👜` `👝` `🛍️` `🎒` `👞` `👟` `👠` `👡` `👢` `👑`

`👒` `🎩` `🎓` `📿` `💄` `💍` `💎`

長按對應（含原選項的重複位置）：

- `👦` → `👦` `👦🏻` `👦🏼` `👦🏽` `👦🏾` `👦🏿`
- `👧` → `👧` `👧🏻` `👧🏼` `👧🏽` `👧🏾` `👧🏿`
- `👨` → `👨` `👨🏻` `👨🏼` `👨🏽` `👨🏾` `👨🏿`
- `👩` → `👩` `👩🏻` `👩🏼` `👩🏽` `👩🏾` `👩🏿`
- `👴` → `👴` `👴🏻` `👴🏼` `👴🏽` `👴🏾` `👴🏿`
- `👵` → `👵` `👵🏻` `👵🏼` `👵🏽` `👵🏾` `👵🏿`
- `👶` → `👶` `👶🏻` `👶🏼` `👶🏽` `👶🏾` `👶🏿`
- `👱` → `👱` `👱🏻` `👱🏼` `👱🏽` `👱🏾` `👱🏿`
- `👮` → `👮` `👮🏻` `👮🏼` `👮🏽` `👮🏾` `👮🏿`
- `👲` → `👲` `👲🏻` `👲🏼` `👲🏽` `👲🏾` `👲🏿`
- `👳` → `👳` `👳🏻` `👳🏼` `👳🏽` `👳🏾` `👳🏿`
- `👷` → `👷` `👷🏻` `👷🏼` `👷🏽` `👷🏾` `👷🏿`
- `👸` → `👸` `👸🏻` `👸🏼` `👸🏽` `👸🏾` `👸🏿`
- `🤴` → `🤴` `🤴🏻` `🤴🏼` `🤴🏽` `🤴🏾` `🤴🏿`
- `💂` → `💂` `💂🏻` `💂🏼` `💂🏽` `💂🏾` `💂🏿`
- `🕵️` → `🕵️` `🕵️🏻` `🕵️🏼` `🕵️🏽` `🕵️🏾` `🕵️🏿`
- `🎅` → `🎅` `🎅🏻` `🎅🏼` `🎅🏽` `🎅🏾` `🎅🏿`
- `🤶` → `🤶` `🤶🏻` `🤶🏼` `🤶🏽` `🤶🏾` `🤶🏿`
- `👰` → `👰` `👰🏻` `👰🏼` `👰🏽` `👰🏾` `👰🏿`
- `🤵` → `🤵` `🤵🏻` `🤵🏼` `🤵🏽` `🤵🏾` `🤵🏿`
- `👼` → `👼` `👼🏻` `👼🏼` `👼🏽` `👼🏾` `👼🏿`
- `💆` → `💆` `💆🏻` `💆🏼` `💆🏽` `💆🏾` `💆🏿`
- `💇` → `💇` `💇🏻` `💇🏼` `💇🏽` `💇🏾` `💇🏿`
- `🙍` → `🙍` `🙍🏻` `🙍🏼` `🙍🏽` `🙍🏾` `🙍🏿`
- `🙎` → `🙎` `🙎🏻` `🙎🏼` `🙎🏽` `🙎🏾` `🙎🏿`
- `🙅` → `🙅` `🙅🏻` `🙅🏼` `🙅🏽` `🙅🏾` `🙅🏿`
- `🙆` → `🙆` `🙆🏻` `🙆🏼` `🙆🏽` `🙆🏾` `🙆🏿`
- `💁` → `💁` `💁🏻` `💁🏼` `💁🏽` `💁🏾` `💁🏿`
- `🤷` → `🤷` `🤷🏻` `🤷🏼` `🤷🏽` `🤷🏾` `🤷🏿`
- `🙋` → `🙋` `🙋🏻` `🙋🏼` `🙋🏽` `🙋🏾` `🙋🏿`
- `🤦` → `🤦` `🤦🏻` `🤦🏼` `🤦🏽` `🤦🏾` `🤦🏿`
- `🙇` → `🙇` `🙇🏻` `🙇🏼` `🙇🏽` `🙇🏾` `🙇🏿`
- `🙌` → `🙌` `🙌🏻` `🙌🏼` `🙌🏽` `🙌🏾` `🙌🏿`
- `🙏` → `🙏` `🙏🏻` `🙏🏼` `🙏🏽` `🙏🏾` `🙏🏿`
- `🤰` → `🤰` `🤰🏻` `🤰🏼` `🤰🏽` `🤰🏾` `🤰🏿`
- `🚶` → `🚶` `🚶🏻` `🚶🏼` `🚶🏽` `🚶🏾` `🚶🏿`
- `🏃` → `🏃` `🏃🏻` `🏃🏼` `🏃🏽` `🏃🏾` `🏃🏿`
- `💃` → `💃` `💃🏻` `💃🏼` `💃🏽` `💃🏾` `💃🏿`
- `🕺` → `🕺` `🕺🏻` `🕺🏼` `🕺🏽` `🕺🏾` `🕺🏿`
- `💪` → `💪` `💪🏻` `💪🏼` `💪🏽` `💪🏾` `💪🏿`
- `🤳` → `🤳` `🤳🏻` `🤳🏼` `🤳🏽` `🤳🏾` `🤳🏿`
- `👈` → `👈` `👈🏻` `👈🏼` `👈🏽` `👈🏾` `👈🏿`
- `👉` → `👉` `👉🏻` `👉🏼` `👉🏽` `👉🏾` `👉🏿`
- `☝️` → `☝️` `☝️🏻` `☝️🏼` `☝️🏽` `☝️🏾` `☝️🏿`
- `👆` → `👆` `👆🏻` `👆🏼` `👆🏽` `👆🏾` `👆🏿`
- `🖕` → `🖕` `🖕🏻` `🖕🏼` `🖕🏽` `🖕🏾` `🖕🏿`
- `👇` → `👇` `👇🏻` `👇🏼` `👇🏽` `👇🏾` `👇🏿`
- `✌️` → `✌️` `✌️🏻` `✌️🏼` `✌️🏽` `✌️🏾` `✌️🏿`
- `🤞` → `🤞` `🤞🏻` `🤞🏼` `🤞🏽` `🤞🏾` `🤞🏿`
- `🖖` → `🖖` `🖖🏻` `🖖🏼` `🖖🏽` `🖖🏾` `🖖🏿`
- `🤘` → `🤘` `🤘🏻` `🤘🏼` `🤘🏽` `🤘🏾` `🤘🏿`
- `🤙` → `🤙` `🤙🏻` `🤙🏼` `🤙🏽` `🤙🏾` `🤙🏿`
- `🖐️` → `🖐️` `🖐️🏻` `🖐️🏼` `🖐️🏽` `🖐️🏾` `🖐️🏿`
- `✋` → `✋` `✋🏻` `✋🏼` `✋🏽` `✋🏾` `✋🏿`
- `👌` → `👌` `👌🏻` `👌🏼` `👌🏽` `👌🏾` `👌🏿`
- `👍` → `👍` `👍🏻` `👍🏼` `👍🏽` `👍🏾` `👍🏿`
- `👎` → `👎` `👎🏻` `👎🏼` `👎🏽` `👎🏾` `👎🏿`
- `✊` → `✊` `✊🏻` `✊🏼` `✊🏽` `✊🏾` `✊🏿`
- `👊` → `👊` `👊🏻` `👊🏼` `👊🏽` `👊🏾` `👊🏿`
- `🤛` → `🤛` `🤛🏻` `🤛🏼` `🤛🏽` `🤛🏾` `🤛🏿`
- `🤜` → `🤜` `🤜🏻` `🤜🏼` `🤜🏽` `🤜🏾` `🤜🏿`
- `🤚` → `🤚` `🤚🏻` `🤚🏼` `🤚🏽` `🤚🏾` `🤚🏿`
- `👋` → `👋` `👋🏻` `👋🏼` `👋🏽` `👋🏾` `👋🏿`
- `👏` → `👏` `👏🏻` `👏🏼` `👏🏽` `👏🏾` `👏🏿`
- `👐` → `👐` `👐🏻` `👐🏼` `👐🏽` `👐🏾` `👐🏿`
- `✍️` → `✍️` `✍️🏻` `✍️🏼` `✍️🏽` `✍️🏾` `✍️🏿`
- `🤝` → `🤝` `🤝🏻` `🤝🏼` `🤝🏽` `🤝🏾` `🤝🏿`
- `💅` → `💅` `💅🏻` `💅🏼` `💅🏽` `💅🏾` `💅🏿`
- `👂` → `👂` `👂🏻` `👂🏼` `👂🏽` `👂🏾` `👂🏿`
- `👃` → `👃` `👃🏻` `👃🏼` `👃🏽` `👃🏾` `👃🏿`

</details>

<details>
<summary>softkeys_input_emoji_category_symbols / base：202 項，長按 0 個位置</summary>

來源：[softkeys_input_emoji_category_symbols.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_emoji_category_symbols.xml)；SHA-256：`1881a948613720b98f3272a2a1fd6fce4d8f9496f445a241af64a423e2b5384c`。

`🏧` `🚮` `🚰` `♿️` `🚹` `🚺` `🚻` `🚼` `🚾` `🛂` `🛃` `🛄`

`🛅` `⚠️` `🚸` `⛔️` `🚫` `🚳` `🚭` `🚯` `🚱` `🚷` `☢️` `☣️`

`⬆️` `↗️` `➡️` `↘️` `⬇️` `↙️` `⬅️` `↖️` `↕️` `↔️` `↩️` `↪️`

`⤴️` `⤵️` `🔃` `🔄` `🔙` `🔚` `🔛` `🔜` `🔝` `🛐` `⚛️` `🕉️`

`✡️` `☸️` `☯️` `✝️` `☦️` `☪️` `☮️` `🕎` `🔯` `♻️` `📛` `⚜️`

`🔰` `🔱` `⭕️` `✅` `☑️` `✔️` `✖️` `❌` `❎` `➕` `➖` `➗`

`➰` `➿` `〽️` `✳️` `✴️` `❇️` `‼️` `⁉️` `❓` `❔` `❕` `❗️`

`〰️` `©` `®` `™️` `♈️` `♉️` `♊️` `♋️` `♌️` `♍️` `♎️` `♏️`

`♐️` `♑️` `♒️` `♓️` `⛎` `🔀` `🔁` `🔂` `▶️` `⏩` `⏭️` `⏯️`

`◀️` `⏪` `⏮️` `🔼` `⏫` `🔽` `⏬` `⏸️` `⏹️` `⏺️` `⏏️` `🎦`

`🔅` `🔆` `📶` `📵` `📳` `📴` `#⃣` `*⃣` `0⃣` `1⃣` `2⃣` `3⃣`

`4⃣` `5⃣` `6⃣` `7⃣` `8⃣` `9⃣` `🔟` `💯` `🔞` `🔠` `🔡` `🔢`

`🔣` `🔤` `🅰️` `🆎` `🅱️` `🆑` `🆒` `🆓` `ℹ️` `🆔` `Ⓜ️` `🆕`

`🆖` `🅾️` `🆗` `🅿️` `🆘` `🆙` `🆚` `🈁` `🈂️` `🈷️` `🈶` `🈯️`

`🉐` `🈹` `🈚️` `🈲` `🉑` `🈸` `🈴` `🈳` `㊗️` `㊙️` `🈺` `🈵`

`▪️` `▫️` `◻️` `◼️` `◽️` `◾️` `⬛️` `⬜️` `🔶` `🔷` `🔸` `🔹`

`🔺` `🔻` `💠` `🔘` `🔲` `🔳` `⚪️` `⚫️` `🔴` `🔵`

</details>

<details>
<summary>softkeys_input_emoji_category_travel_places / base：209 項，長按 12 個位置</summary>

來源：[softkeys_input_emoji_category_travel_places.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_emoji_category_travel_places.xml)；SHA-256：`8c11704e5f1fa984e66088272d9eaab379f716acce5acd04432da8e46f460351`。

`🌍` `🌎` `🌏` `🌐` `🗺️` `🗾` `🏔️` `⛰️` `🌋` `🗻` `🏕️` `🏖️`

`🏜️` `🏝️` `🏞️` `🏟️` `🏛️` `🏗️` `🏘️` `🏙️` `🏚️` `🏠` `🏡` `🏢`

`🏣` `🏤` `🏥` `🏦` `🏨` `🏩` `🏪` `🏫` `🏬` `🏭` `🏯` `🏰`

`💒` `🗼` `🗽` `⛪️` `🕌` `🕍` `⛩️` `🕋` `⛲️` `⛺️` `🌁` `🌃`

`🌄` `🌅` `🌆` `🌇` `🌉` `♨️` `🌌` `🎠` `🎡` `🎢` `💈` `🎪`

`🎭` `🖼️` `🎨` `🎰` `🚂` `🚃` `🚄` `🚅` `🚆` `🚇` `🚈` `🚉`

`🚊` `🚝` `🚞` `🚋` `🚌` `🚍` `🚎` `🚏` `🚐` `🚑` `🚒` `🚓`

`🚔` `🚕` `🚖` `🚗` `🚘` `🚙` `🚚` `🚛` `🚜` `🚲` `⛽️` `🛣️`

`🛤️` `🚨` `🚥` `🚦` `🚧` `🛑` `🛴` `🛵` `⚓️` `⛵️` `🚣` `🛶`

`🚤` `🛳️` `⛴️` `🛥️` `🚢` `✈️` `🛩️` `🛫` `🛬` `💺` `🚁` `🚟`

`🚠` `🚡` `🚀` `🛰️` `🛎️` `🚪` `🛌` `🛏️` `🛋️` `🚽` `🚿` `🛀`

`🛁` `⌛️` `⏳` `⌚️` `⏰` `⏱️` `⏲️` `🕰️` `🕛` `🕧` `🕐` `🕜`

`🕑` `🕝` `🕒` `🕞` `🕓` `🕟` `🕔` `🕠` `🕕` `🕡` `🕖` `🕢`

`🕗` `🕣` `🕘` `🕤` `🕙` `🕥` `🕚` `🕦` `🌑` `🌒` `🌓` `🌔`

`🌕` `🌖` `🌗` `🌘` `🌙` `🌚` `🌛` `🌜` `🌡️` `☀️` `🌝` `🌞`

`⭐️` `🌟` `🌠` `☁️` `⛅️` `⛈️` `🌤️` `🌥️` `🌦️` `🌧️` `🌨️` `🌩️`

`🌪️` `🌫️` `🌬️` `🌀` `🌈` `🌂` `☂️` `☔️` `⛱️` `⚡️` `❄️` `☃️`

`⛄️` `☄️` `🔥` `💧` `🌊`

長按對應（含原選項的重複位置）：

- `🚣` → `🚣` `🚣🏻` `🚣🏼` `🚣🏽` `🚣🏾` `🚣🏿`
- `🛀` → `🛀` `🛀🏻` `🛀🏼` `🛀🏽` `🛀🏾` `🛀🏿`

</details>

<details>
<summary>softkeys_input_emoji_category_activity / xml-v25：75 項，長按 146 個位置</summary>

來源：[softkeys_input_emoji_category_activity.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml-v25/softkeys_input_emoji_category_activity.xml)；SHA-256：`ee7fc782bdb8e061a6d0c88aa3c0e4398ece9374681b666304136771d4a16a49`。

`🎃` `🎄` `🎆` `🎇` `✨` `🎈` `🎉` `🎊` `🎋` `🎍` `🎎` `🎏`

`🎐` `🎑` `🎀` `🎁` `🎗️` `🎟️` `🎫` `🎖️` `🏆` `🏅` `🥇` `🥈`

`🥉` `⚽` `⚾` `🏀` `🏐` `🏈` `🏉` `🎾` `🎱` `🎳` `🏏` `🏑`

`🏒` `🏓` `🏸` `🥊` `🥋` `🥅` `🎯` `⛳` `⛸️` `🎣` `🎽` `🎿`

`🤺` `🏇` `⛷️` `🏂` `🏌️` `🏄` `🚣` `🏊` `⛹️` `🏋️` `🚴` `🚵`

`🤸` `🤼` `🤽` `🤾` `🤹` `🎮` `🕹️` `🎲` `♠️` `♥️` `♦️` `♣️`

`🃏` `🀄` `🎴`

長按對應（含原選項的重複位置）：

- `🏌️` → `🏌️‍♂️` `🏌️‍♀️`
- `🏄` → `🏄‍♂️` `🏄🏻‍♂️` `🏄🏼‍♂️` `🏄🏽‍♂️` `🏄🏾‍♂️` `🏄🏿‍♂️` `🏄‍♀️` `🏄🏻‍♀️` `🏄🏼‍♀️` `🏄🏽‍♀️` `🏄🏾‍♀️` `🏄🏿‍♀️`
- `🚣` → `🚣‍♂️` `🚣🏻‍♂️` `🚣🏼‍♂️` `🚣🏽‍♂️` `🚣🏾‍♂️` `🚣🏿‍♂️` `🚣‍♀️` `🚣🏻‍♀️` `🚣🏼‍♀️` `🚣🏽‍♀️` `🚣🏾‍♀️` `🚣🏿‍♀️`
- `🏊` → `🏊‍♂️` `🏊🏻‍♂️` `🏊🏼‍♂️` `🏊🏽‍♂️` `🏊🏾‍♂️` `🏊🏿‍♂️` `🏊‍♀️` `🏊🏻‍♀️` `🏊🏼‍♀️` `🏊🏽‍♀️` `🏊🏾‍♀️` `🏊🏿‍♀️`
- `⛹️` → `⛹️‍♂️` `⛹🏻‍♂️` `⛹🏼‍♂️` `⛹🏽‍♂️` `⛹🏾‍♂️` `⛹🏿‍♂️` `⛹️‍♀️` `⛹🏻‍♀️` `⛹🏼‍♀️` `⛹🏽‍♀️` `⛹🏾‍♀️` `⛹🏿‍♀️`
- `🏋️` → `🏋️‍♂️` `🏋🏻‍♂️` `🏋🏼‍♂️` `🏋🏽‍♂️` `🏋🏾‍♂️` `🏋🏿‍♂️` `🏋️‍♀️` `🏋🏻‍♀️` `🏋🏼‍♀️` `🏋🏽‍♀️` `🏋🏾‍♀️` `🏋🏿‍♀️`
- `🚴` → `🚴‍♂️` `🚴🏻‍♂️` `🚴🏼‍♂️` `🚴🏽‍♂️` `🚴🏾‍♂️` `🚴🏿‍♂️` `🚴‍♀️` `🚴🏻‍♀️` `🚴🏼‍♀️` `🚴🏽‍♀️` `🚴🏾‍♀️` `🚴🏿‍♀️`
- `🚵` → `🚵‍♂️` `🚵🏻‍♂️` `🚵🏼‍♂️` `🚵🏽‍♂️` `🚵🏾‍♂️` `🚵🏿‍♂️` `🚵‍♀️` `🚵🏻‍♀️` `🚵🏼‍♀️` `🚵🏽‍♀️` `🚵🏾‍♀️` `🚵🏿‍♀️`
- `🤸` → `🤸‍♂️` `🤸🏻‍♂️` `🤸🏼‍♂️` `🤸🏽‍♂️` `🤸🏾‍♂️` `🤸🏿‍♂️` `🤸‍♀️` `🤸🏻‍♀️` `🤸🏼‍♀️` `🤸🏽‍♀️` `🤸🏾‍♀️` `🤸🏿‍♀️`
- `🤼` → `🤼‍♂️` `🤼🏻‍♂️` `🤼🏼‍♂️` `🤼🏽‍♂️` `🤼🏾‍♂️` `🤼🏿‍♂️` `🤼‍♀️` `🤼🏻‍♀️` `🤼🏼‍♀️` `🤼🏽‍♀️` `🤼🏾‍♀️` `🤼🏿‍♀️`
- `🤽` → `🤽‍♂️` `🤽🏻‍♂️` `🤽🏼‍♂️` `🤽🏽‍♂️` `🤽🏾‍♂️` `🤽🏿‍♂️` `🤽‍♀️` `🤽🏻‍♀️` `🤽🏼‍♀️` `🤽🏽‍♀️` `🤽🏾‍♀️` `🤽🏿‍♀️`
- `🤾` → `🤾‍♂️` `🤾🏻‍♂️` `🤾🏼‍♂️` `🤾🏽‍♂️` `🤾🏾‍♂️` `🤾🏿‍♂️` `🤾‍♀️` `🤾🏻‍♀️` `🤾🏼‍♀️` `🤾🏽‍♀️` `🤾🏾‍♀️` `🤾🏿‍♀️`
- `🤹` → `🤹‍♂️` `🤹🏻‍♂️` `🤹🏼‍♂️` `🤹🏽‍♂️` `🤹🏾‍♂️` `🤹🏿‍♂️` `🤹‍♀️` `🤹🏻‍♀️` `🤹🏼‍♀️` `🤹🏽‍♀️` `🤹🏾‍♀️` `🤹🏿‍♀️`

</details>

<details>
<summary>softkeys_input_emoji_category_animals_nature / xml-v25：107 項，長按 0 個位置</summary>

來源：[softkeys_input_emoji_category_animals_nature.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml-v25/softkeys_input_emoji_category_animals_nature.xml)；SHA-256：`4f0e527788a7e774471f7c3faa424a37acb3184dec3199287134ccbab20f0173`。

`🐵` `🐒` `🦍` `🐶` `🐕` `🐩` `🐺` `🦊` `🐱` `🐈` `🦁` `🐯`

`🐅` `🐆` `🐴` `🐎` `🦌` `🦄` `🐮` `🐂` `🐃` `🐄` `🐷` `🐖`

`🐗` `🐽` `🐏` `🐑` `🐐` `🐪` `🐫` `🐘` `🦏` `🐭` `🐁` `🐀`

`🐹` `🐰` `🐇` `🐿️` `🦇` `🐻` `🐨` `🐼` `🐾` `🦃` `🐔` `🐓`

`🐣` `🐤` `🐥` `🐦` `🐧` `🕊️` `🦅` `🦆` `🦉` `🐸` `🐊` `🐢`

`🦎` `🐍` `🐲` `🐉` `🐳` `🐋` `🐬` `🐟` `🐠` `🐡` `🦈` `🐙`

`🐚` `🦀` `🦐` `🦑` `🦋` `🐌` `🐛` `🐜` `🐝` `🐞` `🕷️` `🕸️`

`🦂` `💐` `🌸` `💮` `🏵️` `🌹` `🥀` `🌺` `🌻` `🌼` `🌷` `🌱`

`🌲` `🌳` `🌴` `🌵` `🌾` `🌿` `☘️` `🍀` `🍁` `🍂` `🍃`

</details>

<details>
<summary>softkeys_input_emoji_category_flags / xml-v25：259 項，長按 0 個位置</summary>

來源：[softkeys_input_emoji_category_flags.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml-v25/softkeys_input_emoji_category_flags.xml)；SHA-256：`1f0fa7b73ef29eefef19a6c78e158d091afe5a33337b26081bed86f83c10c0b1`。

`🏁` `🚩` `🎌` `🏴` `🏳️` `🏳️‍🌈` `🇦🇨` `🇦🇩` `🇦🇪` `🇦🇫` `🇦🇬` `🇦🇮`

`🇦🇱` `🇦🇲` `🇦🇴` `🇦🇶` `🇦🇷` `🇦🇸` `🇦🇹` `🇦🇺` `🇦🇼` `🇦🇽` `🇦🇿` `🇧🇦`

`🇧🇧` `🇧🇩` `🇧🇪` `🇧🇫` `🇧🇬` `🇧🇭` `🇧🇮` `🇧🇯` `🇧🇱` `🇧🇲` `🇧🇳` `🇧🇴`

`🇧🇶` `🇧🇷` `🇧🇸` `🇧🇹` `🇧🇼` `🇧🇾` `🇧🇿` `🇨🇦` `🇨🇨` `🇨🇩` `🇨🇫` `🇨🇬`

`🇨🇭` `🇨🇮` `🇨🇰` `🇨🇱` `🇨🇲` `🇨🇳` `🇨🇴` `🇨🇷` `🇨🇺` `🇨🇻` `🇨🇼` `🇨🇽`

`🇨🇾` `🇨🇿` `🇩🇪` `🇩🇬` `🇩🇯` `🇩🇰` `🇩🇲` `🇩🇴` `🇩🇿` `🇪🇦` `🇪🇨` `🇪🇪`

`🇪🇬` `🇪🇭` `🇪🇷` `🇪🇸` `🇪🇹` `🇪🇺` `🇫🇮` `🇫🇯` `🇫🇰` `🇫🇲` `🇫🇴` `🇫🇷`

`🇬🇦` `🇬🇧` `🇬🇩` `🇬🇪` `🇬🇫` `🇬🇬` `🇬🇭` `🇬🇮` `🇬🇱` `🇬🇲` `🇬🇳` `🇬🇵`

`🇬🇶` `🇬🇷` `🇬🇸` `🇬🇹` `🇬🇺` `🇬🇼` `🇬🇾` `🇭🇰` `🇭🇳` `🇭🇷` `🇭🇹` `🇭🇺`

`🇮🇨` `🇮🇩` `🇮🇪` `🇮🇱` `🇮🇲` `🇮🇳` `🇮🇴` `🇮🇶` `🇮🇷` `🇮🇸` `🇮🇹` `🇯🇪`

`🇯🇲` `🇯🇴` `🇯🇵` `🇰🇪` `🇰🇬` `🇰🇭` `🇰🇮` `🇰🇲` `🇰🇳` `🇰🇵` `🇰🇷` `🇰🇼`

`🇰🇾` `🇰🇿` `🇱🇦` `🇱🇧` `🇱🇨` `🇱🇮` `🇱🇰` `🇱🇷` `🇱🇸` `🇱🇹` `🇱🇺` `🇱🇻`

`🇱🇾` `🇲🇦` `🇲🇨` `🇲🇩` `🇲🇪` `🇲🇫` `🇲🇬` `🇲🇭` `🇲🇰` `🇲🇱` `🇲🇲` `🇲🇳`

`🇲🇴` `🇲🇵` `🇲🇶` `🇲🇷` `🇲🇸` `🇲🇹` `🇲🇺` `🇲🇻` `🇲🇼` `🇲🇽` `🇲🇾` `🇲🇿`

`🇳🇦` `🇳🇨` `🇳🇪` `🇳🇫` `🇳🇬` `🇳🇮` `🇳🇱` `🇳🇴` `🇳🇵` `🇳🇷` `🇳🇺` `🇳🇿`

`🇴🇲` `🇵🇦` `🇵🇪` `🇵🇫` `🇵🇬` `🇵🇭` `🇵🇰` `🇵🇱` `🇵🇲` `🇵🇳` `🇵🇷` `🇵🇸`

`🇵🇹` `🇵🇼` `🇵🇾` `🇶🇦` `🇷🇪` `🇷🇴` `🇷🇸` `🇷🇺` `🇷🇼` `🇸🇦` `🇸🇧` `🇸🇨`

`🇸🇩` `🇸🇪` `🇸🇬` `🇸🇭` `🇸🇮` `🇸🇰` `🇸🇱` `🇸🇲` `🇸🇳` `🇸🇴` `🇸🇷` `🇸🇸`

`🇸🇹` `🇸🇻` `🇸🇽` `🇸🇾` `🇸🇿` `🇹🇦` `🇹🇨` `🇹🇩` `🇹🇫` `🇹🇬` `🇹🇭` `🇹🇯`

`🇹🇰` `🇹🇱` `🇹🇲` `🇹🇳` `🇹🇴` `🇹🇷` `🇹🇹` `🇹🇻` `🇹🇼` `🇹🇿` `🇺🇦` `🇺🇬`

`🇺🇳` `🇺🇸` `🇺🇾` `🇺🇿` `🇻🇦` `🇻🇨` `🇻🇪` `🇻🇬` `🇻🇮` `🇻🇳` `🇻🇺` `🇼🇫`

`🇼🇸` `🇽🇰` `🇾🇪` `🇾🇹` `🇿🇦` `🇿🇲` `🇿🇼`

</details>

<details>
<summary>softkeys_input_emoji_category_food_drink / xml-v25：89 項，長按 0 個位置</summary>

來源：[softkeys_input_emoji_category_food_drink.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml-v25/softkeys_input_emoji_category_food_drink.xml)；SHA-256：`fcfb7ee2a58ba9ebe6c5cbe2c7a671ccb8c4144798e0f3b6f4c518795411b78e`。

`🍇` `🍈` `🍉` `🍊` `🍋` `🍌` `🍍` `🍎` `🍏` `🍐` `🍑` `🍒`

`🍓` `🥝` `🍅` `🥑` `🍆` `🥔` `🥕` `🌽` `🌶️` `🥒` `🍄` `🥜`

`🌰` `🍞` `🥐` `🥖` `🥞` `🧀` `🍖` `🍗` `🥓` `🍔` `🍟` `🍕`

`🌭` `🌮` `🌯` `🥙` `🥚` `🍳` `🥘` `🍲` `🥗` `🍿` `🍱` `🍘`

`🍙` `🍚` `🍛` `🍜` `🍝` `🍠` `🍢` `🍣` `🍤` `🍥` `🍡` `🍦`

`🍧` `🍨` `🍩` `🍪` `🎂` `🍰` `🍫` `🍬` `🍭` `🍮` `🍯` `🍼`

`🥛` `☕` `🍵` `🍶` `🍾` `🍷` `🍸` `🍹` `🍺` `🍻` `🥂` `🥃`

`🍽️` `🍴` `🥄` `🔪` `🏺`

</details>

<details>
<summary>softkeys_input_emoji_category_objects / xml-v25：162 項，長按 0 個位置</summary>

來源：[softkeys_input_emoji_category_objects.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml-v25/softkeys_input_emoji_category_objects.xml)；SHA-256：`a44140b5d566e4fbf80b08d5e3ab06ef9af370c871977634428cc8663c56b834`。

`🔇` `🔈` `🔉` `🔊` `📢` `📣` `📯` `🔔` `🔕` `🎼` `🎵` `🎶`

`🎙️` `🎚️` `🎛️` `🎤` `🎧` `📻` `🎷` `🎸` `🎹` `🎺` `🎻` `🥁`

`📱` `📲` `☎️` `📞` `📟` `📠` `🔋` `🔌` `💻` `🖥️` `🖨️` `⌨️`

`🖱️` `🖲️` `💽` `💾` `💿` `📀` `🎥` `🎞️` `📽️` `🎬` `📺` `📷`

`📸` `📹` `📼` `🔍` `🔎` `🔬` `🔭` `📡` `🕯️` `💡` `🔦` `🏮`

`📔` `📕` `📖` `📗` `📘` `📙` `📚` `📓` `📒` `📃` `📜` `📄`

`📰` `🗞️` `📑` `🔖` `🏷️` `💰` `💴` `💵` `💶` `💷` `💸` `💳`

`💹` `💱` `💲` `✉️` `📧` `📨` `📩` `📤` `📥` `📦` `📫` `📪`

`📬` `📭` `📮` `🗳️` `✏️` `✒️` `🖋️` `🖊️` `🖌️` `🖍️` `📝` `💼`

`📁` `📂` `🗂️` `📅` `📆` `🗒️` `🗓️` `📇` `📈` `📉` `📊` `📋`

`📌` `📍` `📎` `🖇️` `📏` `📐` `✂️` `🗃️` `🗄️` `🗑️` `🔒` `🔓`

`🔏` `🔐` `🔑` `🗝️` `🔨` `⛏️` `⚒️` `🛠️` `🗡️` `⚔️` `🔫` `🏹`

`🛡️` `🔧` `🔩` `⚙️` `🗜️` `⚗️` `⚖️` `🔗` `⛓️` `💉` `💊` `🚬`

`⚰️` `⚱️` `🗿` `🛢️` `🔮` `🛒`

</details>

<details>
<summary>softkeys_input_emoji_category_smiley_people / xml-v25：297 項，長按 728 個位置</summary>

來源：[softkeys_input_emoji_category_smiley_people.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml-v25/softkeys_input_emoji_category_smiley_people.xml)；SHA-256：`f2d7a63e59a538f73646a76be71f7181a5a515a2c3f04aa327e0ea0b4c8b9eac`。

`😀` `😃` `😄` `😁` `😆` `😅` `😂` `🤣` `😇` `😉` `😊` `🙂`

`🙃` `☺️` `😋` `😌` `😍` `🥰` `😘` `😗` `😙` `😚` `🥲` `🤪`

`😜` `😝` `😛` `🤑` `😎` `🤓` `🥸` `🧐` `🤠` `🥳` `🤗` `🤡`

`😏` `😶` `😐` `😑` `😒` `🙄` `🤨` `🤔` `🤫` `🤭` `🤥` `😳`

`😞` `😟` `😠` `😡` `🤬` `😔` `😕` `🙁` `☹️` `😬` `🥺` `😣`

`😖` `😫` `😩` `🥱` `😤` `😮‍💨` `😮` `😱` `😨` `😰` `😯` `😦`

`😧` `😢` `😥` `😪` `🤤` `😓` `😭` `🤩` `😵` `😵‍💫` `🥴` `😲`

`🤯` `🤐` `😷` `🤕` `🤒` `🤮` `🤢` `🤧` `🥵` `🥶` `😶‍🌫️` `😴`

`😺` `😸` `😹` `😻` `😼` `😽` `🙀` `😿` `😾` `🙈` `🙉` `🙊`

`👦` `👧` `👨` `👩` `👴` `👵` `👶` `👼` `👩‍⚕️` `👩‍🎓` `👩‍🏫` `👩‍⚖️`

`👩‍🌾` `👩‍🍳` `👩‍🔧` `👩‍🏭` `👩‍💼` `👩‍🔬` `👩‍💻` `👩‍🎤` `👩‍🎨` `👩‍✈️` `👩‍🚀` `👩‍🚒`

`👮` `🕵️` `💂` `👷` `👳` `👱` `🎅` `🤶` `👸` `🤴` `👰` `🤵`

`🤰` `👲` `🙍` `🙎` `🙅` `🙆` `💁` `🙋` `🙇` `🤦` `🤷` `💆`

`💇` `🚶` `🏃` `💃` `🕺` `👯` `🕴️` `🗣️` `👤` `👥` `👫` `👬`

`👭` `💏` `👨‍❤️‍💋‍👨` `👩‍❤️‍💋‍👩` `💑` `👨‍❤️‍👨` `👩‍❤️‍👩` `👪` `👨‍👩‍👧` `👨‍👩‍👧‍👦` `👨‍👩‍👦‍👦` `👨‍👩‍👧‍👧`

`👨‍👨‍👦` `👨‍👨‍👧` `👨‍👨‍👧‍👦` `👨‍👨‍👦‍👦` `👨‍👨‍👧‍👧` `👩‍👩‍👦` `👩‍👩‍👧` `👩‍👩‍👧‍👦` `👩‍👩‍👦‍👦` `👩‍👩‍👧‍👧` `👨‍👦` `👨‍👦‍👦`

`👨‍👧` `👨‍👧‍👦` `👨‍👧‍👧` `👩‍👦` `👩‍👦‍👦` `👩‍👧` `👩‍👧‍👦` `👩‍👧‍👧` `💪` `🤳` `👈` `👉`

`☝️` `👆` `🖕` `👇` `✌️` `🤞` `🖖` `🤘` `🤙` `🖐️` `✋` `👌`

`👍` `👎` `✊` `👊` `🤛` `🤜` `🤚` `👋` `👏` `✍️` `👐` `🙌`

`🙏` `🤝` `💅` `👂` `👃` `👣` `👀` `👁️` `👁️‍🗨️` `👅` `👄` `💋`

`💘` `❤️` `💓` `💔` `💕` `💖` `💗` `💙` `💚` `💛` `💜` `🖤`

`💝` `💞` `💟` `❣️` `💌` `💤` `💢` `💣` `💥` `💦` `💨` `💫`

`💬` `🗨️` `🗯️` `💭` `🕳️` `👓` `🕶️` `👔` `👕` `👖` `👗` `👘`

`👙` `👚` `👛` `👜` `👝` `🛍️` `🎒` `👞` `👟` `👠` `👡` `👢`

`👑` `👒` `🎩` `🎓` `⛑️` `📿` `💄` `💍` `💎`

長按對應（含原選項的重複位置）：

- `👦` → `👦` `👦🏻` `👦🏼` `👦🏽` `👦🏾` `👦🏿`
- `👧` → `👧` `👧🏻` `👧🏼` `👧🏽` `👧🏾` `👧🏿`
- `👨` → `👨` `👨🏻` `👨🏼` `👨🏽` `👨🏾` `👨🏿`
- `👩` → `👩` `👩🏻` `👩🏼` `👩🏽` `👩🏾` `👩🏿`
- `👴` → `👴` `👴🏻` `👴🏼` `👴🏽` `👴🏾` `👴🏿`
- `👵` → `👵` `👵🏻` `👵🏼` `👵🏽` `👵🏾` `👵🏿`
- `👶` → `👶` `👶🏻` `👶🏼` `👶🏽` `👶🏾` `👶🏿`
- `👼` → `👼` `👼🏻` `👼🏼` `👼🏽` `👼🏾` `👼🏿`
- `👩‍⚕️` → `👨‍⚕️` `👨🏻‍⚕️` `👨🏼‍⚕️` `👨🏽‍⚕️` `👨🏾‍⚕️` `👨🏿‍⚕️` `👩‍⚕️` `👩🏻‍⚕️` `👩🏼‍⚕️` `👩🏽‍⚕️` `👩🏾‍⚕️` `👩🏿‍⚕️`
- `👩‍🎓` → `👨‍🎓` `👨🏻‍🎓` `👨🏼‍🎓` `👨🏽‍🎓` `👨🏾‍🎓` `👨🏿‍🎓` `👩‍🎓` `👩🏻‍🎓` `👩🏼‍🎓` `👩🏽‍🎓` `👩🏾‍🎓` `👩🏿‍🎓`
- `👩‍🏫` → `👨‍🏫` `👨🏻‍🏫` `👨🏼‍🏫` `👨🏽‍🏫` `👨🏾‍🏫` `👨🏿‍🏫` `👩‍🏫` `👩🏻‍🏫` `👩🏼‍🏫` `👩🏽‍🏫` `👩🏾‍🏫` `👩🏿‍🏫`
- `👩‍⚖️` → `👨‍⚖️` `👨🏻‍⚖️` `👨🏼‍⚖️` `👨🏽‍⚖️` `👨🏾‍⚖️` `👨🏿‍⚖️` `👩‍⚖️` `👩🏻‍⚖️` `👩🏼‍⚖️` `👩🏽‍⚖️` `👩🏾‍⚖️` `👩🏿‍⚖️`
- `👩‍🌾` → `👨‍🌾` `👨🏻‍🌾` `👨🏼‍🌾` `👨🏽‍🌾` `👨🏾‍🌾` `👨🏿‍🌾` `👩‍🌾` `👩🏻‍🌾` `👩🏼‍🌾` `👩🏽‍🌾` `👩🏾‍🌾` `👩🏿‍🌾`
- `👩‍🍳` → `👨‍🍳` `👨🏻‍🍳` `👨🏼‍🍳` `👨🏽‍🍳` `👨🏾‍🍳` `👨🏿‍🍳` `👩‍🍳` `👩🏻‍🍳` `👩🏼‍🍳` `👩🏽‍🍳` `👩🏾‍🍳` `👩🏿‍🍳`
- `👩‍🔧` → `👨‍🔧` `👨🏻‍🔧` `👨🏼‍🔧` `👨🏽‍🔧` `👨🏾‍🔧` `👨🏿‍🔧` `👩‍🔧` `👩🏻‍🔧` `👩🏼‍🔧` `👩🏽‍🔧` `👩🏾‍🔧` `👩🏿‍🔧`
- `👩‍🏭` → `👨‍🏭` `👨🏻‍🏭` `👨🏼‍🏭` `👨🏽‍🏭` `👨🏾‍🏭` `👨🏿‍🏭` `👩‍🏭` `👩🏻‍🏭` `👩🏼‍🏭` `👩🏽‍🏭` `👩🏾‍🏭` `👩🏿‍🏭`
- `👩‍💼` → `👨‍💼` `👨🏻‍💼` `👨🏼‍💼` `👨🏽‍💼` `👨🏾‍💼` `👨🏿‍💼` `👩‍💼` `👩🏻‍💼` `👩🏼‍💼` `👩🏽‍💼` `👩🏾‍💼` `👩🏿‍💼`
- `👩‍🔬` → `👨‍🔬` `👨🏻‍🔬` `👨🏼‍🔬` `👨🏽‍🔬` `👨🏾‍🔬` `👨🏿‍🔬` `👩‍🔬` `👩🏻‍🔬` `👩🏼‍🔬` `👩🏽‍🔬` `👩🏾‍🔬` `👩🏿‍🔬`
- `👩‍💻` → `👨‍💻` `👨🏻‍💻` `👨🏼‍💻` `👨🏽‍💻` `👨🏾‍💻` `👨🏿‍💻` `👩‍💻` `👩🏻‍💻` `👩🏼‍💻` `👩🏽‍💻` `👩🏾‍💻` `👩🏿‍💻`
- `👩‍🎤` → `👨‍🎤` `👨🏻‍🎤` `👨🏼‍🎤` `👨🏽‍🎤` `👨🏾‍🎤` `👨🏿‍🎤` `👩‍🎤` `👩🏻‍🎤` `👩🏼‍🎤` `👩🏽‍🎤` `👩🏾‍🎤` `👩🏿‍🎤`
- `👩‍🎨` → `👨‍🎨` `👨🏻‍🎨` `👨🏼‍🎨` `👨🏽‍🎨` `👨🏾‍🎨` `👨🏿‍🎨` `👩‍🎨` `👩🏻‍🎨` `👩🏼‍🎨` `👩🏽‍🎨` `👩🏾‍🎨` `👩🏿‍🎨`
- `👩‍✈️` → `👨‍✈️` `👨🏻‍✈️` `👨🏼‍✈️` `👨🏽‍✈️` `👨🏾‍✈️` `👨🏿‍✈️` `👩‍✈️` `👩🏻‍✈️` `👩🏼‍✈️` `👩🏽‍✈️` `👩🏾‍✈️` `👩🏿‍✈️`
- `👩‍🚀` → `👨‍🚀` `👨🏻‍🚀` `👨🏼‍🚀` `👨🏽‍🚀` `👨🏾‍🚀` `👨🏿‍🚀` `👩‍🚀` `👩🏻‍🚀` `👩🏼‍🚀` `👩🏽‍🚀` `👩🏾‍🚀` `👩🏿‍🚀`
- `👩‍🚒` → `👨‍🚒` `👨🏻‍🚒` `👨🏼‍🚒` `👨🏽‍🚒` `👨🏾‍🚒` `👨🏿‍🚒` `👩‍🚒` `👩🏻‍🚒` `👩🏼‍🚒` `👩🏽‍🚒` `👩🏾‍🚒` `👩🏿‍🚒`
- `👮` → `👮‍♂️` `👮🏻‍♂️` `👮🏼‍♂️` `👮🏽‍♂️` `👮🏾‍♂️` `👮🏿‍♂️` `👮‍♀️` `👮🏻‍♀️` `👮🏼‍♀️` `👮🏽‍♀️` `👮🏾‍♀️` `👮🏿‍♀️`
- `🕵️` → `🕵️‍♂️` `🕵🏻‍♂️` `🕵🏼‍♂️` `🕵🏽‍♂️` `🕵🏾‍♂️` `🕵🏿‍♂️` `🕵️‍♀️` `🕵🏻‍♀️` `🕵🏼‍♀️` `🕵🏽‍♀️` `🕵🏾‍♀️` `🕵🏿‍♀️`
- `💂` → `💂‍♂️` `💂🏻‍♂️` `💂🏼‍♂️` `💂🏽‍♂️` `💂🏾‍♂️` `💂🏿‍♂️` `💂‍♀️` `💂🏻‍♀️` `💂🏼‍♀️` `💂🏽‍♀️` `💂🏾‍♀️` `💂🏿‍♀️`
- `👷` → `👷‍♂️` `👷🏻‍♂️` `👷🏼‍♂️` `👷🏽‍♂️` `👷🏾‍♂️` `👷🏿‍♂️` `👷‍♀️` `👷🏻‍♀️` `👷🏼‍♀️` `👷🏽‍♀️` `👷🏾‍♀️` `👷🏿‍♀️`
- `👳` → `👳‍♂️` `👳🏻‍♂️` `👳🏼‍♂️` `👳🏽‍♂️` `👳🏾‍♂️` `👳🏿‍♂️` `👳‍♀️` `👳🏻‍♀️` `👳🏼‍♀️` `👳🏽‍♀️` `👳🏾‍♀️` `👳🏿‍♀️`
- `👱` → `👱‍♂️` `👱🏻‍♂️` `👱🏼‍♂️` `👱🏽‍♂️` `👱🏾‍♂️` `👱🏿‍♂️` `👱‍♀️` `👱🏻‍♀️` `👱🏼‍♀️` `👱🏽‍♀️` `👱🏾‍♀️` `👱🏿‍♀️`
- `🎅` → `🎅` `🎅🏻` `🎅🏼` `🎅🏽` `🎅🏾` `🎅🏿`
- `🤶` → `🤶` `🤶🏻` `🤶🏼` `🤶🏽` `🤶🏾` `🤶🏿`
- `👸` → `👸` `👸🏻` `👸🏼` `👸🏽` `👸🏾` `👸🏿`
- `🤴` → `🤴` `🤴🏻` `🤴🏼` `🤴🏽` `🤴🏾` `🤴🏿`
- `👰` → `👰` `👰🏻` `👰🏼` `👰🏽` `👰🏾` `👰🏿`
- `🤵` → `🤵` `🤵🏻` `🤵🏼` `🤵🏽` `🤵🏾` `🤵🏿`
- `🤰` → `🤰` `🤰🏻` `🤰🏼` `🤰🏽` `🤰🏾` `🤰🏿`
- `👲` → `👲` `👲🏻` `👲🏼` `👲🏽` `👲🏾` `👲🏿`
- `🙍` → `🙍‍♂️` `🙍🏻‍♂️` `🙍🏼‍♂️` `🙍🏽‍♂️` `🙍🏾‍♂️` `🙍🏿‍♂️` `🙍‍♀️` `🙍🏻‍♀️` `🙍🏼‍♀️` `🙍🏽‍♀️` `🙍🏾‍♀️` `🙍🏿‍♀️`
- `🙎` → `🙎‍♂️` `🙎🏻‍♂️` `🙎🏼‍♂️` `🙎🏽‍♂️` `🙎🏾‍♂️` `🙎🏿‍♂️` `🙎‍♀️` `🙎🏻‍♀️` `🙎🏼‍♀️` `🙎🏽‍♀️` `🙎🏾‍♀️` `🙎🏿‍♀️`
- `🙅` → `🙅‍♂️` `🙅🏻‍♂️` `🙅🏼‍♂️` `🙅🏽‍♂️` `🙅🏾‍♂️` `🙅🏿‍♂️` `🙅‍♀️` `🙅🏻‍♀️` `🙅🏼‍♀️` `🙅🏽‍♀️` `🙅🏾‍♀️` `🙅🏿‍♀️`
- `🙆` → `🙆‍♂️` `🙆🏻‍♂️` `🙆🏼‍♂️` `🙆🏽‍♂️` `🙆🏾‍♂️` `🙆🏿‍♂️` `🙆‍♀️` `🙆🏻‍♀️` `🙆🏼‍♀️` `🙆🏽‍♀️` `🙆🏾‍♀️` `🙆🏿‍♀️`
- `💁` → `💁‍♂️` `💁🏻‍♂️` `💁🏼‍♂️` `💁🏽‍♂️` `💁🏾‍♂️` `💁🏿‍♂️` `💁‍♀️` `💁🏻‍♀️` `💁🏼‍♀️` `💁🏽‍♀️` `💁🏾‍♀️` `💁🏿‍♀️`
- `🙋` → `🙋‍♂️` `🙋🏻‍♂️` `🙋🏼‍♂️` `🙋🏽‍♂️` `🙋🏾‍♂️` `🙋🏿‍♂️` `🙋‍♀️` `🙋🏻‍♀️` `🙋🏼‍♀️` `🙋🏽‍♀️` `🙋🏾‍♀️` `🙋🏿‍♀️`
- `🙇` → `🙇‍♂️` `🙇🏻‍♂️` `🙇🏼‍♂️` `🙇🏽‍♂️` `🙇🏾‍♂️` `🙇🏿‍♂️` `🙇‍♀️` `🙇🏻‍♀️` `🙇🏼‍♀️` `🙇🏽‍♀️` `🙇🏾‍♀️` `🙇🏿‍♀️`
- `🤦` → `🤦‍♂️` `🤦🏻‍♂️` `🤦🏼‍♂️` `🤦🏽‍♂️` `🤦🏾‍♂️` `🤦🏿‍♂️` `🤦‍♀️` `🤦🏻‍♀️` `🤦🏼‍♀️` `🤦🏽‍♀️` `🤦🏾‍♀️` `🤦🏿‍♀️`
- `🤷` → `🤷‍♂️` `🤷🏻‍♂️` `🤷🏼‍♂️` `🤷🏽‍♂️` `🤷🏾‍♂️` `🤷🏿‍♂️` `🤷‍♀️` `🤷🏻‍♀️` `🤷🏼‍♀️` `🤷🏽‍♀️` `🤷🏾‍♀️` `🤷🏿‍♀️`
- `💆` → `💆‍♂️` `💆🏻‍♂️` `💆🏼‍♂️` `💆🏽‍♂️` `💆🏾‍♂️` `💆🏿‍♂️` `💆‍♀️` `💆🏻‍♀️` `💆🏼‍♀️` `💆🏽‍♀️` `💆🏾‍♀️` `💆🏿‍♀️`
- `💇` → `💇‍♂️` `💇🏻‍♂️` `💇🏼‍♂️` `💇🏽‍♂️` `💇🏾‍♂️` `💇🏿‍♂️` `💇‍♀️` `💇🏻‍♀️` `💇🏼‍♀️` `💇🏽‍♀️` `💇🏾‍♀️` `💇🏿‍♀️`
- `🚶` → `🚶‍♂️` `🚶🏻‍♂️` `🚶🏼‍♂️` `🚶🏽‍♂️` `🚶🏾‍♂️` `🚶🏿‍♂️` `🚶‍♀️` `🚶🏻‍♀️` `🚶🏼‍♀️` `🚶🏽‍♀️` `🚶🏾‍♀️` `🚶🏿‍♀️`
- `🏃` → `🏃‍♂️` `🏃🏻‍♂️` `🏃🏼‍♂️` `🏃🏽‍♂️` `🏃🏾‍♂️` `🏃🏿‍♂️` `🏃‍♀️` `🏃🏻‍♀️` `🏃🏼‍♀️` `🏃🏽‍♀️` `🏃🏾‍♀️` `🏃🏿‍♀️`
- `💃` → `💃` `💃🏻` `💃🏼` `💃🏽` `💃🏾` `💃🏿`
- `🕺` → `🕺` `🕺🏻` `🕺🏼` `🕺🏽` `🕺🏾` `🕺🏿`
- `👯` → `👯‍♂️` `👯‍♀️`
- `💪` → `💪` `💪🏻` `💪🏼` `💪🏽` `💪🏾` `💪🏿`
- `🤳` → `🤳` `🤳🏻` `🤳🏼` `🤳🏽` `🤳🏾` `🤳🏿`
- `👈` → `👈` `👈🏻` `👈🏼` `👈🏽` `👈🏾` `👈🏿`
- `👉` → `👉` `👉🏻` `👉🏼` `👉🏽` `👉🏾` `👉🏿`
- `☝️` → `☝️` `☝🏻` `☝🏼` `☝🏽` `☝🏾` `☝🏿`
- `👆` → `👆` `👆🏻` `👆🏼` `👆🏽` `👆🏾` `👆🏿`
- `🖕` → `🖕` `🖕🏻` `🖕🏼` `🖕🏽` `🖕🏾` `🖕🏿`
- `👇` → `👇` `👇🏻` `👇🏼` `👇🏽` `👇🏾` `👇🏿`
- `✌️` → `✌️` `✌🏻` `✌🏼` `✌🏽` `✌🏾` `✌🏿`
- `🤞` → `🤞` `🤞🏻` `🤞🏼` `🤞🏽` `🤞🏾` `🤞🏿`
- `🖖` → `🖖` `🖖🏻` `🖖🏼` `🖖🏽` `🖖🏾` `🖖🏿`
- `🤘` → `🤘` `🤘🏻` `🤘🏼` `🤘🏽` `🤘🏾` `🤘🏿`
- `🤙` → `🤙` `🤙🏻` `🤙🏼` `🤙🏽` `🤙🏾` `🤙🏿`
- `🖐️` → `🖐️` `🖐🏻` `🖐🏼` `🖐🏽` `🖐🏾` `🖐🏿`
- `✋` → `✋` `✋🏻` `✋🏼` `✋🏽` `✋🏾` `✋🏿`
- `👌` → `👌` `👌🏻` `👌🏼` `👌🏽` `👌🏾` `👌🏿`
- `👍` → `👍` `👍🏻` `👍🏼` `👍🏽` `👍🏾` `👍🏿`
- `👎` → `👎` `👎🏻` `👎🏼` `👎🏽` `👎🏾` `👎🏿`
- `✊` → `✊` `✊🏻` `✊🏼` `✊🏽` `✊🏾` `✊🏿`
- `👊` → `👊` `👊🏻` `👊🏼` `👊🏽` `👊🏾` `👊🏿`
- `🤛` → `🤛` `🤛🏻` `🤛🏼` `🤛🏽` `🤛🏾` `🤛🏿`
- `🤜` → `🤜` `🤜🏻` `🤜🏼` `🤜🏽` `🤜🏾` `🤜🏿`
- `🤚` → `🤚` `🤚🏻` `🤚🏼` `🤚🏽` `🤚🏾` `🤚🏿`
- `👋` → `👋` `👋🏻` `👋🏼` `👋🏽` `👋🏾` `👋🏿`
- `👏` → `👏` `👏🏻` `👏🏼` `👏🏽` `👏🏾` `👏🏿`
- `✍️` → `✍️` `✍🏻` `✍🏼` `✍🏽` `✍🏾` `✍🏿`
- `👐` → `👐` `👐🏻` `👐🏼` `👐🏽` `👐🏾` `👐🏿`
- `🙌` → `🙌` `🙌🏻` `🙌🏼` `🙌🏽` `🙌🏾` `🙌🏿`
- `🙏` → `🙏` `🙏🏻` `🙏🏼` `🙏🏽` `🙏🏾` `🙏🏿`
- `🤝` → `🤝` `🤝🏻` `🤝🏼` `🤝🏽` `🤝🏾` `🤝🏿`
- `💅` → `💅` `💅🏻` `💅🏼` `💅🏽` `💅🏾` `💅🏿`
- `👂` → `👂` `👂🏻` `👂🏼` `👂🏽` `👂🏾` `👂🏿`
- `👃` → `👃` `👃🏻` `👃🏼` `👃🏽` `👃🏾` `👃🏿`

</details>

<details>
<summary>softkeys_input_emoji_category_symbols / xml-v25：205 項，長按 0 個位置</summary>

來源：[softkeys_input_emoji_category_symbols.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml-v25/softkeys_input_emoji_category_symbols.xml)；SHA-256：`d0a42e7637e06f4905f879a61cf71c9792a07652c5bec21e6ed56ab1d3982f7e`。

`🏧` `🚮` `🚰` `♿` `🚹` `🚺` `🚻` `🚼` `🚾` `🛂` `🛃` `🛄`

`🛅` `⚠️` `🚸` `⛔` `🚫` `🚳` `🚭` `🚯` `🚱` `🚷` `📵` `🔞`

`☢️` `☣️` `⬆️` `↗️` `➡️` `↘️` `⬇️` `↙️` `⬅️` `↖️` `↕️` `↔️`

`↩️` `↪️` `⤴️` `⤵️` `🔃` `🔄` `🔙` `🔚` `🔛` `🔜` `🔝` `🛐`

`⚛️` `🕉️` `✡️` `☸️` `☯️` `✝️` `☦️` `☪️` `☮️` `🕎` `🔯` `♻️`

`📛` `⚜️` `🔰` `🔱` `⭕` `✅` `☑️` `✔️` `✖️` `❌` `❎` `➕`

`♀️` `♂️` `⚕️` `➖` `➗` `➰` `➿` `〽️` `✳️` `✴️` `❇️` `‼️`

`⁉️` `❓` `❔` `❕` `❗` `〰️` `©️` `®️` `™️` `♈` `♉` `♊`

`♋` `♌` `♍` `♎` `♏` `♐` `♑` `♒` `♓` `⛎` `🔀` `🔁`

`🔂` `▶️` `⏩` `⏭️` `⏯️` `◀️` `⏪` `⏮️` `🔼` `⏫` `🔽` `⏬`

`⏸️` `⏹️` `⏺️` `⏏️` `🎦` `🔅` `🔆` `📶` `📳` `📴` `#️⃣` `*️⃣`

`0️⃣` `1️⃣` `2️⃣` `3️⃣` `4️⃣` `5️⃣` `6️⃣` `7️⃣` `8️⃣` `9️⃣` `🔟` `💯`

`🔠` `🔡` `🔢` `🔣` `🔤` `🅰️` `🆎` `🅱️` `🆑` `🆒` `🆓` `ℹ️`

`🆔` `Ⓜ️` `🆕` `🆖` `🅾️` `🆗` `🅿️` `🆘` `🆙` `🆚` `🈁` `🈂️`

`🈷️` `🈶` `🈯` `🉐` `🈹` `🈚` `🈲` `🉑` `🈸` `🈴` `🈳` `㊗️`

`㊙️` `🈺` `🈵` `▪️` `▫️` `◻️` `◼️` `◽` `◾` `⬛` `⬜` `🔶`

`🔷` `🔸` `🔹` `🔺` `🔻` `💠` `🔘` `🔲` `🔳` `⚪` `⚫` `🔴`

`🔵`

</details>

<details>
<summary>softkeys_input_emoji_category_travel_places / xml-v25：210 項，長按 6 個位置</summary>

來源：[softkeys_input_emoji_category_travel_places.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml-v25/softkeys_input_emoji_category_travel_places.xml)；SHA-256：`22d0740d6d113fda66ede572c0b1b06a9c2dfa1e5a96c23feb239debee83ea92`。

`🌍` `🌎` `🌏` `🌐` `🗺️` `🗾` `🏔️` `⛰️` `🌋` `🗻` `🏕️` `🏖️`

`🏜️` `🏝️` `🏞️` `🏟️` `🏛️` `🏗️` `🏘️` `🏙️` `🏚️` `🏠` `🏡` `🏢`

`🏣` `🏤` `🏥` `🏦` `🏨` `🏩` `🏪` `🏫` `🏬` `🏭` `🏯` `🏰`

`💒` `🗼` `🗽` `⛪` `🕌` `🕍` `⛩️` `🕋` `⛲` `⛺` `🌁` `🌃`

`🌄` `🌅` `🌆` `🌇` `🌉` `♨️` `🌌` `🎠` `🎡` `🎢` `💈` `🎪`

`🎭` `🖼️` `🎨` `🎰` `🚂` `🚃` `🚄` `🚅` `🚆` `🚇` `🚈` `🚉`

`🚊` `🚝` `🚞` `🚋` `🚌` `🚍` `🚎` `🚐` `🚑` `🚒` `🚓` `🚔`

`🚕` `🚖` `🚗` `🚘` `🚙` `🚚` `🚛` `🚜` `🚲` `🛴` `🛵` `🏎️`

`🏍️` `🚏` `🛣️` `🛤️` `⛽` `🚨` `🚥` `🚦` `🚧` `🛑` `⚓` `⛵`

`🛶` `🚤` `🛳️` `⛴️` `🛥️` `🚢` `✈️` `🛩️` `🛫` `🛬` `💺` `🚁`

`🚟` `🚠` `🚡` `🚀` `🛰️` `🛎️` `🚪` `🛌` `🛏️` `🛋️` `🚽` `🚿`

`🛀` `🛁` `⌛` `⏳` `⌚` `⏰` `⏱️` `⏲️` `🕰️` `🕛` `🕧` `🕐`

`🕜` `🕑` `🕝` `🕒` `🕞` `🕓` `🕟` `🕔` `🕠` `🕕` `🕡` `🕖`

`🕢` `🕗` `🕣` `🕘` `🕤` `🕙` `🕥` `🕚` `🕦` `🌑` `🌒` `🌓`

`🌔` `🌕` `🌖` `🌗` `🌘` `🌙` `🌚` `🌛` `🌜` `🌡️` `☀️` `🌝`

`🌞` `⭐` `🌟` `🌠` `☁️` `⛅` `⛈️` `🌤️` `🌥️` `🌦️` `🌧️` `🌨️`

`🌩️` `🌪️` `🌫️` `🌬️` `🌀` `🌈` `🌂` `☂️` `☔` `⛱️` `⚡` `❄️`

`☃️` `⛄` `☄️` `🔥` `💧` `🌊`

長按對應（含原選項的重複位置）：

- `🛀` → `🛀` `🛀🏻` `🛀🏼` `🛀🏽` `🛀🏾` `🛀🏿`

</details>

#### 顏文字：完整資料清單

<details>
<summary>softkeys_input_emoticon_category_displeasure / base：17 項，長按 0 個位置</summary>

來源：[softkeys_input_emoticon_category_displeasure.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_emoticon_category_displeasure.xml)；SHA-256：`e3915e83bd1b2e9f2a74515b5de0b1a950179b4061ee4c88ecf2a90dd5febb64`。

<code>(´・ω・`)</code> `(´・ω・｀)` `(´；ω；｀)` `( ˘•ω•˘ )` `_(._.)_` `m(_ _)m` `(*´･д･)?` `( •́ _ •̀)？` <code>(´⊙ω⊙`)</code> `(ﾟдﾟ)` `(☉д⊙)` `Σ(*ﾟдﾟﾉ)ﾉ`

<code>(´///☁///`)</code> <code>(//´/◒/`//)</code> `⁄(⁄ ⁄•⁄ω⁄•⁄ ⁄)⁄` `(๑•́ ₃ •̀๑)` `(,,Ծ 3 Ծ,,)`

</details>

<details>
<summary>softkeys_input_emoticon_category_sadness / base：10 項，長按 0 個位置</summary>

來源：[softkeys_input_emoticon_category_sadness.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_emoticon_category_sadness.xml)；SHA-256：`d1068d69acc0c9df0f9073644e10e3cca26e52b796b958c1f338455a4c8d94af`。

`(¯―¯٥)` `(_ _;)` `(=o=;)` `(-_-;)` `(~_~;)` `(=o=;)` `(-_-;)` `(@@;)` `(@_@)` `(＠_＠;)`

</details>

<details>
<summary>softkeys_input_emoticon_category_smile / base：40 項，長按 0 個位置</summary>

來源：[softkeys_input_emoticon_category_smile.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_emoticon_category_smile.xml)；SHA-256：`d2bfad808087cc2add0ec332b59ca2f418036efabcf0bd6de481e60100232646`。

`(・‿・)` `(・ω・)` `(・∀・)` `(^‿^)` `(^∇^)` `(^ω^)` `(>‿<)` `(>w<)` `(>∇<)` `(>ω<)` `(≧∇≦)` `(≧ω≦)`

<code>(´∇`)</code> <code>(´∀`)</code> <code>(´ω`)</code> `(─‿─)` `(¬‿¬)` `(∩_∩)` `(,,・ω・,,)` <code>(๑´ㅂ`๑)</code> <code>(๑´ڡ`๑)</code> `(๑• . •๑)` `(๑╹◡╹๑)` `(ゝ∀･)`

`ʘ‿ʘ` `◉‿◉` `ɷ◡ɷ` `≖‿≖` `\^o^/` `o(^∇^)o` <code>d(`･∀･)b</code> `(ゝ∀･)b` `٩(^ᴗ^)۶` `٩(●ᴗ●)۶` `٩(๑❛ᴗ❛๑)۶` <code>(´∇`)ﾉ</code>

`(ﾟ∇ﾟ)ﾉ` `(≧∇≦)ﾉ` `(ﾉ>ω<)ﾉ` `(´,,•ω•,,)♡`

</details>

<details>
<summary>softkeys_input_emoticon_category_surprise / base：15 項，長按 0 個位置</summary>

來源：[softkeys_input_emoticon_category_surprise.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_emoticon_category_surprise.xml)；SHA-256：`5b0a68b270c15fa0764524389cbea04fe1675ea958cb4160f1ce45e27173c8aa`。

`(TдT)` `(TOT)` `(｡ŏ﹏ŏ)` <code>(´;ω;`)</code> `(╥﹏╥)` <code>( ´•̥̥̥ω•̥̥̥` )</code> <code>(;´༎ຶД༎ຶ`)</code> `(〒︿〒)` `( ´థ౪థ）σ` `ಥ_ಥ` `இдஇ` <code>｡ﾟヽ(ﾟ´Д`)ﾉﾟ｡</code>

<code>｡ﾟ(ﾟ´ω`ﾟ)ﾟ｡</code> `╰(〒皿〒)╯` `◢▆▅▄▃崩╰(〒皿〒)╯潰▃▄▅▇◣`

</details>

<details>
<summary>softkeys_input_emoticon_category_sweat / base：8 項，長按 0 個位置</summary>

來源：[softkeys_input_emoticon_category_sweat.xml](https://github.com/wdzeng/google-fatty-zhuin-ime/blob/af24a6faa849da013b01a6b0debae52948d19424/src/res/xml/softkeys_input_emoticon_category_sweat.xml)；SHA-256：`3479139be539d329ab875f551dfb3d0e383936e6e18d56ed2dfc5241221aadc0`。

<code>(#`Д´)</code> <code>(#`Д´)ﾉ</code> <code>(#`皿´)</code> <code>(-`ェ´-╬)</code> <code>(`д´)</code> <code>ヽ(#`Д´)ﾉ</code> <code>ヽ(`Д´)ノ</code> `(ಠ益ಠ)`

</details>

### 這份完整資料的界線

本輪已保存 repo 可讀 XML 的全部符號／Emoji／顏文字目錄，含 Unicode range、長按與版本差異；沒有解析二進位／原生資料、取得使用者最近使用、逐個實際送出字符，或宣稱這是 Google 官方最新表。抽取時的 60 個候選檔案無解析錯誤、無未解決 include、無異常 U+ token；文件頁數是來源版型推算。後續可以直接以本機 JSON 實作，再針對新 UI 的顯示、長按、翻頁與字型支援做實測。
