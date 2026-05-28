# Compose 使用規範

1. **狀態提升 (State Hoisting)**:
    *   盡可能使 Composable 成為 **Stateless** (無狀態)。
    *   將狀態向上提升至 Caller 或 ViewModel，透過參數傳遞狀態，並透過 Lambda 回傳事件。

2. **預覽 (Preview)**:
    *   每個主要的 UI Composable 都必須提供 `@Preview`。
    *   建議包含不同佈景主題 (Light/Dark Mode) 的預覽。

3. **語義化與佈景主題 (Theming)**:
    *   禁止使用 Hardcoded (硬編碼) 的顏色值或字體大小。
    *   使用 `MaterialTheme.colorScheme` 取得顏色。
    *   使用 `MaterialTheme.typography` 取得文字樣式。

4. **資源管理 (Resources)**:
    *   所有文字必須定義在 `res/values/strings.xml` 中。
    *   使用 `stringResource(id = R.string.example)` 取得字串，嚴禁直接在代碼中寫死字串內容。

5. **系統互動與權限**:
    *   自定義返回行為需使用 `BackHandler`。
    *   系統權限請求需使用 `rememberLauncherForActivityResult` 配合 `ActivityResultContracts.RequestMultiplePermissions()`。

6. **組件設計**:
    *   將通用的 UI 區塊 (如 Dialog, Chat Bubble) 獨立為專屬的 Composable 文件或函式，提高程式碼重用性。
    *   使用 `LaunchedEffect` 處理與 Compose 狀態相關的副作用（如列表自動捲動）。
