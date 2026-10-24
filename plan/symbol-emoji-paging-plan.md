# 完整符號與 Emoji 翻頁

## 需求與完成證據

把 `doc/google-fatty-zhuyin-catalog.json` 已保存的字符資料加入輸入法，支援分類、平滑水平翻頁及長按變體。保留既有字母梯狀排列、Shift／英文鎖定、數字列設定與工具列；符號頁不增加收起或切換輸入法鍵。以編譯、lint、資料逐項比對和 OPPO 實測完成，盡量不新增 unit test。

## 決策與範圍

- 採 TW 符號六類 532 個位置，另加補充類 10 項，合計七類 542 個位置；Emoji 八類依 API 23–24／25+ 選 base 1,357／新版 1,404 個主選項，保留 504／880 個長按位置；另加入五類共 90 個顏文字。保留原分類順序、重複、內部空白、VS16／ZWJ／膚色／旗幟完整序列。字型支援取決於系統，不載入額外字型。
- 外部來源只提供整理過的 Unicode 字符資料；頁面、手勢、載入與整合自行實作，不引入反編譯程式碼、APK 或圖片。
- 開發用腳本將研究 JSON 轉成每頁 UTF-8 文字檔與小索引；App 不解析或打包 4.28 MB 研究 JSON。不掃描全部 Emoji 或建立全部頁面。
- 相容性補充：還原 Android XML attribute 的跳脫字元（例如 `\@` → `@`），不把資源語法當作輸出。保留 base／TW 的全部手機字符集合，以「補充」類加入 9 個 source-only 字符及原有 `´`；TW 原本未提供的 9 組長按關聯採 base 補齊，最終符號共 87 個長按位置。
- 使用現有相依樹中的 AndroidX ViewPager／PagerAdapter，明確宣告版本 1.0.0。完整頁寬、鄰頁保留值 1；分類跳轉直接定位，避免橫跨數十頁的動畫。只保留附著頁的資料；離開、隱藏或記憶體壓力時釋放。三頁是穩定狀態目標，並非 ViewPager 在跳轉期間的硬上限。
- 分類頁上方提供「符號／Emoji／顏文字」切換及分類選取，顯示分類內頁碼；滑動可跨相鄰分類，首尾不循環。符號每頁 28、Emoji 每頁 20、顏文字每頁 11 個位置，末格保留 Delete。顏文字比來源的 12 格少一格，讓刪除與其他頁一致；所有 90 項仍完整提供。
- 底列維持 ABC／逗號／空白／句點／Enter；Enter 跟隨欄位動作。頁面點選使用整段字串，既有未送出的字根仍按原行為累積；英文鎖定或沒有字根時直接上屏。
- 不增加最近使用紀錄、下載資源、搜尋、設定項或整套第三方鍵盤。

## 實作任務

1. 轉換腳本、精簡 assets 與按頁載入介面；逐項核對原始 catalog、API 分流、總數與長按字串。
2. 分類與頁碼、平滑 pager、格子與長按選單；處理 swipe cancel、Delete repeat、無障礙及關閉釋放。
3. IME 模式／輸入路由／底列與生命週期整合，刪除不再使用的三份靜態鍵盤 XML 與快取。
4. 編譯、lint、實機檢查分類邊界、滑動不誤輸入、Unicode／長按／刪除、ABC 往返與隱藏重開。比較初始化後、使用與重複翻頁後的記憶體；保留原有內容與設定。

## 驗證結果

- `python3 scripts/generate_symbol_catalog.py --check` 通過：28 個索引記錄、176 頁，含兩個 API Emoji 替代版本；共 39,800 bytes，索引 2,153 bytes，最大頁 3,775 bytes。逐項核對 base／TW 手機字符集合、順序、變體及原有 56 符號可達性。5 個 literal 位置依 AAPT2 實際 compile／link／xmltree 結果解碼。
- `:app:assembleDebug :app:lintDebug` 通過。未新增 unit test；正式 release build 未另跑。
- 實機固定 APK：`9fad1bd6c18db7c86af6c491c932d71633cb1b4996e2fb983d07f5719816b0a8`（debug 1.2.11／17）。OPPO CPH1941，Android 11／API 30、zh-TW、720×1600／density 320，高度 116%、數字列關閉；保留原設定。
- 實際點選全部 7／8／5 類；`@#🏳️‍🌈🇦🇨👮‍♂️👮🏾‍♀️` 讀回正確，含 VS16／ZWJ／區域旗幟／膚色與性別。12 格選單滑選、取消不輸入、分類邊界及反向滑動、長按 Delete 放開停止、顏文字內部空白、ABC 返回、底列換行與隱藏重開皆經實機檢查。待送原碼加上組合 Emoji 後，一次 Delete 回到原字母亦已觀察。

### 記憶體

同一 PID 23617，單位為 `dumpsys meminfo` 的整個測試程序 PSS（包含 TestPanel、輸入法、字典及繪字資源），不是單獨 catalog heap。沒有強制 GC；先完成各類別、Emoji 與長按暖機，再於相同 Emoji 頁狀態比較三輪。

| 狀態 | PSS（KiB） | PSS（MiB） |
| :--- | ---: | ---: |
| 字母頁基準 | 89,460 | 87.4 |
| 首次符號頁 | 89,220 | 87.1 |
| 完整功能暖機後（cycle 0） | 119,985 | 117.2 |
| 重複第 1 輪 | 121,442 | 118.6 |
| 重複第 2 輪 | 120,243 | 117.4 |
| 重複第 3 輪 | 120,430 | 117.6 |
| 返回字母頁 | 114,950 | 112.3 |
| 隱藏鍵盤 | 112,684 | 110.0 |

三輪在約 117–119 MiB 間波動，cycle 3 比 cycle 0 多 445 KiB，沒有觀察到逐輪累積的訊號。首次符號頁約與字母頁基準相當；完整 Emoji／顏文字／彈窗使用後的整體程序高於冷基準，不能把約 39 KB 的資產大小當作整體 RAM。這是單裝置、短期重複操作的觀察，沒有進行長時間洩漏測試。

### 繪製與範圍

原生 ViewPager 的連續拖動、換頁、回彈與取消已操作確認。一次 `gfxinfo` 混合操作收集：程序 3,145 frames／160 janky（5.09%），p95 16ms；InputMethod 視窗 1,221 frames／114 janky（9.34%），p95 21ms、p99 57ms。數值包含首次字型／分類、選單、彈窗等操作，不是隔離的翻頁效能測試，也不作為穩定 60fps 保證。

未實測 API 23–24、橫向或其他數字列／高度組合；兩版資料已靜態驗證，現有字母排列未改動。原測試文字、游標與偏好已依新鮮基準還原；剪貼簿本輪未改動。實機報告保存在忽略的 `test-runs/catalog-paging/attempt-001/`。

獨立測試最終結果 **PASS**；健康紀錄完整且 `NO_ISSUE_OBSERVED`，cleanup `COMPLETE`、capture `STOPPED`、lease `released`、contract valid。實機固定 APK 與最終讀回 SHA-256 一致；測試已結束，不再持有手機。


## 實作參考

- [資料來源與翻頁研究](../doc/google-zhuyin-symbols.md)
- [AndroidX ViewPager](https://developer.android.com/reference/androidx/viewpager/widget/ViewPager#setOffscreenPageLimit(int))：保留鄰頁的行為及低保留值。
- [PagerAdapter](https://developer.android.com/reference/androidx/viewpager/widget/PagerAdapter)：建立與移除頁面的責任。
- [Android 記憶體管理](https://developer.android.com/topic/performance/memory-management)：生命週期與釋放資源。
