package com.lexumi.app.domain.usecase

import com.lexumi.app.data.auth.AuthRepository
import com.lexumi.app.domain.repository.LanguageRepository
import javax.inject.Inject

/** Whether the current user is allowed to add/edit content under this language — false for
 * anyone who isn't an admin on a language that came from the server (`remoteId != null`, whether
 * downloaded by a regular user or already published by the admin themselves). A language that's
 * still purely local (`remoteId == null` — self-authored or not yet published) is always
 * editable. This is UX only, not the actual security boundary: Supabase RLS already refuses a
 * non-admin's write to `owner_id is null` content regardless of what the app's UI shows (see
 * backend/SCHEMA.md, "admin writes global") — this just avoids showing add/edit buttons that
 * would silently fail or, worse, create confusingly-scoped personal content inside admin material. */
class IsLanguageEditableUseCase @Inject constructor(
    private val languageRepository: LanguageRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(languageId: Long): Boolean {
        val language = languageRepository.getLanguage(languageId) ?: return true
        if (language.remoteId == null) return true
        return runCatching { authRepository.getMyProfile()?.isAdmin }.getOrNull() == true
    }
}
