package com.churchmanagement.mobile.sdui.render

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.churchmanagement.mobile.sdui.data.DataResolver
import com.churchmanagement.mobile.sdui.model.UiNode
import com.churchmanagement.mobile.sdui.model.int
import com.churchmanagement.mobile.sdui.model.string
import com.churchmanagement.mobile.ui.EmptyState
import com.churchmanagement.mobile.ui.ListSkeleton
import com.churchmanagement.mobile.ui.MemberAvatar
import com.churchmanagement.mobile.ui.SkeletonBox
import com.churchmanagement.mobile.ui.WhatsAppButton
import com.churchmanagement.mobile.ui.shimmerBrush
import com.churchmanagement.mobile.util.currentLocalDate
import com.churchmanagement.mobile.util.monthNamePt
import org.koin.compose.koinInject

/**
 * Catálogo de componentes-núcleo do SDUI (Etapa 1). Cada entrada mapeia um `type` do JSON
 * para um composable. Telas inteiras são montadas recombinando estes blocos.
 */
object CoreComponents {
    val entries: Map<String, NodeRenderer> = mapOf(
        "column" to { n, s -> ColumnComponent(n, s) },
        "row" to { n, s -> RowComponent(n, s) },
        "lazyColumn" to { n, s -> LazyColumnComponent(n, s) },
        "box" to { n, s -> BoxComponent(n, s) },
        "card" to { n, s -> CardComponent(n, s) },
        "text" to { n, s -> TextComponent(n, s) },
        "image" to { n, s -> ImageComponent(n, s) },
        "button" to { n, s -> ButtonComponent(n, s) },
        "sectionHeader" to { n, s -> SectionHeaderComponent(n, s) },
        "grid" to { n, s -> GridComponent(n, s) },
        "iconCard" to { n, s -> IconCardComponent(n, s) },
        "list" to { n, s -> ListComponent(n, s) },
        "avatar" to { n, s -> AvatarComponent(n, s) },
        "whatsapp" to { n, s -> WhatsAppComponent(n, s) },
        "spacer" to { n, _ -> SpacerComponent(n) },
        "divider" to { _, _ -> HorizontalDivider() },
    )
}

/** Avatar circular (foto ou inicial). `name`, `photoUrl`, `size`. */
@Composable
private fun AvatarComponent(node: UiNode, scope: RenderScope) {
    val size = (node.props.int("size") ?: 56).dp
    if (scope.skeleton) {
        Shimmer(Modifier.size(size), CircleShape)
        return
    }
    MemberAvatar(
        name = scope.resolve(node.props.string("name")),
        photoUrl = scope.resolve(node.props.string("photoUrl")).ifBlank { null },
        size = size,
    )
}

/** Botão WhatsApp (`phone`). Não renderiza nada se o número for inválido. */
@Composable
private fun WhatsAppComponent(node: UiNode, scope: RenderScope) {
    if (scope.skeleton) {
        Shimmer(Modifier.fillMaxWidth().height(32.dp), RoundedCornerShape(16.dp))
        return
    }
    WhatsAppButton(phone = scope.resolve(node.props.string("phone")).ifBlank { null }, modifier = Modifier.fillMaxWidth())
}

