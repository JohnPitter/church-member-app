package com.churchmanagement.mobile.feature.blog

import androidx.compose.foundation.layout.Column
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
import com.churchmanagement.mobile.data.BlogRepository
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.LoadingBox
import com.churchmanagement.mobile.ui.ShareButton
import com.churchmanagement.mobile.util.stripHtml
import com.churchmanagement.mobile.util.toLongDate
import org.koin.compose.koinInject

@Composable
fun BlogDetailScreen(postId: String, modifier: Modifier = Modifier) {
    val repo: BlogRepository = koinInject()
    val flow = remember { repo.observePublished() }
    val posts by flow.collectAsState(initial = null)

    when (val list = posts) {
        null -> LoadingBox(modifier)
        else -> {
            val post = list.firstOrNull { it.id == postId }
            if (post == null) {
                EmptyState(title = "Publicação não encontrada", modifier = modifier)
                return
            }
            Column(
                modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            ) {
                if (!post.featuredImage.isNullOrBlank()) {
                    AsyncImage(
                        model = post.featuredImage,
                        contentDescription = post.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                    )
                }
                Column(Modifier.padding(20.dp)) {
                    Text(post.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = buildString {
                            if (post.authorName.isNotBlank()) append(post.authorName)
                            post.publishedAt?.let {
                                if (isNotEmpty()) append(" · ")
                                append(it.toLongDate())
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    val body = post.content.stripHtml()
                    if (body.isNotBlank()) {
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                    ShareButton(
                        text = buildString {
                            append(post.title)
                            val summary = post.excerpt.ifBlank { body }
                            if (summary.isNotBlank()) append("\n\n$summary")
                        },
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
    }
}
