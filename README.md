A customisable search dialog component that provides an alternative to drop-down menus by allowing users to search for items using a text field in a pop-up dialog. This component displays suggestions that closely match the search query and enables users to select from the suggested results.

## Overview

The `SearchDialog` component consists of two main states:

- **View State**: Displays a clickable surface showing the selected item or placeholder text
- **Search State**: Opens a dialog with a search field and filtered suggestions

The component uses advanced string matching algorithms with debounced search queries for optimal performance.

## Demo

https://github.com/user-attachments/assets/1cea632d-505d-4108-b480-ebd11db1dbe4



---
## Basic Usage

```kotlin
@Composable
fun MyScreen() {
    val searchDialogState = rememberSearchDialogState(africanCountriesList)
    
    SearchDialog(
        searchDialogState = searchDialogState,
        placeholder = "Select a country",
        onSelectItem = { selectedCountry ->
            // Handle selection
            myViewModel.onSelect(selectedCountry)
        }
    )
}
```

## Parameters

### SearchDialog

| Parameter           | Type                       | Default                                  | Description                                                         |
| ------------------- | -------------------------- | ---------------------------------------- | ------------------------------------------------------------------- |
| `modifier`          | `Modifier`                 | `Modifier`                               | Modifier to be applied to the component                             |
| `placeholder`       | `String`                   | `"Search"`                               | Placeholder text shown in search field and when no item is selected |
| `searchDialogState` | `SearchDialogState`        | `rememberSearchDialogState(emptyList())` | State holder for the search dialog                                  |
| `leadingIcon`       | `@Composable (() -> Unit)` | `{}`                                     | Optional leading icon composable                                    |
| `onSelectItem`      | `(String) -> Unit`         | Required                                 | Callback invoked when user selects an item                          |

### SearchDialogState

The state holder manages the search functionality and dialog state transitions.

| Property      | Type                               | Description                                  |
| ------------- | ---------------------------------- | -------------------------------------------- |
| `query`       | `String`                           | Current search query                         |
| `foundMatch`  | `Boolean`                          | Whether search results were found            |
| `suggestions` | `StateFlow<List<AnnotatedString>>` | Flow of filtered and highlighted suggestions |
| `queryFlow`   | `StateFlow<String>`                | Flow of search queries for debouncing        |

## Key Features

### 1. Debounced Search

- 300ms debounce prevents excessive search operations
- Uses `distinctUntilChanged()` to avoid duplicate searches
- Performs search operations on IO dispatcher for performance

### 2. Text Highlighting

- Highlights matching text segments in yellow
- Uses `AnnotatedString` with `SpanStyle` for visual emphasis
- Maintains original text while adding visual indicators

### 3. Suffix Array Matching

- Utilizes `GSuffArray` for efficient string matching
- Returns both item indices and character positions
- Supports complex pattern matching scenarios

### 4. Responsive UI States

- **Empty State**: Shows "Type to bring up suggestions"
- **No Results**: Displays "No matching suggestions" with error styling
- **Results**: Shows filtered list with highlighted matches
- **Error State**: TextField shows error state when no matches found

---

## Implementation Details

### State Management

The component uses a combination of:

- `mutableStateOf` for immediate UI updates
- `StateFlow` for reactive search operations
- `LaunchedEffect` for coroutine-based search processing

### Performance Optimizations

- Debounced search queries prevent excessive calls to GSuffArray match method
- IO dispatcher for heavy string matching operations
- Lazy loading of suggestions list
- Efficient suffix array algorithm for pattern matching

### UI Composition

- Material 3 design components (`OutlinedTextField`, `Card`, `Surface`)
- Responsive layout with `LazyColumn` for large datasets
- Smooth animations for state transitions
- Proper accessibility support

## State Transitions

```
┌─────────────┐    click/tap    ┌──────────────┐
│ View State  │ ──────────────► │ Search State │
│             │                 │              │
└─────────────┘ ◄────────────── └──────────────┘
                select item/
                dismiss dialog
```

## Error Handling

The component handles several error scenarios:

- **No matches found**: Shows error state in TextField
- **Empty query**: Displays helpful placeholder text
- **Invalid input**: Gracefully handles edge cases

## Related Components

- `rememberSearchDialogState()`: Creates and remembers the dialog state
- `GSuffArray`: Efficient string matching algorithm implementation
- Standard Material 3 components for consistent UI

## Common Use Cases

This component is designed with the intent to enable easy discovery of items from a decently large item set. Its strength lies in the fact that a search key yields closely matching search results. keys don't have to be spelt entirely correct before a close match is found. It is a UX improvement over drop-down menus for decently large data set where users have to visually scan to find the desired item.

- Country/region selection
- Product search and selection
- User search in contact lists
- Category filtering
- Any searchable drop-down alternative

This component provides a modern, efficient alternative to traditional dropdown menus while maintaining excellent user experience and performance characteristics.