/** Grade de N colunas (props `columns`, `spacing`); distribui os filhos em linhas de largura igual. */
@Composable
private fun GridComponent(node: UiNode, scope: RenderScope) {
    val columns = (node.props.int("columns") ?: 2).coerceAtLeast(1)
    val spacing = node.props.int("spacing") ?: 12
    Column(verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
        node.children.chunked(columns).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.dp)) {
                rowItems.forEach { child -> Box(Modifier.weight(1f)) { RenderNode(child, scope) } }
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Cartão de atalho: ícone centralizado (chave em `icon`) + rótulo (`label`). Espelha a Home nativa. */
@Composable
private fun IconCardComponent(node: UiNode, scope: RenderScope) {
    Card(modifier = Modifier.fillMaxWidth().clickableAction(node, scope)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (scope.skeleton) {
                Shimmer(Modifier.size(24.dp), CircleShape)
                Shimmer(Modifier.fillMaxWidth(0.5f).height(16.dp))
            } else {
                Icon(
                    imageVector = sduiIcon(node.props.string("icon")),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(text = scope.resolve(node.props.string("label")), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * Lista INLINE vinculada a `source`, para listas pequenas dentro do scroll de uma tela
 * (ex.: destaque na Home, carrossel). `orientation` = "horizontal" vira carrossel (LazyRow).
 * `header` é um título exibido só quando há itens. Para listas grandes de tela inteira,
 * prefira o `lazyColumn` data-bound (lazy de verdade).
 */
@Composable
private fun ListComponent(node: UiNode, scope: RenderScope) {
    val source = node.props.string("source") ?: return
    val template = node.itemTemplate ?: return
    val limit = node.props.int("limit")
    val spacing = node.props.int("spacing") ?: 12
    val horizontal = node.props.string("orientation") == "horizontal"
    val itemWidth = node.props.int("itemWidth") ?: 150

    if (scope.skeleton) {
        SkeletonItems(template, scope, limit ?: 3, horizontal, spacing, itemWidth)
        return
    }

    val resolver: DataResolver = koinInject()
    // StateFlow quente e compartilhado → revisita entrega o valor na hora, sem piscar skeleton.
    val items by remember(source, limit, scope.userId) {
        resolver.stream(source, limit, scope.userId)
    }.collectAsState()

    when (val list = items) {
        null -> SkeletonItems(template, scope, limit ?: 3, horizontal, spacing, itemWidth)
        else -> if (list.isEmpty()) {
            node.props.string("emptyText")?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                node.props.string("header")?.let {
                    Text(scope.resolve(it), style = MaterialTheme.typography.titleLarge)
                }
                if (horizontal) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.dp)) {
                        items(list, key = { it["id"] ?: "" }) { item ->
                            Box(Modifier.width(itemWidth.dp)) { RenderNode(template, scope.withItem(item)) }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
                        list.forEach { item -> key(item["id"] ?: "") { RenderNode(template, scope.withItem(item)) } }
                    }
                }
            }
        }
    }
}

// ---- Containers ----

@Composable
private fun ColumnComponent(node: UiNode, scope: RenderScope) {
    Column(
        modifier = Modifier.fillMaxWidth().nodePadding(node).clickableAction(node, scope),
        verticalArrangement = Arrangement.spacedBy((node.props.int("spacing") ?: 0).dp),
        horizontalAlignment = horizontalAlignmentFor(node.props.string("align")),
    ) {
        node.children.forEach { RenderNode(it, scope) }
    }
}

@Composable
private fun RowComponent(node: UiNode, scope: RenderScope) {
    Row(
        modifier = Modifier.fillMaxWidth().nodePadding(node).clickableAction(node, scope),
        horizontalArrangement = Arrangement.spacedBy((node.props.int("spacing") ?: 0).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        node.children.forEach { RenderNode(it, scope) }
    }
}

/**
 * Coluna rolável. Se tiver `source` + `itemTemplate`, vira uma LISTA LAZY data-bound
 * (compõe só o que está visível → rápido em telas com muitos itens); os `children` estáticos
 * viram itens iniciais (cabeçalho). Sem `source`, rola apenas os `children`.
 */
@Composable
private fun LazyColumnComponent(node: UiNode, scope: RenderScope) {
    val padding = PaddingValues((node.props.int("padding") ?: 0).dp)
    val spacing = Arrangement.spacedBy((node.props.int("spacing") ?: 0).dp)
    val source = node.props.string("source")
    val template = node.itemTemplate

    if (scope.skeleton) {
        // Skeleton derivado do layout: cabeçalho estático + N cópias do itemTemplate, tudo shimmer.
        Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = spacing) {
            node.children.forEach { RenderNode(it, scope) }
            template?.let { tpl -> repeat(5) { RenderNode(tpl, scope) } }
        }
        return
    }

    if (source == null || template == null) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = spacing) {
            items(node.children) { child -> RenderNode(child, scope) }
        }
        return
    }

    val resolver: DataResolver = koinInject()
    val limit = node.props.int("limit")
    // StateFlow quente e compartilhado → revisita entrega o valor na hora, sem piscar skeleton.
    val items by remember(source, limit, scope.userId) {
        resolver.stream(source, limit, scope.userId)
    }.collectAsState()

    when (val list = items) {
        null -> ListSkeleton()
        else -> if (node.props.string("filter") == "monthYear") {
            MonthYearFilteredList(list, template, scope, padding, spacing)
        } else if (list.isEmpty() && node.children.isEmpty()) {
            EmptyState(title = node.props.string("emptyText") ?: "Nada por aqui ainda.")
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = spacing) {
                items(node.children) { child -> RenderNode(child, scope) }
                items(list, key = { it["id"] ?: "" }) { item -> RenderNode(template, scope.withItem(item)) }
            }
        }
    }
}

