# FreeShiamy（Android IME）

這是一個**碼表驅動的字根輸入法**鍵盤（Android `InputMethodService`）。你輸入「字根碼／字碼」後，候選字會顯示在鍵盤上方，並可用空白鍵與數字鍵快速選字。

## 資料來源
- **字根碼表**：由熱心網友手動整理／輸入後提供（收錄於 `app/src/main/assets/freeshiamy.cin`）。
- **注音對照表**：來源為教育部公開資料（收錄於 `app/src/main/assets/cht_spells.cin`），用於「同音字」反查功能。

> 本專案為社群開源實作，**不隸屬任何商標／商用輸入法產品**。若你是資料原作者並希望補充標註或移除，請開 Issue 討論。

## 安裝與使用
1. 從 GitHub **Releases** 下載 APK 並安裝（或從 **Actions** 的 artifact 下載）。
2. 到 Android 系統設定啟用此輸入法（語言與輸入法／螢幕鍵盤／管理鍵盤），並在任一輸入框切換鍵盤使用。
3. App 內提供 **Test Panel**，可用來快速測試鍵盤行為與候選選字。

### 基本操作（精簡）
- 輸入字根碼後：候選列會依相關度顯示；點選候選即可送出並進入下一輪。
- **空白鍵／數字鍵**：在「有符合候選」時用於快速選字；在「沒有任何累積字元」時，空白鍵會直接送出空白。
- **符號鍵盤（123/Shift）上的數字與符號**：一律直接送出（不進暫存欄 / raw buffer）。
- **刪除鍵**：若按下時上方暫存欄有內容，長按只會刪到暫存欄清空為止；需放開再按才會刪輸入框內既有文字。
- **提示字根**（設定預設開）：上屏後會在候選列右側顯示該字的最短字碼（含反查選字）。
- **反查同音字**：用單引號前綴（`'`）進入反查；用刪除鍵可取消反查並回到一般狀態（詳細規格見 `freeshiamy.md`）。
- **密碼/隱私欄位**（設定預設開）：遇到密碼欄位會自動切換到其他輸入法（或顯示輸入法選擇器）；可另外設定是否把「不允許個人化學習」也視為隱私欄位。
- **交換刪除鍵與「'」鍵**（設定可開）：把字母區右側的刪除鍵與單引號鍵互換位置。
- **Caps Lock**：鎖定時 raw buffer 會以大寫顯示/送出，但候選查詢不分大小寫。

## 釋出與下載（GitHub Actions）
本專案使用 GitHub Actions 自動編譯 **release APK**：
- **觸發方式**：推送 Git tag（符合 `v*`，例如 `v1.0.0`）或手動 `workflow_dispatch`。
- **如何打 tag**
  ```bash
  git tag -a v1.0.0 -m "v1.0.0"
  git push origin v1.0.0
  ```
- **下載位置**
  - GitHub **Releases**：會附加 `freeshiamy-<tag>.apk`
  - GitHub **Actions**：對應的 workflow run 內可下載 artifact

## 簽章（重要說明）
為了讓 CI 能直接產出可安裝的 APK，repo 內包含一份 **公開的 release keystore**（`keystore/freeshiamy-release.jks`）。這個簽章**不應被視為信任來源**（任何人都能使用相同 keystore 產出相同簽章的 APK）。

如果你在意可驗證的信任鏈，請自行從原始碼編譯並改用你信任的簽章：
- 產生 keystore（示例）：`keytool -genkeypair ...`
- 在本機修改 `keystore/release.properties` 指向你的 keystore 後再執行：
  ```bash
  ./gradlew :app:assembleRelease
  ```
