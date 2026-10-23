package com.rotalog.delivery.login

import com.rotalog.delivery.shared.SectionCard
import com.rotalog.delivery.shared.AppIcons
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSubmit: () -> Unit,
    onRecoverPassword: () -> Unit,
    onExploreDemo: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = { keyboard?.hide(); focus.clearFocus(); onSubmit() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
            val minimumHeight = maxHeight
            Column(
                Modifier.widthIn(max = 420.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .heightIn(min = minimumHeight).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                LoginBrand()
                Spacer(Modifier.weight(1f))
                SectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Entrar na sua conta", style = MaterialTheme.typography.headlineSmall)
                            Text("Use o acesso fornecido pela sua operação.", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        state.message?.let { message ->
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                                Text(message, Modifier.padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite },
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        OutlinedTextField(
                            value = state.email, onValueChange = onEmailChange,
                            modifier = Modifier.fillMaxWidth().testTag("login-email"),
                            label = { Text("E-mail") }, placeholder = { Text("seuemail@exemplo.com") },
                            leadingIcon = { Icon(LoginIcons.Mail, null) },
                            singleLine = true, shape = RoundedCornerShape(12.dp),
                            isError = state.emailError != null,
                            supportingText = state.emailError?.let { error -> { Text(error) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                        )
                        OutlinedTextField(
                            value = state.password, onValueChange = onPasswordChange,
                            modifier = Modifier.fillMaxWidth().testTag("login-password"),
                            label = { Text("Senha") }, placeholder = { Text("Digite sua senha") },
                            leadingIcon = { Icon(LoginIcons.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = onTogglePassword, modifier = Modifier.size(48.dp)) {
                                    Icon(if (state.passwordVisible) LoginIcons.EyeOff else LoginIcons.Eye,
                                        if (state.passwordVisible) "Ocultar senha" else "Mostrar senha")
                                }
                            },
                            singleLine = true, shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = state.passwordError != null,
                            supportingText = state.passwordError?.let { error -> { Text(error) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                        )
                        TextButton(onClick = onRecoverPassword, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                            Text("Esqueci minha senha", style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(onClick = submit, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(12.dp)) {
                            Text("Entrar")
                        }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Acesso para entregadores cadastrados.", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Versão demonstrativa • autenticação não conectada", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onExploreDemo, modifier = Modifier.heightIn(min = 48.dp)) { Text("Explorar demonstração") }
                }
            }
        }
    }
}

@Composable
private fun LoginBrand() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(AppIcons.Truck, null, Modifier.size(26.dp))
            }
        }
        Column {
            Text("RotaLog", style = MaterialTheme.typography.titleMedium)
            Text("Entregador", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
