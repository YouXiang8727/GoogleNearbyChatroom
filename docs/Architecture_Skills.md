# 架構規範 Skills

1. **UI 框架**: 使用 **Jetpack Compose** (詳見 [Compose 使用規範](Compose_Skills.md))。
2. **架構模式**: 採用 **MVI (Model-View-Intent)** + **Clean Architecture**。
    *   **MVI 封裝**: 需建立 `BaseViewModel` 及 `UiState`, `UiEvent`, `UiEffect` 的封裝機制。
    *   **Clean Architecture**: 劃分為 `data`, `domain`, `presentation` 三層，業務邏輯封裝在 **UseCase** 中。
3. **依賴注入 (DI)**: 使用 **Hilt**。
4. **網路請求**: 若需要網路 API 或下載，導入 **Retrofit 2**。
5. **頁面導航**: 導入 **Navigation Compose**。
6. **本地資料庫**: 導入 **Room**。
7. **異步處理**: 使用 **Kotlin Flow** 取代傳統的 Callback。
8. **資料持久化**: 若需要類似 Shared Preferences 的功能，導入 **Data Store**。
9. **依賴管理**: 使用 `libs.versions.toml` (Version Catalog)。
10. **程式碼維護**: 遇到 **Deprecated (過時)** 的方法或 API 時，應主動尋找替代方案並進行 **自動替換/升級**。
11. **導航驅動**: 優先使用 **ViewModel Effect** 觸發頁面跳轉 (Navigation)，確保 UI 邏輯與導航狀態分離。
12. **資料序列化**: P2P 通訊或資料傳輸時，統一使用 **Gson** 或 **Kotlin Serialization** 處理結構化資料。
13. **異步生命週期**: Repository 中若有需超越單一頁面的長時任務，應使用自定義的 **CoroutineScope** (如 `repositoryScope`) 管理。
