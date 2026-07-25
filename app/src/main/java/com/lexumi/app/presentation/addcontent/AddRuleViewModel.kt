package com.lexumi.app.presentation.addcontent

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.repository.SectionRepository
import com.lexumi.app.domain.repository.TopicRepository
import com.lexumi.app.domain.usecase.AddResult
import com.lexumi.app.domain.usecase.AddRuleUseCase
import com.lexumi.app.util.OcrHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddRuleViewModel @Inject constructor(
    private val addRule: AddRuleUseCase,
    private val topicRepository: TopicRepository,
    private val sectionRepository: SectionRepository,
    private val ocrHelper: OcrHelper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val topicId: Long = checkNotNull(savedStateHandle["topicId"])
    private var languageId: Long? = null

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _created = MutableStateFlow(false)
    val created: StateFlow<Boolean> = _created

    private val _recognizedText = MutableStateFlow<String?>(null)
    val recognizedText: StateFlow<String?> = _recognizedText

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    init {
        viewModelScope.launch {
            val topic = topicRepository.getTopic(topicId)
            languageId = topic?.let { sectionRepository.getSection(it.sectionId)?.languageId }
        }
    }

    /** Runs OCR on the attached photo so its text can pre-fill (and be edited in) the text field. */
    fun scanPhoto(uri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            val recognized = ocrHelper.recognizeText(uri)
            if (recognized.isNotBlank()) _recognizedText.value = recognized
            _isScanning.value = false
        }
    }

    fun consumeRecognizedText() { _recognizedText.value = null }

    fun submit(name: String, text: String, imagePath: String?) {
        viewModelScope.launch {
            val langId = languageId ?: return@launch
            when (addRule(langId, name, text, imagePath)) {
                is AddResult.Success -> _created.value = true
                AddResult.AlreadyExists -> _error.value = "Таке правило вже існує в цій мові"
                AddResult.Blank -> _error.value = "Введіть назву і текст правила або додайте фото"
            }
        }
    }

    fun clearError() { _error.value = null }
}
