package com.churchmanagement.mobile.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.churchmanagement.mobile.data.AccountDeletionRepository
import com.churchmanagement.mobile.domain.AppUser
import com.churchmanagement.mobile.util.AppLinks
import com.churchmanagement.mobile.util.openSafely
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// Mesmos textos da página web /excluir-conta (DeleteAccountPage.tsx) e de docs/exclusao-de-conta.md.
private val DELETED_ITEMS = listOf(
    "Seu login e seu perfil (nome, e-mail, telefone, biografia e preferências)",
    "Suas fotos de perfil",
    "Notificações recebidas e preferências de notificação",
    "Confirmações de presença em eventos e inscrições em projetos",
    "Seu progresso em devocionais e planos de leitura",
    "Curtidas, perfil e atividades no fórum e presença em transmissões",
)

private val ANONYMIZED_ITEMS = listOf(
    "Tópicos e respostas no fórum, inclusive a indicação de \"última resposta\" nos tópicos em que você respondeu por último",
    "Comentários no blog e nos devocionais",
    "Pedidos de oração enviados com o e-mail da conta",
)

private val KEPT_ITEMS = listOf(
    "Registros institucionais, financeiros, de assistência, pedagógicos e de frequência",
    "Cadastros de membro, voluntário e liderança mantidos pela secretaria: não têm vínculo com o login e não são alterados (peça à igreja para corrigi-los ou removê-los)",
    "Posts do blog e devocionais publicados pela equipe: o nome do autor permanece até a administração editá-los",
    "Cadastro de profissional de assistência: o vínculo com a conta é removido e o cadastro fica inativo",
    "Logs de auditoria exigidos por lei",
)

/** Âmbar para a seção "anonimizado" (mesma ideia da web; o tema não tem uma cor de alerta). */
private val AmberContainer = Color(0xFFFFF4E0)
private val OnAmberContainer = Color(0xFF7A4B00)

@Composable
fun DeleteAccountScreen(user: AppUser, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val deletion: AccountDeletionRepository = koinInject()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    val method = remember(user.uid) { reauthMethodFor(deletion.providerIds()) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<DeletionError?>(null) }

    val canSubmit = !deleting && canSubmitDeletion(method, password, confirmation)

    fun submit() {
        if (!canSubmit) return
        deleting = true
        error = null
        scope.launch {
            val failure = deletion.deleteOwnAccount(password)
            // Sucesso: o sign-out já aconteceu e o App troca para o login (com o aviso).
            if (failure != null) {
                if (failure.kind == DeletionErrorKind.REAUTH_AGAIN || failure.kind == DeletionErrorKind.WRONG_PASSWORD) {
                    password = ""
                }
                error = failure
                deleting = false
            }
        }
    }

    if (deleting) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.error)
            Text(
                text = "Excluindo sua conta...",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = "Não feche o aplicativo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text("Excluir minha conta", style = MaterialTheme.typography.headlineSmall)
            if (!user.email.isNullOrBlank()) {
                Text(
                    text = "Conta: ${user.email}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Esta ação é permanente e não pode ser desfeita.", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Depois de excluída, sua conta não poderá ser recuperada. Para voltar a usar o " +
                        "sistema será preciso criar uma nova conta.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        PolicySection(
            title = "O que é apagado",
            items = DELETED_ITEMS,
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
        )
        PolicySection(
            title = "O que é anonimizado",
            intro = "O conteúdo continua visível para a comunidade, mas seu nome passa a aparecer como " +
                "\"Usuário removido\" e seu e-mail, telefone e foto são removidos:",
            items = ANONYMIZED_ITEMS,
            container = AmberContainer,
            content = OnAmberContainer,
        )
        PolicySection(
            title = "O que é mantido",
            items = KEPT_ITEMS,
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (method) {
            ReauthMethod.WEB_ONLY -> {
                Text(
                    text = "Sua conta não usa senha (por exemplo, login com Google), e o aplicativo só " +
                        "confirma a identidade com e-mail e senha. Para excluí-la, acesse a página " +
                        "\"Excluir minha conta\" no site e entre com a mesma conta.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = { uriHandler.openSafely(AppLinks.DELETE_ACCOUNT_URL) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Excluir pelo site") }
                ContactLine(prefix = "Não consegue entrar? Peça a exclusão pelo e-mail") {
                    uriHandler.openSafely(AppLinks.CHURCH_CONTACT_MAILTO)
                }
            }
            ReauthMethod.PASSWORD -> {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Confirme sua senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text("Digite $DELETE_CONFIRMATION_WORD para confirmar") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        error?.let { failure ->
            val lastAdmin = failure.kind == DeletionErrorKind.LAST_ADMIN
            Surface(
                color = if (lastAdmin) AmberContainer else MaterialTheme.colorScheme.errorContainer,
                contentColor = if (lastAdmin) OnAmberContainer else MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (lastAdmin) {
                        Text(LAST_ADMIN_TITLE, fontWeight = FontWeight.SemiBold)
                    }
                    Text(failure.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
            ContactLine(prefix = "Precisa de ajuda? Fale com a igreja pelo e-mail") {
                uriHandler.openSafely(AppLinks.CHURCH_CONTACT_MAILTO)
            }
        }

        if (method == ReauthMethod.PASSWORD) {
            Button(
                onClick = ::submit,
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Excluir minha conta permanentemente") }
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Saiba mais na nossa",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { uriHandler.openSafely(AppLinks.PRIVACY_POLICY_URL) }) {
                Text("Política de Privacidade", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Texto + e-mail oficial da igreja (o endereço fica visível mesmo sem app de e-mail instalado). */
@Composable
private fun ContactLine(prefix: String, onOpenMail: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = prefix,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onOpenMail) {
            Text(AppLinks.CHURCH_CONTACT_EMAIL, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    items: List<String>,
    container: Color,
    content: Color,
    intro: String? = null,
) {
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (intro != null) Text(intro, style = MaterialTheme.typography.bodySmall)
            items.forEach { item ->
                Row {
                    Text("•", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 6.dp))
                    Text(item, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