/**
 * Lista filtrável por mês/ano (props `filter: "monthYear"` no lazyColumn). Cada item precisa
 * dos campos `year` e `month`. Barra fixa com dois seletores; default = mês/ano atual.
 */
@Composable
private fun MonthYearFilteredList(
    items: List<Map<String, String>>,
    template: UiNode,
    scope: RenderScope,
    padding: PaddingValues,
    spacing: Arrangement.HorizontalOrVertical,
) {
    val today = remember { currentLocalDate() }
    val years = remember(items) {
        items.mapNotNull { it["year"]?.takeIf(String::isNotBlank) }.distinct().sortedDescending()
    }
    var year by remember(years) {
        mutableStateOf(years.firstOrNull { it == today.year.toString() } ?: years.firstOrNull() ?: today.year.toString())
    }
    var month by remember { mutableStateOf<Int?>(today.monthNumber) }

    val filtered = items.filter { it["year"] == year && (month == null || it["month"] == month.toString()) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilterDropdown(
                label = month?.let { monthNamePt(it) } ?: "Todos os meses",
                options = buildList {
                    add("Todos os meses" to { month = null })
                    (1..12).forEach { m -> add(monthNamePt(m) to { month = m }) }
                },
                modifier = Modifier.weight(1f),
            )
            FilterDropdown(
                label = year,
                options = years.map { y -> y to { year = y } },
                modifier = Modifier.weight(1f),
            )
        }
        if (filtered.isEmpty()) {
            val label = (month?.let { "${monthNamePt(it)} de " } ?: "") + year
            EmptyState(title = "Nenhum evento em $label.")
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = spacing) {
                items(filtered, key = { it["id"] ?: "" }) { item -> RenderNode(template, scope.withItem(item)) }
            }
        }
    }
}

/** Botão que abre um menu suspenso de opções (rótulo + ação). */
@Composable
private fun FilterDropdown(
    label: String,
    options: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label, maxLines = 1)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (text, action) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { action(); open = false })
            }
        }
    }
}

@Composable
private fun BoxComponent(node: UiNode, scope: RenderScope) {
    Box(modifier = Modifier.fillMaxWidth().nodePadding(node).clickableAction(node, scope)) {
        node.children.forEach { RenderNode(it, scope) }
    }
}

@Composable
private fun CardComponent(node: UiNode, scope: RenderScope) {
    val pad = node.props.int("padding") ?: 16
    val spacing = node.props.int("spacing") ?: 8
    Card(modifier = Modifier.fillMaxWidth().clickableAction(node, scope)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(pad.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.dp),
            horizontalAlignment = horizontalAlignmentFor(node.props.string("align")),
        ) {
            node.children.forEach { RenderNode(it, scope) }
        }
    }
}

// ---- Folhas ----

@Composable
private fun TextComponent(node: UiNode, scope: RenderScope) {
    if (scope.skeleton) {
        Shimmer(Modifier.fillMaxWidth(0.7f).height(textSkeletonHeight(node.props.string("style")).dp))
        return
    }
    val maxLines = node.props.int("maxLines")
    var modifier: Modifier = Modifier
    node.props.int("height")?.let { modifier = modifier.height(it.dp) }
    modifier = modifier.clickableAction(node, scope)
    Text(
        text = scope.resolve(node.props.string("text")),
        style = textStyleFor(node.props.string("style")),
        textAlign = textAlignFor(node.props.string("align")),
        maxLines = maxLines ?: Int.MAX_VALUE,
        overflow = if (maxLines != null) TextOverflow.Ellipsis else TextOverflow.Clip,
        modifier = modifier,
    )
}

@Composable
private fun ImageComponent(node: UiNode, scope: RenderScope) {
    if (scope.skeleton) {
        val h = node.props.int("height") ?: 140
        Shimmer(Modifier.fillMaxWidth().height(h.dp), RoundedCornerShape((node.props.int("cornerRadius") ?: 0).dp))
        return
    }
    val url = scope.resolve(node.props.string("url"))
    if (url.isBlank()) return
    var modifier: Modifier = Modifier.fillMaxWidth()
    node.props.int("height")?.let { modifier = modifier.height(it.dp) }
    val radius = node.props.int("cornerRadius") ?: 0
    if (radius > 0) modifier = modifier.clip(RoundedCornerShape(radius.dp))
    modifier = modifier.clickableAction(node, scope)
    AsyncImage(
        model = url,
        contentDescription = node.props.string("contentDescription"),
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

@Composable
private fun ButtonComponent(node: UiNode, scope: RenderScope) {
    if (scope.skeleton) {
        Shimmer(Modifier.fillMaxWidth().height(44.dp), RoundedCornerShape(22.dp))
        return
    }
    Button(
        onClick = { node.action?.let { scope.act(it) } },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(scope.resolve(node.props.string("text")))
    }
}

@Composable
private fun SectionHeaderComponent(node: UiNode, scope: RenderScope) {
    if (scope.skeleton) {
        Shimmer(Modifier.fillMaxWidth(0.5f).height(22.dp))
        return
    }
    val actionLabel = node.props.string("actionLabel")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = scope.resolve(node.props.string("title")), style = MaterialTheme.typography.titleLarge)
        val action = node.action
        if (actionLabel != null && action != null) {
            TextButton(onClick = { scope.act(action) }) { Text(actionLabel) }
        }
    }
}

