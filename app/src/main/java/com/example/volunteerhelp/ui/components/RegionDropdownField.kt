package com.example.volunteerhelp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.volunteerhelp.util.UkraineRegions

@Composable
fun RegionDropdownField(
    selectedRegion: String,
    onRegionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Область",
    includeAllOption: Boolean = false,
    allLabel: String = "Усі області",
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val options = remember(includeAllOption) {
        if (includeAllOption) listOf(allLabel) + UkraineRegions.values else UkraineRegions.values
    }
    val filteredOptions = options.filter { option ->
        searchQuery.isBlank() || option.contains(searchQuery.trim(), ignoreCase = true)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedRegion.ifBlank { if (includeAllOption) allLabel else "" },
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label) },
                placeholder = { Text("Оберіть область") },
                readOnly = true,
                isError = isError,
                trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                singleLine = true
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable {
                        expanded = true
                        searchQuery = ""
                    }
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                AnimatedVisibility(visible = expanded) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("Пошук області") },
                        singleLine = true
                    )
                }
            }
            if (filteredOptions.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Нічого не знайдено", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {}
                )
            } else {
                filteredOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onRegionSelected(if (includeAllOption && option == allLabel) "" else option)
                            expanded = false
                            searchQuery = ""
                        }
                    )
                }
            }
        }
    }
}
