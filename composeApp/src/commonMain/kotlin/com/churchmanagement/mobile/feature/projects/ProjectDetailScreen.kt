package com.churchmanagement.mobile.feature.projects

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.churchmanagement.mobile.data.ProjectRepository
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.LoadingBox
import com.churchmanagement.mobile.ui.ShareButton
import com.churchmanagement.mobile.util.toLongDate
import org.koin.compose.koinInject

@Composable
fun ProjectDetailScreen(projectId: String, modifier: Modifier = Modifier) {
    val repo: ProjectRepository = koinInject()
    val flow = remember { repo.observeProjects() }
    val projects by flow.collectAsState(initial = null)

    when (val list = projects) {
        null -> LoadingBox(modifier)
        else -> {
            val project = list.firstOrNull { it.id == projectId }
            if (project == null) {
                EmptyState(title = "Projeto não encontrado", modifier = modifier)
                return
            }
            Column(
                modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            ) {
                if (!project.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = project.imageUrl,
                        contentDescription = project.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                    )
                }
                Column(Modifier.padding(20.dp)) {
                    Text(project.name, style = MaterialTheme.typography.headlineSmall)

                    val period = buildString {
                        project.startDate?.let { append(it.toLongDate()) }
                        project.endDate?.let {
                            if (isNotEmpty()) append(" — ")
                            append(it.toLongDate())
                        }
                    }
                    if (period.isNotBlank()) {
                        Text(
                            text = period,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (project.responsible.isNotBlank()) {
                        Text(
                            text = "Responsável: ${project.responsible}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (project.description.isNotBlank()) {
                        Text(
                            text = project.description,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                    if (project.objectives.isNotEmpty()) {
                        Text(
                            text = "Objetivos",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                        )
                        project.objectives.forEach { objective ->
                            Row(Modifier.padding(top = 4.dp)) {
                                Text("•  ", style = MaterialTheme.typography.bodyLarge)
                                Text(objective, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    ShareButton(
                        text = buildString {
                            append(project.name)
                            if (project.description.isNotBlank()) append("\n\n${project.description}")
                        },
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
    }
}
