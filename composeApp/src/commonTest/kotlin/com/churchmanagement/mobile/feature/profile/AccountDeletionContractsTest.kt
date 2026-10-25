package com.churchmanagement.mobile.feature.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountDeletionContractsTest {

    @Test
    fun confirmationRequiresTheExactWordIgnoringSurroundingSpaces() {
        assertTrue(isDeleteConfirmationValid("EXCLUIR"))
        assertTrue(isDeleteConfirmationValid(" EXCLUIR "))
        assertFalse(isDeleteConfirmationValid("excluir"))
        assertFalse(isDeleteConfirmationValid("EXCLUI"))
        assertFalse(isDeleteConfirmationValid("EX CLUIR"))
        assertFalse(isDeleteConfirmationValid(""))
    }

    @Test
    fun passwordAccountsReauthenticateInAppAndOthersGoToTheWeb() {
        assertEquals(ReauthMethod.PASSWORD, reauthMethodFor(listOf("firebase", "password")))
        assertEquals(ReauthMethod.PASSWORD, reauthMethodFor(listOf("google.com", "password")))
        assertEquals(ReauthMethod.WEB_ONLY, reauthMethodFor(listOf("google.com")))
        assertEquals(ReauthMethod.WEB_ONLY, reauthMethodFor(emptyList()))
    }

    @Test
    fun submitNeedsPasswordAndConfirmation() {
        assertTrue(canSubmitDeletion(ReauthMethod.PASSWORD, "segredo", "EXCLUIR"))
        assertFalse(canSubmitDeletion(ReauthMethod.PASSWORD, "", "EXCLUIR"))
        assertFalse(canSubmitDeletion(ReauthMethod.PASSWORD, "segredo", "excluir"))
        assertFalse(canSubmitDeletion(ReauthMethod.WEB_ONLY, "segredo", "EXCLUIR"))
    }

    @Test
    fun lastAdminUsesServerMessageAndFallsBackWithoutReason() {
        val serverMessage = "Você é o último administrador do sistema. Promova outro usuário a administrador antes de excluir sua conta."
        val withReason = callableDeletionError("FAILED_PRECONDITION", "last-admin", serverMessage)
        assertEquals(DeletionErrorKind.LAST_ADMIN, withReason.kind)
        assertEquals(serverMessage, withReason.message)

        // Mesma regra da web: failed-precondition sem reason também é "último administrador".
        val withoutReason = callableDeletionError("FAILED_PRECONDITION", null, "")
        assertEquals(DeletionErrorKind.LAST_ADMIN, withoutReason.kind)
        assertTrue(withoutReason.message.contains("último administrador"))
    }

    @Test
    fun staleAuthTimeAsksToConfirmAgain() {
        assertEquals(
            DeletionErrorKind.REAUTH_AGAIN,
            callableDeletionError("UNAUTHENTICATED", "recent-login-required", "Por segurança...").kind,
        )
        assertEquals(DeletionErrorKind.REAUTH_AGAIN, callableDeletionError("UNAUTHENTICATED", null, null).kind)
        // Código no formato da web (functions/unauthenticated → unauthenticated) também é aceito.
        assertEquals(DeletionErrorKind.REAUTH_AGAIN, callableDeletionError("unauthenticated", null, null).kind)
    }

    @Test
    fun internalKeepsServerMessageButHidesRawCodes() {
        val friendly = "Não foi possível concluir a exclusão da sua conta. Tente novamente em alguns minutos."
        assertEquals(friendly, callableDeletionError("INTERNAL", null, friendly).message)
        val raw = callableDeletionError("INTERNAL", null, "INTERNAL")
        assertEquals(DeletionErrorKind.GENERIC, raw.kind)
        assertFalse(raw.message == "INTERNAL")
    }

    @Test
    fun unknownAndTransportErrorsAreGenericOrNetwork() {
        assertEquals(DeletionErrorKind.NETWORK, callableDeletionError("UNAVAILABLE", null, "unavailable").kind)
        assertEquals(DeletionErrorKind.NETWORK, callableDeletionError("DEADLINE_EXCEEDED", null, null).kind)
        val unknown = callableDeletionError("PERMISSION_DENIED", null, "Some English SDK text")
        assertEquals(DeletionErrorKind.GENERIC, unknown.kind)
        assertFalse(unknown.message.contains("English"))
        assertEquals(DeletionErrorKind.GENERIC, callableDeletionError(null, null, null).kind)
    }

    @Test
    fun reasonIsReadFromDetailsMap() {
        assertEquals("last-admin", deletionReasonFrom(mapOf("reason" to "last-admin")))
        assertNull(deletionReasonFrom(null))
        assertNull(deletionReasonFrom("last-admin"))
        assertNull(deletionReasonFrom(mapOf("reason" to 1)))
    }

    @Test
    fun reauthFailuresHaveUserFacingMessages() {
        assertEquals("Senha incorreta. Verifique e tente novamente.", reauthDeletionError(DeletionErrorKind.WRONG_PASSWORD).message)
        assertTrue(reauthDeletionError(DeletionErrorKind.TOO_MANY_REQUESTS).message.startsWith("Muitas tentativas"))
        assertTrue(reauthDeletionError(DeletionErrorKind.GENERIC).message.startsWith("Não foi possível confirmar"))
    }
}
