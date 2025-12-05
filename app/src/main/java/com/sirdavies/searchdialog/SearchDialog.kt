package com.sirdavies.searchdialog

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirdavies.searchdialog.utility.GSuffArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Search dialog is an alternative to drop down menu where
 * users can search for items using a text field in a pop-up dialog
 * instead of scrolling to find desired item. Suggestions that closely match
 * the search query are populated in the dialog and the user can select.
 * */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Composable
fun SearchDialog(
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    searchDialogState: SearchDialogState,
    leadingIcon: @Composable (() -> Unit) = {},
    onSelectItem: (String) -> Unit
) {

    fun highlight(
        query: String,
        items: List<String>,
        matches: Map<Int, List<Int>>
    ): List<AnnotatedString> {
        if (query.isEmpty()) return items.map { AnnotatedString(it) }

        return matches
            .map { entry ->     // build list of annotated string from the index and character positions
                buildAnnotatedString {
                    append(items[entry.key])
                    entry.value.forEach {
                        addStyle(
                            style = SpanStyle(background = Color.Yellow),
                            start = it,
                            end = it + query.length
                        )
                    }
                }
            }
    }



    @Composable
    fun TextFieldError(textError: String) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = textError,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    @Composable
    fun ViewState(text: String) {
        val rotate = animateFloatAsState(if (searchDialogState.isSearch()) 180f else 0f)
        Surface(
            modifier = modifier
                .clickable { searchDialogState.updateViewState() },
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Distributes space between children
            ) {
                // Group leading icon and text, apply spacing within this group
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leadingIcon()
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (text.isEmpty()) placeholder else text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                IconButton(onClick = { searchDialogState.updateViewState() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotate.value)
                    )
                }
            }
        }
    }


    @Composable
    fun SearchState(
        suggestions: List<AnnotatedString>,
        isError: Boolean,
        searchTerm: String,
        onQueryChange: (String) -> Unit,
        onSelectSuggestion: (String) -> Unit
    ) {
        Dialog(
            onDismissRequest = {
                onQueryChange("")
                searchDialogState.updateViewState()
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    //.fillMaxWidth()
                    .width(400.dp)
                    .padding(16.dp) // Padding for the card itself within the dialog
                    .heightIn(max = 500.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp), // Padding for the content inside the Card
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(), // Ensure TextField takes full width
                        value = searchTerm,
                        onValueChange = onQueryChange,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        label = {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        singleLine = true,
                        isError = isError,
                        supportingText = { if (isError) TextFieldError(textError = "No results found") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        },
                    )

                    // Space between TextField and the Divider/List
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Allow LazyColumn to take available space
                            .animateContentSize()
                    ) {
                        if (suggestions.isEmpty() && searchTerm.isNotEmpty() && isError) { // Show "Type to bring up suggestions" or "No results" based on context
                            item {
                                Box( // Use Box to center the text within the LazyColumn area
                                    modifier = Modifier
                                        .fillParentMaxSize() // Fill the LazyColumn space
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No matching suggestions",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (searchTerm.isEmpty() && !isError) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillParentMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Type to bring up suggestions",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(items = suggestions, key = { item -> item }) { itemText ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth() // Ensure item takes full width
                                        .clickable { onSelectSuggestion(itemText.text) }
                                        .padding(
                                            vertical = 12.dp,
                                            horizontal = 8.dp
                                        ) // Padding for each item
                                ) {
                                    Text(
                                        text = itemText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                // Divider between items, but not after the last one
                                if (suggestions.last() != itemText) {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) // Softer divider
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(searchDialogState.queryFlow) {
        searchDialogState.queryFlow
            .debounce(300)                     // wait 300ms of no new queries
            .distinctUntilChanged()            // only if query actually changed
            .mapLatest { query ->
                // perform suffix-array lookup on IO dispatcher
                withContext(Dispatchers.IO) {
                    highlight(
                        query = query,
                        items = searchDialogState.items,
                        matches = searchDialogState.findMatchingIndex(query)
                    )
                }
            }
            .collectLatest { result ->
                searchDialogState.suggestions.value = result
                if (!result.isEmpty()) { searchDialogState.foundMatch = true }
                else { searchDialogState.foundMatch = false }
            }
    }

    Box(
        modifier = Modifier.wrapContentSize(),
        contentAlignment = Alignment.Center
    ) {
        val text = searchDialogState.query
        ViewState(text = text.ifEmpty { placeholder })
        if (searchDialogState.isSearch()) {
            SearchState(
                suggestions = searchDialogState.suggestions.collectAsStateWithLifecycle().value,
                searchTerm = searchDialogState.query,
                isError = searchDialogState.foundMatch.not(),
                onQueryChange = { query ->
                    searchDialogState.query = query
                    searchDialogState.queryFlow.value = query
                },
                onSelectSuggestion = {
                    onSelectItem(it)
                    searchDialogState.query = it
                    searchDialogState.queryFlow.value = it
                    searchDialogState.updateViewState()
                }
            )
        }
    }
}

class SearchDialogState(scope: CoroutineScope, val items: List<String>) {
    var query: String by mutableStateOf("")
    var foundMatch: Boolean by mutableStateOf(false)

    private lateinit var gsa: GSuffArray
    val suggestions: MutableStateFlow<List<AnnotatedString>> = MutableStateFlow(emptyList<AnnotatedString>())
    val queryFlow: MutableStateFlow<String> = MutableStateFlow("")
    private var dialogState by mutableStateOf(State.VIEW)

    init {
        scope.launch {
            withContext(Dispatchers.Default) {
                gsa = GSuffArray(*items.toTypedArray())
            }
        }
    }

    private enum class State { VIEW, SEARCH }

    fun isView(): Boolean = dialogState == State.VIEW
    fun isSearch(): Boolean = dialogState == State.SEARCH

    fun updateViewState() {
        dialogState = State.entries[(dialogState.ordinal + 1) % 2]
    }

    fun findMatchingIndex(query: String): Map<Int, List<Int>> {
        if (query.isEmpty()) return emptyMap()

        return gsa.index(query)
            .groupBy { (_, itemIndex) -> itemIndex } // group by index to yield a map of index to a list of index and character positions
            .mapValues { (_, list) -> list.map { (start, _) -> start } } // generate new map of index(key) to list of character positions (Values)
    }

    fun match(query: String) = gsa.match(query)
}

@Composable
fun rememberSearchDialogState(searchItems: List<String>): SearchDialogState {
    val scope = rememberCoroutineScope()
    return remember {
        SearchDialogState(
            scope = scope,
            items = searchItems
        )
    }
}

val africanCountriesList = listOf(
    "Algeria", "Angola", "Benin", "Botswana", "Burkina Faso", "Burundi",
    "Cabo Verde", "Cameroon", "Central African Republic", "Chad", "Comoros",
    "Democratic Republic of the Congo", "Republic of the Congo", "Côte d'Ivoire",
    "Djibouti", "Egypt", "Equatorial Guinea", "Eritrea", "Eswatini", "Ethiopia",
    "Gabon", "Gambia", "Ghana", "Guinea", "Guinea-Bissau", "Kenya", "Lesotho",
    "Liberia", "Libya", "Madagascar", "Malawi", "Mali", "Mauritania", "Mauritius",
    "Morocco", "Mozambique", "Namibia", "Niger", "Nigeria", "Rwanda",
    "São Tomé and Príncipe", "Senegal", "Seychelles", "Sierra Leone", "Somalia",
    "South Africa", "South Sudan", "Sudan", "Tanzania", "Togo", "Tunisia",
    "Uganda", "Zambia", "Zimbabwe"
)

@Preview
@Composable
fun SearchDialogPreview() {
    val placeholder = "Pick a country"
    SearchDialog(
        searchDialogState = rememberSearchDialogState(africanCountriesList),
        placeholder = placeholder,
        onSelectItem = {  }
    )
}

fun main() {
    // Unit test for GSuffArray index method
    val list = listOf("Hello world Hello", "Fellow", "Yellow", "Hero")
    val gsa = GSuffArray(*list.toTypedArray())
    val query = "ello"
    val positionsByItem = gsa.index(query = query)
        .groupBy { (_, itemIndex) -> itemIndex }
        .mapValues { (_, list) -> list.map { (start, _) -> start } }

    println(positionsByItem)

    println(gsa.index(query))
    list.mapIndexed { index, text ->
        println("$text:")
        println()
        // only style if there are matches for this item
        positionsByItem[index]?.forEach { start ->
            val end = (start + query.length)
            print(text.substring(startIndex = start, endIndex = end))
            print(", ")
        }
        println()
    }

}