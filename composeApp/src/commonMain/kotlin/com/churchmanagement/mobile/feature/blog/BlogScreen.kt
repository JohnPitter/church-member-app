package com.churchmanagement.mobile.feature.blog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.churchmanagement.mobile.data.BlogRepository
import com.churchmanagement.mobile.domain.BlogPost
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.ListSkeleton
import com.churchmanagement.mobile.util.toLongDate
import org.koin.compose.koinInject

@Composable
fun BlogScreen(onOpenPost: (String) -> Unit, modifier: Modifier = Modifier) {
    val repo: BlogRepository = koinInject()
    val flow = remember { repo.observePublished() }
    val posts by flow.collectAsState(initial = null)

    when (val list = posts) {
        null -> ListSkeleton(modifier)
        else -> if (list.isEmpty()) {
            EmptyState(
                title = "Nenhuma publicação ainda",
                subtitle = "Os artigos publicados aparecerão aqui.",
                modifier = modifier,
            )
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.id }) { post ->
                    BlogCard(post = post, onClick = { onOpenPost(post.id) })
                }
            }
        }
    }
}

@Composable
private fun BlogCard(post: BlogPost, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column {
            if (!post.featuredImage.isNullOrBlank()) {
                AsyncImage(
                    model = post.featuredImage,
                    contentDescription = post.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                )
            }
            Column(Modifier.padding(16.dp)) {
                Text(text = post.title, style = MaterialTheme.typography.titleMedium)
                if (post.excerpt.isNotBlank()) {
                    Text(
                        text = post.excerpt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = buildString {
                        if (post.authorName.isNotBlank()) append(post.authorName)
                        post.publishedAt?.let {
                            if (isNotEmpty()) append(" · ")
                            append(it.toLongDate())
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
