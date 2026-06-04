package com.churchmanagement.mobile.feature.forum

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.churchmanagement.mobile.data.AuthRepository
import com.churchmanagement.mobile.data.ForumRepository
import com.churchmanagement.mobile.domain.ForumCategory
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun NewTopicScreen(onCreated: () -> Unit, modifier: Modifier = Modifier) {
    val repo: ForumRepository = koinInject()
    val auth: AuthRepository = koinInject()
    val scope = rememberCoroutineScope()

    val categoriesFlow = remember { repo.observeCategories() }
    val categories = categoriesFlow.collectAsState().value.orEmpty()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<ForumCategory?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val category = selected
        when {
            title.isBlank() -> { formError = "Informe um título."; return }
            category == null -> { formError = "Selecione uma categoria."; return }
            content.isBlank() -> { formError = "Escreva o conteúdo."; return }
        }
        val user = auth.currentUser
        if (user == null) {
            formError = "Sessão expirada. Entre novamente."
            return
        }
        loading = true
        formError = null
        scope.launch {
            try {
                repo.addTopic(category.id, title, content, user)
                onCreated()
            } catch (e: Throwable) {
                formError = "Não foi possível publicar. Tente novamente."
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Título") },
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Categoria",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = !loading && categories.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = selected?.name ?: "Selecione uma categoria",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            selected = category
                            expanded = false
                        },
                    )
                }
            }
        }
        if (categories.isEmpty()) {
            Text(
                text = "Carregando categorias...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Conteúdo") },
            enabled = !loading,
            minLines = 5,
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(top = 16.dp),
        )

        if (formError != null) {
            Text(
                text = formError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Button(
            onClick = ::submit,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp).padding(end = 4.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Text(if (loading) "Publicando..." else "Publicar tópico")
        }
    }
}