@Composable
private fun SpacerComponent(node: UiNode) {
    Spacer(Modifier.height((node.props.int("height") ?: 8).dp))
}

// ---- Helpers ----

/** Aplica a ação do nó como clique, se houver (desabilitado no skeleton). */
private fun Modifier.clickableAction(node: UiNode, scope: RenderScope): Modifier {
    if (scope.skeleton) return this
    val action = node.action ?: return this
    return this.clickable { scope.act(action) }
}

/** Placeholder shimmer (usado no modo skeleton). */
@Composable
private fun Shimmer(modifier: Modifier, shape: Shape = RoundedCornerShape(6.dp)) {
    SkeletonBox(brush = shimmerBrush(), modifier = modifier, shape = shape)
}

/** Altura aproximada do placeholder de uma linha de texto, por estilo. */
private fun textSkeletonHeight(style: String?): Int = when (style) {
    "displayLarge", "displayMedium", "displaySmall" -> 30
    "headlineLarge", "headlineMedium", "headlineSmall" -> 24
    "titleLarge" -> 22
    "titleMedium", "bodyLarge" -> 18
    "titleSmall", "bodyMedium", "bodySmall" -> 14
    "labelLarge", "labelMedium", "labelSmall" -> 12
    else -> 14
}

/** Renderiza [count] cópias do [template] em modo skeleton (vertical ou carrossel). */
@Composable
private fun SkeletonItems(
    template: UiNode,
    scope: RenderScope,
    count: Int,
    horizontal: Boolean,
    spacing: Int,
    itemWidth: Int,
) {
    val sk = scope.asSkeleton()
    if (horizontal) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.dp)) {
            repeat(count) { Box(Modifier.width(itemWidth.dp)) { RenderNode(template, sk) } }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
            repeat(count) { RenderNode(template, sk) }
        }
    }
}

/** Padding uniforme a partir da prop `padding`. */
private fun Modifier.nodePadding(node: UiNode): Modifier =
    node.props.int("padding")?.let { this.padding(it.dp) } ?: this

@Composable
private fun textStyleFor(name: String?): TextStyle = when (name) {
    "displayLarge" -> MaterialTheme.typography.displayLarge
    "displayMedium" -> MaterialTheme.typography.displayMedium
    "displaySmall" -> MaterialTheme.typography.displaySmall
    "headlineLarge" -> MaterialTheme.typography.headlineLarge
    "headlineMedium" -> MaterialTheme.typography.headlineMedium
    "headlineSmall" -> MaterialTheme.typography.headlineSmall
    "titleLarge" -> MaterialTheme.typography.titleLarge
    "titleMedium" -> MaterialTheme.typography.titleMedium
    "titleSmall" -> MaterialTheme.typography.titleSmall
    "bodyLarge" -> MaterialTheme.typography.bodyLarge
    "bodyMedium" -> MaterialTheme.typography.bodyMedium
    "bodySmall" -> MaterialTheme.typography.bodySmall
    "labelLarge" -> MaterialTheme.typography.labelLarge
    "labelMedium" -> MaterialTheme.typography.labelMedium
    "labelSmall" -> MaterialTheme.typography.labelSmall
    else -> MaterialTheme.typography.bodyMedium
}

private fun textAlignFor(name: String?): TextAlign? = when (name) {
    "center" -> TextAlign.Center
    "end", "right" -> TextAlign.End
    "start", "left" -> TextAlign.Start
    else -> null
}

private fun horizontalAlignmentFor(name: String?): Alignment.Horizontal = when (name) {
    "center" -> Alignment.CenterHorizontally
    "end", "right" -> Alignment.End
    else -> Alignment.Start
}
