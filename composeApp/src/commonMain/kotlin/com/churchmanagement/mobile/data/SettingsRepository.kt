package com.churchmanagement.mobile.data

import com.churchmanagement.mobile.data.firestore.Collections
import com.churchmanagement.mobile.data.model.SettingsDto
import com.churchmanagement.mobile.domain.OrgSettings
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val firestore: FirebaseFirestore) {

    /** Configurações da organização (cores, nome). Documento `settings/church`. */
    fun observeSettings(): Flow<OrgSettings> =
        firestore.collection(Collections.SETTINGS)
            .document(Collections.SETTINGS_DOC_CHURCH)
            .snapshots
            .map { snapshot ->
                runCatching { snapshot.data(SettingsDto.serializer()).toDomain() }
                    .getOrDefault(OrgSettings.DEFAULT)
            }
}

private fun SettingsDto.toDomain() = OrgSettings(
    churchName = churchName,
    logoUrl = logoURL,
    primaryColor = primaryColor,
    secondaryColor = secondaryColor,
)
