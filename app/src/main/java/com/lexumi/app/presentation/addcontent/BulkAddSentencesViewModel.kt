package com.lexumi.app.presentation.addcontent

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexumi.app.domain.usecase.AddResult
import com.lexumi.app.domain.usecase.AddSentenceUseCase
import com.lexumi.app.util.BulkLineParser
import com.lexumi.app.util.OcrHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BulkAddSentencesViewModel @Inject constructor(
    private val addSentence: AddSentenceUseCase,
    private val ocrHelper: OcrHelper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val topicId: Long = checkNotNull(savedStateHandle["topicId"])

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _summary = MutableStateFlow<BulkImportSummary?>(null)
    val summary: StateFlow<BulkImportSummary?> = _summary

    fun setText(value: String) {
        _text.value = value
        _summary.value = null
    }

    fun scanPhoto(uri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            val recognized = ocrHelper.recognizeText(uri)
            if (recognized.isNotBlank()) {
                _text.value = if (_text.value.isBlank()) recognized else _text.value + "\n" + recognized
            }
            _isScanning.value = false
        }
    }

    fun submitAll() {
        viewModelScope.launch {
            _isSubmitting.value = true
            val lines = BulkLineParser.parse(_text.value)
            var added = 0
            var skippedDuplicate = 0
            lines.forEach { line ->
                val translations = listOf(line.translation) + line.extraTranslations
                // sentences are named by their own text, since there's no separate "name" field in bulk mode
                when (addSentence(topicId, line.main, line.main, translations, emptyList())) {
                    is AddResult.Success -> added++
                    AddResult.AlreadyExists -> skippedDuplicate++
                    AddResult.Blank -> Unit
                }
            }
            val unparsedCount = _text.value.lines().count { it.isNotBlank() } - lines.size
            _summary.value = BulkImportSummary(added, skippedDuplicate, unparsedCount.coerceAtLeast(0))
            if (added > 0) _text.value = ""
            _isSubmitting.value = false
        }
    }
}
